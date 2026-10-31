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
}
