import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  ENGAGEMENT_TYPE_LABELS,
  OutOfOfficeNotification,
  OutOfOfficeReportBack,
  ReportBackAttachment,
  ReportBackManagementAction,
} from '../../../core/models/stafftrack.models';
import { AuthService } from '../../../core/services/auth.service';
import { ConfirmService } from '../../../core/services/confirm.service';
import {
  GeneralFileAttachmentApiDto,
  StaffTrackApiService,
  extractApiErrorMessage,
} from '../../../core/services/stafftrack-api.service';
import { StaffTrackStore } from '../../../core/services/stafftrack.store';
import { Icon } from '../../../shared/icon/icon';

@Component({
  selector: 'app-report-back',
  imports: [FormsModule, RouterLink, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .engagement-banner {
      background: var(--bg-subtle);
      border: 1px solid var(--border);
      border-inline-start: 4px solid var(--brand-500);
      border-radius: var(--radius-lg);
      padding: 1.1rem 1.3rem;
      display: grid;
      gap: 0.45rem;
    }
    .engagement-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 0.65rem;
      font-size: 0.84rem;
      color: var(--text-muted);
    }
    .no-col {
      width: 56px;
      font-weight: 700;
      text-align: center;
    }
    .action-col {
      width: 64px;
      text-align: end;
    }
    .upload-dropzone {
      position: relative;
      display: grid;
      place-items: center;
      gap: 0.5rem;
      padding: 1.5rem 1.25rem;
      border: 2px dashed var(--border-strong);
      border-radius: var(--radius-lg);
      background: var(--bg-subtle);
      text-align: center;
      cursor: pointer;
      transition: border-color 150ms, background-color 150ms;
    }
    .upload-dropzone:hover,
    .upload-dropzone.drag-active {
      border-color: var(--brand-500);
      background: var(--info-bg);
    }
    .upload-dropzone input[type='file'] {
      position: absolute;
      inset: 0;
      opacity: 0;
      cursor: pointer;
    }
    .upload-icon-circle {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      background: var(--bg-elevated);
      color: var(--brand-500);
      box-shadow: var(--shadow-sm);
    }
    .attachment-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
      gap: 0.9rem;
      margin-top: 1rem;
    }
    .attachment-card {
      border: 1px solid var(--border);
      border-radius: var(--radius);
      background: var(--bg-subtle);
      overflow: hidden;
      display: grid;
      grid-template-rows: 132px 1fr;
      min-width: 0;
    }
    .attachment-preview {
      position: relative;
      background: var(--bg-elevated);
      border-bottom: 1px solid var(--border);
      display: grid;
      place-items: center;
      overflow: hidden;
      min-width: 0;
    }
    .attachment-remove-badge {
      position: absolute;
      top: 0.45rem;
      inset-inline-end: 0.45rem;
      z-index: 2;
      width: 28px;
      height: 28px;
      border-radius: 50%;
      border: 1px solid var(--border);
      background: rgba(255, 255, 255, 0.92);
      color: var(--danger);
      display: inline-grid;
      place-items: center;
      cursor: pointer;
      box-shadow: var(--shadow-sm);
      transition: transform 120ms, background-color 120ms;
    }
    .attachment-remove-badge:hover {
      background: var(--danger-bg);
      transform: scale(1.06);
    }
    .attachment-preview img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .pdf-badge-preview {
      display: grid;
      place-items: center;
      gap: 0.35rem;
      color: var(--danger);
      font-weight: 800;
      font-size: 0.85rem;
    }
    .attachment-body {
      padding: 0.75rem 0.85rem;
      display: grid;
      gap: 0.5rem;
      align-content: space-between;
      min-width: 0;
    }
    .attachment-name {
      font-size: 0.82rem;
      font-weight: 600;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      min-width: 0;
    }
    .attachment-meta {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.4rem;
      font-size: 0.74rem;
      color: var(--text-muted);
      min-width: 0;
    }
    .attachment-actions {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.45rem;
      margin-top: 0.2rem;
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
  `,
  template: `
    <div class="page">
      <header class="page-header">
        <div class="page-title">
          <div style="display: flex; align-items: center; gap: 0.6rem; flex-wrap: wrap">
            <a routerLink="/out-of-office" class="btn btn-sm">
              <span>← Back to Out-of-Office</span>
            </a>
            <span class="badge badge-info">{{ notice()?.referenceNumber || notice()?.id }}</span>
          </div>
          <h1 style="margin-top: 0.35rem">Report Back on Out-of-Office Engagement</h1>
          <p>
            Record activities undertaken, discussions, achievements, attach field photos or PDF
            reports, and assign management follow-up actions to responsible officers.
          </p>
        </div>
      </header>

      @if (store.apiNotice(); as apiMsg) {
        <div
          role="status"
          style="display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.8rem 1.1rem; border-radius: var(--radius); font-size: 0.88rem; font-weight: 600"
          [style.background]="apiMsg.tone === 'success' ? 'var(--success-bg)' : 'var(--danger-bg)'"
          [style.color]="apiMsg.tone === 'success' ? 'var(--success)' : 'var(--danger)'"
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

      @if (notice(); as item) {
        <!-- Engagement Context Banner -->
        <section class="engagement-banner" aria-label="Out-of-office engagement summary">
          <div style="display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap">
            <span class="badge badge-info">{{ engagementLabels[item.engagementType] }}</span>
            <strong style="font-size: 1.02rem">{{ item.destination }}</strong>
          </div>
          <div class="engagement-meta">
            <span><strong>Host / Organizer:</strong> {{ item.hostOrganization }}</span>
            <span>·</span>
            <span><strong>Duty Station:</strong> {{ item.dutyStation }}</span>
            <span>·</span>
            <span><strong>Scheduled Dates:</strong> {{ item.startDate }} → {{ item.endDate }}</span>
          </div>
          <p style="font-size: 0.86rem; color: var(--text-muted)">
            <strong>Original Purpose:</strong> {{ item.purpose }}
          </p>
        </section>

        <form (ngSubmit)="submitReportBack()" style="display: grid; gap: 1.35rem">
          <!-- Report Back Details Card -->
          <section class="card">
            <div class="card-header">
              <div>
                <h2>Engagement Return &amp; Stakeholders</h2>
                <p>Confirm actual return date and stakeholders or participants engaged</p>
              </div>
            </div>

            <div class="card-body">
              <div class="form-grid">
                <div class="field">
                  <label for="rb-return-date">Actual Return Date *</label>
                  <input
                    id="rb-return-date"
                    type="date"
                    name="actualReturnDate"
                    class="input"
                    required
                    [(ngModel)]="form.actualReturnDate"
                  />
                </div>

                <div class="field">
                  <label for="rb-stakeholders">Stakeholders / Participants Met *</label>
                  <input
                    id="rb-stakeholders"
                    name="stakeholdersMet"
                    class="input"
                    required
                    [(ngModel)]="form.stakeholdersMet"
                    placeholder="e.g. Settlement Commandant, District Leaders, Partner Agencies"
                  />
                </div>
              </div>
            </div>
          </section>

          <!-- Key Issues Raised / Matters Discussed Card -->
          <section class="card">
            <div class="card-header">
              <div>
                <h2>Key issues raised / matters discussed *</h2>
                <p>Provide at least 1 key issue raised or matter discussed during the engagement</p>
              </div>
              <button type="button" class="btn btn-sm btn-primary" (click)="addKeyIssueRow()">
                <app-icon name="plus" />
                <span>Add Issue / Matter</span>
              </button>
            </div>

            <div class="numbered-list" role="region" aria-label="Key issues raised and matters discussed">
              @for (issue of keyIssuesRows; track $index) {
                <div class="numbered-row">
                  <span class="row-num-badge" aria-hidden="true">{{ $index + 1 }}</span>
                  <input
                    type="text"
                    [name]="'keyIssue_' + $index"
                    class="input"
                    required
                    [attr.aria-label]="'Key issue or matter discussed ' + ($index + 1)"
                    [(ngModel)]="issue.text"
                    placeholder="Enter key issue raised or matter discussed..."
                  />
                  @if (keyIssuesRows.length > 1) {
                    <button
                      type="button"
                      class="btn btn-sm btn-danger row-icon-btn"
                      title="Remove issue row"
                      aria-label="Remove issue row"
                      (click)="removeKeyIssueRow($index)"
                    >
                      <app-icon name="trash" />
                    </button>
                  }
                </div>
              }
            </div>
          </section>

          <!-- What Was Achieved (Outcomes & Deliverables) Card -->
          <section class="card">
            <div class="card-header">
              <div>
                <h2>What Was Achieved (Outcomes &amp; Deliverables) *</h2>
                <p>Provide at least 1 outcome, milestone, or deliverable achieved</p>
              </div>
              <button type="button" class="btn btn-sm btn-primary" (click)="addAchievementRow()">
                <app-icon name="plus" />
                <span>Add Outcome / Deliverable</span>
              </button>
            </div>

            <div class="numbered-list" role="region" aria-label="Outcomes and deliverables achieved">
              @for (ach of achievementRows; track $index) {
                <div class="numbered-row">
                  <span class="row-num-badge" aria-hidden="true">{{ $index + 1 }}</span>
                  <input
                    type="text"
                    [name]="'achievement_' + $index"
                    class="input"
                    required
                    [attr.aria-label]="'Outcome or deliverable achieved ' + ($index + 1)"
                    [(ngModel)]="ach.text"
                    placeholder="Enter measurable outcome or deliverable achieved..."
                  />
                  @if (achievementRows.length > 1) {
                    <button
                      type="button"
                      class="btn btn-sm btn-danger row-icon-btn"
                      title="Remove outcome row"
                      aria-label="Remove outcome row"
                      (click)="removeAchievementRow($index)"
                    >
                      <app-icon name="trash" />
                    </button>
                  }
                </div>
              }
            </div>
          </section>

          <!-- Management Action Card -->
          <section class="card">
            <div class="card-header">
              <div>
                <h2>Management Action *</h2>
                <p>
                  Provide at least 1 follow-up action required, select the responsible officer from
                  registered staff, and set target completion dates.
                </p>
              </div>
              <button type="button" class="btn btn-sm btn-primary" (click)="addActionRow()">
                <app-icon name="plus" />
                <span>Add Action Row</span>
              </button>
            </div>

            <div class="repeat-stack" role="region" aria-label="Management Action follow-up items">
              @for (row of form.managementActions; track $index) {
                <div class="repeat-card">
                  <div class="repeat-card-header">
                    <div class="repeat-card-title">
                      <span class="row-num-badge">{{ $index + 1 }}</span>
                      <span>Management Follow-Up Action {{ $index + 1 }}</span>
                    </div>
                    @if (form.managementActions.length > 1) {
                      <button
                        type="button"
                        class="btn btn-sm btn-danger row-icon-btn"
                        title="Remove management action row"
                        aria-label="Remove management action row"
                        (click)="removeActionRow($index)"
                      >
                        <app-icon name="trash" />
                      </button>
                    }
                  </div>

                  <div class="form-grid">
                    <div class="field field-full">
                      <label [for]="'mgmt-action-' + $index">Follow Up Action *</label>
                      <input
                        [id]="'mgmt-action-' + $index"
                        type="text"
                        [name]="'followUpAction_' + $index"
                        class="input"
                        required
                        [(ngModel)]="row.followUpAction"
                        placeholder="Describe follow-up action required..."
                      />
                    </div>

                    <div class="field">
                      <label [for]="'mgmt-officer-' + $index">Responsible Officer *</label>
                      <select
                        [id]="'mgmt-officer-' + $index"
                        [name]="'responsibleOfficer_' + $index"
                        class="select"
                        required
                        [(ngModel)]="row.responsibleOfficer"
                      >
                        @for (staffOpt of staffOptions(); track staffOpt.label) {
                          <option [value]="staffOpt.value">{{ staffOpt.label }}</option>
                        }
                      </select>
                    </div>

                    <div class="field">
                      <label [for]="'mgmt-date-' + $index">Target Date *</label>
                      <input
                        [id]="'mgmt-date-' + $index"
                        type="date"
                        [name]="'targetDate_' + $index"
                        class="input"
                        required
                        [(ngModel)]="row.targetDate"
                      />
                    </div>
                  </div>
                </div>
              }
            </div>
          </section>

          <!-- Field Photos or PDF report Card (Last Section) -->
          <section class="card">
            <div class="card-header">
              <div>
                <h2>Field Photos or PDF report</h2>
                <p>
                  Upload supporting field photos (JPG, PNG, WEBP), signed attendance sheets, or PDF
                  reports.
                </p>
              </div>
              <div style="display: flex; align-items: center; gap: 0.5rem">
                @if (uploadingFiles()) {
                  <span class="badge badge-warning">Uploading to Files API...</span>
                }
                <span class="badge badge-info">
                  {{ attachments().length }} File(s) Attached
                </span>
              </div>
            </div>

            <div class="card-body">
              <label
                class="upload-dropzone"
                [class.drag-active]="dragActive()"
                (dragover)="onDragOver($event)"
                (dragleave)="dragActive.set(false)"
                (drop)="onDrop($event)"
              >
                <input
                  type="file"
                  multiple
                  accept="image/*,application/pdf,.pdf,.doc,.docx,.xls,.xlsx"
                  aria-label="Upload photos, PDFs, or supporting documents"
                  [disabled]="uploadingFiles()"
                  (change)="onFilesSelected($event)"
                />
                <span class="upload-icon-circle">
                  <app-icon name="plus" />
                </span>
                <div>
                  <strong style="font-size: 0.92rem">
                    {{
                      uploadingFiles()
                        ? 'Uploading files to Files API...'
                        : 'Click to select files or drag & drop here'
                    }}
                  </strong>
                  <p style="font-size: 0.79rem; color: var(--text-muted); margin-top: 0.15rem">
                    Supports Photos (PNG, JPG, WEBP), PDF documents (.pdf), and Office files
                  </p>
                </div>
              </label>

              @if (uploadError()) {
                <p style="margin-top: 0.65rem; font-size: 0.82rem; color: var(--danger); font-weight: 600">
                  {{ uploadError() }}
                </p>
              }

              @if (attachments().length > 0) {
                <div class="attachment-grid">
                  @for (att of attachments(); track att.id) {
                    <article class="attachment-card">
                      <div class="attachment-preview">
                        <button
                          type="button"
                          class="attachment-remove-badge"
                          title="Remove attachment"
                          aria-label="Remove attachment"
                          (click)="removeAttachment(att.id)"
                        >
                          <app-icon name="close" />
                        </button>
                        @if (att.kind === 'IMAGE') {
                          <img [src]="att.previewUrl || att.streamUrl || att.dataUrl" [alt]="att.name" />
                        } @else if (att.kind === 'PDF') {
                          <div class="pdf-badge-preview">
                            <app-icon name="fileText" />
                            <span>PDF DOCUMENT</span>
                          </div>
                        } @else {
                          <div class="pdf-badge-preview" style="color: var(--brand-500)">
                            <app-icon name="fileText" />
                            <span>ATTACHMENT</span>
                          </div>
                        }
                      </div>

                      <div class="attachment-body">
                        <div style="min-width: 0">
                          <div class="attachment-name" [title]="att.name">{{ att.name }}</div>
                          <div class="attachment-meta">
                            <span>{{ att.kind }}</span>
                            <span>{{ formatBytes(att.sizeBytes) }}</span>
                          </div>
                        </div>

                        <div class="attachment-actions">
                          <a
                            [href]="att.downloadUrl || att.dataUrl"
                            [download]="att.name"
                            target="_blank"
                            rel="noopener"
                            class="btn btn-sm"
                            title="Download attachment"
                          >
                            <app-icon name="download" />
                            <span>Download</span>
                          </a>
                          <button
                            type="button"
                            class="btn btn-sm btn-danger"
                            title="Remove attachment"
                            aria-label="Remove attachment"
                            (click)="removeAttachment(att.id)"
                          >
                            <app-icon name="trash" />
                            <span>Remove</span>
                          </button>
                        </div>
                      </div>
                    </article>
                  }
                </div>
              }
            </div>

            <div class="form-footer">
              <a routerLink="/out-of-office" class="btn">Cancel</a>
              <button
                type="submit"
                class="btn btn-primary"
                [disabled]="submitting() || uploadingFiles()"
              >
                <app-icon name="check" />
                <span>
                  {{
                    submitting()
                      ? 'Saving to API...'
                      : uploadingFiles()
                      ? 'Uploading Files...'
                      : 'Save Report Back & Management Actions'
                  }}
                </span>
              </button>
            </div>
          </section>
        </form>
      } @else {
        <section class="card" style="padding: 2.5rem; text-align: center">
          <p style="color: var(--text-muted); margin-bottom: 1rem">
            Out-of-Office Notification not found.
          </p>
          <a routerLink="/out-of-office" class="btn btn-primary">Return to Out-of-Office</a>
        </section>
      }
    </div>
  `,
})
export class ReportBack implements OnInit {
  protected readonly store = inject(StaffTrackStore);
  private readonly api = inject(StaffTrackApiService);
  private readonly auth = inject(AuthService);
  private readonly confirm = inject(ConfirmService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  protected readonly engagementLabels = ENGAGEMENT_TYPE_LABELS;
  protected readonly noticeId = signal('');
  protected readonly dragActive = signal(false);
  protected readonly uploadError = signal('');
  protected readonly uploadingFiles = signal(false);
  protected readonly submitting = signal(false);
  protected readonly attachments = signal<ReportBackAttachment[]>([]);

  protected readonly notice = computed<OutOfOfficeNotification | undefined>(() =>
    this.store.outOfOfficeList().find((i) => i.id === this.noticeId())
  );

  protected readonly staffOptions = computed(() =>
    this.store.staffMembers().map((s) => ({
      id: s.id,
      value: `${s.surname} ${s.lastName}`,
      label: `${s.surname} ${s.lastName} (${s.designation} · ${s.dutyStation})`,
    }))
  );

  protected keyIssuesRows: Array<{ text: string }> = [{ text: '' }];
  protected achievementRows: Array<{ text: string }> = [{ text: '' }];

  protected form: OutOfOfficeReportBack = {
    actualReturnDate: new Date().toISOString().slice(0, 10),
    stakeholdersMet: '',
    whatWasDone: '',
    whatWasDiscussed: '',
    whatWasAchieved: '',
    outstandingIssues: '',
    keyIssuesList: [],
    achievementsList: [],
    managementActions: [
      {
        followUpAction: '',
        responsibleOfficer: '',
        targetDate: new Date().toISOString().slice(0, 10),
      },
    ],
    attachments: [],
    submittedAt: new Date().toISOString().slice(0, 10),
  };

  constructor() {
    effect(() => {
      const opts = this.staffOptions();
      if (opts.length > 0 && this.form.managementActions.length > 0) {
        let updated = false;
        for (const action of this.form.managementActions) {
          if (!action.responsibleOfficer) {
            action.responsibleOfficer = opts[0].value;
            updated = true;
          }
        }
        if (updated) {
          this.cdr.markForCheck();
        }
      }
    });
  }

  ngOnInit(): void {
    this.store.clearApiNotice();
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.noticeId.set(id);
    if (id) {
      this.store.loadOutOfOfficeById(id, (target) => {
        this.populateFromTarget(target);
        this.loadFilesFromFilesApi(id);
      });
    }
  }

  private resolveOrganisationCode(): string {
    return (
      this.auth.session()?.organisationCode ||
      this.store.organisations()[0]?.code ||
      'OPM'
    );
  }

  private mapFilesApiDtoToAttachment(dto: GeneralFileAttachmentApiDto): ReportBackAttachment {
    const mime = dto.mimeType || 'application/octet-stream';
    const ext = (dto.fileType || '').toLowerCase();
    const lowerName = (dto.originalFilename || dto.filename || '').toLowerCase();
    const kind: ReportBackAttachment['kind'] =
      mime.startsWith('image/') || ['jpg', 'jpeg', 'png', 'webp', 'gif', 'svg'].includes(ext)
        ? 'IMAGE'
        : mime === 'application/pdf' || ext === 'pdf' || lowerName.endsWith('.pdf')
        ? 'PDF'
        : 'DOCUMENT';

    const downloadUrl = this.api.getFileDownloadUrl(dto.fileUuid);
    const streamUrl = this.api.getFileStreamUrl(dto.fileUuid);
    const previewUrl =
      kind === 'IMAGE'
        ? this.api.getFilePreviewUrl(dto.fileUuid)
        : streamUrl;

    return {
      id: dto.fileUuid,
      fileUuid: dto.fileUuid,
      name: dto.originalFilename || dto.filename,
      mimeType: mime,
      sizeBytes: Number(dto.fileSize || 0),
      kind,
      dataUrl: downloadUrl,
      downloadUrl,
      streamUrl,
      previewUrl,
      uploadedAt: dto.date ? dto.date.slice(0, 10) : new Date().toISOString().slice(0, 10),
    };
  }

  private enrichExistingAttachment(att: ReportBackAttachment): ReportBackAttachment {
    const uuidCandidate =
      att.fileUuid ||
      this.extractUuidFromUrl(att.dataUrl) ||
      (this.isUuid(att.id) ? att.id : undefined);
    if (!uuidCandidate) return att;

    const downloadUrl = this.api.getFileDownloadUrl(uuidCandidate);
    const streamUrl = this.api.getFileStreamUrl(uuidCandidate);
    const previewUrl =
      att.kind === 'IMAGE' ? this.api.getFilePreviewUrl(uuidCandidate) : streamUrl;

    return {
      ...att,
      fileUuid: uuidCandidate,
      dataUrl: att.dataUrl?.startsWith('data:') ? att.dataUrl : downloadUrl,
      downloadUrl,
      streamUrl,
      previewUrl: att.dataUrl?.startsWith('data:') ? att.dataUrl : previewUrl,
    };
  }

  private extractUuidFromUrl(url?: string): string | undefined {
    if (!url || url.startsWith('data:')) return undefined;
    const match = url.match(
      /\/(?:download|stream|preview|thumbnail)\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/i
    );
    return match?.[1];
  }

  private isUuid(val?: string): boolean {
    if (!val) return false;
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(val.trim());
  }

  private loadFilesFromFilesApi(entityRefId: string): void {
    this.api.getFilesByEntityRefId(entityRefId).subscribe({
      next: (resp) => {
        const list = resp?.data || [];
        if (list.length === 0) return;

        const fromFilesApi = list.map((dto) => this.mapFilesApiDtoToAttachment(dto));
        this.attachments.update((current) => {
          const merged = [...current.map((a) => this.enrichExistingAttachment(a))];
          for (const apiFile of fromFilesApi) {
            const existingIdx = merged.findIndex(
              (c) =>
                c.fileUuid === apiFile.fileUuid ||
                c.id === apiFile.fileUuid ||
                c.name.toLowerCase() === apiFile.name.toLowerCase()
            );
            if (existingIdx >= 0) {
              merged[existingIdx] = {
                ...merged[existingIdx],
                ...apiFile,
                id: merged[existingIdx].id || apiFile.id,
              };
            } else {
              merged.push(apiFile);
            }
          }
          return merged;
        });
        this.cdr.markForCheck();
      },
      error: () => {
        // Silently keep existing report-back attachments if filesapi is temporarily unreachable
      },
    });
  }

  private populateFromTarget(target: OutOfOfficeNotification): void {
    const defaultStaff = this.staffOptions()[0]?.value || target.officerName || '';
    const defaultDate = target.endDate || new Date().toISOString().slice(0, 10);

    if (target.reportBack) {
      this.attachments.set(
        (target.reportBack.attachments || []).map((a) => this.enrichExistingAttachment(a))
      );
      const rawIssues =
        target.reportBack.keyIssuesList && target.reportBack.keyIssuesList.length > 0
          ? target.reportBack.keyIssuesList
          : target.reportBack.outstandingIssues
          ? target.reportBack.outstandingIssues.split(/\s*;\s*|\n+/).filter(Boolean)
          : [];
      const rawAchievements =
        target.reportBack.achievementsList && target.reportBack.achievementsList.length > 0
          ? target.reportBack.achievementsList
          : target.reportBack.whatWasAchieved
          ? target.reportBack.whatWasAchieved.split(/\s*;\s*|\n+/).filter(Boolean)
          : [];

      const savedIssues = rawIssues.length > 0 ? rawIssues : [''];
      const savedAchievements = rawAchievements.length > 0 ? rawAchievements : [''];

      this.keyIssuesRows = savedIssues.map((text) => ({ text }));
      this.achievementRows = savedAchievements.map((text) => ({ text }));

      this.form = {
        ...target.reportBack,
        attachments: [...(target.reportBack.attachments || [])],
        managementActions:
          target.reportBack.managementActions && target.reportBack.managementActions.length > 0
            ? target.reportBack.managementActions.map((r) => ({
                ...r,
                responsibleOfficer: r.responsibleOfficer || defaultStaff,
                targetDate: r.targetDate || defaultDate,
              }))
            : [
                {
                  followUpAction: '',
                  responsibleOfficer: defaultStaff,
                  targetDate: defaultDate,
                },
              ],
      };
    } else {
      this.attachments.set([]);
      this.keyIssuesRows = [{ text: '' }];
      this.achievementRows = [{ text: '' }];
      this.form = {
        actualReturnDate: defaultDate,
        stakeholdersMet: target.hostOrganization || '',
        whatWasDone: '',
        whatWasDiscussed: '',
        whatWasAchieved: '',
        outstandingIssues: '',
        keyIssuesList: [],
        achievementsList: [],
        attachments: [],
        managementActions: [
          {
            followUpAction: target.managementActionRequired || '',
            responsibleOfficer: defaultStaff,
            targetDate: defaultDate,
          },
        ],
        submittedAt: new Date().toISOString().slice(0, 10),
      };
    }
    this.cdr.markForCheck();
  }

  protected addKeyIssueRow(): void {
    this.keyIssuesRows = [...this.keyIssuesRows, { text: '' }];
    this.cdr.markForCheck();
  }

  protected removeKeyIssueRow(index: number): void {
    if (this.keyIssuesRows.length <= 1) return;
    this.confirm.ask({
      title: 'Remove Key Issue / Matter',
      message: `Are you sure you want to remove Key Issue / Matter row #${index + 1}?`,
      confirmLabel: 'Remove',
      onConfirm: () => {
        this.keyIssuesRows =
          this.keyIssuesRows.length > 1
            ? this.keyIssuesRows.filter((_, idx) => idx !== index)
            : [{ text: '' }];
        this.cdr.markForCheck();
      },
    });
  }

  protected addAchievementRow(): void {
    this.achievementRows = [...this.achievementRows, { text: '' }];
    this.cdr.markForCheck();
  }

  protected removeAchievementRow(index: number): void {
    if (this.achievementRows.length <= 1) return;
    this.confirm.ask({
      title: 'Remove Outcome / Deliverable',
      message: `Are you sure you want to remove Outcome / Deliverable row #${index + 1}?`,
      confirmLabel: 'Remove',
      onConfirm: () => {
        this.achievementRows =
          this.achievementRows.length > 1
            ? this.achievementRows.filter((_, idx) => idx !== index)
            : [{ text: '' }];
        this.cdr.markForCheck();
      },
    });
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragActive.set(true);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragActive.set(false);
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.processFiles(Array.from(files));
    }
  }

  protected onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.processFiles(Array.from(input.files));
      input.value = '';
    }
  }

  private processFiles(files: File[]): void {
    this.uploadError.set('');
    const maxBytes = 50 * 1024 * 1024; // 50 MB per file (supported by filesapi)
    const entityRefId = this.noticeId() || this.notice()?.id || 'general';
    const organisationId = this.resolveOrganisationCode();

    const validFiles: File[] = [];
    for (const file of files) {
      if (file.size > maxBytes) {
        this.uploadError.set(`"${file.name}" exceeds the 50 MB per-file size limit.`);
        continue;
      }
      validFiles.push(file);
    }

    if (validFiles.length === 0) return;

    this.uploadingFiles.set(true);
    let pendingCount = validFiles.length;

    const markOneComplete = () => {
      pendingCount -= 1;
      if (pendingCount <= 0) {
        this.uploadingFiles.set(false);
        this.cdr.markForCheck();
      }
    };

    for (const file of validFiles) {
      const mime = file.type || 'application/octet-stream';
      const lowerName = file.name.toLowerCase();
      const kind: ReportBackAttachment['kind'] = mime.startsWith('image/')
        ? 'IMAGE'
        : mime === 'application/pdf' || lowerName.endsWith('.pdf')
        ? 'PDF'
        : 'DOCUMENT';

      this.api
        .uploadGeneralAttachment({
          file,
          organisationId,
          entityRefId,
          category: 'OUT_OF_OFFICE',
          subcategory: 'REPORT_BACK',
          subEntityRefId: this.notice()?.referenceNumber || entityRefId,
          attachmentOrigin: 'STAFFTRACK_REPORT_BACK',
        })
        .subscribe({
          next: (resp) => {
            const fileUuid = resp?.data;
            if (!fileUuid || !resp?.success) {
              this.uploadError.set(resp?.message || `Failed to upload "${file.name}" to Files API.`);
              markOneComplete();
              return;
            }

            const downloadUrl = this.api.getFileDownloadUrl(fileUuid);
            const streamUrl = this.api.getFileStreamUrl(fileUuid);
            const previewUrl =
              kind === 'IMAGE' ? this.api.getFilePreviewUrl(fileUuid) : streamUrl;

            const att: ReportBackAttachment = {
              id: fileUuid,
              fileUuid,
              name: file.name,
              mimeType: mime,
              sizeBytes: file.size,
              kind,
              dataUrl: downloadUrl,
              downloadUrl,
              streamUrl,
              previewUrl,
              uploadedAt: new Date().toISOString().slice(0, 10),
            };

            this.attachments.update((list) => {
              const filtered = list.filter(
                (existing) =>
                  existing.fileUuid !== fileUuid &&
                  existing.name.toLowerCase() !== file.name.toLowerCase()
              );
              return [...filtered, att];
            });

            this.store.apiNotice.set({
              tone: 'success',
              text: `Uploaded "${file.name}" to Files API.`,
            });
            markOneComplete();
          },
          error: (err) => {
            this.uploadError.set(
              `Files API upload failed for "${file.name}": ${extractApiErrorMessage(err)}`
            );
            markOneComplete();
          },
        });
    }
  }

  protected removeAttachment(id: string): void {
    const target = this.attachments().find((a) => a.id === id);
    if (!target) return;

    this.confirm.ask({
      title: 'Remove Attachment',
      message: `Are you sure you want to remove "${target.name}"?`,
      confirmLabel: 'Remove',
      onConfirm: () => {
        const fileUuid =
          target.fileUuid ||
          this.extractUuidFromUrl(target.dataUrl) ||
          (this.isUuid(target.id) ? target.id : undefined);

        const removeLocally = () => {
          this.attachments.update((list) => list.filter((a) => a.id !== id));
          this.cdr.markForCheck();
        };

        if (fileUuid) {
          this.api.deleteGeneralAttachment(fileUuid).subscribe({
            next: () => {
              removeLocally();
              this.store.apiNotice.set({
                tone: 'success',
                text: `Deleted "${target.name}" from Files API.`,
              });
            },
            error: () => {
              removeLocally();
            },
          });
        } else {
          removeLocally();
        }
      },
    });
  }

  protected formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  }

  protected addActionRow(): void {
    const defaultStaff = this.staffOptions()[0]?.value || this.notice()?.officerName || '';
    const defaultDate = this.form.actualReturnDate || new Date().toISOString().slice(0, 10);
    this.form.managementActions = [
      ...this.form.managementActions,
      {
        followUpAction: '',
        responsibleOfficer: defaultStaff,
        targetDate: defaultDate,
      },
    ];
    this.cdr.markForCheck();
  }

  protected removeActionRow(index: number): void {
    if (this.form.managementActions.length <= 1) return;
    this.confirm.ask({
      title: 'Remove Management Action',
      message: `Are you sure you want to remove Management Follow-Up Action #${index + 1}?`,
      confirmLabel: 'Remove',
      onConfirm: () => {
        const defaultStaff = this.staffOptions()[0]?.value || this.notice()?.officerName || '';
        const defaultDate = this.form.actualReturnDate || new Date().toISOString().slice(0, 10);
        this.form.managementActions =
          this.form.managementActions.length > 1
            ? this.form.managementActions.filter((_, idx) => idx !== index)
            : [
                {
                  followUpAction: '',
                  responsibleOfficer: defaultStaff,
                  targetDate: defaultDate,
                },
              ];
        this.cdr.markForCheck();
      },
    });
  }

  protected submitReportBack(): void {
    const target = this.notice();
    if (!target) return;

    const cleanIssues = this.keyIssuesRows.map((r) => r.text.trim()).filter(Boolean);
    const cleanAchievements = this.achievementRows.map((r) => r.text.trim()).filter(Boolean);

    const cleanActions: ReportBackManagementAction[] = this.form.managementActions
      .map((row) => {
        const matchedStaff = this.staffOptions().find((s) => s.value === row.responsibleOfficer);
        return {
          id: row.id,
          followUpAction: row.followUpAction.trim(),
          responsibleStaffId: matchedStaff?.id,
          responsibleOfficer: row.responsibleOfficer || target.officerName,
          targetDate: row.targetDate,
        };
      })
      .filter((row) => row.followUpAction.length > 0);

    if (cleanIssues.length === 0) {
      this.store.apiNotice.set({
        tone: 'error',
        text: 'Please enter at least 1 item in "Key issues raised / matters discussed".',
      });
      return;
    }

    if (cleanAchievements.length === 0) {
      this.store.apiNotice.set({
        tone: 'error',
        text: 'Please enter at least 1 item in "What Was Achieved (Outcomes & Deliverables)".',
      });
      return;
    }

    if (cleanActions.length === 0) {
      this.store.apiNotice.set({
        tone: 'error',
        text: 'Please enter at least 1 item in "Management Action".',
      });
      return;
    }

    const issuesJoined = cleanIssues.join('; ');
    const achievementsJoined = cleanAchievements.join('; ');

    this.submitting.set(true);
    this.store.submitOutOfOfficeReportBack(
      target.id,
      {
        ...this.form,
        whatWasDone: achievementsJoined || target.purpose,
        whatWasDiscussed: issuesJoined,
        whatWasAchieved: achievementsJoined || 'Completed as scheduled',
        outstandingIssues: issuesJoined,
        keyIssuesList: cleanIssues,
        achievementsList: cleanAchievements,
        managementActions: cleanActions,
        attachments: this.attachments(),
        submittedAt: new Date().toISOString().slice(0, 10),
      },
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
