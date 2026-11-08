import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, input, output, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { SwizimaApi } from '../../core/api/swizima-api';
import { toErrorMessage } from '../../core/http/api-error';
import { CreditLoad, CreditPaymentMethod } from '../../core/models/api.models';
import { Icon } from '../../shared/icon/icon';
import { PAYMENT_METHODS, formatAmount, money, onAmountInput, parseAmount } from './billing-ui';

function positiveAmount(control: AbstractControl<string>): ValidationErrors | null {
  const v = parseAmount(control.value ?? '');
  return Number.isFinite(v) && v > 0 ? null : { amount: true };
}

/**
 * "Load credit" modal for one client's billing wallet
 * (POST /api/v1/payments/billing/wallets/client/{clientRef}/credits).
 * Call `open(clientRef, ...)`; emits `loaded` on success.
 */
@Component({
  selector: 'app-load-credit-dialog',
  imports: [ReactiveFormsModule, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './billing.css',
  template: `
    <dialog #dialog class="dialog card" id="load-credit-dialog" aria-labelledby="load-credit-title"
            (cancel)="saving() && $event.preventDefault()">
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <header class="dialog-head">
          <div>
            <h2 id="load-credit-title">Load credit</h2>
            @if (clientName()) { <p class="cell-secondary">{{ clientName() }}</p> }
          </div>
          <button type="button" class="btn btn-icon btn-ghost" id="load-credit-close" (click)="close()" aria-label="Close">
            <app-icon name="close" />
          </button>
        </header>

        <div class="dialog-body">
          @if (error()) {
            <div class="alert alert-error" role="alert" id="load-credit-error"><app-icon name="alert" /><span>{{ error() }}</span></div>
          }
          <div class="form-grid">
            <div class="field span-2">
              <label for="load-credit-amount">Amount <em aria-hidden="true">*</em></label>
              <div class="input-suffix">
                <input id="load-credit-amount" class="input" type="text" inputmode="decimal" autocomplete="off"
                       formControlName="amount" placeholder="0" [attr.aria-invalid]="invalid('amount')"
                       (input)="amountInput($event)" />
                <span>{{ currency() }}</span>
              </div>
              <div class="quick-amounts" aria-label="Quick amounts">
                @for (q of quickAmounts; track q) {
                  <button type="button" class="chip" [id]="'load-credit-quick-' + q" (click)="setAmount(q)">{{ q.toLocaleString() }}</button>
                }
              </div>
              @if (invalid('amount')) { <p class="field-error">Enter an amount greater than 0.</p> }
            </div>
            <div class="field">
              <label for="load-credit-method">Payment method <em aria-hidden="true">*</em></label>
              <select id="load-credit-method" class="select" formControlName="paymentMethod">
                @for (m of methods; track m.value) { <option [value]="m.value">{{ m.label }}</option> }
              </select>
            </div>
            <div class="field">
              <label for="load-credit-ref">Payment reference</label>
              <input id="load-credit-ref" class="input cell-mono" formControlName="paymentReference" autocomplete="off"
                     spellcheck="false" placeholder="e.g. MP241104.1234" />
              <p class="field-hint">Prevents the same receipt being loaded twice.</p>
            </div>
            <div class="field">
              <label for="load-credit-payer">Paid by</label>
              <input id="load-credit-payer" class="input" formControlName="payerName" placeholder="Optional" />
            </div>
            <div class="field">
              <label for="load-credit-phone">Payer phone</label>
              <input id="load-credit-phone" class="input" type="tel" formControlName="payerPhone" placeholder="Optional" />
            </div>
            <div class="field">
              <label for="load-credit-received">Received by</label>
              <input id="load-credit-received" class="input" formControlName="receivedBy" placeholder="Staff name" />
            </div>
            <div class="field">
              <label for="load-credit-date">Paid on</label>
              <input id="load-credit-date" class="input" type="datetime-local" formControlName="loadedAt" [max]="nowLocal" />
            </div>
            <div class="field span-2">
              <label for="load-credit-notes">Notes</label>
              <textarea id="load-credit-notes" class="input" rows="2" formControlName="notes" placeholder="Optional"></textarea>
            </div>
          </div>
          @if (currentBalance() !== null) {
            <div class="preview" id="load-credit-preview">
              <span>New balance</span>
              <strong>{{ money(newBalance(), currency()) }}</strong>
            </div>
          }
        </div>

        <footer class="dialog-foot">
          <button type="button" class="btn" id="load-credit-cancel" (click)="close()" [disabled]="saving()">Cancel</button>
          <button type="submit" class="btn btn-primary" id="load-credit-submit" [disabled]="saving()">
            @if (saving()) { <span class="spinner" aria-hidden="true"></span> }
            Load credit
          </button>
        </footer>
      </form>
    </dialog>
  `,
})
export class LoadCreditDialog {
  private readonly api = inject(SwizimaApi);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly loaded = output<CreditLoad>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  protected readonly clientRef = signal<string | null>(null);
  protected readonly clientName = signal<string | null>(null);
  protected readonly currency = signal('UGX');
  protected readonly currentBalance = signal<number | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly methods = PAYMENT_METHODS;
  protected readonly quickAmounts = [20000, 50000, 100000, 200000, 500000];
  protected readonly money = money;
  protected nowLocal = '';

  protected readonly form = this.fb.group({
    amount: ['', [Validators.required, positiveAmount]],
    paymentMethod: this.fb.control<CreditPaymentMethod>('MOBILE_MONEY', Validators.required),
    paymentReference: ['', Validators.maxLength(100)],
    payerName: ['', Validators.maxLength(150)],
    payerPhone: ['', Validators.maxLength(30)],
    receivedBy: ['', Validators.maxLength(150)],
    loadedAt: [''],
    notes: ['', Validators.maxLength(500)],
  });

  private readonly amountValue = toSignal(this.form.controls.amount.valueChanges, { initialValue: '' });
  protected readonly newBalance = computed(() => {
    const add = parseAmount(this.amountValue() ?? '');
    return (this.currentBalance() ?? 0) + (Number.isFinite(add) ? add : 0);
  });

  open(clientRef: string, opts: { clientName?: string; currency?: string; balance?: number } = {}): void {
    this.clientRef.set(clientRef);
    this.clientName.set(opts.clientName ?? null);
    this.currency.set(opts.currency ?? 'UGX');
    this.currentBalance.set(opts.balance ?? null);
    this.error.set(null);
    this.nowLocal = toLocalInput(new Date());
    this.form.reset({ loadedAt: this.nowLocal });
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    if (!this.saving()) this.dialog().nativeElement.close();
  }

  protected amountInput(event: Event): void {
    onAmountInput(event, (v) => this.form.controls.amount.setValue(v));
  }

  protected setAmount(value: number): void {
    this.form.controls.amount.setValue(formatAmount(String(value)));
    this.form.controls.amount.markAsDirty();
  }

  protected invalid(name: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[name];
    return c.invalid && (c.touched || c.dirty);
  }

  protected submit(): void {
    const clientRef = this.clientRef();
    if (!clientRef) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.api
      .loadCredit(clientRef, {
        amount: parseAmount(v.amount),
        paymentMethod: v.paymentMethod,
        paymentReference: v.paymentReference.trim() || undefined,
        payerName: v.payerName.trim() || undefined,
        payerPhone: v.payerPhone.trim() || undefined,
        receivedBy: v.receivedBy.trim() || undefined,
        loadedAt: v.loadedAt ? `${v.loadedAt}:00` : undefined,
        notes: v.notes.trim() || undefined,
      })
      .subscribe({
        next: (load) => {
          this.saving.set(false);
          this.dialog().nativeElement.close();
          this.loaded.emit(load);
        },
        error: (err) => {
          this.saving.set(false);
          this.error.set(toErrorMessage(err));
        },
      });
  }
}

function toLocalInput(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
