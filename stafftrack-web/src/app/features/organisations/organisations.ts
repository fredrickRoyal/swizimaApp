import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Organisation, OrganisationStatus } from '../../core/models/stafftrack.models';
import { StaffTrackStore } from '../../core/services/stafftrack.store';
import { Icon } from '../../shared/icon/icon';
import { Pagination } from '../../shared/pagination/pagination';

@Component({
  selector: 'app-organisations',
  imports: [FormsModule, Icon, Pagination],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .cell-stack {
      display: grid;
      gap: 0.16rem;
    }
    .cell-primary {
      font-size: 0.84rem;
      font-weight: 600;
      color: var(--text);
    }
    .cell-stack small {
      color: var(--text-muted);
      font-size: 0.75rem;
      line-height: 1.35;
    }
    .code-pill {
      font-family: var(--font-mono);
      font-size: 0.72rem;
      font-weight: 700;
      padding: 0.14rem 0.48rem;
      border-radius: 6px;
      background: var(--bg-subtle);
      border: 1px solid var(--border-strong);
      color: var(--text);
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
    .checkbox-row {
      display: flex;
      align-items: center;
      gap: 0.55rem;
      padding: 0.65rem 0.85rem;
      border-radius: var(--radius-sm);
      background: var(--bg-subtle);
      border: 1px solid var(--border);
      font-size: 0.83rem;
      font-weight: 600;
      cursor: pointer;
    }
  `,
  template: `
    <div class="page">
      <header class="page-header">
        <div class="page-title">
          <h1>Organisations</h1>
          <p>
            Manage client organisations, ministries, and partner agencies registered on SOOTR (seeded
            with Office of the Prime Minister — OPM).
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
          <button type="button" class="btn btn-primary" (click)="openModal()">
            <app-icon name="plus" />
            <span>Register Organisation</span>
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

      <!-- KPI Summary Cards -->
      <section class="kpi-grid" aria-label="Organisation metrics">
        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Registered Organisations</span>
            <span class="kpi-icon"><app-icon name="building" /></span>
          </div>
          <div class="kpi-value">{{ store.organisations().length }}</div>
          <div class="kpi-sub">Ministries, directorates &amp; partner agencies</div>
        </article>

        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Active Organisations</span>
            <span class="kpi-icon" style="background: var(--success-bg); color: var(--success)">
              <app-icon name="check" />
            </span>
          </div>
          <div class="kpi-value">{{ activeCount() }}</div>
          <div class="kpi-sub">Enabled for portal &amp; OTP sign-in</div>
        </article>

        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Assigned Organisation Users</span>
            <span class="kpi-icon" style="background: var(--purple-bg); color: var(--purple)">
              <app-icon name="users" />
            </span>
          </div>
          <div class="kpi-value">{{ store.clientUserLinks().length }}</div>
          <div class="kpi-sub">Linked in User Management</div>
        </article>
      </section>

      <!-- Organisations Table Card -->
      <section class="card">
        <div class="toolbar">
          <label class="search-box">
            <span class="search-icon"><app-icon name="search" /></span>
            <input
              type="search"
              class="input"
              placeholder="Search by organisation name, code, contact person, email, or phone..."
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
            @for (st of statuses; track st) {
              <option [value]="st">{{ st }}</option>
            }
          </select>
        </div>

        <div tabindex="0" role="region" aria-labelledby="orgs-table-caption" class="table-wrapper">
          <table class="data-table">
            <caption id="orgs-table-caption">
              Organisations registered on SOOTR with primary contact, location, and status
            </caption>
            <thead>
              <tr>
                <th>Organisation</th>
                <th>Type</th>
                <th>Primary Contact</th>
                <th>Location</th>
                <th>Assigned Users</th>
                <th>Status</th>
                <th>Registered</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (org of pagedOrganisations(); track org.id) {
                <tr>
                  <th>
                    <div class="cell-stack">
                      <div style="display: flex; align-items: center; gap: 0.45rem; flex-wrap: wrap">
                        <span class="cell-primary">{{ org.name }}</span>
                        <span class="code-pill">{{ org.code }}</span>
                      </div>
                      @if (org.address) {
                        <small>{{ org.address }}</small>
                      }
                    </div>
                  </th>
                  <td data-label="Type">{{ org.organisationType }}</td>
                  <td data-label="Primary Contact">
                    <div class="cell-stack">
                      <span class="cell-primary">{{ org.contactPerson || '—' }}</span>
                      <small>{{ org.email }} @if (org.phone) { · {{ org.phone }} }</small>
                    </div>
                  </td>
                  <td data-label="Location">{{ formatLocation(org.city, org.country) }}</td>
                  <td data-label="Assigned Users">
                    <span class="badge badge-info">{{ countUsersForOrg(org) }} user(s)</span>
                  </td>
                  <td data-label="Status">
                    <span [class]="statusBadgeClass(org.status)">{{ org.status }}</span>
                  </td>
                  <td data-label="Registered">
                    <small style="color: var(--text-muted)">{{ org.createdAt }}</small>
                  </td>
                  <td data-label="Actions">
                    <div class="action-btns">
                      <button
                        type="button"
                        class="btn btn-sm icon-action-btn"
                        title="Edit organisation"
                        aria-label="Edit organisation"
                        (click)="openModal(org)"
                      >
                        <app-icon name="edit" />
                      </button>
                      <button
                        type="button"
                        class="btn btn-sm icon-action-btn"
                        [title]="org.status === 'ACTIVE' ? 'Suspend organisation' : 'Activate organisation'"
                        [attr.aria-label]="org.status === 'ACTIVE' ? 'Suspend organisation' : 'Activate organisation'"
                        (click)="toggleStatus(org)"
                      >
                        <app-icon name="power" />
                      </button>
                      <button
                        type="button"
                        class="btn btn-sm btn-danger icon-action-btn"
                        title="Delete organisation"
                        aria-label="Delete organisation"
                        (click)="store.deleteOrganisation(org.id)"
                      >
                        <app-icon name="trash" />
                      </button>
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="8" style="text-align: center; padding: 2.2rem; color: var(--text-muted)">
                    No organisations match your current search or filter.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        <app-pagination
          [page]="page()"
          [size]="pageSize()"
          [total]="filteredOrganisations().length"
          (pageChange)="page.set($event)"
          (sizeChange)="pageSize.set($event)"
        />
      </section>

      <!-- Register / Edit Organisation Modal (No backdrop click dismissal) -->
      @if (modalOpen()) {
        <div class="modal-backdrop">
          <div
            class="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="org-modal-title"
          >
            <div class="modal-header">
              <h2 id="org-modal-title">
                {{ editingId() ? 'Update Organisation' : 'Register Organisation' }}
              </h2>
              <button type="button" class="btn btn-sm btn-ghost" (click)="closeModal()">
                <app-icon name="close" />
              </button>
            </div>

            <form (ngSubmit)="saveOrganisation()">
              <div class="modal-body">
                <div class="form-grid">
                  <div class="field">
                    <label for="org-code">Organisation Code *</label>
                    <input
                      id="org-code"
                      name="code"
                      class="input"
                      required
                      [(ngModel)]="form.code"
                      placeholder="e.g. OPM"
                    />
                  </div>

                  <div class="field">
                    <label for="org-type">Organisation Type *</label>
                    <select
                      id="org-type"
                      name="organisationType"
                      class="select"
                      required
                      [(ngModel)]="form.organisationType"
                    >
                      @for (t of organisationTypes; track t) {
                        <option [value]="t">{{ t }}</option>
                      }
                    </select>
                  </div>

                  <div class="field field-full">
                    <label for="org-name">Organisation Full Name *</label>
                    <input
                      id="org-name"
                      name="name"
                      class="input"
                      required
                      [(ngModel)]="form.name"
                      placeholder="e.g. Office of the Prime Minister (OPM)"
                    />
                  </div>

                  @if (store.staffMembers().length > 0) {
                    <div class="field field-full">
                      <label for="org-staff-contact">Select Primary Contact from Staff Register (Optional)</label>
                      <select
                        id="org-staff-contact"
                        name="selectedContactStaffId"
                        class="select"
                        [ngModel]="selectedContactStaffId"
                        (ngModelChange)="onContactStaffSelect($event)"
                      >
                        <option value="">-- Enter contact person details manually --</option>
                        @for (s of store.staffMembers(); track s.id) {
                          <option [value]="s.id">
                            {{ s.surname }} {{ s.lastName }} — {{ s.designation }} ({{ s.email }} · {{ s.phone }})
                          </option>
                        }
                      </select>
                    </div>
                  }

                  <div class="field">
                    <label for="org-contact">Contact Person *</label>
                    <input
                      id="org-contact"
                      name="contactPerson"
                      class="input"
                      required
                      [(ngModel)]="form.contactPerson"
                      placeholder="e.g. Kasoma Fredrick"
                    />
                  </div>

                  <div class="field">
                    <label for="org-email">Contact Email *</label>
                    <input
                      id="org-email"
                      type="email"
                      name="email"
                      class="input"
                      required
                      [(ngModel)]="form.email"
                      placeholder="e.g. kfredrick35@gmail.com"
                    />
                  </div>

                  <div class="field">
                    <label for="org-phone">Contact Phone *</label>
                    <input
                      id="org-phone"
                      type="tel"
                      name="phone"
                      class="input"
                      required
                      [(ngModel)]="form.phone"
                      placeholder="+256 700 408 387"
                    />
                  </div>

                  <div class="field">
                    <label for="org-status">Status *</label>
                    <select
                      id="org-status"
                      name="status"
                      class="select"
                      required
                      [(ngModel)]="form.status"
                    >
                      @for (st of statuses; track st) {
                        <option [value]="st">{{ st }}</option>
                      }
                    </select>
                  </div>

                  <div class="field">
                    <label for="org-city">City *</label>
                    <input
                      id="org-city"
                      name="city"
                      class="input"
                      required
                      [(ngModel)]="form.city"
                      placeholder="Kampala"
                    />
                  </div>

                  <div class="field">
                    <label for="org-country">Country *</label>
                    <input
                      id="org-country"
                      name="country"
                      class="input"
                      required
                      [(ngModel)]="form.country"
                      placeholder="Uganda"
                    />
                  </div>

                  <div class="field field-full">
                    <label for="org-address">Physical Address</label>
                    <input
                      id="org-address"
                      name="address"
                      class="input"
                      [(ngModel)]="form.address"
                      placeholder="e.g. Plot 9-11 Apollo Kaggwa Road, Kampala"
                    />
                  </div>

                  @if (!editingId()) {
                    <div class="field field-full">
                      <label class="checkbox-row">
                        <input
                          type="checkbox"
                          name="assignContactAsAdmin"
                          [(ngModel)]="assignContactAsAdmin"
                        />
                        <span>
                          Also link contact person as a Client Admin for this organisation in User
                          Management
                        </span>
                      </label>
                    </div>
                  }
                </div>
              </div>

              <div class="modal-footer">
                <button type="button" class="btn" (click)="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">
                  <app-icon name="check" />
                  <span>{{ editingId() ? 'Save Changes' : 'Register Organisation' }}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
})
export class Organisations {
  protected readonly store = inject(StaffTrackStore);

  protected readonly statuses: OrganisationStatus[] = ['ACTIVE', 'PENDING', 'SUSPENDED', 'INACTIVE'];
  protected readonly organisationTypes: string[] = [
    'Government Ministry / Agency',
    'Directorate / Department',
    'Regional Desk / Settlement Office',
    'UN / Humanitarian Partner Agency',
    'Implementing Partner / NGO',
  ];

  protected readonly searchQuery = signal('');
  protected readonly statusFilter = signal<'ALL' | OrganisationStatus>('ALL');
  protected readonly pageSize = signal(10);
  protected readonly page = signal(0);

  protected readonly modalOpen = signal(false);
  protected readonly editingId = signal<string | undefined>(undefined);
  protected selectedContactStaffId = '';
  protected assignContactAsAdmin = true;

  protected form = {
    code: '',
    name: '',
    organisationType: 'Government Ministry / Agency',
    contactPerson: 'Kasoma Fredrick',
    email: 'kfredrick35@gmail.com',
    phone: '+256 700 408 387',
    city: 'Kampala',
    country: 'Uganda',
    address: 'Plot 9-11 Apollo Kaggwa Road, Kampala',
    status: 'ACTIVE' as OrganisationStatus,
  };

  protected readonly activeCount = computed(
    () => this.store.organisations().filter((o) => o.status === 'ACTIVE').length
  );

  protected readonly filteredOrganisations = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const st = this.statusFilter();
    return this.store.organisations().filter((org) => {
      if (st !== 'ALL' && org.status !== st) return false;
      if (!q) return true;
      return (
        org.name.toLowerCase().includes(q) ||
        org.code.toLowerCase().includes(q) ||
        org.organisationType.toLowerCase().includes(q) ||
        (org.contactPerson?.toLowerCase().includes(q) ?? false) ||
        (org.email?.toLowerCase().includes(q) ?? false) ||
        (org.phone?.toLowerCase().includes(q) ?? false) ||
        (org.city?.toLowerCase().includes(q) ?? false)
      );
    });
  });

  protected readonly pagedOrganisations = computed(() => {
    const size = this.pageSize();
    const list = this.filteredOrganisations();
    const maxPage = Math.max(0, Math.ceil(list.length / size) - 1);
    const start = Math.min(this.page(), maxPage) * size;
    return list.slice(start, start + size);
  });

  protected countUsersForOrg(org: Organisation): number {
    return this.store
      .clientUserLinks()
      .filter(
        (cu) =>
          cu.clientCode.toLowerCase() === org.code.toLowerCase() ||
          cu.clientName.toLowerCase() === org.name.toLowerCase() ||
          cu.clientRef === org.id
      ).length;
  }

  protected formatLocation(city?: string, country?: string): string {
    return [city, country].filter(Boolean).join(', ') || '—';
  }

  protected statusBadgeClass(status: OrganisationStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'badge badge-success';
      case 'PENDING':
        return 'badge badge-warning';
      case 'SUSPENDED':
        return 'badge badge-danger';
      default:
        return 'badge';
    }
  }

  protected toggleStatus(org: Organisation): void {
    const next: OrganisationStatus = org.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    this.store.updateOrganisationStatus(org.id, next);
  }

  protected openModal(existing?: Organisation): void {
    if (existing) {
      this.editingId.set(existing.id);
      this.selectedContactStaffId = '';
      this.form = {
        code: existing.code,
        name: existing.name,
        organisationType: existing.organisationType,
        contactPerson: existing.contactPerson,
        email: existing.email,
        phone: existing.phone,
        city: existing.city,
        country: existing.country,
        address: existing.address || '',
        status: existing.status,
      };
    } else {
      this.editingId.set(undefined);
      const firstStaff = this.store.staffMembers()[0];
      this.selectedContactStaffId = firstStaff?.id || '';
      this.assignContactAsAdmin = true;
      this.form = {
        code: '',
        name: '',
        organisationType: 'Government Ministry / Agency',
        contactPerson: firstStaff
          ? `${firstStaff.surname} ${firstStaff.lastName}`
          : 'Kasoma Fredrick',
        email: firstStaff?.email || 'kfredrick35@gmail.com',
        phone: firstStaff?.phone || '+256 700 408 387',
        city: 'Kampala',
        country: 'Uganda',
        address: '',
        status: 'ACTIVE',
      };
    }
    this.modalOpen.set(true);
  }

  protected onContactStaffSelect(staffId: string): void {
    this.selectedContactStaffId = staffId;
    if (!staffId) return;
    const staff = this.store.staffMembers().find((s) => s.id === staffId);
    if (staff) {
      this.form.contactPerson = `${staff.surname} ${staff.lastName}`;
      this.form.email = staff.email;
      this.form.phone = staff.phone;
    }
  }

  protected closeModal(): void {
    this.modalOpen.set(false);
  }

  protected saveOrganisation(): void {
    if (!this.form.code.trim() || !this.form.name.trim()) return;
    this.store.saveOrganisation(
      {
        code: this.form.code.trim().toUpperCase(),
        name: this.form.name.trim(),
        organisationType: this.form.organisationType,
        contactPerson: this.form.contactPerson.trim(),
        email: this.form.email.trim().toLowerCase(),
        phone: this.form.phone.trim(),
        city: this.form.city.trim(),
        country: this.form.country.trim(),
        address: this.form.address.trim(),
        status: this.form.status,
      },
      this.editingId(),
      !this.editingId() && this.assignContactAsAdmin
    );
    this.closeModal();
  }
}

