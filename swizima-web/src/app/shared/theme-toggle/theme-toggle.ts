import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ThemeService } from '../../core/theme/theme.service';
import { Icon } from '../icon/icon';

/**
 * Button that toggles between Light and Dark themes.
 * Can render as a compact icon button (default) or with a text label (`[showLabel]="true"`).
 */
@Component({
  selector: 'app-theme-toggle',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      type="button"
      class="theme-toggle"
      [class.with-label]="showLabel()"
      (click)="themeService.toggle()"
      [attr.aria-label]="themeService.isDark() ? 'Switch to light theme' : 'Switch to dark theme'"
      [title]="themeService.isDark() ? 'Switch to light theme' : 'Switch to dark theme'"
    >
      <app-icon [name]="themeService.isDark() ? 'sun' : 'moon'" />
      @if (showLabel()) {
        <span>{{ themeService.isDark() ? 'Light mode' : 'Dark mode' }}</span>
      }
    </button>
  `,
  styles: `
    :host {
      display: inline-flex;
    }
    .theme-toggle {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.45rem;
      min-height: 40px;
      width: 40px;
      padding: 0;
      border-radius: var(--radius);
      border: 1px solid var(--border);
      background: var(--bg-elevated);
      color: var(--text);
      font-size: 0.85rem;
      font-weight: 600;
      transition: background-color var(--duration) var(--ease), border-color var(--duration) var(--ease),
        color var(--duration) var(--ease), transform var(--duration) var(--ease);

      &:hover {
        background: var(--bg-hover);
        border-color: var(--border-strong);
      }
      &:active {
        transform: translateY(1px);
      }
      app-icon {
        width: 18px;
        height: 18px;
        color: var(--brand-500);
      }
    }
    .theme-toggle.with-label {
      width: auto;
      padding: 0.45rem 0.8rem;
    }
  `,
})
export class ThemeToggle {
  protected readonly themeService = inject(ThemeService);
  readonly showLabel = input<boolean>(false);
}

