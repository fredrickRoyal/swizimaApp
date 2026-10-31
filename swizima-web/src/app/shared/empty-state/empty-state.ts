import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Icon, IconName } from '../icon/icon';

/** Friendly placeholder for empty lists and error states. */
@Component({
  selector: 'app-empty-state',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="icon" [class.error]="tone() === 'error'"><app-icon [name]="icon()" /></div>
    <h3>{{ heading() }}</h3>
    @if (message()) {
      <p>{{ message() }}</p>
    }
    <ng-content />
  `,
  styles: `
    :host {
      display: grid;
      justify-items: center;
      gap: 0.5rem;
      padding: 3rem 1.5rem;
      text-align: center;
    }
    .icon {
      display: grid;
      place-items: center;
      width: 56px;
      height: 56px;
      margin-bottom: 0.5rem;
      border-radius: 16px;
      background: color-mix(in srgb, var(--brand-500) 12%, transparent);
      color: var(--brand-500);
      app-icon { width: 26px; height: 26px; }
    }
    .icon.error { background: var(--danger-bg); color: var(--danger); }
    p { max-width: 42ch; color: var(--text-muted); font-size: 0.925rem; }
  `,
})
export class EmptyState {
  readonly icon = input<IconName>('inbox');
  readonly heading = input.required<string>();
  readonly message = input<string | null>();
  readonly tone = input<'default' | 'error'>('default');
}
