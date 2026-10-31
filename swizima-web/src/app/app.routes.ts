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
        path: 'clients/:id',
        loadComponent: () => import('./features/clients/client-layout/client-layout').then((m) => m.ClientLayout),
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'details' },
          {
            path: 'details',
            title: 'Client details',
            loadComponent: () => import('./features/clients/client-details/client-details').then((m) => m.ClientDetails),
          },
          {
            path: 'dashboard',
            title: 'Client dashboard',
            loadComponent: () => import('./features/clients/client-dashboard/client-dashboard').then((m) => m.ClientDashboard),
          },
          {
            path: 'services',
            title: 'Service catalog',
            loadComponent: () => import('./features/clients/client-services/client-services').then((m) => m.ClientServices),
          },
          {
            path: 'staff',
            title: 'Staff',
            loadComponent: () => import('./features/clients/client-staff/client-staff').then((m) => m.ClientStaffPage),
          },
          {
            path: 'transactions',
            title: 'Transactions',
            loadComponent: () =>
              import('./features/clients/client-transactions/client-transactions').then((m) => m.ClientTransactionsPage),
          },
        ],
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
