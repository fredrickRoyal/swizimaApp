import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ActivationStatus,
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
      search?: string;
    } = {},
  ): Observable<PageResponse<VehicleServiceTransaction>> {
    return this.http.get<PageResponse<VehicleServiceTransaction>>(VEHICLE_SERVICES, { params: toParams(query) });
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

  users(
    query: PageQuery & { search?: string; activationStatus?: ActivationStatus | ''; enabled?: boolean | '' } = {},
  ): Observable<PageResponse<SystemUser>> {
    return this.http.get<PageResponse<SystemUser>>('/api/v1/users', { params: toParams(query) });
  }
}

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
