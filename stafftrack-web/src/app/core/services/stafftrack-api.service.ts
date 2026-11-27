import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import {
  ActivationStatus,
  ClientUserLink,
  EngagementType,
  NotificationChannelConfig,
  NotificationSupervisor,
  Organisation,
  OrganisationStatus,
  OutOfOfficeReportBack,
  OutOfOfficeStatus,
  SupervisorConfirmationRecord,
  SupervisorConfirmationStatus,
  SupervisorNotificationStatus,
  SystemUserAccount,
  UnitHeadApprovalStatus,
  UserAccountStatus,
} from '../models/stafftrack.models';

export interface MasterLookupApiDto {
  id: string;
  clientRef?: string;
  code: string;
  name: string;
  description?: string;
  recordStatus?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MasterLookupRequestDto {
  clientRef?: string;
  code: string;
  name: string;
  description?: string;
}

export interface StaffMemberApiDto {
  id: string;
  clientRef?: string;
  staffCode?: string;
  surname: string;
  lastName: string;
  fullName?: string;
  designation: string;
  designationId?: string;
  email: string;
  alternativeEmail?: string;
  phone: string;
  alternativePhone?: string;
  department: string;
  departmentId?: string;
  dutyStation: string;
  dutyStationId?: string;
  status: 'ACTIVE' | 'INACTIVE';
  recordStatus?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface StaffMemberRequestDto {
  clientRef?: string;
  surname: string;
  lastName: string;
  designation: string;
  designationId?: string;
  email: string;
  alternativeEmail?: string;
  phone: string;
  alternativePhone?: string;
  department: string;
  departmentId?: string;
  dutyStation: string;
  dutyStationId?: string;
  status?: 'ACTIVE' | 'INACTIVE';
}

export interface OutOfOfficeApiDto {
  id: string;
  clientRef?: string;
  referenceNumber?: string;
  staffMemberId?: string;
  officerName: string;
  officerRole: string;
  department: string;
  dutyStation: string;
  engagementType: EngagementType;
  destination: string;
  hostOrganization: string;
  startDate: string;
  endDate: string;
  purpose: string;
  purposeList?: string[];
  attendingWithOthers?: boolean;
  attendingOfficers?: Array<{
    id?: string;
    staffMemberId?: string;
    name: string;
    designation: string;
    department: string;
    dutyStation: string;
    isCustom?: boolean;
  }>;
  actingOfficer: string;
  contactPhone: string;
  status: OutOfOfficeStatus;
  managementActionRequired?: string;
  unitHeadSupervisorId?: string;
  unitHeadName?: string;
  unitHeadEmail?: string;
  unitHeadPhone?: string;
  unitHeadApprovalStatus?: UnitHeadApprovalStatus;
  unitHeadReviewedAt?: string;
  unitHeadReviewedBy?: string;
  unitHeadComment?: string;
  submitterRole?: string;
  supervisorConfirmationStatus?: SupervisorConfirmationStatus;
  supervisorSeenAt?: string;
  supervisorConfirmedBy?: string;
  supervisorConfirmations?: SupervisorConfirmationRecord[];
  reportBack?: OutOfOfficeReportBack;
  createdAt?: string;
  updatedAt?: string;
}

export interface DashboardSummaryApiDto {
  currentlyAwayCount: number;
  currentlyInOfficeCount?: number;
  upcomingApprovedCount: number;
  pendingApprovalCount: number;
  returnedCount: number;
  totalNotificationsCount: number;
  registeredStaffCount: number;
  departmentsCount: number;
  designationsCount: number;
  dutyStationsCount: number;
  weeklyReportsCount: number;
  engagementTypeBreakdown: Record<EngagementType, number>;
  deployments: OutOfOfficeApiDto[];
}

export interface WeeklyConsolidateRequestDto {
  clientRef?: string;
  weekLabel: string;
  weekStart: string;
  weekEnd: string;
  departmentFilter?: string;
  dutyStation?: string;
  generatedBy: string;
  generatedByDesignation?: string;
}

export interface WeeklyConsolidatedReportApiDto {
  id: string;
  clientRef?: string;
  referenceNumber?: string;
  weekLabel: string;
  weekStart: string;
  weekEnd: string;
  departmentFilter: string;
  dutyStation?: string;
  generatedBy: string;
  generatedByDesignation?: string;
  generatedAt: string;
  executiveSummary?: string;
  activitiesUndertaken: Array<{
    date: string;
    officerName: string;
    officerDesignation: string;
    department: string;
    dutyStation: string;
    category: string;
    summary: string;
  }>;
  pendingActions: Array<{
    item: string;
    source: string;
    responsibleOfficer: string;
    department: string;
    dutyStation: string;
    targetDate: string;
    status: string;
  }>;
  createdAt?: string;
}

export interface FilesApiResponseDto<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

export interface GeneralFileAttachmentApiDto {
  originalFilename: string;
  filename: string;
  fileUuid: string;
  fileSize: number;
  fileType: string;
  mimeType: string;
  contentType: string;
  filePath?: string;
  organisationId?: string;
  entityRefId: string;
  category?: string;
  subcategory?: string;
  subEntityRefId?: string;
  attachmentOrigin?: string;
  date?: string;
}

const API_PATH = '/api/v1/stafftrack';
const FILES_API_PATH = '/api/v1/files/generalattachments';

/**
 * Primary requests always route through the same-origin proxy (/api -> API Gateway).
 * If the container reverse proxy ever returns 502/503/504 (e.g. before joining swizima-network),
 * fallback automatically retries against the host's API Gateway (:9080).
 */
function resolveGatewayFallbackOrigin(): string | null {
  if (typeof window === 'undefined') return null;
  const { protocol, hostname } = window.location;
  if (protocol === 'http:' && hostname && hostname !== 'localhost' && hostname !== '127.0.0.1') {
    return `${protocol}//${hostname}:9080`;
  }
  return null;
}

function resolveBaseUrls(): { primary: string; fallback: string } {
  const gw = resolveGatewayFallbackOrigin();
  return {
    primary: API_PATH,
    fallback: gw ? `${gw}${API_PATH}` : API_PATH,
  };
}

function resolveFilesApiBaseUrls(): { primary: string; fallback: string } {
  const gw = resolveGatewayFallbackOrigin();
  return {
    primary: FILES_API_PATH,
    fallback: gw ? `${gw}${FILES_API_PATH}` : FILES_API_PATH,
  };
}

export function extractApiErrorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error;
    if (body && typeof body === 'object') {
      if (typeof body.detail === 'string' && body.detail.trim()) {
        return body.detail;
      }
      if (typeof body.message === 'string' && body.message.trim()) {
        return body.message;
      }
      if (body.errors && typeof body.errors === 'object') {
        const firstErr = Object.values(body.errors)[0];
        if (typeof firstErr === 'string') return firstErr;
      }
    }
    if (err.status === 0) {
      return 'Unable to reach StaffTrack API server (check port 9080 / 8086).';
    }
    return err.statusText || 'Request failed';
  }
  return err instanceof Error ? err.message : 'Unexpected error';
}

