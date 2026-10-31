import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { SwizimaApi } from '../../../core/api/swizima-api';
import { DIAL_COUNTRIES } from '../../../core/data/dial-codes';
import { debounced, remoteData } from '../../../core/data/remote-data';
import { toErrorMessage } from '../../../core/http/api-error';
import {
  IdDocumentType,
  PaymentMethod,
  PaymentStatus,
  TransactionPayment,
  VehicleClearanceStatus,
  VehicleServiceTransaction,
  VehicleServiceTransactionRequest,
} from '../../../core/models/api.models';
import { EmptyState } from '../../../shared/empty-state/empty-state';
import { Icon } from '../../../shared/icon/icon';
import { Pagination } from '../../../shared/pagination/pagination';
import { PhoneInput } from '../../../shared/phone-input/phone-input';
import { StatusBadge, humanize } from '../../../shared/status-badge/status-badge';
import { ClientLayout } from '../client-layout/client-layout';

const PAGE_SIZE = 10;
const PHONE = /^\+?[0-9 ()-]{7,30}$/;

function parseAmount(text: string): number {
  const raw = text.replace(/,/g, '').trim();
  return raw === '' || raw === '.' ? NaN : Number(raw);
}

/** Keeps digits and one dot (max 2 decimals) and groups the integer part with commas. */
function formatAmount(text: string): string {
  const cleaned = text.replace(/[^\d.]/g, '');
  const [intPart = '', ...rest] = cleaned.split('.');
  const int = intPart.replace(/^0+(?=\d)/, '').replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return rest.length ? `${int || '0'}.${rest.join('').slice(0, 2)}` : int;
}

function amountValidator(control: AbstractControl<string>): ValidationErrors | null {
  const value = parseAmount(control.value ?? '');
  return Number.isFinite(value) && value >= 0 ? null : { amount: true };
}

