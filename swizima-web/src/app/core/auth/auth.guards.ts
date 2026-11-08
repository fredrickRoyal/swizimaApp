import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Protects the app shell: unauthenticated users go to the login page. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.isAuthenticated()
    ? true
    : inject(Router).createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } });
};

/** Keeps signed-in users away from login/OTP/signup pages. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? inject(Router).createUrlTree([auth.defaultHomeUrl()]) : true;
};

/** Protects platform-admin routes: redirects client-only users to their Client Portal. */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isClientPortalUser()) {
    return inject(Router).createUrlTree(['/portal/dashboard']);
  }
  return true;
};

/** Protects the client-only portal module (/portal/**). */
export const clientPortalGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (!auth.isAuthenticated()) {
    return inject(Router).createUrlTree(['/auth/login']);
  }
  return auth.clientId() ? true : inject(Router).createUrlTree(['/dashboard']);
};

/** The OTP page only makes sense after a successful password or OTP step. */
export const otpChallengeGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.challenge() ? true : inject(Router).createUrlTree(['/auth/login']);
};