@Injectable({ providedIn: 'root' })
export class StaffTrackApiService {
  private readonly http = inject(HttpClient);
  private readonly urls = resolveBaseUrls();
  private readonly filesUrls = resolveFilesApiBaseUrls();

  private withFallback<T>(
    requestFn: (baseUrl: string) => Observable<T>,
    allowRetryOn404 = false
  ): Observable<T> {
    return requestFn(this.urls.primary).pipe(
      catchError((err) => {
        // Fallback to secondary URL on connection/CORS error (status 0) or gateway 502/503/504
        if (
          this.urls.fallback !== this.urls.primary &&
          err instanceof HttpErrorResponse &&
          (err.status === 0 ||
            (allowRetryOn404 && err.status === 404) ||
            err.status === 502 ||
            err.status === 503 ||
            err.status === 504)
        ) {
          return requestFn(this.urls.fallback);
        }
        return throwError(() => err);
      })
    );
  }

  private withFilesFallback<T>(
    requestFn: (baseUrl: string) => Observable<T>
  ): Observable<T> {
    return requestFn(this.filesUrls.primary).pipe(
      catchError((err) => {
        if (
          this.filesUrls.fallback !== this.filesUrls.primary &&
          err instanceof HttpErrorResponse &&
          (err.status === 0 ||
            err.status === 404 ||
            err.status === 502 ||
            err.status === 503 ||
            err.status === 504)
        ) {
          return requestFn(this.filesUrls.fallback);
        }
        return throwError(() => err);
      })
    );
  }

