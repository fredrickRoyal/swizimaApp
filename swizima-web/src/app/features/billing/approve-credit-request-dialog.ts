import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, output, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { SwizimaApi } from '../../core/api/swizima-api';
import { AuthService } from '../../core/auth/auth.service';
import { toErrorMessage } from '../../core/http/api-error';
import { CreditPaymentMethod, CreditTopUpRequest } from '../../core/models/api.models';
import { Icon } from '../../shared/icon/icon';
import { PAYMENT_METHODS, formatAmount, money, onAmountInput, parseAmount, paymentMethodLabel } from './billing-ui';

function positiveAmount(control: AbstractControl<string>): ValidationErrors | null {
  const v = parseAmount(control.value ?? '');
  return Number.isFinite(v) && v > 0 ? null : { amount: true };
}

/**
 * Super Admin modal to Approve (and confirm the actual credit amount to top up)
 * or Reject a client's credit top-up request.
 */
@Component({
  selector: 'app-approve-credit-request-dialog',
  imports: [DatePipe, ReactiveFormsModule, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './billing.css',
  template: `
    <dialog #dialog class="dialog card" id="review-credit-request-dialog" aria-labelledby="review-credit-request-title"
            (cancel)="saving() && $event.preventDefault()">
      @if (activeRequest(); as req) {
        @if (mode() === 'approve') {
          <form [formGroup]="approveForm" (ngSubmit)="submitApprove()" novalidate>
            <header class="dialog-head">
              <div>
                <h2 id="review-credit-request-title">Approve &amp; confirm credit top-up</h2>
                @if (clientName()) { <p class="cell-secondary">{{ clientName() }}</p> }
              </div>
              <button type="button" class="btn btn-icon btn-ghost" id="review-credit-close" (click)="close()" aria-label="Close">
                <app-icon name="close" />
              </button>
            </header>

            <div class="dialog-body">
              @if (error()) {
                <div class="alert alert-error" role="alert" id="review-credit-error">
                  <app-icon name="alert" /><span>{{ error() }}</span>
                </div>
              }

              <div class="preview" style="margin-bottom: 1rem">
                <div>
                  <span style="display: block; font-size: 0.78rem; color: var(--text-muted)">Client requested</span>
                  <strong>{{ money(req.requestedAmount, req.currency) }}</strong>
                </div>
                <div style="text-align: end">
                  <span style="display: block; font-size: 0.78rem; color: var(--text-muted)">
                    {{ methodLabel(req.paymentMethod) }}
                    @if (req.paymentReference) { · <span class="cell-mono">{{ req.paymentReference }}</span> }
                  </span>
                  <small class="cell-secondary">{{ req.createdAt | date: 'medium' }}</small>
                </div>
              </div>

              @if (req.notes) {
                <div class="alert alert-info" style="margin-bottom: 1rem">
                  <app-icon name="info" />
                  <span><strong>Client note:</strong> {{ req.notes }}</span>
                </div>
              }

              <div class="form-grid">
                <div class="field span-2">
                  <label for="approve-credit-amount">Confirmed credit amount to top up <em aria-hidden="true">*</em></label>
                  <div class="input-suffix">
                    <input id="approve-credit-amount" class="input" type="text" inputmode="decimal" autocomplete="off"
                           formControlName="approvedAmount" placeholder="0" [attr.aria-invalid]="invalidApprove('approvedAmount')"
                           (input)="amountInput($event)" />
                    <span>{{ req.currency }}</span>
                  </div>
                  <p class="field-hint">
                    Pre-filled with the client's requested amount ({{ money(req.requestedAmount, req.currency) }}). Adjust if the actual verified payment differs.
                  </p>
                  <div class="quick-amounts" aria-label="Quick amounts">
                    @for (q of quickAmounts; track q) {
                      <button type="button" class="chip" (click)="setAmount(q)">{{ q.toLocaleString() }}</button>
                    }
                  </div>
                  @if (invalidApprove('approvedAmount')) { <p class="field-error">Enter a confirmed amount greater than 0.</p> }
                </div>

                <div class="field">
                  <label for="approve-credit-method">Payment method <em aria-hidden="true">*</em></label>
                  <select id="approve-credit-method" class="select" formControlName="paymentMethod">
                    @for (m of methods; track m.value) { <option [value]="m.value">{{ m.label }}</option> }
                  </select>
                </div>

                <div class="field">
                  <label for="approve-credit-ref">Verified payment reference</label>
                  <input id="approve-credit-ref" class="input cell-mono" formControlName="paymentReference" autocomplete="off"
                         spellcheck="false" placeholder="e.g. MP241104.1234" />
                </div>

                <div class="field span-2">
                  <label for="approve-credit-notes">Admin approval note</label>
                  <textarea id="approve-credit-notes" class="input" rows="2" formControlName="reviewNotes"
                            placeholder="Optional note (e.g. Verified MoMo receipt)"></textarea>
                </div>
              </div>

              @if (currentBalance() !== null) {
                <div class="preview" id="approve-credit-preview">
                  <span>Wallet balance after approval</span>
                  <strong>{{ money(newBalance(), req.currency) }}</strong>
                </div>
              }
            </div>

            <footer class="dialog-foot">
              <button type="button" class="btn" id="approve-credit-switch-reject" (click)="mode.set('reject')" [disabled]="saving()">
                Reject instead
              </button>
              <button type="button" class="btn" id="approve-credit-cancel" (click)="close()" [disabled]="saving()">Cancel</button>
              <button type="submit" class="btn btn-primary" id="approve-credit-submit" [disabled]="saving()">
                @if (saving()) { <span class="spinner" aria-hidden="true"></span> }
                <app-icon name="check" /> Approve &amp; load credit
              </button>
            </footer>
          </form>
        } @else {
          <form [formGroup]="rejectForm" (ngSubmit)="submitReject()" novalidate>
            <header class="dialog-head">
              <div>
                <h2 id="review-credit-request-title">Reject credit request</h2>
                @if (clientName()) { <p class="cell-secondary">{{ clientName() }}</p> }
              </div>
              <button type="button" class="btn btn-icon btn-ghost" (click)="close()" aria-label="Close">
                <app-icon name="close" />
              </button>
            </header>

            <div class="dialog-body">
              @if (error()) {
                <div class="alert alert-error" role="alert">
                  <app-icon name="alert" /><span>{{ error() }}</span>
                </div>
              }

              <div class="preview" style="margin-bottom: 1rem">
                <span>Requested amount</span>
                <strong>{{ money(req.requestedAmount, req.currency) }} ({{ methodLabel(req.paymentMethod) }})</strong>
              </div>

              <div class="field">
                <label for="reject-credit-reason">Reason for rejection <em aria-hidden="true">*</em></label>
                <textarea id="reject-credit-reason" class="input" rows="3" formControlName="reason"
                          placeholder="Explain why this request was rejected (e.g. Payment reference not found)"
                          [attr.aria-invalid]="invalidReject('reason')"></textarea>
                @if (invalidReject('reason')) { <p class="field-error">Please provide a reason for rejecting this request.</p> }
              </div>
            </div>

            <footer class="dialog-foot">
              <button type="button" class="btn" (click)="mode.set('approve')" [disabled]="saving()">
                Back to approve
              </button>
              <button type="button" class="btn" (click)="close()" [disabled]="saving()">Cancel</button>
              <button type="submit" class="btn btn-primary" id="reject-credit-submit" [disabled]="saving()">
                @if (saving()) { <span class="spinner" aria-hidden="true"></span> }
                Confirm rejection
              </button>
            </footer>
          </form>
        }
      }
    </dialog>
  `,
})
export class ApproveCreditRequestDialog {
  private readonly api = inject(SwizimaApi);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly reviewed = output<CreditTopUpRequest>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  protected readonly activeRequest = signal<CreditTopUpRequest | null>(null);
  protected readonly mode = signal<'approve' | 'reject'>('approve');
  protected readonly clientName = signal<string | null>(null);
  protected readonly currentBalance = signal<number | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly methods = PAYMENT_METHODS;
  protected readonly methodLabel = paymentMethodLabel;
  protected readonly quickAmounts = [10000, 25000, 50000, 100000, 250000];
  protected readonly money = money;

  protected readonly approveForm = this.fb.group({
    approvedAmount: ['', [Validators.required, positiveAmount]],
    paymentMethod: this.fb.control<CreditPaymentMethod>('MOBILE_MONEY', Validators.required),
    paymentReference: ['', Validators.maxLength(100)],
    reviewNotes: ['', Validators.maxLength(500)],
  });

  protected readonly rejectForm = this.fb.group({
    reason: ['', [Validators.required, Validators.maxLength(500)]],
  });

  private readonly approvedAmountValue = toSignal(this.approveForm.controls.approvedAmount.valueChanges, { initialValue: '' });
  protected readonly newBalance = computed(() => {
    const add = parseAmount(this.approvedAmountValue() ?? '');
    return (this.currentBalance() ?? 0) + (Number.isFinite(add) ? add : 0);
  });

  openApprove(req: CreditTopUpRequest, opts: { clientName?: string; balance?: number } = {}): void {
    this.open(req, 'approve', opts);
  }

  openReject(req: CreditTopUpRequest, opts: { clientName?: string; balance?: number } = {}): void {
    this.open(req, 'reject', opts);
  }

  private open(
    req: CreditTopUpRequest,
    mode: 'approve' | 'reject',
    opts: { clientName?: string; balance?: number },
  ): void {
    this.activeRequest.set(req);
    this.mode.set(mode);
    this.clientName.set(opts.clientName ?? null);
    this.currentBalance.set(opts.balance ?? null);
    this.error.set(null);

    this.approveForm.reset({
      approvedAmount: formatAmount(String(req.requestedAmount)),
      paymentMethod: req.paymentMethod,
      paymentReference: req.paymentReference ?? '',
      reviewNotes: '',
    });
    this.rejectForm.reset({ reason: '' });
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    if (!this.saving()) this.dialog().nativeElement.close();
  }

  protected amountInput(event: Event): void {
    onAmountInput(event, (v) => this.approveForm.controls.approvedAmount.setValue(v));
  }

  protected setAmount(value: number): void {
    this.approveForm.controls.approvedAmount.setValue(formatAmount(String(value)));
    this.approveForm.controls.approvedAmount.markAsDirty();
  }

  protected invalidApprove(name: keyof typeof this.approveForm.controls): boolean {
    const c = this.approveForm.controls[name];
    return c.invalid && (c.touched || c.dirty);
  }

  protected invalidReject(name: keyof typeof this.rejectForm.controls): boolean {
    const c = this.rejectForm.controls[name];
    return c.invalid && (c.touched || c.dirty);
  }

  protected submitApprove(): void {
    const req = this.activeRequest();
    if (!req) return;
    if (this.approveForm.invalid) {
      this.approveForm.markAllAsTouched();
      return;
    }
    const v = this.approveForm.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.api
      .approveCreditRequest(req.id, {
        approvedAmount: parseAmount(v.approvedAmount),
        paymentMethod: v.paymentMethod,
        paymentReference: v.paymentReference.trim() || undefined,
        reviewedBy: this.auth.displayName() || 'Super Admin',
        reviewNotes: v.reviewNotes.trim() || undefined,
      })
      .subscribe({
        next: (updated) => {
          this.saving.set(false);
          this.dialog().nativeElement.close();
          this.reviewed.emit(updated);
        },
        error: (err) => {
          this.saving.set(false);
          this.error.set(toErrorMessage(err));
        },
      });
  }

  protected submitReject(): void {
    const req = this.activeRequest();
    if (!req) return;
    if (this.rejectForm.invalid) {
      this.rejectForm.markAllAsTouched();
      return;
    }
    const v = this.rejectForm.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.api
      .rejectCreditRequest(req.id, {
        reason: v.reason.trim(),
        reviewedBy: this.auth.displayName() || 'Super Admin',
      })
      .subscribe({
        next: (updated) => {
          this.saving.set(false);
          this.dialog().nativeElement.close();
          this.reviewed.emit(updated);
        },
        error: (err) => {
          this.saving.set(false);
          this.error.set(toErrorMessage(err));
        },
      });
  }
}

