import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';
import { SwizimaApi } from '../../core/api/swizima-api';
import { AuthService } from '../../core/auth/auth.service';
import { remoteData } from '../../core/data/remote-data';
import { PageResponse, ServiceCategory } from '../../core/models/api.models';
import { Icon, IconName } from '../../shared/icon/icon';
import { PageHeader } from '../../shared/page-header/page-header';

interface Kpi {
  id: string;
  label: string;
  icon: IconName;
  value: number | null;
  hint: string;
  link: string;
  tone: 'brand' | 'teal' | 'violet' | 'amber';
}

const CATEGORIES: { value: ServiceCategory; label: string; icon: IconName }[] = [
  { value: 'CAR_PARK', label: 'Car park', icon: 'parking' },
  { value: 'CAR_WASH', label: 'Car wash', icon: 'car' },
  { value: 'REPAIR', label: 'Repair', icon: 'wrench' },
  { value: 'MAINTENANCE', label: 'Maintenance', icon: 'settings' },
  { value: 'OTHER', label: 'Other', icon: 'sparkles' },
];

/** Totals only need `totalElements`, so each count is a size=1 request. */
@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DecimalPipe, Icon, PageHeader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly api = inject(SwizimaApi);
  private readonly auth = inject(AuthService);

  protected readonly firstName = computed(() => {
    const name = this.auth.displayName().split('@')[0];
    return name ? name.charAt(0).toUpperCase() + name.slice(1) : 'there';
  });
  protected readonly mustChangePassword = computed(() => this.auth.session()?.mustChangePassword ?? false);

  /** A failing count (e.g. service down) shows "—" instead of breaking the whole dashboard. */
  private countOf(source: Observable<PageResponse<unknown>>): Observable<number | null> {
    return source.pipe(
      map((page): number | null => page.totalElements),
      catchError(() => of(null)),
    );
  }

  protected readonly stats = remoteData(computed(() => 0), () =>
    forkJoin({
      clients: this.countOf(this.api.clients({ size: 1 })),
      activeClients: this.countOf(this.api.clients({ size: 1, status: 'ACTIVE' })),
      services: this.countOf(this.api.serviceCatalogs({ size: 1 })),
      users: this.countOf(this.api.users({ size: 1 })),
      categories: forkJoin(
        CATEGORIES.map((c) => this.countOf(this.api.serviceCatalogs({ size: 1, category: c.value }))),
      ),
    }),
  );

  protected readonly offline = computed(() => {
    const data = this.stats.state().data;
    return !!data && data.clients === null && data.services === null && data.users === null;
  });

  protected readonly kpis = computed<Kpi[]>(() => {
    const d = this.stats.state().data;
    return [
      { id: 'kpi-clients', label: 'Total clients', icon: 'clients', value: d?.clients ?? null,
        hint: d?.activeClients != null ? `${d.activeClients} active` : 'Businesses on Swizima', link: '/clients', tone: 'brand' },
      { id: 'kpi-services', label: 'Services offered', icon: 'car', value: d?.services ?? null,
        hint: 'Across all clients', link: '/services', tone: 'teal' },
      { id: 'kpi-users', label: 'System users', icon: 'users', value: d?.users ?? null,
        hint: 'Admins and staff', link: '/users', tone: 'violet' },
      { id: 'kpi-payments', label: 'Payments today', icon: 'payments', value: null,
        hint: 'Module coming soon', link: '/payments', tone: 'amber' },
    ];
  });

  protected readonly categories = computed(() => {
    const counts = this.stats.state().data?.categories ?? [];
    const total = counts.reduce<number>((sum, n) => sum + (n ?? 0), 0);
    return CATEGORIES.map((c, i) => {
      const count = counts[i] ?? 0;
      return { ...c, count, percent: total ? Math.round((count / total) * 100) : 0 };
    });
  });

  protected readonly quickLinks: { id: string; label: string; description: string; icon: IconName; link: string }[] = [
    { id: 'quick-clients', label: 'Manage clients', description: 'Browse and search registered businesses', icon: 'clients', link: '/clients' },
    { id: 'quick-services', label: 'Service catalogue', description: 'Car parks, washes, repairs and more', icon: 'car', link: '/services' },
    { id: 'quick-users', label: 'User accounts', description: 'Who can access the portal', icon: 'users', link: '/users' },
    { id: 'quick-config', label: 'Configurations', description: 'System settings and preferences', icon: 'settings', link: '/configurations' },
  ];
}
