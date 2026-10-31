import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { AppTitleStrategy } from '../../core/title/app-title.strategy';
import { Icon } from '../../shared/icon/icon';
import { NavMenu } from '../nav-menu/nav-menu';

/**
 * Authenticated app frame.
 *  - ≥1024px: fixed left sidebar + sticky top bar.
 *  - <1024px: top bar with a hamburger that opens a swipeable drawer
 *    (popover + scroll-snap; see modern-web-guidance "navigation drawer").
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, NavMenu, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell implements AfterViewInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly pageTitle = inject(AppTitleStrategy).pageTitle;
  protected readonly username = this.auth.displayName;
  protected readonly drawerOpen = signal(false);
  protected readonly greeting = computed(() => {
    const hour = new Date().getHours();
    return hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  });

  private readonly drawer = viewChild.required<ElementRef<HTMLElement>>('drawer');
  private readonly scroller = viewChild.required<ElementRef<HTMLElement>>('scroller');
  private readonly sheet = viewChild.required<ElementRef<HTMLElement>>('sheet');
  private readonly main = viewChild.required<ElementRef<HTMLElement>>('main');
  private readonly menuButton = viewChild.required<ElementRef<HTMLButtonElement>>('menuButton');

  ngAfterViewInit(): void {
    const drawer = this.drawer().nativeElement;
    const scroller = this.scroller().nativeElement;
    const sheet = this.sheet().nativeElement;

    const visibleThreshold = 1 / window.innerWidth;
    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries.at(-1);
        if (!entry) return;
        if (entry.intersectionRatio < visibleThreshold) this.onClosed();
        if (entry.intersectionRatio === 1) this.onOpened();
      },
      { root: drawer, threshold: [visibleThreshold, 1] },
    );
    observer.observe(sheet);

    // Backdrop fade fallback for browsers without scroll-driven animations.
    let onScroll: (() => void) | undefined;
    if (!CSS.supports('animation-timeline: scroll()')) {
      onScroll = () => {
        const ratio = Math.max(0, 1 - scroller.scrollLeft / sheet.offsetWidth);
        drawer.style.setProperty('--drawer-backdrop', String(ratio));
      };
      scroller.addEventListener('scroll', onScroll, { passive: true });
    }

    const onKeydown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && this.drawerOpen()) this.closeDrawer();
    };
    document.addEventListener('keydown', onKeydown);

    // Growing to desktop width while the drawer is open: drop it immediately.
    const desktop = window.matchMedia('(min-width: 1024px)');
    const onBreakpoint = (event: MediaQueryListEvent) => {
      if (event.matches) this.onClosed();
    };
    desktop.addEventListener('change', onBreakpoint);

    this.destroyRef.onDestroy(() => {
      observer.disconnect();
      desktop.removeEventListener('change', onBreakpoint);
      document.removeEventListener('keydown', onKeydown);
      if (onScroll) scroller.removeEventListener('scroll', onScroll);
    });
  }

  protected async openDrawer(): Promise<void> {
    const drawer = this.drawer().nativeElement;
    const scroller = this.scroller().nativeElement;
    drawer.showPopover();
    if (!CSS.supports('scroll-initial-target', 'nearest')) {
      scroller.scrollTo({ left: scroller.offsetWidth, behavior: 'instant' });
      await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)));
    }
    scroller.scrollTo({ left: 0, behavior: 'auto' });
  }

  protected closeDrawer(): void {
    const scroller = this.scroller().nativeElement;
    scroller.scrollTo({ left: scroller.offsetWidth, behavior: 'auto' });
  }

  /** Light dismiss: a click on the dimmed area (outside the sheet). */
  protected onDrawerClick(event: MouseEvent): void {
    if (!this.sheet().nativeElement.contains(event.target as Node)) this.closeDrawer();
  }

  protected signOut(): void {
    if (this.drawerOpen()) this.closeDrawer();
    this.auth.logout();
    this.router.navigate(['/auth/login']);
  }

  private onOpened(): void {
    this.drawerOpen.set(true);
    this.main().nativeElement.inert = true;
    this.sheet().nativeElement.focus();
  }

  private onClosed(): void {
    const drawer = this.drawer().nativeElement;
    if (drawer.matches(':popover-open')) drawer.hidePopover();
    this.main().nativeElement.inert = false;
    if (this.drawerOpen()) {
      this.drawerOpen.set(false);
      this.menuButton().nativeElement.focus({ preventScroll: true });
    }
  }
}
