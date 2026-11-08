import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { SwizimaApi } from '../../core/api/swizima-api';
import { remoteData } from '../../core/data/remote-data';
import { toErrorMessage } from '../../core/http/api-error';
import { BillingModel, BillingPlan, BillingPlanRequest, BillingPlanStatus, Client } from '../../core/models/api.models';
import { ClientPicker } from '../../shared/client-picker/client-picker';
import { EmptyState } from '../../shared/empty-state/empty-state';
import { Icon } from '../../shared/icon/icon';
import { PageHeader } from '../../shared/page-header/page-header';
import { Pagination } from '../../shared/pagination/pagination';
import { StatusBadge, humanize } from '../../shared/status-badge/status-badge';
import { ClientNames, formatAmount, money, onAmountInput, parseAmount, percent } from '../billing/billing-ui';

const PAGE_SIZE = 10;
const CURRENCIES = ['UGX', 'KES', 'USD'] as const;

function rateValidator(control: AbstractControl<string>): ValidationErrors | null {
  const raw = (control.value ?? '').trim();
  if (raw === '') return { required: true };
  const v = Number(raw);
  return Number.isFinite(v) && v >= 0 && v <= 100 && /^\d{1,3}(\.\d{1,4})?$/.test(raw) ? null : { rate: true };
}

function requiredPositiveAmount(control: AbstractControl<string>): ValidationErrors | null {
  const raw = (control.value ?? '').trim();
  if (raw === '') return { required: true };
  const v = parseAmount(raw);
  return Number.isFinite(v) && v > 0 ? null : { amount: true };
}

function optionalAmount(control: AbstractControl<string>): ValidationErrors | null {
  const raw = (control.value ?? '').trim();
  if (raw === '') return null;
  const v = parseAmount(raw);
  return Number.isFinite(v) && v >= 0 ? null : { amount: true };
}

/**
 * /configurations — Billing plans: either per-transaction percentage (COMMISSION)
 * or end-of-month agreed amount (MONTHLY_FLAT)
 * (configurationsapi /api/v1/configurations/billing/plans).
 */
@Component({
  selector: 'app-configurations',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, ClientPicker, EmptyState, Icon, PageHeader, Pagination, StatusBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './configurations.html',
  styleUrl: '../billing/billing.css',
})
export class Configurations {
  private readonly api = inject(SwizimaApi);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly names = inject(ClientNames);

  protected readonly humanize = humanize;
  protected readonly money = money;
  protected readonly percent = percent;
  protected readonly pageSize = PAGE_SIZE;
  protected readonly statuses: BillingPlanStatus[] = ['ACTIVE', 'SUSPENDED'];
  protected readonly currencies = CURRENCIES;

  // ---------- List ----------
  protected readonly status = signal<BillingPlanStatus | ''>('');
  protected readonly page = signal(0);

  protected readonly plans = remoteData(
    computed(() => ({ status: this.status(), page: this.page(), size: PAGE_SIZE, sort: 'createdAt,desc' })),
    (q) => this.api.billingPlans(q),
  );

  protected onStatus(value: string): void { this.status.set(value as BillingPlanStatus | ''); this.page.set(0); }

  protected readonly busyId = signal<string | null>(null);
  protected readonly notice = signal<{ tone: 'success' | 'error'; text: string } | null>(null);

  protected clientName(plan: BillingPlan): string {
    return this.names.get(plan.clientRef)()?.name ?? '…';
  }

  protected isMonthlyFlat(plan: BillingPlan): boolean {
    return plan.billingModel === 'MONTHLY_FLAT';
  }

  protected toggleStatus(plan: BillingPlan): void {
    const next: BillingPlanStatus = plan.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    if (next === 'SUSPENDED'
        && !confirm(`Suspend billing for ${this.clientName(plan)}? They won't be able to record new transactions.`)) return;
    this.runRowAction(plan, this.api.updateBillingPlanStatus(plan.id, next),
      `Billing for ${this.clientName(plan)} is now ${humanize(next).toLowerCase()}.`);
  }

  protected remove(plan: BillingPlan): void {
    if (!confirm(`Remove the billing plan for ${this.clientName(plan)}? New transactions will be blocked until a plan is set again.`)) return;
    this.runRowAction(plan, this.api.deleteBillingPlan(plan.id), `Billing plan for ${this.clientName(plan)} was removed.`);
  }

  private runRowAction(plan: BillingPlan, request: Observable<unknown>, success: string): void {
    this.busyId.set(plan.id);
    request.subscribe({
      next: () => { this.busyId.set(null); this.notice.set({ tone: 'success', text: success }); this.plans.reload(); },
      error: (err) => { this.busyId.set(null); this.notice.set({ tone: 'error', text: toErrorMessage(err) }); },
    });
  }