  // ---------- Files API (General File Attachments - port 8087 / 9080) ----------
  getFilesApiBaseUrl(): string {
    return this.filesUrls.primary;
  }

  getFileDownloadUrl(fileUuid: string): string {
    return `${this.filesUrls.primary}/download/${encodeURIComponent(fileUuid)}`;
  }

  getFileStreamUrl(fileUuid: string): string {
    return `${this.filesUrls.primary}/stream/${encodeURIComponent(fileUuid)}`;
  }

  getFilePreviewUrl(fileUuid: string): string {
    return `${this.filesUrls.primary}/preview/${encodeURIComponent(fileUuid)}`;
  }

  getFileThumbnailUrl(fileUuid: string): string {
    return `${this.filesUrls.primary}/thumbnail/${encodeURIComponent(fileUuid)}`;
  }

  uploadGeneralAttachment(params: {
    file: File;
    organisationId: string;
    entityRefId: string;
    category?: string;
    subcategory?: string;
    subEntityRefId?: string;
    attachmentOrigin?: string;
  }): Observable<FilesApiResponseDto<string>> {
    const formData = new FormData();
    formData.append('file', params.file, params.file.name);

    const queryParams: Record<string, string> = {
      organisationId: params.organisationId || 'OPM',
      organisationRef: params.organisationId || 'OPM',
      entityRefId: params.entityRefId,
      category: params.category || 'OUT_OF_OFFICE',
      subcategory: params.subcategory || 'REPORT_BACK',
      subEntityRefId: params.subEntityRefId || params.entityRefId,
      attachmentOrigin: params.attachmentOrigin || 'STAFFTRACK_WEB',
    };

    return this.withFilesFallback((base) =>
      this.http.post<FilesApiResponseDto<string>>(`${base}/upload`, formData, {
        params: queryParams,
      })
    );
  }

  getFilesByEntityRefId(
    entityRefId: string
  ): Observable<FilesApiResponseDto<GeneralFileAttachmentApiDto[]>> {
    return this.withFilesFallback((base) =>
      this.http.get<FilesApiResponseDto<GeneralFileAttachmentApiDto[]>>(
        `${base}/entity/${encodeURIComponent(entityRefId)}`
      )
    );
  }

  deleteGeneralAttachment(fileUuid: string): Observable<FilesApiResponseDto<string>> {
    return this.withFilesFallback((base) =>
      this.http.delete<FilesApiResponseDto<string>>(
        `${base}/${encodeURIComponent(fileUuid)}`
      )
    );
  }

  // ---------- Departments / Units ----------
  getAllDepartments(): Observable<MasterLookupApiDto[]> {
    return this.withFallback((base) =>
      this.http.get<MasterLookupApiDto[]>(`${base}/departments/all`)
    );
  }

  createDepartment(request: MasterLookupRequestDto): Observable<MasterLookupApiDto> {
    return this.withFallback((base) =>
      this.http.post<MasterLookupApiDto>(`${base}/departments`, request)
    );
  }

  updateDepartment(id: string, request: MasterLookupRequestDto): Observable<MasterLookupApiDto> {
    return this.withFallback((base) =>
      this.http.put<MasterLookupApiDto>(
        `${base}/departments/${encodeURIComponent(id)}`,
        request
      )
    );
  }

