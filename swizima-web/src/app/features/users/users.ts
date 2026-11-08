import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { Observable } from 'rxjs';
import { toErrorMessage } from '../../core/http/api-error';
import { DatePipe } from '@angular/common';
import { SwizimaApi } from '../../core/api/swizima-api';
import { debounced, remoteData } from '../../core/data/remote-data';
import { AccountCategory, ActivationStatus, Client, ClientUser, SystemUser } from '../../core/models/api.models';
import { RouterLink } from '@angular/router';
import { ClientPicker } from '../../shared/client-picker/client-picker';
import { EmptyState } from '../../shared/empty-state/empty-state';
import { Icon } from '../../shared/icon/icon';
import { PageHeader } from '../../shared/page-header/page-header';
import { Pagination } from '../../shared/pagination/pagination';
import { StatusBadge, humanize } from '../../shared/status-badge/status-badge';

const PAGE_SIZE = 10;

/** Portal users (usersapi). */
@Component({
  selector: 'app-users',
  imports: [DatePipe, EmptyState, Icon, PageHeader, Pagination, StatusBadge, RouterLink, ClientPicker],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './users.html',
  styles: `
    .user { display: flex; align-items: center; gap: 0.75rem; min-width: 0; }
    .avatar {
      display: grid;
      place-items: center;
      flex: none;
      width: 36px;
      height: 36px;
      border-radius: 50%;
      color: #fff;
      font-weight: 700;
      font-size: 0.85rem;
      background: var(--gradient-brand);
    }
    .channel { display: inline-flex; align-items: center; gap: 0.35rem; app-icon { width: 16px; height: 16px; color: var(--text-muted); } }
    .tabs { display: inline-flex; gap: 0.25rem; padding: 0.25rem; border-radius: 12px; background: var(--bg-muted, rgb(127 127 127 / 0.1)); }
    .tab {
      display: inline-flex; align-items: center; gap: 0.4rem; padding: 0.45rem 0.9rem; border: 0; border-radius: 9px;
      font: inherit; font-weight: 600; color: var(--text-muted); background: transparent; cursor: pointer;
      transition: background 150ms ease, color 150ms ease;
      app-icon { width: 16px; height: 16px; }
      &.active { color: var(--text); background: var(--bg-elevated); box-shadow: var(--shadow-sm, 0 1px 3px rgb(0 0 0 / 0.12)); }
    }
    .tab-bar { margin-bottom: 1rem; }
    .default-pill {
      display: inline-flex; padding: 0.1rem 0.5rem; border-radius: 999px; font-size: 0.72rem; font-weight: 600;
      color: #4338ca; background: rgb(99 102 241 / 0.14);
    }
    .col-actions { width: 1%; white-space: nowrap; text-align: right; }
    .row-actions {
      display: inline-flex; gap: 0.15rem;
      .btn-icon { width: 32px; height: 32px; app-icon { width: 17px; height: 17px; } }
      .on { color: #4338ca; }
    }
    .row-busy { opacity: 0.5; pointer-events: none; }
    .notice { margin-bottom: 1rem; .btn-icon { margin-left: auto; width: 32px; height: 32px; color: inherit; } }
    .dialog {
      width: min(640px, calc(100vw - 2rem)); max-height: calc(100dvh - 2rem); padding: 0;
      border: 1px solid var(--border); color: var(--text); background: var(--bg-elevated); box-shadow: var(--shadow-lg);
      &[open] { animation: dialog-in 200ms var(--ease); }
      &::backdrop { background: rgb(8 12 30 / 0.55); backdrop-filter: blur(3px); }
    }
    @keyframes dialog-in { from { opacity: 0; transform: translateY(8px) scale(0.98); } }
    .dialog-head, .dialog-foot { display: flex; align-items: center; gap: 0.75rem; padding: 1rem 1.25rem; }
    .dialog-head { justify-content: space-between; border-bottom: 1px solid var(--border); h2 { font-size: 1.05rem; margin: 0; } }
    .dialog-foot { justify-content: flex-end; border-top: 1px solid var(--border); }
    .dialog-body { padding: 1.25rem; display: grid; gap: 1.1rem; overflow-y: auto; }
    .details-grid {
      display: grid; grid-template-columns: repeat(2, 1fr); gap: 0.85rem 1rem; margin: 0;
      dt { font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-subtle); }
      dd { margin: 0.15rem 0 0; word-break: break-word; }
    }
    .section-title { margin: 0; font-size: 0.75rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--text-subtle); }
    .org-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.5rem; }
    .org {
      display: flex; align-items: center; gap: 0.75rem; padding: 0.65rem 0.85rem;
      border: 1px solid var(--border); border-radius: 10px; transition: border-color 150ms ease, background 150ms ease;
      &.is-default { border-color: rgb(99 102 241 / 0.45); background: rgb(99 102 241 / 0.06); }
      .grow { flex: 1; min-width: 0; }
      .btn-sm { height: 28px; padding: 0 0.6rem; font-size: 0.78rem; }
    }
    .client-link { color: inherit; font-weight: 600; text-decoration: none; &:hover { text-decoration: underline; } }
    @media (max-width: 720px) { .avatar { display: none; } }
  `,
})
export class Users {
  private readonly api = inject(SwizimaApi);

