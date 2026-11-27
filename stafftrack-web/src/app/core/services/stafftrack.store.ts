import { Injectable, computed, inject, signal } from '@angular/core';
import {
  AccountCategory,
  ActivationStatus,
  ActivityReport,
  BillingPlanStatus,
  ClientBillingPlan,
  ClientUserLink,
  DutyActivity,
  DutyStatus,
  ENGAGEMENT_TYPE_LABELS,
  EngagementType,
  LookupItem,
  ManagementActionItem,
  NotificationChannelConfig,
  NotificationSupervisor,
  Organisation,
  OrganisationStatus,
  OutOfOfficeNotification,
  OutOfOfficeReportBack,
  OutOfOfficeStatus,
  ReportReviewStatus,
  StaffMember,
  SupervisorConfirmationStatus,
  SupervisorNotificationStatus,
  SystemUserAccount,
  UserAccountStatus,
  UserOtpChannel,
  WeeklyConsolidatedReport,
  WeeklyPendingAction,
} from '../models/stafftrack.models';
import {
  DashboardSummaryApiDto,
  MasterLookupApiDto,
  OutOfOfficeApiDto,
  StaffMemberApiDto,
  StaffTrackApiService,
  WeeklyConsolidatedReportApiDto,
  extractApiErrorMessage,
} from './stafftrack-api.service';
import { ConfirmService } from './confirm.service';

const DEFAULT_SEED_ORGANISATION: Organisation = {
  id: 'ORG-OPM-001',
  code: 'OPM',
  name: 'Office of the Prime Minister (OPM)',
  organisationType: 'Government Ministry / Agency',
  contactPerson: 'Kasoma Fredrick',
  email: 'kfredrick35@gmail.com',
  phone: '+256 700 408 387',
  city: 'Kampala',
  country: 'Uganda',
  address: 'Plot 9-11 Apollo Kaggwa Road, Kampala',
  status: 'ACTIVE',
  createdAt: new Date().toISOString().slice(0, 10),
};

const DEFAULT_SEED_USER: SystemUserAccount = {
  id: 'USR-FREDRICK-001',
  fullName: 'Kasoma Fredrick',
  username: 'kfredrick35@gmail.com',
  email: 'kfredrick35@gmail.com',
  phone: '+256 700 408 387',
  designation: 'Principal Officer',
  department: 'Department of Refugees',
  dutyStation: 'DOR-Kampala',
  role: 'SUPER_ADMIN',
  preferredOtpChannel: 'EMAIL',
  status: 'ACTIVE',
  createdAt: new Date().toISOString().slice(0, 10),
};

const DEFAULT_SEED_CLIENT_USER: ClientUserLink = {
  id: 'CU-OPM-FREDRICK-001',
  systemUserId: DEFAULT_SEED_USER.id,
  userPhoneNumber: '+256 700 408 387',
  userEmail: 'kfredrick35@gmail.com',
  accountCategory: 'SUPER_ADMIN',
  clientRef: DEFAULT_SEED_ORGANISATION.id,
  clientName: DEFAULT_SEED_ORGANISATION.name,
  clientCode: DEFAULT_SEED_ORGANISATION.code,
  isDefaultClient: true,
  activationStatus: 'ACTIVE',
  createdAt: new Date().toISOString().slice(0, 10),
};

const DEFAULT_CHANNEL_CONFIGS: NotificationChannelConfig[] = [
  {
    id: 'CHN-EMAIL',
    channelCode: 'EMAIL',
    displayName: 'Email',
    description:
      'Deliver 6-digit login verification codes and official supervisor PDF reports via SMTP email.',
    active: true,
    otpEnabled: true,
    supervisorAlertEnabled: true,
    displayOrder: 1,
    updatedAt: new Date().toISOString().slice(0, 10),
  },
  {
    id: 'CHN-SMS',
    channelCode: 'SMS',
    displayName: 'SMS',
    description:
      'Deliver mobile SMS verification codes and movement alerts (disabled until an SMS gateway is active).',
    active: false,
    otpEnabled: false,
    supervisorAlertEnabled: false,
    displayOrder: 2,
    updatedAt: new Date().toISOString().slice(0, 10),
  },
  {
    id: 'CHN-WHATSAPP',
    channelCode: 'WHATSAPP',
    displayName: 'WhatsApp',
    description:
      'Deliver 6-digit login verification codes and supervisor alerts via the WhatsApp Relay Service.',
    active: true,
    otpEnabled: true,
    supervisorAlertEnabled: true,
    displayOrder: 3,
    updatedAt: new Date().toISOString().slice(0, 10),
  },
];

@Injectable({ providedIn: 'root' })
export class StaffTrackStore {
  private readonly api = inject(StaffTrackApiService);
  private readonly confirm = inject(ConfirmService);

  readonly apiLoading = signal(false);
  readonly apiNotice = signal<{ tone: 'success' | 'error'; text: string } | null>(null);
  readonly dashboardSummary = signal<DashboardSummaryApiDto | null>(null);

  readonly outOfOfficeList = signal<OutOfOfficeNotification[]>([]);
  readonly dutyActivities = signal<DutyActivity[]>([]);
  readonly activityReports = signal<ActivityReport[]>([]);
  readonly weeklyReports = signal<WeeklyConsolidatedReport[]>([]);
  readonly staffMembers = signal<StaffMember[]>([]);

  readonly organisations = signal<Organisation[]>([DEFAULT_SEED_ORGANISATION]);
  readonly departmentItems = signal<LookupItem[]>([]);
  readonly designationItems = signal<LookupItem[]>([]);
  readonly dutyStationItems = signal<LookupItem[]>([]);
  readonly userAccounts = signal<SystemUserAccount[]>([DEFAULT_SEED_USER]);
  readonly clientUserLinks = signal<ClientUserLink[]>([DEFAULT_SEED_CLIENT_USER]);
  readonly billingPlans = signal<ClientBillingPlan[]>([]);
  readonly notificationSupervisors = signal<NotificationSupervisor[]>([]);
  readonly channelConfigs = signal<NotificationChannelConfig[]>(DEFAULT_CHANNEL_CONFIGS);

  readonly activeChannelCount = computed(
    () => this.channelConfigs().filter((c) => c.active).length
  );

  readonly unitHeadSupervisors = computed(() =>
    this.notificationSupervisors().filter(
      (s) => s.status !== 'INACTIVE' && (s.category || 'OVERALL_HEAD') === 'UNIT_HEAD'
    )
  );

  readonly overallHeadSupervisors = computed(() =>
    this.notificationSupervisors().filter(
      (s) => s.status !== 'INACTIVE' && (s.category || 'OVERALL_HEAD') === 'OVERALL_HEAD'
    )
  );

  readonly departments = computed(() => this.departmentItems().map((i) => i.name));
  readonly designations = computed(() => this.designationItems().map((i) => i.name));
  readonly dutyStations = computed(() => this.dutyStationItems().map((i) => i.name));

  constructor() {
    this.restoreUsersFromCache();
    this.refreshFromApi();
  }

