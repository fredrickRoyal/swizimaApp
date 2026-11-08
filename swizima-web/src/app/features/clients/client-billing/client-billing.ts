import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, OnInit, computed, inject, input, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { SwizimaApi } from '../../../core/api/swizima-api';
import { remoteData } from '../../../core/data/remote-data';
import { toErrorMessage } from '../../../core/http/api-error';
import {
  BillingActivityType,
  BillingWallet,
  CreditLoad,
  CreditPaymentMethod,
  CreditRequestStatus,
  CreditTopUpRequest,
} from '../../../core/models/api.models';
import { EmptyState } from '../../../shared/empty-state/empty-state';
import { Icon } from '../../../shared/icon/icon';
import { Pagination } from '../../../shared/pagination/pagination';
import { StatusBadge } from '../../../shared/status-badge/status-badge';
import { ApproveCreditRequestDialog } from '../../billing/approve-credit-request-dialog';
import {
  ACTIVITY_META,
  PAYMENT_METHODS,
  billingProblem,
  formatAmount,
  money,
  parseAmount,
  paymentMethodLabel,
  percent,
} from '../../billing/billing-ui';
import { LoadCreditDialog } from '../../billing/load-credit-dialog';
import { ClientLayout } from '../client-layout/client-layout';

const PAGE_SIZE = 8;
const QUICK_AMOUNTS = [10_000, 25_000, 50_000, 100_000, 250_000];

/** Wallet lookup result: the wallet, or "no billing plan" (402 BILLING_NOT_CONFIGURED). */
type WalletResult = { wallet: BillingWallet } | { notConfigured: true };

/** /clients/:id/billing & /portal/billing — billing plan, credit balance, top-ups/requests and activity. */
@Component({
  selector: 'app-client-billing',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    EmptyState,
    Icon,
    LoadCreditDialog,
    ApproveCreditRequestDialog,
    Pagination,
    StatusBadge,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-billing.html',
  styleUrl: '../../billing/billing.css',
})
export class ClientBilling implements OnInit {
  private readonly api = inject(SwizimaApi);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly layout = inject(ClientLayout);

  /** Route data when mounted at /portal/topup-requests. */
  readonly defaultView = input<'activity' | 'loads' | 'requests'>();

  ngOnInit(): void {
    const initial = this.defaultView();
    if (initial) {
      this.view.set(initial);
    }
  }

  protected readonly money = money;
  protected readonly percent = percent;
  protected readonly methodLabel = paymentMethodLabel;
  protected readonly methods = PAYMENT_METHODS;
  protected readonly quickAmounts = QUICK_AMOUNTS;
  protected readonly activityMeta = ACTIVITY_META;
  protected readonly activityTypes = Object.keys(ACTIVITY_META) as BillingActivityType[];
  protected readonly pageSize = PAGE_SIZE;

  protected readonly notice = signal<{ tone: 'success' | 'error'; text: string } | null>(null);
  private readonly loadDialog = viewChild(LoadCreditDialog);
  private readonly reviewDialog = viewChild(ApproveCreditRequestDialog);
  private readonly requestDialog = viewChild<ElementRef<HTMLDialogElement>>('requestDialog');

  protected readonly wallet = remoteData(
    computed(() => this.layout.id()),
    (clientRef) =>
      this.api.billingWallet(clientRef).pipe(
        map((wallet): WalletResult => ({ wallet })),
        catchError((err) => {
          if (billingProblem(err)?.code === 'BILLING_NOT_CONFIGURED') return of<WalletResult>({ notConfigured: true });
          throw err;
        }),
      ),
  );

  protected readonly walletData = computed(() => {
    const d = this.wallet.state().data;
    return d && 'wallet' in d ? d.wallet : null;
  });
  protected readonly notConfigured = computed(() => {
    const d = this.wallet.state().data;
    return !!d && 'notConfigured' in d;
  });

  // ---------- Activity, Loads & Credit Requests ----------
  protected readonly view = signal<'activity' | 'loads' | 'requests'>('activity');
  protected readonly activityType = signal<BillingActivityType | ''>('');
  protected readonly activityPage = signal(0);
  protected readonly activity = remoteData(
    computed(() => ({
      clientRef: this.layout.id(),
      type: this.activityType(),
      page: this.activityPage(),
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
    })),
    (q) => this.api.billingActivities(q),
  );

  protected readonly loadsPage = signal(0);
  protected readonly loads = remoteData(
    computed(() => ({ clientRef: this.layout.id(), page: this.loadsPage(), size: PAGE_SIZE, sort: 'loadedAt,desc' })),
    (q) => this.api.creditLoads(q),
  );

  protected readonly requestStatusFilter = signal<CreditRequestStatus | ''>('');
  protected readonly requestsPage = signal(0);
  protected readonly creditRequests = remoteData(
    computed(() => ({
      clientRef: this.layout.id(),
      status: this.requestStatusFilter(),
      page: this.requestsPage(),
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
    })),
    (q) => this.api.creditRequests(q),
  );

  protected openLoad(): void {
    if (!this.layout.isAdmin()) return;
    const w = this.walletData();
    this.loadDialog()?.open(this.layout.id(), {
      clientName: this.layout.client.state().data?.clientName,
      currency: w?.currency,
      balance: w?.balance,
    });
  }

  protected onLoaded(load: CreditLoad): void {
    this.notice.set({ tone: 'success', text: `${money(load.amount, load.currency)} loaded. New balance ${money(load.balanceAfter, load.currency)}.` });
    this.refresh();
  }

