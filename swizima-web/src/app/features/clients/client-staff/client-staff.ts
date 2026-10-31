import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SwizimaApi } from '../../../core/api/swizima-api';
import { debounced, remoteData } from '../../../core/data/remote-data';
import { toErrorMessage } from '../../../core/http/api-error';
import { ClientStaff, ClientStaffRequest, Gender } from '../../../core/models/api.models';
import { EmptyState } from '../../../shared/empty-state/empty-state';
import { Icon } from '../../../shared/icon/icon';
import { Pagination } from '../../../shared/pagination/pagination';
import { PhoneInput } from '../../../shared/phone-input/phone-input';
import { humanize } from '../../../shared/status-badge/status-badge';
import { ClientLayout } from '../client-layout/client-layout';

const PAGE_SIZE = 10;
const PHONE = /^\+?[0-9 ()-]{7,30}$/;
const NIN = /^[A-Za-z0-9]{8,20}$/;
const POSITIONS = ['Owner', 'Manager', 'Operations'] as const;

/** "mary-jane o'neil" -> "Mary-Jane O'Neil" (rest of each word left as typed). */
export function capitalizeName(value: string): string {
  return value.replace(/(^|[\s'-])(\p{L})/gu, (_, sep: string, ch: string) => sep + ch.toLocaleUpperCase());
}

/** /clients/:id/staff — people working for a client, backed by /api/v1/client-staff. */
@Component({
  selector: 'app-client-staff',
  imports: [ReactiveFormsModule, EmptyState, Icon, Pagination, PhoneInput],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-staff.html',
  styleUrl: './client-staff.css',
})
export class ClientStaffPage {
  private readonly api = inject(SwizimaApi);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly layout = inject(ClientLayout);

  protected readonly genders: Gender[] = ['MALE', 'FEMALE', 'OTHER'];
  protected readonly humanize = humanize;
  protected readonly pageSize = PAGE_SIZE;
  /** Default dial code for phone fields. */
  protected readonly clientCountry = computed(() => this.layout.client.state().data?.country ?? 'Uganda');

  // ---------- List ----------
  protected readonly search = signal('');
  protected readonly gender = signal<Gender | ''>('');
  protected readonly page = signal(0);
  private readonly searchTerm = debounced(this.search, 350);

  protected readonly staff = remoteData(
    computed(() => ({
      clientId: this.layout.id(),
      query: {
        search: this.searchTerm().trim(),
        gender: this.gender(),
        page: this.page(),
        size: PAGE_SIZE,
        sort: 'surname,asc',
      },
    })),
    ({ clientId, query }) => this.api.staff(clientId, query),
  );

  protected readonly hasFilters = computed(() => !!this.search().trim() || !!this.gender());

  protected onSearch(value: string): void { this.search.set(value); this.page.set(0); }
  protected onGender(value: string): void { this.gender.set(value as Gender | ''); this.page.set(0); }
  protected clearFilters(): void { this.search.set(''); this.gender.set(''); this.page.set(0); }

  protected readonly busyId = signal<string | null>(null);
  protected readonly notice = signal<{ tone: 'success' | 'error'; text: string } | null>(null);

  protected remove(member: ClientStaff): void {
    if (!confirm(`Remove ${member.fullName} from staff?`)) return;
    this.busyId.set(member.id);
    this.api.deleteStaff(this.layout.id(), member.id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.notice.set({ tone: 'success', text: `${member.fullName} was removed.` });
        this.staff.reload();
      },
      error: (err) => {
        this.busyId.set(null);
        this.notice.set({ tone: 'error', text: toErrorMessage(err) });
      },
    });
  }

  protected initials(m: ClientStaff): string {
    return `${m.givenName?.[0] ?? ''}${m.surname?.[0] ?? ''}`.toUpperCase();
  }

  // ---------- Create / edit dialog ----------
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  protected readonly editing = signal<ClientStaff | null>(null);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);

  /** Supported positions, plus the member's current one if it predates the list. */
  protected readonly positionOptions = computed(() => {
    const own = this.editing()?.position;
    return own && !(POSITIONS as readonly string[]).includes(own) ? [...POSITIONS, own] : [...POSITIONS];
  });

  protected readonly form = this.fb.group({
    surname: ['', [Validators.required, Validators.maxLength(100)]],
    givenName: ['', [Validators.required, Validators.maxLength(150)]],
    gender: this.fb.control<Gender | ''>(''),
    phoneNumber: ['', Validators.pattern(PHONE)],
    alternativePhone: ['', Validators.pattern(PHONE)],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    position: ['', Validators.maxLength(100)],
    nin: ['', Validators.pattern(NIN)],
    summary: [''],
  });

  protected openCreate(): void {
    this.editing.set(null);
    this.form.reset();
    this.openDialog();
  }

  protected openEdit(m: ClientStaff): void {
    this.editing.set(m);
    this.form.reset({
      surname: m.surname,
      givenName: m.givenName,
      gender: m.gender ?? '',
      phoneNumber: m.phoneNumber ?? '',
      alternativePhone: m.alternativePhone ?? '',
      email: m.email ?? '',
      position: m.position ?? '',
      nin: m.nin ?? '',
      summary: m.summary ?? '',
    });
    this.openDialog();
  }

  private openDialog(): void {
    this.formError.set(null);
    this.dialog().nativeElement.showModal();
  }

  protected closeDialog(): void {
    if (this.saving()) return;
    this.dialog().nativeElement.close();
  }

  protected capitalize(name: 'surname' | 'givenName', event: Event): void {
    const el = event.target as HTMLInputElement;
    const next = capitalizeName(el.value);
    if (next === el.value) return;
    const caret = el.selectionStart;
    el.value = next;
    this.form.controls[name].setValue(next, { emitEvent: false });
    if (caret !== null) el.setSelectionRange(caret, caret);
  }

  protected invalid(name: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[name];
    return c.invalid && (c.touched || c.dirty);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const opt = (s: string) => s.trim() || undefined;
    const request: ClientStaffRequest = {
      surname: capitalizeName(v.surname.trim()),
      givenName: capitalizeName(v.givenName.trim()),
      gender: v.gender || undefined,
      phoneNumber: opt(v.phoneNumber),
      alternativePhone: opt(v.alternativePhone),
      email: opt(v.email),
      position: opt(v.position),
      nin: opt(v.nin)?.toUpperCase(),
      summary: opt(v.summary),
    };
    const current = this.editing();
    const clientId = this.layout.id();
    const call = current
      ? this.api.updateStaff(clientId, current.id, request)
      : this.api.createStaff(clientId, request);

    this.saving.set(true);
    this.formError.set(null);
    call.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.dialog().nativeElement.close();
        this.notice.set({ tone: 'success', text: `${saved.fullName} was ${current ? 'updated' : 'added'}.` });
        if (!current) this.page.set(0);
        this.staff.reload();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(toErrorMessage(err));
      },
    });
  }
}