  /** Fetches live Departments, Designations, Duty Stations, Staff, and Out-of-Office records from stafftrackapi. */
  refreshFromApi(showNotice = false): void {
    this.apiLoading.set(true);

    this.api.getAllDepartments().subscribe({
      next: (list) => {
        this.departmentItems.set((list || []).map((d) => this.mapLookupDto(d)));
      },
      error: (err) => {
        if (showNotice) {
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        }
      },
    });

    this.api.getAllDesignations().subscribe({
      next: (list) => {
        this.designationItems.set((list || []).map((d) => this.mapLookupDto(d)));
      },
      error: () => {},
    });

    this.api.getAllDutyStations().subscribe({
      next: (list) => {
        this.dutyStationItems.set((list || []).map((s) => this.mapLookupDto(s)));
      },
      error: () => {},
    });

    this.api.getAllStaff().subscribe({
      next: (list) => {
        this.apiLoading.set(false);
        this.staffMembers.set((list || []).map((s) => this.mapStaffDto(s)));
        if (showNotice) {
          this.apiNotice.set({
            tone: 'success',
            text: 'Synchronized with StaffTrack API.',
          });
        }
      },
      error: (err) => {
        this.apiLoading.set(false);
        if (showNotice) {
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        }
      },
    });

    const callerScope = this.resolveCallerOutOfOfficeScope();

    this.api
      .getAllOutOfOffice(
        callerScope.isStaffOfficer
          ? {
              role: 'STAFF_OFFICER',
              staffMemberId: callerScope.staffMemberId,
              officerName: callerScope.officerName,
              callerEmail: callerScope.callerEmail,
              callerPhone: callerScope.callerPhone,
            }
          : undefined
      )
      .subscribe({
        next: (list) => {
          const mapped = (list || []).map((o) => this.mapOooDto(o));
          this.outOfOfficeList.set(
            callerScope.isStaffOfficer
              ? mapped.filter((item) => this.matchesCallerOutOfOfficeScope(item, callerScope))
              : mapped
          );
        },
        error: () => {},
      });

    this.api.getAllWeeklyReports().subscribe({
      next: (list) => {
        this.weeklyReports.set((list || []).map((w) => this.mapWeeklyDto(w)));
      },
      error: () => {},
    });

    this.api.getDashboardSummary().subscribe({
      next: (summary) => {
        this.dashboardSummary.set(summary);
        if (summary?.deployments && !this.resolveCallerOutOfOfficeScope().isStaffOfficer) {
          this.outOfOfficeList.set(summary.deployments.map((o) => this.mapOooDto(o)));
        }
      },
      error: () => {},
    });

    this.api.getAllOrganisations().subscribe({
      next: (orgs) => {
        if (Array.isArray(orgs) && orgs.length > 0) {
          this.organisations.set(orgs);
          this.persistUsersToCache();
        }
      },
      error: () => {},
    });

    this.api.getAllUsers().subscribe({
      next: (users) => {
        if (Array.isArray(users) && users.length > 0) {
          this.userAccounts.set(users);
          this.persistUsersToCache();
        }
      },
      error: () => {},
    });

    this.api.getAllClientUsers().subscribe({
      next: (links) => {
        if (Array.isArray(links) && links.length > 0) {
          this.clientUserLinks.set(links);
          this.persistUsersToCache();
        }
      },
      error: () => {},
    });

    this.api.getAllNotificationSupervisors().subscribe({
      next: (supervisors) => {
        if (Array.isArray(supervisors)) {
          this.notificationSupervisors.set(supervisors);
          this.persistUsersToCache();
        }
      },
      error: () => {},
    });

    this.loadChannelConfigs();
  }

  loadChannelConfigs(): void {
    this.api.getAllChannelConfigs().subscribe({
      next: (configs) => {
        if (Array.isArray(configs) && configs.length > 0) {
          this.channelConfigs.set(configs);
        }
      },
      error: () => {},
    });
  }

  isOtpChannelActive(channelCode: UserOtpChannel): boolean {
    const cfg = this.channelConfigs().find((c) => c.channelCode === channelCode);
    return cfg ? cfg.active && cfg.otpEnabled : channelCode !== 'SMS';
  }

  isSupervisorChannelActive(channelCode: UserOtpChannel): boolean {
    const cfg = this.channelConfigs().find((c) => c.channelCode === channelCode);
    return cfg ? cfg.active && cfg.supervisorAlertEnabled : channelCode !== 'SMS';
  }

  updateChannelConfig(
    channelCode: UserOtpChannel,
    patch: {
      active?: boolean;
      otpEnabled?: boolean;
      supervisorAlertEnabled?: boolean;
      description?: string;
    }
  ): void {
    this.api.updateChannelConfig(channelCode, patch).subscribe({
      next: (updated) => {
        this.channelConfigs.update((list) =>
          list.map((c) => (c.channelCode === updated.channelCode ? updated : c))
        );
        this.apiNotice.set({
          tone: 'success',
          text: `${updated.displayName} channel updated (${updated.active ? 'Active' : 'Disabled'}).`,
        });
      },
      error: (err) => {
        this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
      },
    });
  }

  clearApiNotice(): void {
    this.apiNotice.set(null);
  }

  private resolveCallerOutOfOfficeScope(): {
    isStaffOfficer: boolean;
    staffMemberId?: string;
    officerName?: string;
    callerEmail?: string;
    callerPhone?: string;
  } {
    if (typeof window === 'undefined') {
      return { isStaffOfficer: false };
    }
    try {
      const raw = localStorage.getItem('stafftrack.session.v1');
      if (!raw) return { isStaffOfficer: false };
      const sess = JSON.parse(raw) as {
        id?: string;
        staffId?: string;
        fullName?: string;
        email?: string;
        phone?: string;
        identifier?: string;
        role?: string;
        systemRole?: string;
      };
      const emailLower = (sess.email || sess.identifier || '').trim().toLowerCase();
      const normPhone = this.normalizePhoneDigits(sess.phone || sess.identifier);

      const matchedUser = this.userAccounts().find((u) => {
        if (sess.id && u.id === sess.id) return true;
        if (
          emailLower &&
          (u.email?.toLowerCase() === emailLower || u.username?.toLowerCase() === emailLower)
        ) {
          return true;
        }
        if (normPhone.length > 6 && this.normalizePhoneDigits(u.phone) === normPhone) {
          return true;
        }
        return false;
      });

      let sysRole = matchedUser?.role || '';
      if (!sysRole) {
        const candidate = (sess.systemRole || sess.role || '')
          .trim()
          .toUpperCase()
          .replace(/\s+/g, '_');
        if (
          candidate === 'SUPER_ADMIN' ||
          candidate === 'DEPARTMENT_HEAD' ||
          candidate === 'DESK_OFFICER' ||
          candidate === 'STAFF_OFFICER'
        ) {
          sysRole = candidate;
        } else if (
          emailLower === 'kfredrick35@gmail.com' ||
          emailLower === 'fredrick.kasoma@opm.go.ug' ||
          normPhone === '256700408387' ||
          normPhone === '256772408387'
        ) {
          sysRole = 'SUPER_ADMIN';
        } else {
          sysRole = 'STAFF_OFFICER';
        }
      }

      const matchedStaff = this.staffMembers().find((s) => {
        if (sess.staffId && s.id === sess.staffId) return true;
        if (sess.id && s.id === sess.id) return true;
        if (
          emailLower &&
          (s.email?.toLowerCase() === emailLower ||
            s.alternativeEmail?.toLowerCase() === emailLower)
        ) {
          return true;
        }
        if (
          normPhone.length > 6 &&
          (this.normalizePhoneDigits(s.phone) === normPhone ||
            this.normalizePhoneDigits(s.alternativePhone) === normPhone)
        ) {
          return true;
        }
        return false;
      });

      const staffMemberId =
        sess.staffId || matchedUser?.staffId || matchedStaff?.id || undefined;
      const officerName =
        sess.fullName ||
        matchedUser?.fullName ||
        (matchedStaff ? `${matchedStaff.surname} ${matchedStaff.lastName}` : undefined);

      return {
        isStaffOfficer: sysRole === 'STAFF_OFFICER',
        staffMemberId,
        officerName,
        callerEmail: emailLower || matchedUser?.email?.toLowerCase() || matchedStaff?.email?.toLowerCase(),
        callerPhone: sess.phone || matchedUser?.phone || matchedStaff?.phone,
      };
    } catch {
      return { isStaffOfficer: false };
    }
  }

  private matchesCallerOutOfOfficeScope(
    item: OutOfOfficeNotification,
    scope: {
      isStaffOfficer: boolean;
      staffMemberId?: string;
      officerName?: string;
      callerEmail?: string;
      callerPhone?: string;
    }
  ): boolean {
    if (!scope.isStaffOfficer) return true;
    const targetId = (scope.staffMemberId || '').trim();
    const targetName = (scope.officerName || '').trim();
    const targetEmail = (scope.callerEmail || '').trim().toLowerCase();
    const targetPhone = this.normalizePhoneDigits(scope.callerPhone);

    if (!targetId && !targetName && !targetEmail && !targetPhone) return false;

    if (targetId && item.staffMemberId && item.staffMemberId === targetId) {
      return true;
    }
    if (targetName && this.namesMatchScope(item.officerName, targetName)) {
      return true;
    }
    if (targetName && item.unitHeadName && this.namesMatchScope(item.unitHeadName, targetName)) {
      return true;
    }
    if (targetEmail && item.unitHeadEmail?.trim().toLowerCase() === targetEmail) {
      return true;
    }
    if (targetPhone && this.normalizePhoneDigits(item.unitHeadPhone) === targetPhone) {
      return true;
    }
    return false;
  }