  protected readonly statuses: ActivationStatus[] = ['PENDING_ACTIVATION', 'ACTIVE', 'SUSPENDED', 'LOCKED', 'DEACTIVATED'];
  protected readonly categories: AccountCategory[] = ['SUPER_ADMIN', 'CLIENT_ADMIN', 'USER'];
  protected readonly humanize = humanize;
  protected readonly pageSize = PAGE_SIZE;

  protected readonly search = signal('');
  protected readonly accountCategory = signal<AccountCategory | ''>('');
  protected readonly activationStatus = signal<ActivationStatus | ''>('');
  protected readonly page = signal(0);
  private readonly searchTerm = debounced(this.search, 350);

  protected readonly users = remoteData(
    computed(() => ({
      search: this.searchTerm().trim(),
      accountCategory: this.accountCategory(),
      activationStatus: this.activationStatus(),
      page: this.page(),
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
    })),
    (query) => this.api.users(query),
  );

  protected readonly hasFilters = computed(
    () => !!this.search().trim() || !!this.accountCategory() || !!this.activationStatus(),
  );

  protected onSearch(value: string): void {
    this.search.set(value);
    this.page.set(0);
  }

  protected onCategory(value: string): void {
    this.accountCategory.set(value as AccountCategory | '');
    this.page.set(0);
  }

  protected onStatus(value: string): void {
    this.activationStatus.set(value as ActivationStatus | '');
    this.page.set(0);
  }

  protected clearFilters(): void {
    this.search.set('');
    this.accountCategory.set('');
    this.activationStatus.set('');
    this.page.set(0);
  }

  // ---------- Client users ----------
  protected readonly tab = signal<'system' | 'client'>('system');
  protected readonly cuSearch = signal('');
  protected readonly cuStatus = signal<ActivationStatus | ''>('');
  protected readonly cuDefault = signal<'' | 'true' | 'false'>('');
  protected readonly cuClient = signal<Client | null>(null);
  protected readonly cuPage = signal(0);
  private readonly cuTerm = debounced(this.cuSearch, 350);

  protected readonly clientUsers = remoteData(
    computed(() => ({
      search: this.cuTerm().trim(),
      activationStatus: this.cuStatus(),
      isDefaultClient: this.cuDefault() === '' ? ('' as const) : this.cuDefault() === 'true',
      clientRef: this.cuClient()?.id,
      page: this.cuPage(),
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
      active: this.tab() === 'client',
    })),
    ({ active, ...query }) => this.api.clientUsers(active ? query : { ...query, size: 1 }),
  );

  /** Client names for the rows (clientapi). */
  private readonly clientsLookup = remoteData(
    computed(() => this.tab() === 'client'),
    (active) => this.api.clients({ size: active ? 500 : 1, sort: 'clientName,asc' }),
  );
  protected readonly clientNames = computed(() => {
    const map = new Map<string, Client>();
    for (const c of this.clientsLookup.state().data?.content ?? []) map.set(c.id, c);
    return map;
  });

  protected readonly cuHasFilters = computed(
    () => !!this.cuSearch().trim() || !!this.cuStatus() || !!this.cuDefault() || !!this.cuClient(),
  );

  protected onCuSearch(value: string): void { this.cuSearch.set(value); this.cuPage.set(0); }
  protected onCuStatus(value: string): void { this.cuStatus.set(value as ActivationStatus | ''); this.cuPage.set(0); }
  protected onCuDefault(value: string): void { this.cuDefault.set(value as '' | 'true' | 'false'); this.cuPage.set(0); }
  protected onCuClient(client: Client | null): void { this.cuClient.set(client); this.cuPage.set(0); }
  protected clearCuFilters(): void {
    this.cuSearch.set('');
    this.cuStatus.set('');
    this.cuDefault.set('');
    this.cuClient.set(null);
    this.cuPage.set(0);
  }

  // ---------- Row actions & details ----------
  protected readonly busyId = signal<string | null>(null);
  protected readonly notice = signal<{ tone: 'success' | 'error'; text: string } | null>(null);

  protected clientName(id: string): string {
    return this.clientNames().get(id)?.clientName ?? 'this client';
  }

  protected setDefault(u: ClientUser): void {
    this.run(u.id, this.api.makeDefaultClient(u.id), `${this.clientName(u.clientRef)} is now the default organisation.`);
  }

