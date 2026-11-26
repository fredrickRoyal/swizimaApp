import { Location } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  ENGAGEMENT_TYPE_LABELS,
  OOO_STATUS_LABELS,
  OutOfOfficeNotification,
  ReportBackAttachment,
} from '../../../core/models/stafftrack.models';
import {
  GeneralFileAttachmentApiDto,
  StaffTrackApiService,
} from '../../../core/services/stafftrack-api.service';
import { AuthService } from '../../../core/services/auth.service';
import { StaffTrackStore } from '../../../core/services/stafftrack.store';
import { Icon } from '../../../shared/icon/icon';

interface QrCell {
  x: number;
  y: number;
}

interface BarcodeBar {
  x: number;
  w: number;
}

@Component({
  selector: 'app-view-report',
  imports: [RouterLink, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .report-toolbar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
    }
    .pdf-sheet {
      background: var(--bg-elevated);
      color: var(--text);
      border: 1px solid var(--border-strong);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow);
      padding: clamp(1.25rem, 3vw, 2.25rem);
      max-width: 1020px;
      margin-inline: auto;
      display: grid;
      gap: 1.35rem;
    }
    .ug-flag-ribbon {
      height: 6px;
      border-radius: 4px;
      background: linear-gradient(
        90deg,
        #111827 0%,
        #111827 33.3%,
        #facc15 33.3%,
        #facc15 66.6%,
        #dc2626 66.6%,
        #dc2626 100%
      );
    }
    .doc-banner {
      display: grid;
      grid-template-columns: 1fr;
      gap: 1rem;
      padding-bottom: 1.1rem;
      border-bottom: 2px solid #005eb8;
      align-items: center;
    }
    @media (min-width: 760px) {
      .doc-banner {
        grid-template-columns: 1fr auto;
      }
    }
    .doc-brand {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .opm-emblem {
      width: 74px;
      height: 74px;
      flex: none;
    }
    .doc-title-block {
      display: grid;
      gap: 0.15rem;
    }
    .doc-govt {
      font-size: 0.73rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: var(--text-muted);
    }
    .doc-dept {
      font-size: 0.88rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: #005eb8;
    }
    .doc-title-block h1 {
      font-size: clamp(1.08rem, 2vw, 1.32rem);
      font-weight: 800;
      letter-spacing: -0.015em;
      line-height: 1.2;
    }
    .doc-title-block p {
      font-size: 0.78rem;
      color: var(--text-muted);
    }

    .verification-cluster {
      display: flex;
      align-items: center;
      gap: 0.85rem;
      padding: 0.55rem 0.75rem;
      border: 1px solid var(--border-strong);
      border-radius: var(--radius);
      background: var(--bg-subtle);
      justify-self: start;
    }
    @media (min-width: 760px) {
      .verification-cluster {
        justify-self: end;
      }
    }
    .qr-box {
      width: 72px;
      height: 72px;
      padding: 4px;
      background: #ffffff;
      border: 1px solid #cbd5e1;
      border-radius: 6px;
      flex: none;
    }
    .qr-box svg {
      width: 100%;
      height: 100%;
      display: block;
    }
    .code-stack {
      display: grid;
      gap: 0.25rem;
      font-size: 0.75rem;
    }
    .barcode-box {
      background: #ffffff;
      border: 1px solid #cbd5e1;
      border-radius: 4px;
      padding: 3px 6px 2px;
      display: grid;
      justify-items: center;
    }
    .barcode-box svg {
      width: 148px;
      height: 26px;
      display: block;
    }
    .barcode-box span {
      font-family: var(--font-mono);
      font-size: 0.63rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      color: #0f172a;
    }
    .meta-lines {
      display: grid;
      gap: 0.1rem;
      color: var(--text-muted);
    }

    .section-heading {
      background: #005eb8;
      color: #ffffff;
      padding: 0.5rem 0.85rem;
      font-size: 0.84rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      border-radius: 6px 6px 0 0;
    }
    .section-sub-heading {
      background: var(--bg-subtle);
      color: var(--text);
      padding: 0.45rem 0.8rem;
      font-size: 0.78rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      border: 1px solid var(--border-strong);
      border-bottom: 0;
      margin-top: 0.75rem;
    }

    .pdf-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.86rem;
    }
    .pdf-table th,
    .pdf-table td {
      border: 1px solid var(--border-strong);
      padding: 0.6rem 0.8rem;
      vertical-align: top;
      text-align: start;
    }
    .pdf-table th {
      background: var(--bg-subtle);
      font-weight: 700;
      color: var(--text);
      width: 22%;
    }
    .pdf-table td {
      width: 28%;
    }
    .pdf-list-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.86rem;
    }
    .pdf-list-table th,
    .pdf-list-table td {
      border: 1px solid var(--border-strong);
      padding: 0.55rem 0.8rem;
      vertical-align: top;
      text-align: start;
    }
    .pdf-list-table thead th {
      background: var(--bg-subtle);
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }
    .col-no {
      width: 52px;
      text-align: center !important;
      font-weight: 700;
    }

    .photo-strip {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      gap: 0.75rem;
      margin-top: 0.6rem;
    }
    .photo-box {
      border: 1px solid var(--border-strong);
      border-radius: 6px;
      overflow: hidden;
      background: var(--bg-subtle);
    }
    .photo-box img {
      width: 100%;
      height: 120px;
      object-fit: cover;
      display: block;
    }
    .photo-box small {
      display: block;
      padding: 0.35rem 0.5rem;
      font-size: 0.74rem;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .doc-footer-strip {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding-top: 0.85rem;
      border-top: 1px dashed var(--border-strong);
      font-size: 0.74rem;
      color: var(--text-muted);
    }

    @media print {
      .no-print {
        display: none !important;
      }
      .pdf-sheet {
        border: 0;
        box-shadow: none;
        padding: 0;
        max-width: 100%;
      }
    }
  `,
  template: `
    <div class="page">
      <div class="report-toolbar no-print">
        <div style="display: flex; align-items: center; gap: 0.6rem; flex-wrap: wrap">
          <button type="button" class="btn btn-sm" (click)="goBack()">
            <span>← Back</span>
          </button>
        </div>

        <div style="display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap">
          <button type="button" class="btn btn-primary" (click)="printPdf()">
            <app-icon name="printer" />
            <span>Print / Save as PDF</span>
          </button>
        </div>
      </div>

      @if (notice(); as item) {
        <article
          #reportSheetEl
          class="pdf-sheet"
          aria-label="Official Out-of-Office Activity Report PDF View"
        >
          <div class="ug-flag-ribbon" aria-hidden="true"></div>

          <!-- Official OPM Department of Refugees Header with QR Code & Barcode -->
          <header class="doc-banner">
            <div class="doc-brand">
              <img
                src="opm-emblem.svg"
                alt="Office of the Prime Minister Department of Refugees Emblem"
                class="opm-emblem"
                width="74"
                height="74"
              />
              <div class="doc-title-block">
                <span class="doc-govt">Republic of Uganda · Office of the Prime Minister</span>
                <span class="doc-dept">Department of Refugees</span>
                <h1>Staff Out-of-Office Report</h1>
              </div>
            </div>

            <!-- QR Code + Barcode + Reference Metadata -->
            <div class="verification-cluster" aria-label="Document QR Code and Barcode Verification">
              <div class="qr-box" title="Official SOOTR Verification QR Code">
                <svg viewBox="0 0 21 21" shape-rendering="crispEdges" aria-label="Verification QR Code">
                  <rect width="21" height="21" fill="#ffffff" />
                  @for (cell of qrMatrix(item); track $index) {
                    <rect [attr.x]="cell.x" [attr.y]="cell.y" width="1" height="1" fill="#0f172a" />
                  }
                </svg>
              </div>

              <div class="code-stack">
                <div class="barcode-box" title="Official Document Barcode">
                  <svg viewBox="0 0 148 26" shape-rendering="crispEdges" aria-label="Reference Barcode">
                    <rect width="148" height="26" fill="#ffffff" />
                    @for (bar of barcodeBars(item); track $index) {
                      <rect [attr.x]="bar.x" y="1" [attr.width]="bar.w" height="24" fill="#0f172a" />
                    }
                  </svg>
                  <span>*{{ item.referenceNumber || item.id.slice(0, 8).toUpperCase() }}-{{ item.startDate.replaceAll('-', '') }}*</span>
                </div>
                <div class="meta-lines">
                  <div>
                    <strong>Ref No:</strong> {{ item.referenceNumber || item.id.slice(0, 8).toUpperCase() }}
                  </div>
                  <div><strong>Station:</strong> {{ item.dutyStation }} · <strong>Issued:</strong> {{ todayDate }}</div>
                </div>
              </div>
            </div>
          </header>

          <!-- SECTION A: PLANNED ACTIVITY DETAILS -->
          <section>
            <div class="section-heading">
              Section A: Planned Activity &amp; Out-of-Office Details
            </div>
            <table class="pdf-table">
              <tbody>
                <tr>
                  <th>Staff Name</th>
                  <td><strong>{{ item.officerName }}</strong></td>
                  <th>Designation</th>
                  <td>{{ item.officerRole }}</td>
                </tr>
                <tr>
                  <th>Department / Unit</th>
                  <td>{{ item.department }}</td>
                  <th>Duty Station</th>
                  <td>{{ item.dutyStation }}</td>
                </tr>
                <tr>
                  <th>Engagement Type</th>
                  <td>{{ engagementLabels[item.engagementType] }}</td>
                  <th>Field Contact Phone</th>
                  <td>{{ item.contactPhone }}</td>
                </tr>
                <tr>
                  <th>Destination / Venue</th>
                  <td><strong>{{ item.destination }}</strong></td>
                  <th>Host / Organizer</th>
                  <td>{{ item.hostOrganization }}</td>
                </tr>
                <tr>
                  <th>Planned Start Date</th>
                  <td>{{ item.startDate }}</td>
                  <th>Planned Return Date</th>
                  <td>
                    {{ item.endDate }}
                    ({{ store.computeDurationDays(item.startDate, item.endDate) }} day(s))
                  </td>
                </tr>
                <tr>
                  <th>Acting / Cover Officer</th>
                  <td>{{ item.actingOfficer }}</td>
                  <th>Date Logged</th>
                  <td>{{ item.createdAt }}</td>
                </tr>
              </tbody>
            </table>

            <!-- A1. Tabular Planned Purpose & Deliverables -->
            <div class="section-sub-heading">
              A1. Official Purpose &amp; Planned Deliverables
            </div>
            <table class="pdf-list-table">
              <thead>
                <tr>
                  <th class="col-no">No</th>
                  <th>Purpose / Planned Deliverable</th>
                </tr>
              </thead>
              <tbody>
                @for (p of purposeList(item); track $index) {
                  <tr>
                    <td class="col-no">{{ $index + 1 }}</td>
                    <td>{{ p }}</td>
                  </tr>
                }
              </tbody>
            </table>

            @if (item.attendingWithOthers && item.attendingOfficers?.length) {
              <div class="section-sub-heading">
                A2. Other OPM Staff Attending Activity
              </div>
              <table class="pdf-list-table">
                <thead>
                  <tr>
                    <th class="col-no">No</th>
                    <th>Staff Name</th>
                    <th>Designation</th>
                    <th>Department / Duty Station</th>
                  </tr>
                </thead>
                <tbody>
                  @for (ao of item.attendingOfficers!; track $index) {
                    <tr>
                      <td class="col-no">{{ $index + 1 }}</td>
                      <td><strong>{{ ao.name }}</strong></td>
                      <td>{{ ao.designation }}</td>
                      <td>{{ ao.dutyStation }} · {{ ao.department }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </section>

          <!-- SECTION B: REPORT BACK & MANAGEMENT ACTIONS -->
          <section>
            <div class="section-heading">
              Section B: Post-Engagement Report Back &amp; Management Actions
            </div>

            <!-- Return & Stakeholders Summary -->
            <table class="pdf-table">
              <tbody>
                <tr>
                  <th>Actual Return Date</th>
                  <td>{{ item.reportBack?.actualReturnDate || item.endDate }}</td>
                  <th>Report Submitted On</th>
                  <td>{{ item.reportBack?.submittedAt || 'Pending Submission' }}</td>
                </tr>
                <tr>
                  <th>Stakeholders / Participants Met</th>
                  <td colspan="3">
                    {{ item.reportBack?.stakeholdersMet || item.hostOrganization }}
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- B1. Key Issues Raised / Matters Discussed Table -->
            <div class="section-sub-heading">
              B1. Key Issues Raised / Matters Discussed
            </div>
            <table class="pdf-list-table">
              <thead>
                <tr>
                  <th class="col-no">No</th>
                  <th>Key Issue Raised / Matter Discussed</th>
                </tr>
              </thead>
              <tbody>
                @for (issue of keyIssuesList(item); track $index) {
                  <tr>
                    <td class="col-no">{{ $index + 1 }}</td>
                    <td>{{ issue }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="2" style="color: var(--text-muted); text-align: center">
                      No key issues raised or matters discussed recorded yet.
                    </td>
                  </tr>
                }
              </tbody>
            </table>

            <!-- B2. What Was Achieved (Deliverables) Table -->
            <div class="section-sub-heading">
              B2. What Was Achieved (Deliverables)
            </div>
            <table class="pdf-list-table">
              <thead>
                <tr>
                  <th class="col-no">No</th>
                  <th>Deliverable Achieved</th>
                </tr>
              </thead>
              <tbody>
                @for (ach of achievementsList(item); track $index) {
                  <tr>
                    <td class="col-no">{{ $index + 1 }}</td>
                    <td>{{ ach }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="2" style="color: var(--text-muted); text-align: center">
                      No outcomes or deliverables recorded yet.
                    </td>
                  </tr>
                }
              </tbody>
            </table>

            <!-- B3. Management Action Table -->
            <div class="section-sub-heading">
              B3. Management Action
            </div>
            <table class="pdf-list-table">
              <thead>
                <tr>
                  <th class="col-no">No</th>
                  <th>Follow Up Action</th>
                  <th>Responsible Officer</th>
                  <th>Target Date</th>
                </tr>
              </thead>
              <tbody>
                @for (act of managementActionsList(item); track $index) {
                  <tr>
                    <td class="col-no">{{ $index + 1 }}</td>
                    <td>{{ act.followUpAction }}</td>
                    <td>{{ act.responsibleOfficer }}</td>
                    <td>{{ act.targetDate }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="4" style="color: var(--text-muted); text-align: center">
                      No management follow-up actions recorded.
                    </td>
                  </tr>
                }
              </tbody>
            </table>

            <!-- B4. Field Photos or PDF report -->
            <div class="section-sub-heading">
              B4. Field Photos or PDF report
            </div>
            <table class="pdf-list-table">
              <thead>
                <tr>
                  <th class="col-no">No</th>
                  <th>File Name</th>
                  <th>Document Type</th>
                  <th>Uploaded Date</th>
                </tr>
              </thead>
              <tbody>
                @for (att of allAttachments(item); track att.id) {
                  <tr>
                    <td class="col-no">{{ $index + 1 }}</td>
                    <td>
                      @if (att.downloadUrl || att.dataUrl) {
                        <a
                          [href]="att.downloadUrl || att.dataUrl"
                          [download]="att.name"
                          target="_blank"
                          rel="noopener"
                          style="color: #005eb8; font-weight: 600; text-decoration: underline"
                        >
                          {{ att.name }}
                        </a>
                      } @else {
                        {{ att.name }}
                      }
                    </td>
                    <td>{{ att.kind }}</td>
                    <td>{{ att.uploadedAt }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="4" style="color: var(--text-muted); text-align: center">
                      No attachments uploaded.
                    </td>
                  </tr>
                }
              </tbody>
            </table>

            @if (imageAttachments(item).length > 0) {
              <div class="photo-strip">
                @for (img of imageAttachments(item); track img.id) {
                  <div class="photo-box">
                    <img [src]="img.previewUrl || img.streamUrl || img.dataUrl" [alt]="img.name" />
                    <small>{{ img.name }}</small>
                  </div>
                }
              </div>
            }
          </section>

          <!-- Official Sign-Off Table -->
          <section>
            <table class="pdf-table">
              <tbody>
                <tr>
                  <th>Submitted By (Officer)</th>
                  <td colspan="3">
                    <strong>{{ item.officerName }}</strong><br />
                    <small style="color: var(--text-muted)">{{ item.officerRole }} · {{ item.dutyStation }}</small>
                  </td>
                </tr>
              </tbody>
            </table>
          </section>

          <!-- Footer Verification Strip -->
          <footer class="doc-footer-strip">
            <span>
              <strong>OPM · Department of Refugees</strong> · Official SOOTR Verification Document
            </span>
            <span style="font-family: var(--font-mono)">
              VERIFY-ID: SOOTR-{{ item.id }}-{{ item.dutyStation.toUpperCase().replaceAll(' ', '') }}
            </span>
          </footer>
        </article>
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
export class ViewReport implements OnInit {
  protected readonly store = inject(StaffTrackStore);
  private readonly auth = inject(AuthService);
  private readonly api = inject(StaffTrackApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);

  protected readonly engagementLabels = ENGAGEMENT_TYPE_LABELS;
  protected readonly statusLabels = OOO_STATUS_LABELS;
  protected readonly noticeId = signal('');
  protected readonly filesApiAttachments = signal<ReportBackAttachment[]>([]);
  protected readonly todayDate = new Date().toISOString().slice(0, 10);
  private readonly reportSheetRef = viewChild<ElementRef<HTMLElement>>('reportSheetEl');

  protected readonly notice = computed<OutOfOfficeNotification | undefined>(() =>
    this.store.outOfOfficeList().find((i) => i.id === this.noticeId())
  );

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.noticeId.set(id);
    if (id) {
      const confirmedBy = this.auth.session()?.fullName || 'Supervisor';
      this.store.loadOutOfOfficeById(id, (loaded) => {
        if ((loaded.supervisorConfirmationStatus || 'NEW') === 'NEW') {
          this.store.markNotificationAsRead(loaded.id, confirmedBy, true);
        }
      });
      this.loadFilesFromFilesApi(id);
    }
  }

  private loadFilesFromFilesApi(entityRefId: string): void {
    this.api.getFilesByEntityRefId(entityRefId).subscribe({
      next: (resp) => {
        const list = resp?.data || [];
        if (list.length === 0) return;
        this.filesApiAttachments.set(
          list.map((dto: GeneralFileAttachmentApiDto) => {
            const mime = dto.mimeType || 'application/octet-stream';
            const ext = (dto.fileType || '').toLowerCase();
            const lowerName = (dto.originalFilename || dto.filename || '').toLowerCase();
            const kind: ReportBackAttachment['kind'] =
              mime.startsWith('image/') ||
              ['jpg', 'jpeg', 'png', 'webp', 'gif', 'svg'].includes(ext)
                ? 'IMAGE'
                : mime === 'application/pdf' || ext === 'pdf' || lowerName.endsWith('.pdf')
                ? 'PDF'
                : 'DOCUMENT';
            const downloadUrl = this.api.getFileDownloadUrl(dto.fileUuid);
            const streamUrl = this.api.getFileStreamUrl(dto.fileUuid);
            const previewUrl =
              kind === 'IMAGE' ? this.api.getFilePreviewUrl(dto.fileUuid) : streamUrl;
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
              uploadedAt: dto.date ? dto.date.slice(0, 10) : this.todayDate,
            };
          })
        );
      },
      error: () => {},
    });
  }

  protected goBack(): void {
    this.location.back();
  }

  /** Generates a 21x21 QR Code matrix with finder squares, timing lines, and payload-seeded data cells */
  protected qrMatrix(item: OutOfOfficeNotification): QrCell[] {
    const size = 21;
    const grid: boolean[][] = Array.from({ length: size }, () => Array(size).fill(false));
    const reserved: boolean[][] = Array.from({ length: size }, () => Array(size).fill(false));

    const drawFinder = (ox: number, oy: number) => {
      for (let dy = -1; dy <= 7; dy++) {
        for (let dx = -1; dx <= 7; dx++) {
          const x = ox + dx;
          const y = oy + dy;
          if (x < 0 || x >= size || y < 0 || y >= size) continue;
          reserved[y][x] = true;
          if (dx >= 0 && dx <= 6 && dy >= 0 && dy <= 6) {
            const onOuter = dx === 0 || dx === 6 || dy === 0 || dy === 6;
            const onInner = dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4;
            grid[y][x] = onOuter || onInner;
          }
        }
      }
    };

    drawFinder(0, 0);
    drawFinder(size - 7, 0);
    drawFinder(0, size - 7);

    // Timing patterns
    for (let i = 8; i < size - 8; i++) {
      reserved[6][i] = true;
      reserved[i][6] = true;
      grid[6][i] = i % 2 === 0;
      grid[i][6] = i % 2 === 0;
    }

    const payload = `SOOTR|${item.id}|${item.officerName}|${item.dutyStation}|${item.startDate}|${item.endDate}`;
    let hash = 2166136261;
    for (let i = 0; i < payload.length; i++) {
      hash ^= payload.charCodeAt(i);
      hash = Math.imul(hash, 16777619);
    }

    for (let y = 0; y < size; y++) {
      for (let x = 0; x < size; x++) {
        if (reserved[y][x]) continue;
        hash ^= (x * 37 + y * 73 + payload.charCodeAt((x + y) % payload.length)) & 0xff;
        hash = Math.imul(hash, 16777619);
        grid[y][x] = (Math.abs(hash) & 3) !== 0 && ((x + y + (hash >>> 3)) & 1) === 1;
      }
    }

    const cells: QrCell[] = [];
    for (let y = 0; y < size; y++) {
      for (let x = 0; x < size; x++) {
        if (grid[y][x]) cells.push({ x, y });
      }
    }
    return cells;
  }

  /** Generates Code-128 style barcode bars deterministically from the report reference ID */
  protected barcodeBars(item: OutOfOfficeNotification): BarcodeBar[] {
    const code = `*${item.id}-${item.startDate.replaceAll('-', '')}*`;
    const bars: BarcodeBar[] = [];
    let cursor = 4;

    // Start guard bars
    bars.push({ x: cursor, w: 2 });
    cursor += 3;
    bars.push({ x: cursor, w: 1 });
    cursor += 3;

    for (let i = 0; i < code.length; i++) {
      const ch = code.charCodeAt(i);
      const w1 = (ch % 3) + 1;
      const s1 = ((ch >> 2) % 2) + 1;
      const w2 = ((ch >> 3) % 2) + 1;
      const s2 = ((ch >> 4) % 2) + 1;
      if (cursor + w1 + s1 + w2 + s2 > 140) break;
      bars.push({ x: cursor, w: w1 });
      cursor += w1 + s1;
      bars.push({ x: cursor, w: w2 });
      cursor += w2 + s2;
    }

    // Stop guard bars
    if (cursor <= 142) {
      bars.push({ x: cursor, w: 2 });
      cursor += 3;
      bars.push({ x: cursor, w: 1 });
    }
    return bars;
  }

  protected keyIssuesList(item: OutOfOfficeNotification): string[] {
    if (item.reportBack?.keyIssuesList?.length) {
      return item.reportBack.keyIssuesList;
    }
    if (item.reportBack?.outstandingIssues) {
      return item.reportBack.outstandingIssues.split(/\s*;\s*|\n+/).filter(Boolean);
    }
    return [];
  }

  protected purposeList(item: OutOfOfficeNotification): string[] {
    if (item.purposeList?.length) {
      return item.purposeList;
    }
    if (item.purpose) {
      return item.purpose.split(/\s*;\s*|\n+/).filter(Boolean);
    }
    return [];
  }

  protected achievementsList(item: OutOfOfficeNotification): string[] {
    if (item.reportBack?.achievementsList?.length) {
      return item.reportBack.achievementsList;
    }
    if (item.reportBack?.whatWasAchieved) {
      return item.reportBack.whatWasAchieved.split(/\s*;\s*|\n+/).filter(Boolean);
    }
    return [];
  }

  protected managementActionsList(item: OutOfOfficeNotification) {
    if (item.reportBack?.managementActions?.length) {
      return item.reportBack.managementActions;
    }
    if (item.managementActionRequired) {
      return [
        {
          followUpAction: item.managementActionRequired,
          responsibleOfficer: item.officerName,
          targetDate: item.endDate,
        },
      ];
    }
    return [];
  }

  protected allAttachments(item: OutOfOfficeNotification): ReportBackAttachment[] {
    const reportAtts = [...(item.reportBack?.attachments || [])];
    const fromFilesApi = this.filesApiAttachments();
    if (fromFilesApi.length === 0) return reportAtts;

    const merged = [...reportAtts];
    for (const apiFile of fromFilesApi) {
      const idx = merged.findIndex(
        (a) =>
          a.fileUuid === apiFile.fileUuid ||
          a.id === apiFile.fileUuid ||
          a.name.toLowerCase() === apiFile.name.toLowerCase()
      );
      if (idx >= 0) {
        merged[idx] = { ...merged[idx], ...apiFile };
      } else {
        merged.push(apiFile);
      }
    }
    return merged;
  }

  protected imageAttachments(item: OutOfOfficeNotification): ReportBackAttachment[] {
    return this.allAttachments(item).filter((a) => a.kind === 'IMAGE');
  }

  protected printPdf(): void {
    if (typeof window === 'undefined' || typeof document === 'undefined') return;
    const sheet = this.reportSheetRef()?.nativeElement;
    const item = this.notice();
    if (!sheet || !item) {
      window.print();
      return;
    }

    const iframe = document.createElement('iframe');
    iframe.style.position = 'fixed';
    iframe.style.right = '0';
    iframe.style.bottom = '0';
    iframe.style.width = '0';
    iframe.style.height = '0';
    iframe.style.border = '0';
    iframe.setAttribute('aria-hidden', 'true');
    document.body.appendChild(iframe);

    const doc = iframe.contentWindow?.document;
    if (!doc || !iframe.contentWindow) {
      document.body.removeChild(iframe);
      window.print();
      return;
    }

    const baseHref = document.baseURI || window.location.origin + '/';
    doc.open();
    doc.write(`<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <base href="${baseHref}">
  <title>Staff Out-of-Office Report - ${item.id}</title>
  <style>
    @page { size: A4 portrait; margin: 12mm; }
    *, *::before, *::after { box-sizing: border-box; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    body {
      margin: 0;
      padding: 0;
      font-family: 'Inter', system-ui, -apple-system, sans-serif;
      color: #0f172a;
      background: #ffffff;
      line-height: 1.45;
    }
    h1, h2, h3, p { margin: 0; }
    .pdf-sheet { display: grid; gap: 1.1rem; }
    .ug-flag-ribbon {
      height: 6px;
      border-radius: 4px;
      background: linear-gradient(90deg, #111827 0%, #111827 33.3%, #facc15 33.3%, #facc15 66.6%, #dc2626 66.6%, #dc2626 100%);
    }
    .doc-banner {
      display: grid;
      grid-template-columns: 1fr auto;
      gap: 1rem;
      padding-bottom: 0.9rem;
      border-bottom: 2px solid #005eb8;
      align-items: center;
    }
    .doc-brand { display: flex; align-items: center; gap: 0.9rem; }
    .opm-emblem { width: 68px; height: 68px; flex: none; }
    .doc-title-block { display: grid; gap: 0.15rem; }
    .doc-govt { font-size: 0.7rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.08em; color: #475569; }
    .doc-dept { font-size: 0.85rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.04em; color: #005eb8; }
    .doc-title-block h1 { font-size: 1.22rem; font-weight: 800; color: #0f172a; }
    .verification-cluster {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.45rem 0.65rem;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      background: #f8fafc;
    }
    .qr-box { width: 66px; height: 66px; padding: 3px; background: #fff; border: 1px solid #cbd5e1; border-radius: 5px; flex: none; }
    .qr-box svg { width: 100%; height: 100%; display: block; }
    .code-stack { display: grid; gap: 0.2rem; font-size: 0.72rem; }
    .barcode-box { background: #fff; border: 1px solid #cbd5e1; border-radius: 4px; padding: 3px 6px 2px; display: grid; justify-items: center; }
    .barcode-box svg { width: 142px; height: 24px; display: block; }
    .barcode-box span { font-family: ui-monospace, monospace; font-size: 0.6rem; font-weight: 700; letter-spacing: 0.08em; color: #0f172a; }
    .meta-lines { display: grid; gap: 0.1rem; color: #475569; }
    .section-heading {
      background: #005eb8;
      color: #ffffff;
      padding: 0.45rem 0.75rem;
      font-size: 0.8rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      border-radius: 5px 5px 0 0;
    }
    .section-sub-heading {
      background: #eef2f9;
      color: #0f172a;
      padding: 0.4rem 0.75rem;
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      border: 1px solid #cbd5e1;
      border-bottom: 0;
      margin-top: 0.65rem;
    }
    .pdf-table, .pdf-list-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; }
    .pdf-table th, .pdf-table td, .pdf-list-table th, .pdf-list-table td {
      border: 1px solid #cbd5e1;
      padding: 0.5rem 0.7rem;
      vertical-align: top;
      text-align: left;
    }
    .pdf-table th { background: #f1f5f9; font-weight: 700; width: 22%; }
    .pdf-table td { width: 28%; }
    .pdf-list-table thead th { background: #f1f5f9; font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.04em; }
    .col-no { width: 48px; text-align: center !important; font-weight: 700; }
    .photo-strip { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.65rem; margin-top: 0.55rem; }
    .photo-box { border: 1px solid #cbd5e1; border-radius: 5px; overflow: hidden; background: #f8fafc; }
    .photo-box img { width: 100%; height: 110px; object-fit: cover; display: block; }
    .photo-box small { display: block; padding: 0.3rem 0.45rem; font-size: 0.7rem; }
    .doc-footer-strip {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-top: 0.7rem;
      border-top: 1px dashed #cbd5e1;
      font-size: 0.7rem;
      color: #475569;
    }
  </style>
</head>
<body>
  <div class="pdf-sheet">${sheet.innerHTML}</div>
</body>
</html>`);
    doc.close();

    setTimeout(() => {
      iframe.contentWindow?.focus();
      iframe.contentWindow?.print();
      setTimeout(() => {
        if (document.body.contains(iframe)) {
          document.body.removeChild(iframe);
        }
      }, 1500);
    }, 250);
  }
}
