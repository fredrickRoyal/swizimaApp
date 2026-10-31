import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ActivationStatus,
  Client,
  ClientRequest,
  ClientStatus,
  PageResponse,
  ServiceCatalogItem,
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

  users(
    query: PageQuery & { search?: string; activationStatus?: ActivationStatus | ''; enabled?: boolean | '' } = {},
  ): Observable<PageResponse<SystemUser>> {
    return this.http.get<PageResponse<SystemUser>>('/api/v1/users', { params: toParams(query) });
  }
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
