import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ActivityReport,
  DEPARTMENTS,
  DUTY_STATIONS,
  OFFICER_DESIGNATIONS,
  OTHER_DESIGNATION_OPTION,
  OTHER_DUTY_STATION_OPTION,
  ReportReviewStatus,
} from '../../core/models/stafftrack.models';
import { StaffTrackStore } from '../../core/services/stafftrack.store';
import { Icon } from '../../shared/icon/icon';

@Component({
  selector: 'app-activity-reports',
  imports: [FormsModule, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .report-list {
      display: grid;
      gap: 1.1rem;
    }
    .report-card {
      background: var(--bg-elevated);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-sm);
      overflow: hidden;
    }
    .report-head {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-start;
      justify-content: space-between;
      gap: 0.85rem;
      padding: 1.15rem 1.35rem;
      border-bottom: 1px solid var(--border);
      background: var(--bg-subtle);
    }
    .report-title-block {
      display: grid;
      gap: 0.25rem;
    }
    .report-title-block h2 {
      font-size: 1.05rem;
      font-weight: 700;
    }
    .report-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 0.6rem;
      font-size: 0.8rem;
      color: var(--text-muted);
    }
    .quadrant-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 1rem;
      padding: 1.25rem 1.35rem;
    }
    .quadrant {
      padding: 0.95rem 1.05rem;
      border-radius: var(--radius);
      border: 1px solid var(--border);
      background: var(--bg-subtle);
      display: grid;
      gap: 0.35rem;
      align-content: start;
    }
    .quadrant.issue-alert {
      border-color: color-mix(in srgb, var(--danger) 38%, var(--border));
      background: var(--danger-bg);
    }
    .quadrant h3 {
      font-size: 0.76rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--brand-500);
    }
    .quadrant.issue-alert h3 {
      color: var(--danger);
    }
    .quadrant p {
      font-size: 0.87rem;
      line-height: 1.5;
    }
    .report-foot {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.85rem 1.35rem;
      border-top: 1px solid var(--border);
      font-size: 0.82rem;
    }
    .foot-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.45rem;
    }
  `,
  template: `
    <div class="page">
      <header class="page-header">
        <div class="page-title">
          <h1>Activity Reporting</h1>
          <p>
            Record structured field, meeting, and assignment debriefs capturing what was done,
            what was discussed, what was achieved, and any outstanding issues.
          </p>
        </div>
        <div class="header-actions">
          <button type="button" class="btn btn-primary" (click)="openModal()">
            <app-icon name="plus" />
            <span>Submit Activity Report</span>
          </button>
        </div>
      </header>

      <!-- Filter Toolbar Card -->
      <section class="card">
        <div class="toolbar" style="border-bottom: 0; border-radius: var(--radius-lg)">
          <label class="search-box">
            <span class="search-icon"><app-icon name="search" /></span>
            <input
              type="search"
              class="input"
              placeholder="Search report title, officer, what was done, discussed, achieved, or issues..."
              [ngModel]="searchQuery()"
              (ngModelChange)="searchQuery.set($event)"
            />
          </label>

          <select
            class="select filter-select"
            aria-label="Filter by department"
            [ngModel]="departmentFilter()"
            (ngModelChange)="departmentFilter.set($event)"
          >
            <option value="ALL">All Departments</option>
            @for (dept of departments; track dept) {
              <option [value]="dept">{{ dept }}</option>
            }
          </select>

          <select
            class="select filter-select"
            aria-label="Filter by review status"
            [ngModel]="reviewFilter()"
            (ngModelChange)="reviewFilter.set($event)"
          >
            <option value="ALL">All Review Statuses</option>
            <option value="SUBMITTED">Submitted (Pending Review)</option>
            <option value="ACTION_REQUIRED">Management Action Required</option>
            <option value="REVIEWED">Reviewed &amp; Closed</option>
          </select>
        </div>
      </section>

      <!-- Structured 4-Quadrant Activity Reports -->
      <section class="report-list" aria-label="Submitted activity reports">
        @for (rep of filteredReports(); track rep.id) {
          <article class="report-card">
            <header class="report-head">
              <div class="report-title-block">
                <div style="display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap">
                  <span class="badge badge-info">{{ rep.id }}</span>
                  <h2>{{ rep.title }}</h2>
                </div>
                <div class="report-meta">
                  <span><strong>Officer:</strong> {{ rep.officerName }} ({{ rep.officerDesignation }} · {{ rep.dutyStation }} · {{ rep.department }})</span>
                  <span>·</span>
                  <span><strong>Date:</strong> {{ rep.reportDate }}</span>
                  <span>·</span>
                  <span><strong>Linked Engagement:</strong> {{ rep.relatedEngagement }}</span>
                </div>
              </div>
              <div style="display: flex; align-items: center; gap: 0.45rem; flex-wrap: wrap">
                @if (rep.requiresManagementIntervention) {
                  <span class="badge badge-danger">Escalated to Management</span>
                }
                <span [class]="reviewBadgeClass(rep.reviewStatus)">
                  {{ reviewLabel(rep.reviewStatus) }}
                </span>
              </div>
            </header>

            <div class="quadrant-grid">
              <div class="quadrant">
                <h3>1. What Was Done</h3>
                <p>{{ rep.whatWasDone }}</p>
              </div>
              <div class="quadrant">
                <h3>2. What Was Discussed</h3>
                <p>{{ rep.whatWasDiscussed }}</p>
              </div>
              <div class="quadrant">
                <h3>3. What Was Achieved</h3>
                <p>{{ rep.whatWasAchieved }}</p>
              </div>
              <div class="quadrant" [class.issue-alert]="rep.requiresManagementIntervention">
                <h3>4. Outstanding Issues &amp; Escalations</h3>
                <p>{{ rep.outstandingIssues }}</p>
              </div>
            </div>

            <footer class="report-foot">
              <span style="color: var(--text-muted)">
                <strong>Stakeholders / Participants Met:</strong> {{ rep.stakeholdersMet }}
              </span>
              <div class="foot-actions">
                @if (rep.reviewStatus !== 'REVIEWED') {
                  <button
                    type="button"
                    class="btn btn-sm btn-success"
                    (click)="store.updateReportReviewStatus(rep.id, 'REVIEWED')"
                  >
                    <app-icon name="check" />
                    Mark Reviewed
                  </button>
                }
                <button type="button" class="btn btn-sm" (click)="openModal(rep)">
                  <app-icon name="edit" />
                  Edit
                </button>
                <button
                  type="button"
                  class="btn btn-sm btn-danger"
                  (click)="store.deleteActivityReport(rep.id)"
                >
                  <app-icon name="trash" />
                </button>
              </div>
            </footer>
          </article>
        } @empty {
          <div class="card" style="padding: 2.5rem; text-align: center; color: var(--text-muted)">
            No activity reports match your current filter.
          </div>
        }
      </section>

      <!-- Submit / Edit Activity Report Modal -->
      @if (modalOpen()) {
        <div class="modal-backdrop">
          <div
            class="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="rep-modal-title"
          >
            <div class="modal-header">
              <h2 id="rep-modal-title">
                {{ editingId() ? 'Edit Activity Report' : 'Submit Activity Report' }}
              </h2>
              <button type="button" class="btn btn-sm btn-ghost" (click)="closeModal()">
                <app-icon name="close" />
              </button>
            </div>

            <form (ngSubmit)="saveForm()">
              <div class="modal-body">
                <div class="form-grid">
                  <div class="field field-full">
                    <label for="rep-title">Report Title *</label>
                    <input
                      id="rep-title"
                      name="title"
                      class="input"
                      required
                      [(ngModel)]="form.title"
                      placeholder="e.g. Mbarara Cluster Field Inspection – Day 1 & 2 Debrief"
                    />
                  </div>
                  <div class="field">
                    <label for="rep-date">Report Date *</label>
                    <input
                      id="rep-date"
                      type="date"
                      name="reportDate"
                      class="input"
                      required
                      [(ngModel)]="form.reportDate"
                    />
                  </div>
                  <div class="field">
                    <label for="rep-officer">Reporting Officer *</label>
                    <input
                      id="rep-officer"
                      name="officerName"
                      class="input"
                      required
                      [(ngModel)]="form.officerName"
                      placeholder="e.g. Grace Namusisi"
                    />
                  </div>
                  <div class="field">
                    <label for="rep-desig">Designation of Officer *</label>
                    <select
                      id="rep-desig"
                      name="designationOption"
                      class="select"
                      required
                      [(ngModel)]="designationOption"
                    >
                      @for (des of designations; track des) {
                        <option [value]="des">{{ des }}</option>
                      }
                      <option [value]="otherOption">{{ otherOption }}</option>
                    </select>
                  </div>
                  @if (designationOption === otherOption) {
                    <div class="field">
                      <label for="rep-desig-other">Specify Other Designation *</label>
                      <input
                        id="rep-desig-other"
                        name="otherDesignation"
                        class="input"
                        required
                        [(ngModel)]="otherDesignation"
                        placeholder="Enter custom job title / designation"
                      />
                    </div>
                  }
                  <div class="field">
                    <label for="rep-dept">Department / Unit *</label>
                    <select id="rep-dept" name="department" class="select" [(ngModel)]="form.department">
                      @for (dept of departments; track dept) {
                        <option [value]="dept">{{ dept }}</option>
                      }
                    </select>
                  </div>
                  <div class="field">
                    <label for="rep-station">Duty station / office / settlement *</label>
                    <select
                      id="rep-station"
                      name="dutyStationOption"
                      class="select"
                      required
                      [(ngModel)]="dutyStationOption"
                    >
                      @for (st of dutyStations; track st) {
                        <option [value]="st">{{ st }}</option>
                      }
                      <option [value]="otherStationOption">{{ otherStationOption }}</option>
                    </select>
                  </div>
                  @if (dutyStationOption === otherStationOption) {
                    <div class="field">
                      <label for="rep-station-other">Specify Other Duty Station *</label>
                      <input
                        id="rep-station-other"
                        name="otherDutyStation"
                        class="input"
                        required
                        [(ngModel)]="otherDutyStation"
                        placeholder="Enter custom duty station / office / settlement"
                      />
                    </div>
                  }
                  <div class="field">
                    <label for="rep-rel">Related Engagement / Assignment</label>
                    <input
                      id="rep-rel"
                      name="relatedEngagement"
                      class="input"
                      [(ngModel)]="form.relatedEngagement"
                      placeholder="e.g. Field Assignment (OOO-101)"
                    />
                  </div>
                  <div class="field field-full">
                    <label for="rep-stake">Stakeholders / Participants Met *</label>
                    <input
                      id="rep-stake"
                      name="stakeholdersMet"
                      class="input"
                      required
                      [(ngModel)]="form.stakeholdersMet"
                      placeholder="Who attended or was engaged during the activity?"
                    />
                  </div>
                  <div class="field field-full">
                    <label for="rep-done">1. What Was Done *</label>
                    <textarea
                      id="rep-done"
                      name="whatWasDone"
                      class="textarea"
                      required
                      [(ngModel)]="form.whatWasDone"
                      placeholder="Record concrete tasks and activities executed..."
                    ></textarea>
                  </div>
                  <div class="field field-full">
                    <label for="rep-disc">2. What Was Discussed *</label>
                    <textarea
                      id="rep-disc"
                      name="whatWasDiscussed"
                      class="textarea"
                      required
                      [(ngModel)]="form.whatWasDiscussed"
                      placeholder="Key deliberations, feedback, and agreements..."
                    ></textarea>
                  </div>
                  <div class="field field-full">
                    <label for="rep-ach">3. What Was Achieved *</label>
                    <textarea
                      id="rep-ach"
                      name="whatWasAchieved"
                      class="textarea"
                      required
                      [(ngModel)]="form.whatWasAchieved"
                      placeholder="Measurable outputs, milestones, and deliverables completed..."
                    ></textarea>
                  </div>
                  <div class="field field-full">
                    <label for="rep-iss">4. Outstanding Issues &amp; Follow-Ups *</label>
                    <textarea
                      id="rep-iss"
                      name="outstandingIssues"
                      class="textarea"
                      required
                      [(ngModel)]="form.outstandingIssues"
                      placeholder="Unresolved challenges, risks, or support needed..."
                    ></textarea>
                  </div>
                  <div class="field">
                    <label style="display: flex; align-items: center; gap: 0.5rem; margin-top: 0.4rem">
                      <input
                        type="checkbox"
                        name="requiresManagementIntervention"
                        [(ngModel)]="form.requiresManagementIntervention"
                      />
                      <span>Flag for Management Intervention</span>
                    </label>
                  </div>
                </div>
              </div>

              <div class="modal-footer">
                <button type="button" class="btn" (click)="closeModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">
                  <app-icon name="check" />
                  <span>{{ editingId() ? 'Save Changes' : 'Publish Report' }}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
})
export class ActivityReports {
  protected readonly store = inject(StaffTrackStore);
  protected readonly departments = DEPARTMENTS;
  protected readonly designations = OFFICER_DESIGNATIONS;
  protected readonly otherOption = OTHER_DESIGNATION_OPTION;
  protected readonly dutyStations = DUTY_STATIONS;
  protected readonly otherStationOption = OTHER_DUTY_STATION_OPTION;

