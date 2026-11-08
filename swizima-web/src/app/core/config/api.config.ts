import { InjectionToken } from '@angular/core';

interface SwizimaRuntimeEnv {
  mobileAppDownloadUrl?: string;
}

const DEFAULT_MOBILE_APP_DOWNLOAD_URL =
  'https://drive.google.com/drive/folders/1cLdYhVLrO4Oyh1xLOhFIJIWCW-99WSg4?usp=sharing';

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

/**
 * Mobile App (Android APK) download URL.
 * Overridable at container startup via the `MOBILE_APP_DOWNLOAD_URL` environment variable
 * (written to `/env.js` by Nginx's entrypoint).
 */
export const MOBILE_APP_DOWNLOAD_URL = new InjectionToken<string>('MOBILE_APP_DOWNLOAD_URL', {
  providedIn: 'root',
  factory: () => {
    const env = (globalThis as { __SWIZIMA_ENV__?: SwizimaRuntimeEnv }).__SWIZIMA_ENV__;
    return env?.mobileAppDownloadUrl?.trim() || DEFAULT_MOBILE_APP_DOWNLOAD_URL;
  },
});
