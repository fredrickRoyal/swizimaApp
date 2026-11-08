import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_BASE_URL } from '../config/api.config';

/**
 * Generates a UUID v4 request ID that works in both secure (HTTPS / localhost)
 * and non-secure (plain HTTP over IP/hostname) browser contexts.
 */
function newRequestId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    const bytes = new Uint8Array(16);
    crypto.getRandomValues(bytes);
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
  }
  return `${Date.now().toString(16)}-${Math.random().toString(16).slice(2, 10)}`;
}

/**
 * Prefixes relative `/api/...` URLs with the configured base URL and tags each
 * request with an X-Request-Id so calls can be traced through the gateway logs.
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const baseUrl = inject(API_BASE_URL);
  if (!req.url.startsWith('/api/')) {
    return next(req);
  }
  return next(
    req.clone({
      url: `${baseUrl}${req.url}`,
      setHeaders: { 'X-Request-Id': newRequestId() },
    }),
  );
};
