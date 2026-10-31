import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SwizimaApi } from '../../../core/api/swizima-api';
import { remoteData } from '../../../core/data/remote-data';
import { EmptyState } from '../../../shared/empty-state/empty-state';
import { Icon, IconName } from '../../../shared/icon/icon';
import { StatusBadge, humanize } from '../../../shared/status-badge/status-badge';

interface Tab { path: string; label: string; icon: IconName }

/**
 * Parent route for /clients/:id/*. Loads the client once (GET /api/v1/clients/{id})
 * and shares it with child pages, which inject this component.
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

  /** Bound from the `:id` route param (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly client = remoteData(
    computed(() => this.id()),
    (id) => this.api.client(id),
  );

  protected readonly humanize = humanize;
  protected readonly tabs: Tab[] = [
    { path: 'details', label: 'Details', icon: 'info' },
    { path: 'dashboard', label: 'Dashboard', icon: 'dashboard' },
    { path: 'services', label: 'Service catalog', icon: 'catalog' },
    { path: 'staff', label: 'Staff', icon: 'users' },
    { path: 'transactions', label: 'Transactions', icon: 'transfer' },
  ];

  protected initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((w) => w[0]!.toUpperCase())
      .join('');
  }
}
