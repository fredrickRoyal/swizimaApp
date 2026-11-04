import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { SwizimaApi } from '../../../core/api/swizima-api';
import { remoteData } from '../../../core/data/remote-data';
import { PaymentStatus, StaffSummary, VehicleClearanceStatus } from '../../../core/models/api.models';
import { EmptyState } from '../../../shared/empty-state/empty-state';
import { Icon } from '../../../shared/icon/icon';
import { Pagination } from '../../../shared/pagination/pagination';
import { StatusBadge, humanize } from '../../../shared/status-badge/status-badge';
import { money } from '../../billing/billing-ui';
import { ClientLayout } from '../client-layout/client-layout';

const PAGE_SIZE = 10;
/** Value sent as `assignedTo` to select unassigned transactions. */
const UNASSIGNED = '-';

type Preset = 'today' | 'yesterday' | 'week' | 'month' | 'custom';

/** "yyyy-MM-dd" in local time. */
function isoDate(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function presetRange(preset: Exclude<Preset, 'custom'>): { from: string; to: string } {
  const today = new Date();
  const d = new Date(today);
  switch (preset) {
    case 'today':
      return { from: isoDate(today), to: isoDate(today) };
    case 'yesterday':
      d.setDate(d.getDate() - 1);
      return { from: isoDate(d), to: isoDate(d) };
    case 'week':
      d.setDate(d.getDate() - ((d.getDay() + 6) % 7)); // Monday
      return { from: isoDate(d), to: isoDate(today) };
    case 'month':
      return { from: isoDate(new Date(today.getFullYear(), today.getMonth(), 1)), to: isoDate(today) };
  }
}

/**
 * /clients/:id/reports — transaction summary for a day or period: totals, payment and
 * clearance status breakdowns, work done per assigned staff member, and the matching transactions.
 */
@Component({
  selector: 'app-client-reports',
  imports: [DatePipe, EmptyState, Icon, Pagination, StatusBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-reports.html',
  styleUrls: ['../../billing/billing.css', './client-reports.css'],
})
export class ClientReports {
  private readonly api = inject(SwizimaApi);
  protected readonly layout = inject(ClientLayout);

  protected readonly money = money;
  protected readonly humanize = humanize;
  protected readonly pageSize = PAGE_SIZE;
  protected readonly today = isoDate(new Date());
  protected readonly paymentStatuses: PaymentStatus[] = ['PENDING', 'PARTIALLY_PAID', 'PAID', 'CANCELLED'];
  protected readonly clearanceStatuses: VehicleClearanceStatus[] = ['IN_SERVICE', 'READY', 'CLEARED', 'RELEASED'];
  protected readonly presets: { value: Preset; label: string }[] = [
    { value: 'today', label: 'Today' },
    { value: 'yesterday', label: 'Yesterday' },
    { value: 'week', label: 'This week' },
    { value: 'month', label: 'This month' },
    { value: 'custom', label: 'Custom' },
  ];

  protected readonly preset = signal<Preset>('today');
  protected readonly fromDate = signal(this.today);
  protected readonly toDate = signal(this.today);

  /** Staff filter for the transactions list: null = everyone, UNASSIGNED = no staff. */
  protected readonly staff = signal<string | null>(null);
  protected readonly paymentFilter = signal<PaymentStatus | ''>('');
  protected readonly clearanceFilter = signal<VehicleClearanceStatus | ''>('');
  protected readonly page = signal(0);

  private readonly range = computed(() => ({
    clientRef: this.layout.id(),
    fromDate: this.fromDate(),
    toDate: this.toDate() < this.fromDate() ? this.fromDate() : this.toDate(),
  }));

  protected readonly summary = remoteData(this.range, (q) => this.api.transactionSummary(q));

  protected readonly transactions = remoteData(
    computed(() => ({
      ...this.range(),
      assignedTo: this.staff() ?? '',
      paymentStatus: this.paymentFilter(),
      clearanceStatus: this.clearanceFilter(),
      page: this.page(),
      size: PAGE_SIZE,
      sort: 'entryDateTime,desc',
    })),
    (q) => this.api.vehicleServices(q),
  );

  protected readonly currency = 'UGX';
  protected readonly singleDay = computed(() => this.range().fromDate === this.range().toDate);

  /** Highest daily count, for scaling the trend bars. */
  protected readonly dailyMax = computed(() =>
    Math.max(1, ...(this.summary.state().data?.daily ?? []).map((d) => d.totals.count)),
  );

  protected readonly collectionRate = computed(() => {
    const t = this.summary.state().data?.totals;
    return t && t.amountCharged > 0 ? Math.round((t.amountPaid / t.amountCharged) * 100) : null;
  });

  protected readonly selectedStaffLabel = computed(() => {
    const s = this.staff();
    return s === null ? null : s === UNASSIGNED ? 'Unassigned' : s;
  });

  protected setPreset(value: Preset): void {
    this.preset.set(value);
    if (value !== 'custom') {
      const r = presetRange(value);
      this.fromDate.set(r.from);
      this.toDate.set(r.to);
      this.page.set(0);
    }
  }

  protected setFrom(value: string): void {
    if (!value) return;
    this.preset.set('custom');
    this.fromDate.set(value);
    if (this.toDate() < value) this.toDate.set(value);
    this.page.set(0);
  }

  protected setTo(value: string): void {
    if (!value) return;
    this.preset.set('custom');
    this.toDate.set(value);
    if (this.fromDate() > value) this.fromDate.set(value);
    this.page.set(0);
  }

  protected staffKey(s: StaffSummary): string {
    return s.assignedTo ?? UNASSIGNED;
  }

  protected toggleStaff(s: StaffSummary): void {
    const key = this.staffKey(s);
    this.staff.set(this.staff() === key ? null : key);
    this.page.set(0);
  }

  protected filterBy(kind: 'payment' | 'clearance', value: string): void {
    if (kind === 'payment') {
      this.paymentFilter.set(this.paymentFilter() === value ? '' : (value as PaymentStatus));
    } else {
      this.clearanceFilter.set(this.clearanceFilter() === value ? '' : (value as VehicleClearanceStatus));
    }
    this.page.set(0);
  }

  protected clearFilters(): void {
    this.staff.set(null);
    this.paymentFilter.set('');
    this.clearanceFilter.set('');
    this.page.set(0);
  }

  /** Share of `count` in the period total, as a CSS percentage. */
  protected share(count: number): string {
    const total = this.summary.state().data?.totals.count ?? 0;
    return total ? `${(count / total) * 100}%` : '0%';
  }

  protected refresh(): void {
    this.summary.reload();
    this.transactions.reload();
  }
}
