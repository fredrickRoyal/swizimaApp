import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
  input,
  model,
  signal,
} from '@angular/core';
import { SwizimaApi } from '../../core/api/swizima-api';
import { debounced, remoteData } from '../../core/data/remote-data';
import { Client } from '../../core/models/api.models';
import { Icon } from '../icon/icon';

let nextId = 0;

/**
 * Searchable client combobox. Queries GET /api/v1/clients?search=… as the user types
 * (debounced) and exposes the selection through the two-way `selected` model.
 */
@Component({
  selector: 'app-client-picker',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'onDocumentClick($event)',
  },
  template: `
    <div class="combo" [class.open]="open()">
      <app-icon name="clients" class="lead" />
      <input
        class="input"
        type="text"
        role="combobox"
        autocomplete="off"
        spellcheck="false"
        [id]="inputId()"
        [placeholder]="selected() ? '' : placeholder()"
        [value]="open() ? query() : (selected()?.clientName ?? '')"
        [attr.aria-expanded]="open()"
        [attr.aria-controls]="listId"
        [attr.aria-activedescendant]="open() && active() >= 0 ? optionId(active()) : null"
        aria-autocomplete="list"
        [attr.aria-label]="label()"
        (focus)="openList()"
        (click)="openList()"
        (input)="onInput($any($event.target).value)"
        (keydown)="onKeydown($event)"
      />
      @if (selected() && !open()) {
        <button type="button" class="clear" [id]="inputId() + '-clear'" (click)="clear($event)" aria-label="Clear client filter">
          <app-icon name="close" />
        </button>
      } @else {
        <app-icon name="search" class="trail" />
      }

      @if (open()) {
        <ul class="list" role="listbox" [id]="listId" [attr.aria-label]="label()">
          @let state = results.state();
          @if (state.error) {
            <li class="info error">{{ state.error }}</li>
          } @else if (state.loading && !state.data) {
            <li class="info">Searching…</li>
          } @else {
            <li role="option" class="option all" [id]="optionId(-1)" [class.active]="active() === -1"
                [attr.aria-selected]="!selected()" (mousedown)="$event.preventDefault()" (click)="choose(null)">
              <span class="name">All clients</span>
            </li>
            @for (c of options(); track c.id; let i = $index) {
              <li role="option" class="option" [id]="optionId(i)" [class.active]="active() === i"
                  [attr.aria-selected]="selected()?.id === c.id" (mousedown)="$event.preventDefault()"
                  (mouseenter)="active.set(i)" (click)="choose(c)">
                <span class="name">{{ c.clientName }}</span>
                <span class="code">{{ c.clientCode }}</span>
              </li>
            } @empty {
              <li class="info">No clients match “{{ query() }}”.</li>
            }
            @if (state.data && state.data.totalElements > options().length) {
              <li class="info hint">Showing {{ options().length }} of {{ state.data.totalElements }} — keep typing to narrow down.</li>
            }
          }
        </ul>
      }
    </div>
  `,
  styles: `
    :host { display: block; position: relative; min-width: 240px; }
    .combo { position: relative; }
    .input { padding-inline: 2.4rem 2.4rem; text-overflow: ellipsis; }
    .lead, .trail {
      position: absolute; top: 50%; transform: translateY(-50%);
      width: 18px; height: 18px; color: var(--text-subtle); pointer-events: none;
    }
    .lead { left: 0.8rem; }
    .trail { right: 0.8rem; }
    .clear {
      position: absolute; right: 0.4rem; top: 50%; transform: translateY(-50%);
      display: grid; place-items: center; width: 30px; height: 30px; border: 0; border-radius: 8px;
      background: transparent; color: var(--text-muted); cursor: pointer;
      app-icon { width: 16px; height: 16px; }
      &:hover { background: var(--bg-hover); color: var(--text); }
      &:focus-visible { outline: none; box-shadow: var(--ring); }
    }
    .list {
      position: absolute; z-index: 50; left: 0; right: 0; top: calc(100% + 6px);
      max-height: 300px; overflow-y: auto; margin: 0; padding: 0.35rem; list-style: none;
      background: var(--bg-elevated); border: 1px solid var(--border); border-radius: var(--radius);
      box-shadow: var(--shadow-lg); animation: pop 140ms var(--ease);
    }
    @keyframes pop { from { opacity: 0; transform: translateY(-4px); } }
    .option {
      display: flex; align-items: center; justify-content: space-between; gap: 0.75rem;
      padding: 0.55rem 0.7rem; border-radius: var(--radius-sm); cursor: pointer;
      &.active { background: var(--bg-hover); }
      &[aria-selected='true'] .name { color: var(--brand-500); font-weight: 600; }
    }
    .all { border-bottom: 1px solid var(--border); border-radius: var(--radius-sm) var(--radius-sm) 0 0; margin-bottom: 0.25rem; }
    .name { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .code { flex: none; font-family: var(--font-mono); font-size: 0.75rem; color: var(--text-subtle); }
    .info { padding: 0.6rem 0.7rem; font-size: 0.85rem; color: var(--text-muted); }
    .info.error { color: var(--danger); }
    .info.hint { font-size: 0.75rem; color: var(--text-subtle); }
  `,
})
export class ClientPicker {
  private readonly api = inject(SwizimaApi);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly selected = model<Client | null>(null);
  readonly placeholder = input('All clients');
  readonly label = input('Filter by client');
  readonly inputId = input(`client-picker-${nextId}`);
  readonly pageSize = input(20);

