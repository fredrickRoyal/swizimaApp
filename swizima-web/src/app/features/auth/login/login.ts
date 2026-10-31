import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { toErrorMessage } from '../../../core/http/api-error';
import { OtpChannel } from '../../../core/models/api.models';
import { Icon, IconName } from '../../../shared/icon/icon';

interface ChannelOption {
  value: OtpChannel;
  label: string;
  icon: IconName;
}

/** Step 1 of sign-in: username (email or phone) + password, then an OTP is sent. */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.html',
  styleUrl: '../auth-form.css',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Bound from the query string (withComponentInputBinding). */
  readonly returnUrl = input<string>();

  protected readonly channels: ChannelOption[] = [
    { value: 'EMAIL', label: 'Email', icon: 'mail' },
    { value: 'SMS', label: 'SMS', icon: 'sms' },
    { value: 'WHATSAPP', label: 'WhatsApp', icon: 'whatsapp' },
  ];

  protected readonly form = inject(NonNullableFormBuilder).group({
    username: ['', [Validators.required, Validators.maxLength(150)]],
    password: ['', [Validators.required]],
    otpChannel: ['EMAIL' as OtpChannel],
  });

  protected readonly submitting = signal(false);
  protected readonly showPassword = signal(false);
  protected readonly error = signal<string | null>(null);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    this.submitting.set(true);
    const { username, password, otpChannel } = this.form.getRawValue();

    this.auth
      .login({ username: username.trim(), password, otpChannel })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () =>
          this.router.navigate(['/auth/verify-otp'], {
            queryParams: this.returnUrl() ? { returnUrl: this.returnUrl() } : {},
          }),
        error: (err) => this.error.set(toErrorMessage(err)),
      });
  }

  protected invalid(name: 'username' | 'password'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }
}