  private namesMatchScope(nameA?: string, nameB?: string): boolean {
    if (!nameA || !nameB) return false;
    const normA = nameA.trim().toLowerCase().replace(/\s+/g, ' ');
    const normB = nameB.trim().toLowerCase().replace(/\s+/g, ' ');
    if (!normA || !normB) return false;
    if (normA === normB) return true;
    const tokensA = normA.split(' ').filter(Boolean).sort().join(' ');
    const tokensB = normB.split(' ').filter(Boolean).sort().join(' ');
    return tokensA.length > 0 && tokensA === tokensB;
  }

  private normalizePhoneDigits(phone?: string): string {
    if (!phone) return '';
    const digits = phone.replace(/[^0-9]/g, '');
    if (digits.startsWith('0') && digits.length === 10) {
      return '256' + digits.slice(1);
    }
    return digits;
  }

  /** Officers currently away, upcoming, or returned on the Dashboard deployment roster */
  readonly awayOrUpcomingOfficers = computed(() => {
    const priority = (status: OutOfOfficeStatus): number => {
      switch (status) {
        case 'ACTIVE_AWAY':
          return 0;
        case 'APPROVED':
          return 1;
        case 'PENDING_APPROVAL':
          return 2;
        case 'RETURNED':
          return 3;
        default:
          return 4;
      }
    };
    return this.outOfOfficeList()
      .filter((item) => item.status !== 'CANCELLED')
      .slice()
      .sort(
        (a, b) =>
          priority(a.status) - priority(b.status) ||
          b.startDate.localeCompare(a.startDate)
      );
  });

  readonly currentlyAwayCount = computed(
    () => this.outOfOfficeList().filter((i) => i.status === 'ACTIVE_AWAY').length
  );

  readonly currentlyInOfficeCount = computed(() => {
    const activeStaff = this.staffMembers().filter((s) => s.status !== 'INACTIVE');
    const awayStaffIds = new Set<string>();
    const awayNames = new Set<string>();

    for (const ooo of this.outOfOfficeList()) {
      if (ooo.status !== 'ACTIVE_AWAY') continue;
      if (ooo.staffMemberId) {
        awayStaffIds.add(ooo.staffMemberId);
      }
      if (ooo.officerName) {
        awayNames.add(ooo.officerName.trim().toLowerCase());
      }
      if (ooo.attendingWithOthers && ooo.attendingOfficers?.length) {
        for (const ao of ooo.attendingOfficers) {
          if (ao.staffMemberId) {
            awayStaffIds.add(ao.staffMemberId);
          }
          if (ao.name) {
            awayNames.add(ao.name.trim().toLowerCase());
          }
        }
      }
    }

    let inOffice = 0;
    for (const s of activeStaff) {
      const full1 = `${s.surname} ${s.lastName}`.trim().toLowerCase();
      const full2 = `${s.lastName} ${s.surname}`.trim().toLowerCase();
      const isAway =
        awayStaffIds.has(s.id) ||
        awayNames.has(full1) ||
        awayNames.has(full2);
      if (!isAway) {
        inOffice += 1;
      }
    }
    return inOffice;
  });

  readonly pendingApprovalCount = computed(
    () => this.outOfOfficeList().filter((i) => i.status === 'PENDING_APPROVAL').length
  );

  readonly engagementTypeSummary = computed(() => {
    const active = this.awayOrUpcomingOfficers();
    const types: EngagementType[] = [
      'TRAVEL',
      'MEETING',
      'WORKSHOP',
      'FIELD_ASSIGNMENT',
      'OFFICIAL_ENGAGEMENT',
    ];
    return types.map((type) => ({
      type,
      label: ENGAGEMENT_TYPE_LABELS[type],
      count: active.filter((a) => a.engagementType === type).length,
    }));
  });

  readonly managementActionsRequired = computed<ManagementActionItem[]>(() => {
    const actions: ManagementActionItem[] = [];

    for (const ooo of this.outOfOfficeList()) {
      if (ooo.status === 'PENDING_APPROVAL') {
        actions.push({
          id: `act-ooo-${ooo.id}`,
          sourceModule: 'OUT_OF_OFFICE',
          sourceId: ooo.id,
          officerName: ooo.officerName,
          department: ooo.department,
          dutyStation: ooo.dutyStation,
          title: `Approve ${ENGAGEMENT_TYPE_LABELS[ooo.engagementType]} to ${ooo.destination}`,
          detail: `${ooo.startDate} → ${ooo.endDate} (${this.computeDurationDays(ooo.startDate, ooo.endDate)} days) · Duty station: ${ooo.dutyStation} · Acting cover: ${ooo.actingOfficer}. ${ooo.managementActionRequired || ooo.purpose}`,
          urgency: 'HIGH',
          actionLabel: 'Approve Travel / Engagement',
        });
      } else if (ooo.status === 'ACTIVE_AWAY' && ooo.managementActionRequired) {
        actions.push({
          id: `act-ooo-note-${ooo.id}`,
          sourceModule: 'OUT_OF_OFFICE',
          sourceId: ooo.id,
          officerName: ooo.officerName,
          department: ooo.department,
          dutyStation: ooo.dutyStation,
          title: `Management Action while Away (${ooo.destination})`,
          detail: ooo.managementActionRequired,
          urgency: 'NORMAL',
          actionLabel: 'Acknowledge & Clear Action',
        });
      }
    }

    return actions;
  });

  computeDurationDays(startDate: string, endDate: string): number {
    const start = new Date(startDate);
    const end = new Date(endDate);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return 1;
    const diff = Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
    return Math.max(1, diff);
  }

  // ---------- Out-of-Office Actions (API-backed) ----------
  loadOutOfOfficeById(
    id: string,
    onLoaded?: (item: OutOfOfficeNotification) => void
  ): void {
    const cached = this.outOfOfficeList().find((i) => i.id === id);
    if (cached && onLoaded) {
      onLoaded(cached);
    }
    this.api.getOutOfOfficeById(id).subscribe({
      next: (dto) => {
        const mapped = this.mapOooDto(dto);
        this.outOfOfficeList.update((list) => {
          const exists = list.some((i) => i.id === mapped.id);
          return exists
            ? list.map((i) => (i.id === mapped.id ? mapped : i))
            : [mapped, ...list];
        });
        onLoaded?.(mapped);
      },
      error: (err) => {
        this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
      },
    });
  }

  saveOutOfOffice(
    payload: Omit<OutOfOfficeNotification, 'id' | 'createdAt'>,
    existingId?: string,
    onSuccess?: (saved: OutOfOfficeNotification) => void,
    onError?: (message: string) => void
  ): void {
    const call = existingId
      ? this.api.updateOutOfOffice(existingId, payload)
      : this.api.recordOutOfOffice(payload);

    call.subscribe({
      next: (saved) => {
        const mapped = this.mapOooDto(saved);
        if (existingId) {
          this.outOfOfficeList.update((list) =>
            list.map((item) => (item.id === existingId ? mapped : item))
          );
          this.apiNotice.set({
            tone: 'success',
            text: `Updated Out-of-Office notification ${mapped.referenceNumber || ''} for ${mapped.officerName}.`,
          });
        } else {
          this.outOfOfficeList.update((list) => [mapped, ...list]);
          this.apiNotice.set({
            tone: 'success',
            text: `Recorded Out-of-Office notification ${mapped.referenceNumber || ''} for ${mapped.officerName}.`,
          });
        }
        onSuccess?.(mapped);
      },
      error: (err) => {
        const msg = extractApiErrorMessage(err);
        this.apiNotice.set({ tone: 'error', text: msg });
        onError?.(msg);
      },
    });
  }

  updateOutOfOfficeStatus(id: string, status: OutOfOfficeStatus): void {
    this.api.updateOutOfOfficeStatus(id, status).subscribe({
      next: (saved) => {
        const mapped = this.mapOooDto(saved);
        this.outOfOfficeList.update((list) =>
          list.map((item) => (item.id === id ? mapped : item))
        );
        this.apiNotice.set({
          tone: 'success',
          text: `Updated status for ${mapped.officerName} to ${mapped.status}.`,
        });
      },
      error: (err) => {
        this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
      },
    });
  }

