import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { toErrorMessage } from '../../../core/http/api-error';
import { OtpChannel } from '../../../core/models/api.models';
import { Icon, IconName } from '../../../shared/icon/icon';
import { PhoneInput } from '../../../shared/phone-input/phone-input';

interface ChannelOption {
  value: OtpChannel;
  label: string;
  icon: IconName;
}

const PHONE_PATTERN = /^\+?[0-9 ()-]{7,25}$/;

/** Step 1 of sign-in: email or phone (with country code) + OTP (or password). Account category/role is resolved by the backend. */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, Icon, PhoneInput],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.html',
  styleUrl: '../auth-form.css',
})
export class Login implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Bound from the query string (withComponentInputBinding). */
  readonly returnUrl = input<string>();
  readonly registered = input<string>();
  readonly email = input<string>();
  readonly phone = input<string>();

  protected readonly channels: ChannelOption[] = [
    { value: 'EMAIL', label: 'Email', icon: 'mail' },
    { value: 'SMS', label: 'SMS', icon: 'sms' },
    { value: 'WHATSAPP', label: 'WhatsApp', icon: 'whatsapp' },
  ];

  /** Whether the user is signing in with their email address or phone number. */
  protected readonly loginBy = signal<'email' | 'phone'>('email');

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    phone: ['', [Validators.pattern(PHONE_PATTERN)]],
    password: [''],
    otpChannel: ['EMAIL' as OtpChannel],
  });

  protected readonly submitting = signal(false);
  protected readonly showPassword = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly info = signal<string | null>(null);
  /** 'otp' = passwordless (OTP only, default), 'password' = password + OTP. */
  protected readonly mode = signal<'password' | 'otp'>('otp');

  ngOnInit(): void {
    const emailParam = this.email()?.trim();
    const phoneParam = this.phone()?.trim();
    if (phoneParam) {
      this.setLoginBy('phone');
      this.form.controls.phone.setValue(phoneParam);
    } else if (emailParam) {
      if (emailParam.includes('@')) {
        this.setLoginBy('email');
        this.form.controls.email.setValue(emailParam);
      } else {
        this.setLoginBy('phone');
        this.form.controls.phone.setValue(emailParam);
      }
    }
    if (this.registered()) {
      this.info.set(
        `Your business (${this.registered()}) and Client Admin account have been registered! Click "Send me a code" to sign in with OTP.`,
      );
    }
  }

  protected setLoginBy(method: 'email' | 'phone'): void {
    if (this.loginBy() === method) return;
    this.loginBy.set(method);
    this.error.set(null);

    const emailCtrl = this.form.controls.email;
    const phoneCtrl = this.form.controls.phone;
    const channelCtrl = this.form.controls.otpChannel;

    if (method === 'email') {
      emailCtrl.setValidators([Validators.required, Validators.email, Validators.maxLength(150)]);
      phoneCtrl.setValidators([Validators.pattern(PHONE_PATTERN)]);
      if (channelCtrl.value === 'SMS') {
        channelCtrl.setValue('EMAIL');
      }
    } else {
      emailCtrl.setValidators([Validators.email, Validators.maxLength(150)]);
      phoneCtrl.setValidators([Validators.required, Validators.pattern(PHONE_PATTERN)]);
      if (channelCtrl.value === 'EMAIL') {
        channelCtrl.setValue('SMS');
      }
    }
    emailCtrl.updateValueAndValidity();
    phoneCtrl.updateValueAndValidity();
  }

  protected setMode(mode: 'password' | 'otp'): void {
    this.mode.set(mode);
    this.error.set(null);
    const password = this.form.controls.password;
    password.setValidators(mode === 'password' ? [Validators.required] : []);
    if (mode === 'otp') password.reset('');
    password.updateValueAndValidity();
  }

  protected submit(): void {
    const activeControl = this.loginBy() === 'email' ? this.form.controls.email : this.form.controls.phone;
    activeControl.markAsTouched();
    if (this.mode() === 'password') {
      this.form.controls.password.markAsTouched();
    }
    if (activeControl.invalid || (this.mode() === 'password' && this.form.controls.password.invalid)) {
      return;
    }

    this.error.set(null);
    this.submitting.set(true);
    const { email, phone, password, otpChannel } = this.form.getRawValue();
    const username = (this.loginBy() === 'email' ? email : phone).trim();

    const request$ =
      this.mode() === 'otp'
        ? this.auth.loginWithOtp({ username, otpChannel })
        : this.auth.login({ username, password, otpChannel });

    request$
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () =>
          this.router.navigate(['/auth/verify-otp'], {
            queryParams: this.returnUrl() ? { returnUrl: this.returnUrl() } : {},
          }),
        error: (err) => this.error.set(toErrorMessage(err)),
      });
  }

  protected invalid(name: 'email' | 'phone' | 'password'): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.touched || control.dirty);
  }
}
