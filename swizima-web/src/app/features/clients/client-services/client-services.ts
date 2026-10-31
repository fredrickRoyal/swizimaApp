import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { SwizimaApi } from '../../../core/api/swizima-api';
import { debounced, remoteData } from '../../../core/data/remote-data';
import { toErrorMessage } from '../../../core/http/api-error';
import {
  ServiceCatalogItem,
  ServiceCatalogRequest,
  ServiceCatalogStatus,
  ServiceCategory,
} from '../../../core/models/api.models';
import { EmptyState } from '../../../shared/empty-state/empty-state';
import { Icon } from '../../../shared/icon/icon';
import { Pagination } from '../../../shared/pagination/pagination';
import { StatusBadge, humanize } from '../../../shared/status-badge/status-badge';
import { CATEGORY_ICON } from '../../services/services';
import { ClientLayout } from '../client-layout/client-layout';

const PAGE_SIZE = 10;
const CURRENCIES = ['UGX', 'KES', 'USD'] as const;

/** "1,250,000.5" -> 1250000.5 (NaN when blank/invalid). */
function parsePrice(text: string): number {
  const raw = text.replace(/,/g, '').trim();
  return raw === '' || raw === '.' ? NaN : Number(raw);
}

/** Keeps digits and one dot (max 2 decimals) and groups the integer part with commas. */
function formatPrice(text: string): string {
  const cleaned = text.replace(/[^\d.]/g, '');
  const [intPart = '', ...rest] = cleaned.split('.');
  const int = intPart.replace(/^0+(?=\d)/, '').replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return rest.length ? `${int || '0'}.${rest.join('').slice(0, 2)}` : int;
}

function priceValidator(control: AbstractControl<string>): ValidationErrors | null {
  const value = parsePrice(control.value ?? '');
  return Number.isFinite(value) && value >= 0 ? null : { price: true };
}

