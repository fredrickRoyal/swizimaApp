import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  effect,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  AttendingOfficer,
  ENGAGEMENT_TYPE_LABELS,
  EngagementType,
  OTHER_DESIGNATION_OPTION,
  OTHER_DUTY_STATION_OPTION,
  OutOfOfficeNotification,
  OutOfOfficeStatus,
} from '../../../core/models/stafftrack.models';
import { AuthService } from '../../../core/services/auth.service';
import { ConfirmService } from '../../../core/services/confirm.service';
import { StaffTrackStore } from '../../../core/services/stafftrack.store';
import { Icon } from '../../../shared/icon/icon';

interface AttendingRow {
  selectionMode: 'LIST' | 'CUSTOM';
  selectedStaffId: string;
  customName: string;
  designation: string;
  department: string;
  dutyStation: string;
}

const OTHER_DEPARTMENT_OPTION = 'Other (Specify)';

@Component({
  selector: 'app-out-of-office-form',
  imports: [FormsModule, RouterLink, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .radio-bar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.85rem;
    }
    .radio-stack {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
    }
    .radio-choice {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.55rem 1.1rem;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-strong);
      background: var(--bg-elevated);
      font-weight: 600;
      font-size: 0.9rem;
      cursor: pointer;
    }
    .radio-choice.active {
      border-color: var(--brand-500);
      background: var(--info-bg);
      color: var(--brand-500);
    }
    .staff-summary-box {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.5rem 0.85rem;
      padding: 0.6rem 0.85rem;
      border-radius: var(--radius-sm);
      background: var(--bg-elevated);
      border: 1px solid var(--border);
      font-size: 0.85rem;
    }
    .input-readonly {
      background: var(--bg-subtle);
      color: var(--text);
      font-weight: 600;
      cursor: not-allowed;
      opacity: 0.92;
    }
    .form-footer {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: 0.75rem;
      padding: 1.1rem 1.3rem;
      border-top: 1px solid var(--border);
      background: var(--bg-subtle);
      border-radius: 0 0 var(--radius-lg) var(--radius-lg);
    }
    @media (max-width: 540px) {
      .form-footer .btn {
        flex: 1 1 auto;
      }
    }
  `,
  template: `
    <div class="page">
      <header class="page-header">
        <div class="page-title">
          <div style="display: flex; align-items: center; gap: 0.6rem; flex-wrap: wrap">
            <a routerLink="/out-of-office" class="btn btn-sm">
              <span>← Back to Out-of-Office</span>
            </a>
            @if (editingRef()) {
              <span class="badge badge-info">{{ editingRef() }}</span>
            }
          </div>
          <h1 style="margin-top: 0.35rem">
            {{ editingId() ? 'Update Out-of-Office Notification' : 'Record Out-of-Office Notification' }}
          </h1>
          <p>
            Provide engagement details, planned official purpose &amp; deliverables, and any other
            OPM officers attending with you.
          </p>
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

      <form (ngSubmit)="saveForm()" style="display: grid; gap: 1.35rem">
        <!-- Engagement & Station Details Card -->
        <section class="card">
          <div class="card-header">
            <div>
              <h2>Engagement, Station &amp; Schedule Details</h2>
              <p>Specify your designation, department, duty station, destination, host institution, and dates</p>
            </div>
          </div>

          <div class="card-body">
            <div class="form-grid">
              <div class="field">
                <label for="ooo-officer">Officer Full Name *</label>
                <input
                  id="ooo-officer"
                  name="officerName"
                  class="input input-readonly"
                  required
                  readonly
                  aria-readonly="true"
                  [(ngModel)]="form.officerName"
                />
              </div>

              <div class="field">
                <label for="ooo-role">Designation of Officer *</label>
                <input
                  id="ooo-role"
                  name="designationOption"
                  class="input input-readonly"
                  required
                  readonly
                  aria-readonly="true"
                  [(ngModel)]="designationOption"
                />
              </div>

              <div class="field">
                <label for="ooo-dept">Department / Unit *</label>
                <input
                  id="ooo-dept"
                  name="departmentOption"
                  class="input input-readonly"
                  required
                  readonly
                  aria-readonly="true"
                  [(ngModel)]="departmentOption"
                />
              </div>

              <div class="field">
                <label for="ooo-station">Duty station / office / settlement *</label>
                <input
                  id="ooo-station"
                  name="dutyStationOption"
                  class="input input-readonly"
                  required
                  readonly
                  aria-readonly="true"
                  [(ngModel)]="dutyStationOption"
                />
              </div>

              <div class="field">
                <label for="ooo-type">Engagement Type *</label>
                <select
                  id="ooo-type"
                  name="engagementType"
                  class="select"
                  [(ngModel)]="form.engagementType"
                >
                  @for (type of engagementTypes; track type) {
                    <option [value]="type">{{ engagementLabels[type] }}</option>
                  }
                </select>
              </div>

              <div class="field">
                <label for="ooo-phone">Field Contact Phone *</label>
                <input
                  id="ooo-phone"
                  name="contactPhone"
                  class="input input-readonly"
                  required
                  readonly
                  aria-readonly="true"
                  [(ngModel)]="form.contactPhone"
                  placeholder="+256 700 408 387"
                />
              </div>

              <div class="field">
                <label for="ooo-dest">Destination / Venue *</label>
                <input
                  id="ooo-dest"
                  name="destination"
                  class="input"
                  required
                  [(ngModel)]="form.destination"
                  placeholder="e.g. Nakivale & Oruchinga Refugee Settlements"
                />
              </div>

              <div class="field">
                <label for="ooo-host">Host / Organizer Institution *</label>
                <input
                  id="ooo-host"
                  name="hostOrganization"
                  class="input"
                  required
                  [(ngModel)]="form.hostOrganization"
                  placeholder="e.g. UNHCR & OPM Regional Desk Officer Mbarara"
                />
              </div>

              <div class="field">
                <label for="ooo-start">Start Date *</label>
                <input
                  id="ooo-start"
                  type="date"
                  name="startDate"
                  class="input"
                  required
                  [(ngModel)]="form.startDate"
                />
              </div>

              <div class="field">
                <label for="ooo-end">Expected Return Date *</label>
                <input
                  id="ooo-end"
                  type="date"
                  name="endDate"
                  class="input"
                  required
                  [(ngModel)]="form.endDate"
                />
              </div>
            </div>
          </div>
        </section>

        <!-- Purpose & Deliverables Card -->
        <section class="card">
          <div class="card-header">
            <div>
              <h2>Purpose &amp; Deliverables *</h2>
            </div>
            <button type="button" class="btn btn-sm btn-primary" (click)="addPurposeRow()">
              <app-icon name="plus" />
              <span>Add Purpose / Deliverable</span>
            </button>
          </div>

          <div class="numbered-list" role="region" aria-label="Official Purpose and Deliverables">
            @for (row of purposeRows; track $index) {
              <div class="numbered-row">
                <span class="row-num-badge" aria-hidden="true">{{ $index + 1 }}</span>
                <input
                  type="text"
                  [name]="'purposeRow_' + $index"
                  class="input"
                  required
                  [attr.aria-label]="'Official Purpose or Deliverable ' + ($index + 1)"
                  [(ngModel)]="row.text"
                  placeholder="Describe planned official purpose or deliverable..."
                />
                @if (purposeRows.length > 1) {
                  <button
                    type="button"
                    class="btn btn-sm btn-danger row-icon-btn"
                    title="Remove purpose row"
                    aria-label="Remove purpose row"
                    (click)="removePurposeRow($index)"
                  >
                    <app-icon name="trash" />
                  </button>
                }
              </div>
            }
          </div>
        </section>

        <!-- Other Officers Attending from OPM Card -->
        <section class="card">
          <div class="card-header">
            <div>
              <h2>* Are you attending this activity with other officers from OPM?</h2>
              <p>
                Select Yes to add co-attending officers from the registered staff list or type
                officers not yet in the list.
              </p>
            </div>
          </div>

          <div class="card-body">
            <div class="radio-bar">
              <div class="radio-stack" role="radiogroup" aria-label="Attending with other officers from OPM">
                <label class="radio-choice" [class.active]="attendingWithOthers === true">
                  <input
                    type="radio"
                    name="attendingWithOthers"
                    [value]="true"
                    [(ngModel)]="attendingWithOthers"
                    (ngModelChange)="onAttendingToggle($event)"
                  />
                  <span>Yes</span>
                </label>

                <label class="radio-choice" [class.active]="attendingWithOthers === false">
                  <input
                    type="radio"
                    name="attendingWithOthers"
                    [value]="false"
                    [(ngModel)]="attendingWithOthers"
                    (ngModelChange)="onAttendingToggle($event)"
                  />
                  <span>No</span>
                </label>
              </div>

              @if (attendingWithOthers) {
                <button type="button" class="btn btn-sm btn-primary" (click)="addAttendingOfficerRow()">
                  <app-icon name="plus" />
                  <span>Add Attending Officer</span>
                </button>
              }
            </div>
          </div>

          @if (attendingWithOthers) {
            <div class="repeat-stack" style="border-top: 1px solid var(--border)">
              @for (att of attendingRows; track $index) {
                <div class="repeat-card">
                  <div class="repeat-card-header">
                    <div class="repeat-card-title">
                      <span class="row-num-badge">{{ $index + 1 }}</span>
                      <span>Co-Attending staff {{ $index + 1 }}</span>
                    </div>
                    @if (attendingRows.length > 1) {
                      <button
                        type="button"
                        class="btn btn-sm btn-danger row-icon-btn"
                        title="Remove attending staff"
                        aria-label="Remove attending staff"
                        (click)="removeAttendingOfficerRow($index)"
                      >
                        <app-icon name="trash" />
                      </button>
                    }
                  </div>

                  <div class="form-grid">
                    <div class="field" [class.field-full]="att.selectionMode === 'LIST'">
                      <select
                        [id]="'att-select-' + $index"
                        [name]="'attSelect_' + $index"
                        class="select"
                        [attr.aria-label]="'Co-Attending staff ' + ($index + 1)"
                        [ngModel]="att.selectedStaffId"
                        (ngModelChange)="onAttendingStaffChange($index, $event)"
                      >
                        @for (s of store.staffMembers(); track s.id) {
                          <option [value]="s.id">
                            {{ s.surname }} {{ s.lastName }} ({{ s.designation }})
                          </option>
                        }
                        <option value="__CUSTOM__">Not in list — Type Officer Manually</option>
                      </select>
                    </div>

                    @if (att.selectionMode === 'CUSTOM') {
                      <div class="field">
                        <label [for]="'att-name-' + $index">Officer Full Name *</label>
                        <input
                          [id]="'att-name-' + $index"
                          type="text"
                          [name]="'attCustomName_' + $index"
                          class="input"
                          required
                          [(ngModel)]="att.customName"
                          placeholder="Type full name of officer..."
                        />
                      </div>

                      <div class="field">
                        <label [for]="'att-desig-' + $index">Designation</label>
                        <input
                          [id]="'att-desig-' + $index"
                          type="text"
                          [name]="'attDesig_' + $index"
                          class="input"
                          [(ngModel)]="att.designation"
                          placeholder="Designation / Job title"
                        />
                      </div>

                      <div class="field">
                        <label [for]="'att-station-' + $index">Department / Duty Station</label>
                        <input
                          [id]="'att-station-' + $index"
                          type="text"
                          [name]="'attStation_' + $index"
                          class="input"
                          [(ngModel)]="att.dutyStation"
                          placeholder="Department or Duty Station"
                        />
                      </div>
                    } @else {
                      <div class="field field-full">
                        <div class="staff-summary-box">
                          <strong>{{ att.customName }}</strong>
                          <span class="badge badge-info">{{ att.designation }}</span>
                          <span style="color: var(--text-muted)">
                            {{ att.dutyStation }} · {{ att.department }}
                          </span>
                        </div>
                      </div>
                    }
                  </div>
                </div>
              }
            </div>
          }

          <div class="form-footer">
            <a routerLink="/out-of-office" class="btn">Cancel</a>
            <button type="submit" class="btn btn-primary" [disabled]="submitting()">
              <app-icon name="check" />
              <span>
                {{
                  submitting()
                    ? 'Saving to API...'
                    : editingId()
                    ? 'Save Changes'
                    : 'Submit Out-of-Office Notification'
                }}
              </span>
            </button>
          </div>
        </section>
      </form>
    </div>
  `,
})
export class OutOfOfficeForm implements OnInit {
  protected readonly store = inject(StaffTrackStore);
  protected readonly auth = inject(AuthService);
  private readonly confirm = inject(ConfirmService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  protected readonly otherOption = OTHER_DESIGNATION_OPTION;
  protected readonly otherDepartmentOption = OTHER_DEPARTMENT_OPTION;
  protected readonly otherStationOption = OTHER_DUTY_STATION_OPTION;
  protected readonly engagementLabels = ENGAGEMENT_TYPE_LABELS;
  protected readonly engagementTypes: EngagementType[] = [
    'TRAVEL',
    'MEETING',
    'WORKSHOP',
    'FIELD_ASSIGNMENT',
    'OFFICIAL_ENGAGEMENT',
  ];

  protected readonly editingId = signal<string | undefined>(undefined);
  protected readonly editingRef = signal<string | undefined>(undefined);
  protected readonly submitting = signal(false);

  protected selectedStaffMemberId = '';
  protected designationOption = '';
  protected otherDesignation = '';
  protected departmentOption = '';
  protected otherDepartment = '';
  protected dutyStationOption = '';
  protected otherDutyStation = '';

  protected purposeRows: Array<{ text: string }> = [{ text: '' }];
  protected attendingWithOthers = false;
  protected attendingRows: AttendingRow[] = [];

  protected form = {
    officerName: '',
    engagementType: 'FIELD_ASSIGNMENT' as EngagementType,
    destination: '',
    hostOrganization: '',
    startDate: new Date().toISOString().slice(0, 10),
    endDate: new Date(Date.now() + 2 * 86400000).toISOString().slice(0, 10),
    actingOfficer: 'Assigned by Unit Supervisor',
    contactPhone: '+256 700 408 387',
    status: 'ACTIVE_AWAY' as OutOfOfficeStatus,
  };

  constructor() {
    // Reactively prefill officer profile from logged-in user and Staff Register
    effect(() => {
      this.store.staffMembers();
      this.store.userAccounts();
      this.auth.session();

      if (!this.editingId()) {
        this.applyLoggedInUserProfile();
      }
    });
  }

  ngOnInit(): void {
    this.store.clearApiNotice();
    const id = this.route.snapshot.paramMap.get('id') || undefined;

    if (id) {
      this.editingId.set(id);
      this.store.loadOutOfOfficeById(id, (existing) => {
        this.populateFromExisting(existing);
      });
      return;
    }

    this.applyLoggedInUserProfile();
    this.attendingRows = [this.createDefaultAttendingRow()];
  }

  private applyLoggedInUserProfile(): void {
    const currentSession = this.auth.session();
    const staff = this.store.staffMembers();
    const users = this.store.userAccounts();

    const sessionEmail = (currentSession?.email || currentSession?.identifier || '')
      .trim()
      .toLowerCase();
    const sessionName = (currentSession?.fullName || '').trim().toLowerCase();

    const matchedStaff = staff.find((s) => {
      if (currentSession?.staffId && s.id === currentSession.staffId) return true;
      if (
        sessionEmail &&
        (s.email.toLowerCase() === sessionEmail ||
          s.alternativeEmail?.toLowerCase() === sessionEmail)
      ) {
        return true;
      }
      const full1 = `${s.surname} ${s.lastName}`.trim().toLowerCase();
      const full2 = `${s.lastName} ${s.surname}`.trim().toLowerCase();
      return Boolean(sessionName && (full1 === sessionName || full2 === sessionName));
    });

    const matchedUser = users.find((u) => {
      if (currentSession?.id && u.id === currentSession.id) return true;
      if (
        sessionEmail &&
        (u.email.toLowerCase() === sessionEmail || u.username.toLowerCase() === sessionEmail)
      ) {
        return true;
      }
      return Boolean(sessionName && u.fullName.trim().toLowerCase() === sessionName);
    });

    if (matchedStaff) {
      this.selectedStaffMemberId = matchedStaff.id;
    } else if (currentSession?.staffId) {
      this.selectedStaffMemberId = currentSession.staffId;
    }

    this.form.officerName =
      currentSession?.fullName ||
      (matchedStaff ? `${matchedStaff.surname} ${matchedStaff.lastName}` : '') ||
      matchedUser?.fullName ||
      'Kasoma Fredrick';

    this.designationOption =
      currentSession?.designation ||
      matchedStaff?.designation ||
      matchedUser?.designation ||
      currentSession?.role ||
      'Principal Officer';

    this.departmentOption =
      currentSession?.department ||
      matchedStaff?.department ||
      matchedUser?.department ||
      'Department of Refugees';

    this.dutyStationOption =
      currentSession?.dutyStation ||
      matchedStaff?.dutyStation ||
      matchedUser?.dutyStation ||
      'DOR-Kampala';

    this.form.contactPhone =
      currentSession?.phone ||
      matchedStaff?.phone ||
      matchedUser?.phone ||
      '+256 700 408 387';

    this.cdr.markForCheck();
  }

  private populateFromExisting(existing: OutOfOfficeNotification): void {
    this.editingId.set(existing.id);
    this.editingRef.set(existing.referenceNumber || existing.id);
    this.selectedStaffMemberId = existing.staffMemberId || '';

    this.designationOption = existing.officerRole;
    this.otherDesignation = '';
    this.departmentOption = existing.department;
    this.otherDepartment = '';
    this.dutyStationOption = existing.dutyStation;
    this.otherDutyStation = '';

    const rawPurposes =
      existing.purposeList && existing.purposeList.length > 0
        ? existing.purposeList
        : existing.purpose
        ? existing.purpose.split(/\s*;\s*|\n+/).filter(Boolean)
        : [];
    const savedPurposes = rawPurposes.length > 0 ? rawPurposes : [''];
    this.purposeRows = savedPurposes.map((text) => ({ text }));

    this.attendingWithOthers = Boolean(existing.attendingWithOthers);
    if (existing.attendingOfficers && existing.attendingOfficers.length > 0) {
      this.attendingRows = existing.attendingOfficers.map((ao) => {
        const matchedStaff = this.store
          .staffMembers()
          .find(
            (s) =>
              (ao.staffMemberId && s.id === ao.staffMemberId) ||
              `${s.surname} ${s.lastName}` === ao.name
          );
        if (matchedStaff && !ao.isCustom) {
          return {
            selectionMode: 'LIST',
            selectedStaffId: matchedStaff.id,
            customName: `${matchedStaff.surname} ${matchedStaff.lastName}`,
            designation: matchedStaff.designation,
            department: matchedStaff.department,
            dutyStation: matchedStaff.dutyStation,
          };
        }
        return {
          selectionMode: 'CUSTOM',
          selectedStaffId: '__CUSTOM__',
          customName: ao.name,
          designation: ao.designation,
          department: ao.department,
          dutyStation: ao.dutyStation,
        };
      });
    } else {
      this.attendingRows = [this.createDefaultAttendingRow()];
    }

    this.form = {
      officerName: existing.officerName,
      engagementType: existing.engagementType,
      destination: existing.destination,
      hostOrganization: existing.hostOrganization,
      startDate: existing.startDate,
      endDate: existing.endDate,
      actingOfficer: existing.actingOfficer,
      contactPhone: existing.contactPhone,
      status: existing.status,
    };
    this.cdr.markForCheck();
  }

  protected onPrimaryStaffSelect(staffId: string): void {
    this.selectedStaffMemberId = staffId;
    if (!staffId) return;
    const member = this.store.staffMembers().find((s) => s.id === staffId);
    if (!member) return;

    this.form.officerName = `${member.surname} ${member.lastName}`;
    if (member.phone) {
      this.form.contactPhone = member.phone;
    }

    if (this.store.designations().includes(member.designation)) {
      this.designationOption = member.designation;
      this.otherDesignation = '';
    } else {
      this.designationOption = this.otherOption;
      this.otherDesignation = member.designation;
    }

    if (this.store.departments().includes(member.department)) {
      this.departmentOption = member.department;
      this.otherDepartment = '';
    } else {
      this.departmentOption = this.otherDepartmentOption;
      this.otherDepartment = member.department;
    }

    if (this.store.dutyStations().includes(member.dutyStation)) {
      this.dutyStationOption = member.dutyStation;
      this.otherDutyStation = '';
    } else {
      this.dutyStationOption = this.otherStationOption;
      this.otherDutyStation = member.dutyStation;
    }
  }

  protected addPurposeRow(): void {
    this.purposeRows = [...this.purposeRows, { text: '' }];
    this.cdr.markForCheck();
  }

  protected removePurposeRow(index: number): void {
    if (this.purposeRows.length <= 1) return;
    this.confirm.ask({
      title: 'Remove Purpose / Deliverable',
      message: `Are you sure you want to remove Purpose / Deliverable row #${index + 1}?`,
      confirmLabel: 'Remove',
      onConfirm: () => {
        this.purposeRows =
          this.purposeRows.length > 1
            ? this.purposeRows.filter((_, idx) => idx !== index)
            : [{ text: '' }];
        this.cdr.markForCheck();
      },
    });
  }

  protected onAttendingToggle(val: boolean): void {
    this.attendingWithOthers = val;
    if (val && this.attendingRows.length === 0) {
      this.attendingRows = [this.createDefaultAttendingRow()];
    }
    this.cdr.markForCheck();
  }

  protected addAttendingOfficerRow(): void {
    this.attendingRows = [...this.attendingRows, this.createDefaultAttendingRow()];
    this.cdr.markForCheck();
  }

  protected removeAttendingOfficerRow(index: number): void {
    if (this.attendingRows.length <= 1) return;
    const row = this.attendingRows[index];
    const label = row?.customName ? `"${row.customName}"` : `Co-Attending Officer #${index + 1}`;
    this.confirm.ask({
      title: 'Remove Co-Attending Officer',
      message: `Are you sure you want to remove ${label} from this engagement?`,
      confirmLabel: 'Remove',
      onConfirm: () => {
        this.attendingRows =
          this.attendingRows.length > 1
            ? this.attendingRows.filter((_, idx) => idx !== index)
            : [this.createDefaultAttendingRow()];
        this.cdr.markForCheck();
      },
    });
  }

  protected onAttendingStaffChange(index: number, selectedId: string): void {
    const row = this.attendingRows[index];
    if (!row) return;
    if (selectedId === '__CUSTOM__') {
      row.selectionMode = 'CUSTOM';
      row.selectedStaffId = '__CUSTOM__';
      row.customName = '';
      row.designation = this.store.designations()[0] || 'Officer';
      row.department = this.store.departments()[0] || 'Department of Refugees';
      row.dutyStation = this.store.dutyStations()[0] || 'OPM Main';
    } else {
      const staff = this.store.staffMembers().find((s) => s.id === selectedId);
      if (staff) {
        row.selectionMode = 'LIST';
        row.selectedStaffId = staff.id;
        row.customName = `${staff.surname} ${staff.lastName}`;
        row.designation = staff.designation;
        row.department = staff.department;
        row.dutyStation = staff.dutyStation;
      }
    }
  }

  private createDefaultAttendingRow(): AttendingRow {
    const first = this.store.staffMembers()[0];
    if (first) {
      return {
        selectionMode: 'LIST',
        selectedStaffId: first.id,
        customName: `${first.surname} ${first.lastName}`,
        designation: first.designation,
        department: first.department,
        dutyStation: first.dutyStation,
      };
    }
    return {
      selectionMode: 'CUSTOM',
      selectedStaffId: '__CUSTOM__',
      customName: '',
      designation: this.store.designations()[0] || 'Officer',
      department: this.store.departments()[0] || 'Department of Refugees',
      dutyStation: this.store.dutyStations()[0] || 'OPM Main',
    };
  }

  protected saveForm(): void {
    if (!this.form.destination.trim() || !this.form.hostOrganization.trim()) return;

    const resolvedDesignation =
      this.designationOption === this.otherOption
        ? this.otherDesignation.trim() || 'Officer'
        : this.designationOption || this.store.designations()[0] || 'Officer';

    const resolvedDepartment =
      this.departmentOption === this.otherDepartmentOption
        ? this.otherDepartment.trim() || 'Department of Refugees'
        : this.departmentOption || this.store.departments()[0] || 'Department of Refugees';

    const resolvedStation =
      this.dutyStationOption === this.otherStationOption
        ? this.otherDutyStation.trim() || 'OPM Main'
        : this.dutyStationOption || this.store.dutyStations()[0] || 'OPM Main';

    if (this.designationOption === this.otherOption && this.otherDesignation.trim()) {
      this.store.addDesignation(this.otherDesignation.trim());
    }
    if (this.departmentOption === this.otherDepartmentOption && this.otherDepartment.trim()) {
      this.store.addDepartment(this.otherDepartment.trim());
    }
    if (this.dutyStationOption === this.otherStationOption && this.otherDutyStation.trim()) {
      this.store.addDutyStation(this.otherDutyStation.trim());
    }

    const cleanPurposes = this.purposeRows.map((p) => p.text.trim()).filter(Boolean);
    if (cleanPurposes.length === 0) {
      this.store.apiNotice.set({
        tone: 'error',
        text: 'Please enter at least 1 item in "Official Purpose & Deliverables".',
      });
      return;
    }
    const joinedPurpose = cleanPurposes.join('; ');

    const cleanAttending: AttendingOfficer[] = this.attendingWithOthers
      ? this.attendingRows
          .filter((r) => r.customName.trim().length > 0)
          .map((r) => ({
            staffMemberId: r.selectionMode === 'LIST' ? r.selectedStaffId : undefined,
            name: r.customName.trim(),
            designation: r.designation.trim() || 'Officer',
            department: r.department.trim() || resolvedDepartment,
            dutyStation: r.dutyStation.trim() || resolvedStation,
            isCustom: r.selectionMode === 'CUSTOM',
          }))
      : [];

    const currentSession = this.auth.session();
    this.submitting.set(true);

    this.store.saveOutOfOffice(
      {
        staffMemberId: this.selectedStaffMemberId || undefined,
        officerName: (this.form.officerName || currentSession?.fullName || 'Fredrick Kasoma').trim(),
        officerRole: resolvedDesignation,
        department: resolvedDepartment,
        dutyStation: resolvedStation,
        engagementType: this.form.engagementType,
        destination: this.form.destination.trim(),
        hostOrganization: this.form.hostOrganization.trim(),
        startDate: this.form.startDate,
        endDate: this.form.endDate,
        purpose: joinedPurpose,
        purposeList: cleanPurposes.length > 0 ? cleanPurposes : [joinedPurpose],
        attendingWithOthers: this.attendingWithOthers,
        attendingOfficers: cleanAttending,
        actingOfficer: this.form.actingOfficer || 'Assigned by Unit Supervisor',
        contactPhone: this.form.contactPhone,
        status: this.form.status,
      },
      this.editingId(),
      () => {
        this.submitting.set(false);
        void this.router.navigateByUrl('/out-of-office');
      },
      () => {
        this.submitting.set(false);
      }
    );
  }
}
