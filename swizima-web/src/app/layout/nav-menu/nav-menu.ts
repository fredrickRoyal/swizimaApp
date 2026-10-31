import { ChangeDetectionStrategy, Component, computed, inject, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { Icon } from '../../shared/icon/icon';
import { NAV_GROUPS } from '../nav-items';

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

  protected readonly groups = NAV_GROUPS;
  protected readonly username = this.auth.displayName;
  protected readonly initials = computed(() => initialsOf(this.username()));
}

function initialsOf(username: string): string {
  const base = username.split('@')[0].replace(/[^a-zA-Z0-9]+/g, ' ').trim();
  if (!base) return 'SW';
  const parts = base.split(' ');
  return (parts.length > 1 ? parts[0][0] + parts[1][0] : base.slice(0, 2)).toUpperCase();
}
