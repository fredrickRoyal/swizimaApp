import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ComingSoon, PlannedFeature } from '../../shared/coming-soon/coming-soon';
import { PageHeader } from '../../shared/page-header/page-header';

/** paymentsapi (port 8083) has no endpoints yet – show what's planned. */
@Component({
  selector: 'app-payments',
  imports: [ComingSoon, PageHeader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header heading="Payments" subtitle="Collections and settlements for every service on Swizima." />
    <app-coming-soon class="page-enter" icon="payments" heading="Payments are on the way"
      message="The payments service is being built. Once it is live you'll track every transaction, settlement and refund from here."
      [features]="features" />
  `,
})
export class Payments {
  protected readonly features: PlannedFeature[] = [
    { icon: 'sms', title: 'Mobile money', description: 'Collect via MTN MoMo and Airtel Money with instant confirmation.' },
    { icon: 'payments', title: 'Card payments', description: 'Accept Visa and Mastercard for parking, washes and repairs.' },
    { icon: 'trendingUp', title: 'Revenue insights', description: 'Daily, weekly and monthly takings per client and service.' },
    { icon: 'clients', title: 'Client settlements', description: 'Automatic payouts to clients, less Swizima commission.' },
    { icon: 'refresh', title: 'Refunds & reversals', description: 'Handle disputes and failed transactions with a full audit trail.' },
    { icon: 'inbox', title: 'Receipts', description: 'Branded e-receipts sent by email, SMS or WhatsApp.' },
  ];
}
