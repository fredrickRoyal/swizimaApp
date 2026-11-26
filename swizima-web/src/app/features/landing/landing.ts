import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { MOBILE_APP_DOWNLOAD_URL } from '../../core/config/api.config';
import { Icon, IconName } from '../../shared/icon/icon';
import { ThemeToggle } from '../../shared/theme-toggle/theme-toggle';

interface FeatureItem {
  icon: IconName;
  title: string;
  description: string;
}

@Component({
  selector: 'app-landing',
  imports: [RouterLink, DecimalPipe, Icon, ThemeToggle],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './landing.html',
  styleUrl: './landing.css',
})
export class Landing {
  protected readonly auth = inject(AuthService);
  protected readonly year = new Date().getFullYear();

  protected readonly mobileAppUrl = inject(MOBILE_APP_DOWNLOAD_URL);

  /** Controls the interactive Pricing & Wallet Calculator modal. */
  protected readonly pricingModalOpen = signal(false);

  /** Interactive calculator state inside the pricing modal. */
  protected readonly selectedPlan = signal<'with-printer' | 'without-printer'>('with-printer');
  protected readonly selectedBillingMode = signal<'commission' | 'monthly'>('commission');
  protected readonly sampleTicketAmount = signal(5000);
  protected readonly sampleWalletTopup = signal(50000);
  protected readonly customMonthlyFee = signal(150000);
  protected readonly estimatedCarsPerDay = signal(40);

  protected readonly onboardingFee = computed(() =>
    this.selectedPlan() === 'with-printer' ? 200_000 : 50_000,
  );
  protected readonly commissionPerTicket = computed(() =>
    this.selectedBillingMode() === 'commission' ? Math.round(this.sampleTicketAmount() * 0.05) : 0,
  );
  protected readonly starterTicketsCount = computed(() => {
    const comm = Math.round(this.sampleTicketAmount() * 0.05);
    return comm > 0 ? Math.floor(10_000 / comm) : 0;
  });
  protected readonly topupTicketsCount = computed(() => {
    const comm = Math.round(this.sampleTicketAmount() * 0.05);
    return comm > 0 ? Math.floor(this.sampleWalletTopup() / comm) : 0;
  });
  protected readonly monthlyCarsCount = computed(() => this.estimatedCarsPerDay() * 30);
  protected readonly effectiveMonthlyCostPerCar = computed(() => {
    const cars = this.monthlyCarsCount();
    return cars > 0 ? Math.round(this.customMonthlyFee() / cars) : 0;
  });

  protected readonly features: FeatureItem[] = [
    {
      icon: 'parking',
      title: 'Car Park Management',
      description:
        'Record vehicle check-ins and check-outs in seconds, print thermal receipts on the spot, and prevent revenue leakage at entry and exit gates.',
    },
    {
      icon: 'car',
      title: 'Car Wash & Bay Operations',
      description:
        'Configure custom wash packages by vehicle type, assign attendants, issue physical or WhatsApp tickets, and track every bay in real time.',
    },
    {
      icon: 'wrench',
      title: 'Workshops & Service Records',
      description:
        'Log repairs, maintenance jobs, and itemized service catalogs with full customer history and instant digital receipts.',
    },
    {
      icon: 'printer',
      title: 'Portable Bluetooth Printing',
      description:
        'Pair your Android phone with the Swizima portable Bluetooth thermal printer to issue branded paper tickets anywhere on your premises.',
    },
    {
      icon: 'wallet',
      title: '5% Wallet Commission or Custom Monthly Fee',
      description:
        'Pay 5% per transaction from your prepaid Swizima Wallet (with UGX 10,000 free starter credit), or choose a custom agreed end-of-month fee with 0% per-transaction deduction.',
    },
    {
      icon: 'trendingUp',
      title: 'Live Reports & Multi-Staff Control',
      description:
        'Monitor daily collections, staff activity, and transaction reports from the web admin portal or mobile app from anywhere.',
    },
  ];

  protected readonly customerBenefits: FeatureItem[] = [
    {
      icon: 'shield',
      title: 'Assurance of Authorized Handover',
      description:
        'Every motorist receives an official printed or WhatsApp receipt bearing the station details, vehicle registration, and attendant record—confirming their vehicle is in the right hands.',
    },
    {
      icon: 'receipt',
      title: 'Greater Customer Confidence & Trust',
      description:
        'Clear proof of custody and transparent pricing build lasting trust, encouraging vehicle owners to return to your car park, wash bay, or workshop with complete peace of mind.',
    },
    {
      icon: 'sms',
      title: 'Easy Owner Contact Anytime',
      description:
        'Because the motorist’s phone number is captured alongside their vehicle plate, attendants can easily reach the owner when their wash is ready or if there is any urgent need regarding their car.',
    },
  ];

  protected openPricingModal(
    plan?: 'with-printer' | 'without-printer',
    billingMode?: 'commission' | 'monthly',
  ): void {
    if (plan) {
      this.selectedPlan.set(plan);
    }
    if (billingMode) {
      this.selectedBillingMode.set(billingMode);
    }
    this.pricingModalOpen.set(true);
  }

  protected closePricingModal(): void {
    this.pricingModalOpen.set(false);
  }

  protected updateTicketAmount(value: string): void {
    const parsed = Number(value);
    if (!Number.isNaN(parsed) && parsed >= 500) {
      this.sampleTicketAmount.set(parsed);
    }
  }

  protected updateWalletTopup(value: string): void {
    const parsed = Number(value);
    if (!Number.isNaN(parsed) && parsed >= 1000) {
      this.sampleWalletTopup.set(parsed);
    }
  }

  protected updateCustomMonthlyFee(value: string): void {
    const parsed = Number(value);
    if (!Number.isNaN(parsed) && parsed >= 10000) {
      this.customMonthlyFee.set(parsed);
    }
  }

  protected updateEstimatedCarsPerDay(value: string): void {
    const parsed = Number(value);
    if (!Number.isNaN(parsed) && parsed >= 1) {
      this.estimatedCarsPerDay.set(parsed);
    }
  }
}