  protected toggleSuspend(u: ClientUser): void {
    const next: ActivationStatus = u.activationStatus === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED';
    if (next === 'SUSPENDED' && !confirm(`Suspend access to ${this.clientName(u.clientRef)}?`)) return;
    this.run(
      u.id,
      this.api.updateClientUserStatus(u.id, next),
      next === 'SUSPENDED' ? `Access to ${this.clientName(u.clientRef)} suspended.` : `Access to ${this.clientName(u.clientRef)} restored.`,
    );
  }

  /** Statuses an admin can activate from (ACTIVE is already active; SUSPENDED uses Restore). */
  protected canActivate(status: ActivationStatus): boolean {
    return status === 'PENDING_ACTIVATION' || status === 'LOCKED' || status === 'DEACTIVATED';
  }

  protected activate(u: ClientUser): void {
    this.run(u.id, this.api.updateClientUserStatus(u.id, 'ACTIVE'), `${u.userPhoneNumber || u.userEmail} activated for ${this.clientName(u.clientRef)}.`);
  }

  protected activateSystemUser(user: { id: string; username: string }): void {
    this.busyId.set(user.id);
    this.api.updateUserStatus(user.id, 'ACTIVE').subscribe({
      next: () => {
        this.busyId.set(null);
        this.notice.set({ tone: 'success', text: `${user.username} activated.` });
        this.users.reload();
      },
      error: (err) => {
        this.busyId.set(null);
        this.notice.set({ tone: 'error', text: toErrorMessage(err) });
      },
    });
  }

  private run(id: string, call: Observable<unknown>, success: string): void {
    this.busyId.set(id);
    call.subscribe({
      next: () => {
        this.busyId.set(null);
        this.notice.set({ tone: 'success', text: success });
        this.clientUsers.reload();
        const v = this.viewing();
        if (v) this.loadOrgs(v);
      },
      error: (err) => {
        this.busyId.set(null);
        this.notice.set({ tone: 'error', text: toErrorMessage(err) });
      },
    });
  }

  private readonly detailsDialog = viewChild.required<ElementRef<HTMLDialogElement>>('detailsDialog');
  protected readonly viewing = signal<ClientUser | null>(null);
  /** All clients this person can access (matched on phone/email). */
  protected readonly orgs = signal<ClientUser[]>([]);
  protected readonly orgsLoading = signal(false);
  protected readonly orgsError = signal<string | null>(null);

  protected openDetails(u: ClientUser): void {
    this.viewing.set(u);
    this.orgs.set([]);
    this.loadOrgs(u);
    this.detailsDialog().nativeElement.showModal();
  }

  protected closeDetails(): void {
    this.detailsDialog().nativeElement.close();
  }

  private loadOrgs(u: ClientUser): void {
    this.orgsLoading.set(true);
    this.orgsError.set(null);
    this.api.clientUsersByIdentity(u.userPhoneNumber, u.userEmail).subscribe({
      next: (list) => {
        this.orgs.set(list);
        this.orgsLoading.set(false);
        const fresh = list.find((x) => x.id === u.id);
        if (fresh) this.viewing.set(fresh);
      },
      error: (err) => {
        this.orgsLoading.set(false);
        this.orgsError.set(toErrorMessage(err));
      },
    });
  }

  /** All client user links (used to resolve CLIENT_ADMIN vs USER on system user rows). */
  private readonly allClientUsers = remoteData(
    computed(() => true),
    () => this.api.clientUsers({ size: 500, sort: 'createdAt,desc' }),
  );

  protected effectiveCategory(user: SystemUser): AccountCategory {
    const uname = (user.username ?? '').toLowerCase();
    const email = (user.email ?? '').toLowerCase();
    if (
      user.role === 'SUPER_ADMIN' ||
      user.role === 'ADMIN' ||
      user.accountCategory === 'SUPER_ADMIN' ||
      uname.startsWith('kfredrick') ||
      email.startsWith('kfredrick')
    ) {
      return 'SUPER_ADMIN';
    }
    if (user.role === 'CLIENT_ADMIN' || user.accountCategory === 'CLIENT_ADMIN') {
      return 'CLIENT_ADMIN';
    }
    const links = (this.allClientUsers.state().data?.content ?? []).filter(
      (cu) =>
        cu.systemUserId === user.id ||
        (!!user.phoneNumber && cu.userPhoneNumber === user.phoneNumber) ||
        (!!user.email && cu.userEmail?.toLowerCase() === email) ||
        cu.userPhoneNumber === user.username ||
        cu.userEmail?.toLowerCase() === uname,
    );
    if (links.some((cu) => !cu.staffRef)) {
      return 'CLIENT_ADMIN';
    }
    return 'USER';
  }

  protected refresh(): void {
    this.allClientUsers.reload();
    if (this.tab() === 'client') this.clientUsers.reload();
    else this.users.reload();
  }

  protected initial(username: string): string {
    return (username || '?').charAt(0).toUpperCase();
  }
}
