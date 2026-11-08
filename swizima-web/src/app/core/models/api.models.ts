/**
 * Types mirroring the Swizima backend DTOs (usersapi / clientapi).
 * Keep in sync with the Java records in swizima-api.
 */

// ---------- Shared ----------
export type RecordStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

/** RFC 9457 problem details returned by every service on error. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: Record<string, string>;
}

// ---------- Auth (usersapi) ----------
export type OtpChannel = 'EMAIL' | 'SMS' | 'WHATSAPP';
export type UsernameType = 'EMAIL' | 'PHONE';
export type ActivationStatus = 'PENDING_ACTIVATION' | 'ACTIVE' | 'SUSPENDED' | 'LOCKED' | 'DEACTIVATED';
export type UserRole = 'SUPER_ADMIN' | 'CLIENT_ADMIN' | 'USER' | 'ADMIN';
export type AccountCategory = 'SUPER_ADMIN' | 'CLIENT_ADMIN' | 'USER';

export interface LoginRequest {
  username: string;
  password: string;
  otpChannel?: OtpChannel;
  accountCategory?: AccountCategory;
}

export interface OtpChallengeResponse {
  otpReference: string;
  channel: OtpChannel;
  maskedDestination: string;
  expiresAt: string;
  resendAvailableAt: string;
  expiresInSeconds: number;
  resendAvailableInSeconds: number;
  message: string;
}

export interface VerifyOtpRequest {
  otpReference: string;
  code: string;
}

export interface ResendOtpRequest {
  otpReference: string;
  channel?: OtpChannel;
}

export interface LoginResponse {
  userId: string;
  username: string;
  usernameType: UsernameType;
  email?: string;
  phoneNumber?: string;
  role?: UserRole;
  accountCategory?: AccountCategory;
  defaultClientId?: string;
  clientIds?: string[];
  activationStatus: ActivationStatus;
  mustChangePassword: boolean;
  lastLoginAt: string;
  message: string;
}

export interface SystemUser {
  id: string;
  username: string;
  usernameType: UsernameType;
  email?: string;
  phoneNumber?: string;
  preferredOtpChannel: OtpChannel;
  enabled: boolean;
  activationStatus: ActivationStatus;
  role: UserRole;
  accountCategory?: AccountCategory;
  passwordSystemGenerated: boolean;
  mustChangePassword: boolean;
  passwordChangedAt?: string;
  lastLoginAt?: string;
  createdAt: string;
  recordStatus: RecordStatus;
}

// ---------- Clients (clientapi) ----------
export type ClientStatus = 'PENDING' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
export type ClientType = 'INDIVIDUAL' | 'COMPANY' | 'GOVERNMENT' | 'NGO' | 'OTHER';

export interface Client {
  id: string;
  clientCode: string;
  clientName: string;
  tradingName?: string;
  clientType?: ClientType;
  industrySectors?: string[];
  email?: string;
  phoneNumber?: string;
  city?: string;
  district?: string;
  country?: string;
  contactPerson?: string;
  status: ClientStatus;
  createdAt: string;
  recordStatus: RecordStatus;
  // Present on GET /clients/{id} (full ClientResponse)
  description?: string;
  alternativePhone?: string;
  address?: string;
  contactPersonTitle?: string;
  contactPersonEmail?: string;
  contactPersonPhone?: string;
  createdBy?: string;
  updatedBy?: string;
  updatedAt?: string;
}

/** POST/PUT /api/v1/clients body (mirrors ClientRequest.java). */
export interface ClientRequest {
  clientCode: string;
  clientName: string;
  tradingName?: string;
  clientType?: ClientType;
  /** Stored by clientapi as a JSON array string. */
  industrySectors?: string[];
  description?: string;
  email?: string;
  phoneNumber?: string;
  alternativePhone?: string;
  address?: string;
  country?: string;
  district?: string;
  city?: string;
  contactPerson?: string;
  contactPersonTitle?: string;
  contactPersonEmail?: string;
  contactPersonPhone?: string;
  status?: ClientStatus;
}