  protected readonly searchQuery = signal('');
  protected readonly departmentFilter = signal('ALL');
  protected readonly reviewFilter = signal<'ALL' | ReportReviewStatus>('ALL');

  protected readonly modalOpen = signal(false);
  protected readonly editingId = signal<string | undefined>(undefined);
  protected designationOption = OFFICER_DESIGNATIONS[0];
  protected otherDesignation = '';
  protected dutyStationOption = DUTY_STATIONS[0];
  protected otherDutyStation = '';
  protected form = this.emptyForm();

  protected readonly filteredReports = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const dept = this.departmentFilter();
    const rev = this.reviewFilter();
    return this.store.activityReports().filter((r) => {
      if (dept !== 'ALL' && r.department !== dept) return false;
      if (rev !== 'ALL' && r.reviewStatus !== rev) return false;
      if (!q) return true;
      return (
        r.title.toLowerCase().includes(q) ||
        r.officerName.toLowerCase().includes(q) ||
        r.whatWasDone.toLowerCase().includes(q) ||
        r.whatWasDiscussed.toLowerCase().includes(q) ||
        r.whatWasAchieved.toLowerCase().includes(q) ||
        r.outstandingIssues.toLowerCase().includes(q)
      );
    });
  });

  protected openModal(existing?: ActivityReport): void {
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
    if (!this.form.title.trim() || !this.form.officerName.trim()) return;
    const resolvedDesignation =
      this.designationOption === this.otherOption
        ? this.otherDesignation.trim() || 'Officer'
        : this.designationOption;
    const resolvedStation =
      this.dutyStationOption === this.otherStationOption
        ? this.otherDutyStation.trim() || DUTY_STATIONS[0]
        : this.dutyStationOption;
    const reviewStatus: ReportReviewStatus = this.form.requiresManagementIntervention
      ? 'ACTION_REQUIRED'
      : this.form.reviewStatus;
    this.store.saveActivityReport(
      {
        ...this.form,
        officerDesignation: resolvedDesignation,
        dutyStation: resolvedStation,
        reviewStatus,
      },
      this.editingId()
    );
    this.closeModal();
  }

  protected reviewLabel(status: ReportReviewStatus): string {
    switch (status) {
      case 'REVIEWED':
        return 'Reviewed';
      case 'ACTION_REQUIRED':
        return 'Action Required';
      default:
        return 'Submitted';
    }
  }

  protected reviewBadgeClass(status: ReportReviewStatus): string {
    switch (status) {
      case 'REVIEWED':
        return 'badge badge-success';
      case 'ACTION_REQUIRED':
        return 'badge badge-danger';
      default:
        return 'badge badge-warning';
    }
  }

  private emptyForm(): Omit<ActivityReport, 'id'> {
    return {
      reportDate: '2026-11-24',
      title: '',
      relatedEngagement: '',
      officerName: '',
      officerDesignation: OFFICER_DESIGNATIONS[0],
      department: DEPARTMENTS[0],
      dutyStation: DUTY_STATIONS[0],
      stakeholdersMet: '',
      whatWasDone: '',
      whatWasDiscussed: '',
      whatWasAchieved: '',
      outstandingIssues: '',
      requiresManagementIntervention: false,
      reviewStatus: 'SUBMITTED',
    };
  }
}

