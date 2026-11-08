import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { SwizimaApi } from '../../../core/api/swizima-api';
import { debounced, remoteData } from '../../../core/data/remote-data';
import { toErrorMessage } from '../../../core/http/api-error';
import {
  AccountCategory,
  ActivationStatus,
  ClientStaff,
  ClientStaffRequest,
  ClientUser,
  Gender,
  OtpChannel,
} from '../../../core/models/api.models';
import { EmptyState } from '../../../shared/empty-state/empty-state';
import { Icon } from '../../../shared/icon/icon';
import { Pagination } from '../../../shared/pagination/pagination';
import { PhoneInput } from '../../../shared/phone-input/phone-input';
import { StatusBadge, humanize } from '../../../shared/status-badge/status-badge';
import { ClientLayout } from '../client-layout/client-layout';

const PAGE_SIZE = 10;
const PHONE = /^\+?[0-9 ()-]{7,30}$/;
const NIN = /^[A-Za-z0-9]{8,20}$/;
const POSITIONS = ['Owner', 'Manager', 'Operations'] as const;

/** "mary-jane o'neil" -> "Mary-Jane O'Neil" (rest of each word left as typed). */
export function capitalizeName(value: string): string {
  return value.replace(/(^|[\s'-])(\p{L})/gu, (_, sep: string, ch: string) => sep + ch.toLocaleUpperCase());
}

/** /clients/:id/staff & /portal/staff — staff directory + client user management. */
@Component({
  selector: 'app-client-staff',
  imports: [DatePipe, ReactiveFormsModule, EmptyState, Icon, Pagination, PhoneInput, StatusBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-staff.html',
  styleUrl: './client-staff.css',
})
export class ClientStaffPage {
  private readonly api = inject(SwizimaApi);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly layout = inject(ClientLayout);

  protected readonly tab = signal<'staff' | 'users'>('staff');
  protected readonly genders: Gender[] = ['MALE', 'FEMALE', 'OTHER'];
  protected readonly statuses: ActivationStatus[] = ['PENDING_ACTIVATION', 'ACTIVE', 'SUSPENDED', 'LOCKED', 'DEACTIVATED'];
  protected readonly humanize = humanize;
  protected readonly pageSize = PAGE_SIZE;
  /** Default dial code for phone fields. */
  protected readonly clientCountry = computed(() => this.layout.client.state().data?.country ?? 'Uganda');

  // ---------- Staff List ----------
  protected readonly search = signal('');
  protected readonly gender = signal<Gender | ''>('');
  protected readonly page = signal(0);
  private readonly searchTerm = debounced(this.search, 350);

  protected readonly staff = remoteData(
    computed(() => ({
      clientId: this.layout.id(),
      query: {
        search: this.searchTerm().trim(),
        gender: this.gender(),
        page: this.page(),
        size: PAGE_SIZE,
        sort: 'surname,asc',
      },
    })),
    ({ clientId, query }) => this.api.staff(clientId, query),
  );

  /** Full staff list for this client (used to resolve staff names on the User accounts tab & picker). */
  protected readonly allStaff = remoteData(
    computed(() => this.layout.id()),
    (clientId) => this.api.staff(clientId, { size: 500, sort: 'surname,asc' }),
  );
  protected readonly staffById = computed(() => {
    const map = new Map<string, ClientStaff>();
    for (const s of this.allStaff.state().data?.content ?? []) map.set(s.id, s);
    return map;
  });

  protected readonly hasFilters = computed(() => !!this.search().trim() || !!this.gender());

  protected onSearch(value: string): void { this.search.set(value); this.page.set(0); }
  protected onGender(value: string): void { this.gender.set(value as Gender | ''); this.page.set(0); }
  protected clearFilters(): void { this.search.set(''); this.gender.set(''); this.page.set(0); }

  protected readonly busyId = signal<string | null>(null);
  protected readonly notice = signal<{ tone: 'success' | 'error'; text: string } | null>(null);

  protected remove(member: ClientStaff): void {
    if (!confirm(`Remove ${member.fullName} from staff?`)) return;
    this.busyId.set(member.id);
    this.api.deleteStaff(this.layout.id(), member.id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.notice.set({ tone: 'success', text: `${member.fullName} was removed.` });
        this.staff.reload();
        this.allStaff.reload();
      },
      error: (err) => {
        this.busyId.set(null);
        this.notice.set({ tone: 'error', text: toErrorMessage(err) });
      },
    });
  }

  protected initials(m: ClientStaff): string {
    return `${m.givenName?.[0] ?? ''}${m.surname?.[0] ?? ''}`.toUpperCase();
  }

  protected userInitial(u: ClientUser): string {
    const staff = u.staffRef ? this.staffById().get(u.staffRef) : undefined;
    if (staff) return this.initials(staff);
    return (u.userEmail || u.userPhoneNumber || '?').charAt(0).toUpperCase();
  }

  // ---------- Create / edit staff dialog ----------
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  protected readonly editing = signal<ClientStaff | null>(null);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);

  /** Supported positions, plus the member's current one if it predates the list. */
  protected readonly positionOptions = computed(() => {
    const own = this.editing()?.position;
    return own && !(POSITIONS as readonly string[]).includes(own) ? [...POSITIONS, own] : [...POSITIONS];
  });

  protected readonly form = this.fb.group({
    surname: ['', [Validators.required, Validators.maxLength(100)]],
    givenName: ['', [Validators.required, Validators.maxLength(150)]],
    gender: this.fb.control<Gender | ''>(''),
    phoneNumber: ['', Validators.pattern(PHONE)],
    alternativePhone: ['', Validators.pattern(PHONE)],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    position: ['', Validators.maxLength(100)],
    nin: ['', Validators.pattern(NIN)],
    summary: [''],
    createAccount: [true],
  });

  protected openCreate(): void {
    this.editing.set(null);
    this.form.reset({
      surname: '',
      givenName: '',
      gender: '',
      phoneNumber: '',
      alternativePhone: '',
      email: '',
      position: '',
      nin: '',
      summary: '',
      createAccount: true,
    });
    this.openDialog();
  }

  protected openEdit(m: ClientStaff): void {
    this.editing.set(m);
    this.form.reset({
      surname: m.surname,
      givenName: m.givenName,
      gender: m.gender ?? '',
      phoneNumber: m.phoneNumber ?? '',
      alternativePhone: m.alternativePhone ?? '',
      email: m.email ?? '',
      position: m.position ?? '',
      nin: m.nin ?? '',
      summary: m.summary ?? '',
      createAccount: false,
    });
    this.openDialog();
  }

  private openDialog(): void {
    this.formError.set(null);
    this.dialog().nativeElement.showModal();
  }

  protected closeDialog(): void {
    if (this.saving()) return;
    this.dialog().nativeElement.close();
  }

  protected capitalize(name: 'surname' | 'givenName', event: Event): void {
    const el = event.target as HTMLInputElement;
    const next = capitalizeName(el.value);
    if (next === el.value) return;
    const caret = el.selectionStart;
    el.value = next;
    this.form.controls[name].setValue(next, { emitEvent: false });
    if (caret !== null) el.setSelectionRange(caret, caret);
  }

  protected invalid(name: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[name];
    return c.invalid && (c.touched || c.dirty);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const opt = (s: string) => s.trim() || undefined;
    const request: ClientStaffRequest = {
      surname: capitalizeName(v.surname.trim()),
      givenName: capitalizeName(v.givenName.trim()),
      gender: v.gender || undefined,
      phoneNumber: opt(v.phoneNumber),
      alternativePhone: opt(v.alternativePhone),
      email: opt(v.email),
      position: opt(v.position),
      nin: opt(v.nin)?.toUpperCase(),
      summary: opt(v.summary),
    };
    const current = this.editing();
    const clientId = this.layout.id();
    const call = current
      ? this.api.updateStaff(clientId, current.id, request)
      : this.api.createStaff(clientId, request);

    this.saving.set(true);
    this.formError.set(null);
    call.subscribe({
      next: (saved) => {
        const shouldAssign = !current && v.createAccount && (!!saved.phoneNumber || !!saved.email);
        if (!shouldAssign) {
          this.saving.set(false);
          this.dialog().nativeElement.close();
          this.notice.set({ tone: 'success', text: `${saved.fullName} was ${current ? 'updated' : 'added'}.` });
          if (!current) this.page.set(0);
          this.staff.reload();
          this.allStaff.reload();
          return;
        }
        const phone = this.compactPhone(saved.phoneNumber);
        const email = saved.email || undefined;
        const username = phone || email!;
        this.api
          .assignAccount({
            clientRef: clientId,
            staffRef: saved.id,
            username,
            phoneNumber: phone,
            email,
            preferredOtpChannel: email ? 'EMAIL' : 'SMS',
            isDefaultClient: true,
            sendCredentials: true,
            organisationName: this.layout.client.state().data?.clientName,
          })
          .subscribe({
            next: (res) => {
              this.saving.set(false);
              this.dialog().nativeElement.close();
              this.issued.set({
                username: res.user.username,
                password: res.temporaryPassword,
                created: res.userCreated,
                sentTo: res.credentialsSentTo,
                note: res.deliveryNote,
              });
              this.notice.set({
                tone: 'success',
                text: `${saved.fullName} was added and assigned sign-in account (${res.user.username}).`,
              });
              this.page.set(0);
              this.staff.reload();
              this.allStaff.reload();
              this.accounts.reload();
            },
            error: (err) => {
              this.saving.set(false);
              this.dialog().nativeElement.close();
              this.notice.set({
                tone: 'error',
                text: `${saved.fullName} was added, but account creation failed: ${toErrorMessage(err)}`,
              });
              this.staff.reload();
              this.allStaff.reload();
            },
          });
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(toErrorMessage(err));
      },
    });
  }

  // ---------- User accounts (usersapi client users) ----------
  /** Client users of this client, keyed by staff id, so rows can show who has an account. */
  protected readonly accounts = remoteData(
    computed(() => this.layout.id()),
    (clientRef) => this.api.clientUsers({ clientRef, size: 500, sort: 'createdAt,desc' }),
  );
  protected readonly accountByStaff = computed(() => {
    const map = new Map<string, ClientUser>();
    for (const u of this.accounts.state().data?.content ?? []) if (u.staffRef) map.set(u.staffRef, u);
    return map;
  });

  // ---------- User management tab filters ----------
  protected readonly userSearch = signal('');
  protected readonly userStatus = signal<ActivationStatus | ''>('');
  protected readonly userRoleFilter = signal<'' | 'CLIENT_ADMIN' | 'USER'>('');

  protected readonly filteredAccounts = computed(() => {
    const list = this.accounts.state().data?.content ?? [];
    const term = this.userSearch().trim().toLowerCase();
    const status = this.userStatus();
    const roleFilter = this.userRoleFilter();
    return list.filter((u) => {
      if (status && u.activationStatus !== status) return false;
      const role = this.userCategory(u);
      if (roleFilter && role !== roleFilter) return false;
      if (!term) return true;
      const staff = u.staffRef ? this.staffById().get(u.staffRef) : undefined;
      const hay = [
        u.userPhoneNumber,
        u.userEmail,
        staff?.fullName,
        staff?.position,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return hay.includes(term);
    });
  });

  protected readonly hasUserFilters = computed(
    () => !!this.userSearch().trim() || !!this.userStatus() || !!this.userRoleFilter(),
  );

  protected clearUserFilters(): void {
    this.userSearch.set('');
    this.userStatus.set('');
    this.userRoleFilter.set('');
  }

  protected userCategory(u: ClientUser): AccountCategory {
    if (u.accountCategory) return u.accountCategory;
    return u.staffRef ? 'USER' : 'CLIENT_ADMIN';
  }

  protected canActivate(status: ActivationStatus): boolean {
    return status === 'PENDING_ACTIVATION' || status === 'LOCKED' || status === 'DEACTIVATED';
  }

  protected activateAccount(u: ClientUser): void {
    this.busyId.set(u.id);
    this.api.updateClientUserStatus(u.id, 'ACTIVE').subscribe({
      next: () => {
        this.busyId.set(null);
        this.notice.set({ tone: 'success', text: `Account ${u.userPhoneNumber || u.userEmail} activated.` });
        this.accounts.reload();
      },
      error: (err) => {
        this.busyId.set(null);
        this.notice.set({ tone: 'error', text: toErrorMessage(err) });
      },
    });
  }

  protected resetPasswordForAccount(u: ClientUser): void {
    if (!u.systemUserId) return;
    const label = u.userPhoneNumber || u.userEmail || 'this user';
    if (!confirm(`Generate a new temporary password for ${label}?`)) return;
    this.busyId.set(u.id);
    this.api.resetUserPassword(u.systemUserId).subscribe({
      next: (res) => {
        this.busyId.set(null);
        this.issued.set({
          username: res.user.username,
          password: res.temporaryPassword,
          created: false,
        });
        this.notice.set({ tone: 'success', text: `Temporary password generated for ${res.user.username}.` });
      },
      error: (err) => {
        this.busyId.set(null);
        this.notice.set({ tone: 'error', text: toErrorMessage(err) });
      },
    });
  }

  // ---------- Add / Assign user account modal (from User accounts tab) ----------
  private readonly addUserDialog = viewChild.required<ElementRef<HTMLDialogElement>>('addUserDialog');
  protected readonly addUserError = signal<string | null>(null);
  protected readonly addUserSaving = signal(false);

  /** Staff members who do not have a sign-in account yet. */
  protected readonly unassignedStaff = computed(() => {
    const linked = this.accountByStaff();
    return (this.allStaff.state().data?.content ?? []).filter(
      (s) => !linked.has(s.id) && (!!s.phoneNumber || !!s.email),
    );
  });

  protected readonly addUserForm = this.fb.group({
    source: this.fb.control<'staff' | 'custom'>('staff'),
    staffId: [''],
    phoneNumber: ['', Validators.pattern(PHONE)],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    role: this.fb.control<'USER' | 'CLIENT_ADMIN'>('USER'),
    otpChannel: this.fb.control<OtpChannel>('EMAIL'),
    isDefaultClient: [true],
    sendCredentials: [true],
  });

  protected openAddUser(): void {
    this.addUserError.set(null);
    const available = this.unassignedStaff();
    const first = available[0];
    this.addUserForm.reset({
      source: first ? 'staff' : 'custom',
      staffId: first?.id ?? '',
      phoneNumber: '',
      email: '',
      role: 'USER',
      otpChannel: 'EMAIL',
      isDefaultClient: true,
      sendCredentials: true,
    });
    this.addUserDialog().nativeElement.showModal();
  }

  protected closeAddUser(): void {
    if (this.addUserSaving()) return;
    this.addUserDialog().nativeElement.close();
  }

  protected submitAddUser(): void {
    const v = this.addUserForm.getRawValue();
    const clientRef = this.layout.id();
    let staffRef: string | undefined;
    let phone: string | undefined;
    let email: string | undefined;

    if (v.source === 'staff') {
      const m = this.staffById().get(v.staffId);
      if (!m) {
        this.addUserError.set('Select a staff member or switch to custom contact details.');
        return;
      }
      staffRef = m.id;
      phone = this.compactPhone(m.phoneNumber);
      email = m.email || undefined;
    } else {
      phone = this.compactPhone(v.phoneNumber.trim());
      email = v.email.trim() || undefined;
    }

    const username = phone || email;
    if (!username) {
      this.addUserError.set('Provide at least a phone number or an email address.');
      return;
    }

    this.addUserSaving.set(true);
    this.addUserError.set(null);
    this.api
      .assignAccount({
        clientRef,
        staffRef,
        username,
        phoneNumber: phone,
        email,
        preferredOtpChannel: v.otpChannel,
        accountCategory: v.source === 'staff' ? 'USER' : v.role,
        isDefaultClient: v.isDefaultClient,
        sendCredentials: v.sendCredentials,
        organisationName: this.layout.client.state().data?.clientName,
      })
      .subscribe({
        next: (res) => {
          this.addUserSaving.set(false);
          this.addUserDialog().nativeElement.close();
          this.issued.set({
            username: res.user.username,
            password: res.temporaryPassword,
            created: res.userCreated,
            sentTo: res.credentialsSentTo,
            note: res.deliveryNote,
          });
          this.notice.set({
            tone: 'success',
            text: res.userCreated
              ? `User account ${res.user.username} created.`
              : `Linked existing account ${res.user.username} to this client.`,
          });
          this.accounts.reload();
        },
        error: (err) => {
          this.addUserSaving.set(false);
          this.addUserError.set(toErrorMessage(err));
        },
      });
  }

  private readonly detailsDialog = viewChild.required<ElementRef<HTMLDialogElement>>('detailsDialog');
  protected readonly viewing = signal<ClientStaff | null>(null);
  protected readonly viewingAccount = computed(() => {
    const m = this.viewing();
    return m ? this.accountByStaff().get(m.id) ?? null : null;
  });
  protected readonly accountBusy = signal(false);
  protected readonly accountError = signal<string | null>(null);
  /** Shown once after a new system user is created with a generated password. */
  protected readonly issued = signal<{ username: string; password?: string; created: boolean; sentTo?: string; note?: string } | null>(null);
  protected readonly copied = signal(false);

  protected readonly assignForm = this.fb.group({
    loginWith: this.fb.control<'phone' | 'email'>('phone'),
    otpChannel: this.fb.control<OtpChannel>('SMS'),
    isDefaultClient: [true],
    sendCredentials: [true],
  });

  protected openDetails(m: ClientStaff): void {
    this.viewing.set(m);
    this.accountError.set(null);
    this.issued.set(null);
    this.copied.set(false);
    const loginWith = m.phoneNumber ? 'phone' : 'email';
    this.assignForm.reset({ loginWith, otpChannel: loginWith === 'phone' ? 'SMS' : 'EMAIL', isDefaultClient: true, sendCredentials: true });
    this.detailsDialog().nativeElement.showModal();
  }

  protected closeDetails(): void {
    if (this.accountBusy()) return;
    this.detailsDialog().nativeElement.close();
  }

  protected editFromDetails(m: ClientStaff): void {
    this.detailsDialog().nativeElement.close();
    this.openEdit(m);
  }

  protected onLoginWith(value: 'phone' | 'email'): void {
    this.assignForm.controls.otpChannel.setValue(value === 'phone' ? 'SMS' : 'EMAIL');
  }

  /** Phone stored as "+256 700123456" -> "+256700123456" for the username. */
  private compactPhone(phone?: string): string | undefined {
    return phone ? phone.replace(/[\s()-]/g, '') : undefined;
  }

  protected assignAccount(): void {
    const m = this.viewing();
    if (!m) return;
    const v = this.assignForm.getRawValue();
    const phone = this.compactPhone(m.phoneNumber);
    const email = m.email || undefined;
    const username = v.loginWith === 'phone' ? phone : email;
    if (!username) {
      this.accountError.set(`This staff member has no ${v.loginWith === 'phone' ? 'phone number' : 'email'}. Edit their details first.`);
      return;
    }
    this.accountBusy.set(true);
    this.accountError.set(null);
    this.api
      .assignAccount({
        clientRef: this.layout.id(),
        staffRef: m.id,
        username,
        phoneNumber: phone,
        email,
        preferredOtpChannel: v.otpChannel,
        isDefaultClient: v.isDefaultClient,
        sendCredentials: v.sendCredentials,
        organisationName: this.layout.client.state().data?.clientName,
      })
      .subscribe({
        next: (res) => {
          this.accountBusy.set(false);
          this.issued.set({
            username: res.user.username,
            password: res.temporaryPassword,
            created: res.userCreated,
            sentTo: res.credentialsSentTo,
            note: res.deliveryNote,
          });
          this.notice.set({
            tone: 'success',
            text: res.userCreated
              ? `Account created for ${m.fullName}.`
              : `${m.fullName} was linked to the existing account ${res.user.username}.`,
          });
          this.accounts.reload();
        },
        error: (err) => {
          this.accountBusy.set(false);
          this.accountError.set(toErrorMessage(err));
        },
      });
  }

  protected makeDefault(account: ClientUser): void {
    this.runAccountAction(this.api.makeDefaultClient(account.id), `${this.layout.client.state().data?.clientName ?? 'This client'} is now the default organisation.`);
  }

  protected toggleAccess(account: ClientUser): void {
    const next = account.activationStatus === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED';
    this.runAccountAction(
      this.api.updateClientUserStatus(account.id, next),
      next === 'SUSPENDED' ? 'Access suspended for this client.' : 'Access restored.',
    );
  }

  protected removeAccess(account: ClientUser): void {
    if (!confirm('Remove this person\'s access to the client? Their sign-in account is kept.')) return;
    this.runAccountAction(this.api.removeClientUser(account.id), 'Access removed.');
  }

  private runAccountAction(call: Observable<unknown>, success: string): void {
    this.accountBusy.set(true);
    this.accountError.set(null);
    call.subscribe({
      next: () => {
        this.accountBusy.set(false);
        this.notice.set({ tone: 'success', text: success });
        this.accounts.reload();
      },
      error: (err) => {
        this.accountBusy.set(false);
        this.accountError.set(toErrorMessage(err));
      },
    });
  }

  protected copyPassword(password: string): void {
    navigator.clipboard?.writeText(password).then(() => this.copied.set(true));
  }
}
