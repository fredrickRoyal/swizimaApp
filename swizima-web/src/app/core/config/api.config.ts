import { InjectionToken } from '@angular/core';

/**
 * Base URL prepended to every `/api/...` request.
 * - Development: '' – the Angular dev server proxies /api to the API gateway (proxy.conf.json).
 * - Production: '' when the web app is served behind the same domain as the gateway,
 *   otherwise e.g. 'https://api.swizima.com'.
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => '',
});
