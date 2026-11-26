import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  DEPARTMENTS,
  DUTY_STATIONS,
  OFFICER_DESIGNATIONS,
  OTHER_DESIGNATION_OPTION,
  OTHER_DUTY_STATION_OPTION,
  WeeklyConsolidatedReport,
} from '../../core/models/stafftrack.models';
import { StaffTrackStore } from '../../core/services/stafftrack.store';
import { Icon } from '../../shared/icon/icon';

@Component({
  selector: 'app-weekly-reports',
  imports: [FormsModule, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .weekly-stack {
      display: grid;
      gap: 1.35rem;
    }
    .weekly-card {
      background: var(--bg-elevated);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-sm);
      overflow: hidden;
    }
    .weekly-header {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-start;
      justify-content: space-between;
      gap: 1rem;
      padding: 1.25rem 1.4rem;
      border-bottom: 1px solid var(--border);
      background: linear-gradient(135deg, rgb(0 94 184 / 0.09), rgb(247 136 18 / 0.08));
    }
    .weekly-title {
      display: grid;
      gap: 0.25rem;
    }
    .weekly-title h2 {
      font-size: 1.15rem;
      font-weight: 800;
    }
    .weekly-meta {
      font-size: 0.81rem;
      color: var(--text-muted);
      display: flex;
      flex-wrap: wrap;
      gap: 0.55rem;
    }
    .weekly-body {
      padding: 1.35rem;
      display: grid;
      gap: 1.35rem;
    }
    .exec-box {
      padding: 1rem 1.15rem;
      border-radius: var(--radius);
      background: var(--bg-subtle);
      border-inline-start: 4px solid var(--brand-500);
      font-size: 0.92rem;
    }
    .section-block {
      display: grid;
      gap: 0.7rem;
    }
    .section-block h3 {
      font-size: 0.88rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: var(--brand-500);
    }
    .outcome-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 0.8rem;
    }
    .outcome-card {
      padding: 0.85rem 1rem;
      border-radius: var(--radius);
      border: 1px solid var(--border);
      background: var(--bg-subtle);
      display: grid;
      gap: 0.25rem;
      font-size: 0.86rem;
    }
    .outcome-card small {
      color: var(--text-muted);
      font-size: 0.76rem;
    }
  `,
  template: `
    <div class="page">
      <header class="page-header">
        <div class="page-title">
          <h1>Weekly Activity Reports</h1>
          <p>
            Consolidate activities undertaken during the week, achieved outcomes, pending follow-up
            actions, and responsible officers into executive weekly briefs.
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
          <button type="button" class="btn btn-primary" (click)="openConsolidateModal()">
            <app-icon name="sparkles" />
            <span>Auto-Consolidate Weekly Report</span>
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

      <!-- Consolidated Weekly Briefs -->
      <section class="weekly-stack" aria-label="Consolidated weekly activity reports">
        @for (report of store.weeklyReports(); track report.id) {
          <article class="weekly-card">
            <div class="weekly-body">
              <!-- 1. Consolidated Activities Undertaken -->
              <div class="section-block">
                <h3>1. Consolidated Activities Undertaken ({{ report.activitiesUndertaken.length }})</h3>
                <div
                  tabindex="0"
                  role="region"
                  [attr.aria-label]="'Activities undertaken for ' + report.weekLabel"
                  class="table-wrapper"
                >
                  <table class="data-table">
                    <thead>
                      <tr>
                        <th>Officer &amp; Department</th>
                        <th>Date</th>
                        <th>Category</th>
                        <th>Activity Summary</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (act of report.activitiesUndertaken; track $index) {
                        <tr>
                          <th>
                            <div>{{ act.officerName }}</div>
                            <small style="color: var(--text-muted); font-weight: 400">
                              {{ act.officerDesignation }} · {{ act.dutyStation }} · {{ act.department }}
                            </small>
                          </th>
                          <td data-label="Date">{{ act.date }}</td>
                          <td data-label="Category">
                            <span class="badge badge-info">{{ act.category }}</span>
                          </td>
                          <td data-label="Summary">{{ act.summary }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- 2. Pending Actions & Responsible Officers -->
              <div class="section-block">
                <h3>2. Pending Actions &amp; Responsible Officers ({{ report.pendingActions.length }})</h3>
                <div
                  tabindex="0"
                  role="region"
                  [attr.aria-label]="'Pending actions for ' + report.weekLabel"
                  class="table-wrapper"
                >
                  <table class="data-table">
                    <thead>
                      <tr>
                        <th>Responsible Officer</th>
                        <th>Pending Action / Follow-Up</th>
                        <th>Source Module</th>
                        <th>Target Date</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (pa of report.pendingActions; track $index) {
                        <tr>
                          <th>
                            <div>{{ pa.responsibleOfficer }}</div>
                            <small style="color: var(--text-muted); font-weight: 400">
                              {{ pa.dutyStation }} · {{ pa.department }}
                            </small>
                          </th>
                          <td data-label="Pending Action">{{ pa.item }}</td>
                          <td data-label="Source">{{ pa.source }}</td>
                          <td data-label="Target Date">{{ pa.targetDate }}</td>
                          <td data-label="Status">
                            <span class="badge badge-warning">{{ pa.status }}</span>
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </article>
        }
      </section>

      <!-- Auto-Consolidate Weekly Report Modal -->
      @if (modalOpen()) {
        <div class="modal-backdrop">
          <div
            class="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="wk-modal-title"
          >
            <div class="modal-header">
              <h2 id="wk-modal-title">Auto-Consolidate Weekly Activity Report</h2>
              <button type="button" class="btn btn-sm btn-ghost" (click)="modalOpen.set(false)">
                <app-icon name="close" />
              </button>
            </div>

            <form (ngSubmit)="generateReport()">
              <div class="modal-body">
                <p style="font-size: 0.88rem; color: var(--text-muted)">
                  Automatically pulls all Out-of-Office deployments, Duty Activity logs, and
                  Activity Reports within the selected date window to compile a consolidated weekly
                  brief with outcomes, pending actions, and responsible officers.
                </p>
                <div class="form-grid">
                  <div class="field field-full">
                    <label for="wk-label">Week Title / Reference *</label>
                    <input
                      id="wk-label"
                      name="weekLabel"
                      class="input"
                      required
                      [(ngModel)]="form.weekLabel"
                      placeholder="e.g. Week 48 · 2026 (Nov 23 – Nov 29)"
                    />
                  </div>
                  <div class="field">
                    <label for="wk-start">Week Start Date *</label>
                    <input
                      id="wk-start"
                      type="date"
                      name="weekStart"
                      class="input"
                      required
                      [(ngModel)]="form.weekStart"
                    />
                  </div>
                  <div class="field">
                    <label for="wk-end">Week End Date *</label>
                    <input
                      id="wk-end"
                      type="date"
                      name="weekEnd"
                      class="input"
                      required
                      [(ngModel)]="form.weekEnd"
                    />
                  </div>
                  <div class="field">
                    <label for="wk-dept">Department Scope</label>
                    <select id="wk-dept" name="departmentFilter" class="select" [(ngModel)]="form.departmentFilter">
                      <option value="ALL">All Departments (Organization-Wide)</option>
                      @for (dept of store.departments(); track dept) {
                        <option [value]="dept">{{ dept }}</option>
                      }
                    </select>
                  </div>
                  <div class="field">
                    <label for="wk-station">Duty station / office / settlement *</label>
                    <select
                      id="wk-station"
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
                      <label for="wk-station-other">Specify Other Duty Station *</label>
                      <input
                        id="wk-station-other"
                        name="otherDutyStation"
                        class="input"
                        required
                        [(ngModel)]="otherDutyStation"
                        placeholder="Enter custom duty station / office / settlement"
                      />
                    </div>
                  }
                  <div class="field">
                    <label for="wk-by">Consolidated By (Officer Name) *</label>
                    <input
                      id="wk-by"
                      name="generatedBy"
                      class="input"
                      required
                      [(ngModel)]="form.generatedBy"
                    />
                  </div>
                  <div class="field">
                    <label for="wk-desig">Designation of Officer *</label>
                    <select
                      id="wk-desig"
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
                      <label for="wk-desig-other">Specify Other Designation *</label>
                      <input
                        id="wk-desig-other"
                        name="otherDesignation"
                        class="input"
                        required
                        [(ngModel)]="otherDesignation"
                        placeholder="Enter custom job title / designation"
                      />
                    </div>
                  }
                </div>
              </div>

              <div class="modal-footer">
                <button type="button" class="btn" (click)="modalOpen.set(false)">Cancel</button>
                <button type="submit" class="btn btn-primary">
                  <app-icon name="sparkles" />
                  <span>Generate Consolidated Report</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
})
export class WeeklyReports {
  protected readonly store = inject(StaffTrackStore);
  protected readonly departments = DEPARTMENTS;
  protected readonly designations = OFFICER_DESIGNATIONS;
  protected readonly otherOption = OTHER_DESIGNATION_OPTION;
  protected readonly dutyStations = DUTY_STATIONS;
  protected readonly otherStationOption = OTHER_DUTY_STATION_OPTION;
  protected readonly modalOpen = signal(false);

  protected designationOption = OFFICER_DESIGNATIONS[0];
  protected otherDesignation = '';
  protected dutyStationOption = DUTY_STATIONS[0];
  protected otherDutyStation = '';
  protected form = {
    weekLabel: 'Week 48 · 2026 (Nov 20 – Nov 29)',
    weekStart: '2026-11-20',
    weekEnd: '2026-11-29',
    departmentFilter: 'ALL',
    generatedBy: 'Fredrick Kasoma',
  };

  protected openConsolidateModal(): void {
    this.designationOption = this.store.designations()[0] || 'Principal Officer';
    this.dutyStationOption = this.store.dutyStations()[0] || 'OPM Main';
    this.modalOpen.set(true);
  }

  protected generateReport(): void {
    const resolvedDesignation =
      this.designationOption === this.otherOption
        ? this.otherDesignation.trim() || 'Officer'
        : this.designationOption || this.store.designations()[0] || 'Officer';
    const resolvedStation =
      this.dutyStationOption === this.otherStationOption
        ? this.otherDutyStation.trim() || 'OPM Main'
        : this.dutyStationOption || this.store.dutyStations()[0] || 'OPM Main';
    this.store.consolidateWeeklyReport(
      this.form.weekStart,
      this.form.weekEnd,
      this.form.weekLabel,
      this.form.departmentFilter,
      this.form.generatedBy,
      resolvedDesignation,
      resolvedStation
    );
    this.modalOpen.set(false);
  }

  protected printReport(): void {
    if (typeof window !== 'undefined') {
      window.print();
    }
  }

  protected exportCsv(report: WeeklyConsolidatedReport): void {
    const rows: string[][] = [
      ['Section', 'Date / Target', 'Officer', 'Duty Station', 'Department', 'Category / Source', 'Details'],
      ...report.activitiesUndertaken.map((a) => [
        'Activity Undertaken',
        a.date,
        `${a.officerName} (${a.officerDesignation})`,
        a.dutyStation,
        a.department,
        a.category,
        a.summary,
      ]),
      ...report.keyOutcomes.map((o) => [
        'Key Outcome',
        report.weekEnd,
        `${o.officerName} (${o.officerDesignation})`,
        o.dutyStation,
        o.department,
        'Outcome',
        o.outcome,
      ]),
      ...report.pendingActions.map((p) => [
        'Pending Action',
        p.targetDate,
        p.responsibleOfficer,
        p.dutyStation,
        p.department,
        p.source,
        `${p.item} [${p.status}]`,
      ]),
    ];

    const csv = rows
      .map((r) => r.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
      .join('\n');

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${report.id}-${report.weekStart}-to-${report.weekEnd}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }
}

