import { ChangeDetectionStrategy, Component, HostListener, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import {
  ENGAGEMENT_TYPE_LABELS,
  OutOfOfficeNotification,
} from '../../core/models/stafftrack.models';
import { AuthService } from '../../core/services/auth.service';
import { StaffTrackStore } from '../../core/services/stafftrack.store';
import { ThemeService } from '../../core/services/theme.service';
import { Icon, IconName } from '../../shared/icon/icon';

interface NavItem {
  path: string;
  label: string;
  subtitle: string;
  icon: IconName;
  badgeType?: 'actions' | 'away' | 'duty' | 'staff' | 'orgs' | 'users';
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shell {
  protected readonly auth = inject(AuthService);
  protected readonly store = inject(StaffTrackStore);
  protected readonly theme = inject(ThemeService);
  private readonly router = inject(Router);
  protected readonly mobileDrawerOpen = signal(false);
  protected readonly desktopSidebarHidden = signal(false);
  protected readonly notificationsOpen = signal(false);
  protected readonly engagementLabels = ENGAGEMENT_TYPE_LABELS;

  protected readonly newNotifications = computed<OutOfOfficeNotification[]>(() =>
    this.store
      .outOfOfficeList()
      .filter(
        (item) =>
          item.status !== 'CANCELLED' &&
          (item.supervisorConfirmationStatus || 'NEW') === 'NEW' &&
          this.auth.canViewOutOfOfficeRecord(item)
      )
  );

  protected readonly visibleCurrentlyAwayCount = computed<number>(() =>
    this.store
      .outOfOfficeList()
      .filter(
        (item) =>
          item.status === 'ACTIVE_AWAY' &&
          this.auth.canViewOutOfOfficeRecord(item)
      ).length
  );

  protected readonly allNavItems: NavItem[] = [
    {
      path: '/dashboard',
      label: 'Dashboard',
      subtitle: 'Who is away & summary metrics',
      icon: 'dashboard',
    },
    {
      path: '/out-of-office',
      label: 'Out-of-Office Notifications',
      subtitle: 'Travel, meetings & field trips',
      icon: 'plane',
      badgeType: 'away',
    },
    {
      path: '/weekly-reports',
      label: 'Weekly Activity Reports',
      subtitle: 'Consolidated weekly digests',
      icon: 'calendar',
    },
    {
      path: '/staff-register',
      label: 'Staff Register',
      subtitle: 'Staff, departments & stations',
      icon: 'users',
      badgeType: 'staff',
    },
    {
      path: '/organisations',
      label: 'Organisations',
      subtitle: 'Client organisations & contacts',
      icon: 'building',
      badgeType: 'orgs',
    },
    {
      path: '/user-management',
      label: 'User Management',
      subtitle: 'Portal users, roles & OTP access',
      icon: 'shield',
      badgeType: 'users',
    },
    {
      path: '/configurations',
      label: 'Configurations',
      subtitle: 'Supervisors notifications & billing',
      icon: 'settings',
    },
  ];

  protected readonly navItems = computed<NavItem[]>(() => {
    if (this.auth.isStaffOfficer()) {
      return this.allNavItems.filter((item) => item.path === '/out-of-office');
    }
    return this.allNavItems;
  });

  protected toggleMenu(): void {
    if (typeof window !== 'undefined' && window.innerWidth >= 1024) {
      this.desktopSidebarHidden.update((hidden) => !hidden);
    } else {
      this.mobileDrawerOpen.update((open) => !open);
    }
  }

  protected toggleDesktopSidebar(): void {
    this.desktopSidebarHidden.update((hidden) => !hidden);
  }

  protected toggleDrawer(): void {
    this.mobileDrawerOpen.update((open) => !open);
  }

  protected closeDrawer(): void {
    this.mobileDrawerOpen.set(false);
  }

  protected toggleNotifications(): void {
    const next = !this.notificationsOpen();
    this.notificationsOpen.set(next);
    if (next) {
      this.store.refreshFromApi();
    }
  }

  protected closeNotifications(): void {
    this.notificationsOpen.set(false);
  }

  protected viewAndMarkNotificationRead(item: OutOfOfficeNotification): void {
    const actor = this.auth.session()?.fullName || 'Supervisor';
    this.store.markNotificationAsRead(item.id, actor, true);
    this.notificationsOpen.set(false);
    void this.router.navigate(['/out-of-office', item.id, 'view-report']);
  }

  protected markAllNotificationsRead(): void {
    const actor = this.auth.session()?.fullName || 'Supervisor';
    for (const item of this.newNotifications()) {
      this.store.markNotificationAsRead(item.id, actor, true);
    }
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    if (this.mobileDrawerOpen()) {
      this.closeDrawer();
    }
    if (this.notificationsOpen()) {
      this.closeNotifications();
    }
  }
}

