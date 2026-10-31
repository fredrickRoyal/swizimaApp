import { ChangeDetectionStrategy, Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { SwizimaApi } from '../../../core/api/swizima-api';
import { HasUnsavedChanges } from '../../../core/forms/unsaved-changes.guard';
import { toErrorMessage } from '../../../core/http/api-error';
import { ClientRequest, ClientStatus, ClientType, ProblemDetail } from '../../../core/models/api.models';
import { Icon } from '../../../shared/icon/icon';
import { PageHeader } from '../../../shared/page-header/page-header';
import { PhoneInput } from '../../../shared/phone-input/phone-input';
import { humanize } from '../../../shared/status-badge/status-badge';
import {
  ALL_DISTRICTS,
  UGANDA_DISTRICTS,
  UGANDA_TOWNS,
  districtOfTown,
  townsForDistrict,
} from '../../../core/data/uganda-locations';

const PHONE = /^\+?[0-9 ()-]{7,20}$/;

/** Selectable sectors. Picked values are sent as `industrySectors: string[]` and stored as a JSON array. */
export const INDUSTRY_SECTORS = [
  'Car park & parking',
  'Car wash & detailing',
  'Auto repair & servicing',
  'Tyres & wheel alignment',
  'Spare parts & accessories',
  'Fuel & service station',
  'Car rental & hire',
  'Vehicle dealership',
  'Towing & roadside assistance',
  'Fleet management',
  'Transport & logistics',
  'Driving school',
  'Motor insurance',
] as const;
/** Mirrors ClientRequest.industrySectors: @Size(max = 20) List<@Size(max = 150) String>. */
const MAX_SECTORS = 20;
const MAX_SECTOR_LENGTH = 150;
/** Select value for "Other…" in the district / city dropdowns. */
const OTHER = '__other__';
const LOCATION_MAX = 100;

/** Navigation state passed back to the list after a successful registration. */
export interface ClientCreatedState {
  createdClientId: string;
  createdClientName: string;
  createdClientCode: string;
}

/**
 * /clients/new – register a client (POST /api/v1/clients via the gateway).
 * 400 field errors and 409 duplicate code are mapped back onto the form.
 */
@Component({
  selector: 'app-client-form',
  imports: [ReactiveFormsModule, RouterLink, Icon, PageHeader, PhoneInput],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-form.html',
  styleUrl: './client-form.css',
})
export class ClientForm implements HasUnsavedChanges {
  private readonly api = inject(SwizimaApi);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly types: ClientType[] = ['COMPANY', 'INDIVIDUAL', 'GOVERNMENT', 'NGO', 'OTHER'];
  protected readonly statuses: ClientStatus[] = ['PENDING', 'ACTIVE'];
  protected readonly humanize = humanize;
  protected readonly sectors = INDUSTRY_SECTORS;
  protected readonly districtGroups = UGANDA_DISTRICTS;
  protected readonly other = OTHER;

  protected readonly form = inject(NonNullableFormBuilder).group({
    // Business
    clientName: ['', [Validators.required, Validators.maxLength(255)]],
    clientCode: ['', [Validators.required, Validators.maxLength(50), Validators.pattern(/^[A-Za-z0-9_-]+$/)]],
    tradingName: ['', Validators.maxLength(255)],
    clientType: ['COMPANY' as ClientType],
    industrySectors: [[] as string[]],
    industrySectorOther: [''],
    status: ['PENDING' as ClientStatus],
    description: [''],
    // Contact
    email: ['', [Validators.email, Validators.maxLength(150)]],
    phoneNumber: ['', [Validators.pattern(PHONE), Validators.maxLength(30)]],
    alternativePhone: ['', [Validators.pattern(PHONE), Validators.maxLength(30)]],
    // Location (Uganda: district/city are dropdown values or OTHER + free text)
    address: ['', Validators.maxLength(255)],
    district: ['', Validators.maxLength(LOCATION_MAX)],
    districtOther: [''],
    city: ['', Validators.maxLength(LOCATION_MAX)],
    cityOther: [''],
    country: ['Uganda', Validators.maxLength(LOCATION_MAX)],
    // Contact person
    contactPerson: ['', Validators.maxLength(150)],
    contactPersonTitle: ['', Validators.maxLength(100)],
    contactPersonEmail: ['', [Validators.email, Validators.maxLength(150)]],
    contactPersonPhone: ['', [Validators.pattern(PHONE), Validators.maxLength(30)]],
  });

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  /** "Other…" chip ticked: reveals the free-text input. */
  protected readonly otherSectorOn = signal(false);
  /** Stop auto-suggesting the code once the user edits it themselves. */
  private codeTouchedByUser = false;
  private saved = false;

  hasUnsavedChanges(): boolean {
    return this.form.dirty && !this.saved;
  }

  /** Tab close / reload with unsaved input. */
  @HostListener('window:beforeunload', ['$event'])
  protected onBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.hasUnsavedChanges()) event.preventDefault();
  }

  protected onNameInput(): void {
    if (!this.codeTouchedByUser) {
      this.form.controls.clientCode.setValue(suggestCode(this.form.controls.clientName.value));
    }
  }

  protected onCodeInput(): void {
    this.codeTouchedByUser = true;
    const control = this.form.controls.clientCode;
    const upper = control.value.toUpperCase().replace(/\s+/g, '-');
    if (upper !== control.value) control.setValue(upper);
  }

  protected isSectorSelected(sector: string): boolean {
    return this.form.controls.industrySectors.value.includes(sector);
  }

  protected toggleSector(sector: string): void {
    const control = this.form.controls.industrySectors;
    const selected = new Set(control.value);
    if (selected.has(sector)) selected.delete(sector);
    else selected.add(sector);
    // Keep the catalogue order so the saved string is stable.
    control.setValue(this.sectors.filter((s) => selected.has(s)));
    control.markAsDirty();
    control.markAsTouched();
  }

  /** The free-text field is only required (and kept) while "Other…" is ticked. */
  protected toggleOther(): void {
    const on = !this.otherSectorOn();
    this.otherSectorOn.set(on);
    const other = this.form.controls.industrySectorOther;
    if (on) {
      other.setValidators(Validators.required);
      queueMicrotask(() => this.host.nativeElement.querySelector<HTMLElement>('#cf-sector-other')?.focus());
    } else {
      other.clearValidators();
      other.reset('');
    }
    other.updateValueAndValidity();
    this.form.markAsDirty();
  }

  /** Number of sectors that will be saved (ticked + typed). */
  protected sectorCount(): number {
    return this.collectSectors().length;
  }

  /** Ticked sectors, then any typed under "Other…" (comma separated), de-duplicated case-insensitively. */
  private collectSectors(): string[] {
    const typed = this.otherSectorOn()
      ? this.form.controls.industrySectorOther.value.split(',').map((s) => s.trim()).filter(Boolean)
      : [];
    const seen = new Set<string>();
    return [...this.form.controls.industrySectors.value, ...typed].filter((s) => {
      const key = s.toLowerCase();
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  }

  protected regenerateCode(): void {
    this.codeTouchedByUser = false;
    this.form.controls.clientCode.setValue(suggestCode(this.form.controls.clientName.value || 'Client'));
    this.form.controls.clientCode.markAsDirty();
  }

  // ---------- Location ----------

  /** District / city dropdowns only make sense for Uganda; other countries get free text. */
  protected isUganda(): boolean {
    return this.form.controls.country.value.trim().toLowerCase() === 'uganda';
  }

  /** Towns for the chosen district, or every known town when no district is chosen yet. */
  protected cityOptions(): readonly { town: string; district: string }[] {
    const district = this.form.controls.district.value;
    if (!district || district === OTHER) return UGANDA_TOWNS;
    return townsForDistrict(district).map((town) => ({ town, district }));
  }

  protected onDistrictChange(): void {
    const { district, city } = this.form.controls;
    this.toggleOtherInput(district.value, this.form.controls.districtOther, '#cf-district-other');
    // Drop a city that does not belong to the newly chosen district.
    if (city.value && city.value !== OTHER && !this.cityOptions().some((o) => o.town === city.value)) {
      city.setValue('');
    }
  }

  protected onCityChange(): void {
    const { city, district } = this.form.controls;
    this.toggleOtherInput(city.value, this.form.controls.cityOther, '#cf-city-other');
    // Picking a town first fills in its district.
    if (!district.value && city.value && city.value !== OTHER) {
      const match = districtOfTown(city.value);
      if (match) {
        district.setValue(match);
        district.markAsDirty();
      }
    }
  }

  /** Switching country converts between dropdown values and free text without losing input. */
  protected onCountryInput(): void {
    const { district, districtOther, city, cityOther } = this.form.controls;
    if (this.isUganda()) {
      this.toDropdownValue(district, districtOther, ALL_DISTRICTS);
      this.toDropdownValue(city, cityOther, UGANDA_TOWNS.map((t) => t.town));
    } else {
      if (district.value === OTHER) district.setValue(districtOther.value);
      if (city.value === OTHER) city.setValue(cityOther.value);
      for (const c of [districtOther, cityOther]) {
        c.clearValidators();
        c.reset('');
      }
    }
  }

  private toDropdownValue(
    control: typeof this.form.controls.district,
    otherControl: typeof this.form.controls.districtOther,
    known: readonly string[],
  ): void {
    const value = control.value.trim();
    if (!value || value === OTHER || known.includes(value)) return;
    control.setValue(OTHER);
    otherControl.setValue(value);
    otherControl.setValidators([Validators.required, Validators.maxLength(LOCATION_MAX)]);
    otherControl.updateValueAndValidity();
  }

  /** Free-text input is required (and kept) only while "Other…" is selected. */
  private toggleOtherInput(value: string, otherControl: typeof this.form.controls.districtOther, selector: string): void {
    if (value === OTHER) {
      otherControl.setValidators([Validators.required, Validators.maxLength(LOCATION_MAX)]);
      queueMicrotask(() => this.host.nativeElement.querySelector<HTMLElement>(selector)?.focus());
    } else {
      otherControl.clearValidators();
      otherControl.reset('');
    }
    otherControl.updateValueAndValidity();
  }

  protected submit(): void {
    const industrySectors = this.collectSectors();
    this.form.controls.industrySectors.updateValueAndValidity();
    this.form.controls.industrySectorOther.updateValueAndValidity();
    if (industrySectors.length > MAX_SECTORS) {
      const control = this.form.controls.industrySectors;
      control.setErrors({ tooManySectors: true });
      control.markAsTouched();
    } else if (industrySectors.some((s) => s.length > MAX_SECTOR_LENGTH)) {
      const control = this.form.controls.industrySectorOther;
      control.setErrors({ sectorTooLong: true });
      control.markAsTouched();
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Please fix the highlighted fields.');
      this.focusFirstInvalid();
      return;
    }
    this.error.set(null);
    this.submitting.set(true);

    const {
      industrySectors: _picked,
      industrySectorOther: _typed,
      districtOther,
      cityOther,
      ...raw
    } = this.form.getRawValue();
    if (raw.district === OTHER) raw.district = districtOther;
    if (raw.city === OTHER) raw.city = cityOther;

    this.api
      .createClient({ ...toRequest(raw), industrySectors })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (client) => {
          this.saved = true;
          const state: ClientCreatedState = {
            createdClientId: client.id,
            createdClientName: client.clientName,
            createdClientCode: client.clientCode,
          };
          this.router.navigate(['/clients'], { state });
        },
        error: (err) => this.applyServerError(err),
      });
  }

  protected invalid(name: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.touched || control.dirty);
  }

  protected errorFor(name: keyof typeof this.form.controls): string {
    const errors = this.form.controls[name].errors ?? {};
    if (errors['server']) return errors['server'];
    if (errors['required']) return 'This field is required.';
    if (errors['tooManySectors']) return `Select at most ${MAX_SECTORS} sectors.`;
    if (errors['sectorTooLong']) return `Each sector must be at most ${MAX_SECTOR_LENGTH} characters.`;
    if (errors['email']) return 'Enter a valid email address.';
    if (errors['maxlength']) return `Must be at most ${errors['maxlength'].requiredLength} characters.`;
    if (errors['pattern']) {
      return name === 'clientCode'
        ? 'Use letters, numbers, dashes or underscores only.'
        : 'Enter a valid phone number, e.g. 700 123456.';
    }
    return 'Invalid value.';
  }

  private applyServerError(err: unknown): void {
    const body = err instanceof HttpErrorResponse ? (err.error as ProblemDetail | null) : null;

    // 400: field errors from @Valid
    if (body?.errors && typeof body.errors === 'object') {
      for (const [field, message] of Object.entries(body.errors)) {
        // Sector errors arrive as "industrySectors" or "industrySectors[2]" (element constraint).
        const target = field.startsWith('industrySectors') ? 'industrySectors' : field;
        const control = this.form.get(target);
        control?.setErrors({ server: message });
        control?.markAsTouched();
      }
      this.error.set('Please fix the highlighted fields.');
      this.focusFirstInvalid();
      return;
    }
    // 409: duplicate client code
    if (err instanceof HttpErrorResponse && err.status === 409) {
      const control = this.form.controls.clientCode;
      control.setErrors({ server: body?.detail ?? 'This client code is already in use.' });
      control.markAsTouched();
      this.error.set(body?.detail ?? 'A client with this code already exists.');
      this.focusFirstInvalid();
      return;
    }
    this.error.set(toErrorMessage(err));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private focusFirstInvalid(): void {
    queueMicrotask(() => {
      const el = this.host.nativeElement.querySelector<HTMLElement>('.ng-invalid:not(form)');
      el?.focus();
      el?.scrollIntoView({ block: 'center', behavior: 'smooth' });
    });
  }
}

/** "Kampala Auto Spa" -> "KAS-4821" (editable suggestion; uniqueness enforced by the API). */
function suggestCode(name: string): string {
  const words = name.trim().toUpperCase().replace(/[^A-Z0-9 ]/g, '').split(/\s+/).filter(Boolean);
  if (!words.length) return '';
  const prefix = (words.length > 1 ? words.map((w) => w[0]).join('') : words[0]).slice(0, 4);
  const suffix = Math.floor(1000 + Math.random() * 9000);
  return `${prefix}-${suffix}`;
}

/** Trim strings and drop empty optional values so the API stores nulls, not "". */
function toRequest(value: Record<string, string>): ClientRequest {
  const request: Record<string, string> = {};
  for (const [key, raw] of Object.entries(value)) {
    const v = typeof raw === 'string' ? raw.trim() : raw;
    if (v) request[key] = v;
  }
  return request as unknown as ClientRequest;
}
