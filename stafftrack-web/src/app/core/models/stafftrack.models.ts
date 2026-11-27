export type EngagementType =
  | 'TRAVEL'
  | 'MEETING'
  | 'WORKSHOP'
  | 'FIELD_ASSIGNMENT'
  | 'OFFICIAL_ENGAGEMENT';

export type OutOfOfficeStatus =
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'ACTIVE_AWAY'
  | 'RETURNED'
  | 'CANCELLED';

export type SupervisorConfirmationStatus = 'NEW' | 'SEEN';

export type SupervisorCategory = 'UNIT_HEAD' | 'OVERALL_HEAD';

export type UnitHeadApprovalStatus =
  | 'PENDING_UNIT_HEAD'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'NOT_REQUIRED';

export const SUPERVISOR_CATEGORY_LABELS: Record<SupervisorCategory, string> = {
  UNIT_HEAD: 'Unit Head',
  OVERALL_HEAD: 'Overall Head',
};

export const UNIT_HEAD_APPROVAL_LABELS: Record<UnitHeadApprovalStatus, string> = {
  PENDING_UNIT_HEAD: 'Pending Unit Head',
  ACCEPTED: 'Accepted by Unit Head',
  REJECTED: 'Rejected by Unit Head',
  NOT_REQUIRED: 'Not Required',
};

export interface SupervisorConfirmationRecord {
  id: string;
  notificationId?: string;
  notificationRef?: string;
  supervisorId?: string;
  supervisorName?: string;
  supervisorEmail?: string;
  supervisorPhone?: string;
  supervisorCategory?: SupervisorCategory;
  confirmationStatus: SupervisorConfirmationStatus;
  seenAt?: string;
  confirmedBy?: string;
  createdAt?: string;
}

export interface ReportBackManagementAction {
  id?: string;
  followUpAction: string;
  responsibleStaffId?: string;
  responsibleOfficer: string;
  targetDate: string;
}

export interface ReportBackAttachment {
  id: string;
  fileUuid?: string;
  name: string;
  mimeType: string;
  sizeBytes: number;
  kind: 'IMAGE' | 'PDF' | 'DOCUMENT';
  dataUrl: string;
  downloadUrl?: string;
  streamUrl?: string;
  previewUrl?: string;
  uploadedAt: string;
}

export interface OutOfOfficeReportBack {
  id?: string;
  notificationId?: string;
  actualReturnDate: string;
  stakeholdersMet: string;
  whatWasDone: string;
  whatWasDiscussed: string;
  whatWasAchieved: string;
  outstandingIssues: string;
  keyIssuesList?: string[];
  achievementsList?: string[];
  managementActions: ReportBackManagementAction[];
  attachments?: ReportBackAttachment[];
  submittedAt: string;
}

export interface AttendingOfficer {
  id?: string;
  staffMemberId?: string;
  name: string;
  designation: string;
  department: string;
  dutyStation: string;
  isCustom?: boolean;
}

export interface OutOfOfficeNotification {
  id: string;
  referenceNumber?: string;
  staffMemberId?: string;
  officerName: string;
  officerRole: string;
  department: string;
  dutyStation: string;
  engagementType: EngagementType;
  destination: string;
  hostOrganization: string;
  startDate: string; // YYYY-MM-DD
  endDate: string;   // YYYY-MM-DD
  purpose: string;
  purposeList?: string[];
  attendingWithOthers?: boolean;
  attendingOfficers?: AttendingOfficer[];
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
  supervisorConfirmationStatus?: SupervisorConfirmationStatus;
  supervisorSeenAt?: string;
  supervisorConfirmedBy?: string;
  supervisorConfirmations?: SupervisorConfirmationRecord[];
  reportBack?: OutOfOfficeReportBack;
  createdAt: string;
}

export type DutyStatus =
  | 'NOT_STARTED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'FOLLOW_UP_REQUIRED'
  | 'BLOCKED';

export type PriorityLevel = 'HIGH' | 'MEDIUM' | 'LOW';

