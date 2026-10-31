import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { SwizimaApi } from '../../core/api/swizima-api';
import { debounced, remoteData } from '../../core/data/remote-data';
import { ClientStatus } from '../../core/models/api.models';
import { EmptyState } from '../../shared/empty-state/empty-state';
import { Icon } from '../../shared/icon/icon';
import { PageHeader } from '../../shared/page-header/page-header';
import { Pagination } from '../../shared/pagination/pagination';
import { StatusBadge, humanize } from '../../shared/status-badge/status-badge';
import type { ClientCreatedState } from './client-form/client-form';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-clients',
  imports: [DatePipe, RouterLink, EmptyState, Icon, PageHeader, Pagination, StatusBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './clients.html',
  styles: `
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