  reviewOutOfOfficeByUnitHead(
    id: string,
    decision: 'ACCEPTED' | 'REJECTED',
    reviewedBy = 'Unit Head',
    comment?: string,
    onSuccess?: (saved: OutOfOfficeNotification) => void,
    onError?: (message: string) => void
  ): void {
    this.api
      .submitUnitHeadDecision(id, {
        decision,
        reviewedBy,
        comment: comment?.trim() || undefined,
      })
      .subscribe({
        next: (saved) => {
          const mapped = this.mapOooDto(saved);
          this.outOfOfficeList.update((list) =>
            list.map((item) => (item.id === id ? mapped : item))
          );
          this.apiNotice.set({
            tone: 'success',
            text:
              decision === 'ACCEPTED'
                ? `Accepted Out-of-Office notification ${mapped.referenceNumber || ''} and notified Overall Head.`
                : `Rejected Out-of-Office notification ${mapped.referenceNumber || ''}.`,
          });
          onSuccess?.(mapped);
        },
        error: (err) => {
          const msg = extractApiErrorMessage(err);
          this.apiNotice.set({ tone: 'error', text: msg });
          onError?.(msg);
        },
      });
  }

  updateOutOfOfficeSupervisorConfirmation(
    id: string,
    confirmationStatus: SupervisorConfirmationStatus,
    confirmedBy = 'Supervisor',
    silent = false
  ): void {
    const applyLocal = (status: SupervisorConfirmationStatus) => {
      const seenAt = status === 'SEEN' ? new Date().toISOString() : undefined;
      this.outOfOfficeList.update((list) =>
        list.map((item) =>
          item.id === id
            ? {
                ...item,
                supervisorConfirmationStatus: status,
                supervisorSeenAt: seenAt,
                supervisorConfirmedBy: status === 'SEEN' ? confirmedBy : undefined,
              }
            : item
        )
      );
      if (!silent) {
        this.apiNotice.set({
          tone: 'success',
          text: `Supervisor confirmation updated to ${status === 'SEEN' ? 'Seen' : 'New'}.`,
        });
      }
    };

    this.api
      .updateOutOfOfficeSupervisorConfirmation(id, confirmationStatus, confirmedBy)
      .subscribe({
        next: (saved) => {
          const mapped = this.mapOooDto(saved);
          this.outOfOfficeList.update((list) =>
            list.map((item) => (item.id === id ? mapped : item))
          );
          if (!silent) {
            this.apiNotice.set({
              tone: 'success',
              text: `Supervisor confirmation for ${mapped.referenceNumber || mapped.officerName} marked as ${
                mapped.supervisorConfirmationStatus === 'SEEN' ? 'Seen' : 'New'
              }.`,
            });
          }
        },
        error: (err) => {
          if (err?.status === 404 || err?.status === 0) {
            applyLocal(confirmationStatus);
            return;
          }
          if (!silent) {
            this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
          }
        },
      });
  }

  markNotificationAsRead(id: string, confirmedBy = 'Supervisor', silent = true): void {
    const item = this.outOfOfficeList().find((i) => i.id === id);
    if (item && (item.supervisorConfirmationStatus || 'NEW') === 'SEEN') {
      return;
    }
    this.updateOutOfOfficeSupervisorConfirmation(id, 'SEEN', confirmedBy, silent);
  }

  deleteOutOfOffice(id: string): void {
    const target = this.outOfOfficeList().find((i) => i.id === id);
    const label = target
      ? `Out-of-Office notification for ${target.officerName} (${target.destination})`
      : 'this Out-of-Office notification';
    this.confirm.ask({
      title: 'Delete Out-of-Office Notification',
      message: `Are you sure you want to delete ${label}?`,
      onConfirm: () => {
        this.api.deleteOutOfOffice(id).subscribe({
          next: () => {
            this.outOfOfficeList.update((list) => list.filter((i) => i.id !== id));
            this.apiNotice.set({
              tone: 'success',
              text: 'Out-of-Office notification deleted.',
            });
          },
          error: (err) => {
            if (err?.status === 404) {
              this.outOfOfficeList.update((list) => list.filter((i) => i.id !== id));
              this.apiNotice.set({
                tone: 'success',
                text: 'Out-of-Office notification deleted.',
              });
              return;
            }
            this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
          },
        });
      },
    });
  }

  submitOutOfOfficeReportBack(
    id: string,
    reportBack: OutOfOfficeReportBack,
    onSuccess?: (saved: OutOfOfficeNotification) => void,
    onError?: (message: string) => void
  ): void {
    this.api.submitReportBack(id, reportBack).subscribe({
      next: (saved) => {
        const mapped = this.mapOooDto(saved);
        this.outOfOfficeList.update((list) =>
          list.map((item) => (item.id === id ? mapped : item))
        );
        this.apiNotice.set({
          tone: 'success',
          text: `Saved Report Back & Management Actions for ${mapped.officerName} (${mapped.destination}).`,
        });
        onSuccess?.(mapped);
      },
      error: (err) => {
        const msg = extractApiErrorMessage(err);
        this.apiNotice.set({ tone: 'error', text: msg });
        onError?.(msg);
      },
    });
  }

  // ---------- Duty Activity Actions ----------
  saveDutyActivity(payload: Omit<DutyActivity, 'id'>, existingId?: string): void {
    if (existingId) {
      this.dutyActivities.update((list) =>
        list.map((item) => (item.id === existingId ? { ...item, ...payload } : item))
      );
    } else {
      const created: DutyActivity = {
        ...payload,
        id: 'DUTY-' + Date.now().toString(36).toUpperCase(),
      };
      this.dutyActivities.update((list) => [created, ...list]);
    }
  }

  updateDutyProgress(id: string, progressPercent: number, status: DutyStatus): void {
    this.dutyActivities.update((list) =>
      list.map((item) => (item.id === id ? { ...item, progressPercent, status } : item))
    );
  }

  deleteDutyActivity(id: string): void {
    const target = this.dutyActivities().find((i) => i.id === id);
    this.confirm.ask({
      title: 'Delete Duty Activity',
      message: `Are you sure you want to delete "${target?.activityTitle || id}"?`,
      onConfirm: () => {
        this.dutyActivities.update((list) => list.filter((i) => i.id !== id));
      },
    });
  }

  // ---------- Activity Report Actions ----------
  saveActivityReport(payload: Omit<ActivityReport, 'id'>, existingId?: string): void {
    if (existingId) {
      this.activityReports.update((list) =>
        list.map((item) => (item.id === existingId ? { ...item, ...payload } : item))
      );
    } else {
      const created: ActivityReport = {
        ...payload,
        id: 'REP-' + Date.now().toString(36).toUpperCase(),
      };
      this.activityReports.update((list) => [created, ...list]);
    }
  }

  updateReportReviewStatus(id: string, reviewStatus: ReportReviewStatus): void {
    this.activityReports.update((list) =>
      list.map((item) =>
        item.id === id
          ? {
              ...item,
              reviewStatus,
              requiresManagementIntervention:
                reviewStatus === 'REVIEWED' ? false : item.requiresManagementIntervention,
            }
          : item
      )
    );
  }

  deleteActivityReport(id: string): void {
    const target = this.activityReports().find((i) => i.id === id);
    this.confirm.ask({
      title: 'Delete Activity Report',
      message: `Are you sure you want to delete "${target?.title || id}"?`,
      onConfirm: () => {
        this.activityReports.update((list) => list.filter((i) => i.id !== id));
      },
    });
  }

  // ---------- Weekly Consolidation Engine (API-backed) ----------
  consolidateWeeklyReport(
    weekStart: string,
    weekEnd: string,
    weekLabel: string,
    departmentFilter: string,
    generatedBy: string,
    generatedByDesignation = '',
    dutyStation = ''
  ): void {
    this.api
      .consolidateWeeklyReport({
        weekLabel,
        weekStart,
        weekEnd,
        departmentFilter,
        dutyStation,
        generatedBy,
        generatedByDesignation,
      })
      .subscribe({
        next: (saved) => {
          const mapped = this.mapWeeklyDto(saved);
          this.weeklyReports.update((list) => [mapped, ...list]);
          this.apiNotice.set({
            tone: 'success',
            text: `Consolidated weekly report "${mapped.weekLabel}" (${mapped.activitiesUndertaken.length} activities, ${mapped.pendingActions.length} pending actions).`,
          });
        },
        error: (err) => {
          if (err?.status === 404) {
            const local = this.consolidateWeeklyReportFromLoadedApiData(
              weekStart,
              weekEnd,
              weekLabel,
              departmentFilter,
              generatedBy,
              generatedByDesignation,
              dutyStation
            );
            this.weeklyReports.update((list) => [local, ...list]);
            this.apiNotice.set({
              tone: 'success',
              text: `Consolidated weekly report "${local.weekLabel}" (${local.activitiesUndertaken.length} activities, ${local.pendingActions.length} pending actions).`,
            });
            return;
          }
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        },
      });
  }