  deleteDepartment(id: string): Observable<void> {
    return this.withFallback((base) =>
      this.http.delete<void>(`${base}/departments/${encodeURIComponent(id)}`)
    );
  }

  // ---------- Officer Designations ----------
  getAllDesignations(): Observable<MasterLookupApiDto[]> {
    return this.withFallback((base) =>
      this.http.get<MasterLookupApiDto[]>(`${base}/designations/all`)
    );
  }

  createDesignation(request: MasterLookupRequestDto): Observable<MasterLookupApiDto> {
    return this.withFallback((base) =>
      this.http.post<MasterLookupApiDto>(`${base}/designations`, request)
    );
  }

  updateDesignation(id: string, request: MasterLookupRequestDto): Observable<MasterLookupApiDto> {
    return this.withFallback((base) =>
      this.http.put<MasterLookupApiDto>(
        `${base}/designations/${encodeURIComponent(id)}`,
        request
      )
    );
  }

  deleteDesignation(id: string): Observable<void> {
    return this.withFallback((base) =>
      this.http.delete<void>(`${base}/designations/${encodeURIComponent(id)}`)
    );
  }

  // ---------- Duty Stations ----------
  getAllDutyStations(): Observable<MasterLookupApiDto[]> {
    return this.withFallback((base) =>
      this.http.get<MasterLookupApiDto[]>(`${base}/duty-stations/all`)
    );
  }

  createDutyStation(request: MasterLookupRequestDto): Observable<MasterLookupApiDto> {
    return this.withFallback((base) =>
      this.http.post<MasterLookupApiDto>(`${base}/duty-stations`, request)
    );
  }

  updateDutyStation(id: string, request: MasterLookupRequestDto): Observable<MasterLookupApiDto> {
    return this.withFallback((base) =>
      this.http.put<MasterLookupApiDto>(
        `${base}/duty-stations/${encodeURIComponent(id)}`,
        request
      )
    );
  }

  deleteDutyStation(id: string): Observable<void> {
    return this.withFallback((base) =>
      this.http.delete<void>(`${base}/duty-stations/${encodeURIComponent(id)}`)
    );
  }

  // ---------- Staff Register ----------
  getAllStaff(): Observable<StaffMemberApiDto[]> {
    return this.withFallback((base) =>
      this.http.get<StaffMemberApiDto[]>(`${base}/staff/all`)
    );
  }

  registerStaff(request: StaffMemberRequestDto): Observable<StaffMemberApiDto> {
    return this.withFallback((base) =>
      this.http.post<StaffMemberApiDto>(`${base}/staff`, request)
    );
  }

  updateStaff(id: string, request: StaffMemberRequestDto): Observable<StaffMemberApiDto> {
    return this.withFallback((base) =>
      this.http.put<StaffMemberApiDto>(`${base}/staff/${encodeURIComponent(id)}`, request)
    );
  }

  deleteStaff(id: string): Observable<void> {
    return this.withFallback((base) =>
      this.http.delete<void>(`${base}/staff/${encodeURIComponent(id)}`)
    );
  }

  // ---------- Out-of-Office & Report Back ----------
  getAllOutOfOffice(params?: {
    role?: string;
    staffMemberId?: string;
    officerName?: string;
    callerEmail?: string;
    callerPhone?: string;
  }): Observable<OutOfOfficeApiDto[]> {
    const queryParams: Record<string, string> = {};
    if (params?.role?.trim()) queryParams['role'] = params.role.trim();
    if (params?.staffMemberId?.trim()) queryParams['staffMemberId'] = params.staffMemberId.trim();
    if (params?.officerName?.trim()) queryParams['officerName'] = params.officerName.trim();
    if (params?.callerEmail?.trim()) queryParams['callerEmail'] = params.callerEmail.trim();
    if (params?.callerPhone?.trim()) queryParams['callerPhone'] = params.callerPhone.trim();

    return this.withFallback((base) =>
      this.http.get<OutOfOfficeApiDto[]>(`${base}/out-of-office/all`, {
        params: queryParams,
      })
    );
  }

