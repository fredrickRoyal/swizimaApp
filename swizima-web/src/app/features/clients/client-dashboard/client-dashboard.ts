import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SwizimaApi } from '../../../core/api/swizima-api';
import { remoteData } from '../../../core/data/remote-data';
import { ServiceCategory } from '../../../core/models/api.models';
import { EmptyState } from '../../../shared/empty-state/empty-state';
import { Icon } from '../../../shared/icon/icon';
import { StatusBadge, humanize } from '../../../shared/status-badge/status-badge';
import { CATEGORY_ICON } from '../../services/services';
import { ClientLayout } from '../client-layout/client-layout';

/** Upper bound of catalog items pulled to compute the overview (clients rarely exceed this). */
const OVERVIEW_SIZE = 200;

/** /clients/:id/dashboard — at-a-glance overview of a client's catalog. */
@Component({
  selector: 'app-client-dashboard',
  imports: [CurrencyPipe, DatePipe, RouterLink, EmptyState, Icon, StatusBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-dashboard.html',
  styleUrl: './client-dashboard.css',
})
export class ClientDashboard {
  private readonly api = inject(SwizimaApi);
  protected readonly layout = inject(ClientLayout);
  protected readonly humanize = humanize;
  protected readonly categoryIcon = CATEGORY_ICON;

  protected readonly services = remoteData(
    computed(() => this.layout.id()),
    (clientId) => this.api.serviceCatalogs({ clientId, size: OVERVIEW_SIZE, sort: 'createdAt,desc' }),
  );

  protected readonly stats = computed(() => {
    const page = this.services.state().data;
    if (!page) return null;
    const items = page.content;
    const active = items.filter((s) => s.status === 'ACTIVE');
    const byCategory = new Map<ServiceCategory, number>();
    for (const s of items) byCategory.set(s.category, (byCategory.get(s.category) ?? 0) + 1);
    const max = Math.max(1, ...byCategory.values());

    // Prices can be in different currencies — report the dominant one.
    const currencies = new Map<string, number[]>();
    for (const s of active) currencies.set(s.currency, [...(currencies.get(s.currency) ?? []), s.price]);
    const [currency, prices] = [...currencies.entries()].sort((a, b) => b[1].length - a[1].length)[0] ?? ['', []];
    const avg = prices.length ? prices.reduce((a, b) => a + b, 0) / prices.length : null;

    return {
      total: page.totalElements,
      active: active.length,
      inactive: items.length - active.length,
      currency,
      avgPrice: avg,
      minPrice: prices.length ? Math.min(...prices) : null,
      maxPrice: prices.length ? Math.max(...prices) : null,
      categories: [...byCategory.entries()]
        .sort((a, b) => b[1] - a[1])
        .map(([category, count]) => ({ category, count, pct: (count / max) * 100 })),
      recent: items.slice(0, 5),
    };
  });
}
