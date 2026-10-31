import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Icon } from '../icon/icon';

/** "Showing 1–20 of 134" + previous/next buttons. Pages are zero-based (Spring Data). */
@Component({
  selector: 'app-pagination',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p class="summary">
      @if (total() > 0) {
        Showing <strong>{{ from() }}–{{ to() }}</strong> of <strong>{{ total() }}</strong>
      } @else {
        No results
      }
    </p>
    <div class="controls">
      <button type="button" class="btn btn-icon" (click)="pageChange.emit(page() - 1)"
              [disabled]="page() <= 0" aria-label="Previous page">
        <app-icon name="chevronLeft" />
      </button>
      <span class="page">Page {{ page() + 1 }} of {{ pages() }}</span>
      <button type="button" class="btn btn-icon" (click)="pageChange.emit(page() + 1)"
              [disabled]="page() + 1 >= pages()" aria-label="Next page">
        <app-icon name="chevronRight" />
      </button>
    </div>
  `,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.85rem 1.25rem;
      border-top: 1px solid var(--border);
      font-size: 0.875rem;
      color: var(--text-muted);
    }
    strong { color: var(--text); font-weight: 600; }
    .controls { display: flex; align-items: center; gap: 0.5rem; }
    .page { min-width: 6.5rem; text-align: center; }
  `,
})
export class Pagination {
  readonly page = input.required<number>();
  readonly size = input.required<number>();
  readonly total = input.required<number>();
  readonly pageChange = output<number>();

  protected readonly pages = computed(() => Math.max(1, Math.ceil(this.total() / this.size())));
  protected readonly from = computed(() => (this.total() === 0 ? 0 : this.page() * this.size() + 1));
  protected readonly to = computed(() => Math.min(this.total(), (this.page() + 1) * this.size()));
}
