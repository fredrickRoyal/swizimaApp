import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { SwizimaApi } from '../../core/api/swizima-api';
import { debounced, remoteData } from '../../core/data/remote-data';
import { ActivationStatus } from '../../core/models/api.models';
import { EmptyState } from '../../shared/empty-state/empty-state';
import { Icon } from '../../shared/icon/icon';
import { PageHeader } from '../../shared/page-header/page-header';
import { Pagination } from '../../shared/pagination/pagination';
import { StatusBadge, humanize } from '../../shared/status-badge/status-badge';

const PAGE_SIZE = 10;

/** Portal users (usersapi). */
@Component({
  selector: 'app-users',
  imports: [DatePipe, EmptyState, Icon, PageHeader, Pagination, StatusBadge],
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
    @media (max-width: 720px) { .avatar { display: none; } }
  `,
})
export class Users {
  private readonly api = inject(SwizimaApi);

  protected readonly statuses: ActivationStatus[] = ['PENDING_ACTIVATION', 'ACTIVE', 'SUSPENDED', 'LOCKED', 'DEACTIVATED'];
  protected readonly humanize = humanize;
  protected readonly pageSize = PAGE_SIZE;

  protected readonly search = signal('');
  protected readonly activationStatus = signal<ActivationStatus | ''>('');
  protected readonly page = signal(0);
  private readonly searchTerm = debounced(this.search, 350);

  protected readonly users = remoteData(
    computed(() => ({
      search: this.searchTerm().trim(),
      activationStatus: this.activationStatus(),
      page: this.page(),
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
    })),
    (query) => this.api.users(query),
  );

  protected readonly hasFilters = computed(() => !!this.search().trim() || !!this.activationStatus());

  protected onSearch(value: string): void {
    this.search.set(value);
    this.page.set(0);
  }

  protected onStatus(value: string): void {
    this.activationStatus.set(value as ActivationStatus | '');
    this.page.set(0);
  }

  protected clearFilters(): void {
    this.search.set('');
    this.activationStatus.set('');
    this.page.set(0);
  }

  protected initial(username: string): string {
    return (username || '?').charAt(0).toUpperCase();
  }
}
