import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ENGAGEMENT_TYPE_LABELS,
  EngagementType,
  OOO_STATUS_LABELS,
  OutOfOfficeNotification,
  OutOfOfficeStatus,
  SupervisorConfirmationStatus,
  UNIT_HEAD_APPROVAL_LABELS,
} from '../../core/models/stafftrack.models';
import { AuthService } from '../../core/services/auth.service';
import { ConfirmService } from '../../core/services/confirm.service';
import { StaffTrackStore } from '../../core/services/stafftrack.store';
import { Icon } from '../../shared/icon/icon';
import { Pagination } from '../../shared/pagination/pagination';

@Component({
  selector: 'app-out-of-office',
  imports: [FormsModule, RouterLink, Icon, Pagination],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .cell-stack {
      display: grid;
      gap: 0.18rem;
    }
    .cell-primary {
      font-size: 0.84rem;
      font-weight: 600;
      color: var(--text);
    }
    .cell-stack small {
      color: var(--text-muted);
      font-size: 0.75rem;
      font-weight: 400;
      line-height: 1.35;
    }
    .officer-top {
      display: flex;
      align-items: center;
      gap: 0.45rem;
      flex-wrap: wrap;
    }
    .ref-pill {
      font-family: var(--font-mono);
      font-size: 0.68rem;
      font-weight: 600;
      padding: 0.1rem 0.42rem;
      border-radius: 5px;
      background: var(--bg-subtle);
      border: 1px solid var(--border-strong);
      color: var(--text-muted);
    }
    .duration-cell {
      white-space: nowrap;
    }
    .purpose-col {
      max-width: 42ch;
      font-size: 0.81rem;
      color: var(--text-muted);
      line-height: 1.45;
    }
    .action-btns {
      display: flex;
      align-items: center;
      gap: 0.35rem;
      white-space: nowrap;
    }
    .icon-action-btn {
      width: 32px;
      height: 32px;
      min-height: 32px;
      padding: 0;
    }
  `,
  template: `
    <div class="page">
      <header class="page-header">
        <div class="page-title">
          <h1>Out-of-Office Notifications</h1>
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
          <a routerLink="/out-of-office/new" class="btn btn-primary">
            <app-icon name="plus" />
            <span>Log Out-of-Office Notice</span>
          </a>
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

      <!-- Engagement Category Filter Chips -->
      <section class="kpi-grid" aria-label="Engagement type summary">
        @for (summary of visibleEngagementTypeSummary(); track summary.type) {
          <article class="kpi-card">
            <div class="kpi-top">
              <span class="kpi-label">{{ summary.label }}</span>
              <span class="kpi-icon"><app-icon name="mapPin" /></span>
            </div>
            <div class="kpi-value">{{ summary.count }}</div>
            <div class="kpi-sub">Active or upcoming deployments</div>
          </article>
        }
      </section>

      <!-- Registry Table Card -->
      <section class="card">
        <div class="card-header">
          <div>
            <h2>Officer Movement &amp; Engagement Registry</h2>
            <p>Showing {{ filteredList().length }} of {{ visibleList().length }} notifications</p>
          </div>
        </div>

        <div class="toolbar">
          <label class="search-box">
            <span class="search-icon"><app-icon name="search" /></span>
            <input
              type="search"
              class="input"
              placeholder="Search destination, host institution, duty station, or purpose..."
              [ngModel]="searchQuery()"
              (ngModelChange)="searchQuery.set($event)"
            />
          </label>

          <select
            class="select filter-select"
            aria-label="Filter by engagement type"
            [ngModel]="typeFilter()"
            (ngModelChange)="typeFilter.set($event)"
          >
            <option value="ALL">All Engagement Types</option>
            @for (type of engagementTypes; track type) {
              <option [value]="type">{{ engagementLabels[type] }}</option>
            }
          </select>

          <select
            class="select filter-select"
            aria-label="Filter by status"
            [ngModel]="statusFilter()"
            (ngModelChange)="statusFilter.set($event)"
          >
            <option value="ALL">All Statuses</option>
            @for (st of statusOptions; track st) {
              <option [value]="st">{{ statusLabels[st] }}</option>
            }
          </select>

          <select
            class="select filter-select"
            aria-label="Filter by supervisor confirmation"
            [ngModel]="confirmationFilter()"
            (ngModelChange)="confirmationFilter.set($event)"
          >
            <option value="ALL">All Supervisor Confirmations</option>
            <option value="NEW">New (Unseen)</option>
            <option value="SEEN">Seen by Supervisor</option>
          </select>
        </div>

        <div tabindex="0" role="region" aria-labelledby="ooo-table-caption" class="table-wrapper">
          <table class="data-table">
            <caption id="ooo-table-caption">
              Out-of-Office Notifications for travel, meetings, workshops, and field assignments
            </caption>
            <thead>
              <tr>
                <th>Officer &amp; Station</th>
                <th>Engagement &amp; Destination</th>
                <th>Duration &amp; Dates</th>
                <th>Purpose</th>
                <th>Status</th>
                <th>Unit Head &amp; Confirmation</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (item of pagedList(); track item.id) {
                <tr>
                  <th>
                    <div class="cell-stack">
                      <div class="officer-top">
                        <span class="cell-primary">{{ item.officerName }}</span>
                        @if (item.referenceNumber) {
                          <span class="ref-pill">{{ item.referenceNumber }}</span>
                        }
                      </div>
                      <small>{{ item.officerRole }} · {{ item.dutyStation }}</small>
                    </div>
                  </th>
                  <td data-label="Engagement & Destination">
                    <div class="cell-stack">
                      <div class="officer-top">
                        <span class="cell-primary">{{ item.destination }}</span>
                        <span class="badge badge-info">
                          {{ engagementLabels[item.engagementType] }}
                        </span>
                      </div>
                      <small>Host: {{ item.hostOrganization }} · {{ item.department }}</small>
                    </div>
                  </td>
                  <td data-label="Duration & Dates" class="duration-cell">
                    <div class="cell-stack">
                      <span class="cell-primary">{{ store.computeDurationDays(item.startDate, item.endDate) }} day(s)</span>
                      <small>{{ item.startDate }} → {{ item.endDate }}</small>
                    </div>
                  </td>
                  <td data-label="Purpose">
                    <div class="purpose-col">{{ item.purpose }}</div>
                  </td>
                  <td data-label="Status">
                    <span [class]="statusBadgeClass(item.status)">
                      {{ statusLabels[item.status] }}
                    </span>
                  </td>
                  <td data-label="Unit Head & Confirmation">
                    <div class="cell-stack">
                      @if (item.unitHeadName) {
                        <small><strong>Unit Head:</strong> {{ item.unitHeadName }}</small>
                      }
                      @if (isPendingUnitHead(item)) {
                        <div style="display: flex; align-items: center; gap: 0.35rem; flex-wrap: wrap">
                          <span class="badge badge-warning">Pending Unit Head</span>
                          @if (canReviewUnitHead(item)) {
                            <button
                              type="button"
                              class="btn btn-sm btn-primary"
                              style="font-size: 0.72rem; padding: 0.15rem 0.55rem; min-height: 24px"
                              title="Accept as Unit Head and notify Overall Head"
                              (click)="acceptAsUnitHead(item)"
                            >
                              Accept
                            </button>
                            <button
                              type="button"
                              class="btn btn-sm btn-danger"
                              style="font-size: 0.72rem; padding: 0.15rem 0.55rem; min-height: 24px"
                              title="Reject notification"
                              (click)="rejectAsUnitHead(item)"
                            >
                              Reject
                            </button>
                          }
                        </div>
                      } @else if (item.unitHeadApprovalStatus === 'REJECTED') {
                        <span class="badge" style="width: fit-content">
                          {{ unitHeadLabels['REJECTED'] }}
                        </span>
                      } @else {
                        @if (item.unitHeadApprovalStatus === 'ACCEPTED') {
                          <span class="badge badge-success" style="width: fit-content">
                            {{ unitHeadLabels['ACCEPTED'] }}
                          </span>
                        }
                        <div style="display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap">
                          @if ((item.supervisorConfirmationStatus || 'NEW') === 'SEEN') {
                            <span class="badge badge-info">Overall Seen</span>
                            <button
                              type="button"
                              class="btn btn-sm"
                              style="font-size: 0.72rem; padding: 0.15rem 0.5rem; min-height: 24px"
                              title="Mark notification as New (unseen)"
                              (click)="toggleSupervisorConfirmation(item)"
                            >
                              Mark New
                            </button>
                          } @else {
                            <span class="badge badge-warning">New</span>
                            <button
                              type="button"
                              class="btn btn-sm btn-primary"
                              style="font-size: 0.72rem; padding: 0.15rem 0.5rem; min-height: 24px"
                              title="Confirm as Seen"
                              (click)="toggleSupervisorConfirmation(item)"
                            >
                              Mark Seen
                            </button>
                          }
                        </div>
                        @if ((item.supervisorConfirmationStatus || 'NEW') === 'SEEN' && item.supervisorSeenAt) {
                          <small>
                            {{ item.supervisorConfirmedBy || 'Supervisor' }} · {{ item.supervisorSeenAt.slice(0, 10) }}
                          </small>
                        }
                      }
                    </div>
                  </td>
                  <td data-label="Actions">
                    <div class="action-btns">
                      <a
                        [routerLink]="['/out-of-office', item.id, 'view-report']"
                        class="btn btn-sm icon-action-btn"
                        aria-label="View Report (Section A & B PDF)"
                        title="View Report (Section A & B PDF)"
                        (click)="markReadOnView(item)"
                      >
                        <app-icon name="eye" />
                      </a>
                      <a
                        [routerLink]="['/out-of-office', item.id, 'report-back']"
                        class="btn btn-sm btn-accent icon-action-btn"
                        [attr.aria-label]="item.reportBack ? 'Edit Report Back' : 'Report Back'"
                        [title]="item.reportBack ? 'Edit Report Back' : 'Report Back'"
                      >
                        <app-icon name="fileText" />
                      </a>
                      <a
                        [routerLink]="['/out-of-office', item.id, 'edit']"
                        class="btn btn-sm icon-action-btn"
                        aria-label="Edit notification"
                        title="Edit notification"
                      >
                        <app-icon name="edit" />
                      </a>
                      <button
                        type="button"
                        class="btn btn-sm btn-danger icon-action-btn"
                        aria-label="Delete notification"
                        title="Delete notification"
                        (click)="store.deleteOutOfOffice(item.id)"
                      >
                        <app-icon name="trash" />
                      </button>
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="7" style="text-align: center; padding: 2.2rem; color: var(--text-muted)">
                    No out-of-office notifications match your filter.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <app-pagination
          [page]="page()"
          [size]="pageSize()"
          [total]="filteredList().length"
          (pageChange)="page.set($event)"
          (sizeChange)="pageSize.set($event)"
        />
      </section>
    </div>
  `,
})
export class OutOfOffice {
  protected readonly store = inject(StaffTrackStore);
  private readonly auth = inject(AuthService);
  private readonly confirm = inject(ConfirmService);
  protected readonly engagementLabels = ENGAGEMENT_TYPE_LABELS;
  protected readonly statusLabels = OOO_STATUS_LABELS;
  protected readonly unitHeadLabels = UNIT_HEAD_APPROVAL_LABELS;
  protected readonly engagementTypes: EngagementType[] = [
    'TRAVEL',
    'MEETING',
    'WORKSHOP',
    'FIELD_ASSIGNMENT',
    'OFFICIAL_ENGAGEMENT',
  ];
  protected readonly statusOptions: OutOfOfficeStatus[] = [
    'PENDING_APPROVAL',
    'APPROVED',
    'ACTIVE_AWAY',
    'RETURNED',
    'CANCELLED',
  ];

  protected readonly searchQuery = signal('');
  protected readonly typeFilter = signal<'ALL' | EngagementType>('ALL');
  protected readonly statusFilter = signal<'ALL' | OutOfOfficeStatus>('ALL');
  protected readonly confirmationFilter = signal<'ALL' | SupervisorConfirmationStatus>('ALL');
  protected readonly pageSize = signal(10);
  protected readonly page = signal(0);

  protected readonly visibleList = computed(() =>
    this.store
      .outOfOfficeList()
      .filter((item) => this.auth.canViewOutOfOfficeRecord(item))
  );

  protected readonly visibleEngagementTypeSummary = computed(() => {
    const active = this.visibleList().filter((item) => item.status !== 'CANCELLED');
    return this.engagementTypes.map((type) => ({
      type,
      label: this.engagementLabels[type],
      count: active.filter((a) => a.engagementType === type).length,
    }));
  });

  protected readonly filteredList = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const type = this.typeFilter();
    const status = this.statusFilter();
    const conf = this.confirmationFilter();
    return this.visibleList().filter((item) => {
      if (type !== 'ALL' && item.engagementType !== type) return false;
      if (status !== 'ALL' && item.status !== status) return false;
      const itemConf: SupervisorConfirmationStatus = item.supervisorConfirmationStatus || 'NEW';
      if (conf !== 'ALL' && itemConf !== conf) return false;
      if (!q) return true;
      return (
        item.officerName.toLowerCase().includes(q) ||
        item.destination.toLowerCase().includes(q) ||
        item.hostOrganization.toLowerCase().includes(q) ||
        item.purpose.toLowerCase().includes(q) ||
        item.dutyStation.toLowerCase().includes(q) ||
        item.department.toLowerCase().includes(q)
      );
    });
  });

  protected isPendingUnitHead(item: OutOfOfficeNotification): boolean {
    return (
      item.unitHeadApprovalStatus === 'PENDING_UNIT_HEAD' ||
      (item.status === 'PENDING_APPROVAL' && item.unitHeadApprovalStatus !== 'ACCEPTED')
    );
  }

  protected canReviewUnitHead(item: OutOfOfficeNotification): boolean {
    return this.auth.canReviewAsUnitHead(item);
  }

  protected acceptAsUnitHead(item: OutOfOfficeNotification): void {
    const reviewer = this.auth.session()?.fullName || item.unitHeadName || 'Unit Head';
    this.store.reviewOutOfOfficeByUnitHead(item.id, 'ACCEPTED', reviewer);
  }

  protected rejectAsUnitHead(item: OutOfOfficeNotification): void {
    const reviewer = this.auth.session()?.fullName || item.unitHeadName || 'Unit Head';
    this.confirm.ask({
      title: 'Reject Out-of-Office Notification',
      message: `Reject ${item.officerName}'s Out-of-Office notification (${item.destination})? Overall Head will not be notified.`,
      confirmLabel: 'Reject Notification',
      onConfirm: () => {
        this.store.reviewOutOfOfficeByUnitHead(item.id, 'REJECTED', reviewer);
      },
    });
  }

  protected toggleSupervisorConfirmation(item: OutOfOfficeNotification): void {
    const current: SupervisorConfirmationStatus = item.supervisorConfirmationStatus || 'NEW';
    const next: SupervisorConfirmationStatus = current === 'SEEN' ? 'NEW' : 'SEEN';
    const confirmedBy = this.auth.session()?.fullName || 'Supervisor';
    this.store.updateOutOfOfficeSupervisorConfirmation(item.id, next, confirmedBy);
  }

  protected markReadOnView(item: OutOfOfficeNotification): void {
    if ((item.supervisorConfirmationStatus || 'NEW') === 'NEW') {
      const confirmedBy = this.auth.session()?.fullName || 'Supervisor';
      this.store.markNotificationAsRead(item.id, confirmedBy, true);
    }
  }

  protected readonly pagedList = computed(() => {
    const size = this.pageSize();
    const list = this.filteredList();
    const maxPage = Math.max(0, Math.ceil(list.length / size) - 1);
    const start = Math.min(this.page(), maxPage) * size;
    return list.slice(start, start + size);
  });

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
