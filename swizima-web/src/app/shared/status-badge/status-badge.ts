import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

const TONE: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'neutral'> = {
  ACTIVE: 'success',
  ENABLED: 'success',
  PENDING: 'warning',
  PENDING_ACTIVATION: 'warning',
  SUSPENDED: 'danger',
  LOCKED: 'danger',
  DEACTIVATED: 'neutral',
  INACTIVE: 'neutral',
  DISABLED: 'neutral',
  SUPER_ADMIN: 'info',
  ADMIN: 'info',
};

/** Coloured pill for enum statuses, e.g. ACTIVE -> green "Active". */
@Component({
  selector: 'app-status-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span [class]="'badge badge-' + tone()">{{ label() }}</span>`,
})
export class StatusBadge {
  readonly status = input.required<string>();
  protected readonly tone = computed(() => TONE[this.status()] ?? 'neutral');
  protected readonly label = computed(() => humanize(this.status()));
}

export function humanize(value: string | null | undefined): string {
  if (!value) return '';
  const text = value.replaceAll('_', ' ').toLowerCase();
  return text.charAt(0).toUpperCase() + text.slice(1);
}
