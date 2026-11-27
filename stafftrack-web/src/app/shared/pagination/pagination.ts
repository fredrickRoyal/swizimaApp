import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Icon } from '../icon/icon';

/** "Showing 1–10 of 20" + rows-per-page selector + previous/next buttons. Pages are zero-based. */
@Component({
  selector: 'app-pagination',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.7rem 1.25rem;
      border-top: 1px solid var(--border);
      background: var(--bg-elevated);
      font-size: 0.8rem;
      color: var(--text-muted);
    }
    strong {
      color: var(--text);
      font-weight: 600;
    }
    .summary {
      margin: 0;
    }
    .controls {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.85rem;
    }
    .size-picker {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      font-size: 0.82rem;
      color: var(--text-muted);
    }
    .size-select {
      height: 32px;
      min-height: 32px;
      padding: 0 1.75rem 0 0.6rem;
      border-radius: 6px;
      border: 1px solid var(--border-strong);
      background-color: var(--bg-surface);
      color: var(--text);
      font: inherit;
      font-size: 0.82rem;
      font-weight: 600;
      cursor: pointer;
    }
    .size-select:focus-visible {
      outline: none;
      border-color: var(--brand);
      box-shadow: 0 0 0 3px var(--brand-ring);
    }
    .nav-group {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
    }
    .page-label {
      min-width: 6.5rem;
      text-align: center;
      font-weight: 500;
    }
    .nav-btn {
      width: 32px;
      height: 32px;
      min-height: 32px;
      padding: 0;
    }
    .nav-btn:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }
  `,
  template: `
    <p class="summary">
      @if (total() > 0) {
        Showing <strong>{{ from() }}–{{ to() }}</strong> of <strong>{{ total() }}</strong>
      } @else {
        No results
      }
    </p>
    <div class="controls">
      <label class="size-picker">
        <span>Rows per page</span>
        <select
          class="size-select"
          aria-label="Rows per page"
          [value]="size()"
          (change)="onSizeChange($event)"
        >
          @for (opt of sizeOptions(); track opt) {
            <option [value]="opt" [selected]="opt === size()">{{ opt }}</option>
          }
        </select>
      </label>

      <div class="nav-group">
        <button
          type="button"
          class="btn btn-sm nav-btn"
          (click)="pageChange.emit(safePage() - 1)"
          [disabled]="safePage() <= 0"
          aria-label="Previous page"
          title="Previous page"
        >
          <app-icon name="chevronLeft" />
        </button>
        <span class="page-label">Page {{ safePage() + 1 }} of {{ pages() }}</span>
        <button
          type="button"
          class="btn btn-sm nav-btn"
          (click)="pageChange.emit(safePage() + 1)"
          [disabled]="safePage() + 1 >= pages()"
          aria-label="Next page"
          title="Next page"
        >
          <app-icon name="chevronRight" />
        </button>
      </div>
    </div>
  `,
})
export class Pagination {
  readonly page = input.required<number>();
  readonly size = input<number>(10);
  readonly total = input.required<number>();
  readonly sizeOptions = input<readonly number[]>([5, 10, 20, 50, 100]);
  readonly pageChange = output<number>();
  readonly sizeChange = output<number>();

  protected readonly pages = computed(() => Math.max(1, Math.ceil(this.total() / this.size())));
  protected readonly safePage = computed(() =>
    Math.min(Math.max(0, this.page()), this.pages() - 1)
  );
  protected readonly from = computed(() =>
    this.total() === 0 ? 0 : this.safePage() * this.size() + 1
  );
  protected readonly to = computed(() =>
    Math.min(this.total(), (this.safePage() + 1) * this.size())
  );

  protected onSizeChange(event: Event): void {
    const next = Number((event.target as HTMLSelectElement).value);
    if (!Number.isNaN(next) && next > 0) {
      this.sizeChange.emit(next);
      this.pageChange.emit(0);
    }
  }
}
