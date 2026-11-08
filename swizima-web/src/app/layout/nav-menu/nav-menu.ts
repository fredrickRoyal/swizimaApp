import { ChangeDetectionStrategy, Component, computed, inject, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { MOBILE_APP_DOWNLOAD_URL } from '../../core/config/api.config';
import { Icon } from '../../shared/icon/icon';
import { CLIENT_PORTAL_NAV_GROUPS, NAV_GROUPS } from '../nav-items';

/**
 * Brand + grouped links + signed-in user. Rendered both in the desktop sidebar
 * and inside the mobile drawer, so the markup exists in one place only.
 */
@Component({
  selector: 'app-nav-menu',
  imports: [RouterLink, RouterLinkActive, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './nav-menu.html',
  styleUrl: './nav-menu.css',
})
export class NavMenu {
  private readonly auth = inject(AuthService);

  /** Emitted when a link is chosen (mobile drawer closes) */
  readonly navigate = output<void>();
  readonly signOut = output<void>();

  protected readonly groups = computed(() =>
    this.auth.isClientPortalUser() ? CLIENT_PORTAL_NAV_GROUPS : NAV_GROUPS,
  );
  protected readonly roleLabel = computed(() => {
    const cat = this.auth.accountCategory();
    if (cat === 'SUPER_ADMIN') return 'Super Admin';
    if (cat === 'CLIENT_ADMIN') return 'Client Admin';
    return 'User';
  });
  protected readonly mobileAppUrl = inject(MOBILE_APP_DOWNLOAD_URL);
  protected readonly username = this.auth.displayName;
  protected readonly initials = computed(() => initialsOf(this.username()));
}

function initialsOf(username: string): string {
  const base = username.split('@')[0].replace(/[^a-zA-Z0-9]+/g, ' ').trim();
  if (!base) return 'SW';
  const parts = base.split(' ');
  return (parts.length > 1 ? parts[0][0] + parts[1][0] : base.slice(0, 2)).toUpperCase();
}
