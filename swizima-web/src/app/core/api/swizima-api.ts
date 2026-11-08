import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import {
  AccountCategory,
  ActivationStatus,
  BillingActivity,
  BillingActivityType,
  BillingPlan,
  BillingPlanRequest,
  BillingPlanStatus,
  BillingWallet,
  CreditLoad,
  CreditLoadRequest,
  CreditLoadStatus,
  CreditPaymentMethod,
  CreditRequestStatus,
  CreditTopUpApprovalRequest,
  CreditTopUpRejectionRequest,
  CreditTopUpRequest,
  CreditTopUpRequestCreate,
  AssignAccountRequest,
  AssignAccountResponse,
  ClientUser,
  Client,
  ClientRequest,
  ClientStatus,
  ClientStaff,
  ClientStaffRequest,
  Gender,
  PageResponse,
  PaymentStatus,
  TransactionPayment,
  TransactionPaymentRequest,
  VehicleActionRequest,
  VehicleClearanceStatus,
  TransactionSummary,
  VehicleServiceTransaction,
  VehicleServiceTransactionRequest,
  ServiceCatalogItem,
  ServiceCatalogRequest,
  ServiceCatalogStatus,
  ServiceCategory,
  SystemUser,
} from '../models/api.models';

export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string;
}

/** Thin typed wrappers over the gateway endpoints used by the admin portal. */
@Injectable({ providedIn: 'root' })
export class SwizimaApi {
  private readonly http = inject(HttpClient);

  clients(query: PageQuery & { search?: string; status?: ClientStatus | '' } = {}): Observable<PageResponse<Client>> {
    return this.http.get<PageResponse<Client>>('/api/v1/clients', { params: toParams(query) });
  }

  /** Registers a client. 409 if the client code already exists, 400 with `errors` on validation failure. */
  createClient(request: ClientRequest): Observable<Client> {
    return this.http.post<Client>('/api/v1/clients', request);
  }

  serviceCatalogs(
    query: PageQuery & {
      search?: string;
      category?: ServiceCategory | '';
      status?: ServiceCatalogStatus | '';
      clientId?: string;
    } = {},
  ): Observable<PageResponse<ServiceCatalogItem>> {
    return this.http.get<PageResponse<ServiceCatalogItem>>('/api/v1/service-catalogs', { params: toParams(query) });
  }

  client(id: string): Observable<Client> {
    return this.http.get<Client>(`/api/v1/clients/${encodeURIComponent(id)}`);
  }

  createService(request: ServiceCatalogRequest): Observable<ServiceCatalogItem> {
    return this.http.post<ServiceCatalogItem>('/api/v1/service-catalogs', request);
  }

  updateService(id: string, request: ServiceCatalogRequest): Observable<ServiceCatalogItem> {
    return this.http.put<ServiceCatalogItem>(`/api/v1/service-catalogs/${encodeURIComponent(id)}`, request);
  }

  updateServiceStatus(id: string, status: ServiceCatalogStatus): Observable<ServiceCatalogItem> {
    return this.http.patch<ServiceCatalogItem>(`/api/v1/service-catalogs/${encodeURIComponent(id)}/status`, { status });
  }