export interface DutyActivity {
  id: string;
  date: string; // YYYY-MM-DD
  officerName: string;
  officerDesignation: string;
  department: string;
  dutyStation: string;
  activityTitle: string;
  assignmentCategory: string;
  priority: PriorityLevel;
  progressPercent: number;
  status: DutyStatus;
  outcomes: string;
  followUpAction: string;
  followUpOwner: string;
  followUpDueDate: string;
}

export type ReportReviewStatus = 'SUBMITTED' | 'REVIEWED' | 'ACTION_REQUIRED';

export interface ActivityReport {
  id: string;
  reportDate: string; // YYYY-MM-DD
  title: string;
  relatedEngagement: string;
  officerName: string;
  officerDesignation: string;
  department: string;
  dutyStation: string;
  stakeholdersMet: string;
  whatWasDone: string;
  whatWasDiscussed: string;
  whatWasAchieved: string;
  outstandingIssues: string;
  requiresManagementIntervention: boolean;
  reviewStatus: ReportReviewStatus;
}

export interface ManagementActionItem {
  id: string;
  sourceModule: 'OUT_OF_OFFICE' | 'DUTY_TRACKER' | 'ACTIVITY_REPORT';
  sourceId: string;
  officerName: string;
  department: string;
  dutyStation: string;
  title: string;
  detail: string;
  urgency: 'CRITICAL' | 'HIGH' | 'NORMAL';
  actionLabel: string;
}

export interface WeeklyPendingAction {
  item: string;
  source: string;
  responsibleOfficer: string;
  department: string;
  dutyStation: string;
  targetDate: string;
  status: string;
}

export interface WeeklyConsolidatedReport {
  id: string;
  weekLabel: string;
  weekStart: string;
  weekEnd: string;
  departmentFilter: string;
  dutyStation: string;
  generatedBy: string;
  generatedByDesignation: string;
  generatedAt: string;
  executiveSummary: string;
  activitiesUndertaken: Array<{
    date: string;
    officerName: string;
    officerDesignation: string;
    department: string;
    dutyStation: string;
    category: string;
    summary: string;
  }>;
  keyOutcomes: Array<{
    officerName: string;
    officerDesignation: string;
    department: string;
    dutyStation: string;
    outcome: string;
  }>;
  pendingActions: WeeklyPendingAction[];
}

export interface StaffMember {
  id: string;
  staffCode?: string;
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
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}

export const ENGAGEMENT_TYPE_LABELS: Record<EngagementType, string> = {
  TRAVEL: 'Official Travel',
  MEETING: 'External Meeting',
  WORKSHOP: 'Workshop / Training',
  FIELD_ASSIGNMENT: 'Field Assignment',
  OFFICIAL_ENGAGEMENT: 'Official Engagement',
};

export const OOO_STATUS_LABELS: Record<OutOfOfficeStatus, string> = {
  PENDING_APPROVAL: 'Pending Approval',
  APPROVED: 'Approved (Upcoming)',
  ACTIVE_AWAY: 'Currently Away',
  RETURNED: 'Returned',
  CANCELLED: 'Cancelled',
};

export const DUTY_STATUS_LABELS: Record<DutyStatus, string> = {
  NOT_STARTED: 'Not Started',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
  FOLLOW_UP_REQUIRED: 'Follow-Up Required',
  BLOCKED: 'Blocked / Escalated',
};

export const DEPARTMENTS: string[] = [];

export const OFFICER_DESIGNATIONS: string[] = [];

export const OTHER_DESIGNATION_OPTION = 'Other (Specify)';

export const DUTY_STATIONS: string[] = [];

export interface LookupItem {
  id: string;
  code: string;
  name: string;
}

export const DEFAULT_DEPARTMENT_ITEMS: LookupItem[] = [];

export const DEFAULT_DESIGNATION_ITEMS: LookupItem[] = [];

export const DEFAULT_DUTY_STATION_ITEMS: LookupItem[] = [];

export const OTHER_DUTY_STATION_OPTION = 'Other (Specify)';