// ---------- Client staff (clientapi) ----------
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

export interface ClientStaff {
  id: string;
  clientId: string;
  clientCode: string;
  clientName: string;
  surname: string;
  givenName: string;
  fullName: string;
  gender?: Gender;
  phoneNumber?: string;
  alternativePhone?: string;
  email?: string;
  position?: string;
  nin?: string;
  summary?: string;
  createdAt: string;
  updatedAt?: string;
  recordStatus: RecordStatus;
}

/** POST/PUT /api/v1/clients/{clientId}/staff body (mirrors ClientStaffRequest.java). */
export interface ClientStaffRequest {
  surname: string;
  givenName: string;
  gender?: Gender;
  phoneNumber?: string;
  alternativePhone?: string;
  email?: string;
  position?: string;
  nin?: string;
  summary?: string;
}

// ---------- Service catalog (clientapi) ----------
export type ServiceCategory = 'CAR_PARK' | 'CAR_WASH' | 'REPAIR' | 'MAINTENANCE' | 'OTHER';
export type ServiceCatalogStatus = 'ACTIVE' | 'INACTIVE';

export interface ServiceCatalogItem {
  id: string;
  clientId: string;
  clientCode: string;
  clientName: string;
  serviceCode: string;
  serviceName: string;
  category: ServiceCategory;
  description?: string;
  price: number;
  currency: string;
  durationMinutes?: number;
  status: ServiceCatalogStatus;
  createdAt: string;
  updatedAt?: string;
}

/** POST/PUT /api/v1/service-catalogs body (mirrors ServiceCatalogRequest.java). */
export interface ServiceCatalogRequest {
  clientId: string;
  serviceCode: string;
  serviceName: string;
  category: ServiceCategory;
  description?: string;
  price: number;
  currency?: string;
  durationMinutes?: number;
  status?: ServiceCatalogStatus;
}

// ---------- Client transactions API ----------
export type IdDocumentType = 'NATIONAL_ID' | 'PASSPORT' | 'DRIVING_PERMIT' | 'OTHER';
export type PaymentStatus = 'PENDING' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED';
export type VehicleClearanceStatus = 'IN_SERVICE' | 'READY' | 'CLEARED' | 'RELEASED';

/** GET /api/v1/client-transactions/vehicle-services item (mirrors VehicleServiceTransactionResponse.java). */
export interface VehicleServiceTransaction {
  id: string;
  clientRef: string;
  customerPhoneNumber: string;
  customerAlternativePhone?: string;
  vehicleNumberPlate: string;
  serviceType: string;
  amountCharged: number;
  amountPaid: number;
  balance: number;
  assignedTo?: string;
  vehicleKeyShared: boolean;
  entryDateTime: string;
  entryDate: string;
  entryTime: string;
  expectedExitTime?: string;
  actualExitDateTime?: string;
  actualExitDate?: string;
  actualExitTime?: string;
  releasedBy?: string;
  clearedAt?: string;
  clearedBy?: string;
  customerName?: string;
  customerNationality?: string;
  customerIdDocumentType?: IdDocumentType;
  customerIdNumber?: string;
  customerAddress?: string;
  generalComment?: string;
  paymentStatus: PaymentStatus;
  vehicleClearanceStatus: VehicleClearanceStatus;
  recordStatus: RecordStatus;
  createdAt: string;
  updatedAt?: string;
}

/** Money/count bucket in the transactions report (money excludes CANCELLED). */
export interface SummaryTotals {
  count: number;
  amountCharged: number;
  amountPaid: number;
  outstanding: number;
}

/** Work done by one assigned staff member; assignedTo is null for unassigned. */
export interface StaffSummary {
  assignedTo: string | null;
  totals: SummaryTotals;
  paymentStatusCounts: Record<PaymentStatus, number>;
  clearanceStatusCounts: Record<VehicleClearanceStatus, number>;
}