  getOutOfOfficeById(id: string): Observable<OutOfOfficeApiDto> {
    return this.withFallback((base) =>
      this.http.get<OutOfOfficeApiDto>(`${base}/out-of-office/${encodeURIComponent(id)}`)
    );
  }

  recordOutOfOffice(request: Partial<OutOfOfficeApiDto>): Observable<OutOfOfficeApiDto> {
    const clean = this.sanitizeOutOfOfficePayload(request);
    return this.withFallback((base) =>
      this.http.post<OutOfOfficeApiDto>(`${base}/out-of-office`, clean)
    );
  }

  updateOutOfOffice(id: string, request: Partial<OutOfOfficeApiDto>): Observable<OutOfOfficeApiDto> {
    const clean = this.sanitizeOutOfOfficePayload(request);
    return this.withFallback((base) =>
      this.http.put<OutOfOfficeApiDto>(
        `${base}/out-of-office/${encodeURIComponent(id)}`,
        clean
      )
    );
  }

  updateOutOfOfficeStatus(id: string, status: OutOfOfficeStatus): Observable<OutOfOfficeApiDto> {
    return this.withFallback((base) =>
      this.http.put<OutOfOfficeApiDto>(
        `${base}/out-of-office/${encodeURIComponent(id)}/status`,
        { status }
      )
    );
  }

  submitUnitHeadDecision(
    id: string,
    payload: {
      decision: 'ACCEPTED' | 'REJECTED';
      reviewedBy?: string;
      comment?: string;
    }
  ): Observable<OutOfOfficeApiDto> {
    return this.withFallback((base) =>
      this.http.put<OutOfOfficeApiDto>(
        `${base}/out-of-office/${encodeURIComponent(id)}/unit-head-decision`,
        payload
      )
    );
  }

  updateOutOfOfficeSupervisorConfirmation(
    id: string,
    confirmationStatus: SupervisorConfirmationStatus,
    confirmedBy?: string
  ): Observable<OutOfOfficeApiDto> {
    return this.withFallback((base) =>
      this.http.put<OutOfOfficeApiDto>(
        `${base}/out-of-office/${encodeURIComponent(id)}/supervisor-confirmation`,
        { confirmationStatus, confirmedBy }
      )
    );
  }

  submitReportBack(
    id: string,
    reportBack: OutOfOfficeReportBack
  ): Observable<OutOfOfficeApiDto> {
    const cleanPayload = {
      actualReturnDate: reportBack.actualReturnDate,
      stakeholdersMet: reportBack.stakeholdersMet,
      whatWasDone: reportBack.whatWasDone,
      whatWasDiscussed: reportBack.whatWasDiscussed,
      whatWasAchieved: reportBack.whatWasAchieved,
      outstandingIssues: reportBack.outstandingIssues,
      keyIssuesList: reportBack.keyIssuesList || [],
      achievementsList: reportBack.achievementsList || [],
      managementActions: (reportBack.managementActions || []).map((ma) => ({
        id: this.isUuid(ma.id) ? ma.id : undefined,
        followUpAction: ma.followUpAction,
        responsibleStaffId: this.isUuid(ma.responsibleStaffId) ? ma.responsibleStaffId : undefined,
        responsibleOfficer: ma.responsibleOfficer,
        targetDate: ma.targetDate || undefined,
      })),
      attachments: (reportBack.attachments || []).map((att) => ({
        id: this.isUuid(att.id) ? att.id : undefined,
        name: att.name,
        mimeType: att.mimeType,
        sizeBytes: att.sizeBytes,
        kind: att.kind,
        dataUrl: att.dataUrl,
        uploadedAt: att.uploadedAt || undefined,
      })),
    };
    return this.withFallback((base) =>
      this.http.post<OutOfOfficeApiDto>(
        `${base}/out-of-office/${encodeURIComponent(id)}/report-back`,
        cleanPayload
      )
    );
  }

