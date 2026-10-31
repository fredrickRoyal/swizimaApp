import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { SwizimaApi } from '../../core/api/swizima-api';
import { debounced, remoteData } from '../../core/data/remote-data';
import { Client, ServiceCatalogStatus, ServiceCategory } from '../../core/models/api.models';
import { ClientPicker } from '../../shared/client-picker/client-picker';
import { EmptyState } from '../../shared/empty-state/empty-state';
import { Icon, IconName } from '../../shared/icon/icon';
import { PageHeader } from '../../shared/page-header/page-header';
import { Pagination } from '../../shared/pagination/pagination';
import { StatusBadge, humanize } from '../../shared/status-badge/status-badge';

const PAGE_SIZE = 10;

export const CATEGORY_ICON: Record<ServiceCategory, IconName> = {
  CAR_PARK: 'parking',
  CAR_WASH: 'car',
  REPAIR: 'wrench',
  MAINTENANCE: 'settings',
  OTHER: 'sparkles',
};

/** Service catalogue: what each client offers (car park, wash, repair, …). */
@Component({
  selector: 'app-services',
  imports: [CurrencyPipe, ClientPicker, EmptyState, Icon, PageHeader, Pagination, StatusBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './services.html',
  styles: `
    .list-card { overflow: visible; }
    app-client-picker { flex: 1 1 240px; max-width: 320px; }
    .service { display: flex; align-items: center; gap: 0.75rem; min-width: 0; }
    .service-icon {
      display: grid;
      place-items: center;
      flex: none;
      width: 36px;
      height: 36px;
      border-radius: 10px;
      color: var(--brand-500);
      background: color-mix(in srgb, var(--brand-500) 12%, transparent);
      app-icon { width: 18px; height: 18px; }
    }
    .price { font-weight: 650; font-variant-numeric: tabular-nums; white-space: nowrap; }
    @media (max-width: 720px) { .service-icon { display: none; } }
  `,
})
export class Services {
  private readonly api = inject(SwizimaApi);

  protected readonly categories = Object.keys(CATEGORY_ICON) as ServiceCategory[];
  protected readonly statuses: ServiceCatalogStatus[] = ['ACTIVE', 'INACTIVE'];
  protected readonly categoryIcon = CATEGORY_ICON;
  protected readonly humanize = humanize;
  protected readonly pageSize = PAGE_SIZE;

  protected readonly search = signal('');
  protected readonly category = signal<ServiceCategory | ''>('');
  protected readonly status = signal<ServiceCatalogStatus | ''>('');
  protected readonly page = signal(0);
  protected readonly client = signal<Client | null>(null);
  private readonly searchTerm = debounced(this.search, 350);

  protected readonly services = remoteData(
    computed(() => ({
      search: this.searchTerm().trim(),
      category: this.category(),
      status: this.status(),
      clientId: this.client()?.id,
      page: this.page(),
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
    })),
    (query) => this.api.serviceCatalogs(query),
  );

  protected readonly hasFilters = computed(
    () => !!this.search().trim() || !!this.category() || !!this.status() || !!this.client(),
  );

  protected onClient(client: Client | null): void {
    this.client.set(client);
    this.page.set(0);
  }

  protected onSearch(value: string): void {
    this.search.set(value);
    this.page.set(0);
  }

  protected onCategory(value: string): void {
    this.category.set(value as ServiceCategory | '');
    this.page.set(0);
  }

  protected onStatus(value: string): void {
    this.status.set(value as ServiceCatalogStatus | '');
    this.page.set(0);
  }

  protected clearFilters(): void {
    this.search.set('');
    this.category.set('');
    this.status.set('');
    this.client.set(null);
    this.page.set(0);
  }

  protected duration(minutes?: number): string {
    if (!minutes) return '—';
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return h ? `${h}h${m ? ` ${m}m` : ''}` : `${m} min`;
  }
}
