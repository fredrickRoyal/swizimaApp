import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from '../models/api.models';

/**
 * Turns any HTTP error into a short, user-friendly message.
 * Understands the RFC 9457 ProblemDetail bodies returned by the Swizima services.
 */
export function toErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }
  if (error.status === 0) {
    return 'Cannot reach the server. Check your connection or that the API gateway is running.';
  }

  const body = error.error as ProblemDetail | string | null;
  if (body && typeof body === 'object') {
    if (body.errors && Object.keys(body.errors).length) {
      return Object.values(body.errors).join(' ');
    }
    if (body.detail) return body.detail;
    if (body.title) return body.title;
  }

  switch (error.status) {
    case 401: return 'Your session has expired. Please sign in again.';
    case 403: return 'You do not have permission to perform this action.';
    case 404: return 'The requested resource was not found.';
    case 429: return 'Too many requests. Please wait a moment and try again.';
    case 503: return 'The service is temporarily unavailable. Please try again shortly.';
    default: return fallback;
  }
}
