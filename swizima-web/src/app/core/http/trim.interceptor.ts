import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Trims leading/trailing whitespace from every string in JSON request bodies
 * (recursively). Keys containing "password" are left as typed.
 */
export const trimInterceptor: HttpInterceptorFn = (req, next) => {
  const body = req.body;
  if (body === null || typeof body !== 'object' || body instanceof FormData || body instanceof Blob) {
    return next(req);
  }
  return next(req.clone({ body: trimDeep(body) }));
};

function trimDeep(value: unknown, key = ''): unknown {
  if (typeof value === 'string') return /password/i.test(key) ? value : value.trim();
  if (Array.isArray(value)) return value.map((v) => trimDeep(v, key));
  if (value && typeof value === 'object' && Object.getPrototypeOf(value) === Object.prototype) {
    return Object.fromEntries(Object.entries(value).map(([k, v]) => [k, trimDeep(v, k)]));
  }
  return value;
}