  deleteOutOfOffice(id: string): Observable<void> {
    return this.withFallback((base) =>
      this.http.delete<void>(`${base}/out-of-office/${encodeURIComponent(id)}`)
    );
  }

  // ---------- Dashboard ----------
  getDashboardSummary(params?: {
    query?: string;
    department?: string;
    status?: OutOfOfficeStatus;
  }): Observable<DashboardSummaryApiDto> {
    const queryParams: Record<string, string> = {};
    if (params?.query?.trim()) queryParams['query'] = params.query.trim();
    if (params?.department && params.department !== 'ALL') {
      queryParams['department'] = params.department;
    }
    if (params?.status) queryParams['status'] = params.status;

    return this.withFallback((base) =>
      this.http.get<DashboardSummaryApiDto>(`${base}/dashboard/summary`, {
        params: queryParams,
      })
    );
  }

  getDashboardDeployments(params?: {
    query?: string;
    department?: string;
    status?: OutOfOfficeStatus;
  }): Observable<OutOfOfficeApiDto[]> {
    const queryParams: Record<string, string> = {};
    if (params?.query?.trim()) queryParams['query'] = params.query.trim();
    if (params?.department && params.department !== 'ALL') {
      queryParams['department'] = params.department;
    }
    if (params?.status) queryParams['status'] = params.status;

    return this.withFallback((base) =>
      this.http.get<OutOfOfficeApiDto[]>(`${base}/dashboard/deployments`, {
        params: queryParams,
      })
    );
  }

  // ---------- Weekly Consolidated Reports ----------
  getAllWeeklyReports(): Observable<WeeklyConsolidatedReportApiDto[]> {
    return this.withFallback((base) =>
      this.http.get<WeeklyConsolidatedReportApiDto[]>(`${base}/weekly-reports/all`)
    );
  }

  consolidateWeeklyReport(
    request: WeeklyConsolidateRequestDto
  ): Observable<WeeklyConsolidatedReportApiDto> {
    return this.withFallback((base) =>
      this.http.post<WeeklyConsolidatedReportApiDto>(
        `${base}/weekly-reports/consolidate`,
        request
      )
    );
  }

  deleteWeeklyReport(id: string): Observable<void> {
    return this.withFallback((base) =>
      this.http.delete<void>(`${base}/weekly-reports/${encodeURIComponent(id)}`)
    );
  }

  // ---------- User Management (System Users & Client Users) ----------
  getAllUsers(): Observable<SystemUserAccount[]> {
    return this.withFallback((base) =>
      this.http.get<SystemUserAccount[]>(`${base}/users/all`)
    );
  }

  createUser(
    request: Omit<SystemUserAccount, 'id' | 'createdAt'>
  ): Observable<SystemUserAccount> {
    const clean = {
      ...request,
      staffId: this.isUuid(request.staffId) ? request.staffId : undefined,
    };
    return this.withFallback((base) =>
      this.http.post<SystemUserAccount>(`${base}/users`, clean)
    );
  }

  updateUser(
    id: string,
    request: Omit<SystemUserAccount, 'id' | 'createdAt'>
  ): Observable<SystemUserAccount> {
    const clean = {
      ...request,
      staffId: this.isUuid(request.staffId) ? request.staffId : undefined,
    };
    return this.withFallback((base) =>
      this.http.put<SystemUserAccount>(`${base}/users/${encodeURIComponent(id)}`, clean)
    );
  }

  updateUserStatus(id: string, status: UserAccountStatus): Observable<SystemUserAccount> {
    return this.withFallback((base) =>
      this.http.put<SystemUserAccount>(
        `${base}/users/${encodeURIComponent(id)}/status`,
        { status }
      )
    );
  }

  deleteUser(id: string): Observable<void> {
    return this.withFallback((base) =>
      this.http.delete<void>(`${base}/users/${encodeURIComponent(id)}`)
    );
  }

