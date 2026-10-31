import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { toErrorMessage } from '../../../core/http/api-error';
import { OtpChannel } from '../../../core/models/api.models';
import { Icon, IconName } from '../../../shared/icon/icon';

const CHANNEL_META: Record<OtpChannel, { label: string; icon: IconName }> = {
  EMAIL: { label: 'Email', icon: 'mail' },
  SMS: { label: 'SMS', icon: 'sms' },
  WHATSAPP: { label: 'WhatsApp', icon: 'whatsapp' },
};

/** WebOTP API (Chromium on Android) – not in lib.dom yet. */
interface OtpCredentialLike extends Credential {
  code: string;
}

/** Step 2 of sign-in: enter the 6-digit code that was sent. */
@Component({
  selector: 'app-verify-otp',
  imports: [ReactiveFormsModule, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './verify-otp.html',
  styleUrl: '../auth-form.css',
})
export class VerifyOtp implements AfterViewInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly returnUrl = input<string>();

  private readonly formEl = viewChild.required<ElementRef<HTMLFormElement>>('otpForm');
  private readonly codeEl = viewChild.required<ElementRef<HTMLInputElement>>('codeInput');

  protected readonly challenge = this.auth.challenge;
  protected readonly form = inject(NonNullableFormBuilder).group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  protected readonly submitting = signal(false);
  protected readonly resending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly info = signal<string | null>(null);

  // ----- Segmented boxes (visual only; the real input holds the value) -----
  protected readonly slots = [0, 1, 2, 3, 4, 5];
  protected readonly focused = signal(false);
  private readonly code = toSignal(this.form.controls.code.valueChanges, { initialValue: '' });
  protected readonly digits = computed(() => this.code().split(''));
  protected readonly activeIndex = computed(() => Math.min(this.code().length, 5));

  /** Ticks every second to drive both countdowns. */
  private readonly now = signal(Date.now());

  protected readonly secondsLeft = computed(() =>
    Math.max(0, Math.ceil(((this.challenge()?.expiresAtMs ?? 0) - this.now()) / 1000)),
  );
  protected readonly resendIn = computed(() =>
    Math.max(0, Math.ceil(((this.challenge()?.resendAtMs ?? 0) - this.now()) / 1000)),
  );
  protected readonly expired = computed(() => this.secondsLeft() === 0);
  protected readonly channelMeta = computed(() => CHANNEL_META[this.challenge()?.channel ?? 'EMAIL']);
  protected readonly otherChannels = computed(() =>
    (Object.keys(CHANNEL_META) as OtpChannel[])
      .filter((c) => c !== this.challenge()?.channel)
      .map((value) => ({ value, ...CHANNEL_META[value] })),
  );

  constructor() {
    const timer = setInterval(() => this.now.set(Date.now()), 1000);
    this.destroyRef.onDestroy(() => clearInterval(timer));
  }

  ngAfterViewInit(): void {
    this.codeEl().nativeElement.focus();
    this.listenForWebOtp();
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.expired()) {
      this.error.set('This code has expired. Request a new one below.');
      return;
    }
    this.error.set(null);
    this.info.set(null);
    this.submitting.set(true);

    this.auth
      .verifyOtp(this.form.controls.code.value)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (session) => {
          const target = safeReturnUrl(this.returnUrl());
          this.router.navigateByUrl(target, {
            state: session.mustChangePassword ? { mustChangePassword: true } : undefined,
          });
        },
        error: (err) => {
          this.error.set(toErrorMessage(err));
          this.form.controls.code.reset('');
          this.codeEl().nativeElement.focus();
        },
      });
  }

  /** Strip anything that is not a digit as the user types or pastes. */
  protected sanitize(): void {
    const control = this.form.controls.code;
    const digits = control.value.replace(/\D/g, '').slice(0, 6);
    if (digits !== control.value) control.setValue(digits);
    if (digits.length > 0) this.error.set(null);
    if (digits.length === 6 && !this.submitting()) {
      this.formEl().nativeElement.requestSubmit();
    }
  }

  /** Boxes fill left-to-right, so keep the caret at the end (Backspace deletes the last digit). */
  protected keepCaretAtEnd(): void {
    const el = this.codeEl().nativeElement;
    const end = el.value.length;
    if (el.selectionStart !== end || el.selectionEnd !== end) el.setSelectionRange(end, end);
  }

  protected resend(channel?: OtpChannel): void {
    this.error.set(null);
    this.info.set(null);
    this.resending.set(true);
    this.auth
      .resendOtp(channel)
      .pipe(finalize(() => this.resending.set(false)))
      .subscribe({
        next: (next) => {
          this.form.controls.code.reset('');
          this.info.set(`A new code was sent to ${next.maskedDestination}.`);
          this.now.set(Date.now());
          this.codeEl().nativeElement.focus();
        },
        error: (err) => this.error.set(toErrorMessage(err)),
      });
  }

  protected backToLogin(): void {
    this.auth.clearChallenge();
    this.router.navigate(['/auth/login'], {
      queryParams: this.returnUrl() ? { returnUrl: this.returnUrl() } : {},
    });
  }

  protected formatTime(totalSeconds: number): string {
    const m = Math.floor(totalSeconds / 60);
    const s = totalSeconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  /** Auto-fill SMS codes where the WebOTP API is available (progressive enhancement). */
  private listenForWebOtp(): void {
    if (!('OTPCredential' in window)) return;
    const abort = new AbortController();
    this.destroyRef.onDestroy(() => abort.abort());
    navigator.credentials
      .get({ otp: { transport: ['sms'] }, signal: abort.signal } as CredentialRequestOptions)
      .then((credential) => {
        const code = (credential as OtpCredentialLike | null)?.code;
        if (code) {
          this.form.controls.code.setValue(code);
          this.sanitize();
        }
      })
      .catch(() => {
        /* user dismissed or aborted – ignore */
      });
  }
}

/** Only allow in-app relative paths (prevents open redirects via ?returnUrl=). */
function safeReturnUrl(url: string | undefined): string {
  return url && url.startsWith('/') && !url.startsWith('//') && !url.startsWith('/auth') ? url : '/dashboard';
}
