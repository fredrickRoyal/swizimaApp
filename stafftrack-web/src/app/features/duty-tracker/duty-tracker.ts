import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  DEPARTMENTS,
  DUTY_STATIONS,
  DUTY_STATUS_LABELS,
  DutyActivity,
  DutyStatus,
  OFFICER_DESIGNATIONS,
  OTHER_DESIGNATION_OPTION,
  OTHER_DUTY_STATION_OPTION,
  PriorityLevel,
} from '../../core/models/stafftrack.models';
import { StaffTrackStore } from '../../core/services/stafftrack.store';
import { Icon } from '../../shared/icon/icon';

@Component({
  selector: 'app-duty-tracker',
  imports: [FormsModule, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .cell-stack {
      display: grid;
      gap: 0.15rem;
    }
    .cell-stack small {
      color: var(--text-muted);
      font-size: 0.76rem;
    }
    .progress-wrap {
      display: grid;
      gap: 0.35rem;
      min-width: 145px;
    }
    .progress-meta {
      display: flex;
      justify-content: space-between;
      font-size: 0.76rem;
      font-weight: 600;
    }
    .outcome-text {
      max-width: 36ch;
      font-size: 0.83rem;
      line-height: 1.45;
    }
    .action-btns {
      display: flex;
      flex-wrap: wrap;
      gap: 0.35rem;
    }
  `,
  template: `
    <div class="page">
      <header class="page-header">
        <div class="page-title">
          <h1>Out-of-Office Notifications</h1>
          <p>
            Capture out-of-office notifications, official assignments, completion progress, achieved
            outcomes, and required follow-up actions.
          </p>
        </div>
        <div class="header-actions">
          <button type="button" class="btn btn-primary" (click)="openModal()">
            <app-icon name="plus" />
            <span>Log Out-of-Office Notification</span>
          </button>
        </div>
      </header>

      <!-- Summary KPIs -->
      <section class="kpi-grid" aria-label="Out-of-office notification metrics">
        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Total Logged Notifications</span>
            <span class="kpi-icon"><app-icon name="clipboard" /></span>
          </div>
          <div class="kpi-value">{{ store.dutyActivities().length }}</div>
          <div class="kpi-sub">Across all active units</div>
        </article>

        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Completed Engagements</span>
            <span class="kpi-icon" style="background: var(--success-bg); color: var(--success)">
              <app-icon name="check" />
            </span>
          </div>
          <div class="kpi-value">{{ completedCount() }}</div>
          <div class="kpi-sub">100% deliverable completion</div>
        </article>

        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">In Progress</span>
            <span class="kpi-icon"><app-icon name="clock" /></span>
          </div>
          <div class="kpi-value">{{ inProgressCount() }}</div>
          <div class="kpi-sub">Active execution</div>
        </article>

        <article class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-label">Follow-Up / Blocked</span>
            <span class="kpi-icon" style="background: var(--danger-bg); color: var(--danger)">
              <app-icon name="alert" />
            </span>
          </div>
          <div class="kpi-value">{{ followUpOrBlockedCount() }}</div>
          <div class="kpi-sub">Requires follow-up action</div>
        </article>
      </section>

      <!-- Out-of-Office Notifications Table -->
      <section class="card">
        <div class="card-header">
          <div>
            <h2>Out-of-Office Notifications, Progress, Outcomes &amp; Follow-Ups</h2>
            <p>Showing {{ filteredList().length }} of {{ store.dutyActivities().length }} out-of-office notifications</p>
          </div>
        </div>

        <div class="toolbar">
          <label class="search-box">
            <span class="search-icon"><app-icon name="search" /></span>
            <input
              type="search"
              class="input"
              placeholder="Search activity, officer, outcome, or follow-up action..."
              [ngModel]="searchQuery()"
              (ngModelChange)="searchQuery.set($event)"
            />
          </label>

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
            aria-label="Filter by priority"
            [ngModel]="priorityFilter()"
            (ngModelChange)="priorityFilter.set($event)"
          >
            <option value="ALL">All Priorities</option>
            <option value="HIGH">High Priority</option>
            <option value="MEDIUM">Medium Priority</option>
            <option value="LOW">Low Priority</option>
          </select>
        </div>

        <div tabindex="0" role="region" aria-labelledby="duty-table-caption" class="table-wrapper">
          <table class="data-table">
            <caption id="duty-table-caption">
              Out-of-Office Notifications, assignments, progress, outcomes and follow-up actions
            </caption>
            <thead>
              <tr>
                <th>Date &amp; Officer</th>
                <th>Assignment &amp; Category</th>
                <th>Progress &amp; Status</th>
                <th>Outcomes Achieved</th>
                <th>Follow-Up Action &amp; Due</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (item of filteredList(); track item.id) {
                <tr>
                  <th>
                    <div class="cell-stack">
                      <span>{{ item.officerName }}</span>
                      <small>{{ item.officerDesignation }} · {{ item.dutyStation }}</small>
                      <small>{{ item.department }} · {{ item.date }}</small>
                    </div>
                  </th>
                  <td data-label="Assignment & Category">
                    <div class="cell-stack">
                      <strong>{{ item.activityTitle }}</strong>
                      <div style="display: flex; gap: 0.35rem; flex-wrap: wrap">
                        <span class="badge badge-info">{{ item.assignmentCategory }}</span>
                        <span [class]="priorityBadgeClass(item.priority)">{{ item.priority }}</span>
                      </div>
                    </div>
                  </td>
                  <td data-label="Progress & Status">
                    <div class="progress-wrap">
                      <div class="progress-meta">
                        <span [class]="statusBadgeClass(item.status)">
                          {{ statusLabels[item.status] }}
                        </span>
                        <span>{{ item.progressPercent }}%</span>
                      </div>
                      <div class="progress-track">
                        <div class="progress-fill" [style.width.%]="item.progressPercent"></div>
                      </div>
                    </div>
                  </td>
                  <td data-label="Outcomes Achieved">
                    <div class="outcome-text">{{ item.outcomes }}</div>
                  </td>
                  <td data-label="Follow-Up Action">
                    <div class="outcome-text">
                      <div>{{ item.followUpAction }}</div>
                      <small style="color: var(--text-muted)">
                        Owner: <strong>{{ item.followUpOwner }}</strong> · Due {{ item.followUpDueDate }}
                      </small>
                    </div>
                  </td>
                  <td data-label="Actions">
                    <div class="action-btns">
                      @if (item.status !== 'COMPLETED') {
                        <button
                          type="button"
                          class="btn btn-sm btn-success"
                          (click)="store.updateDutyProgress(item.id, 100, 'COMPLETED')"
                        >
                          <app-icon name="check" />
                          Complete
                        </button>
                      }
                      <button
                        type="button"
                        class="btn btn-sm"
                        aria-label="Edit duty activity"
                        (click)="openModal(item)"
                      >
                        <app-icon name="edit" />
                      </button>
                      <button
                        type="button"
                        class="btn btn-sm btn-danger"
                        aria-label="Delete duty activity"
                        (click)="store.deleteDutyActivity(item.id)"
                      >
                        <app-icon name="trash" />
                      </button>
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="6" style="text-align: center; padding: 2.2rem; color: var(--text-muted)">
                    No duty activities match the selected filters.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>

      <!-- Create / Edit Duty Activity Modal -->
      @if (modalOpen()) {
        <div class="modal-backdrop">
          <div
            class="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="duty-modal-title"
          >
            <div class="modal-header">
              <h2 id="duty-modal-title">
                {{ editingId() ? 'Update Out-of-Office Notification' : 'Record Out-of-Office Notification' }}
              </h2>
              <button type="button" class="btn btn-sm btn-ghost" (click)="closeModal()">
                <app-icon name="close" />
              </button>
            </div>

            <form (ngSubmit)="saveForm()">
              <div class="modal-body">
                <div class="form-grid">
                  <div class="field">
                    <label for="duty-date">Activity Date *</label>
                    <input
                      id="duty-date"
                      type="date"
                      name="date"
                      class="input"
                      required
                      [(ngModel)]="form.date"
                    />
                  </div>
                  <div class="field">
                    <label for="duty-officer">Officer Name *</label>
                    <input
                      id="duty-officer"
                      name="officerName"
                      class="input"
                      required
                      [(ngModel)]="form.officerName"
                      placeholder="e.g. Daniel Okello"
                    />
                  </div>
                  <div class="field">
                    <label for="duty-desig">Designation of Officer *</label>
                    <select
                      id="duty-desig"
                      name="designationOption"
                      class="select"
                      required
                      [(ngModel)]="designationOption"
                    >
                      @for (des of store.designations(); track des) {
                        <option [value]="des">{{ des }}</option>
                      }
                      <option [value]="otherOption">{{ otherOption }}</option>
                    </select>
                  </div>
                  @if (designationOption === otherOption) {
                    <div class="field">
                      <label for="duty-desig-other">Specify Other Designation *</label>
                      <input
                        id="duty-desig-other"
                        name="otherDesignation"
                        class="input"
                        required
                        [(ngModel)]="otherDesignation"
                        placeholder="Enter custom job title / designation"
                      />
                    </div>
                  }
                  <div class="field">
                    <label for="duty-dept">Department / Unit *</label>
                    <select id="duty-dept" name="department" class="select" [(ngModel)]="form.department">
                      @for (dept of store.departments(); track dept) {
                        <option [value]="dept">{{ dept }}</option>
                      }
                    </select>
                  </div>
                  <div class="field">
                    <label for="duty-station">Duty station / office / settlement *</label>
                    <select
                      id="duty-station"
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
                      <label for="duty-station-other">Specify Other Duty Station *</label>
                      <input
                        id="duty-station-other"
                        name="otherDutyStation"
                        class="input"
                        required
                        [(ngModel)]="otherDutyStation"
                        placeholder="Enter custom duty station / office / settlement"
                      />
                    </div>
                  }
                  <div class="field">
                    <label for="duty-cat">Assignment Category *</label>
                    <input
                      id="duty-cat"
                      name="assignmentCategory"
                      class="input"
                      required
                      [(ngModel)]="form.assignmentCategory"
                      placeholder="e.g. Compliance Audit, Client Onboarding"
                    />
                  </div>
                  <div class="field field-full">
                    <label for="duty-title">Activity / Assignment Title *</label>
                    <input
                      id="duty-title"
                      name="activityTitle"
                      class="input"
                      required
                      [(ngModel)]="form.activityTitle"
                      placeholder="e.g. Verify Kampala Central Car Park & Wash Bay Tariffs"
                    />
                  </div>
                  <div class="field">
                    <label for="duty-prio">Priority</label>
                    <select id="duty-prio" name="priority" class="select" [(ngModel)]="form.priority">
                      <option value="HIGH">High</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="LOW">Low</option>
                    </select>
                  </div>
                  <div class="field">
                    <label for="duty-status">Status</label>
                    <select id="duty-status" name="status" class="select" [(ngModel)]="form.status">
                      @for (st of statusOptions; track st) {
                        <option [value]="st">{{ statusLabels[st] }}</option>
                      }
                    </select>
                  </div>
                  <div class="field field-full">
                    <label for="duty-prog">Completion Progress ({{ form.progressPercent }}%)</label>
                    <input
                      id="duty-prog"
                      type="range"
                      min="0"
                      max="100"
                      step="5"
                      name="progressPercent"
                      [(ngModel)]="form.progressPercent"
                    />
                  </div>
                  <div class="field field-full">
                    <label for="duty-outcomes">Outcomes &amp; Deliverables Achieved *</label>
                    <textarea
                      id="duty-outcomes"
                      name="outcomes"
                      class="textarea"
                      required
                      [(ngModel)]="form.outcomes"
                      placeholder="Summarize measurable outcomes achieved..."
                    ></textarea>
                  </div>
                  <div class="field field-full">
                    <label for="duty-followup">Follow-Up Action Required *</label>
                    <input
                      id="duty-followup"
                      name="followUpAction"
                      class="input"
                      required
                      [(ngModel)]="form.followUpAction"
                      placeholder="Next steps or pending follow-up action..."
                    />
                  </div>
                  <div class="field">
                    <label for="duty-owner">Follow-Up Responsible Officer *</label>
                    <input
                      id="duty-owner"
                      name="followUpOwner"
                      class="input"
                      required
                      [(ngModel)]="form.followUpOwner"
                      placeholder="Officer responsible for follow-up"
                    />
                  </div>
                  <div class="field">
                    <label for="duty-due">Follow-Up Target Date *</label>
                    <input
                      id="duty-due"
                      type="date"
                      name="followUpDueDate"
                      class="input"
                      required
                      [(ngModel)]="form.followUpDueDate"
                    />
                  </div>
                </div>
              </div>

              <div class="modal-footer">
                <button type="button" class="btn" (click)="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">
                  <app-icon name="check" />
                  <span>{{ editingId() ? 'Update Notification' : 'Save Notification' }}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
})
export class DutyTracker {
  protected readonly store = inject(StaffTrackStore);
  protected readonly departments = DEPARTMENTS;
  protected readonly designations = OFFICER_DESIGNATIONS;
  protected readonly otherOption = OTHER_DESIGNATION_OPTION;
  protected readonly dutyStations = DUTY_STATIONS;
  protected readonly otherStationOption = OTHER_DUTY_STATION_OPTION;
  protected readonly statusLabels = DUTY_STATUS_LABELS;
  protected readonly statusOptions: DutyStatus[] = [
    'NOT_STARTED',
    'IN_PROGRESS',
    'COMPLETED',
    'FOLLOW_UP_REQUIRED',
    'BLOCKED',
  ];

  protected readonly searchQuery = signal('');
  protected readonly statusFilter = signal<'ALL' | DutyStatus>('ALL');
  protected readonly priorityFilter = signal<'ALL' | PriorityLevel>('ALL');

  protected readonly modalOpen = signal(false);
  protected readonly editingId = signal<string | undefined>(undefined);
  protected designationOption = OFFICER_DESIGNATIONS[0];
  protected otherDesignation = '';
  protected dutyStationOption = DUTY_STATIONS[0];
  protected otherDutyStation = '';
  protected form = this.emptyForm();

  protected readonly completedCount = computed(
    () => this.store.dutyActivities().filter((d) => d.status === 'COMPLETED').length
  );
  protected readonly inProgressCount = computed(
    () => this.store.dutyActivities().filter((d) => d.status === 'IN_PROGRESS').length
  );
  protected readonly followUpOrBlockedCount = computed(
    () =>
      this.store
        .dutyActivities()
        .filter((d) => d.status === 'FOLLOW_UP_REQUIRED' || d.status === 'BLOCKED').length
  );

  protected readonly filteredList = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const st = this.statusFilter();
    const prio = this.priorityFilter();
    return this.store.dutyActivities().filter((item) => {
      if (st !== 'ALL' && item.status !== st) return false;
      if (prio !== 'ALL' && item.priority !== prio) return false;
      if (!q) return true;
      return (
        item.activityTitle.toLowerCase().includes(q) ||
        item.officerName.toLowerCase().includes(q) ||
        item.outcomes.toLowerCase().includes(q) ||
        item.followUpAction.toLowerCase().includes(q) ||
        item.department.toLowerCase().includes(q)
      );
    });
  });

  protected openModal(existing?: DutyActivity): void {
    if (existing) {
      this.editingId.set(existing.id);
      if (this.designations.includes(existing.officerDesignation)) {
        this.designationOption = existing.officerDesignation;
        this.otherDesignation = '';
      } else {
        this.designationOption = this.otherOption;
        this.otherDesignation = existing.officerDesignation || '';
      }
      if (this.dutyStations.includes(existing.dutyStation)) {
        this.dutyStationOption = existing.dutyStation;
        this.otherDutyStation = '';
      } else {
        this.dutyStationOption = this.otherStationOption;
        this.otherDutyStation = existing.dutyStation || '';
      }
      this.form = { ...existing };
    } else {
      this.editingId.set(undefined);
      this.designationOption = OFFICER_DESIGNATIONS[0];
      this.otherDesignation = '';
      this.dutyStationOption = DUTY_STATIONS[0];
      this.otherDutyStation = '';
      this.form = this.emptyForm();
    }
    this.modalOpen.set(true);
  }

  protected closeModal(): void {
    this.modalOpen.set(false);
  }

  protected saveForm(): void {
    if (!this.form.activityTitle.trim() || !this.form.officerName.trim()) return;
    const resolvedDesignation =
      this.designationOption === this.otherOption
        ? this.otherDesignation.trim() || 'Officer'
        : this.designationOption;
    const resolvedStation =
      this.dutyStationOption === this.otherStationOption
        ? this.otherDutyStation.trim() || DUTY_STATIONS[0]
        : this.dutyStationOption;
    this.store.saveDutyActivity(
      {
        ...this.form,
        officerDesignation: resolvedDesignation,
        dutyStation: resolvedStation,
      },
      this.editingId()
    );
    this.closeModal();
  }

  protected statusBadgeClass(status: DutyStatus): string {
    switch (status) {
      case 'COMPLETED':
        return 'badge badge-success';
      case 'IN_PROGRESS':
        return 'badge badge-info';
      case 'FOLLOW_UP_REQUIRED':
        return 'badge badge-warning';
      case 'BLOCKED':
        return 'badge badge-danger';
      default:
        return 'badge';
    }
  }

  protected priorityBadgeClass(prio: PriorityLevel): string {
    switch (prio) {
      case 'HIGH':
        return 'badge badge-danger';
      case 'MEDIUM':
        return 'badge badge-warning';
      default:
        return 'badge';
    }
  }

  private emptyForm(): Omit<DutyActivity, 'id'> {
    return {
      date: '2026-11-24',
      officerName: '',
      officerDesignation: OFFICER_DESIGNATIONS[0],
      department: DEPARTMENTS[0],
      dutyStation: DUTY_STATIONS[0],
      activityTitle: '',
      assignmentCategory: 'Field Operations',
      priority: 'HIGH',
      progressPercent: 50,
      status: 'IN_PROGRESS',
      outcomes: '',
      followUpAction: '',
      followUpOwner: '',
      followUpDueDate: '2026-11-27',
    };
  }
}