  private consolidateWeeklyReportFromLoadedApiData(
    weekStart: string,
    weekEnd: string,
    weekLabel: string,
    departmentFilter: string,
    generatedBy: string,
    generatedByDesignation = '',
    dutyStation = ''
  ): WeeklyConsolidatedReport {
    const matchesDept = (dept: string) =>
      !departmentFilter || departmentFilter === 'ALL' || dept === departmentFilter;

    const ooo = this.outOfOfficeList().filter(
      (o) =>
        matchesDept(o.department) &&
        o.startDate <= weekEnd &&
        o.endDate >= weekStart &&
        o.status !== 'CANCELLED'
    );

    const activitiesUndertaken: WeeklyConsolidatedReport['activitiesUndertaken'] = ooo.map(
      (o) => ({
        date: o.startDate,
        officerName: o.officerName,
        officerDesignation: o.officerRole,
        department: o.department,
        dutyStation: o.dutyStation,
        category: ENGAGEMENT_TYPE_LABELS[o.engagementType],
        summary: `${o.destination} (${o.hostOrganization}) – ${o.purpose}`,
      })
    );

    const pendingActions: WeeklyPendingAction[] = ooo.flatMap((o) =>
      (o.reportBack?.managementActions || [])
        .filter((ma) => ma.followUpAction.trim().length > 0)
        .map((ma) => ({
          item: ma.followUpAction,
          source: `Report Back (${o.destination})`,
          responsibleOfficer: ma.responsibleOfficer || o.officerName,
          department: o.department,
          dutyStation: o.dutyStation,
          targetDate: ma.targetDate || weekEnd,
          status: 'Management Action',
        }))
    );

    const deptTitle =
      !departmentFilter || departmentFilter === 'ALL' ? 'All Departments' : departmentFilter;

    return {
      id: 'WKR-' + Date.now().toString(36).toUpperCase(),
      weekLabel,
      weekStart,
      weekEnd,
      departmentFilter: deptTitle,
      dutyStation,
      generatedBy,
      generatedByDesignation,
      generatedAt: new Date().toISOString().slice(0, 10),
      executiveSummary: '',
      activitiesUndertaken,
      keyOutcomes: [],
      pendingActions,
    };
  }

  deleteWeeklyReport(id: string): void {
    const target = this.weeklyReports().find((w) => w.id === id);
    this.confirm.ask({
      title: 'Delete Weekly Report',
      message: `Are you sure you want to delete "${target?.weekLabel || id}"?`,
      onConfirm: () => {
        this.api.deleteWeeklyReport(id).subscribe({
          next: () => {
            this.weeklyReports.update((list) => list.filter((w) => w.id !== id));
            this.apiNotice.set({
              tone: 'success',
              text: 'Weekly Consolidated Report deleted.',
            });
          },
          error: (err) => {
            if (err?.status === 404) {
              this.weeklyReports.update((list) => list.filter((w) => w.id !== id));
              return;
            }
            this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
          },
        });
      },
    });
  }

  // ---------- Staff Register & Master Lookups (Strictly API-driven) ----------
  saveStaffMember(
    payload: Omit<StaffMember, 'id' | 'createdAt'>,
    existingId?: string
  ): void {
    const matchedDept = this.departmentItems().find((d) => d.name === payload.department);
    const matchedDesig = this.designationItems().find((d) => d.name === payload.designation);
    const matchedStation = this.dutyStationItems().find((s) => s.name === payload.dutyStation);

    const requestBody = {
      surname: payload.surname.trim(),
      lastName: payload.lastName.trim(),
      designation: payload.designation.trim(),
      designationId: matchedDesig?.id,
      email: payload.email.trim(),
      alternativeEmail: payload.alternativeEmail?.trim() || undefined,
      phone: payload.phone.trim(),
      alternativePhone: payload.alternativePhone?.trim() || undefined,
      department: payload.department.trim(),
      departmentId: matchedDept?.id,
      dutyStation: payload.dutyStation.trim(),
      dutyStationId: matchedStation?.id,
      status: payload.status || 'ACTIVE',
    };

    if (existingId) {
      this.api.updateStaff(existingId, requestBody).subscribe({
        next: (saved) => {
          const mapped = this.mapStaffDto(saved);
          this.staffMembers.update((list) =>
            list.map((s) => (s.id === existingId ? mapped : s))
          );
          this.apiNotice.set({
            tone: 'success',
            text: `Updated staff profile for ${mapped.surname} ${mapped.lastName}.`,
          });
        },
        error: (err) => {
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        },
      });
    } else {
      this.api.registerStaff(requestBody).subscribe({
        next: (saved) => {
          const mapped = this.mapStaffDto(saved);
          this.staffMembers.update((list) => [mapped, ...list]);
          this.apiNotice.set({
            tone: 'success',
            text: `Registered staff member ${mapped.surname} ${mapped.lastName} (${mapped.staffCode || mapped.id}).`,
          });
        },
        error: (err) => {
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        },
      });
    }
  }

  deleteStaffMember(id: string): void {
    const target = this.staffMembers().find((s) => s.id === id);
    const fullName = target ? `${target.surname} ${target.lastName}` : id;
    this.confirm.ask({
      title: 'Delete Staff Member',
      message: `Are you sure you want to delete staff member "${fullName}" from the directory?`,
      onConfirm: () => {
        this.api.deleteStaff(id).subscribe({
          next: () => {
            this.staffMembers.update((list) => list.filter((s) => s.id !== id));
            this.apiNotice.set({ tone: 'success', text: 'Staff member removed from directory.' });
          },
          error: (err) => {
            if (err?.status === 404) {
              this.staffMembers.update((list) => list.filter((s) => s.id !== id));
              this.apiNotice.set({ tone: 'success', text: 'Staff member removed from directory.' });
              return;
            }
            this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
          },
        });
      },
    });
  }

  saveLookupItem(
    kind: 'DEPARTMENT' | 'DESIGNATION' | 'STATION',
    payload: { code: string; name: string },
    existingId?: string
  ): void {
    const cleanCode = payload.code.trim().toUpperCase();
    const cleanName = payload.name.trim();
    if (!cleanCode || !cleanName) return;

    const targetSignal =
      kind === 'DEPARTMENT'
        ? this.departmentItems
        : kind === 'DESIGNATION'
        ? this.designationItems
        : this.dutyStationItems;
    const label =
      kind === 'DEPARTMENT'
        ? 'Department / Unit'
        : kind === 'DESIGNATION'
        ? 'Officer Designation'
        : 'Duty Station';

    const req = { code: cleanCode, name: cleanName };

    if (existingId) {
      const updateCall =
        kind === 'DEPARTMENT'
          ? this.api.updateDepartment(existingId, req)
          : kind === 'DESIGNATION'
          ? this.api.updateDesignation(existingId, req)
          : this.api.updateDutyStation(existingId, req);

      updateCall.subscribe({
        next: (saved) => {
          const mapped = this.mapLookupDto(saved);
          targetSignal.update((list) =>
            list.map((item) => (item.id === existingId ? mapped : item))
          );
          this.apiNotice.set({
            tone: 'success',
            text: `Updated ${label}: ${mapped.code} — ${mapped.name}.`,
          });
        },
        error: (err) => {
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        },
      });
    } else {
      const createCall =
        kind === 'DEPARTMENT'
          ? this.api.createDepartment(req)
          : kind === 'DESIGNATION'
          ? this.api.createDesignation(req)
          : this.api.createDutyStation(req);

      createCall.subscribe({
        next: (saved) => {
          const mapped = this.mapLookupDto(saved);
          targetSignal.update((list) => [...list, mapped]);
          this.apiNotice.set({
            tone: 'success',
            text: `Added ${label}: ${mapped.code} — ${mapped.name}.`,
          });
        },
        error: (err) => {
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        },
      });
    }
  }

