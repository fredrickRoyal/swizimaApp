import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, Signal, inject, signal } from '@angular/core';
import { SwizimaApi } from '../../core/api/swizima-api';
import {
  BillingActivityType,
  BillingProblem,
  CreditPaymentMethod,
} from '../../core/models/api.models';
import { IconName } from '../../shared/icon/icon';

export const PAYMENT_METHODS: { value: CreditPaymentMethod; label: string }[] = [
  { value: 'MOBILE_MONEY', label: 'Mobile money' },
  { value: 'CASH', label: 'Cash' },
  { value: 'BANK_TRANSFER', label: 'Bank transfer' },
  { value: 'CARD', label: 'Card' },
  { value: 'OTHER', label: 'Other' },
];

export const ACTIVITY_META: Record<BillingActivityType, { label: string; icon: IconName; tone: 'credit' | 'debit' }> = {
  CREDIT_LOAD: { label: 'Credit loaded', icon: 'plus', tone: 'credit' },
  CREDIT_LOAD_REVERSAL: { label: 'Credit load reversed', icon: 'refresh', tone: 'debit' },
  TRANSACTION_CHARGE: { label: 'Transaction charge', icon: 'receipt', tone: 'debit' },
  CHARGE_REVERSAL: { label: 'Charge refunded', icon: 'refresh', tone: 'credit' },
};

export function paymentMethodLabel(m: CreditPaymentMethod): string {
  return PAYMENT_METHODS.find((p) => p.value === m)?.label ?? m;
}

/** 12,500 UGX style (no decimals for whole amounts). */
export function money(amount: number | null | undefined, currency = 'UGX'): string {
  if (amount === null || amount === undefined) return '—';
  const n = Number(amount);
  const text = n.toLocaleString('en-UG', {
    minimumFractionDigits: Number.isInteger(n) ? 0 : 2,
    maximumFractionDigits: 2,
  });
  return `${text} ${currency}`;
}

export function percent(rate: number | null | undefined): string {
  if (rate === null || rate === undefined) return '—';
  return `${Number(rate).toLocaleString('en-UG', { maximumFractionDigits: 4 })}%`;
}

/** Reads the billing ProblemDetail from a failed request, if any. */
export function billingProblem(error: unknown): BillingProblem | null {
  if (!(error instanceof HttpErrorResponse)) return null;
  const body = error.error;
  return body && typeof body === 'object' && 'code' in body ? (body as BillingProblem) : null;
}

/** "1,250,000.5" -> 1250000.5 (NaN when blank/invalid). */
export function parseAmount(text: string): number {
  const raw = text.replace(/,/g, '').trim();
  return raw === '' || raw === '.' ? NaN : Number(raw);
}

/** Keeps digits and one dot (max 2 decimals) and groups the integer part with commas. */
export function formatAmount(text: string): string {
  const cleaned = text.replace(/[^\d.]/g, '');
  const [intPart = '', ...rest] = cleaned.split('.');
  const int = intPart.replace(/^0+(?=\d)/, '').replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return rest.length ? `${int || '0'}.${rest.join('').slice(0, 2)}` : int;
}

/** Re-formats an amount input with commas while typing, keeping the caret position. */
export function onAmountInput(event: Event, set: (value: string) => void): void {
  const el = event.target as HTMLInputElement;
  const caret = el.selectionStart ?? el.value.length;
  const significantBefore = el.value.slice(0, caret).replace(/[^\d.]/g, '').length;
  const formatted = formatAmount(el.value);
  set(formatted);
  el.value = formatted;
  let pos = 0;
  for (let seen = 0; pos < formatted.length && seen < significantBefore; pos++) {
    if (formatted[pos] !== ',') seen++;
  }
  el.setSelectionRange(pos, pos);
}

/**
 * Billing records only carry `clientRef`; this resolves and caches client names
 * (one GET /api/v1/clients/{id} per client, shared across pages).
 */
@Injectable({ providedIn: 'root' })
export class ClientNames {
  private readonly api = inject(SwizimaApi);
  private readonly cache = new Map<string, ReturnType<typeof signal<{ name: string; code?: string } | null>>>();

  get(clientRef: string): Signal<{ name: string; code?: string } | null> {
    let entry = this.cache.get(clientRef);
    if (!entry) {
      entry = signal<{ name: string; code?: string } | null>(null);
      this.cache.set(clientRef, entry);
      const target = entry;
      this.api.client(clientRef).subscribe({
        next: (c) => target.set({ name: c.clientName, code: c.clientCode }),
        error: () => target.set({ name: 'Unknown client' }),
      });
    }
    return entry.asReadonly();
  }
}