  // ---------- Create / edit dialog ----------
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  protected readonly editing = signal<BillingPlan | null>(null);
  protected readonly pickedClient = signal<Client | null>(null);
  protected readonly clientTouched = signal(false);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly form = this.fb.group({
    billingModel: this.fb.control<BillingModel>('COMMISSION'),
    percentageRate: ['5', [rateValidator]],
    monthlyAmount: [''],
    currency: ['UGX', Validators.required],
    lowBalanceThreshold: ['', [optionalAmount]],
    status: this.fb.control<BillingPlanStatus>('ACTIVE'),
    notes: ['', Validators.maxLength(500)],
  });

  /** Live example: what a 100,000 transaction would cost (for COMMISSION) or monthly fee (for MONTHLY_FLAT). */
  protected readonly example = signal<number | null>(null);

  protected setBillingModel(model: BillingModel): void {
    this.form.controls.billingModel.setValue(model);
    if (model === 'MONTHLY_FLAT') {
      this.form.controls.percentageRate.clearValidators();
      this.form.controls.monthlyAmount.setValidators([requiredPositiveAmount]);
    } else {
      this.form.controls.percentageRate.setValidators([rateValidator]);
      this.form.controls.monthlyAmount.clearValidators();
    }
    this.form.controls.percentageRate.updateValueAndValidity();
    this.form.controls.monthlyAmount.updateValueAndValidity();
    this.updateExample();
  }

  protected openCreate(): void {
    this.editing.set(null);
    this.pickedClient.set(null);
    this.clientTouched.set(false);
    this.form.reset({
      billingModel: 'COMMISSION',
      percentageRate: '5',
      monthlyAmount: '',
      currency: 'UGX',
      lowBalanceThreshold: '',
      status: 'ACTIVE',
      notes: '',
    });
    this.setBillingModel('COMMISSION');
    this.openDialog();
  }

  protected openEdit(plan: BillingPlan): void {
    this.editing.set(plan);
    const model: BillingModel = plan.billingModel === 'MONTHLY_FLAT' ? 'MONTHLY_FLAT' : 'COMMISSION';
    this.form.reset({
      billingModel: model,
      percentageRate: String(plan.percentageRate ?? 0),
      monthlyAmount: plan.monthlyAmount != null ? formatAmount(String(plan.monthlyAmount)) : '',
      currency: plan.currency,
      lowBalanceThreshold: plan.lowBalanceThreshold != null ? formatAmount(String(plan.lowBalanceThreshold)) : '',
      status: plan.status,
      notes: plan.notes ?? '',
    });
    this.setBillingModel(model);
    this.openDialog();
  }

  private openDialog(): void {
    this.formError.set(null);
    this.dialog().nativeElement.showModal();
  }

  protected closeDialog(): void {
    if (!this.saving()) this.dialog().nativeElement.close();
  }

  protected updateExample(): void {
    if (this.form.controls.billingModel.value === 'MONTHLY_FLAT') {
      const amt = parseAmount(this.form.controls.monthlyAmount.value);
      this.example.set(this.form.controls.monthlyAmount.valid && Number.isFinite(amt) ? amt : null);
    } else {
      const rate = Number(this.form.controls.percentageRate.value);
      this.example.set(this.form.controls.percentageRate.valid ? Math.round(100000 * rate) / 100 : null);
    }
  }

  protected monthlyAmountInput(event: Event): void {
    onAmountInput(event, (v) => {
      this.form.controls.monthlyAmount.setValue(v);
      this.updateExample();
    });
  }

  protected thresholdInput(event: Event): void {
    onAmountInput(event, (v) => this.form.controls.lowBalanceThreshold.setValue(v));
  }

  protected invalid(name: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[name];
    return c.invalid && (c.touched || c.dirty);
  }

  protected submit(): void {
    const current = this.editing();
    const clientRef = current?.clientRef ?? this.pickedClient()?.id;
    this.clientTouched.set(true);
    if (this.form.invalid || !clientRef) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const isMonthly = v.billingModel === 'MONTHLY_FLAT';
    const request: BillingPlanRequest = {
      clientRef,
      billingModel: v.billingModel,
      percentageRate: isMonthly ? 0 : Number(v.percentageRate),
      monthlyAmount: isMonthly ? parseAmount(v.monthlyAmount) : undefined,
      currency: v.currency,
      lowBalanceThreshold: !isMonthly && v.lowBalanceThreshold.trim() ? parseAmount(v.lowBalanceThreshold) : undefined,
      status: v.status,
      notes: v.notes.trim() || undefined,
    };
    const call = current ? this.api.updateBillingPlan(current.id, request) : this.api.createBillingPlan(request);
    this.saving.set(true);
    this.formError.set(null);
    call.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.dialog().nativeElement.close();
        const name = this.names.get(saved.clientRef)()?.name ?? this.pickedClient()?.clientName ?? 'client';
        const summary = saved.billingModel === 'MONTHLY_FLAT'
          ? `${money(saved.monthlyAmount, saved.currency)} per end of month`
          : `${percent(saved.percentageRate)} per transaction`;
        this.notice.set({ tone: 'success', text: `Billing plan for ${name} ${current ? 'updated' : 'created'} — ${summary}.` });
        if (!current) this.page.set(0);
        this.plans.reload();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(toErrorMessage(err));
      },
    });
  }
}
