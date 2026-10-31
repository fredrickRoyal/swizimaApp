import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_BASE_URL } from '../config/api.config';

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
      setHeaders: { 'X-Request-Id': crypto.randomUUID() },
    }),
  );
};