  deleteLookupItem(kind: 'DEPARTMENT' | 'DESIGNATION' | 'STATION', id: string): void {
    const targetSignal =
      kind === 'DEPARTMENT'
        ? this.departmentItems
        : kind === 'DESIGNATION'
        ? this.designationItems
        : this.dutyStationItems;
    const kindLabel =
      kind === 'DEPARTMENT'
        ? 'Department / Unit'
        : kind === 'DESIGNATION'
        ? 'Officer Designation'
        : 'Duty Station';
    const item = targetSignal().find((i) => i.id === id);
    const itemName = item ? `${item.code} — ${item.name}` : id;

    this.confirm.ask({
      title: `Delete ${kindLabel}`,
      message: `Are you sure you want to delete ${kindLabel} "${itemName}"?`,
      onConfirm: () => {
        const deleteCall =
          kind === 'DEPARTMENT'
            ? this.api.deleteDepartment(id)
            : kind === 'DESIGNATION'
            ? this.api.deleteDesignation(id)
            : this.api.deleteDutyStation(id);

        deleteCall.subscribe({
          next: () => {
            targetSignal.update((list) => list.filter((i) => i.id !== id));
            this.apiNotice.set({ tone: 'success', text: `${kindLabel} removed.` });
          },
          error: (err) => {
            if (err?.status === 404) {
              targetSignal.update((list) => list.filter((i) => i.id !== id));
              this.apiNotice.set({ tone: 'success', text: `${kindLabel} removed.` });
              return;
            }
            this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
          },
        });
      },
    });
  }

  addDepartment(name: string): void {
    const clean = name.trim();
    if (!clean || this.departments().includes(clean)) return;
    const code = clean
      .split(/\s+/)
      .map((w) => w[0]?.toUpperCase() || '')
      .join('')
      .slice(0, 6);
    this.saveLookupItem('DEPARTMENT', { code: code || 'DEPT', name: clean });
  }

  addDesignation(name: string): void {
    const clean = name.trim();
    if (!clean || this.designations().includes(clean)) return;
    const code = clean
      .split(/\s+/)
      .map((w) => w[0]?.toUpperCase() || '')
      .join('')
      .slice(0, 6);
    this.saveLookupItem('DESIGNATION', { code: code || 'DES', name: clean });
  }

  addDutyStation(name: string): void {
    const clean = name.trim();
    if (!clean || this.dutyStations().includes(clean)) return;
    const code = clean
      .split(/[\s\-]+/)
      .map((w) => w.slice(0, 3).toUpperCase())
      .join('-')
      .slice(0, 8);
    this.saveLookupItem('STATION', { code: code || 'STN', name: clean });
  }

  // ---------- User Management Actions ----------
  saveUserAccount(
    payload: Omit<SystemUserAccount, 'id' | 'createdAt'>,
    existingId?: string
  ): void {
    const applyLocalSave = (savedUser: SystemUserAccount) => {
      if (existingId) {
        this.userAccounts.update((list) =>
          list.map((u) => (u.id === existingId ? savedUser : u))
        );
        this.apiNotice.set({
          tone: 'success',
          text: `Updated user assignment for ${savedUser.fullName} (${savedUser.role}).`,
        });
      } else {
        this.userAccounts.update((list) => [savedUser, ...list]);
        this.apiNotice.set({
          tone: 'success',
          text: `Assigned ${savedUser.fullName} to User Management as ${savedUser.role}.`,
        });
      }
      this.ensureLocalClientUserLink(savedUser);
      this.persistUsersToCache();
    };

    if (existingId) {
      this.api.updateUser(existingId, payload).subscribe({
        next: (saved) => applyLocalSave(saved),
        error: (err) => {
          if (err?.status === 404) {
            const current = this.userAccounts().find((u) => u.id === existingId);
            applyLocalSave({
              ...payload,
              id: existingId,
              createdAt: current?.createdAt || new Date().toISOString().slice(0, 10),
            });
            return;
          }
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        },
      });
    } else {
      this.api.createUser(payload).subscribe({
        next: (saved) => {
          applyLocalSave(saved);
          this.api.getAllClientUsers().subscribe({
            next: (links) => {
              if (Array.isArray(links) && links.length > 0) {
                this.clientUserLinks.set(links);
                this.persistUsersToCache();
              }
            },
            error: () => {},
          });
        },
        error: (err) => {
          if (err?.status === 404) {
            applyLocalSave({
              ...payload,
              id: 'USR-' + Date.now().toString(36).toUpperCase(),
              createdAt: new Date().toISOString().slice(0, 10),
            });
            return;
          }
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        },
      });
    }
  }

  private ensureLocalClientUserLink(user: SystemUserAccount): void {
    const existingForPerson = this.clientUserLinks().filter(
      (cu) =>
        (user.email && cu.userEmail?.toLowerCase() === user.email.toLowerCase()) ||
        (user.phone && cu.userPhoneNumber === user.phone)
    );
    const alreadyInDept = existingForPerson.some(
      (cu) => cu.clientName.toLowerCase() === user.department.toLowerCase()
    );
    if (alreadyInDept) return;

    const matchedDept = this.departmentItems().find(
      (d) => d.name.toLowerCase() === user.department.toLowerCase()
    );
    const category: AccountCategory =
      user.role === 'SUPER_ADMIN'
        ? 'SUPER_ADMIN'
        : user.role === 'DEPARTMENT_HEAD' || user.role === 'DESK_OFFICER'
        ? 'CLIENT_ADMIN'
        : 'USER';

    const link: ClientUserLink = {
      id: 'CU-' + Date.now().toString(36).toUpperCase(),
      systemUserId: user.id,
      userPhoneNumber: user.phone,
      userEmail: user.email,
      accountCategory: category,
      clientRef: matchedDept?.id || matchedDept?.code || 'OPM-DEPT',
      clientName: user.department,
      clientCode: matchedDept?.code || 'OPM',
      isDefaultClient: existingForPerson.length === 0,
      activationStatus: user.status,
      createdAt: user.createdAt || new Date().toISOString().slice(0, 10),
    };
    this.clientUserLinks.update((list) => [link, ...list]);
  }

  updateUserAccountStatus(id: string, status: UserAccountStatus): void {
    this.api.updateUserStatus(id, status).subscribe({
      next: (saved) => {
        this.userAccounts.update((list) =>
          list.map((u) => (u.id === id ? saved : u))
        );
        this.persistUsersToCache();
      },
      error: () => {
        this.userAccounts.update((list) =>
          list.map((u) => (u.id === id ? { ...u, status } : u))
        );
        this.persistUsersToCache();
      },
    });
  }

  deleteUserAccount(id: string): void {
    const target = this.userAccounts().find((u) => u.id === id);
    this.confirm.ask({
      title: 'Delete User Account',
      message: `Are you sure you want to delete user account "${target?.fullName || target?.email || id}"?`,
      onConfirm: () => {
        const removeLocally = () => {
          this.userAccounts.update((list) => list.filter((u) => u.id !== id));
          this.clientUserLinks.update((list) =>
            list.filter(
              (cu) =>
                cu.systemUserId !== id &&
                (!target?.email || cu.userEmail?.toLowerCase() !== target.email.toLowerCase())
            )
          );
          this.persistUsersToCache();
          this.apiNotice.set({ tone: 'success', text: 'User removed from User Management.' });
        };

        this.api.deleteUser(id).subscribe({
          next: () => removeLocally(),
          error: (err) => {
            if (err?.status === 404) {
              removeLocally();
              return;
            }
            this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
          },
        });
      },
    });
  }

  // ---------- Client Users Actions ----------
  saveClientUserLink(payload: Omit<ClientUserLink, 'id' | 'createdAt'>): void {
    const applyLocal = (saved: ClientUserLink) => {
      this.clientUserLinks.update((list) => {
        const updated = payload.isDefaultClient
          ? list.map((cu) => {
              const samePerson =
                (saved.userEmail && cu.userEmail?.toLowerCase() === saved.userEmail.toLowerCase()) ||
                (saved.userPhoneNumber && cu.userPhoneNumber === saved.userPhoneNumber);
              return samePerson ? { ...cu, isDefaultClient: false } : cu;
            })
          : list;
        return [saved, ...updated];
      });
      this.persistUsersToCache();
      this.apiNotice.set({
        tone: 'success',
        text: `Assigned ${saved.userEmail || saved.userPhoneNumber} to ${saved.clientName} (${saved.accountCategory}).`,
      });
    };

    this.api.createClientUser(payload).subscribe({
      next: (saved) => applyLocal(saved),
      error: (err) => {
        if (err?.status === 404) {
          applyLocal({
            ...payload,
            id: 'CU-' + Date.now().toString(36).toUpperCase(),
            createdAt: new Date().toISOString().slice(0, 10),
          });
          return;
        }
        this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
      },
    });
  }