export type UserSystemRole =
  | 'SUPER_ADMIN'
  | 'DEPARTMENT_HEAD'
  | 'DESK_OFFICER'
  | 'STAFF_OFFICER';

export type UserAccountStatus =
  | 'ACTIVE'
  | 'PENDING_ACTIVATION'
  | 'SUSPENDED';

export type UserOtpChannel = 'EMAIL' | 'SMS' | 'WHATSAPP';

export interface SystemUserAccount {
  id: string;
  staffId?: string;
  fullName: string;
  username: string;
  email: string;
  phone: string;
  designation: string;
  department: string;
  dutyStation: string;
  role: UserSystemRole;
  preferredOtpChannel: UserOtpChannel;
  status: UserAccountStatus;
  lastLoginAt?: string;
  createdAt: string;
}

export const USER_ROLE_LABELS: Record<UserSystemRole, string> = {
  SUPER_ADMIN: 'Super Administrator',
  DEPARTMENT_HEAD: 'Department / Unit Head',
  DESK_OFFICER: 'Desk Officer / Commandant',
  STAFF_OFFICER: 'Staff Officer',
};

export const USER_STATUS_LABELS: Record<UserAccountStatus, string> = {
  ACTIVE: 'Active',
  PENDING_ACTIVATION: 'Pending Activation',
  SUSPENDED: 'Suspended',
};

export interface SystemConfigurationSettings {
  organizationName: string;
  directorateName: string;
  systemShortName: string;
  systemFullTitle: string;
  reportHeaderTitle: string;
  defaultDialCode: string;
  defaultOtpChannel: UserOtpChannel;
  otpExpiryMinutes: number;
  showQrAndBarcodeOnReports: boolean;
  requireReportBackOnReturn: boolean;
  supportEmail: string;
  supportPhone: string;
}

export type BillingModel = 'COMMISSION' | 'MONTHLY_FLAT';
export type BillingPlanStatus = 'ACTIVE' | 'SUSPENDED';

export interface ClientBillingPlan {
  id: string;
  clientRef: string;
  clientName: string;
  clientCode: string;
  billingModel: BillingModel;
  percentageRate: number;
  monthlyAmount?: number;
  currency: 'UGX' | 'KES' | 'USD';
  lowBalanceThreshold?: number;
  status: BillingPlanStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export type AccountCategory = 'SUPER_ADMIN' | 'CLIENT_ADMIN' | 'USER';
export type ActivationStatus =
  | 'PENDING_ACTIVATION'
  | 'ACTIVE'
  | 'SUSPENDED'
  | 'LOCKED'
  | 'DEACTIVATED';

export interface ClientUserLink {
  id: string;
  systemUserId?: string;
  userPhoneNumber?: string;
  userEmail?: string;
  accountCategory: AccountCategory;
  clientRef: string;
  clientName: string;
  clientCode: string;
  isDefaultClient: boolean;
  activationStatus: ActivationStatus;
  createdAt: string;
}

export type OrganisationStatus = 'ACTIVE' | 'PENDING' | 'INACTIVE' | 'SUSPENDED';

export interface Organisation {
  id: string;
  code: string;
  name: string;
  organisationType: string;
  contactPerson: string;
  email: string;
  phone: string;
  city: string;
  country: string;
  address?: string;
  status: OrganisationStatus;
  createdAt: string;
}

export type SupervisorNotificationStatus = 'ACTIVE' | 'INACTIVE';

export interface NotificationSupervisor {
  id: string;
  clientRef?: string;
  entityName?: string;
  entityCode?: string;
  name?: string;
  email?: string;
  phone?: string;
  category?: SupervisorCategory;
  department?: string;
  status: SupervisorNotificationStatus;
  createdAt: string;
}

export interface NotificationChannelConfig {
  id: string;
  channelCode: UserOtpChannel;
  displayName: string;
  description: string;
  active: boolean;
  otpEnabled: boolean;
  supervisorAlertEnabled: boolean;
  displayOrder: number;
  updatedAt: string;
}




