import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  BillingModel,
  BillingPlanStatus,
  ClientBillingPlan,
  NotificationChannelConfig,
  NotificationSupervisor,
  SupervisorCategory,
  SupervisorNotificationStatus,
} from '../../core/models/stafftrack.models';
import { ConfirmService } from '../../core/services/confirm.service';
import { StaffTrackStore } from '../../core/services/stafftrack.store';
import { Icon } from '../../shared/icon/icon';
import { Pagination } from '../../shared/pagination/pagination';

interface ClientOption {
  clientRef: string;
  clientName: string;
  clientCode: string;
}

type ConfigTab = 'SUPERVISORS' | 'CHANNELS' | 'BILLING';

@Component({
  selector: 'app-configurations',
  imports: [FormsModule, Icon, Pagination],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .tab-strip {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem;
      border-bottom: 1px solid var(--border);
      padding: 0 1rem;
      background: var(--bg-subtle);
      border-radius: var(--radius-lg) var(--radius-lg) 0 0;
    }
    .tab-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.85rem 1.05rem;
      font: inherit;
      font-size: 0.88rem;
      font-weight: 600;
      color: var(--text-muted);
      background: transparent;
      border: 0;
      border-bottom: 3px solid transparent;
      cursor: pointer;
      transition: color 150ms, border-color 150ms;
    }
    .tab-btn:hover {
      color: var(--text);
    }
    .tab-btn.active {
      color: var(--brand-500);
      border-bottom-color: var(--brand-500);
    }
    .tab-count {
      display: inline-grid;
      place-items: center;
      min-width: 22px;
      height: 20px;
      padding: 0 0.45rem;
      border-radius: 999px;
      font-size: 0.73rem;
      font-weight: 700;
      background: var(--bg-elevated);
      color: var(--text-muted);
      border: 1px solid var(--border);
    }
    .tab-btn.active .tab-count {
      background: var(--info-bg);
      color: var(--brand-500);
      border-color: color-mix(in srgb, var(--brand-500) 30%, var(--border));
    }
    .info-banner {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      padding: 0.9rem 1.15rem;
      border-radius: var(--radius);
      border: 1px solid color-mix(in srgb, var(--info) 35%, var(--border));
      background: var(--info-bg);
      color: var(--text);
      font-size: 0.88rem;
    }
    .info-banner app-icon {
      color: var(--info);
      margin-top: 0.1rem;
    }
    .notice-banner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.8rem 1.1rem;
      border-radius: var(--radius);
      background: var(--success-bg);
      color: var(--success);
      font-weight: 600;
      font-size: 0.88rem;
    }
    .error-banner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.75rem 1rem;
      border-radius: var(--radius);
      background: var(--danger-bg);
      color: var(--danger);
      font-weight: 600;
      font-size: 0.84rem;
    }
    .toolbar-spacer {
      flex: 1;
    }
    .cell-stack {
      display: grid;
      gap: 0.12rem;
    }
    .cell-primary {
      font-weight: 600;
      color: var(--text);
    }
    .cell-secondary {
      font-size: 0.76rem;
      color: var(--text-muted);
    }
    .cell-mono {
      font-family: var(--font-mono);
    }
    .ta-right {
      text-align: end;
    }
    .amount {
      font-weight: 700;
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
    }
    .row-actions {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
      white-space: nowrap;
    }
    .icon-btn {
      width: 34px;
      height: 34px;
      min-height: 34px;
      padding: 0;
    }
    .segments {
      display: inline-flex;
      flex-wrap: wrap;
      gap: 0.3rem;
      padding: 0.25rem;
      border: 1px solid var(--border-strong);
      border-radius: 12px;
      background: var(--bg-subtle);
      width: fit-content;
    }
    .seg-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      border: 0;
      background: transparent;
      color: var(--text-muted);
      font: inherit;
      font-weight: 600;
      font-size: 0.86rem;
      padding: 0.5rem 0.9rem;
      border-radius: 9px;
      cursor: pointer;
    }
    .seg-btn.active {
      color: var(--brand-500);
      background: var(--bg-elevated);
      box-shadow: var(--shadow-sm);
    }
    .input-suffix {
      position: relative;
    }
    .input-suffix .input {
      padding-inline-end: 3.4rem;
      font-variant-numeric: tabular-nums;
    }
    .input-suffix span {
      position: absolute;
      inset-inline-end: 0.85rem;
      top: 50%;
      transform: translateY(-50%);
      color: var(--text-muted);
      font-size: 0.8rem;
      font-weight: 700;
      pointer-events: none;
    }
    .preview-box {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: center;
      gap: 0.75rem;
      padding: 0.85rem 1rem;
      border-radius: var(--radius);
      border: 1px dashed var(--border-strong);
      background: var(--bg-subtle);
      font-size: 0.86rem;
      color: var(--text-muted);
    }
    .preview-box strong {
      color: var(--text);
      font-variant-numeric: tabular-nums;
    }
    .channel-pills {
      display: inline-flex;
      flex-wrap: wrap;
      gap: 0.35rem;
    }
    .channel-check-label {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      font-size: 0.84rem;
      font-weight: 600;
      cursor: pointer;
    }
    .channel-check-label.is-disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .channel-check-label input[type='checkbox'] {
      width: 1.05rem;
      height: 1.05rem;
      accent-color: var(--brand-500);
      margin: 0;
      cursor: inherit;
    }
    .cat-fieldset {
      border: 1px solid var(--border-strong);
      border-radius: var(--radius);
      padding: 0.75rem 0.9rem;
      margin: 0;
      background: var(--bg-subtle);
    }
    .cat-legend {
      font-size: 0.84rem;
      font-weight: 700;
      color: var(--text);
      padding: 0 0.35rem;
    }
    .cat-radio-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 0.65rem;
      margin-top: 0.35rem;
    }
    .cat-radio-item {
      display: flex;
      align-items: flex-start;
      gap: 0.55rem;
      padding: 0.65rem 0.75rem;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-strong);
      background: var(--bg-elevated);
    }
    .cat-radio-item.is-selected {
      border-color: var(--brand-500);
      background: var(--info-bg);
    }
    .cat-radio-item input[type='radio'] {
      margin-top: 0.2rem;
      width: 1.05rem;
      height: 1.05rem;
      accent-color: var(--brand-500);
      flex: none;
      cursor: pointer;
    }
    .cat-radio-item label {
      display: grid;
      gap: 0.15rem;
      cursor: pointer;
      font-size: 0.84rem;
      line-height: 1.35;
    }
    .cat-radio-item label span {
      font-size: 0.76rem;
      font-weight: 400;
      color: var(--text-muted);
    }
  `,
  template: `
    <div class="page">
      <header class="page-header">
        <div class="page-title">
          <h1>Configurations</h1>
          <p>
            Manage the Supervisors List, active Notification &amp; OTP Delivery Channels, and client
            billing plans.
          </p>
        </div>
      </header>

      @if (store.apiNotice(); as apiMsg) {
        <div
          role="status"
          [class]="apiMsg.tone === 'success' ? 'notice-banner' : 'error-banner'"
        >
          <div style="display: flex; align-items: center; gap: 0.5rem">
            <app-icon [name]="apiMsg.tone === 'success' ? 'check' : 'alert'" />
            <span>{{ apiMsg.text }}</span>
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

      @if (notice()) {
        <div class="notice-banner" role="status">
          <div style="display: flex; align-items: center; gap: 0.5rem">
            <app-icon name="check" />
            <span>{{ notice() }}</span>
          </div>
          <button type="button" class="btn btn-sm btn-ghost" (click)="notice.set('')">
            <app-icon name="close" />
          </button>
        </div>
      }

      <section class="card">
        <div class="tab-strip" role="tablist" aria-label="Configuration sections">
          <button
            type="button"
            role="tab"
            class="tab-btn"
            [class.active]="activeTab() === 'SUPERVISORS'"
            [attr.aria-selected]="activeTab() === 'SUPERVISORS'"
            (click)="activeTab.set('SUPERVISORS')"
          >
            <app-icon name="users" />
            <span>Entity Supervisors (Notifications)</span>
            <span class="tab-count">{{ store.notificationSupervisors().length }}</span>
          </button>

          <button
            type="button"
            role="tab"
            class="tab-btn"
            [class.active]="activeTab() === 'CHANNELS'"
            [attr.aria-selected]="activeTab() === 'CHANNELS'"
            (click)="activeTab.set('CHANNELS')"
          >
            <app-icon name="settings" />
            <span>Delivery Channels</span>
            <span class="tab-count">{{ store.activeChannelCount() }}/{{ store.channelConfigs().length }}</span>
          </button>

          <button
            type="button"
            role="tab"
            class="tab-btn"
            [class.active]="activeTab() === 'BILLING'"
            [attr.aria-selected]="activeTab() === 'BILLING'"
            (click)="activeTab.set('BILLING')"
          >
            <app-icon name="fileText" />
            <span>Client Billing Plans</span>
            <span class="tab-count">{{ store.billingPlans().length }}</span>
          </button>
        </div>

        <!-- TAB 1: SUPERVISORS LIST TO RECEIVE NOTIFICATIONS -->
        @if (activeTab() === 'SUPERVISORS') {
          <div style="padding: 1rem 1.25rem 0">
            <div class="info-banner">
              <app-icon name="mail" />
              <span>
                Categorize supervisors as <strong>Unit Head</strong> (selected by staff on creating
                Out-of-Office; views &amp; accepts before sending to Overall Head) or
                <strong>Overall Head</strong> (notified once Unit Head accepts — no accept/reject
                needed). On returning, <strong>both Unit Head and Overall Head</strong> are notified.
              </span>
            </div>
          </div>

          <div class="toolbar">
            <label class="search-box" style="max-width: 320px">
              <span class="search-icon"><app-icon name="search" /></span>
              <input
                type="search"
                name="supSearchQuery"
                class="input"
                placeholder="Search supervisor name, email, or phone..."
                [ngModel]="supSearchQuery()"
                (ngModelChange)="supSearchQuery.set($event); supPage.set(0)"
              />
            </label>

            <select
              name="supCategoryFilter"
              class="select filter-select"
              aria-label="Filter supervisors by category"
              [ngModel]="supCategoryFilter()"
              (ngModelChange)="supCategoryFilter.set($event); supPage.set(0)"
            >
              <option value="ALL">All categories</option>
              <option value="UNIT_HEAD">Unit Head (Reviews &amp; Accepts)</option>
              <option value="OVERALL_HEAD">Overall Head (Notified Only)</option>
            </select>

            <select
              name="supStatusFilter"
              class="select filter-select"
              aria-label="Filter supervisors by status"
              [ngModel]="supStatusFilter()"
              (ngModelChange)="supStatusFilter.set($event); supPage.set(0)"
            >
              <option value="ALL">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>

            <span class="toolbar-spacer"></span>

            <button
              type="button"
              class="btn"
              [disabled]="store.apiLoading()"
              (click)="store.refreshFromApi(true)"
            >
              <app-icon name="refresh" />
              <span>{{ store.apiLoading() ? 'Refreshing...' : 'Refresh' }}</span>
            </button>

            <button type="button" class="btn btn-primary" (click)="openCreateSupervisor()">
              <app-icon name="plus" />
              <span>Add Supervisor</span>
            </button>
          </div>

          <div tabindex="0" role="region" aria-labelledby="supervisors-caption" class="table-wrapper">
            <table class="data-table">
              <caption id="supervisors-caption">
                Entity supervisors configured to receive email or SMS/WhatsApp notifications
              </caption>
              <thead>
                <tr>
                  <th>Entity / Organisation</th>
                  <th>Supervisor Name</th>
                  <th>Category</th>
                  <th>Email Address</th>
                  <th>Phone Number</th>
                  <th>Notification Channels</th>
                  <th>Status</th>
                  <th class="ta-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (sup of pagedSupervisors(); track sup.id) {
                  <tr>
                    <td data-label="Entity / Organisation">
                      @if (sup.entityName || sup.entityCode) {
                        <div class="cell-stack">
                          <span class="cell-primary">{{ sup.entityName || sup.entityCode }}</span>
                          @if (sup.entityCode) {
                            <span class="cell-secondary cell-mono">{{ sup.entityCode }}</span>
                          }
                        </div>
                      } @else {
                        <span class="cell-secondary">All Entities (OPM)</span>
                      }
                    </td>
                    <td data-label="Supervisor Name">
                      <span class="cell-primary">{{ sup.name || '—' }}</span>
                    </td>
                    <td data-label="Category">
                      @if ((sup.category || 'OVERALL_HEAD') === 'UNIT_HEAD') {
                        <div class="cell-stack">
                          <span class="badge badge-purple" style="width: fit-content">Unit Head</span>
                          <span class="cell-secondary">Views &amp; Accepts</span>
                        </div>
                      } @else {
                        <div class="cell-stack">
                          <span class="badge badge-info" style="width: fit-content">Overall Head</span>
                          <span class="cell-secondary">Notified Only</span>
                        </div>
                      }
                    </td>
                    <td data-label="Email Address">
                      @if (sup.email) {
                        <span class="cell-mono" style="font-size: 0.84rem">{{ sup.email }}</span>
                      } @else {
                        <span class="cell-secondary">—</span>
                      }
                    </td>
                    <td data-label="Phone Number">
                      @if (sup.phone) {
                        <span class="cell-mono" style="font-size: 0.84rem">{{ sup.phone }}</span>
                      } @else {
                        <span class="cell-secondary">—</span>
                      }
                    </td>
                    <td data-label="Notification Channels">
                      <div class="channel-pills">
                        @if (sup.email && store.isSupervisorChannelActive('EMAIL')) {
                          <span class="badge badge-info">Email</span>
                        }
                        @if (sup.phone && store.isSupervisorChannelActive('WHATSAPP')) {
                          <span class="badge badge-success">WhatsApp</span>
                        }
                        @if (sup.phone && store.isSupervisorChannelActive('SMS')) {
                          <span class="badge badge-warning">SMS</span>
                        }
                      </div>
                    </td>
                    <td data-label="Status">
                      <span
                        [class]="
                          sup.status === 'ACTIVE' ? 'badge badge-success' : 'badge badge-warning'
                        "
                      >
                        {{ sup.status === 'ACTIVE' ? 'Active' : 'Inactive' }}
                      </span>
                    </td>
                    <td data-label="Actions" class="ta-right">
                      <div class="row-actions">
                        <button
                          type="button"
                          class="btn btn-sm icon-btn"
                          title="Edit supervisor"
                          aria-label="Edit supervisor"
                          (click)="openEditSupervisor(sup)"
                        >
                          <app-icon name="edit" />
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm icon-btn"
                          [title]="
                            sup.status === 'ACTIVE'
                              ? 'Pause notifications'
                              : 'Enable notifications'
                          "
                          [attr.aria-label]="
                            sup.status === 'ACTIVE'
                              ? 'Pause notifications'
                              : 'Enable notifications'
                          "
                          (click)="toggleSupervisorStatus(sup)"
                        >
                          <app-icon name="power" />
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm btn-danger icon-btn"
                          title="Remove supervisor"
                          aria-label="Remove supervisor"
                          (click)="removeSupervisor(sup)"
                        >
                          <app-icon name="trash" />
                        </button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="8" style="text-align: center; padding: 2.2rem; color: var(--text-muted)">
                      No entity supervisors match your filter. Click "+ Add Supervisor" to configure a Unit Head or Overall Head.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <app-pagination
            [page]="supPage()"
            [size]="supPageSize()"
            [total]="filteredSupervisors().length"
            (pageChange)="supPage.set($event)"
            (sizeChange)="supPageSize.set($event)"
          />
        }

        <!-- TAB 2: DELIVERY CHANNELS (EMAIL / SMS / WHATSAPP) -->
        @if (activeTab() === 'CHANNELS') {
          <div style="padding: 1rem 1.25rem 0">
            <div class="info-banner">
              <app-icon name="shield" />
              <span>
                Control which communication channels (<strong>Email</strong>, <strong>SMS</strong>,
                and <strong>WhatsApp</strong>) are active for 6-digit OTP sign-in and supervisor
                movement alerts. Disabled channels are automatically locked on the Sign-In screen.
              </span>
            </div>
          </div>

          <div class="toolbar">
            <span class="cell-secondary">
              <strong>{{ store.activeChannelCount() }}</strong> of
              <strong>{{ store.channelConfigs().length }}</strong> channels active
            </span>
            <span class="toolbar-spacer"></span>
            <button
              type="button"
              class="btn"
              [disabled]="store.apiLoading()"
              (click)="store.loadChannelConfigs()"
            >
              <app-icon name="refresh" />
              <span>Refresh Channels</span>
            </button>
          </div>

          <div tabindex="0" role="region" aria-labelledby="channels-caption" class="table-wrapper">
            <table class="data-table">
              <caption id="channels-caption">
                Active and disabled notification and OTP delivery channels
              </caption>
              <thead>
                <tr>
                  <th>Channel</th>
                  <th>Description</th>
                  <th>OTP Sign-In</th>
                  <th>Supervisor Alerts</th>
                  <th>Status</th>
                  <th class="ta-right">Action</th>
                </tr>
              </thead>
              <tbody>
                @for (ch of store.channelConfigs(); track ch.channelCode) {
                  <tr>
                    <td data-label="Channel">
                      <div class="cell-stack">
                        <span class="cell-primary">{{ ch.displayName }}</span>
                        <span class="cell-secondary cell-mono">{{ ch.channelCode }}</span>
                      </div>
                    </td>
                    <td data-label="Description" style="max-width: 360px">
                      <span style="font-size: 0.84rem; color: var(--text-muted)">
                        {{ ch.description }}
                      </span>
                    </td>
                    <td data-label="OTP Sign-In">
                      <label class="channel-check-label" [class.is-disabled]="!ch.active">
                        <input
                          type="checkbox"
                          [name]="'otp-' + ch.channelCode"
                          [disabled]="!ch.active"
                          [checked]="ch.active && ch.otpEnabled"
                          (change)="toggleChannelOtp(ch, $any($event.target).checked)"
                        />
                        <span>{{ ch.active && ch.otpEnabled ? 'Enabled' : 'Disabled' }}</span>
                      </label>
                    </td>
                    <td data-label="Supervisor Alerts">
                      <label class="channel-check-label" [class.is-disabled]="!ch.active">
                        <input
                          type="checkbox"
                          [name]="'sup-' + ch.channelCode"
                          [disabled]="!ch.active"
                          [checked]="ch.active && ch.supervisorAlertEnabled"
                          (change)="toggleChannelSupervisorAlert(ch, $any($event.target).checked)"
                        />
                        <span>
                          {{ ch.active && ch.supervisorAlertEnabled ? 'Enabled' : 'Disabled' }}
                        </span>
                      </label>
                    </td>
                    <td data-label="Status">
                      <span [class]="ch.active ? 'badge badge-success' : 'badge badge-warning'">
                        {{ ch.active ? 'Active' : 'Disabled' }}
                      </span>
                    </td>
                    <td data-label="Action" class="ta-right">
                      <button
                        type="button"
                        [class]="ch.active ? 'btn btn-sm' : 'btn btn-sm btn-primary'"
                        (click)="toggleChannelActive(ch)"
                      >
                        <span>{{ ch.active ? 'Disable' : 'Enable' }}</span>
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }

        <!-- TAB 3: CLIENT BILLING PLANS -->
        @if (activeTab() === 'BILLING') {
          <div style="padding: 1rem 1.25rem 0">
            <div class="info-banner">
              <app-icon name="sparkles" />
              <span>
                Clients can be billed either by <strong>commission per transaction</strong> (deducted
                from their prepaid wallet) or by an <strong>agreed monthly amount</strong> paid at
                the end of every month.
              </span>
            </div>
          </div>

          <div class="toolbar">
            <label class="search-box" style="max-width: 340px">
              <span class="search-icon"><app-icon name="search" /></span>
              <input
                type="search"
                class="input"
                placeholder="Search client name or code..."
                [ngModel]="searchQuery()"
                (ngModelChange)="searchQuery.set($event); page.set(0)"
              />
            </label>

            <select
              class="select filter-select"
              aria-label="Filter by status"
              [ngModel]="statusFilter()"
              (ngModelChange)="statusFilter.set($event); page.set(0)"
            >
              <option value="ALL">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="SUSPENDED">Suspended</option>
            </select>

            <span class="toolbar-spacer"></span>

            <button
              type="button"
              class="btn"
              [disabled]="store.apiLoading()"
              (click)="store.refreshFromApi(true)"
            >
              <app-icon name="refresh" />
              <span>{{ store.apiLoading() ? 'Refreshing...' : 'Refresh' }}</span>
            </button>

            <button type="button" class="btn btn-primary" (click)="openCreate()">
              <app-icon name="plus" />
              <span>New billing plan</span>
            </button>
          </div>

          <div tabindex="0" role="region" aria-labelledby="plans-caption" class="table-wrapper">
            <table class="data-table">
              <caption id="plans-caption">
                Client billing plans configured with commission per transaction or monthly agreed amount
              </caption>
              <thead>
                <tr>
                  <th>Client</th>
                  <th>Billing model</th>
                  <th class="ta-right">Rate / Agreed amount</th>
                  <th class="ta-right">Low-balance alert</th>
                  <th>Status</th>
                  <th>Updated</th>
                  <th class="ta-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (plan of pagedPlans(); track plan.id) {
                  <tr>
                    <th>
                      <div class="cell-stack">
                        <span class="cell-primary">{{ plan.clientName }}</span>
                        <span class="cell-secondary cell-mono">{{ plan.clientCode }}</span>
                      </div>
                    </th>
                    <td data-label="Billing model">
                      <span
                        [class]="
                          plan.billingModel === 'MONTHLY_FLAT'
                            ? 'badge badge-info'
                            : 'badge badge-success'
                        "
                      >
                        {{ plan.billingModel === 'MONTHLY_FLAT' ? 'Monthly agreed' : 'Commission' }}
                      </span>
                    </td>
                    <td data-label="Rate / Agreed amount" class="ta-right">
                      @if (plan.billingModel === 'MONTHLY_FLAT') {
                        <div class="cell-stack">
                          <span class="amount">{{ formatMoney(plan.monthlyAmount, plan.currency) }}</span>
                          <span class="cell-secondary">End of month</span>
                        </div>
                      } @else {
                        <div class="cell-stack">
                          <span class="amount">{{ plan.percentageRate }}%</span>
                          <span class="cell-secondary">Per transaction</span>
                        </div>
                      }
                    </td>
                    <td data-label="Low-balance alert" class="ta-right">
                      <span class="cell-secondary">
                        {{
                          plan.billingModel === 'COMMISSION' && plan.lowBalanceThreshold != null
                            ? formatMoney(plan.lowBalanceThreshold, plan.currency)
                            : '—'
                        }}
                      </span>
                    </td>
                    <td data-label="Status">
                      <span
                        [class]="
                          plan.status === 'ACTIVE' ? 'badge badge-success' : 'badge badge-warning'
                        "
                      >
                        {{ plan.status === 'ACTIVE' ? 'Active' : 'Suspended' }}
                      </span>
                    </td>
                    <td data-label="Updated">
                      <span class="cell-secondary">{{ plan.updatedAt || plan.createdAt }}</span>
                    </td>
                    <td data-label="Actions" class="ta-right">
                      <div class="row-actions">
                        <button
                          type="button"
                          class="btn btn-sm icon-btn"
                          title="Edit plan"
                          aria-label="Edit plan"
                          (click)="openEdit(plan)"
                        >
                          <app-icon name="edit" />
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm icon-btn"
                          [title]="plan.status === 'ACTIVE' ? 'Suspend billing' : 'Activate billing'"
                          [attr.aria-label]="
                            plan.status === 'ACTIVE' ? 'Suspend billing' : 'Activate billing'
                          "
                          (click)="toggleStatus(plan)"
                        >
                          <app-icon name="power" />
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm btn-danger icon-btn"
                          title="Remove plan"
                          aria-label="Remove plan"
                          (click)="removePlan(plan)"
                        >
                          <app-icon name="trash" />
                        </button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="7" style="text-align: center; padding: 2.2rem; color: var(--text-muted)">
                      No billing plans match the selected status filter.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <app-pagination
            [page]="page()"
            [size]="pageSize()"
            [total]="filteredPlans().length"
            (pageChange)="page.set($event)"
            (sizeChange)="pageSize.set($event)"
          />
        }
      </section>

      <!-- Add / Edit Notification Supervisor Modal -->
      @if (supDialogOpen()) {
        <div class="modal-backdrop">
          <div
            class="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="sup-dialog-title"
          >
            <div class="modal-header">
              <h2 id="sup-dialog-title">
                {{ editingSupId() ? 'Edit Notification Supervisor' : 'Add Supervisor to Receive Notifications' }}
              </h2>
              <button type="button" class="btn btn-sm btn-ghost" (click)="closeSupervisorDialog()">
                <app-icon name="close" />
              </button>
            </div>

            <form (ngSubmit)="submitSupervisor()">
              <div class="modal-body">
                @if (supFormError()) {
                  <div class="error-banner" role="alert">
                    <div style="display: flex; align-items: center; gap: 0.45rem">
                      <app-icon name="alert" />
                      <span>{{ supFormError() }}</span>
                    </div>
                  </div>
                }

                <div class="form-grid">
                  <div class="field field-full">
                    <fieldset class="cat-fieldset">
                      <legend class="cat-legend">
                        Supervisor Category <span aria-hidden="true">*</span>
                      </legend>
                      <div class="cat-radio-grid">
                        <div
                          class="cat-radio-item"
                          [class.is-selected]="supForm.category === 'UNIT_HEAD'"
                        >
                          <input
                            type="radio"
                            id="sup-cat-unit"
                            name="supCategory"
                            value="UNIT_HEAD"
                            [(ngModel)]="supForm.category"
                          />
                          <label for="sup-cat-unit">
                            <strong>Unit Head</strong>
                            <span>
                              Selected by staff on creating Out-of-Office; views &amp; accepts
                              before sending to Overall Head; notified on return.
                            </span>
                          </label>
                        </div>

                        <div
                          class="cat-radio-item"
                          [class.is-selected]="supForm.category === 'OVERALL_HEAD'"
                        >
                          <input
                            type="radio"
                            id="sup-cat-overall"
                            name="supCategory"
                            value="OVERALL_HEAD"
                            [(ngModel)]="supForm.category"
                          />
                          <label for="sup-cat-overall">
                            <strong>Overall Head</strong>
                            <span>
                              Notified automatically once Unit Head accepts (no accept/reject
                              needed) and notified on return.
                            </span>
                          </label>
                        </div>
                      </div>
                    </fieldset>
                  </div>

                  <div class="field field-full">
                    <label for="sup-entity">Entity / Organisation / Department (Optional)</label>
                    <select
                      id="sup-entity"
                      name="supEntityRef"
                      class="select"
                      [(ngModel)]="supForm.clientRef"
                    >
                      <option value="">-- Default Entity (OPM) --</option>
                      @for (c of availableClients(); track c.clientRef) {
                        <option [value]="c.clientRef">
                          {{ c.clientName }} ({{ c.clientCode }})
                        </option>
                      }
                    </select>
                  </div>

                  @if (store.staffMembers().length > 0) {
                    <div class="field field-full">
                      <label for="sup-staff-picker">
                        Quick-fill from Registered Staff (Optional)
                      </label>
                      <select
                        id="sup-staff-picker"
                        name="selectedStaffId"
                        class="select"
                        [ngModel]="selectedSupStaffId()"
                        (ngModelChange)="onSupervisorStaffPick($event)"
                      >
                        <option value="">-- Enter details below or select from Staff Register --</option>
                        @for (s of store.staffMembers(); track s.id) {
                          <option [value]="s.id">
                            {{ s.surname }} {{ s.lastName }} — {{ s.email || s.phone }} ({{ s.department }})
                          </option>
                        }
                      </select>
                    </div>
                  }

                  <div class="field field-full">
                    <label for="sup-name">Supervisor Name (Optional if Email or Phone provided)</label>
                    <input
                      id="sup-name"
                      type="text"
                      name="supName"
                      class="input"
                      [(ngModel)]="supForm.name"
                      placeholder="e.g. Commissioner for Refugees"
                    />
                  </div>

                  <div class="field">
                    <label for="sup-email">
                      Email Address {{ !supForm.phone.trim() ? '*' : '(Optional)' }}
                    </label>
                    <input
                      id="sup-email"
                      type="email"
                      name="supEmail"
                      class="input"
                      autocomplete="email"
                      autocorrect="off"
                      autocapitalize="none"
                      spellcheck="false"
                      [(ngModel)]="supForm.email"
                      (ngModelChange)="supFormError.set('')"
                      placeholder="e.g. supervisor@opm.go.ug"
                    />
                  </div>

                  <div class="field">
                    <label for="sup-phone">
                      Phone Number {{ !supForm.email.trim() ? '*' : '(Optional)' }}
                    </label>
                    <input
                      id="sup-phone"
                      type="tel"
                      name="supPhone"
                      class="input"
                      autocomplete="tel"
                      [(ngModel)]="supForm.phone"
                      (ngModelChange)="supFormError.set('')"
                      placeholder="e.g. +256 700 000 000"
                    />
                  </div>

                  <div class="field field-full">
                    <label for="sup-status">Notification Status</label>
                    <select
                      id="sup-status"
                      name="supStatus"
                      class="select"
                      [(ngModel)]="supForm.status"
                    >
                      <option value="ACTIVE">Active (Receive notifications)</option>
                      <option value="INACTIVE">Inactive (Paused)</option>
                    </select>
                  </div>
                </div>

                <div class="preview-box">
                  <span>
                    Requirement: provide at least <strong>Email Address</strong> or
                    <strong>Phone Number</strong>. Name and the remaining field are optional.
                  </span>
                </div>
              </div>

              <div class="modal-footer">
                <button type="button" class="btn" (click)="closeSupervisorDialog()">Cancel</button>
                <button type="submit" class="btn btn-primary">
                  <app-icon name="check" />
                  <span>{{ editingSupId() ? 'Save Changes' : 'Add Supervisor' }}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- New / Edit Billing Plan Modal -->
      @if (dialogOpen()) {
        <div class="modal-backdrop">
          <div
            class="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="plan-dialog-title"
          >
            <div class="modal-header">
              <h2 id="plan-dialog-title">
                {{ editingId() ? 'Edit billing plan' : 'New billing plan' }}
              </h2>
              <button type="button" class="btn btn-sm btn-ghost" (click)="closeDialog()">
                <app-icon name="close" />
              </button>
            </div>

            <form (ngSubmit)="submitPlan()">
              <div class="modal-body">
                <div class="form-grid">
                  <div class="field field-full">
                    <label for="plan-client">Client *</label>
                    <select
                      id="plan-client"
                      name="clientRef"
                      class="select"
                      required
                      [disabled]="Boolean(editingId())"
                      [(ngModel)]="form.clientRef"
                    >
                      @for (c of availableClients(); track c.clientRef) {
                        <option [value]="c.clientRef">
                          {{ c.clientName }} ({{ c.clientCode }})
                        </option>
                      }
                    </select>
                  </div>

                  <div class="field field-full">
                    <label>Billing model *</label>
                    <div class="segments" role="radiogroup" aria-label="Billing model">
                      <button
                        type="button"
                        class="seg-btn"
                        [class.active]="form.billingModel === 'COMMISSION'"
                        (click)="form.billingModel = 'COMMISSION'"
                      >
                        <app-icon name="fileText" />
                        <span>Commission per transaction</span>
                      </button>
                      <button
                        type="button"
                        class="seg-btn"
                        [class.active]="form.billingModel === 'MONTHLY_FLAT'"
                        (click)="form.billingModel = 'MONTHLY_FLAT'"
                      >
                        <app-icon name="clock" />
                        <span>End-of-month agreed amount</span>
                      </button>
                    </div>
                  </div>

                  @if (form.billingModel === 'MONTHLY_FLAT') {
                    <div class="field">
                      <label for="plan-monthly">Agreed monthly amount *</label>
                      <div class="input-suffix">
                        <input
                          id="plan-monthly"
                          type="number"
                          min="1"
                          name="monthlyAmount"
                          class="input"
                          required
                          [(ngModel)]="form.monthlyAmount"
                          placeholder="e.g. 300000"
                        />
                        <span>{{ form.currency }}</span>
                      </div>
                    </div>
                  } @else {
                    <div class="field">
                      <label for="plan-rate">Percentage per transaction *</label>
                      <div class="input-suffix">
                        <input
                          id="plan-rate"
                          type="number"
                          min="0"
                          max="100"
                          step="0.01"
                          name="percentageRate"
                          class="input"
                          required
                          [(ngModel)]="form.percentageRate"
                          placeholder="e.g. 5"
                        />
                        <span>%</span>
                      </div>
                    </div>
                  }

                  <div class="field">
                    <label for="plan-currency">Currency</label>
                    <select
                      id="plan-currency"
                      name="currency"
                      class="select"
                      [(ngModel)]="form.currency"
                    >
                      @for (cur of currencies; track cur) {
                        <option [value]="cur">{{ cur }}</option>
                      }
                    </select>
                  </div>

                  @if (form.billingModel === 'COMMISSION') {
                    <div class="field">
                      <label for="plan-threshold">Low-balance alert at</label>
                      <div class="input-suffix">
                        <input
                          id="plan-threshold"
                          type="number"
                          min="0"
                          name="lowBalanceThreshold"
                          class="input"
                          [(ngModel)]="form.lowBalanceThreshold"
                          placeholder="Optional"
                        />
                        <span>{{ form.currency }}</span>
                      </div>
                    </div>
                  }

                  <div class="field">
                    <label for="plan-status">Status</label>
                    <select
                      id="plan-status"
                      name="status"
                      class="select"
                      [(ngModel)]="form.status"
                    >
                      <option value="ACTIVE">Active</option>
                      <option value="SUSPENDED">Suspended</option>
                    </select>
                  </div>

                  <div class="field field-full">
                    <label for="plan-notes">Notes</label>
                    <textarea
                      id="plan-notes"
                      name="notes"
                      class="textarea"
                      rows="2"
                      [(ngModel)]="form.notes"
                      placeholder="Optional (e.g. agreed billing terms or due date)"
                    ></textarea>
                  </div>
                </div>

                <!-- Live Example Preview -->
                <div class="preview-box">
                  @if (form.billingModel === 'MONTHLY_FLAT') {
                    <span>Client pays at the end of every month (0% per transaction):</span>
                    <strong>{{ formatMoney(form.monthlyAmount || 0, form.currency) }} / month</strong>
                  } @else {
                    <span>A 100,000 {{ form.currency }} transaction is charged</span>
                    <strong>
                      {{
                        formatMoney(
                          Math.round((100000 * (form.percentageRate || 0)) / 100),
                          form.currency
                        )
                      }}
                    </strong>
                  }
                </div>
              </div>

              <div class="modal-footer">
                <button type="button" class="btn" (click)="closeDialog()">Cancel</button>
                <button type="submit" class="btn btn-primary">
                  <app-icon name="check" />
                  <span>{{ editingId() ? 'Save changes' : 'Create plan' }}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
})
export class Configurations {
  protected readonly store = inject(StaffTrackStore);
  private readonly confirm = inject(ConfirmService);
  protected readonly Math = Math;
  protected readonly Boolean = Boolean;

  protected readonly activeTab = signal<ConfigTab>('SUPERVISORS');

  // ---------- Notification Supervisors State ----------
  protected readonly supSearchQuery = signal('');
  protected readonly supCategoryFilter = signal<'ALL' | SupervisorCategory>('ALL');
  protected readonly supStatusFilter = signal<'ALL' | SupervisorNotificationStatus>('ALL');
  protected readonly supDialogOpen = signal(false);
  protected readonly editingSupId = signal<string | undefined>(undefined);
  protected readonly selectedSupStaffId = signal('');
  protected readonly supFormError = signal('');
  protected readonly supPageSize = signal(10);
  protected readonly supPage = signal(0);

  protected supForm = {
    clientRef: '',
    name: '',
    email: '',
    phone: '',
    category: 'UNIT_HEAD' as SupervisorCategory,
    department: '',
    status: 'ACTIVE' as SupervisorNotificationStatus,
  };

  protected readonly filteredSupervisors = computed(() => {
    const q = this.supSearchQuery().trim().toLowerCase();
    const cat = this.supCategoryFilter();
    const st = this.supStatusFilter();
    return this.store.notificationSupervisors().filter((s) => {
      const supCat: SupervisorCategory = s.category === 'UNIT_HEAD' ? 'UNIT_HEAD' : 'OVERALL_HEAD';
      if (cat !== 'ALL' && supCat !== cat) return false;
      if (st !== 'ALL' && s.status !== st) return false;
      if (!q) return true;
      return (
        (s.entityName || '').toLowerCase().includes(q) ||
        (s.entityCode || '').toLowerCase().includes(q) ||
        (s.name || '').toLowerCase().includes(q) ||
        (s.email || '').toLowerCase().includes(q) ||
        (s.phone || '').toLowerCase().includes(q)
      );
    });
  });

  protected readonly pagedSupervisors = computed(() => {
    const size = this.supPageSize();
    const list = this.filteredSupervisors();
    const maxPage = Math.max(0, Math.ceil(list.length / size) - 1);
    const start = Math.min(this.supPage(), maxPage) * size;
    return list.slice(start, start + size);
  });

  protected openCreateSupervisor(): void {
    this.editingSupId.set(undefined);
    this.selectedSupStaffId.set('');
    this.supFormError.set('');
    const firstClient = this.availableClients()[0];
    this.supForm = {
      clientRef: firstClient?.clientRef ?? '',
      name: '',
      email: '',
      phone: '',
      category: 'UNIT_HEAD',
      department: '',
      status: 'ACTIVE',
    };
    this.supDialogOpen.set(true);
  }

  protected openEditSupervisor(sup: NotificationSupervisor): void {
    this.editingSupId.set(sup.id);
    this.selectedSupStaffId.set('');
    this.supFormError.set('');
    const matchedClient = this.availableClients().find(
      (c) =>
        (sup.clientRef && c.clientRef === sup.clientRef) ||
        (sup.entityCode && c.clientCode.toLowerCase() === sup.entityCode.toLowerCase()) ||
        (sup.entityName && c.clientName.toLowerCase() === sup.entityName.toLowerCase())
    );
    this.supForm = {
      clientRef: matchedClient?.clientRef || sup.clientRef || '',
      name: sup.name || '',
      email: sup.email || '',
      phone: sup.phone || '',
      category: sup.category === 'UNIT_HEAD' ? 'UNIT_HEAD' : 'OVERALL_HEAD',
      department: sup.department || '',
      status: sup.status || 'ACTIVE',
    };
    this.supDialogOpen.set(true);
  }

  protected closeSupervisorDialog(): void {
    this.supDialogOpen.set(false);
    this.supFormError.set('');
  }

  protected onSupervisorStaffPick(staffId: string): void {
    this.selectedSupStaffId.set(staffId);
    if (!staffId) return;
    const member = this.store.staffMembers().find((s) => s.id === staffId);
    if (!member) return;
    this.supForm.name = `${member.surname} ${member.lastName}`.trim();
    this.supForm.email = member.email || '';
    this.supForm.phone = member.phone || '';
    this.supForm.department = member.department || '';
    const matchedDeptClient = this.availableClients().find(
      (c) => c.clientName.toLowerCase() === member.department.toLowerCase()
    );
    if (matchedDeptClient) {
      this.supForm.clientRef = matchedDeptClient.clientRef;
    }
    this.supFormError.set('');
  }

  protected submitSupervisor(): void {
    const cleanName = this.supForm.name.trim();
    const cleanEmail = this.supForm.email.trim().toLowerCase();
    const cleanPhone = this.supForm.phone.trim();

    if (!cleanEmail && !cleanPhone) {
      this.supFormError.set(
        'Please provide at least an email address or a phone number. Once either is provided, the remaining fields are optional.'
      );
      return;
    }

    const matchedClient = this.availableClients().find(
      (c) => c.clientRef === this.supForm.clientRef
    );

    this.store.saveNotificationSupervisor(
      {
        clientRef: matchedClient?.clientRef || undefined,
        entityName: matchedClient?.clientName || 'Office of the Prime Minister (OPM)',
        entityCode: matchedClient?.clientCode || 'OPM',
        name: cleanName || undefined,
        email: cleanEmail || undefined,
        phone: cleanPhone || undefined,
        category: this.supForm.category,
        department: this.supForm.department.trim() || undefined,
        status: this.supForm.status,
      },
      this.editingSupId()
    );
    this.closeSupervisorDialog();
  }

  protected toggleSupervisorStatus(sup: NotificationSupervisor): void {
    const next: SupervisorNotificationStatus = sup.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.store.updateNotificationSupervisorStatus(sup.id, next);
  }

  protected removeSupervisor(sup: NotificationSupervisor): void {
    this.store.deleteNotificationSupervisor(sup.id);
  }

  // ---------- Notification & OTP Channel Config Actions ----------
  protected toggleChannelActive(ch: NotificationChannelConfig): void {
    const nextActive = !ch.active;
    this.store.updateChannelConfig(ch.channelCode, {
      active: nextActive,
      otpEnabled: nextActive,
      supervisorAlertEnabled: nextActive && ch.channelCode !== 'SMS' ? true : nextActive,
    });
  }

  protected toggleChannelOtp(ch: NotificationChannelConfig, enabled: boolean): void {
    this.store.updateChannelConfig(ch.channelCode, { otpEnabled: enabled });
  }

  protected toggleChannelSupervisorAlert(ch: NotificationChannelConfig, enabled: boolean): void {
    this.store.updateChannelConfig(ch.channelCode, { supervisorAlertEnabled: enabled });
  }

  // ---------- Client Billing Plans State ----------
  protected readonly availableClients = computed<ClientOption[]>(() => {
    const orgs = this.store.organisations().map((o) => ({
      clientRef: o.id,
      clientName: o.name,
      clientCode: o.code,
    }));
    const depts = this.store.departmentItems().map((d) => ({
      clientRef: d.id,
      clientName: d.name,
      clientCode: d.code,
    }));
    return [...orgs, ...depts];
  });
  protected readonly currencies: Array<'UGX' | 'KES' | 'USD'> = ['UGX', 'KES', 'USD'];

  protected readonly searchQuery = signal('');
  protected readonly statusFilter = signal<'ALL' | BillingPlanStatus>('ALL');
  protected readonly notice = signal('');
  protected readonly dialogOpen = signal(false);
  protected readonly editingId = signal<string | undefined>(undefined);
  protected readonly pageSize = signal(10);
  protected readonly page = signal(0);

  protected form = {
    clientRef: '',
    billingModel: 'COMMISSION' as BillingModel,
    percentageRate: 5,
    monthlyAmount: 300000,
    currency: 'UGX' as 'UGX' | 'KES' | 'USD',
    lowBalanceThreshold: 100000 as number | undefined,
    status: 'ACTIVE' as BillingPlanStatus,
    notes: '',
  };

  protected readonly filteredPlans = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const st = this.statusFilter();
    return this.store.billingPlans().filter((p) => {
      if (st !== 'ALL' && p.status !== st) return false;
      if (!q) return true;
      return (
        p.clientName.toLowerCase().includes(q) ||
        p.clientCode.toLowerCase().includes(q)
      );
    });
  });

  protected readonly pagedPlans = computed(() => {
    const size = this.pageSize();
    const list = this.filteredPlans();
    const maxPage = Math.max(0, Math.ceil(list.length / size) - 1);
    const start = Math.min(this.page(), maxPage) * size;
    return list.slice(start, start + size);
  });

  protected formatMoney(amount: number | undefined, currency = 'UGX'): string {
    if (amount === undefined || amount === null) return '—';
    return `${Number(amount).toLocaleString('en-UG')} ${currency}`;
  }

  protected toggleStatus(plan: ClientBillingPlan): void {
    const next: BillingPlanStatus = plan.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    this.store.updateBillingPlanStatus(plan.id, next);
    this.notice.set(
      `Billing for ${plan.clientName} is now ${next === 'ACTIVE' ? 'active' : 'suspended'}.`
    );
  }

  protected removePlan(plan: ClientBillingPlan): void {
    this.confirm.ask({
      title: 'Remove Billing Plan',
      message: `Are you sure you want to remove the billing plan for "${plan.clientName}"?`,
      confirmLabel: 'Remove Plan',
      onConfirm: () => {
        this.store.deleteBillingPlan(plan.id);
        this.notice.set(`Billing plan for ${plan.clientName} was removed.`);
      },
    });
  }

  protected openCreate(): void {
    this.editingId.set(undefined);
    const firstClient = this.availableClients()[0];
    this.form = {
      clientRef: firstClient?.clientRef ?? '',
      billingModel: 'COMMISSION',
      percentageRate: 5,
      monthlyAmount: 300000,
      currency: 'UGX',
      lowBalanceThreshold: 100000,
      status: 'ACTIVE',
      notes: '',
    };
    this.dialogOpen.set(true);
  }

  protected openEdit(plan: ClientBillingPlan): void {
    this.editingId.set(plan.id);
    this.form = {
      clientRef: plan.clientRef,
      billingModel: plan.billingModel,
      percentageRate: plan.percentageRate ?? 5,
      monthlyAmount: plan.monthlyAmount ?? 300000,
      currency: plan.currency,
      lowBalanceThreshold: plan.lowBalanceThreshold,
      status: plan.status,
      notes: plan.notes ?? '',
    };
    this.dialogOpen.set(true);
  }

  protected closeDialog(): void {
    this.dialogOpen.set(false);
  }

  protected submitPlan(): void {
    const clients = this.availableClients();
    const client = clients.find((c) => c.clientRef === this.form.clientRef) ?? clients[0];
    if (!client) return;
    const isMonthly = this.form.billingModel === 'MONTHLY_FLAT';

    const saved = this.store.saveBillingPlan(
      {
        clientRef: client.clientRef,
        clientName: client.clientName,
        clientCode: client.clientCode,
        billingModel: this.form.billingModel,
        percentageRate: isMonthly ? 0 : Number(this.form.percentageRate || 0),
        monthlyAmount: isMonthly ? Number(this.form.monthlyAmount || 0) : undefined,
        currency: this.form.currency,
        lowBalanceThreshold:
          !isMonthly && this.form.lowBalanceThreshold != null
            ? Number(this.form.lowBalanceThreshold)
            : undefined,
        status: this.form.status,
        notes: this.form.notes.trim() || undefined,
      },
      this.editingId()
    );

    const summary =
      saved.billingModel === 'MONTHLY_FLAT'
        ? `${this.formatMoney(saved.monthlyAmount, saved.currency)} per end of month`
        : `${saved.percentageRate}% per transaction`;
    this.notice.set(
      `Billing plan for ${saved.clientName} ${
        this.editingId() ? 'updated' : 'created'
      } — ${summary}.`
    );
    this.closeDialog();
  }
}