  updateClientUserStatus(id: string, activationStatus: ActivationStatus): void {
    this.api.updateClientUserStatus(id, activationStatus).subscribe({
      next: (saved) => {
        this.clientUserLinks.update((list) =>
          list.map((cu) => (cu.id === id ? saved : cu))
        );
        this.persistUsersToCache();
      },
      error: () => {
        this.clientUserLinks.update((list) =>
          list.map((cu) => (cu.id === id ? { ...cu, activationStatus } : cu))
        );
        this.persistUsersToCache();
      },
    });
  }

  makeDefaultClientUser(id: string): void {
    const target = this.clientUserLinks().find((cu) => cu.id === id);
    if (!target) return;
    const applyLocalDefault = () => {
      this.clientUserLinks.update((list) =>
        list.map((cu) => {
          const samePerson =
            (target.userEmail && cu.userEmail === target.userEmail) ||
            (target.userPhoneNumber && cu.userPhoneNumber === target.userPhoneNumber);
          if (!samePerson) return cu;
          return { ...cu, isDefaultClient: cu.id === id };
        })
      );
      this.persistUsersToCache();
    };

    this.api.makeDefaultClientUser(id).subscribe({
      next: () => applyLocalDefault(),
      error: () => applyLocalDefault(),
    });
  }

  deleteClientUserLink(id: string): void {
    const target = this.clientUserLinks().find((cu) => cu.id === id);
    const label = target
      ? `${target.userEmail || target.userPhoneNumber} → ${target.clientName}`
      : id;
    this.confirm.ask({
      title: 'Remove Client / Department Assignment',
      message: `Are you sure you want to remove assignment "${label}"?`,
      onConfirm: () => {
        const removeLocal = () => {
          this.clientUserLinks.update((list) => list.filter((cu) => cu.id !== id));
          this.persistUsersToCache();
          this.apiNotice.set({ tone: 'success', text: 'Client user assignment removed.' });
        };
        this.api.deleteClientUser(id).subscribe({
          next: () => removeLocal(),
          error: () => removeLocal(),
        });
      },
    });
  }

  // ---------- Organisations Actions ----------
  saveOrganisation(
    payload: Omit<Organisation, 'id' | 'createdAt'>,
    existingId?: string,
    assignContactAsAdmin = false
  ): void {
    const cleanPayload: Omit<Organisation, 'id' | 'createdAt'> = {
      ...payload,
      code: payload.code.trim().toUpperCase(),
      name: payload.name.trim(),
    };

    const applyLocal = (saved: Organisation) => {
      if (existingId) {
        this.organisations.update((list) =>
          list.map((o) => (o.id === existingId ? saved : o))
        );
        this.apiNotice.set({
          tone: 'success',
          text: `Updated organisation ${saved.name} (${saved.code}).`,
        });
      } else {
        this.organisations.update((list) => [...list, saved]);
        this.apiNotice.set({
          tone: 'success',
          text: `Registered organisation ${saved.name} (${saved.code}).`,
        });
      }

      if (assignContactAsAdmin && (saved.email || saved.phone)) {
        const alreadyLinked = this.clientUserLinks().some(
          (cu) =>
            cu.clientCode.toLowerCase() === saved.code.toLowerCase() &&
            ((saved.email && cu.userEmail?.toLowerCase() === saved.email.toLowerCase()) ||
              (saved.phone && cu.userPhoneNumber === saved.phone))
        );
        if (!alreadyLinked) {
          this.saveClientUserLink({
            userEmail: saved.email,
            userPhoneNumber: saved.phone,
            accountCategory: 'CLIENT_ADMIN',
            clientRef: saved.id,
            clientName: saved.name,
            clientCode: saved.code,
            isDefaultClient: false,
            activationStatus: 'ACTIVE',
          });
        }
      }
      this.persistUsersToCache();
    };

    if (existingId) {
      this.api.updateOrganisation(existingId, cleanPayload).subscribe({
        next: (saved) => applyLocal(saved),
        error: (err) => {
          if (err?.status === 404) {
            const current = this.organisations().find((o) => o.id === existingId);
            applyLocal({
              ...cleanPayload,
              id: existingId,
              createdAt: current?.createdAt || new Date().toISOString().slice(0, 10),
            });
            return;
          }
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        },
      });
    } else {
      this.api.createOrganisation(cleanPayload).subscribe({
        next: (saved) => applyLocal(saved),
        error: (err) => {
          if (err?.status === 404) {
            applyLocal({
              ...cleanPayload,
              id: 'ORG-' + Date.now().toString(36).toUpperCase(),
              createdAt: new Date().toISOString().slice(0, 10),
            });
            return;
          }
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        },
      });
    }
  }

  updateOrganisationStatus(id: string, status: OrganisationStatus): void {
    this.api.updateOrganisationStatus(id, status).subscribe({
      next: (saved) => {
        this.organisations.update((list) =>
          list.map((o) => (o.id === id ? saved : o))
        );
        this.persistUsersToCache();
      },
      error: () => {
        this.organisations.update((list) =>
          list.map((o) => (o.id === id ? { ...o, status } : o))
        );
        this.persistUsersToCache();
      },
    });
  }

  deleteOrganisation(id: string): void {
    const target = this.organisations().find((o) => o.id === id);
    this.confirm.ask({
      title: 'Delete Organisation',
      message: `Are you sure you want to delete organisation "${target ? `${target.name} (${target.code})` : id}"?`,
      onConfirm: () => {
        const removeLocal = () => {
          this.organisations.update((list) => list.filter((o) => o.id !== id));
          this.persistUsersToCache();
          this.apiNotice.set({ tone: 'success', text: 'Organisation deleted.' });
        };
        this.api.deleteOrganisation(id).subscribe({
          next: () => removeLocal(),
          error: (err) => {
            if (err?.status === 404) {
              removeLocal();
              return;
            }
            this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
          },
        });
      },
    });
  }

  recordUserLogin(identifier: string): void {
    const today = new Date().toISOString().slice(0, 10);
    const lower = identifier.trim().toLowerCase();
    const normPhone = identifier.replace(/[^0-9]/g, '');
    this.userAccounts.update((list) =>
      list.map((u) => {
        const emailMatch = u.email.toLowerCase() === lower || u.username.toLowerCase() === lower;
        const phoneMatch = normPhone.length > 6 && u.phone.replace(/[^0-9]/g, '').endsWith(normPhone.slice(-9));
        return emailMatch || phoneMatch ? { ...u, lastLoginAt: today } : u;
      })
    );
    this.persistUsersToCache();
  }

  private restoreUsersFromCache(): void {
    if (typeof window === 'undefined') return;
    try {
      const rawOrgs = localStorage.getItem('stafftrack.organisations.v1');
      if (rawOrgs) {
        const parsedOrgs = JSON.parse(rawOrgs);
        if (Array.isArray(parsedOrgs) && parsedOrgs.length > 0) {
          this.organisations.set(parsedOrgs);
        }
      }
      const rawUsers = localStorage.getItem('stafftrack.users.v1');
      if (rawUsers) {
        const parsed = JSON.parse(rawUsers);
        if (Array.isArray(parsed) && parsed.length > 0) {
          this.userAccounts.set(parsed);
        }
      }
      const rawClientUsers = localStorage.getItem('stafftrack.client_users.v1');
      if (rawClientUsers) {
        const parsedCu = JSON.parse(rawClientUsers);
        if (Array.isArray(parsedCu) && parsedCu.length > 0) {
          this.clientUserLinks.set(parsedCu);
        }
      }
      const rawSupervisors = localStorage.getItem('stafftrack.notification_supervisors.v1');
      if (rawSupervisors) {
        const parsedSup = JSON.parse(rawSupervisors);
        if (Array.isArray(parsedSup)) {
          this.notificationSupervisors.set(parsedSup);
        }
      }
    } catch {}
  }