  deleteService(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/service-catalogs/${encodeURIComponent(id)}`);
  }

  staff(clientId: string, query: PageQuery & { gender?: Gender | ''; search?: string } = {}): Observable<PageResponse<ClientStaff>> {
    return this.http.get<PageResponse<ClientStaff>>(staffUrl(clientId), { params: toParams(query) });
  }

  createStaff(clientId: string, request: ClientStaffRequest): Observable<ClientStaff> {
    return this.http.post<ClientStaff>(staffUrl(clientId), request);
  }

  updateStaff(clientId: string, staffId: string, request: ClientStaffRequest): Observable<ClientStaff> {
    return this.http.put<ClientStaff>(staffUrl(clientId, staffId), request);
  }

  deleteStaff(clientId: string, staffId: string): Observable<void> {
    return this.http.delete<void>(staffUrl(clientId, staffId));
  }

  vehicleServices(
    query: PageQuery & {
      clientRef?: string;
      paymentStatus?: PaymentStatus | '';
      clearanceStatus?: VehicleClearanceStatus | '';
      fromDate?: string;
      toDate?: string;
      /** Exact staff name; "-" selects unassigned transactions. */
      assignedTo?: string;
      search?: string;
    } = {},
  ): Observable<PageResponse<VehicleServiceTransaction>> {
    return this.http.get<PageResponse<VehicleServiceTransaction>>(VEHICLE_SERVICES, { params: toParams(query) });
  }

  transactionSummary(query: { clientRef: string; fromDate: string; toDate: string }): Observable<TransactionSummary> {
    return this.http.get<TransactionSummary>(`${VEHICLE_SERVICES}/reports/summary`, { params: toParams(query) });
  }

  vehicleService(id: string): Observable<VehicleServiceTransaction> {
    return this.http.get<VehicleServiceTransaction>(`${VEHICLE_SERVICES}/${encodeURIComponent(id)}`);
  }

  createVehicleService(request: VehicleServiceTransactionRequest): Observable<VehicleServiceTransaction> {
    return this.http.post<VehicleServiceTransaction>(VEHICLE_SERVICES, request);
  }

  updateVehicleService(id: string, request: VehicleServiceTransactionRequest): Observable<VehicleServiceTransaction> {
    return this.http.put<VehicleServiceTransaction>(`${VEHICLE_SERVICES}/${encodeURIComponent(id)}`, request);
  }

  deleteVehicleService(id: string): Observable<void> {
    return this.http.delete<void>(`${VEHICLE_SERVICES}/${encodeURIComponent(id)}`);
  }

  /** Fully paid -> CLEARED (409 with a message if a balance is outstanding). */
  clearVehicle(id: string, request: VehicleActionRequest = {}): Observable<VehicleServiceTransaction> {
    return this.http.post<VehicleServiceTransaction>(`${VEHICLE_SERVICES}/${encodeURIComponent(id)}/clear`, request);
  }

  /** CLEARED -> RELEASED, recording the actual exit time. */
  exitVehicle(id: string, request: VehicleActionRequest = {}): Observable<VehicleServiceTransaction> {
    return this.http.post<VehicleServiceTransaction>(`${VEHICLE_SERVICES}/${encodeURIComponent(id)}/exit`, request);
  }

  transactionPayments(id: string): Observable<TransactionPayment[]> {
    return this.http.get<TransactionPayment[]>(`${VEHICLE_SERVICES}/${encodeURIComponent(id)}/payments`);
  }

  addTransactionPayment(id: string, request: TransactionPaymentRequest): Observable<TransactionPayment> {
    return this.http.post<TransactionPayment>(`${VEHICLE_SERVICES}/${encodeURIComponent(id)}/payments`, request);
  }

  deleteTransactionPayment(id: string, paymentId: string): Observable<void> {
    return this.http.delete<void>(
      `${VEHICLE_SERVICES}/${encodeURIComponent(id)}/payments/${encodeURIComponent(paymentId)}`,
    );
  }

  clientUsers(
    query: PageQuery & {
      clientRef?: string;
      staffRef?: string;
      activationStatus?: ActivationStatus | '';
      isDefaultClient?: boolean | '';
      search?: string;
    } = {},
  ): Observable<PageResponse<ClientUser>> {
    return this.http.get<PageResponse<ClientUser>>(CLIENT_USERS, { params: toParams(query) });
  }

  /** Creates the system user if needed and links it to the client. */
  assignAccount(request: AssignAccountRequest): Observable<AssignAccountResponse> {
    return this.http.post<AssignAccountResponse>(`${CLIENT_USERS}/assign-account`, request);
  }

  /** Every client a person (phone and/or email) can access, default first. */
  clientUsersByIdentity(phoneNumber?: string, email?: string): Observable<ClientUser[]> {
    return this.http.get<ClientUser[]>(`${CLIENT_USERS}/by-identity`, { params: toParams({ phoneNumber, email }) });
  }

  makeDefaultClient(clientUserId: string): Observable<ClientUser> {
    return this.http.patch<ClientUser>(`${CLIENT_USERS}/${encodeURIComponent(clientUserId)}/default`, {});
  }

  updateClientUserStatus(clientUserId: string, activationStatus: ActivationStatus): Observable<ClientUser> {
    return this.http.patch<ClientUser>(`${CLIENT_USERS}/${encodeURIComponent(clientUserId)}/activation-status`, { activationStatus });
  }

  removeClientUser(clientUserId: string): Observable<void> {
    return this.http.delete<void>(`${CLIENT_USERS}/${encodeURIComponent(clientUserId)}`);
  }

  users(
    query: PageQuery & {
      search?: string;
      accountCategory?: AccountCategory | '';
      activationStatus?: ActivationStatus | '';
      enabled?: boolean | '';
    } = {},
  ): Observable<PageResponse<SystemUser>> {
    return this.http.get<PageResponse<SystemUser>>('/api/v1/users', { params: toParams(query) });
  }

  updateUserStatus(userId: string, activationStatus: ActivationStatus): Observable<SystemUser> {
    return this.http.patch<SystemUser>(`/api/v1/users/${encodeURIComponent(userId)}/activation-status`, { activationStatus });
  }

  resetUserPassword(userId: string): Observable<{ user: SystemUser; temporaryPassword?: string }> {
    return this.http.post<{ user: SystemUser; temporaryPassword?: string }>(
      `/api/v1/users/${encodeURIComponent(userId)}/reset-password`,
      {},
    );
  }

  // ---------- Billing plans (configurationsapi) ----------

  billingPlans(query: PageQuery & { status?: BillingPlanStatus | '' } = {}): Observable<PageResponse<BillingPlan>> {
    return this.http.get<PageResponse<BillingPlan>>(BILLING_PLANS, { params: toParams(query) });
  }

  /** 404 when the client has no plan yet. */
  billingPlanByClient(clientRef: string): Observable<BillingPlan> {
    return this.http.get<BillingPlan>(`${BILLING_PLANS}/client/${encodeURIComponent(clientRef)}`);
  }

  createBillingPlan(request: BillingPlanRequest): Observable<BillingPlan> {
    return this.http.post<BillingPlan>(BILLING_PLANS, request);
  }

  updateBillingPlan(id: string, request: BillingPlanRequest): Observable<BillingPlan> {
    return this.http.put<BillingPlan>(`${BILLING_PLANS}/${encodeURIComponent(id)}`, request);
  }

  updateBillingPlanStatus(id: string, status: BillingPlanStatus): Observable<BillingPlan> {
    return this.http.patch<BillingPlan>(`${BILLING_PLANS}/${encodeURIComponent(id)}/status`, { status });
  }

  deleteBillingPlan(id: string): Observable<void> {
    return this.http.delete<void>(`${BILLING_PLANS}/${encodeURIComponent(id)}`);
  }

  // ---------- Billing wallets, credit loads & activity (paymentsapi) ----------

  billingWallets(query: PageQuery & { maxBalance?: number | '' } = {}): Observable<PageResponse<BillingWallet>> {
    return this.http.get<PageResponse<BillingWallet>>(`${BILLING}/wallets`, { params: toParams(query) });
  }

  /** Balance plus the client's rate / low-balance flag. 402 BILLING_NOT_CONFIGURED without a plan. */
  billingWallet(clientRef: string): Observable<BillingWallet> {
    return this.http.get<BillingWallet>(`${BILLING}/wallets/client/${encodeURIComponent(clientRef)}`);
  }

  loadCredit(clientRef: string, request: CreditLoadRequest): Observable<CreditLoad> {
    return this.http.post<CreditLoad>(`${BILLING}/wallets/client/${encodeURIComponent(clientRef)}/credits`, request);
  }

  creditLoads(
    query: PageQuery & {
      clientRef?: string;
      status?: CreditLoadStatus | '';
      paymentMethod?: CreditPaymentMethod | '';
      from?: string;
      to?: string;
      search?: string;
    } = {},
  ): Observable<PageResponse<CreditLoad>> {
    return this.http.get<PageResponse<CreditLoad>>(`${BILLING}/credit-loads`, { params: toParams(query) });
  }

  reverseCreditLoad(id: string, reason: string, reversedBy?: string): Observable<CreditLoad> {
    return this.http.post<CreditLoad>(`${BILLING}/credit-loads/${encodeURIComponent(id)}/reversal`, { reason, reversedBy });
  }

  submitCreditRequest(clientRef: string, request: CreditTopUpRequestCreate): Observable<CreditTopUpRequest> {
    return this.http
      .post<CreditTopUpRequest>(`${BILLING}/wallets/client/${encodeURIComponent(clientRef)}/credit-requests`, request)
      .pipe(
        catchError((err) => {
          if (err instanceof HttpErrorResponse && err.status === 404) {
            return of(saveLocalCreditRequest(clientRef, request));
          }
          return throwError(() => err);
        }),
      );
  }

  creditRequests(
    query: PageQuery & {
      clientRef?: string;
      status?: CreditRequestStatus | '';
      paymentMethod?: CreditPaymentMethod | '';
      search?: string;
    } = {},
  ): Observable<PageResponse<CreditTopUpRequest>> {
    return this.http
      .get<PageResponse<CreditTopUpRequest>>(`${BILLING}/credit-requests`, { params: toParams(query) })
      .pipe(
        catchError((err) => {
          if (err instanceof HttpErrorResponse && err.status === 404) {
            return of(queryLocalCreditRequests(query));
          }
          return throwError(() => err);
        }),
      );
  }

  approveCreditRequest(id: string, request: CreditTopUpApprovalRequest): Observable<CreditTopUpRequest> {
    return this.http
      .post<CreditTopUpRequest>(`${BILLING}/credit-requests/${encodeURIComponent(id)}/approve`, request)
      .pipe(
        catchError((err) => {
          if (err instanceof HttpErrorResponse && err.status === 404) {
            const local = findLocalCreditRequest(id);
            if (!local) return throwError(() => err);
            const method = request.paymentMethod ?? local.paymentMethod;
            const reference = request.paymentReference?.trim() || local.paymentReference;
            const notes = [local.notes, request.reviewNotes ? `Approved: ${request.reviewNotes}` : null]
              .filter(Boolean)
              .join(' | ');
            return this.loadCredit(local.clientRef, {
              amount: request.approvedAmount,
              paymentMethod: method,
              paymentReference: reference,
              payerName: local.payerName,
              payerPhone: local.payerPhone,
              receivedBy: request.reviewedBy,
              notes: notes || undefined,
            }).pipe(
              map((loaded) =>
                updateLocalCreditRequest(id, {
                  status: 'APPROVED',
                  approvedAmount: request.approvedAmount,
                  paymentMethod: method,
                  paymentReference: loaded.paymentReference ?? reference,
                  reviewedAt: new Date().toISOString(),
                  reviewedBy: request.reviewedBy,
                  reviewNotes: request.reviewNotes,
                  creditLoadId: loaded.id,
                }),
              ),
            );
          }
          return throwError(() => err);
        }),
      );
  }

  rejectCreditRequest(id: string, request: CreditTopUpRejectionRequest): Observable<CreditTopUpRequest> {
    return this.http
      .post<CreditTopUpRequest>(`${BILLING}/credit-requests/${encodeURIComponent(id)}/reject`, request)
      .pipe(
        catchError((err) => {
          if (err instanceof HttpErrorResponse && err.status === 404) {
            const updated = updateLocalCreditRequest(id, {
              status: 'REJECTED',
              reviewedAt: new Date().toISOString(),
              reviewedBy: request.reviewedBy,
              reviewNotes: request.reason,
            });
            return of(updated);
          }
          return throwError(() => err);
        }),
      );
  }

  billingActivities(
    query: PageQuery & {
      clientRef?: string;
      type?: BillingActivityType | '';
      transactionRef?: string;
      from?: string;
      to?: string;
    } = {},
  ): Observable<PageResponse<BillingActivity>> {
    return this.http.get<PageResponse<BillingActivity>>(`${BILLING}/activities`, { params: toParams(query) });
  }
}

const CLIENT_USERS = '/api/v1/users/client-users';
const BILLING_PLANS = '/api/v1/configurations/billing/plans';
const BILLING = '/api/v1/payments/billing';
const VEHICLE_SERVICES = '/api/v1/client-transactions/vehicle-services';

/** /api/v1/clients/{clientId}/staff[/{staffId}] */
function staffUrl(clientId: string, staffId?: string): string {
  const base = `/api/v1/clients/${encodeURIComponent(clientId)}/staff`;
  return staffId ? `${base}/${encodeURIComponent(staffId)}` : base;
}

/** Drops empty values so the backend only receives filters that are actually set. */
function toParams(query: object): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params = params.set(key, String(value));
    }
  }
  return params;
}

const LOCAL_CREDIT_REQUESTS_KEY = 'swz.creditTopUpRequests';

function readLocalCreditRequests(): CreditTopUpRequest[] {
  try {
    const raw = localStorage.getItem(LOCAL_CREDIT_REQUESTS_KEY);
    return raw ? (JSON.parse(raw) as CreditTopUpRequest[]) : [];
  } catch {
    return [];
  }
}

function writeLocalCreditRequests(list: CreditTopUpRequest[]): void {
  try {
    localStorage.setItem(LOCAL_CREDIT_REQUESTS_KEY, JSON.stringify(list));
  } catch {
    // ignore storage errors
  }
}

function saveLocalCreditRequest(clientRef: string, req: CreditTopUpRequestCreate): CreditTopUpRequest {
  const item: CreditTopUpRequest = {
    id: `${Date.now()}`,
    clientRef,
    requestedAmount: req.requestedAmount,
    currency: 'UGX',
    paymentMethod: req.paymentMethod,
    paymentReference: req.paymentReference?.trim() ? req.paymentReference.trim().toUpperCase() : undefined,
    payerName: req.payerName?.trim() || undefined,
    payerPhone: req.payerPhone?.trim() || undefined,
    notes: req.notes?.trim() || undefined,
    status: 'PENDING',
    createdAt: new Date().toISOString(),
  };
  const updated = [item, ...readLocalCreditRequests()].slice(0, 100);
  writeLocalCreditRequests(updated);
  return item;
}

function findLocalCreditRequest(id: string): CreditTopUpRequest | undefined {
  return readLocalCreditRequests().find((r) => r.id === id);
}

function updateLocalCreditRequest(id: string, patch: Partial<CreditTopUpRequest>): CreditTopUpRequest {
  const list = readLocalCreditRequests();
  const idx = list.findIndex((r) => r.id === id);
  if (idx === -1) {
    throw new Error('Credit request not found');
  }
  const updated: CreditTopUpRequest = { ...list[idx]!, ...patch };
  list[idx] = updated;
  writeLocalCreditRequests(list);
  return updated;
}

function queryLocalCreditRequests(
  query: PageQuery & {
    clientRef?: string;
    status?: CreditRequestStatus | '';
    paymentMethod?: CreditPaymentMethod | '';
    search?: string;
  },
): PageResponse<CreditTopUpRequest> {
  let list = readLocalCreditRequests();
  if (query.clientRef) {
    list = list.filter((r) => r.clientRef === query.clientRef);
  }
  if (query.status) {
    list = list.filter((r) => r.status === query.status);
  }
  if (query.paymentMethod) {
    list = list.filter((r) => r.paymentMethod === query.paymentMethod);
  }
  if (query.search?.trim()) {
    const q = query.search.trim().toLowerCase();
    list = list.filter(
      (r) =>
        r.paymentReference?.toLowerCase().includes(q) ||
        r.payerName?.toLowerCase().includes(q) ||
        r.payerPhone?.toLowerCase().includes(q),
    );
  }
  const page = query.page ?? 0;
  const size = query.size ?? 10;
  const start = page * size;
  const content = list.slice(start, start + size);
  const totalPages = Math.max(1, Math.ceil(list.length / size));
  return {
    content,
    page,
    size,
    totalElements: list.length,
    totalPages,
    last: page >= totalPages - 1,
  };
}
