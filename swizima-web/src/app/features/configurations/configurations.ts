import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { SwizimaApi } from '../../core/api/swizima-api';
import { remoteData } from '../../core/data/remote-data';
import { toErrorMessage } from '../../core/http/api-error';
import { BillingPlan, BillingPlanRequest, BillingPlanStatus, Client } from '../../core/models/api.models';
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

function optionalAmount(control: AbstractControl<string>): ValidationErrors | null {
  const raw = (control.value ?? '').trim();
  if (raw === '') return null;
  const v = parseAmount(raw);
  return Number.isFinite(v) && v >= 0 ? null : { amount: true };
}

/**
 * /configurations — Billing plans: which percentage each client is charged per transaction
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
    percentageRate: ['', [rateValidator]],
    currency: ['UGX', Validators.required],
    lowBalanceThreshold: ['', [optionalAmount]],
    status: this.fb.control<BillingPlanStatus>('ACTIVE'),
    notes: ['', Validators.maxLength(500)],
  });

  /** Live example: what a 100,000 transaction would cost. */
  protected readonly example = signal<number | null>(null);

  protected openCreate(): void {
    this.editing.set(null);
    this.pickedClient.set(null);
    this.clientTouched.set(false);
    this.form.reset();
    this.updateExample();
    this.openDialog();
  }

  protected openEdit(plan: BillingPlan): void {
    this.editing.set(plan);
    this.form.reset({
      percentageRate: String(plan.percentageRate),
      currency: plan.currency,
      lowBalanceThreshold: plan.lowBalanceThreshold != null ? formatAmount(String(plan.lowBalanceThreshold)) : '',
      status: plan.status,
      notes: plan.notes ?? '',
    });
    this.updateExample();
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
    const rate = Number(this.form.controls.percentageRate.value);
    this.example.set(this.form.controls.percentageRate.valid ? Math.round(100000 * rate) / 100 : null);
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
    const request: BillingPlanRequest = {
      clientRef,
      percentageRate: Number(v.percentageRate),
      currency: v.currency,
      lowBalanceThreshold: v.lowBalanceThreshold.trim() ? parseAmount(v.lowBalanceThreshold) : undefined,
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
        this.notice.set({ tone: 'success', text: `Billing plan for ${name} ${current ? 'updated' : 'created'} — ${percent(saved.percentageRate)} per transaction.` });
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
