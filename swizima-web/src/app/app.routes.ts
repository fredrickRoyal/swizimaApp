import { Routes } from '@angular/router';
import { authGuard, guestGuard, otpChallengeGuard } from './core/auth/auth.guards';
import { unsavedChangesGuard } from './core/forms/unsaved-changes.guard';

export const routes: Routes = [
  {
    path: 'auth',
    loadComponent: () => import('./features/auth/auth-layout/auth-layout').then((m) => m.AuthLayout),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'login' },
      {
        path: 'login',
        title: 'Sign in',
        canActivate: [guestGuard],
        loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
      },
      {
        path: 'verify-otp',
        title: 'Verify code',
        canActivate: [guestGuard, otpChallengeGuard],
        loadComponent: () => import('./features/auth/verify-otp/verify-otp').then((m) => m.VerifyOtp),
      },
    ],
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Dashboard',
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'clients',
        title: 'Clients',
        loadComponent: () => import('./features/clients/clients').then((m) => m.Clients),
      },
      {
        path: 'clients/new',
        title: 'Register client',
        canDeactivate: [unsavedChangesGuard],
        loadComponent: () => import('./features/clients/client-form/client-form').then((m) => m.ClientForm),
      },
      {
        path: 'services',
        title: 'Services offered',
        loadComponent: () => import('./features/services/services').then((m) => m.Services),
      },
      {
        path: 'payments',
        title: 'Payments',
        loadComponent: () => import('./features/payments/payments').then((m) => m.Payments),
      },
      {
        path: 'users',
        title: 'Users',
        loadComponent: () => import('./features/users/users').then((m) => m.Users),
      },
      {
        path: 'configurations',
        title: 'Configurations',
        loadComponent: () => import('./features/configurations/configurations').then((m) => m.Configurations),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
