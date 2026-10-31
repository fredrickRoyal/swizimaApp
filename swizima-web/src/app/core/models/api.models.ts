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
export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'USER';

export interface LoginRequest {
  username: string;
  password: string;
  otpChannel?: OtpChannel;
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
