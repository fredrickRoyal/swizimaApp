import { ChangeDetectionStrategy, Component, computed, effect, forwardRef, input, signal, untracked } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DIAL_COUNTRIES, DialCountry, findDialCountry, splitPhone } from '../../core/data/dial-codes';

const FALLBACK = DIAL_COUNTRIES[0]!;

/**
 * Phone field with a country-code dropdown. Works with reactive forms
 * (formControlName) and emits "+256 700123456", or '' when no number is entered.
 *
 * Until the user picks a code (or a saved number is loaded), the code follows
 * `defaultCountry` — e.g. the client's country.
 */
@Component({
  selector: 'app-phone-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => PhoneInput), multi: true }],
  host: { '[class.invalid]': 'invalid()', '[class.disabled]': 'disabled()' },
  template: `
    <label class="visually-hidden" [for]="inputId() + '-code'">Country code</label>
    <select class="code" [id]="inputId() + '-code'" [value]="country().iso" [disabled]="disabled()"
            (change)="onCountry($any($event.target).value)" (blur)="onTouched()">
      @for (c of countries; track c.iso) {
        <option [value]="c.iso">{{ c.flag }} {{ c.dial }}</option>
      }
    </select>
    <input class="number" type="tel" inputmode="tel" [id]="inputId()" [value]="local()" [disabled]="disabled()"
           [placeholder]="placeholder()" [attr.autocomplete]="autocomplete()" [attr.aria-invalid]="invalid() || null"
           (input)="onLocal($any($event.target).value)" (blur)="onTouched()" />
  `,
  styles: `
    :host {
      display: flex;
      align-items: stretch;
      min-height: 46px;
      background: var(--bg-elevated);
      border: 1px solid var(--border-strong);
      border-radius: var(--radius);
      transition: border-color var(--duration), box-shadow var(--duration);
      overflow: hidden;
    }
    :host(:hover) { border-color: var(--brand-400); }
    :host(:focus-within) { border-color: var(--brand-500); box-shadow: var(--ring); }
    :host(.invalid) { border-color: var(--danger); }
    :host(.disabled) { opacity: 0.6; }
    select, input { border: 0; background: transparent; color: inherit; font: inherit; font-size: 1rem; outline: none; }
    .code {
      flex: none;
      padding: 0 0.4rem 0 0.75rem;
      border-right: 1px solid var(--border);
      cursor: pointer;
      font-variant-numeric: tabular-nums;
    }
    .number { flex: 1; min-width: 0; padding: 0.65rem 0.85rem; font-variant-numeric: tabular-nums; }
    .number::placeholder { color: var(--text-subtle); }
  `,
})
export class PhoneInput implements ControlValueAccessor {
  /** Country name or ISO code used until the user picks a code, e.g. the client's country. */
  readonly defaultCountry = input<string | null | undefined>('Uganda');
  readonly inputId = input('phone');
  readonly placeholder = input('700 123456');
  readonly autocomplete = input<string>('tel-national');
  readonly invalid = input(false);

  protected readonly countries = DIAL_COUNTRIES;
  protected readonly local = signal('');
  protected readonly disabled = signal(false);
  /** Set when the user picks a code or a saved number carries one. */
  private readonly chosen = signal<DialCountry | null>(null);

  protected readonly country = computed(
    () => this.chosen() ?? findDialCountry(this.defaultCountry()) ?? FALLBACK,
  );

  private onChange: (value: string) => void = () => {};
  protected onTouched: () => void = () => {};

  constructor() {
    let previous: DialCountry | undefined;
    effect(() => {
      const current = this.country();
      if (previous && previous !== current && !untracked(this.chosen) && untracked(this.local).trim()) {
        untracked(() => this.emit());
      }
      previous = current;
    });
  }

  writeValue(value: string | null): void {
    const { country, local } = splitPhone(value);
    this.chosen.set(country ?? null);
    this.local.set(local);
  }

  registerOnChange(fn: (value: string) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
  setDisabledState(disabled: boolean): void { this.disabled.set(disabled); }

  protected onCountry(iso: string): void {
    this.chosen.set(DIAL_COUNTRIES.find((c) => c.iso === iso) ?? null);
    this.emit();
  }

  protected onLocal(raw: string): void {
    // Pasted full international number: adopt its code.
    if (raw.trim().startsWith('+')) {
      const { country, local } = splitPhone(raw);
      if (country) {
        this.chosen.set(country);
        raw = local;
      }
    }
    this.local.set(raw);
    this.emit();
  }

  private emit(): void {
    // Drop the trunk "0" (0700… -> 700…) since the country code replaces it.
    const local = this.local().trim().replace(/^0+/, '');
    this.onChange(local ? `${this.country().dial} ${local}` : '');
  }
}
