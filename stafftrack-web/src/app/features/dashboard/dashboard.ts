import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ENGAGEMENT_TYPE_LABELS,
  EngagementType,
  OOO_STATUS_LABELS,
  OutOfOfficeNotification,
  OutOfOfficeStatus,
} from '../../core/models/stafftrack.models';
import { AuthService } from '../../core/services/auth.service';
import { StaffTrackStore } from '../../core/services/stafftrack.store';
import { Icon } from '../../shared/icon/icon';
import { Pagination } from '../../shared/pagination/pagination';

@Component({
  selector: 'app-dashboard',
  imports: [FormsModule, RouterLink, Icon, Pagination],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .officer-cell {
      display: grid;
      gap: 0.15rem;
    }
    .officer-cell small {
      color: var(--text-muted);
      font-size: 0.77rem;
    }
    .where-cell {
      display: grid;
      gap: 0.15rem;
    }
    .where-cell small {
      color: var(--text-muted);
      font-size: 0.76rem;
    }
    .purpose-text {
      max-width: 44ch;
      font-size: 0.83rem;
      color: var(--text-muted);
      line-height: 1.4;
    }
  `,
  template: `
    <div class="page">
      <header class="page-header">
        <div class="page-title">
          <h1>Dashboard</h1>
          <p>
            Real-time executive summary of who is away, where they are deployed, duration, and
            purpose across all departments.
          </p>
        </div>
        <div class="header-actions">
          <button
            type="button"
            class="btn"
            [disabled]="store.apiLoading()"
            (click)="store.refreshFromApi(true)"
          >
            <app-icon name="refresh" />
            <span>{{ store.apiLoading() ? 'Refreshing...' : 'Refresh' }}</span>
          </button>
        </div>
      </header>

      @if (store.apiNotice(); as notice) {
        <div
          role="status"
          style="display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.8rem 1.1rem; border-radius: var(--radius); font-size: 0.88rem; font-weight: 600"
          [style.background]="notice.tone === 'success' ? 'var(--success-bg)' : 'var(--danger-bg)'"
          [style.color]="notice.tone === 'success' ? 'var(--success)' : 'var(--danger)'"
        >
          <div style="display: flex; align-items: center; gap: 0.5rem">
            <app-icon [name]="notice.tone === 'success' ? 'check' : 'alert'" />
            <span>{{ notice.text }}</span>
          </div>
          <button
            type="button"
            class="btn btn-sm btn-ghost"
            aria-label="Dismiss notification"
            (click)="store.clearApiNotice()"
          >
            <app-icon name="close" />
          </button>
        </div>
      }

      <!-- Top KPI Cards -->
      <section class="kpi-grid" aria-label="Key metrics">
        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Officers Currently Away</span>
            <span class="kpi-icon"><app-icon name="plane" /></span>
          </div>
          <div class="kpi-value">{{ store.currentlyAwayCount() }}</div>
          <div class="kpi-sub">
            <span>of {{ store.outOfOfficeList().length }} total logged notifications</span>
          </div>
        </article>

        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Officers Currently in Office</span>
            <span class="kpi-icon" style="background: var(--success-bg); color: var(--success)">
              <app-icon name="users" />
            </span>
          </div>
          <div class="kpi-value">{{ store.currentlyInOfficeCount() }}</div>
          <div class="kpi-sub">
            <span>of {{ store.staffMembers().length }} registered staff across {{ store.departments().length }} departments</span>
          </div>
        </article>

        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Weekly Consolidated Reports</span>
            <span class="kpi-icon" style="background: var(--purple-bg); color: var(--purple)">
              <app-icon name="calendar" />
            </span>
          </div>
          <div class="kpi-value">{{ store.weeklyReports().length }}</div>
          <div class="kpi-sub">
            <span>Executive weekly briefs</span>
          </div>
        </article>
      </section>

      <!-- Who Is Away, Where They Are, Duration & Purpose -->
      <section class="card">
        <div class="card-header">
          <div>
            <h2>Who Is Away, Location, Duration &amp; Purpose</h2>
            <p>Live roster of officers on official travel, meetings, workshops &amp; field assignments</p>
          </div>
          <a routerLink="/out-of-office" class="btn btn-sm">View Full Registry</a>
        </div>

        <div class="toolbar">
          <label class="search-box">
            <span class="search-icon"><app-icon name="search" /></span>
            <input
              type="search"
              class="input"
              placeholder="Filter by officer, duty station, destination, or purpose..."
              [ngModel]="searchQuery()"
              (ngModelChange)="searchQuery.set($event); page.set(0)"
            />
          </label>
          <select
            class="select filter-select"
            aria-label="Filter by department"
            [ngModel]="departmentFilter()"
            (ngModelChange)="departmentFilter.set($event); page.set(0)"
          >
            <option value="ALL">All Departments</option>
            @for (dept of store.departments(); track dept) {
              <option [value]="dept">{{ dept }}</option>
            }
          </select>
          <select
            class="select filter-select"
            aria-label="Filter by deployment status"
            [ngModel]="statusFilter()"
            (ngModelChange)="statusFilter.set($event); page.set(0)"
          >
            <option value="ALL">All Deployments</option>
            <option value="ACTIVE_AWAY">Currently Away</option>
            <option value="RETURNED">Returned</option>
          </select>
        </div>

        <div
          tabindex="0"
          role="region"
          aria-labelledby="away-officers-caption"
          class="table-wrapper"
        >
          <table class="data-table">
            <caption id="away-officers-caption">
              Officers currently away or scheduled for out-of-office engagements
            </caption>
            <thead>
              <tr>
                <th>Officer &amp; Unit</th>
                <th>Where They Are</th>
                <th>Duration</th>
                <th>Purpose &amp; Cover</th>
                <th>Status</th>
                <th>Supervisor Confirmation</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (item of pagedAwayOfficers(); track item.id) {
                <tr>
                  <th>
                    <div class="officer-cell">
                      <div style="display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap">
                        <strong>{{ item.officerName }}</strong>
                        @if (item.referenceNumber) {
                          <span class="badge" style="font-family: var(--font-mono); font-size: 0.68rem">
                            {{ item.referenceNumber }}
                          </span>
                        }
                      </div>
                      <small>{{ item.officerRole }} · {{ item.dutyStation }}</small>
                      <small>{{ item.department }}</small>
                    </div>
                  </th>
                  <td data-label="Where They Are">
                    <div class="where-cell">
                      <strong>{{ item.destination }}</strong>
                      <small>{{ engagementLabel(item.engagementType) }} · {{ item.hostOrganization }}</small>
                    </div>
                  </td>
                  <td data-label="Duration" style="white-space: nowrap">
                    <div class="where-cell">
                      <strong>{{ store.computeDurationDays(item.startDate, item.endDate) }} day(s)</strong>
                      <small>{{ item.startDate }} → {{ item.endDate }}</small>
                    </div>
                  </td>
                  <td data-label="Purpose & Cover">
                    <div class="purpose-text">
                      {{ item.purpose }}
                      <div><small><strong>Acting cover:</strong> {{ item.actingOfficer }}</small></div>
                    </div>
                  </td>
                  <td data-label="Status">
                    <span [class]="statusBadgeClass(item.status)">
                      {{ statusLabel(item.status) }}
                    </span>
                  </td>
                  <td data-label="Supervisor Confirmation">
                    @if ((item.supervisorConfirmationStatus || 'NEW') === 'SEEN') {
                      <span class="badge badge-success">Seen</span>
                    } @else {
                      <span class="badge badge-warning">New</span>
                    }
                  </td>
                  <td data-label="Actions">
                    <a
                      [routerLink]="['/out-of-office', item.id, 'view-report']"
                      class="btn btn-sm"
                      style="width: 32px; height: 32px; padding: 0"
                      title="View Report (Section A & B PDF)"
                      aria-label="View Report (Section A & B PDF)"
                      (click)="markReadOnView(item)"
                    >
                      <app-icon name="eye" />
                    </a>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-muted)">
                    No officers match the current filter criteria.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <app-pagination
          [page]="page()"
          [size]="pageSize()"
          [total]="filteredAwayOfficers().length"
          (pageChange)="page.set($event)"
          (sizeChange)="pageSize.set($event)"
        />
      </section>
    </div>
  `,
})
export class Dashboard {
  protected readonly store = inject(StaffTrackStore);
  private readonly auth = inject(AuthService);
  protected readonly searchQuery = signal('');

  protected markReadOnView(item: OutOfOfficeNotification): void {
    if ((item.supervisorConfirmationStatus || 'NEW') === 'NEW') {
      const confirmedBy = this.auth.session()?.fullName || 'Supervisor';
      this.store.markNotificationAsRead(item.id, confirmedBy, true);
    }
  }
  protected readonly departmentFilter = signal('ALL');
  protected readonly statusFilter = signal<'ALL' | OutOfOfficeStatus>('ALL');
  protected readonly pageSize = signal(10);
  protected readonly page = signal(0);

  protected readonly filteredAwayOfficers = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const dept = this.departmentFilter();
    const st = this.statusFilter();
    return this.store.awayOrUpcomingOfficers().filter((item) => {
      if (dept !== 'ALL' && item.department !== dept) return false;
      if (st !== 'ALL' && item.status !== st) return false;
      if (!q) return true;
      return (
        item.officerName.toLowerCase().includes(q) ||
        (item.referenceNumber?.toLowerCase().includes(q) ?? false) ||
        item.dutyStation.toLowerCase().includes(q) ||
        item.destination.toLowerCase().includes(q) ||
        item.purpose.toLowerCase().includes(q) ||
        item.hostOrganization.toLowerCase().includes(q)
      );
    });
  });

  protected readonly pagedAwayOfficers = computed(() => {
    const size = this.pageSize();
    const list = this.filteredAwayOfficers();
    const maxPage = Math.max(0, Math.ceil(list.length / size) - 1);
    const start = Math.min(this.page(), maxPage) * size;
    return list.slice(start, start + size);
  });

  protected engagementLabel(type: EngagementType): string {
    return ENGAGEMENT_TYPE_LABELS[type];
  }

  protected statusLabel(status: OutOfOfficeStatus): string {
    return OOO_STATUS_LABELS[status];
  }

  protected statusBadgeClass(status: OutOfOfficeStatus): string {
    switch (status) {
      case 'ACTIVE_AWAY':
        return 'badge badge-info';
      case 'PENDING_APPROVAL':
        return 'badge badge-warning';
      case 'APPROVED':
        return 'badge badge-purple';
      case 'RETURNED':
        return 'badge badge-success';
      default:
        return 'badge';
    }
  }
}