  protected readonly listId = `client-picker-list-${nextId++}`;
  protected readonly open = signal(false);
  protected readonly query = signal('');
  /** -1 = "All clients" row. */
  protected readonly active = signal(-1);
  private readonly term = debounced(this.query, 250);

  protected readonly results = remoteData(
    computed(() => ({ search: this.term().trim(), size: this.pageSize(), sort: 'clientName,asc', open: this.open() })),
    ({ open, ...q }) => this.api.clients(open ? q : { ...q, size: 1 }),
  );
  protected readonly options = computed(() => this.results.state().data?.content ?? []);

  protected optionId(i: number): string {
    return `${this.listId}-opt-${i < 0 ? 'all' : i}`;
  }

  protected openList(): void {
    if (this.open()) return;
    this.query.set('');
    this.active.set(-1);
    this.open.set(true);
  }

  protected close(): void {
    this.open.set(false);
  }

  protected onInput(value: string): void {
    this.query.set(value);
    this.active.set(0);
    this.open.set(true);
  }

  protected choose(client: Client | null): void {
    this.selected.set(client);
    this.close();
    (this.host.nativeElement as HTMLElement).querySelector('input')?.blur();
  }

  protected clear(event: Event): void {
    event.stopPropagation();
    this.selected.set(null);
  }

  protected onKeydown(event: KeyboardEvent): void {
    const count = this.options().length;
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        if (!this.open()) return this.openList();
        this.active.update((i) => Math.min(i + 1, count - 1));
        this.scrollActive();
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.active.update((i) => Math.max(i - 1, -1));
        this.scrollActive();
        break;
      case 'Enter':
        if (!this.open()) return;
        event.preventDefault();
        this.choose(this.active() < 0 ? null : (this.options()[this.active()] ?? null));
        break;
      case 'Escape':
        if (this.open()) { event.preventDefault(); this.close(); }
        break;
      case 'Tab':
        this.close();
        break;
    }
  }

  private scrollActive(): void {
    queueMicrotask(() =>
      document.getElementById(this.optionId(this.active()))?.scrollIntoView({ block: 'nearest' }),
    );
  }

  protected onDocumentClick(event: MouseEvent): void {
    if (this.open() && !(this.host.nativeElement as HTMLElement).contains(event.target as Node)) this.close();
  }
}
