import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { SwizimaApi } from '../../../core/api/swizima-api';
import { remoteData } from '../../../core/data/remote-data';
import { toErrorMessage } from '../../../core/http/api-error';
import { BillingActivityType, BillingWallet, CreditLoad } from '../../../core/models/api.models';
import { EmptyState } from '../../../shared/empty-state/empty-state';
import { Icon } from '../../../shared/icon/icon';
import { Pagination } from '../../../shared/pagination/pagination';
import { StatusBadge } from '../../../shared/status-badge/status-badge';
import { ACTIVITY_META, billingProblem, money, paymentMethodLabel, percent } from '../../billing/billing-ui';
import { LoadCreditDialog } from '../../billing/load-credit-dialog';
import { ClientLayout } from '../client-layout/client-layout';

const PAGE_SIZE = 8;

/** Wallet lookup result: the wallet, or "no billing plan" (402 BILLING_NOT_CONFIGURED). */
type WalletResult = { wallet: BillingWallet } | { notConfigured: true };

/** /clients/:id/billing — the client's billing plan, credit balance, top-ups and activity. */
@Component({
  selector: 'app-client-billing',
  imports: [DatePipe, RouterLink, EmptyState, Icon, LoadCreditDialog, Pagination, StatusBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-billing.html',
  styleUrl: '../../billing/billing.css',
})
export class ClientBilling {
  private readonly api = inject(SwizimaApi);
  protected readonly layout = inject(ClientLayout);

  protected readonly money = money;
  protected readonly percent = percent;
  protected readonly methodLabel = paymentMethodLabel;
  protected readonly activityMeta = ACTIVITY_META;
  protected readonly activityTypes = Object.keys(ACTIVITY_META) as BillingActivityType[];
  protected readonly pageSize = PAGE_SIZE;

  protected readonly notice = signal<{ tone: 'success' | 'error'; text: string } | null>(null);
  private readonly loadDialog = viewChild.required(LoadCreditDialog);

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

  // ---------- Activity ----------
  protected readonly view = signal<'activity' | 'loads'>('activity');
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

  protected openLoad(): void {
    const w = this.walletData();
    this.loadDialog().open(this.layout.id(), {
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

  protected refresh(): void {
    this.wallet.reload();
    this.activity.reload();
    this.loads.reload();
  }
}
