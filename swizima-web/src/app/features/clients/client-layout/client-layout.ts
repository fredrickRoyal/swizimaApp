import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SwizimaApi } from '../../../core/api/swizima-api';
import { AuthService } from '../../../core/auth/auth.service';
import { remoteData } from '../../../core/data/remote-data';
import { EmptyState } from '../../../shared/empty-state/empty-state';
import { Icon, IconName } from '../../../shared/icon/icon';
import { StatusBadge, humanize } from '../../../shared/status-badge/status-badge';

interface Tab { path: string; label: string; icon: IconName }

/**
 * Parent route for /clients/:id/* (Admin) and /portal/* (Client Portal module).
 * Loads the client once (GET /api/v1/clients/{id}) and shares it with child pages.
 */
@Component({
  selector: 'app-client-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, EmptyState, Icon, StatusBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-layout.html',
  styleUrl: './client-layout.css',
})
export class ClientLayout {
  private readonly api = inject(SwizimaApi);
  private readonly auth = inject(AuthService);

  /** Bound from the `:id` route param when under /clients/:id; omitted under /portal. */
  readonly routeId = input<string | undefined>(undefined, { alias: 'id' });

  /** Effective client ID (from route param or the signed-in client user's defaultClientId). */
  readonly id = computed(() => this.routeId() || this.auth.clientId() || '');

  /** True when the signed-in user is a platform administrator (SUPER_ADMIN). */
  readonly isAdmin = this.auth.isAdmin;

  /** True when rendered inside the client-only login module (/portal). */
  readonly isClientPortal = computed(() => !this.routeId() || this.auth.isClientPortalUser());

  readonly client = remoteData(
    computed(() => this.id()),
    (id) => this.api.client(id),
  );

  protected readonly humanize = humanize;
  private readonly allTabs: Tab[] = [
    { path: 'details', label: 'View details', icon: 'eye' },
    { path: 'dashboard', label: 'Dashboard', icon: 'dashboard' },
    { path: 'services', label: 'Service catalog', icon: 'catalog' },
    { path: 'staff', label: 'Staff', icon: 'users' },
    { path: 'transactions', label: 'Transactions', icon: 'transfer' },
    { path: 'reports', label: 'Reports', icon: 'trendingUp' },
    { path: 'billing', label: 'Billing', icon: 'receipt' },
  ];

  protected readonly tabs = computed(() =>
    this.isClientPortal()
      ? this.allTabs.filter((t) => t.path !== 'reports')
      : this.allTabs,
  );

  protected initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((w) => w[0]!.toUpperCase())
      .join('');
  }
}
