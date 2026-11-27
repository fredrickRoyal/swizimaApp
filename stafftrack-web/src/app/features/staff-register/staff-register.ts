import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  LookupItem,
  OTHER_DESIGNATION_OPTION,
  OTHER_DUTY_STATION_OPTION,
  StaffMember,
} from '../../core/models/stafftrack.models';
import { COUNTRY_DIAL_CODES } from '../../core/services/auth.service';
import { StaffTrackStore } from '../../core/services/stafftrack.store';
import { Icon } from '../../shared/icon/icon';
import { Pagination } from '../../shared/pagination/pagination';

export type StaffRegisterTab = 'staff' | 'departments' | 'designations' | 'stations';
export type LookupKind = 'DEPARTMENT' | 'DESIGNATION' | 'STATION';

@Component({
  selector: 'app-staff-register',
  imports: [FormsModule, Icon, Pagination],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    /* Single-row scrollable tablist following modern-web-guidance tabs guide */
    .tabs-bar {
      display: flex;
      overflow-x: auto;
      scrollbar-width: none;
      border-bottom: 1px solid var(--border);
      background: var(--bg-elevated);
      border-radius: var(--radius-lg) var(--radius-lg) 0 0;
      padding-inline: 0.75rem;
    }
    .tab-btn {
      position: relative;
      flex: none;
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      min-height: 48px;
      padding: 0.65rem 1.1rem;
      border: 0;
      background: transparent;
      color: var(--text-muted);
      font-size: 0.88rem;
      font-weight: 600;
      white-space: nowrap;
      scroll-snap-align: start;
    }
    .tab-btn:hover {
      color: var(--text);
    }
    .tab-btn[aria-selected='true'] {
      color: var(--brand-500);
    }
    .tab-btn[aria-selected='true']::after {
      content: '';
      position: absolute;
      inset: auto 0 0;
      border-block-end: 3px solid var(--brand-500);
    }
    .cell-stack {
      display: grid;
      gap: 0.15rem;
    }
    .cell-stack small {
      color: var(--text-muted);
      font-size: 0.76rem;
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
    .phone-row {
      display: grid;
      grid-template-columns: 122px 1fr;
      gap: 0.45rem;
    }
    .tab-panel-toolbar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.95rem 1.25rem;
      border-bottom: 1px solid var(--border);
      background: var(--bg-subtle);
    }
    .code-pill {
      font-family: var(--font-mono);
      font-size: 0.78rem;
      font-weight: 700;
      padding: 0.2rem 0.55rem;
      border-radius: 6px;
      background: var(--bg-subtle);
      border: 1px solid var(--border-strong);
    }
  `,
  template: `
    <div class="page">
      <header class="page-header">
        <div class="page-title">
          <h1>Staff Register &amp; Organization Directory</h1>
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
          @if (activeTab() === 'staff') {
            <button type="button" class="btn btn-primary" (click)="openStaffModal()">
              <app-icon name="plus" />
              <span>Register Staff Member</span>
            </button>
          } @else if (activeTab() === 'departments') {
            <button type="button" class="btn btn-primary" (click)="openLookupModal('DEPARTMENT')">
              <app-icon name="plus" />
              <span>Add Department / Unit</span>
            </button>
          } @else if (activeTab() === 'designations') {
            <button type="button" class="btn btn-primary" (click)="openLookupModal('DESIGNATION')">
              <app-icon name="plus" />
              <span>Add Officer Designation</span>
            </button>
          } @else if (activeTab() === 'stations') {
            <button type="button" class="btn btn-primary" (click)="openLookupModal('STATION')">
              <app-icon name="plus" />
              <span>Add Duty Station</span>
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

      <!-- KPI Cards -->
      <section class="kpi-grid" aria-label="Staff register summary">
        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Registered Staff</span>
            <span class="kpi-icon"><app-icon name="users" /></span>
          </div>
          <div class="kpi-value">{{ store.staffMembers().length }}</div>
          <div class="kpi-sub">Active officers in directory</div>
        </article>

        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Departments / Units</span>
            <span class="kpi-icon" style="background: var(--purple-bg); color: var(--purple)">
              <app-icon name="dashboard" />
            </span>
          </div>
          <div class="kpi-value">{{ store.departmentItems().length }}</div>
          <div class="kpi-sub">Configured operational units</div>
        </article>

        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Officer Designations</span>
            <span class="kpi-icon" style="background: var(--success-bg); color: var(--success)">
              <app-icon name="clipboard" />
            </span>
          </div>
          <div class="kpi-value">{{ store.designationItems().length }}</div>
          <div class="kpi-sub">Approved job titles</div>
        </article>

        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Duty Stations</span>
            <span class="kpi-icon"><app-icon name="mapPin" /></span>
          </div>
          <div class="kpi-value">{{ store.dutyStationItems().length }}</div>
          <div class="kpi-sub">Offices, desks &amp; settlements</div>
        </article>
      </section>

      <!-- Tabbed Container -->
      <section class="card" style="overflow: hidden">
        <div
          class="tabs-bar"
          role="tablist"
          aria-label="Staff Register and Organization Setup Tabs"
        >
          <button
            type="button"
            role="tab"
            id="tab-staff"
            class="tab-btn"
            [attr.aria-selected]="activeTab() === 'staff'"
            (click)="activeTab.set('staff')"
          >
            <app-icon name="users" />
            <span>Staff Directory</span>
            <span class="badge badge-info">{{ store.staffMembers().length }}</span>
          </button>

          <button
            type="button"
            role="tab"
            id="tab-departments"
            class="tab-btn"
            [attr.aria-selected]="activeTab() === 'departments'"
            (click)="activeTab.set('departments')"
          >
            <app-icon name="dashboard" />
            <span>Department / Unit</span>
            <span class="badge">{{ store.departmentItems().length }}</span>
          </button>

          <button
            type="button"
            role="tab"
            id="tab-designations"
            class="tab-btn"
            [attr.aria-selected]="activeTab() === 'designations'"
            (click)="activeTab.set('designations')"
          >
            <app-icon name="clipboard" />
            <span>Officer Designations</span>
            <span class="badge">{{ store.designationItems().length }}</span>
          </button>

          <button
            type="button"
            role="tab"
            id="tab-stations"
            class="tab-btn"
            [attr.aria-selected]="activeTab() === 'stations'"
            (click)="activeTab.set('stations')"
          >
            <app-icon name="mapPin" />
            <span>Duty Stations</span>
            <span class="badge">{{ store.dutyStationItems().length }}</span>
          </button>
        </div>

        <!-- TAB PANEL 1: Staff Directory -->
        <div
          role="tabpanel"
          aria-labelledby="tab-staff"
          [hidden]="activeTab() !== 'staff'"
        >
          <div class="toolbar">
            <label class="search-box">
              <span class="search-icon"><app-icon name="search" /></span>
              <input
                type="search"
                class="input"
                placeholder="Search surname, last name, email, phone, department, or duty station..."
                [ngModel]="searchQuery()"
                (ngModelChange)="searchQuery.set($event); staffPage.set(0)"
              />
            </label>

            <select
              class="select filter-select"
              aria-label="Filter by department"
              [ngModel]="departmentFilter()"
              (ngModelChange)="departmentFilter.set($event); staffPage.set(0)"
            >
              <option value="ALL">All Departments / Units</option>
              @for (dept of store.departments(); track dept) {
                <option [value]="dept">{{ dept }}</option>
              }
            </select>

            <select
              class="select filter-select"
              aria-label="Filter by duty station"
              [ngModel]="stationFilter()"
              (ngModelChange)="stationFilter.set($event); staffPage.set(0)"
            >
              <option value="ALL">All Duty Stations</option>
              @for (st of store.dutyStations(); track st) {
                <option [value]="st">{{ st }}</option>
              }
            </select>
          </div>

          <div tabindex="0" role="region" aria-labelledby="staff-table-caption" class="table-wrapper">
            <table class="data-table">
              <caption id="staff-table-caption">
                Registered staff directory with contact details, department, designation, and duty station
              </caption>
              <thead>
                <tr>
                  <th>Surname &amp; Last Name</th>
                  <th>Designation</th>
                  <th>Email &amp; Alternative Email</th>
                  <th>Phone &amp; Alternative Phone</th>
                  <th>Department / Unit</th>
                  <th>Duty Station</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (staff of pagedStaff(); track staff.id) {
                  <tr>
                    <th>
                      <div class="cell-stack">
                        <span>{{ staff.surname }} {{ staff.lastName }}</span>
                        <small>{{ staff.staffCode || staff.id }} · Registered {{ staff.createdAt }}</small>
                      </div>
                    </th>
                    <td data-label="Designation">
                      <span class="badge badge-info">{{ staff.designation }}</span>
                    </td>
                    <td data-label="Email Contacts">
                      <div class="cell-stack">
                        <span>{{ staff.email }}</span>
                        @if (staff.alternativeEmail) {
                          <small>Alt: {{ staff.alternativeEmail }}</small>
                        }
                      </div>
                    </td>
                    <td data-label="Phone Contacts">
                      <div class="cell-stack">
                        <span>{{ staff.phone }}</span>
                        @if (staff.alternativePhone) {
                          <small>Alt: {{ staff.alternativePhone }}</small>
                        }
                      </div>
                    </td>
                    <td data-label="Department / Unit">{{ staff.department }}</td>
                    <td data-label="Duty Station">
                      <span class="badge badge-purple">{{ staff.dutyStation }}</span>
                    </td>
                    <td data-label="Actions">
                      <div class="action-btns">
                        <button
                          type="button"
                          class="btn btn-sm icon-action-btn"
                          aria-label="Edit staff member"
                          title="Edit staff member"
                          (click)="openStaffModal(staff)"
                        >
                          <app-icon name="edit" />
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm btn-danger icon-action-btn"
                          aria-label="Delete staff member"
                          title="Delete staff member"
                          (click)="store.deleteStaffMember(staff.id)"
                        >
                          <app-icon name="trash" />
                        </button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="7" style="text-align: center; padding: 2.2rem; color: var(--text-muted)">
                      No staff members match your current filter.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <app-pagination
            [page]="staffPage()"
            [size]="pageSize()"
            [total]="filteredStaff().length"
            (pageChange)="staffPage.set($event)"
            (sizeChange)="pageSize.set($event)"
          />
        </div>

        <!-- TAB PANEL 2: Department / Unit Management -->
        <div
          role="tabpanel"
          aria-labelledby="tab-departments"
          [hidden]="activeTab() !== 'departments'"
        >
          <div class="tab-panel-toolbar">
            <div>
              <strong style="font-size: 0.92rem">Departments &amp; Units</strong>
              <p style="font-size: 0.78rem; color: var(--text-muted)">
                Manage department codes and official names
              </p>
            </div>
            <div style="display: flex; flex-wrap: wrap; align-items: center; gap: 0.65rem; flex: 1; justify-content: flex-end">
              <label class="search-box" style="max-width: 340px">
                <span class="search-icon"><app-icon name="search" /></span>
                <input
                  type="search"
                  class="input"
                  placeholder="Search department code or name..."
                  [ngModel]="deptSearchQuery()"
                  (ngModelChange)="deptSearchQuery.set($event); deptPage.set(0)"
                />
              </label>
              <button
                type="button"
                class="btn btn-sm btn-primary"
                (click)="openLookupModal('DEPARTMENT')"
              >
                <app-icon name="plus" />
                <span>Add Department / Unit</span>
              </button>
            </div>
          </div>

          <div tabindex="0" role="region" aria-label="Departments and Units" class="table-wrapper">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Department / Unit Name</th>
                  <th>Registered Staff Count</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (item of pagedDepartments(); track item.id) {
                  <tr>
                    <th><span class="code-pill">{{ item.code }}</span></th>
                    <td data-label="Name"><strong>{{ item.name }}</strong></td>
                    <td data-label="Staff Count">
                      <span class="badge badge-info">{{ countStaffByDepartment(item.name) }} staff</span>
                    </td>
                    <td data-label="Actions">
                      <div class="action-btns">
                        <button
                          type="button"
                          class="btn btn-sm icon-action-btn"
                          aria-label="Edit department"
                          title="Edit department"
                          (click)="openLookupModal('DEPARTMENT', item)"
                        >
                          <app-icon name="edit" />
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm btn-danger icon-action-btn"
                          aria-label="Remove department"
                          title="Remove department"
                          (click)="store.deleteLookupItem('DEPARTMENT', item.id)"
                        >
                          <app-icon name="trash" />
                        </button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="4" style="text-align: center; padding: 2.2rem; color: var(--text-muted)">
                      No departments or units match your search.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <app-pagination
            [page]="deptPage()"
            [size]="pageSize()"
            [total]="filteredDepartments().length"
            (pageChange)="deptPage.set($event)"
            (sizeChange)="pageSize.set($event)"
          />
        </div>

        <!-- TAB PANEL 3: Officer Designations Management -->
        <div
          role="tabpanel"
          aria-labelledby="tab-designations"
          [hidden]="activeTab() !== 'designations'"
        >
          <div class="tab-panel-toolbar">
            <div>
              <strong style="font-size: 0.92rem">Officer Designations</strong>
              <p style="font-size: 0.78rem; color: var(--text-muted)">
                Manage official job title codes and designation names
              </p>
            </div>
            <div style="display: flex; flex-wrap: wrap; align-items: center; gap: 0.65rem; flex: 1; justify-content: flex-end">
              <label class="search-box" style="max-width: 340px">
                <span class="search-icon"><app-icon name="search" /></span>
                <input
                  type="search"
                  class="input"
                  placeholder="Search designation code or title..."
                  [ngModel]="desigSearchQuery()"
                  (ngModelChange)="desigSearchQuery.set($event); desigPage.set(0)"
                />
              </label>
              <button
                type="button"
                class="btn btn-sm btn-primary"
                (click)="openLookupModal('DESIGNATION')"
              >
                <app-icon name="plus" />
                <span>Add Officer Designation</span>
              </button>
            </div>
          </div>

          <div tabindex="0" role="region" aria-label="Officer Designations" class="table-wrapper">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Officer Designation / Job Title</th>
                  <th>Registered Staff Count</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (item of pagedDesignations(); track item.id) {
                  <tr>
                    <th><span class="code-pill">{{ item.code }}</span></th>
                    <td data-label="Designation"><strong>{{ item.name }}</strong></td>
                    <td data-label="Staff Count">
                      <span class="badge badge-info">{{ countStaffByDesignation(item.name) }} staff</span>
                    </td>
                    <td data-label="Actions">
                      <div class="action-btns">
                        <button
                          type="button"
                          class="btn btn-sm icon-action-btn"
                          aria-label="Edit designation"
                          title="Edit designation"
                          (click)="openLookupModal('DESIGNATION', item)"
                        >
                          <app-icon name="edit" />
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm btn-danger icon-action-btn"
                          aria-label="Remove designation"
                          title="Remove designation"
                          (click)="store.deleteLookupItem('DESIGNATION', item.id)"
                        >
                          <app-icon name="trash" />
                        </button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="4" style="text-align: center; padding: 2.2rem; color: var(--text-muted)">
                      No officer designations match your search.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <app-pagination
            [page]="desigPage()"
            [size]="pageSize()"
            [total]="filteredDesignations().length"
            (pageChange)="desigPage.set($event)"
            (sizeChange)="pageSize.set($event)"
          />
        </div>

        <!-- TAB PANEL 4: Duty Stations Management -->
        <div
          role="tabpanel"
          aria-labelledby="tab-stations"
          [hidden]="activeTab() !== 'stations'"
        >
          <div class="tab-panel-toolbar">
            <div>
              <strong style="font-size: 0.92rem">Duty Stations, Offices &amp; Settlements</strong>
              <p style="font-size: 0.78rem; color: var(--text-muted)">
                Manage duty station codes and office or refugee settlement names
              </p>
            </div>
            <div style="display: flex; flex-wrap: wrap; align-items: center; gap: 0.65rem; flex: 1; justify-content: flex-end">
              <label class="search-box" style="max-width: 340px">
                <span class="search-icon"><app-icon name="search" /></span>
                <input
                  type="search"
                  class="input"
                  placeholder="Search duty station code or name..."
                  [ngModel]="stationSearchQuery()"
                  (ngModelChange)="stationSearchQuery.set($event); stationPage.set(0)"
                />
              </label>
              <button
                type="button"
                class="btn btn-sm btn-primary"
                (click)="openLookupModal('STATION')"
              >
                <app-icon name="plus" />
                <span>Add Duty Station</span>
              </button>
            </div>
          </div>

          <div tabindex="0" role="region" aria-label="Duty Stations" class="table-wrapper">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Duty Station / Office / Settlement</th>
                  <th>Registered Staff Count</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (item of pagedDutyStations(); track item.id) {
                  <tr>
                    <th><span class="code-pill">{{ item.code }}</span></th>
                    <td data-label="Duty Station"><strong>{{ item.name }}</strong></td>
                    <td data-label="Staff Count">
                      <span class="badge badge-purple">{{ countStaffByStation(item.name) }} staff</span>
                    </td>
                    <td data-label="Actions">
                      <div class="action-btns">
                        <button
                          type="button"
                          class="btn btn-sm icon-action-btn"
                          aria-label="Edit duty station"
                          title="Edit duty station"
                          (click)="openLookupModal('STATION', item)"
                        >
                          <app-icon name="edit" />
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm btn-danger icon-action-btn"
                          aria-label="Remove duty station"
                          title="Remove duty station"
                          (click)="store.deleteLookupItem('STATION', item.id)"
                        >
                          <app-icon name="trash" />
                        </button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="4" style="text-align: center; padding: 2.2rem; color: var(--text-muted)">
                      No duty stations match your search.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <app-pagination
            [page]="stationPage()"
            [size]="pageSize()"
            [total]="filteredDutyStations().length"
            (pageChange)="stationPage.set($event)"
            (sizeChange)="pageSize.set($event)"
          />
        </div>
      </section>

      <!-- Add / Edit Lookup (Department, Designation, Duty Station) Dialog -->
      @if (lookupModalOpen()) {
        <div class="modal-backdrop">
          <div
            class="modal-card"
            style="width: min(520px, 100%)"
            role="dialog"
            aria-modal="true"
            aria-labelledby="lookup-modal-title"
          >
            <div class="modal-header">
              <h2 id="lookup-modal-title">
                {{ lookupEditingId() ? 'Update' : 'Add' }} {{ lookupKindLabel() }}
              </h2>
              <button type="button" class="btn btn-sm btn-ghost" (click)="closeLookupModal()">
                <app-icon name="close" />
              </button>
            </div>

            <form (ngSubmit)="saveLookup()">
              <div class="modal-body">
                <div class="form-grid">
                  <div class="field">
                    <label for="lkp-code">Code *</label>
                    <input
                      id="lkp-code"
                      name="lookupCode"
                      class="input"
                      required
                      [(ngModel)]="lookupForm.code"
                      placeholder="e.g. DOR, PO, NAK-RS"
                    />
                  </div>

                  <div class="field">
                    <label for="lkp-name">Name *</label>
                    <input
                      id="lkp-name"
                      name="lookupName"
                      class="input"
                      required
                      [(ngModel)]="lookupForm.name"
                      [placeholder]="'Enter ' + lookupKindLabel() + ' name...'"
                    />
                  </div>
                </div>
              </div>

              <div class="modal-footer">
                <button type="button" class="btn" (click)="closeLookupModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">
                  <app-icon name="check" />
                  <span>{{ lookupEditingId() ? 'Save Changes' : 'Save ' + lookupKindLabel() }}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Register / Edit Staff Modal -->
      @if (staffModalOpen()) {
        <div class="modal-backdrop">
          <div
            class="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="staff-modal-title"
          >
            <div class="modal-header">
              <h2 id="staff-modal-title">
                {{ editingId() ? 'Update Staff Profile' : 'Register New Staff Member' }}
              </h2>
              <button type="button" class="btn btn-sm btn-ghost" (click)="closeStaffModal()">
                <app-icon name="close" />
              </button>
            </div>

            <form (ngSubmit)="saveStaff()">
              <div class="modal-body">
                <div class="form-grid">
                  <div class="field">
                    <label for="stf-surname">Surname *</label>
                    <input
                      id="stf-surname"
                      name="surname"
                      class="input"
                      required
                      [(ngModel)]="form.surname"
                      placeholder="e.g. Kasoma"
                    />
                  </div>

                  <div class="field">
                    <label for="stf-lastname">Last Name *</label>
                    <input
                      id="stf-lastname"
                      name="lastName"
                      class="input"
                      required
                      [(ngModel)]="form.lastName"
                      placeholder="e.g. Fredrick"
                    />
                  </div>

                  <div class="field">
                    <label for="stf-email">Email Address *</label>
                    <input
                      id="stf-email"
                      type="email"
                      name="email"
                      class="input"
                      required
                      [(ngModel)]="form.email"
                      placeholder="officer@opm.go.ug"
                    />
                  </div>

                  <div class="field">
                    <label for="stf-alt-email">Alternative Email</label>
                    <input
                      id="stf-alt-email"
                      type="email"
                      name="alternativeEmail"
                      class="input"
                      [(ngModel)]="form.alternativeEmail"
                      placeholder="personal@gmail.com (optional)"
                    />
                  </div>

                  <div class="field">
                    <label for="stf-phone">Phone Number *</label>
                    <div class="phone-row">
                      <select
                        name="phoneDial"
                        class="select"
                        aria-label="Primary phone country code"
                        [(ngModel)]="primaryDial"
                      >
                        @for (c of countries; track c.code) {
                          <option [value]="c.dial">{{ c.flag }} {{ c.dial }}</option>
                        }
                      </select>
                      <input
                        id="stf-phone"
                        type="tel"
                        name="phoneLocal"
                        class="input"
                        required
                        [(ngModel)]="primaryPhoneLocal"
                        placeholder="700 000 000"
                      />
                    </div>
                  </div>

                  <div class="field">
                    <label for="stf-alt-phone">Alternative Phone</label>
                    <div class="phone-row">
                      <select
                        name="altPhoneDial"
                        class="select"
                        aria-label="Alternative phone country code"
                        [(ngModel)]="altDial"
                      >
                        @for (c of countries; track c.code) {
                          <option [value]="c.dial">{{ c.flag }} {{ c.dial }}</option>
                        }
                      </select>
                      <input
                        id="stf-alt-phone"
                        type="tel"
                        name="altPhoneLocal"
                        class="input"
                        [(ngModel)]="altPhoneLocal"
                        placeholder="772 ... (optional)"
                      />
                    </div>
                  </div>

                  <div class="field">
                    <label for="stf-desig">Designation of Officer *</label>
                    <select
                      id="stf-desig"
                      name="designationOption"
                      class="select"
                      required
                      [(ngModel)]="designationOption"
                    >
                      @for (des of store.designations(); track des) {
                        <option [value]="des">{{ des }}</option>
                      }
                      <option [value]="otherDesignationOption">{{ otherDesignationOption }}</option>
                    </select>
                  </div>

                  @if (designationOption === otherDesignationOption) {
                    <div class="field">
                      <label for="stf-desig-other">Specify Other Designation *</label>
                      <input
                        id="stf-desig-other"
                        name="otherDesignation"
                        class="input"
                        required
                        [(ngModel)]="otherDesignation"
                        placeholder="Enter custom job title / designation"
                      />
                    </div>
                  }

                  <div class="field">
                    <label for="stf-dept">Department / Unit *</label>
                    <select
                      id="stf-dept"
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
                    <label for="stf-station">Duty station / office / settlement *</label>
                    <select
                      id="stf-station"
                      name="dutyStationOption"
                      class="select"
                      required
                      [(ngModel)]="dutyStationOption"
                    >
                      @for (st of store.dutyStations(); track st) {
                        <option [value]="st">{{ st }}</option>
                      }
                      <option [value]="otherStationOption">{{ otherStationOption }}</option>
                    </select>
                  </div>

                  @if (dutyStationOption === otherStationOption) {
                    <div class="field">
                      <label for="stf-station-other">Specify Other Duty Station *</label>
                      <input
                        id="stf-station-other"
                        name="otherDutyStation"
                        class="input"
                        required
                        [(ngModel)]="otherDutyStation"
                        placeholder="Enter custom duty station / office / settlement"
                      />
                    </div>
                  }
                </div>
              </div>

              <div class="modal-footer">
                <button type="button" class="btn" (click)="closeStaffModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">
                  <app-icon name="check" />
                  <span>{{ editingId() ? 'Save Changes' : 'Register Staff' }}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
})
export class StaffRegister {
  protected readonly store = inject(StaffTrackStore);
  protected readonly countries = COUNTRY_DIAL_CODES;
  protected readonly otherDesignationOption = OTHER_DESIGNATION_OPTION;
  protected readonly otherStationOption = OTHER_DUTY_STATION_OPTION;

  protected readonly activeTab = signal<StaffRegisterTab>('staff');
  protected readonly searchQuery = signal('');
  protected readonly departmentFilter = signal('ALL');
  protected readonly stationFilter = signal('ALL');

  protected readonly deptSearchQuery = signal('');
  protected readonly desigSearchQuery = signal('');
  protected readonly stationSearchQuery = signal('');

  protected readonly pageSize = signal(10);
  protected readonly staffPage = signal(0);
  protected readonly deptPage = signal(0);
  protected readonly desigPage = signal(0);
  protected readonly stationPage = signal(0);

  protected readonly staffModalOpen = signal(false);
  protected readonly editingId = signal<string | undefined>(undefined);

  protected readonly lookupModalOpen = signal(false);
  protected readonly lookupKind = signal<LookupKind>('DEPARTMENT');
  protected readonly lookupEditingId = signal<string | undefined>(undefined);
  protected lookupForm = { code: '', name: '' };

  protected primaryDial = '+256';
  protected primaryPhoneLocal = '';
  protected altDial = '+256';
  protected altPhoneLocal = '';

  protected designationOption = '';
  protected otherDesignation = '';
  protected dutyStationOption = '';
  protected otherDutyStation = '';

  protected form = this.emptyForm();

  protected readonly lookupKindLabel = computed(() => {
    switch (this.lookupKind()) {
      case 'DEPARTMENT':
        return 'Department / Unit';
      case 'DESIGNATION':
        return 'Officer Designation';
      case 'STATION':
        return 'Duty Station';
    }
  });

  protected readonly filteredStaff = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const dept = this.departmentFilter();
    const st = this.stationFilter();
    return this.store.staffMembers().filter((s) => {
      if (dept !== 'ALL' && s.department !== dept) return false;
      if (st !== 'ALL' && s.dutyStation !== st) return false;
      if (!q) return true;
      return (
        s.surname.toLowerCase().includes(q) ||
        s.lastName.toLowerCase().includes(q) ||
        s.email.toLowerCase().includes(q) ||
        (s.alternativeEmail ?? '').toLowerCase().includes(q) ||
        s.phone.toLowerCase().includes(q) ||
        (s.alternativePhone ?? '').toLowerCase().includes(q) ||
        s.department.toLowerCase().includes(q) ||
        s.designation.toLowerCase().includes(q) ||
        s.dutyStation.toLowerCase().includes(q)
      );
    });
  });

  protected readonly filteredDepartments = computed(() => {
    const q = this.deptSearchQuery().trim().toLowerCase();
    if (!q) return this.store.departmentItems();
    return this.store
      .departmentItems()
      .filter((item) => item.code.toLowerCase().includes(q) || item.name.toLowerCase().includes(q));
  });

  protected readonly filteredDesignations = computed(() => {
    const q = this.desigSearchQuery().trim().toLowerCase();
    if (!q) return this.store.designationItems();
    return this.store
      .designationItems()
      .filter((item) => item.code.toLowerCase().includes(q) || item.name.toLowerCase().includes(q));
  });

  protected readonly filteredDutyStations = computed(() => {
    const q = this.stationSearchQuery().trim().toLowerCase();
    if (!q) return this.store.dutyStationItems();
    return this.store
      .dutyStationItems()
      .filter((item) => item.code.toLowerCase().includes(q) || item.name.toLowerCase().includes(q));
  });

  protected readonly pagedStaff = computed(() => {
    const size = this.pageSize();
    const list = this.filteredStaff();
    const maxPage = Math.max(0, Math.ceil(list.length / size) - 1);
    const start = Math.min(this.staffPage(), maxPage) * size;
    return list.slice(start, start + size);
  });

  protected readonly pagedDepartments = computed(() => {
    const size = this.pageSize();
    const list = this.filteredDepartments();
    const maxPage = Math.max(0, Math.ceil(list.length / size) - 1);
    const start = Math.min(this.deptPage(), maxPage) * size;
    return list.slice(start, start + size);
  });

  protected readonly pagedDesignations = computed(() => {
    const size = this.pageSize();
    const list = this.filteredDesignations();
    const maxPage = Math.max(0, Math.ceil(list.length / size) - 1);
    const start = Math.min(this.desigPage(), maxPage) * size;
    return list.slice(start, start + size);
  });

  protected readonly pagedDutyStations = computed(() => {
    const size = this.pageSize();
    const list = this.filteredDutyStations();
    const maxPage = Math.max(0, Math.ceil(list.length / size) - 1);
    const start = Math.min(this.stationPage(), maxPage) * size;
    return list.slice(start, start + size);
  });

  protected countStaffByDepartment(dept: string): number {
    return this.store.staffMembers().filter((s) => s.department === dept).length;
  }

  protected countStaffByDesignation(des: string): number {
    return this.store.staffMembers().filter((s) => s.designation === des).length;
  }

  protected countStaffByStation(st: string): number {
    return this.store.staffMembers().filter((s) => s.dutyStation === st).length;
  }

  protected openLookupModal(kind: LookupKind, existing?: LookupItem): void {
    this.lookupKind.set(kind);
    if (existing) {
      this.lookupEditingId.set(existing.id);
      this.lookupForm = { code: existing.code, name: existing.name };
    } else {
      this.lookupEditingId.set(undefined);
      this.lookupForm = { code: '', name: '' };
    }
    this.lookupModalOpen.set(true);
  }

  protected closeLookupModal(): void {
    this.lookupModalOpen.set(false);
    this.lookupEditingId.set(undefined);
  }

  protected saveLookup(): void {
    if (!this.lookupForm.code.trim() || !this.lookupForm.name.trim()) return;
    this.store.saveLookupItem(this.lookupKind(), this.lookupForm, this.lookupEditingId());
    this.closeLookupModal();
  }

  protected openStaffModal(existing?: StaffMember): void {
    const designations = this.store.designations();
    const stations = this.store.dutyStations();
    if (existing) {
      this.editingId.set(existing.id);
      this.form = {
        surname: existing.surname,
        lastName: existing.lastName,
        designation: existing.designation,
        email: existing.email,
        alternativeEmail: existing.alternativeEmail || '',
        phone: existing.phone,
        alternativePhone: existing.alternativePhone || '',
        department: existing.department,
        dutyStation: existing.dutyStation,
        status: existing.status,
      };
      this.primaryDial = '+256';
      this.primaryPhoneLocal = existing.phone.replace(/^\+256/, '');
      this.altDial = '+256';
      this.altPhoneLocal = (existing.alternativePhone || '').replace(/^\+256/, '');

      if (designations.includes(existing.designation)) {
        this.designationOption = existing.designation;
        this.otherDesignation = '';
      } else {
        this.designationOption = this.otherDesignationOption;
        this.otherDesignation = existing.designation;
      }

      if (stations.includes(existing.dutyStation)) {
        this.dutyStationOption = existing.dutyStation;
        this.otherDutyStation = '';
      } else {
        this.dutyStationOption = this.otherStationOption;
        this.otherDutyStation = existing.dutyStation;
      }
    } else {
      this.editingId.set(undefined);
      this.form = this.emptyForm();
      this.primaryDial = '+256';
      this.primaryPhoneLocal = '';
      this.altDial = '+256';
      this.altPhoneLocal = '';
      this.designationOption = designations[0] || 'Principal Officer';
      this.otherDesignation = '';
      this.dutyStationOption = stations[0] || 'OPM Main';
      this.otherDutyStation = '';
    }
    this.staffModalOpen.set(true);
  }

  protected closeStaffModal(): void {
    this.staffModalOpen.set(false);
  }

  protected saveStaff(): void {
    if (!this.form.surname.trim() || !this.form.lastName.trim() || !this.form.email.trim()) {
      return;
    }
    const resolvedDesignation =
      this.designationOption === this.otherDesignationOption
        ? this.otherDesignation.trim() || 'Officer'
        : this.designationOption;
    const resolvedStation =
      this.dutyStationOption === this.otherStationOption
        ? this.otherDutyStation.trim() || 'OPM Main'
        : this.dutyStationOption;

    const cleanPrimary = this.formatPhone(this.primaryDial, this.primaryPhoneLocal);
    const cleanAlt = this.altPhoneLocal.trim()
      ? this.formatPhone(this.altDial, this.altPhoneLocal)
      : undefined;

    if (
      this.designationOption === this.otherDesignationOption &&
      this.otherDesignation.trim()
    ) {
      this.store.addDesignation(resolvedDesignation);
    }
    if (
      this.dutyStationOption === this.otherStationOption &&
      this.otherDutyStation.trim()
    ) {
      this.store.addDutyStation(resolvedStation);
    }

    this.store.saveStaffMember(
      {
        ...this.form,
        designation: resolvedDesignation,
        dutyStation: resolvedStation,
        phone: cleanPrimary,
        alternativePhone: cleanAlt,
        alternativeEmail: this.form.alternativeEmail?.trim() || undefined,
      },
      this.editingId()
    );
    this.closeStaffModal();
  }

  private formatPhone(dial: string, rawLocal: string): string {
    const clean = rawLocal.trim().replace(/[\s\-()]/g, '');
    if (!clean) return '';
    if (clean.startsWith('+')) return clean;
    return `${dial}${clean.replace(/^0+/, '')}`;
  }

  private emptyForm(): Omit<StaffMember, 'id' | 'createdAt'> {
    return {
      surname: '',
      lastName: '',
      designation: this.store.designations()[0] || 'Principal Officer',
      email: '',
      alternativeEmail: '',
      phone: '',
      alternativePhone: '',
      department: this.store.departments()[0] || 'Department of Refugees',
      dutyStation: this.store.dutyStations()[0] || 'OPM Main',
      status: 'ACTIVE',
    };
  }
}