/** /clients/:id/services — a single client's service catalog with create / edit / status / delete. */
@Component({
  selector: 'app-client-services',
  imports: [CurrencyPipe, ReactiveFormsModule, EmptyState, Icon, Pagination, StatusBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-services.html',
  styleUrl: './client-services.css',
})
export class ClientServices {
  private readonly api = inject(SwizimaApi);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly layout = inject(ClientLayout);

  protected readonly categories = Object.keys(CATEGORY_ICON) as ServiceCategory[];
  protected readonly statuses: ServiceCatalogStatus[] = ['ACTIVE', 'INACTIVE'];
  protected readonly categoryIcon = CATEGORY_ICON;
  protected readonly humanize = humanize;
  protected readonly pageSize = PAGE_SIZE;

  // ---------- List ----------
  protected readonly search = signal('');
  protected readonly category = signal<ServiceCategory | ''>('');
  protected readonly status = signal<ServiceCatalogStatus | ''>('');
  protected readonly page = signal(0);
  private readonly searchTerm = debounced(this.search, 350);

  protected readonly services = remoteData(
    computed(() => ({
      clientId: this.layout.id(),
      search: this.searchTerm().trim(),
      category: this.category(),
      status: this.status(),
      page: this.page(),
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
    })),
    (query) => this.api.serviceCatalogs(query),
  );

  protected readonly hasFilters = computed(() => !!this.search().trim() || !!this.category() || !!this.status());

  protected onSearch(value: string): void { this.search.set(value); this.page.set(0); }
  protected onCategory(value: string): void { this.category.set(value as ServiceCategory | ''); this.page.set(0); }
  protected onStatus(value: string): void { this.status.set(value as ServiceCatalogStatus | ''); this.page.set(0); }
  protected clearFilters(): void { this.search.set(''); this.category.set(''); this.status.set(''); this.page.set(0); }

  /** Row-level action in flight (status toggle / delete), keyed by service id. */
  protected readonly busyId = signal<string | null>(null);
  protected readonly notice = signal<{ tone: 'success' | 'error'; text: string } | null>(null);

  protected toggleStatus(item: ServiceCatalogItem): void {
    const next: ServiceCatalogStatus = item.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.runRowAction(item, this.api.updateServiceStatus(item.id, next),
      `${item.serviceName} is now ${humanize(next).toLowerCase()}.`);
  }

  protected remove(item: ServiceCatalogItem): void {
    if (!confirm(`Delete "${item.serviceName}"? This cannot be undone.`)) return;
    this.runRowAction(item, this.api.deleteService(item.id), `${item.serviceName} was deleted.`);
  }

  private runRowAction(item: ServiceCatalogItem, request: Observable<unknown>, success: string): void {
    this.busyId.set(item.id);
    request.subscribe({
      next: () => {
        this.busyId.set(null);
        this.notice.set({ tone: 'success', text: success });
        this.services.reload();
      },
      error: (err) => {
        this.busyId.set(null);
        this.notice.set({ tone: 'error', text: toErrorMessage(err) });
      },
    });
  }

  // ---------- Create / edit dialog ----------
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  protected readonly editing = signal<ServiceCatalogItem | null>(null);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);

  /** Supported currencies, plus the item's own currency when editing a legacy value. */
  protected readonly currencyOptions = computed(() => {
    const own = this.editing()?.currency;
    return own && !(CURRENCIES as readonly string[]).includes(own) ? [...CURRENCIES, own] : [...CURRENCIES];
  });

  protected readonly form = this.fb.group({
    serviceName: ['', [Validators.required, Validators.maxLength(255)]],
    serviceCode: ['', [Validators.required, Validators.maxLength(50)]],
    category: this.fb.control<ServiceCategory>('CAR_WASH', Validators.required),
    price: ['', [Validators.required, priceValidator]],
    currency: ['UGX', Validators.required],
    durationMinutes: this.fb.control<number | null>(null, [Validators.min(1)]),
    status: this.fb.control<ServiceCatalogStatus>('ACTIVE'),
    description: [''],
  });

  protected openCreate(): void {
    this.editing.set(null);
    this.form.reset();
    this.openDialog();
  }

  protected openEdit(item: ServiceCatalogItem): void {
    this.editing.set(item);
    this.form.reset({
      serviceName: item.serviceName,
      serviceCode: item.serviceCode,
      category: item.category,
      price: formatPrice(String(item.price)),
      currency: item.currency,
      durationMinutes: item.durationMinutes ?? null,
      status: item.status,
      description: item.description ?? '',
    });
    this.openDialog();
  }

  private openDialog(): void {
    this.formError.set(null);
    this.dialog().nativeElement.showModal();
  }

  protected closeDialog(): void {
    if (this.saving()) return;
    this.dialog().nativeElement.close();
  }

  /** Suggests a code from the name while creating, until the user edits the code themselves. */
  protected onNameInput(): void {
    const code = this.form.controls.serviceCode;
    if (this.editing() || code.dirty) return;
    const slug = this.form.controls.serviceName.value
      .toUpperCase()
      .replace(/[^A-Z0-9]+/g, '-')
      .replace(/^-|-$/g, '')
      .slice(0, 40);
    code.setValue(slug);
  }

  /** Re-formats with commas as the user types, keeping the caret after the same digit. */
  protected onPriceInput(event: Event): void {
    const el = event.target as HTMLInputElement;
    const caret = el.selectionStart ?? el.value.length;
    const significantBefore = el.value.slice(0, caret).replace(/[^\d.]/g, '').length;
    const formatted = formatPrice(el.value);
    this.form.controls.price.setValue(formatted, { emitEvent: false });
    el.value = formatted;

    let pos = 0;
    for (let seen = 0; pos < formatted.length && seen < significantBefore; pos++) {
      if (formatted[pos] !== ',') seen++;
    }
    el.setSelectionRange(pos, pos);
  }

  /** Tidies a trailing dot, e.g. "1,000." -> "1,000". */
  protected onPriceBlur(): void {
    const c = this.form.controls.price;
    if (c.value.endsWith('.')) c.setValue(c.value.slice(0, -1));
  }

  protected invalid(name: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[name];
    return c.invalid && (c.touched || c.dirty);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const request: ServiceCatalogRequest = {
      clientId: this.layout.id(),
      serviceName: v.serviceName.trim(),
      serviceCode: v.serviceCode.trim(),
      category: v.category,
      price: parsePrice(v.price),
      currency: v.currency,
      durationMinutes: v.durationMinutes || undefined,
      status: v.status,
      description: v.description.trim() || undefined,
    };
    const current = this.editing();
    const call = current ? this.api.updateService(current.id, request) : this.api.createService(request);

    this.saving.set(true);
    this.formError.set(null);
    call.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.dialog().nativeElement.close();
        this.notice.set({ tone: 'success', text: `${saved.serviceName} was ${current ? 'updated' : 'added'}.` });
        if (!current) this.page.set(0);
        this.services.reload();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(toErrorMessage(err));
      },
    });
  }

  protected duration(minutes?: number): string {
    if (!minutes) return '—';
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return h ? `${h}h${m ? ` ${m}m` : ''}` : `${m} min`;
  }
}
