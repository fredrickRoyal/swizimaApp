import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Page title block with optional actions projected on the right. */
@Component({
  selector: 'app-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="text">
      <h1>{{ heading() }}</h1>
      @if (subtitle()) {
        <p>{{ subtitle() }}</p>
      }
    </div>
    <div class="actions"><ng-content /></div>
  `,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-end;
      justify-content: space-between;
      gap: 1rem;
      margin-block-end: 1.5rem;
    }
    p { margin-top: 0.25rem; color: var(--text-muted); }
    .actions { display: flex; gap: 0.5rem; flex-wrap: wrap; }
    .actions:empty { display: none; }
  `,
})
export class PageHeader {
  readonly heading = input.required<string>();
  readonly subtitle = input<string>();
}