  protected readonly busyId = signal<string | null>(null);
  protected reverse(load: CreditLoad): void {
    if (!this.layout.isAdmin()) return;
    const reason = prompt(`Reverse ${money(load.amount, load.currency)}?\n\nReason (required):`);
    if (!reason?.trim()) return;
    this.busyId.set(load.id);
    this.api.reverseCreditLoad(load.id, reason.trim()).subscribe({
      next: () => {
        this.busyId.set(null);
        this.notice.set({ tone: 'success', text: `Credit load of ${money(load.amount, load.currency)} was reversed.` });
        this.refresh();
      },
      error: (err) => { this.busyId.set(null); this.notice.set({ tone: 'error', text: toErrorMessage(err) }); },
    });
  }

  // ---------- Super Admin: Approve / Reject credit request ----------
  protected openApproveRequest(req: CreditTopUpRequest): void {
    if (!this.layout.isAdmin()) return;
    this.reviewDialog()?.openApprove(req, {
      clientName: this.layout.client.state().data?.clientName,
      balance: this.walletData()?.balance,
    });
  }

  protected openRejectRequest(req: CreditTopUpRequest): void {
    if (!this.layout.isAdmin()) return;
    this.reviewDialog()?.openReject(req, {
      clientName: this.layout.client.state().data?.clientName,
      balance: this.walletData()?.balance,
    });
  }

  protected onRequestReviewed(req: CreditTopUpRequest): void {
    if (req.status === 'APPROVED') {
      this.notice.set({
        tone: 'success',
        text: `Credit request approved — ${money(req.approvedAmount ?? req.requestedAmount, req.currency)} loaded into wallet.`,
      });
    } else {
      this.notice.set({
        tone: 'success',
        text: `Credit request for ${money(req.requestedAmount, req.currency)} was rejected.`,
      });
    }
    this.refresh();
  }

  // ---------- Request credit (for non-admin client users) ----------
  protected readonly requestForm = this.fb.group({
    amount: ['', Validators.required],
    paymentMethod: this.fb.control<CreditPaymentMethod>('MOBILE_MONEY'),
    paymentReference: [''],
    contactPhone: [''],
    notes: [''],
  });
  protected readonly requestError = signal<string | null>(null);
  protected readonly requestSaving = signal(false);

  protected openRequestCredit(): void {
    this.requestError.set(null);
    const client = this.layout.client.state().data;
    this.requestForm.reset({
      amount: '',
      paymentMethod: 'MOBILE_MONEY',
      paymentReference: '',
      contactPhone: client?.phoneNumber ?? '',
      notes: '',
    });
    this.requestDialog()?.nativeElement.showModal();
  }

  protected closeRequestCredit(): void {
    if (!this.requestSaving()) this.requestDialog()?.nativeElement.close();
  }

  protected onRequestAmountInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const formatted = formatAmount(input.value);
    input.value = formatted;
    this.requestForm.controls.amount.setValue(formatted, { emitEvent: false });
  }

  protected pickRequestAmount(amount: number): void {
    this.requestForm.controls.amount.setValue(formatAmount(String(amount)));
    this.requestForm.controls.amount.markAsDirty();
  }

  protected submitRequestCredit(channel: 'submit' | 'whatsapp'): void {
    const raw = this.requestForm.getRawValue();
    const amount = parseAmount(raw.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      this.requestError.set('Enter a credit amount greater than zero.');
      return;
    }
    const w = this.walletData();
    const currency = w?.currency ?? 'UGX';
    const client = this.layout.client.state().data;

    this.requestSaving.set(true);
    this.requestError.set(null);
    this.api
      .submitCreditRequest(this.layout.id(), {
        requestedAmount: amount,
        paymentMethod: raw.paymentMethod,
        paymentReference: raw.paymentReference.trim() || undefined,
        payerName: client?.clientName || undefined,
        payerPhone: raw.contactPhone.trim() || client?.phoneNumber || undefined,
        notes: raw.notes.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.requestSaving.set(false);
          if (channel === 'whatsapp') {
            const msg = [
              `Hello Swizima Admin, I would like to request a credit top-up:`,
              `• Business: ${client?.clientName ?? 'Client'} (${client?.clientCode ?? this.layout.id()})`,
              `• Amount: ${money(amount, currency)}`,
              `• Payment Method: ${paymentMethodLabel(raw.paymentMethod)}`,
              raw.paymentReference.trim() ? `• Payment Reference: ${raw.paymentReference.trim()}` : null,
              raw.contactPhone.trim() ? `• Contact Phone: ${raw.contactPhone.trim()}` : null,
              raw.notes.trim() ? `• Notes: ${raw.notes.trim()}` : null,
            ]
              .filter(Boolean)
              .join('\n');
            window.open(`https://wa.me/256700408387?text=${encodeURIComponent(msg)}`, '_blank', 'noopener');
          }

          this.closeRequestCredit();
          this.view.set('requests');
          this.creditRequests.reload();
          this.notice.set({
            tone: 'success',
            text: `Credit request for ${money(amount, currency)} submitted for administrator approval.`,
          });
        },
        error: (err) => {
          this.requestSaving.set(false);
          this.requestError.set(toErrorMessage(err));
        },
      });
  }

  protected refresh(): void {
    this.wallet.reload();
    this.activity.reload();
    this.loads.reload();
    this.creditRequests.reload();
  }
}