/** Value for <input type="datetime-local">: "yyyy-MM-ddTHH:mm" in local time. */
function toLocalInput(value?: string | Date | null): string {
  if (!value) return '';
  if (typeof value === 'string') return value.slice(0, 16);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}`;
}

/** /clients/:id/transactions — vehicle service transactions for the selected client (clienttransactionsapi). */
@Component({
  selector: 'app-client-transactions',
  imports: [ReactiveFormsModule, EmptyState, Icon, Pagination, PhoneInput, StatusBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-transactions.html',
  styleUrl: './client-transactions.css',
})
export class ClientTransactionsPage {
  private readonly api = inject(SwizimaApi);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly layout = inject(ClientLayout);

  protected readonly humanize = humanize;
  protected readonly pageSize = PAGE_SIZE;
  protected readonly paymentStatuses: PaymentStatus[] = ['PENDING', 'PARTIALLY_PAID', 'PAID', 'CANCELLED'];
  protected readonly clearanceStatuses: VehicleClearanceStatus[] = ['IN_SERVICE', 'READY', 'CLEARED', 'RELEASED'];
  /** Statuses that can be picked when editing; CLEARED / RELEASED come from the Clear / Exit actions. */
  protected readonly editableClearance: VehicleClearanceStatus[] = ['IN_SERVICE', 'READY'];
  protected readonly paymentMethods: PaymentMethod[] = ['CASH', 'MOBILE_MONEY', 'CARD', 'BANK'];
  protected readonly idDocumentTypes: IdDocumentType[] = ['NATIONAL_ID', 'PASSPORT', 'DRIVING_PERMIT', 'OTHER'];
  protected readonly nationalities = DIAL_COUNTRIES.map((c) => c.name);
  protected readonly clientCountry = computed(() => this.layout.client.state().data?.country ?? 'Uganda');

  // ---------- List ----------
  protected readonly search = signal('');
  protected readonly paymentStatus = signal<PaymentStatus | ''>('');
  protected readonly clearanceStatus = signal<VehicleClearanceStatus | ''>('');
  protected readonly fromDate = signal('');
  protected readonly toDate = signal('');
  protected readonly page = signal(0);
  private readonly searchTerm = debounced(this.search, 350);

  protected readonly transactions = remoteData(
    computed(() => ({
      clientRef: this.layout.id(),
      search: this.searchTerm().trim(),
      paymentStatus: this.paymentStatus(),
      clearanceStatus: this.clearanceStatus(),
      fromDate: this.fromDate(),
      toDate: this.toDate(),
      page: this.page(),
      size: PAGE_SIZE,
      sort: 'entryDateTime,desc',
    })),
    (query) => this.api.vehicleServices(query),
  );

  /** Client's catalog and staff feed the "Service type" and "Assigned to" suggestions. */
  private readonly catalog = remoteData(
    computed(() => this.layout.id()),
    (clientId) => this.api.serviceCatalogs({ clientId, size: 200, sort: 'serviceName,asc' }),
  );
  private readonly staffList = remoteData(
    computed(() => this.layout.id()),
    (clientId) => this.api.staff(clientId, { size: 200, sort: 'surname,asc' }),
  );
  protected readonly serviceOptions = computed(() => this.catalog.state().data?.content ?? []);
  protected readonly staffOptions = computed(() => (this.staffList.state().data?.content ?? []).map((s) => s.fullName));

  protected readonly currency = computed(() => this.serviceOptions()[0]?.currency ?? 'UGX');

  protected readonly hasFilters = computed(
    () => !!this.search().trim() || !!this.paymentStatus() || !!this.clearanceStatus() || !!this.fromDate() || !!this.toDate(),
  );

  protected onSearch(value: string): void { this.search.set(value); this.page.set(0); }
  protected onPayment(value: string): void { this.paymentStatus.set(value as PaymentStatus | ''); this.page.set(0); }
  protected onClearance(value: string): void { this.clearanceStatus.set(value as VehicleClearanceStatus | ''); this.page.set(0); }
  protected onFrom(value: string): void { this.fromDate.set(value); this.page.set(0); }
  protected onTo(value: string): void { this.toDate.set(value); this.page.set(0); }
  protected clearFilters(): void {
    this.search.set('');
    this.paymentStatus.set('');
    this.clearanceStatus.set('');
    this.fromDate.set('');
    this.toDate.set('');
    this.page.set(0);
  }

  protected readonly busyId = signal<string | null>(null);
  protected readonly notice = signal<{ tone: 'success' | 'error'; text: string } | null>(null);

  protected formatMoney(value: number): string {
    return Number(value ?? 0).toLocaleString(undefined, { maximumFractionDigits: 2 });
  }

  protected formatDateTime(value?: string): string {
    if (!value) return '—';
    const d = new Date(value);
    return Number.isNaN(d.getTime())
      ? value
      : d.toLocaleString(undefined, { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
  }

  protected remove(t: VehicleServiceTransaction): void {
    if (!confirm(`Delete the transaction for ${t.vehicleNumberPlate}?`)) return;
    this.busyId.set(t.id);
    this.api.deleteVehicleService(t.id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.notice.set({ tone: 'success', text: `Transaction for ${t.vehicleNumberPlate} was deleted.` });
        this.transactions.reload();
      },
      error: (err) => {
        this.busyId.set(null);
        this.notice.set({ tone: 'error', text: toErrorMessage(err) });
      },
    });
  }

  // ---------- Create / edit dialog ----------
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  protected readonly editing = signal<VehicleServiceTransaction | null>(null);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly form = this.fb.group({
    vehicleNumberPlate: ['', [Validators.required, Validators.maxLength(20)]],
    serviceType: ['', [Validators.required, Validators.maxLength(150)]],
    amountCharged: ['', [Validators.required, amountValidator]],
    assignedTo: ['', Validators.maxLength(150)],
    vehicleKeyShared: [false],
    entryDateTime: ['', Validators.required],
    expectedExitTime: [''],
    actualExitDateTime: [''],
    customerName: ['', Validators.maxLength(150)],
    customerPhoneNumber: ['', [Validators.required, Validators.pattern(PHONE)]],
    customerAlternativePhone: ['', Validators.pattern(PHONE)],
    customerNationality: ['', Validators.maxLength(100)],
    customerIdDocumentType: this.fb.control<IdDocumentType | ''>(''),
    customerIdNumber: ['', Validators.maxLength(50)],
    customerAddress: ['', Validators.maxLength(255)],
    generalComment: [''],
    cancelled: [false],
    vehicleClearanceStatus: this.fb.control<VehicleClearanceStatus>('IN_SERVICE'),
  });

  constructor() {
    // Number plates are always stored and shown in capitals (typed, pasted or autofilled).
    const plate = this.form.controls.vehicleNumberPlate;
    plate.valueChanges.pipe(takeUntilDestroyed()).subscribe((value) => {
      const upper = value.toUpperCase();
      if (upper !== value) plate.setValue(upper, { emitEvent: false });
    });
  }

  protected openCreate(): void {
    this.editing.set(null);
    this.form.reset({ entryDateTime: toLocalInput(new Date()), customerNationality: this.clientCountry() });
    this.form.controls.vehicleClearanceStatus.enable();
    this.form.controls.actualExitDateTime.disable();
    this.openDialog();
  }

  protected openEdit(t: VehicleServiceTransaction): void {
    this.editing.set(t);
    this.form.reset({
      vehicleNumberPlate: t.vehicleNumberPlate,
      serviceType: t.serviceType,
      amountCharged: formatAmount(String(t.amountCharged)),
      assignedTo: t.assignedTo ?? '',
      vehicleKeyShared: !!t.vehicleKeyShared,
      entryDateTime: toLocalInput(t.entryDateTime),
      expectedExitTime: toLocalInput(t.expectedExitTime),
      actualExitDateTime: toLocalInput(t.actualExitDateTime),
      customerName: t.customerName ?? '',
      customerPhoneNumber: t.customerPhoneNumber,
      customerAlternativePhone: t.customerAlternativePhone ?? '',
      customerNationality: t.customerNationality ?? '',
      customerIdDocumentType: t.customerIdDocumentType ?? '',
      customerIdNumber: t.customerIdNumber ?? '',
      customerAddress: t.customerAddress ?? '',
      generalComment: t.generalComment ?? '',
      cancelled: t.paymentStatus === 'CANCELLED',
      vehicleClearanceStatus: t.vehicleClearanceStatus,
    });
    // CLEARED / RELEASED are set by the Clear / Exit actions, not by editing.
    if (this.isOpen(t)) this.form.controls.vehicleClearanceStatus.enable();
    else this.form.controls.vehicleClearanceStatus.disable();
    if (t.vehicleClearanceStatus === 'RELEASED') this.form.controls.actualExitDateTime.enable();
    else this.form.controls.actualExitDateTime.disable();
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

  /** Picking a catalog service pre-fills the amount (only when it's still empty). */
  protected onServiceType(value: string): void {
    const match = this.serviceOptions().find((s) => s.serviceName === value.trim());
    if (match && !this.form.controls.amountCharged.value) {
      this.form.controls.amountCharged.setValue(formatAmount(String(match.price)));
    }
  }

  protected onAmountInput(event: Event): void {
    const el = event.target as HTMLInputElement;
    const next = formatAmount(el.value);
    if (next !== el.value) {
      el.value = next;
      this.form.controls.amountCharged.setValue(next, { emitEvent: false });
    }
  }

  protected onPlateInput(event: Event): void {
    const el = event.target as HTMLInputElement;
    const next = el.value.toUpperCase();
    if (next === el.value) return;
    const caret = el.selectionStart;
    el.value = next;
    this.form.controls.vehicleNumberPlate.setValue(next, { emitEvent: false });
    if (caret !== null) el.setSelectionRange(caret, caret);
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
    const opt = (s: string) => s.trim() || undefined;
    const request: VehicleServiceTransactionRequest = {
      clientRef: this.layout.id(),
      vehicleNumberPlate: v.vehicleNumberPlate.trim().toUpperCase(),
      serviceType: v.serviceType.trim(),
      amountCharged: parseAmount(v.amountCharged),
      assignedTo: opt(v.assignedTo),
      vehicleKeyShared: v.vehicleKeyShared,
      entryDateTime: opt(v.entryDateTime),
      expectedExitTime: opt(v.expectedExitTime),
      actualExitDateTime: opt(v.actualExitDateTime),
      customerName: opt(v.customerName),
      customerPhoneNumber: v.customerPhoneNumber.trim(),
      customerAlternativePhone: opt(v.customerAlternativePhone),
      customerNationality: opt(v.customerNationality),
      customerIdDocumentType: v.customerIdDocumentType || undefined,
      customerIdNumber: opt(v.customerIdNumber)?.toUpperCase(),
      customerAddress: opt(v.customerAddress),
      generalComment: opt(v.generalComment),
      // PENDING here just means "not cancelled" - the API derives the real status from payments.
      paymentStatus: v.cancelled ? 'CANCELLED' : 'PENDING',
      vehicleClearanceStatus: v.vehicleClearanceStatus,
    };
    const current = this.editing();
    const call = current ? this.api.updateVehicleService(current.id, request) : this.api.createVehicleService(request);

    this.saving.set(true);
    this.formError.set(null);
    call.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.dialog().nativeElement.close();
        this.notice.set({ tone: 'success', text: `Transaction for ${saved.vehicleNumberPlate} was ${current ? 'updated' : 'recorded'}.` });
        if (!current) this.page.set(0);
        this.transactions.reload();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(toErrorMessage(err));
      },
    });
  }

  // ---------- Workflow helpers ----------
  protected isOpen(t: VehicleServiceTransaction): boolean {
    return t.vehicleClearanceStatus === 'IN_SERVICE' || t.vehicleClearanceStatus === 'READY';
  }
  protected canClear(t: VehicleServiceTransaction): boolean {
    return this.isOpen(t) && t.paymentStatus === 'PAID';
  }
  protected canExit(t: VehicleServiceTransaction): boolean {
    return t.vehicleClearanceStatus === 'CLEARED';
  }
  protected clearHint(t: VehicleServiceTransaction): string {
    if (!this.isOpen(t)) return `Already ${humanize(t.vehicleClearanceStatus).toLowerCase()}`;
    if (t.paymentStatus === 'CANCELLED') return 'Transaction is cancelled';
    return t.paymentStatus === 'PAID' ? 'Clear vehicle' : `Balance of ${this.formatMoney(t.balance)} must be paid first`;
  }

  // ---------- Clear / exit dialog ----------
  private readonly actionDialog = viewChild.required<ElementRef<HTMLDialogElement>>('actionDialog');
  protected readonly action = signal<{ kind: 'clear' | 'exit'; tx: VehicleServiceTransaction } | null>(null);
  protected readonly actionSaving = signal(false);
  protected readonly actionError = signal<string | null>(null);
  protected readonly actionForm = this.fb.group({ at: ['', Validators.required], by: ['', Validators.maxLength(150)], comment: [''] });

  protected openAction(kind: 'clear' | 'exit', tx: VehicleServiceTransaction): void {
    this.action.set({ kind, tx });
    this.actionError.set(null);
    this.actionForm.reset({ at: toLocalInput(new Date()) });
    this.actionDialog().nativeElement.showModal();
  }

  protected closeAction(): void {
    if (this.actionSaving()) return;
    this.actionDialog().nativeElement.close();
  }

  protected submitAction(): void {
    const current = this.action();
    if (!current) return;
    if (this.actionForm.invalid) {
      this.actionForm.markAllAsTouched();
      return;
    }
    const v = this.actionForm.getRawValue();
    const body = { at: v.at || undefined, by: v.by.trim() || undefined, comment: v.comment.trim() || undefined };
    const call = current.kind === 'clear' ? this.api.clearVehicle(current.tx.id, body) : this.api.exitVehicle(current.tx.id, body);
    this.actionSaving.set(true);
    this.actionError.set(null);
    call.subscribe({
      next: (saved) => {
        this.actionSaving.set(false);
        this.actionDialog().nativeElement.close();
        this.notice.set({
          tone: 'success',
          text: current.kind === 'clear' ? `${saved.vehicleNumberPlate} is cleared to leave.` : `${saved.vehicleNumberPlate} has exited.`,
        });
        this.transactions.reload();
      },
      error: (err) => {
        this.actionSaving.set(false);
        this.actionError.set(toErrorMessage(err));
      },
    });
  }

  // ---------- Payments dialog ----------
  private readonly paymentsDialog = viewChild.required<ElementRef<HTMLDialogElement>>('paymentsDialog');
  protected readonly payTx = signal<VehicleServiceTransaction | null>(null);
  protected readonly payments = signal<TransactionPayment[]>([]);
  protected readonly paymentsLoading = signal(false);
  protected readonly paymentSaving = signal(false);
  protected readonly paymentError = signal<string | null>(null);
  protected readonly paymentBusyId = signal<string | null>(null);

  protected readonly paymentForm = this.fb.group({
    amount: ['', [Validators.required, amountValidator]],
    paymentMethod: this.fb.control<PaymentMethod>('CASH'),
    referenceNumber: ['', Validators.maxLength(100)],
    paidByName: ['', Validators.maxLength(150)],
    paidByPhone: ['', Validators.pattern(PHONE)],
    receivedBy: ['', Validators.maxLength(150)],
    paymentDateTime: ['', Validators.required],
    comment: [''],
  });

  protected openPayments(tx: VehicleServiceTransaction): void {
    this.payTx.set(tx);
    this.payments.set([]);
    this.paymentError.set(null);
    this.resetPaymentForm(tx);
    this.loadPayments(tx.id);
    this.paymentsDialog().nativeElement.showModal();
  }

  protected closePayments(): void {
    if (this.paymentSaving()) return;
    this.paymentsDialog().nativeElement.close();
  }

  private resetPaymentForm(tx: VehicleServiceTransaction): void {
    this.paymentForm.reset({
      amount: tx.balance > 0 ? formatAmount(String(tx.balance)) : '',
      paymentMethod: 'CASH',
      paidByName: tx.customerName ?? '',
      paidByPhone: tx.customerPhoneNumber ?? '',
      paymentDateTime: toLocalInput(new Date()),
    });
  }

  private loadPayments(id: string): void {
    this.paymentsLoading.set(true);
    this.api.transactionPayments(id).subscribe({
      next: (list) => { this.payments.set(list); this.paymentsLoading.set(false); },
      error: (err) => { this.paymentsLoading.set(false); this.paymentError.set(toErrorMessage(err)); },
    });
  }

  /** Re-reads the transaction so the summary (paid / balance / status) is current. */
  private refreshPayTx(id: string): void {
    this.api.vehicleService(id).subscribe({
      next: (fresh) => { this.payTx.set(fresh); this.resetPaymentForm(fresh); },
    });
    this.loadPayments(id);
    this.transactions.reload();
  }

  protected onPaymentAmountInput(event: Event): void {
    const el = event.target as HTMLInputElement;
    const next = formatAmount(el.value);
    if (next !== el.value) {
      el.value = next;
      this.paymentForm.controls.amount.setValue(next, { emitEvent: false });
    }
  }

  protected paymentInvalid(name: keyof typeof this.paymentForm.controls): boolean {
    const c = this.paymentForm.controls[name];
    return c.invalid && (c.touched || c.dirty);
  }

  protected submitPayment(): void {
    const tx = this.payTx();
    if (!tx) return;
    if (this.paymentForm.invalid) {
      this.paymentForm.markAllAsTouched();
      return;
    }
    const v = this.paymentForm.getRawValue();
    const amount = parseAmount(v.amount);
    if (amount <= 0 || amount > tx.balance) {
      this.paymentError.set(`Amount must be between 1 and the balance of ${this.formatMoney(tx.balance)}.`);
      return;
    }
    const opt = (s: string) => s.trim() || undefined;
    this.paymentSaving.set(true);
    this.paymentError.set(null);
    this.api
      .addTransactionPayment(tx.id, {
        amount,
        paymentMethod: v.paymentMethod,
        referenceNumber: opt(v.referenceNumber)?.toUpperCase(),
        paidByName: opt(v.paidByName),
        paidByPhone: opt(v.paidByPhone),
        receivedBy: opt(v.receivedBy),
        paymentDateTime: opt(v.paymentDateTime),
        comment: opt(v.comment),
      })
      .subscribe({
        next: (p) => {
          this.paymentSaving.set(false);
          this.notice.set({ tone: 'success', text: `Payment of ${this.formatMoney(p.amount)} recorded for ${tx.vehicleNumberPlate}.` });
          this.refreshPayTx(tx.id);
        },
        error: (err) => {
          this.paymentSaving.set(false);
          this.paymentError.set(toErrorMessage(err));
        },
      });
  }

  protected removePayment(p: TransactionPayment): void {
    const tx = this.payTx();
    if (!tx || !confirm(`Remove the payment of ${this.formatMoney(p.amount)}?`)) return;
    this.paymentBusyId.set(p.id);
    this.api.deleteTransactionPayment(tx.id, p.id).subscribe({
      next: () => { this.paymentBusyId.set(null); this.refreshPayTx(tx.id); },
      error: (err) => { this.paymentBusyId.set(null); this.paymentError.set(toErrorMessage(err)); },
    });
  }
}
