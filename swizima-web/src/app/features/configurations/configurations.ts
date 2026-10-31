import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ComingSoon, PlannedFeature } from '../../shared/coming-soon/coming-soon';
import { PageHeader } from '../../shared/page-header/page-header';

/** configurationsapi (port 8082) has no endpoints yet – show what's planned. */
@Component({
  selector: 'app-configurations',
  imports: [ComingSoon, PageHeader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header heading="Configurations" subtitle="System-wide settings that control how Swizima behaves." />
    <app-coming-soon class="page-enter" icon="settings" heading="Configuration centre coming soon"
      message="The configurations service is being built. You'll manage lookups, notification templates and security policies from here."
      [features]="features" />
  `,
})
export class Configurations {
  protected readonly features: PlannedFeature[] = [
    { icon: 'car', title: 'Service categories', description: 'Maintain the list of service types clients can offer.' },
    { icon: 'payments', title: 'Fees & commission', description: 'Platform commission, taxes and currency settings.' },
    { icon: 'mail', title: 'Notification templates', description: 'Edit OTP, welcome and receipt messages per channel.' },
    { icon: 'shield', title: 'Security policies', description: 'OTP expiry, password rules and session timeouts.' },
    { icon: 'clock', title: 'Operating hours', description: 'Default opening hours and public holidays.' },
    { icon: 'bell', title: 'Integrations', description: 'SMS, WhatsApp, email and payment provider credentials.' },
  ];
}
