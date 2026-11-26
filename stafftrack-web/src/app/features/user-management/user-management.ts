import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  AccountCategory,
  ActivationStatus,
  ClientUserLink,
  SystemUserAccount,
  USER_ROLE_LABELS,
  USER_STATUS_LABELS,
  UserAccountStatus,
  UserOtpChannel,
  UserSystemRole,
} from '../../core/models/stafftrack.models';
import { StaffTrackStore } from '../../core/services/stafftrack.store';
import { Icon } from '../../shared/icon/icon';
import { Pagination } from '../../shared/pagination/pagination';

@Component({
  selector: 'app-user-management',
  imports: [FormsModule, Icon, Pagination],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .tab-bar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
    }
    .tabs {
      display: inline-flex;
      gap: 0.25rem;
      padding: 0.25rem;
      border-radius: 12px;
      border: 1px solid var(--border);
      background: var(--bg-subtle);
    }
    .tab {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      padding: 0.5rem 0.95rem;
      border: 0;
      border-radius: 9px;
      font: inherit;
      font-size: 0.88rem;
      font-weight: 600;
      color: var(--text-muted);
      background: transparent;
      cursor: pointer;
    }
    .tab.active {
      color: var(--text);
      background: var(--bg-elevated);
      box-shadow: var(--shadow-sm);
    }
    .user-cell {
      display: flex;
      align-items: center;
      gap: 0.7rem;
    }
    .user-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      background: var(--gradient-brand);
      color: #ffffff;
      font-size: 0.78rem;
      font-weight: 700;
      flex: none;
    }
    .cell-stack {
      display: grid;
      gap: 0.14rem;
    }
    .cell-stack small {
      color: var(--text-muted);
      font-size: 0.76rem;
    }
    .channel-pill {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      font-size: 0.8rem;
      font-weight: 600;
      color: var(--text);
    }
    .default-pill {
      display: inline-flex;
      padding: 0.15rem 0.55rem;
      border-radius: 999px;
      font-size: 0.73rem;
      font-weight: 700;
      color: var(--purple);
      background: var(--purple-bg);
    }
    .action-btns {
      display: flex;
      align-items: center;
      gap: 0.35rem;
      white-space: nowrap;
    }
    .icon-action-btn {
      width: 34px;
      height: 34px;
      min-height: 34px;
      padding: 0;
    }
    .details-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 0.85rem 1rem;
      margin: 0;
    }
    .details-grid dt {
      font-size: 0.72rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      font-weight: 700;
    }
    .details-grid dd {
      margin: 0.15rem 0 0;
      font-weight: 600;
      word-break: break-word;
    }
    .org-list {
      display: grid;
      gap: 0.55rem;
    }
    .org-item {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.7rem 0.9rem;
      border: 1px solid var(--border);
      border-radius: var(--radius);
      background: var(--bg-subtle);
    }
    .org-item.is-default {
      border-color: var(--brand-500);
      background: var(--info-bg);
    }
  `,
  template: `
    <div class="page">
      <header class="page-header">
        <div class="page-title">
          <h1>Users</h1>
          <p>People who can sign in to SOOTR, and the clients / departments they can access.</p>
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
          @if (tab() === 'system') {
            <button type="button" class="btn btn-primary" (click)="openUserModal()">
              <app-icon name="plus" />
              <span>Assign System User</span>
            </button>
          } @else {
            <button type="button" class="btn btn-primary" (click)="openClientUserModal()">
              <app-icon name="plus" />
              <span>Assign Client User</span>
            </button>
          }
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

      <div class="tab-bar" role="tablist" aria-label="User lists">
        <div class="tabs">
          <button
            type="button"
            role="tab"
            class="tab"
            [class.active]="tab() === 'system'"
            [attr.aria-selected]="tab() === 'system'"
            (click)="tab.set('system')"
          >
            <app-icon name="shield" />
            <span>System users</span>
          </button>
          <button
            type="button"
            role="tab"
            class="tab"
            [class.active]="tab() === 'client'"
            [attr.aria-selected]="tab() === 'client'"
            (click)="tab.set('client')"
          >
            <app-icon name="users" />
            <span>Client users</span>
          </button>
        </div>
      </div>

      <!-- TAB 1: System users -->
      @if (tab() === 'system') {
        <section class="card">
          <div class="toolbar">
            <label class="search-box">
              <span class="search-icon"><app-icon name="search" /></span>
              <input
                type="search"
                class="input"
                placeholder="Search by username, email or phone"
                [ngModel]="searchQuery()"
                (ngModelChange)="searchQuery.set($event)"
              />
            </label>

            <select
              class="select filter-select"
              aria-label="Filter by account category"
              [ngModel]="roleFilter()"
              (ngModelChange)="roleFilter.set($event)"
            >
              <option value="ALL">All categories</option>
              @for (role of roles; track role) {
                <option [value]="role">{{ roleLabels[role] }}</option>
              }
            </select>

            <select
              class="select filter-select"
              aria-label="Filter by activation status"
              [ngModel]="statusFilter()"
              (ngModelChange)="statusFilter.set($event)"
            >
              <option value="ALL">All statuses</option>
              @for (st of statuses; track st) {
                <option [value]="st">{{ statusLabels[st] }}</option>
              }
            </select>
          </div>

          <div tabindex="0" role="region" aria-labelledby="users-table-caption" class="table-wrapper">
            <table class="data-table">
              <caption id="users-table-caption">
                System users with role, OTP channel, status, access, and last sign-in
              </caption>
              <thead>
                <tr>
                  <th>User</th>
                  <th>Role / Category</th>
                  <th>OTP channel</th>
                  <th>Status</th>
                  <th>Access</th>
                  <th>Last sign-in</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (user of pagedUsers(); track user.id) {
                  <tr>
                    <th>
                      <div class="user-cell">
                        <span class="user-avatar" aria-hidden="true">{{ initials(user.fullName) }}</span>
                        <div class="cell-stack">
                          <span>{{ user.email }}</span>
                          <small>{{ user.fullName }} · {{ user.phone }}</small>
                        </div>
                      </div>
                    </th>
                    <td data-label="Role / Category">
                      <span [class]="roleBadgeClass(user.role)">
                        {{ roleLabels[user.role] }}
                      </span>
                    </td>
                    <td data-label="OTP channel">
                      <span class="channel-pill">
                        <app-icon [name]="user.preferredOtpChannel === 'EMAIL' ? 'mail' : 'phone'" />
                        <span>{{ user.preferredOtpChannel }}</span>
                      </span>
                    </td>
                    <td data-label="Status">
                      <span [class]="statusBadgeClass(user.status)">
                        {{ statusLabels[user.status] }}
                      </span>
                    </td>
                    <td data-label="Access">
                      <span
                        [class]="
                          user.status === 'ACTIVE' ? 'badge badge-success' : 'badge badge-danger'
                        "
                      >
                        {{ user.status === 'ACTIVE' ? 'Enabled' : 'Disabled' }}
                      </span>
                    </td>
                    <td data-label="Last sign-in">
                      <small style="color: var(--text-muted)">
                        {{ user.lastLoginAt || 'Never' }}
                      </small>
                    </td>
                    <td data-label="Actions">
                      <div class="action-btns">
                        @if (user.status !== 'ACTIVE') {
                          <button
                            type="button"
                            class="btn btn-sm btn-success"
                            (click)="store.updateUserAccountStatus(user.id, 'ACTIVE')"
                          >
                            <app-icon name="check" />
                            <span>Activate</span>
                          </button>
                        } @else {
                          <button
                            type="button"
                            class="btn btn-sm icon-action-btn"
                            title="Suspend access"
                            aria-label="Suspend access"
                            (click)="store.updateUserAccountStatus(user.id, 'SUSPENDED')"
                          >
                            <app-icon name="power" />
                          </button>
                        }
                        <button
                          type="button"
                          class="btn btn-sm icon-action-btn"
                          title="Edit user"
                          aria-label="Edit user"
                          (click)="openUserModal(user)"
                        >
                          <app-icon name="edit" />
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm btn-danger icon-action-btn"
                          title="Delete user"
                          aria-label="Delete user"
                          (click)="store.deleteUserAccount(user.id)"
                        >
                          <app-icon name="trash" />
                        </button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="7" style="text-align: center; padding: 2.2rem; color: var(--text-muted)">
                      No system users found.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <app-pagination
            [page]="sysPage()"
            [size]="pageSize()"
            [total]="filteredUsers().length"
            (pageChange)="sysPage.set($event)"
            (sizeChange)="pageSize.set($event)"
          />
        </section>
      }

      <!-- TAB 2: Client users -->
      @if (tab() === 'client') {
        <section class="card">
          <div class="toolbar">
            <label class="search-box">
              <span class="search-icon"><app-icon name="search" /></span>
              <input
                type="search"
                class="input"
                placeholder="Search by phone, email or client"
                [ngModel]="cuSearch()"
                (ngModelChange)="cuSearch.set($event)"
              />
            </label>

            <select
              class="select filter-select"
              aria-label="Filter by activation status"
              [ngModel]="cuStatusFilter()"
              (ngModelChange)="cuStatusFilter.set($event)"
            >
              <option value="ALL">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="PENDING_ACTIVATION">Pending Activation</option>
              <option value="SUSPENDED">Suspended</option>
            </select>

            <select
              class="select filter-select"
              aria-label="Filter by default organisation"
              [ngModel]="cuDefaultFilter()"
              (ngModelChange)="cuDefaultFilter.set($event)"
            >
              <option value="ALL">Default or not</option>
              <option value="true">Default organisation</option>
              <option value="false">Other organisations</option>
            </select>
          </div>

          <div tabindex="0" role="region" aria-labelledby="client-users-caption" class="table-wrapper">
            <table class="data-table">
              <caption id="client-users-caption">
                Client users linked to organisations and their default organisation status
              </caption>
              <thead>
                <tr>
                  <th>User</th>
                  <th>Category</th>
                  <th>Client</th>
                  <th>Default</th>
                  <th>Status</th>
                  <th>Linked</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (cu of pagedClientUsers(); track cu.id) {
                  <tr>
                    <th>
                      <div class="user-cell">
                        <span class="user-avatar" aria-hidden="true">
                          {{ initials(cu.userEmail || cu.userPhoneNumber || 'U') }}
                        </span>
                        <div class="cell-stack">
                          <span>{{ cu.userPhoneNumber || cu.userEmail }}</span>
                          @if (cu.userPhoneNumber && cu.userEmail) {
                            <small>{{ cu.userEmail }}</small>
                          }
                        </div>
                      </div>
                    </th>
                    <td data-label="Category">
                      <span [class]="categoryBadgeClass(cu.accountCategory)">
                        {{ cu.accountCategory }}
                      </span>
                    </td>
                    <td data-label="Client">
                      <div class="cell-stack">
                        <strong>{{ cu.clientName }}</strong>
                        <small>{{ cu.clientCode }}</small>
                      </div>
                    </td>
                    <td data-label="Default">
                      @if (cu.isDefaultClient) {
                        <span class="default-pill">Default</span>
                      } @else {
                        <span style="color: var(--text-muted)">—</span>
                      }
                    </td>
                    <td data-label="Status">
                      <span [class]="cuStatusBadgeClass(cu.activationStatus)">
                        {{ cu.activationStatus }}
                      </span>
                    </td>
                    <td data-label="Linked">
                      <small style="color: var(--text-muted)">{{ cu.createdAt }}</small>
                    </td>
                    <td data-label="Actions">
                      <div class="action-btns">
                        @if (cu.activationStatus !== 'ACTIVE') {
                          <button
                            type="button"
                            class="btn btn-sm btn-primary"
                            (click)="store.updateClientUserStatus(cu.id, 'ACTIVE')"
                          >
                            Activate
                          </button>
                        }
                        <button
                          type="button"
                          class="btn btn-sm icon-action-btn"
                          title="View details"
                          aria-label="View details"
                          (click)="openClientUserDetails(cu)"
                        >
                          <app-icon name="eye" />
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm icon-action-btn"
                          [disabled]="cu.isDefaultClient"
                          [title]="
                            cu.isDefaultClient
                              ? 'Default organisation'
                              : 'Set as default organisation'
                          "
                          (click)="store.makeDefaultClientUser(cu.id)"
                        >
                          <app-icon name="check" />
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm icon-action-btn"
                          [title]="
                            cu.activationStatus === 'SUSPENDED'
                              ? 'Restore access'
                              : 'Suspend access'
                          "
                          (click)="toggleClientUserSuspend(cu)"
                        >
                          <app-icon name="power" />
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm btn-danger icon-action-btn"
                          title="Remove assignment"
                          aria-label="Remove assignment"
                          (click)="store.deleteClientUserLink(cu.id)"
                        >
                          <app-icon name="trash" />
                        </button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="7" style="text-align: center; padding: 2.2rem; color: var(--text-muted)">
                      No client users match the current filters.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <app-pagination
            [page]="cuPage()"
            [size]="pageSize()"
            [total]="filteredClientUsers().length"
            (pageChange)="cuPage.set($event)"
            (sizeChange)="pageSize.set($event)"
          />
        </section>
      }

      <!-- Client User Details Modal -->
      @if (viewingClientUser(); as v) {
        <div class="modal-backdrop">
          <div
            class="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="cu-details-title"
          >
            <div class="modal-header">
              <div class="user-cell">
                <span class="user-avatar" aria-hidden="true">
                  {{ initials(v.userEmail || v.userPhoneNumber || 'U') }}
                </span>
                <div class="cell-stack">
                  <h2 id="cu-details-title">{{ v.userPhoneNumber || v.userEmail }}</h2>
                  <small>{{ v.systemUserId ? 'Has a sign-in account' : 'No sign-in account' }}</small>
                </div>
              </div>
              <button
                type="button"
                class="btn btn-sm btn-ghost"
                (click)="viewingClientUser.set(null)"
              >
                <app-icon name="close" />
              </button>
            </div>

            <div class="modal-body">
              <dl class="details-grid">
                <div>
                  <dt>Phone</dt>
                  <dd>{{ v.userPhoneNumber || '—' }}</dd>
                </div>
                <div>
                  <dt>Email</dt>
                  <dd>{{ v.userEmail || '—' }}</dd>
                </div>
                <div>
                  <dt>Account category</dt>
                  <dd>
                    <span [class]="categoryBadgeClass(v.accountCategory)">
                      {{ v.accountCategory }}
                    </span>
                  </dd>
                </div>
                <div>
                  <dt>Client</dt>
                  <dd>{{ v.clientName }}</dd>
                </div>
                <div>
                  <dt>Status</dt>
                  <dd>
                    <span [class]="cuStatusBadgeClass(v.activationStatus)">
                      {{ v.activationStatus }}
                    </span>
                  </dd>
                </div>
                <div>
                  <dt>Linked</dt>
                  <dd>{{ v.createdAt }}</dd>
                </div>
              </dl>

              <strong style="font-size: 0.76rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted)">
                Organisations this user can access
              </strong>
              <div class="org-list">
                @for (org of userOrgs(); track org.id) {
                  <div class="org-item" [class.is-default]="org.isDefaultClient">
                    <div class="cell-stack">
                      <strong>{{ org.clientName }}</strong>
                      <small>{{ org.clientCode }}</small>
                    </div>
                    <div style="display: flex; align-items: center; gap: 0.45rem; flex-wrap: wrap">
                      <span [class]="cuStatusBadgeClass(org.activationStatus)">
                        {{ org.activationStatus }}
                      </span>
                      @if (org.isDefaultClient) {
                        <span class="default-pill">Default</span>
                      } @else {
                        <button
                          type="button"
                          class="btn btn-sm"
                          (click)="store.makeDefaultClientUser(org.id)"
                        >
                          Set default
                        </button>
                      }
                      <button
                        type="button"
                        class="btn btn-sm"
                        (click)="toggleClientUserSuspend(org)"
                      >
                        {{ org.activationStatus === 'SUSPENDED' ? 'Restore' : 'Suspend' }}
                      </button>
                    </div>
                  </div>
                }
              </div>
            </div>

            <div class="modal-footer">
              <button
                type="button"
                class="btn btn-primary"
                (click)="viewingClientUser.set(null)"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      }

      <!-- Add / Edit System User Modal (No backdrop click dismissal) -->
      @if (modalOpen()) {
        <div class="modal-backdrop">
          <div
            class="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="user-modal-title"
          >
            <div class="modal-header">
              <h2 id="user-modal-title">
                {{ editingId() ? 'Update Assigned System User' : 'Assign User to User Management' }}
              </h2>
              <button type="button" class="btn btn-sm btn-ghost" (click)="closeModal()">
                <app-icon name="close" />
              </button>
            </div>

            <form (ngSubmit)="saveUser()">
              <div class="modal-body">
                <div class="form-grid">
                  <div class="field field-full">
                    <label for="usr-staff-link">Select Registered Staff Member *</label>
                    <select
                      id="usr-staff-link"
                      name="linkedStaffId"
                      class="select"
                      [ngModel]="selectedStaffId"
                      (ngModelChange)="onStaffLinkChange($event)"
                    >
                      @for (s of store.staffMembers(); track s.id) {
                        <option [value]="s.id">
                          {{ s.surname }} {{ s.lastName }} — {{ s.designation }} ({{ s.email }} · {{ s.department }})
                          {{ isStaffAssigned(s.id, s.email) ? ' [Already Assigned]' : '' }}
                        </option>
                      }
                      <option value="__CUSTOM__">-- Enter custom user details manually --</option>
                    </select>
                  </div>

                  <div class="field">
                    <label for="usr-fullname">Full Name *</label>
                    <input
                      id="usr-fullname"
                      name="fullName"
                      class="input"
                      required
                      [(ngModel)]="form.fullName"
                      placeholder="e.g. Kasoma Fredrick"
                    />
                  </div>

                  <div class="field">
                    <label for="usr-designation">Designation *</label>
                    <select
                      id="usr-designation"
                      name="designation"
                      class="select"
                      required
                      [(ngModel)]="form.designation"
                    >
                      @for (des of store.designations(); track des) {
                        <option [value]="des">{{ des }}</option>
                      }
                    </select>
                  </div>

                  <div class="field">
                    <label for="usr-email">Email Address (Sign-in) *</label>
                    <input
                      id="usr-email"
                      type="email"
                      name="email"
                      class="input"
                      required
                      [(ngModel)]="form.email"
                      placeholder="officer@opm.go.ug"
                    />
                  </div>

                  <div class="field">
                    <label for="usr-phone">Phone Number (OTP Sign-in) *</label>
                    <input
                      id="usr-phone"
                      type="tel"
                      name="phone"
                      class="input"
                      required
                      [(ngModel)]="form.phone"
                      placeholder="+256 700 408 387"
                    />
                  </div>

                  <div class="field">
                    <label for="usr-dept">Department / Unit *</label>
                    <select
                      id="usr-dept"
                      name="department"
                      class="select"
                      required
                      [(ngModel)]="form.department"
                    >
                      @for (dept of store.departments(); track dept) {
                        <option [value]="dept">{{ dept }}</option>
                      }
                    </select>
                  </div>

                  <div class="field">
                    <label for="usr-station">Duty Station *</label>
                    <select
                      id="usr-station"
                      name="dutyStation"
                      class="select"
                      required
                      [(ngModel)]="form.dutyStation"
                    >
                      @for (st of store.dutyStations(); track st) {
                        <option [value]="st">{{ st }}</option>
                      }
                    </select>
                  </div>

                  <div class="field">
                    <label for="usr-role">Role / Category *</label>
                    <select
                      id="usr-role"
                      name="role"
                      class="select"
                      required
                      [(ngModel)]="form.role"
                    >
                      @for (role of roles; track role) {
                        <option [value]="role">{{ roleLabels[role] }}</option>
                      }
                    </select>
                  </div>

                  <div class="field">
                    <label for="usr-otp">Preferred OTP Channel *</label>
                    <select
                      id="usr-otp"
                      name="preferredOtpChannel"
                      class="select"
                      required
                      [(ngModel)]="form.preferredOtpChannel"
                    >
                      @for (ch of otpChannels; track ch) {
                        <option [value]="ch">{{ ch }}</option>
                      }
                    </select>
                  </div>
                </div>
              </div>

              <div class="modal-footer">
                <button type="button" class="btn" (click)="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">
                  <app-icon name="check" />
                  <span>{{ editingId() ? 'Save Changes' : 'Assign User' }}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Assign Client / Department User Modal (No backdrop click dismissal) -->
      @if (clientModalOpen()) {
        <div class="modal-backdrop">
          <div
            class="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="cu-modal-title"
          >
            <div class="modal-header">
              <h2 id="cu-modal-title">Assign User to Client / Department</h2>
              <button type="button" class="btn btn-sm btn-ghost" (click)="clientModalOpen.set(false)">
                <app-icon name="close" />
              </button>
            </div>

            <form (ngSubmit)="saveClientUserAssignment()">
              <div class="modal-body">
                <div class="form-grid">
                  <div class="field field-full">
                    <label for="cu-person-select">Select User / Staff Member *</label>
                    <select
                      id="cu-person-select"
                      name="selectedPersonKey"
                      class="select"
                      required
                      [ngModel]="cuForm.selectedPersonKey"
                      (ngModelChange)="onClientPersonSelect($event)"
                    >
                      @for (p of assignablePeople(); track p.key) {
                        <option [value]="p.key">{{ p.label }}</option>
                      }
                    </select>
                  </div>

                  <div class="field">
                    <label for="cu-client-select">Organisation / Department *</label>
                    <select
                      id="cu-client-select"
                      name="clientRef"
                      class="select"
                      required
                      [(ngModel)]="cuForm.clientRef"
                    >
                      @for (org of assignableOrganisations(); track org.id) {
                        <option [value]="org.id">{{ org.name }} ({{ org.code }})</option>
                      }
                    </select>
                  </div>

                  <div class="field">
                    <label for="cu-category-select">Account Category *</label>
                    <select
                      id="cu-category-select"
                      name="accountCategory"
                      class="select"
                      required
                      [(ngModel)]="cuForm.accountCategory"
                    >
                      @for (cat of accountCategories; track cat) {
                        <option [value]="cat">{{ cat }}</option>
                      }
                    </select>
                  </div>

                  <div class="field">
                    <label for="cu-status-select">Activation Status *</label>
                    <select
                      id="cu-status-select"
                      name="activationStatus"
                      class="select"
                      required
                      [(ngModel)]="cuForm.activationStatus"
                    >
                      <option value="ACTIVE">ACTIVE</option>
                      <option value="PENDING_ACTIVATION">PENDING_ACTIVATION</option>
                      <option value="SUSPENDED">SUSPENDED</option>
                    </select>
                  </div>

                  <div class="field">
                    <label for="cu-default-select">Default Organisation</label>
                    <select
                      id="cu-default-select"
                      name="isDefaultClient"
                      class="select"
                      [(ngModel)]="cuForm.isDefaultClient"
                    >
                      <option [ngValue]="true">Yes — Set as Default Organisation</option>
                      <option [ngValue]="false">No — Secondary Organisation</option>
                    </select>
                  </div>
                </div>
              </div>

              <div class="modal-footer">
                <button type="button" class="btn" (click)="clientModalOpen.set(false)">Cancel</button>
                <button type="submit" class="btn btn-primary">
                  <app-icon name="check" />
                  <span>Assign to Client / Department</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
})
export class UserManagement {
  protected readonly store = inject(StaffTrackStore);

  protected readonly tab = signal<'system' | 'client'>('system');

  protected readonly roleLabels = USER_ROLE_LABELS;
  protected readonly statusLabels = USER_STATUS_LABELS;
  protected readonly roles: UserSystemRole[] = [
    'SUPER_ADMIN',
    'DEPARTMENT_HEAD',
    'DESK_OFFICER',
    'STAFF_OFFICER',
  ];
  protected readonly statuses: UserAccountStatus[] = [
    'ACTIVE',
    'PENDING_ACTIVATION',
    'SUSPENDED',
  ];
  protected readonly otpChannels: UserOtpChannel[] = ['EMAIL', 'SMS', 'WHATSAPP'];

  protected readonly searchQuery = signal('');
  protected readonly roleFilter = signal('ALL');
  protected readonly statusFilter = signal('ALL');

  protected readonly cuSearch = signal('');
  protected readonly cuStatusFilter = signal('ALL');
  protected readonly cuDefaultFilter = signal('ALL');
  protected readonly viewingClientUser = signal<ClientUserLink | null>(null);

  protected readonly pageSize = signal(10);
  protected readonly sysPage = signal(0);
  protected readonly cuPage = signal(0);

  protected readonly modalOpen = signal(false);
  protected readonly clientModalOpen = signal(false);
  protected readonly editingId = signal<string | undefined>(undefined);
  protected selectedStaffId = '';
  protected readonly accountCategories: AccountCategory[] = [
    'SUPER_ADMIN',
    'CLIENT_ADMIN',
    'USER',
  ];

  protected readonly assignableOrganisations = computed(() => {
    const items: Array<{ id: string; code: string; name: string }> = [];
    for (const org of this.store.organisations()) {
      items.push({ id: org.id, code: org.code, name: org.name });
    }
    for (const dept of this.store.departmentItems()) {
      if (!items.some((i) => i.code.toLowerCase() === dept.code.toLowerCase())) {
        items.push({ id: dept.id, code: dept.code, name: dept.name });
      }
    }
    return items;
  });

  protected cuForm = {
    selectedPersonKey: '',
    systemUserId: '' as string | undefined,
    userEmail: '',
    userPhoneNumber: '',
    clientRef: '',
    accountCategory: 'USER' as AccountCategory,
    activationStatus: 'ACTIVE' as ActivationStatus,
    isDefaultClient: true,
  };

  protected readonly assignablePeople = computed(() => {
    const map = new Map<
      string,
      {
        key: string;
        systemUserId?: string;
        email: string;
        phone: string;
        fullName: string;
        department: string;
        label: string;
      }
    >();

    for (const u of this.store.userAccounts()) {
      const key = u.email.toLowerCase();
      map.set(key, {
        key,
        systemUserId: u.id,
        email: u.email,
        phone: u.phone,
        fullName: u.fullName,
        department: u.department,
        label: `${u.fullName} — ${u.email} (${this.roleLabels[u.role]})`,
      });
    }

    for (const s of this.store.staffMembers()) {
      const key = s.email.toLowerCase();
      if (!map.has(key)) {
        map.set(key, {
          key,
          email: s.email,
          phone: s.phone,
          fullName: `${s.surname} ${s.lastName}`,
          department: s.department,
          label: `${s.surname} ${s.lastName} — ${s.email} (${s.designation} · Staff Register)`,
        });
      }
    }

    return Array.from(map.values());
  });

  protected form = {
    fullName: '',
    email: '',
    phone: '+256',
    designation: 'Principal Officer',
    department: 'Department of Refugees',
    dutyStation: 'OPM Main',
    role: 'STAFF_OFFICER' as UserSystemRole,
    preferredOtpChannel: 'EMAIL' as UserOtpChannel,
    status: 'ACTIVE' as UserAccountStatus,
  };

  protected readonly filteredUsers = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const role = this.roleFilter();
    const status = this.statusFilter();

    return this.store.userAccounts().filter((u) => {
      if (role !== 'ALL' && u.role !== role) return false;
      if (status !== 'ALL' && u.status !== status) return false;
      if (!q) return true;
      return (
        u.fullName.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        u.phone.toLowerCase().includes(q) ||
        u.department.toLowerCase().includes(q) ||
        u.dutyStation.toLowerCase().includes(q)
      );
    });
  });

  protected readonly filteredClientUsers = computed(() => {
    const q = this.cuSearch().trim().toLowerCase();
    const st = this.cuStatusFilter();
    const def = this.cuDefaultFilter();

    return this.store.clientUserLinks().filter((cu) => {
      if (st !== 'ALL' && cu.activationStatus !== st) return false;
      if (def === 'true' && !cu.isDefaultClient) return false;
      if (def === 'false' && cu.isDefaultClient) return false;
      if (!q) return true;
      return (
        (cu.userEmail?.toLowerCase().includes(q) ?? false) ||
        (cu.userPhoneNumber?.toLowerCase().includes(q) ?? false) ||
        cu.clientName.toLowerCase().includes(q) ||
        cu.clientCode.toLowerCase().includes(q)
      );
    });
  });

  protected readonly pagedUsers = computed(() => {
    const size = this.pageSize();
    const list = this.filteredUsers();
    const maxPage = Math.max(0, Math.ceil(list.length / size) - 1);
    const start = Math.min(this.sysPage(), maxPage) * size;
    return list.slice(start, start + size);
  });

  protected readonly pagedClientUsers = computed(() => {
    const size = this.pageSize();
    const list = this.filteredClientUsers();
    const maxPage = Math.max(0, Math.ceil(list.length / size) - 1);
    const start = Math.min(this.cuPage(), maxPage) * size;
    return list.slice(start, start + size);
  });

  protected readonly userOrgs = computed(() => {
    const current = this.viewingClientUser();
    if (!current) return [];
    return this.store
      .clientUserLinks()
      .filter(
        (cu) =>
          (current.userEmail && cu.userEmail === current.userEmail) ||
          (current.userPhoneNumber && cu.userPhoneNumber === current.userPhoneNumber)
      );
  });

  protected initials(name: string): string {
    return (
      name
        .split(/[\s@._]+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((p) => p[0]?.toUpperCase() ?? '')
        .join('') || 'U'
    );
  }

  protected roleBadgeClass(role: UserSystemRole): string {
    switch (role) {
      case 'SUPER_ADMIN':
        return 'badge badge-purple';
      case 'DEPARTMENT_HEAD':
        return 'badge badge-info';
      case 'DESK_OFFICER':
        return 'badge badge-warning';
      default:
        return 'badge';
    }
  }

  protected categoryBadgeClass(cat: AccountCategory): string {
    switch (cat) {
      case 'SUPER_ADMIN':
        return 'badge badge-purple';
      case 'CLIENT_ADMIN':
        return 'badge badge-info';
      default:
        return 'badge';
    }
  }

  protected statusBadgeClass(status: UserAccountStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'badge badge-success';
      case 'PENDING_ACTIVATION':
        return 'badge badge-warning';
      case 'SUSPENDED':
        return 'badge badge-danger';
      default:
        return 'badge';
    }
  }

  protected cuStatusBadgeClass(status: ActivationStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'badge badge-success';
      case 'PENDING_ACTIVATION':
        return 'badge badge-warning';
      default:
        return 'badge badge-danger';
    }
  }

  protected toggleClientUserSuspend(cu: ClientUserLink): void {
    const next: ActivationStatus = cu.activationStatus === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED';
    this.store.updateClientUserStatus(cu.id, next);
    const current = this.viewingClientUser();
    if (current && current.id === cu.id) {
      this.viewingClientUser.set({ ...current, activationStatus: next });
    }
  }

  protected openClientUserDetails(cu: ClientUserLink): void {
    this.viewingClientUser.set(cu);
  }

  protected isStaffAssigned(staffId: string, email: string): boolean {
    return this.store
      .userAccounts()
      .some(
        (u) =>
          u.staffId === staffId ||
          u.email.toLowerCase() === email.trim().toLowerCase()
      );
  }

  protected openUserModal(existing?: SystemUserAccount): void {
    if (existing) {
      this.editingId.set(existing.id);
      this.selectedStaffId = existing.staffId || '__CUSTOM__';
      this.form = {
        fullName: existing.fullName,
        email: existing.email,
        phone: existing.phone,
        designation: existing.designation,
        department: existing.department,
        dutyStation: existing.dutyStation,
        role: existing.role,
        preferredOtpChannel: existing.preferredOtpChannel,
        status: existing.status,
      };
    } else {
      this.editingId.set(undefined);
      const staffList = this.store.staffMembers();
      const firstUnassigned =
        staffList.find((s) => !this.isStaffAssigned(s.id, s.email)) || staffList[0];

      if (firstUnassigned) {
        this.selectedStaffId = firstUnassigned.id;
        this.form = {
          fullName: `${firstUnassigned.surname} ${firstUnassigned.lastName}`,
          email: firstUnassigned.email,
          phone: firstUnassigned.phone,
          designation: firstUnassigned.designation,
          department: firstUnassigned.department,
          dutyStation: firstUnassigned.dutyStation,
          role: 'STAFF_OFFICER',
          preferredOtpChannel: 'EMAIL',
          status: 'ACTIVE',
        };
      } else {
        this.selectedStaffId = '__CUSTOM__';
        this.form = {
          fullName: '',
          email: '',
          phone: '+256 ',
          designation: this.store.designations()[0] || 'Principal Officer',
          department: this.store.departments()[0] || 'Department of Refugees',
          dutyStation: this.store.dutyStations()[0] || 'OPM Main',
          role: 'STAFF_OFFICER',
          preferredOtpChannel: 'EMAIL',
          status: 'ACTIVE',
        };
      }
    }
    this.modalOpen.set(true);
  }

  protected onStaffLinkChange(staffId: string): void {
    this.selectedStaffId = staffId;
    if (!staffId || staffId === '__CUSTOM__') {
      this.form.fullName = '';
      this.form.email = '';
      this.form.phone = '+256 ';
      return;
    }
    const staff = this.store.staffMembers().find((s) => s.id === staffId);
    if (staff) {
      this.form.fullName = `${staff.surname} ${staff.lastName}`;
      this.form.email = staff.email;
      this.form.phone = staff.phone;
      this.form.designation = staff.designation;
      this.form.department = staff.department;
      this.form.dutyStation = staff.dutyStation;
    }
  }

  protected openClientUserModal(): void {
    const people = this.assignablePeople();
    const firstPerson = people[0];
    const orgs = this.assignableOrganisations();
    const matchedOrg =
      orgs.find((o) => o.code === 'OPM' || o.name === firstPerson?.department) || orgs[0];

    this.cuForm = {
      selectedPersonKey: firstPerson?.key || '',
      systemUserId: firstPerson?.systemUserId,
      userEmail: firstPerson?.email || '',
      userPhoneNumber: firstPerson?.phone || '',
      clientRef: matchedOrg?.id || '',
      accountCategory: 'USER',
      activationStatus: 'ACTIVE',
      isDefaultClient: true,
    };
    this.clientModalOpen.set(true);
  }

  protected onClientPersonSelect(personKey: string): void {
    this.cuForm.selectedPersonKey = personKey;
    const person = this.assignablePeople().find((p) => p.key === personKey);
    if (!person) return;
    this.cuForm.systemUserId = person.systemUserId;
    this.cuForm.userEmail = person.email;
    this.cuForm.userPhoneNumber = person.phone;
    const matchedOrg =
      this.assignableOrganisations().find((o) => o.code === 'OPM') ||
      this.assignableOrganisations().find((o) => o.name === person.department);
    if (matchedOrg) {
      this.cuForm.clientRef = matchedOrg.id;
    }
  }

  protected saveClientUserAssignment(): void {
    const org = this.assignableOrganisations().find((d) => d.id === this.cuForm.clientRef);
    if (!org || (!this.cuForm.userEmail && !this.cuForm.userPhoneNumber)) return;

    this.store.saveClientUserLink({
      systemUserId: this.cuForm.systemUserId,
      userEmail: this.cuForm.userEmail,
      userPhoneNumber: this.cuForm.userPhoneNumber,
      accountCategory: this.cuForm.accountCategory,
      clientRef: org.id,
      clientName: org.name,
      clientCode: org.code,
      isDefaultClient: this.cuForm.isDefaultClient,
      activationStatus: this.cuForm.activationStatus,
    });
    this.clientModalOpen.set(false);
  }

  protected closeModal(): void {
    this.modalOpen.set(false);
  }

  protected saveUser(): void {
    if (!this.form.fullName.trim() || !this.form.email.trim()) return;
    const linkedStaffId =
      this.selectedStaffId && this.selectedStaffId !== '__CUSTOM__'
        ? this.selectedStaffId
        : undefined;
    this.store.saveUserAccount(
      {
        staffId: linkedStaffId,
        fullName: this.form.fullName.trim(),
        username: this.form.email.trim().toLowerCase(),
        email: this.form.email.trim().toLowerCase(),
        phone: this.form.phone.trim(),
        designation: this.form.designation,
        department: this.form.department,
        dutyStation: this.form.dutyStation,
        role: this.form.role,
        preferredOtpChannel: this.form.preferredOtpChannel,
        status: this.form.status,
      },
      this.editingId()
    );
    this.closeModal();
  }
}