  getAllClientUsers(): Observable<ClientUserLink[]> {
    return this.withFallback((base) =>
      this.http.get<ClientUserLink[]>(`${base}/client-users/all`)
    );
  }

  createClientUser(
    request: Omit<ClientUserLink, 'id' | 'createdAt'>
  ): Observable<ClientUserLink> {
    const clean = {
      ...request,
      systemUserId: this.isUuid(request.systemUserId) ? request.systemUserId : undefined,
      clientRef: this.isUuid(request.clientRef) ? request.clientRef : undefined,
    };
    return this.withFallback((base) =>
      this.http.post<ClientUserLink>(`${base}/client-users`, clean)
    );
  }

  updateClientUserStatus(
    id: string,
    activationStatus: ActivationStatus
  ): Observable<ClientUserLink> {
    return this.withFallback((base) =>
      this.http.put<ClientUserLink>(
        `${base}/client-users/${encodeURIComponent(id)}/status`,
        { activationStatus }
      )
    );
  }

  makeDefaultClientUser(id: string): Observable<ClientUserLink> {
    return this.withFallback((base) =>
      this.http.put<ClientUserLink>(
        `${base}/client-users/${encodeURIComponent(id)}/default`,
        {}
      )
    );
  }

  deleteClientUser(id: string): Observable<void> {
    return this.withFallback((base) =>
      this.http.delete<void>(`${base}/client-users/${encodeURIComponent(id)}`)
    );
  }

  // ---------- Organisations ----------
  getAllOrganisations(): Observable<Organisation[]> {
    return this.withFallback((base) =>
      this.http.get<Organisation[]>(`${base}/organisations/all`)
    );
  }

  createOrganisation(
    request: Omit<Organisation, 'id' | 'createdAt'>
  ): Observable<Organisation> {
    return this.withFallback((base) =>
      this.http.post<Organisation>(`${base}/organisations`, request)
    );
  }

  updateOrganisation(
    id: string,
    request: Omit<Organisation, 'id' | 'createdAt'>
  ): Observable<Organisation> {
    return this.withFallback((base) =>
      this.http.put<Organisation>(
        `${base}/organisations/${encodeURIComponent(id)}`,
        request
      )
    );
  }

  updateOrganisationStatus(
    id: string,
    status: OrganisationStatus
  ): Observable<Organisation> {
    return this.withFallback((base) =>
      this.http.put<Organisation>(
        `${base}/organisations/${encodeURIComponent(id)}/status`,
        { status }
      )
    );
  }

  deleteOrganisation(id: string): Observable<void> {
    return this.withFallback((base) =>
      this.http.delete<void>(`${base}/organisations/${encodeURIComponent(id)}`)
    );
  }

  // ---------- Entity Supervisors (Notification Supervisors) ----------
  getAllNotificationSupervisors(): Observable<NotificationSupervisor[]> {
    return this.withFallback((base) =>
      this.http.get<NotificationSupervisor[]>(`${base}/supervisors/all`)
    );
  }

  createNotificationSupervisor(
    request: Omit<NotificationSupervisor, 'id' | 'createdAt'>
  ): Observable<NotificationSupervisor> {
    const clean = {
      ...request,
      clientRef: this.isUuid(request.clientRef) ? request.clientRef : undefined,
    };
    return this.withFallback((base) =>
      this.http.post<NotificationSupervisor>(`${base}/supervisors`, clean)
    );
  }

  updateNotificationSupervisor(
    id: string,
    request: Omit<NotificationSupervisor, 'id' | 'createdAt'>
  ): Observable<NotificationSupervisor> {
    const clean = {
      ...request,
      clientRef: this.isUuid(request.clientRef) ? request.clientRef : undefined,
    };
    return this.withFallback((base) =>
      this.http.put<NotificationSupervisor>(
        `${base}/supervisors/${encodeURIComponent(id)}`,
        clean
      )
    );
  }

