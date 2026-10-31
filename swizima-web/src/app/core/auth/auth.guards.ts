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

/** Keeps signed-in users away from login/OTP pages. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? inject(Router).createUrlTree(['/dashboard']) : true;
};

/** The OTP page only makes sense after a successful password step. */
export const otpChallengeGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.challenge() ? true : inject(Router).createUrlTree(['/auth/login']);
};
