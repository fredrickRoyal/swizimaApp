import { Routes } from '@angular/router';
import { authGuard, guestGuard, nonStaffOfficerGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Sign in · SOOTR',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Dashboard · SOOTR',
        canActivate: [nonStaffOfficerGuard],
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'out-of-office',
        title: 'Out-of-Office Notifications · SOOTR',
        loadComponent: () =>
          import('./features/out-of-office/out-of-office').then((m) => m.OutOfOffice),
      },
      {
        path: 'out-of-office/new',
        title: 'Record Out-of-Office Notification · SOOTR',
        loadComponent: () =>
          import('./features/out-of-office/out-of-office-form/out-of-office-form').then(
            (m) => m.OutOfOfficeForm
          ),
      },
      {
        path: 'out-of-office/:id/edit',
        title: 'Update Out-of-Office Notification · SOOTR',
        loadComponent: () =>
          import('./features/out-of-office/out-of-office-form/out-of-office-form').then(
            (m) => m.OutOfOfficeForm
          ),
      },
      {
        path: 'out-of-office/:id/report-back',
        title: 'Report Back & Management Action · SOOTR',
        loadComponent: () =>
          import('./features/out-of-office/report-back/report-back').then((m) => m.ReportBack),
      },
      {
        path: 'out-of-office/:id/view-report',
        title: 'View Activity Report (Section A & B) · SOOTR',
        loadComponent: () =>
          import('./features/out-of-office/view-report/view-report').then((m) => m.ViewReport),
      },
      {
        path: 'duty-tracker',
        redirectTo: 'out-of-office',
        pathMatch: 'full',
      },
      {
        path: 'staff-register',
        title: 'Staff Register · SOOTR',
        canActivate: [nonStaffOfficerGuard],
        loadComponent: () =>
          import('./features/staff-register/staff-register').then((m) => m.StaffRegister),
      },
      {
        path: 'organisations',
        title: 'Organisations · SOOTR',
        canActivate: [nonStaffOfficerGuard],
        loadComponent: () =>
          import('./features/organisations/organisations').then((m) => m.Organisations),
      },
      {
        path: 'clients',
        redirectTo: 'organisations',
        pathMatch: 'full',
      },
      {
        path: 'activity-reports',
        redirectTo: 'staff-register',
        pathMatch: 'full',
      },
      {
        path: 'weekly-reports',
        title: 'Weekly Activity Reports · SOOTR',
        canActivate: [nonStaffOfficerGuard],
        loadComponent: () =>
          import('./features/weekly-reports/weekly-reports').then((m) => m.WeeklyReports),
      },
      {
        path: 'user-management',
        title: 'User Management · SOOTR',
        canActivate: [nonStaffOfficerGuard],
        loadComponent: () =>
          import('./features/user-management/user-management').then((m) => m.UserManagement),
      },
      {
        path: 'users',
        redirectTo: 'user-management',
        pathMatch: 'full',
      },
      {
        path: 'configurations',
        title: 'Configurations · SOOTR',
        canActivate: [nonStaffOfficerGuard],
        loadComponent: () =>
          import('./features/configurations/configurations').then((m) => m.Configurations),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
