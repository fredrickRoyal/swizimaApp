import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { SwizimaApi } from '../../core/api/swizima-api';
import { debounced, remoteData } from '../../core/data/remote-data';
import { ClientStatus } from '../../core/models/api.models';
import { EmptyState } from '../../shared/empty-state/empty-state';
import { Icon, IconName } from '../../shared/icon/icon';
import { PageHeader } from '../../shared/page-header/page-header';
import { Pagination } from '../../shared/pagination/pagination';
import { StatusBadge, humanize } from '../../shared/status-badge/status-badge';
import type { ClientCreatedState } from './client-form/client-form';

const PAGE_SIZE = 10;
const MENU_WIDTH = 200;

interface ClientAction { key: string; label: string; icon: IconName; path: string }

@Component({
  selector: 'app-clients',
  imports: [DatePipe, RouterLink, EmptyState, Icon, PageHeader, Pagination, StatusBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './clients.html',
  host: {
    '(document:click)': 'closeMenu()',
    '(document:keydown.escape)': 'closeMenu()',
    '(window:resize)': 'closeMenu()',
    '(window:scroll)': 'closeMenu()',
  },
  styles: `
    .col-actions { width: 1%; text-align: right; white-space: nowrap; }
    .col-actions .btn-icon { width: 32px; height: 32px; }
    .action-menu {
      position: fixed; z-index: 1000; width: 200px; padding: 0.35rem;
      background: var(--bg-elevated); border: 1px solid var(--border);
      border-radius: 10px; box-shadow: 0 12px 32px rgba(0,0,0,.18);
      animation: menu-in 140ms ease-out;
    }
    .action-item {
      display: flex; align-items: center; gap: 0.6rem; padding: 0.5rem 0.65rem;
      border-radius: 6px; color: inherit; text-decoration: none; font-size: 0.9rem;
      app-icon { width: 16px; height: 16px; opacity: 0.75; }
      &:hover, &:focus-visible { background: color-mix(in srgb, currentColor 8%, transparent); outline: none; }
    }
    @keyframes menu-in { from { opacity: 0; transform: translateY(-4px); } }
    .created-notice {
      margin-bottom: 1.25rem;
      code { font-family: var(--font-mono); font-size: 0.85em; }
      .btn-icon { margin-left: auto; width: 32px; height: 32px; color: inherit; }
      app-icon { flex: none; }
    }
    .row-new { animation: row-flash 2.4s var(--ease); }
    @keyframes row-flash {
      0%, 40% { background: color-mix(in srgb, var(--success) 14%, transparent); }
    }
  `,
})
export class Clients {
  private readonly api = inject(SwizimaApi);

  protected readonly statuses: ClientStatus[] = ['PENDING', 'ACTIVE', 'INACTIVE', 'SUSPENDED'];
  protected readonly humanize = humanize;
  protected readonly pageSize = PAGE_SIZE;

  protected readonly search = signal('');
  protected readonly status = signal<ClientStatus | ''>('');
  protected readonly page = signal(0);
  private readonly searchTerm = debounced(this.search, 350);

  protected readonly clients = remoteData(
    computed(() => ({
      search: this.searchTerm().trim(),
      status: this.status(),
      page: this.page(),
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
    })),
    (query) => this.api.clients(query),
  );

  protected readonly hasFilters = computed(() => !!this.search().trim() || !!this.status());

  protected onSearch(value: string): void {
    this.search.set(value);
    this.page.set(0);
  }

  protected onStatus(value: string): void {
    this.status.set(value as ClientStatus | '');
    this.page.set(0);
  }

  protected clearFilters(): void {
    this.search.set('');
    this.status.set('');
    this.page.set(0);
  }

  /** Set when arriving from /clients/new after a successful registration (newest-first list shows it on top). */
  protected readonly justCreated = signal<ClientCreatedState | null>(
    consumeCreatedState(inject(Router).currentNavigation()?.extras.state),
  );

  protected readonly actions: ClientAction[] = [
    { key: 'details', label: 'View details', icon: 'eye', path: 'details' },
    { key: 'dashboard', label: 'Dashboard', icon: 'dashboard', path: 'dashboard' },
    { key: 'services', label: 'Service catalog', icon: 'catalog', path: 'services' },
    { key: 'staff', label: 'Staff', icon: 'users', path: 'staff' },
    { key: 'transactions', label: 'Transactions', icon: 'transfer', path: 'transactions' },
    { key: 'billing', label: 'Billing', icon: 'receipt', path: 'billing' },
  ];

  protected readonly openMenu = signal<{ id: string; top: number; left: number } | null>(null);

  protected toggleMenu(id: string, event: MouseEvent): void {
    event.stopPropagation();
    if (this.openMenu()?.id === id) return this.closeMenu();
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const menuHeight = this.actions.length * 38 + 12;
    const below = rect.bottom + 4;
    const top = below + menuHeight > window.innerHeight ? Math.max(8, rect.top - menuHeight - 4) : below;
    const left = Math.max(8, rect.right - MENU_WIDTH);
    this.openMenu.set({ id, top, left });
  }

  protected closeMenu(): void {
    if (this.openMenu()) this.openMenu.set(null);
  }

  protected location(city?: string, country?: string): string {
    return [city, country].filter(Boolean).join(', ') || '—';
  }
}

/** Reads the state passed by router.navigate(['/clients'], { state }) once, then removes it from history. */
function consumeCreatedState(navState: unknown): ClientCreatedState | null {
  const state = (navState ?? history.state) as Partial<ClientCreatedState> | null;
  if (!state?.createdClientId) return null;
  const { createdClientId, createdClientName = '', createdClientCode = '' } = state;
  // Let the router finish writing history first, then strip the one-time fields (reload won't re-show it).
  setTimeout(() => {
    const { createdClientId: _id, createdClientName: _n, createdClientCode: _c, ...rest } = history.state ?? {};
    history.replaceState(rest, '');
  });
  return { createdClientId, createdClientName, createdClientCode };
}
