import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SwizimaApi } from '../../core/api/swizima-api';
import { debounced, remoteData } from '../../core/data/remote-data';
import { toErrorMessage } from '../../core/http/api-error';
import {
  BillingActivityType,
  BillingWallet,
  Client,
  CreditLoad,
  CreditLoadStatus,
  CreditPaymentMethod,
} from '../../core/models/api.models';
import { ClientPicker } from '../../shared/client-picker/client-picker';
import { EmptyState } from '../../shared/empty-state/empty-state';
import { Icon, IconName } from '../../shared/icon/icon';
import { PageHeader } from '../../shared/page-header/page-header';
import { Pagination } from '../../shared/pagination/pagination';
import { ACTIVITY_META, ClientNames, PAYMENT_METHODS, money, paymentMethodLabel, percent } from '../billing/billing-ui';
import { LoadCreditDialog } from '../billing/load-credit-dialog';

type Section = 'wallets' | 'credit-loads' | 'activity';
const PAGE_SIZE = 10;

/**
 * /payments — client billing (paymentsapi /api/v1/payments/billing):
 * prepaid wallets, credit top-ups and the activity ledger.
 */
@Component({
  selector: 'app-payments',
  imports: [DatePipe, RouterLink, ClientPicker, EmptyState, Icon, LoadCreditDialog, PageHeader, Pagination],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './payments.html',
  styleUrl: '../billing/billing.css',
})
export class Payments {
  private readonly api = inject(SwizimaApi);
  protected readonly names = inject(ClientNames);

  protected readonly money = money;
  protected readonly percent = percent;
  protected readonly methodLabel = paymentMethodLabel;
  protected readonly activityMeta = ACTIVITY_META;
  protected readonly methods = PAYMENT_METHODS;
  protected readonly activityTypes = Object.keys(ACTIVITY_META) as BillingActivityType[];
  protected readonly pageSize = PAGE_SIZE;

  protected readonly sections: { id: Section; label: string; icon: IconName }[] = [
    { id: 'wallets', label: 'Wallets', icon: 'payments' },
    { id: 'credit-loads', label: 'Credit loads', icon: 'plus' },
    { id: 'activity', label: 'Activity', icon: 'transfer' },
  ];
  protected readonly section = signal<Section>('wallets');

  protected readonly notice = signal<{ tone: 'success' | 'error'; text: string } | null>(null);
  private readonly loadDialog = viewChild.required(LoadCreditDialog);

  // Shared client filter for loads / activity
  protected readonly client = signal<Client | null>(null);
  protected onClient(c: Client | null): void { this.client.set(c); this.loadsPage.set(0); this.activityPage.set(0); }

  // ---------- Wallets ----------
  protected readonly lowOnly = signal(false);
  protected readonly lowLimit = 10000;
  protected readonly walletsPage = signal(0);
  protected readonly wallets = remoteData(
    computed(() => ({
      maxBalance: this.lowOnly() ? this.lowLimit : ('' as const),
      page: this.walletsPage(),
      size: PAGE_SIZE,
      sort: 'balance,asc',
    })),
    (q) => this.api.billingWallets(q),
  );

  protected openLoad(w: BillingWallet): void {
    this.loadDialog().open(w.clientRef, {
      clientName: this.names.get(w.clientRef)()?.name,
      currency: w.currency,
      balance: w.balance,
    });
  }

  protected openLoadForPicked(): void {
    const c = this.client();
    if (c) this.loadDialog().open(c.id, { clientName: c.clientName });
  }

  protected onLoaded(load: CreditLoad): void {
    const name = this.names.get(load.clientRef)()?.name ?? 'client';
    this.notice.set({ tone: 'success', text: `${money(load.amount, load.currency)} loaded for ${name}. New balance ${money(load.balanceAfter, load.currency)}.` });
    this.wallets.reload();
    this.loads.reload();
    this.activity.reload();
  }

  // ---------- Credit loads ----------
  protected readonly loadSearch = signal('');
  private readonly loadSearchTerm = debounced(this.loadSearch, 350);
  protected readonly loadMethod = signal<CreditPaymentMethod | ''>('');
  protected readonly loadStatus = signal<CreditLoadStatus | ''>('');
  protected readonly loadsPage = signal(0);
  protected readonly loads = remoteData(
    computed(() => ({
      clientRef: this.client()?.id,
      search: this.loadSearchTerm().trim(),
      paymentMethod: this.loadMethod(),
      status: this.loadStatus(),
      page: this.loadsPage(),
      size: PAGE_SIZE,
      sort: 'loadedAt,desc',
    })),
    (q) => this.api.creditLoads(q),
  );
  protected readonly busyId = signal<string | null>(null);

  protected reverse(load: CreditLoad): void {
    const reason = prompt(`Reverse ${money(load.amount, load.currency)} loaded on ${new Date(load.loadedAt).toLocaleString()}?\n\nReason (required):`);
    if (!reason?.trim()) return;
    this.busyId.set(load.id);
    this.api.reverseCreditLoad(load.id, reason.trim()).subscribe({
      next: () => {
        this.busyId.set(null);
        this.notice.set({ tone: 'success', text: `Credit load of ${money(load.amount, load.currency)} was reversed.` });
        this.loads.reload();
        this.wallets.reload();
        this.activity.reload();
      },
      error: (err) => { this.busyId.set(null); this.notice.set({ tone: 'error', text: toErrorMessage(err) }); },
    });
  }

  // ---------- Activity ----------
  protected readonly activityType = signal<BillingActivityType | ''>('');
  protected readonly activityPage = signal(0);
  protected readonly activity = remoteData(
    computed(() => ({
      clientRef: this.client()?.id,
      type: this.activityType(),
      page: this.activityPage(),
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
    })),
    (q) => this.api.billingActivities(q),
  );
}