  private persistUsersToCache(): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem('stafftrack.organisations.v1', JSON.stringify(this.organisations()));
      localStorage.setItem('stafftrack.users.v1', JSON.stringify(this.userAccounts()));
      localStorage.setItem('stafftrack.client_users.v1', JSON.stringify(this.clientUserLinks()));
      localStorage.setItem(
        'stafftrack.notification_supervisors.v1',
        JSON.stringify(this.notificationSupervisors())
      );
    } catch {}
  }

  // ---------- Entity Supervisors (Configurations) Actions ----------
  saveNotificationSupervisor(
    payload: Omit<NotificationSupervisor, 'id' | 'createdAt'>,
    existingId?: string
  ): void {
    const cleanPayload: Omit<NotificationSupervisor, 'id' | 'createdAt'> = {
      clientRef: payload.clientRef,
      entityName: (payload.entityName || '').trim(),
      entityCode: (payload.entityCode || '').trim().toUpperCase(),
      name: (payload.name || '').trim(),
      email: (payload.email || '').trim().toLowerCase(),
      phone: (payload.phone || '').trim(),
      category: payload.category || 'OVERALL_HEAD',
      department: (payload.department || '').trim() || undefined,
      status: payload.status || 'ACTIVE',
    };

    const applyLocal = (saved: NotificationSupervisor) => {
      if (existingId) {
        this.notificationSupervisors.update((list) =>
          list.map((s) => (s.id === existingId ? saved : s))
        );
        this.apiNotice.set({
          tone: 'success',
          text: `Updated notification supervisor ${saved.name || saved.email || saved.phone}.`,
        });
      } else {
        this.notificationSupervisors.update((list) => [saved, ...list]);
        this.apiNotice.set({
          tone: 'success',
          text: `Added supervisor ${saved.name || saved.email || saved.phone} to notification recipients.`,
        });
      }
      this.persistUsersToCache();
    };

    if (existingId) {
      this.api.updateNotificationSupervisor(existingId, cleanPayload).subscribe({
        next: (saved) => applyLocal(saved),
        error: (err) => {
          if (err?.status === 404 || err?.status === 0) {
            const current = this.notificationSupervisors().find((s) => s.id === existingId);
            applyLocal({
              ...cleanPayload,
              id: existingId,
              createdAt: current?.createdAt || new Date().toISOString().slice(0, 10),
            });
            return;
          }
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        },
      });
    } else {
      this.api.createNotificationSupervisor(cleanPayload).subscribe({
        next: (saved) => applyLocal(saved),
        error: (err) => {
          if (err?.status === 404 || err?.status === 0) {
            applyLocal({
              ...cleanPayload,
              id: 'SUP-' + Date.now().toString(36).toUpperCase(),
              createdAt: new Date().toISOString().slice(0, 10),
            });
            return;
          }
          this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
        },
      });
    }
  }

  updateNotificationSupervisorStatus(
    id: string,
    status: SupervisorNotificationStatus
  ): void {
    this.api.updateNotificationSupervisorStatus(id, status).subscribe({
      next: (saved) => {
        this.notificationSupervisors.update((list) =>
          list.map((s) => (s.id === id ? saved : s))
        );
        this.persistUsersToCache();
      },
      error: () => {
        this.notificationSupervisors.update((list) =>
          list.map((s) => (s.id === id ? { ...s, status } : s))
        );
        this.persistUsersToCache();
      },
    });
  }

  deleteNotificationSupervisor(id: string): void {
    const target = this.notificationSupervisors().find((s) => s.id === id);
    const label = target?.name || target?.email || target?.phone || id;
    this.confirm.ask({
      title: 'Remove Notification Supervisor',
      message: `Are you sure you want to remove "${label}" from receiving supervisor notifications?`,
      confirmLabel: 'Remove Supervisor',
      onConfirm: () => {
        const removeLocal = () => {
          this.notificationSupervisors.update((list) => list.filter((s) => s.id !== id));
          this.persistUsersToCache();
          this.apiNotice.set({
            tone: 'success',
            text: `Removed ${label} from notification supervisors.`,
          });
        };
        this.api.deleteNotificationSupervisor(id).subscribe({
          next: () => removeLocal(),
          error: (err) => {
            if (err?.status === 404 || err?.status === 0) {
              removeLocal();
              return;
            }
            this.apiNotice.set({ tone: 'error', text: extractApiErrorMessage(err) });
          },
        });
      },
    });
  }

  // ---------- Billing Plans (Configurations) Actions ----------
  saveBillingPlan(
    payload: Omit<ClientBillingPlan, 'id' | 'createdAt' | 'updatedAt'>,
    existingId?: string
  ): ClientBillingPlan {
    const today = new Date().toISOString().slice(0, 10);
    if (existingId) {
      let updatedPlan!: ClientBillingPlan;
      this.billingPlans.update((list) =>
        list.map((p) => {
          if (p.id !== existingId) return p;
          updatedPlan = { ...p, ...payload, updatedAt: today };
          return updatedPlan;
        })
      );
      return updatedPlan;
    } else {
      const created: ClientBillingPlan = {
        ...payload,
        id: 'PLN-' + Date.now().toString(36).toUpperCase(),
        createdAt: today,
        updatedAt: today,
      };
      this.billingPlans.update((list) => [created, ...list]);
      return created;
    }
  }

  updateBillingPlanStatus(id: string, status: BillingPlanStatus): void {
    const today = new Date().toISOString().slice(0, 10);
    this.billingPlans.update((list) =>
      list.map((p) => (p.id === id ? { ...p, status, updatedAt: today } : p))
    );
  }

  deleteBillingPlan(id: string): void {
    this.billingPlans.update((list) => list.filter((p) => p.id !== id));
  }

  private mapLookupDto(dto: MasterLookupApiDto): LookupItem {
    return {
      id: dto.id,
      code: dto.code,
      name: dto.name,
    };
  }

  private mapStaffDto(dto: StaffMemberApiDto): StaffMember {
    return {
      id: dto.id,
      staffCode: dto.staffCode || dto.id.slice(0, 8).toUpperCase(),
      surname: dto.surname,
      lastName: dto.lastName,
      designation: dto.designation,
      designationId: dto.designationId,
      email: dto.email,
      alternativeEmail: dto.alternativeEmail,
      phone: dto.phone,
      alternativePhone: dto.alternativePhone,
      department: dto.department,
      departmentId: dto.departmentId,
      dutyStation: dto.dutyStation,
      dutyStationId: dto.dutyStationId,
      status: dto.status || 'ACTIVE',
      createdAt: dto.createdAt ? dto.createdAt.slice(0, 10) : new Date().toISOString().slice(0, 10),
    };
  }

  private mapOooDto(dto: OutOfOfficeApiDto): OutOfOfficeNotification {
    return {
      id: dto.id,
      referenceNumber: dto.referenceNumber,
      staffMemberId: dto.staffMemberId,
      officerName: dto.officerName,
      officerRole: dto.officerRole,
      department: dto.department,
      dutyStation: dto.dutyStation,
      engagementType: dto.engagementType,
      destination: dto.destination,
      hostOrganization: dto.hostOrganization,
      startDate: dto.startDate,
      endDate: dto.endDate,
      purpose: dto.purpose,
      purposeList: dto.purposeList,
      attendingWithOthers: dto.attendingWithOthers,
      attendingOfficers: dto.attendingOfficers,
      actingOfficer: dto.actingOfficer,
      contactPhone: dto.contactPhone,
      status: dto.status,
      managementActionRequired: dto.managementActionRequired,
      unitHeadSupervisorId: dto.unitHeadSupervisorId,
      unitHeadName: dto.unitHeadName,
      unitHeadEmail: dto.unitHeadEmail,
      unitHeadPhone: dto.unitHeadPhone,
      unitHeadApprovalStatus: dto.unitHeadApprovalStatus,
      unitHeadReviewedAt: dto.unitHeadReviewedAt,
      unitHeadReviewedBy: dto.unitHeadReviewedBy,
      unitHeadComment: dto.unitHeadComment,
      supervisorConfirmationStatus: dto.supervisorConfirmationStatus || 'NEW',
      supervisorSeenAt: dto.supervisorSeenAt,
      supervisorConfirmedBy: dto.supervisorConfirmedBy,
      supervisorConfirmations: dto.supervisorConfirmations || [],
      reportBack: dto.reportBack,
      createdAt: dto.createdAt ? dto.createdAt.slice(0, 10) : new Date().toISOString().slice(0, 10),
    };
  }

  private mapWeeklyDto(dto: WeeklyConsolidatedReportApiDto): WeeklyConsolidatedReport {
    return {
      id: dto.id,
      weekLabel: dto.weekLabel,
      weekStart: dto.weekStart,
      weekEnd: dto.weekEnd,
      departmentFilter: dto.departmentFilter,
      dutyStation: dto.dutyStation || '',
      generatedBy: dto.generatedBy,
      generatedByDesignation: dto.generatedByDesignation || '',
      generatedAt: dto.generatedAt,
      executiveSummary: dto.executiveSummary || '',
      activitiesUndertaken: dto.activitiesUndertaken || [],
      keyOutcomes: [],
      pendingActions: dto.pendingActions || [],
    };
  }
}