  updateNotificationSupervisorStatus(
    id: string,
    status: SupervisorNotificationStatus
  ): Observable<NotificationSupervisor> {
    return this.withFallback((base) =>
      this.http.put<NotificationSupervisor>(
        `${base}/supervisors/${encodeURIComponent(id)}/status`,
        { status }
      )
    );
  }

  deleteNotificationSupervisor(id: string): Observable<void> {
    return this.withFallback((base) =>
      this.http.delete<void>(`${base}/supervisors/${encodeURIComponent(id)}`)
    );
  }

  // ---------- Notification & OTP Delivery Channels ----------
  getAllChannelConfigs(): Observable<NotificationChannelConfig[]> {
    return this.withFallback((base) =>
      this.http.get<NotificationChannelConfig[]>(`${base}/channel-configs/all`)
    );
  }

  updateChannelConfig(
    channelCode: string,
    payload: {
      active?: boolean;
      otpEnabled?: boolean;
      supervisorAlertEnabled?: boolean;
      description?: string;
    }
  ): Observable<NotificationChannelConfig> {
    return this.withFallback((base) =>
      this.http.put<NotificationChannelConfig>(
        `${base}/channel-configs/${encodeURIComponent(channelCode)}`,
        payload
      )
    );
  }

  // ---------- Authentication (OTP Sign-In) ----------
  requestAuthOtp(request: {
    identifier: string;
    loginMode: 'email' | 'phone';
    channel: 'EMAIL' | 'SMS' | 'WHATSAPP';
  }): Observable<{
    otpReference: string;
    identifier: string;
    maskedIdentifier: string;
    loginMode: 'email' | 'phone';
    channel: 'EMAIL' | 'SMS' | 'WHATSAPP';
    expiresAt: number;
    fullName: string;
    role: string;
    department: string;
    message?: string;
  }> {
    return this.withFallback((base) =>
      this.http.post<{
        otpReference: string;
        identifier: string;
        maskedIdentifier: string;
        loginMode: 'email' | 'phone';
        channel: 'EMAIL' | 'SMS' | 'WHATSAPP';
        expiresAt: number;
        fullName: string;
        role: string;
        department: string;
        message?: string;
      }>(`${base}/auth/request-otp`, request)
    );
  }

  verifyAuthOtp(request: {
    otpReference?: string;
    identifier: string;
    code: string;
  }): Observable<{
    id: string;
    staffId?: string;
    fullName: string;
    initials: string;
    email?: string;
    phone?: string;
    role: string;
    systemRole?: string;
    designation?: string;
    department: string;
    dutyStation?: string;
    organisationId?: string;
    organisationName?: string;
    organisationCode?: string;
    identifier: string;
    loginMode: 'email' | 'phone';
    signedInAt: string;
  }> {
    return this.withFallback((base) =>
      this.http.post<{
        id: string;
        staffId?: string;
        fullName: string;
        initials: string;
        email?: string;
        phone?: string;
        role: string;
        systemRole?: string;
        designation?: string;
        department: string;
        dutyStation?: string;
        organisationId?: string;
        organisationName?: string;
        organisationCode?: string;
        identifier: string;
        loginMode: 'email' | 'phone';
        signedInAt: string;
      }>(`${base}/auth/verify-otp`, request)
    );
  }

  private isUuid(val?: string): boolean {
    if (!val) return false;
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(val.trim());
  }

  private sanitizeOutOfOfficePayload(request: Partial<OutOfOfficeApiDto>): Partial<OutOfOfficeApiDto> {
    return {
      ...request,
      staffMemberId: this.isUuid(request.staffMemberId) ? request.staffMemberId : undefined,
      unitHeadSupervisorId: this.isUuid(request.unitHeadSupervisorId)
        ? request.unitHeadSupervisorId
        : undefined,
      attendingOfficers: (request.attendingOfficers || []).map((ao) => ({
        ...ao,
        id: this.isUuid(ao.id) ? ao.id : undefined,
        staffMemberId: this.isUuid(ao.staffMemberId) ? ao.staffMemberId : undefined,
      })),
    };
  }
}