/** GET /vehicle-services/reports/summary (mirrors TransactionSummaryResponse.java). */
export interface TransactionSummary {
  clientRef: string;
  fromDate: string;
  toDate: string;
  totals: SummaryTotals;
  byPaymentStatus: Record<PaymentStatus, SummaryTotals>;
  byClearanceStatus: Record<VehicleClearanceStatus, SummaryTotals>;
  byStaff: StaffSummary[];
  daily: { date: string; totals: SummaryTotals }[];
}

/** POST/PUT body (mirrors VehicleServiceTransactionRequest.java). Datetimes are local ISO "yyyy-MM-ddTHH:mm". */
export interface VehicleServiceTransactionRequest {
  clientRef: string;
  customerPhoneNumber: string;
  customerAlternativePhone?: string;
  vehicleNumberPlate: string;
  serviceType: string;
  amountCharged: number;
  assignedTo?: string;
  vehicleKeyShared?: boolean;
  entryDateTime?: string;
  expectedExitTime?: string;
  actualExitDateTime?: string;
  customerName?: string;
  customerNationality?: string;
  customerIdDocumentType?: IdDocumentType;
  customerIdNumber?: string;
  customerAddress?: string;
  generalComment?: string;
  paymentStatus?: PaymentStatus;
  vehicleClearanceStatus?: VehicleClearanceStatus;
}

export type PaymentMethod = 'CASH' | 'MOBILE_MONEY' | 'CARD' | 'BANK';

/** GET /api/v1/client-transactions/vehicle-services/{id}/payments item. */
export interface TransactionPayment {
  id: string;
  transactionId: string;
  amount: number;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
  paidByName?: string;
  paidByPhone?: string;
  receivedBy?: string;
  paymentDateTime: string;
  comment?: string;
  createdAt: string;
}

export interface TransactionPaymentRequest {
  amount: number;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
  paidByName?: string;
  paidByPhone?: string;
  receivedBy?: string;
  paymentDateTime?: string;
  comment?: string;
}

/** Body for POST .../{id}/clear and .../{id}/exit (all optional; `at` defaults to now). */
export interface VehicleActionRequest {
  at?: string;
  by?: string;
  comment?: string;
}

// ---------- Client users (usersapi) ----------
/** GET /api/v1/users/client-users item (mirrors ClientUserResponse.java). */
export interface ClientUser {
  id: string;
  clientRef: string;
  staffRef?: string;
  systemUserId?: string;
  userPhoneNumber?: string;
  userEmail?: string;
  accountCategory?: AccountCategory;
  activationStatus: ActivationStatus;
  isDefaultClient: boolean;
  recordStatus: RecordStatus;
  createdAt: string;
  updatedAt?: string;
}

/** POST /api/v1/users/client-users/assign-account body. `username` is the sign-in id (phone or email). */
export interface AssignAccountRequest {
  clientRef: string;
  staffRef?: string;
  username: string;
  phoneNumber?: string;
  email?: string;
  preferredOtpChannel?: OtpChannel;
  accountCategory?: AccountCategory;
  isDefaultClient?: boolean;
  /** Email the temporary password to the user (defaults to true). */
  sendCredentials?: boolean;
  /** Client name used in the welcome email. */
  organisationName?: string;
}

export interface AssignAccountResponse {
  clientUser: ClientUser;
  user: SystemUser;
  userCreated: boolean;
  /** Only when a new system user was created with a generated password - shown once. */
  temporaryPassword?: string;
  /** Masked email the credentials were sent to. */
  credentialsSentTo?: string;
  /** Why the credentials weren't sent, if they weren't. */
  deliveryNote?: string;
}

// ---------- Billing plans (configurationsapi) ----------
export type BillingPlanStatus = 'ACTIVE' | 'SUSPENDED';
export type BillingModel = 'COMMISSION' | 'MONTHLY_FLAT';

/** Billing rules for a client: either per-transaction percentage or end-of-month agreed amount. */
export interface BillingPlan {
  id: string;
  clientRef: string;
  billingModel?: BillingModel;
  percentageRate: number;
  monthlyAmount?: number;
  currency: string;
  lowBalanceThreshold?: number;
  status: BillingPlanStatus;
  notes?: string;
  createdAt: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
}

export interface BillingPlanRequest {
  clientRef: string;
  billingModel?: BillingModel;
  percentageRate?: number;
  monthlyAmount?: number;
  currency?: string;
  lowBalanceThreshold?: number;
  status?: BillingPlanStatus;
  notes?: string;
}

// ---------- Billing wallets, credit loads & activity (paymentsapi) ----------
export type CreditPaymentMethod = 'CASH' | 'MOBILE_MONEY' | 'BANK_TRANSFER' | 'CARD' | 'OTHER';
export type CreditLoadStatus = 'COMPLETED' | 'REVERSED';
export type CreditRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type BillingActivityType = 'CREDIT_LOAD' | 'CREDIT_LOAD_REVERSAL' | 'TRANSACTION_CHARGE' | 'CHARGE_REVERSAL';

/** Prepaid credit balance. Rate / threshold / planStatus are filled when fetched per client. */
export interface BillingWallet {
  id?: string;
  clientRef: string;
  currency: string;
  balance: number;
  totalCredited: number;
  totalCharged: number;
  billingModel?: BillingModel;
  percentageRate?: number;
  monthlyAmount?: number;
  lowBalanceThreshold?: number;
  lowBalance?: boolean;
  planStatus?: BillingPlanStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreditLoadRequest {
  amount: number;
  paymentMethod: CreditPaymentMethod;
  paymentReference?: string;
  payerName?: string;
  payerPhone?: string;
  receivedBy?: string;
  /** ISO local date-time; defaults to now on the server. */
  loadedAt?: string;
  notes?: string;
}

export interface CreditLoad {
  id: string;
  walletId: string;
  clientRef: string;
  amount: number;
  currency: string;
  paymentMethod: CreditPaymentMethod;
  paymentReference?: string;
  payerName?: string;
  payerPhone?: string;
  receivedBy?: string;
  loadedAt: string;
  balanceBefore: number;
  balanceAfter: number;
  notes?: string;
  status: CreditLoadStatus;
  reversedAt?: string;
  reversedBy?: string;
  reversalReason?: string;
  createdAt: string;
  createdBy?: string;
}

export interface CreditTopUpRequestCreate {
  requestedAmount: number;
  paymentMethod: CreditPaymentMethod;
  paymentReference?: string;
  payerName?: string;
  payerPhone?: string;
  notes?: string;
}

export interface CreditTopUpApprovalRequest {
  approvedAmount: number;
  paymentMethod?: CreditPaymentMethod;
  paymentReference?: string;
  reviewedBy?: string;
  reviewNotes?: string;
}

export interface CreditTopUpRejectionRequest {
  reason: string;
  reviewedBy?: string;
}

export interface CreditTopUpRequest {
  id: string;
  clientRef: string;
  requestedAmount: number;
  approvedAmount?: number;
  currency: string;
  paymentMethod: CreditPaymentMethod;
  paymentReference?: string;
  payerName?: string;
  payerPhone?: string;
  notes?: string;
  status: CreditRequestStatus;
  reviewedAt?: string;
  reviewedBy?: string;
  reviewNotes?: string;
  creditLoadId?: string;
  createdAt: string;
  createdBy?: string;
}

export interface BillingActivity {
  id: string;
  walletId: string;
  clientRef: string;
  activityType: BillingActivityType;
  /** Signed: positive = credit, negative = debit. */
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  currency: string;
  transactionRef?: string;
  transactionAmount?: number;
  percentageApplied?: number;
  creditLoadId?: string;
  paymentReference?: string;
  description?: string;
  createdAt: string;
  createdBy?: string;
}

/** Extra ProblemDetail fields returned when billing blocks a transaction. */
export interface BillingProblem extends ProblemDetail {
  code?: 'INSUFFICIENT_CREDIT' | 'BILLING_NOT_CONFIGURED' | 'BILLING_REJECTED' | 'BILLING_UNAVAILABLE';
  topUpRequired?: boolean;
  requiredAmount?: number;
  balance?: number;
  currency?: string;
}
