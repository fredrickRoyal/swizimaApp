import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  AuthService,
  COUNTRY_DIAL_CODES,
  LoginMode,
  OtpChannel,
} from '../../core/services/auth.service';
import { StaffTrackStore } from '../../core/services/stafftrack.store';
import { ThemeService } from '../../core/services/theme.service';
import { Icon } from '../../shared/icon/icon';

@Component({
  selector: 'app-login',
  imports: [FormsModule, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .auth-shell {
      min-height: 100svh;
      display: grid;
      grid-template-columns: 1fr;
    }
    @media (min-width: 980px) {
      .auth-shell {
        grid-template-columns: 1.05fr 1fr;
      }
    }
    .hero-pane {
      display: none;
      position: relative;
      padding: 2.75rem 3.25rem;
      color: #f8fafc;
      background:
        radial-gradient(80% 70% at 15% 15%, rgb(56 189 248 / 0.25), transparent 60%),
        radial-gradient(70% 70% at 85% 85%, rgb(247 136 18 / 0.22), transparent 60%),
        linear-gradient(155deg, #071126 0%, #0b2447 55%, #004282 100%);
      flex-direction: column;
      justify-content: space-between;
    }
    @media (min-width: 980px) {
      .hero-pane {
        display: flex;
      }
    }
    .hero-brand {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .hero-brand img {
      width: 42px;
      height: 42px;
      border-radius: 10px;
      box-shadow: 0 8px 22px rgb(0 0 0 / 0.35);
    }
    .hero-brand-text {
      display: grid;
      line-height: 1.2;
    }
    .hero-brand-text strong {
      font-size: 1.15rem;
      letter-spacing: -0.01em;
    }
    .hero-brand-text small {
      font-size: 0.78rem;
      color: rgb(226 232 240 / 0.85);
      white-space: normal;
    }
    .hero-pitch {
      display: grid;
      gap: 1.15rem;
      max-width: 34rem;
    }
    .hero-eyebrow {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      width: fit-content;
      padding: 0.35rem 0.8rem;
      border-radius: 999px;
      font-size: 0.78rem;
      font-weight: 600;
      background: rgb(255 255 255 / 0.12);
      border: 1px solid rgb(255 255 255 / 0.2);
    }
    .hero-pitch h1 {
      font-size: clamp(1.85rem, 2.7vw, 2.45rem);
      font-weight: 800;
      line-height: 1.15;
      letter-spacing: -0.025em;
    }
    .hero-pitch p {
      font-size: 0.96rem;
      color: rgb(226 232 240 / 0.85);
    }
    .hero-points {
      display: grid;
      gap: 0.65rem;
      margin-top: 0.35rem;
      font-size: 0.9rem;
      color: rgb(241 245 249 / 0.92);
    }
    .hero-points li {
      display: flex;
      align-items: center;
      gap: 0.6rem;
    }
    .hero-copy {
      font-size: 0.78rem;
      color: rgb(226 232 240 / 0.65);
    }

    /* Form Pane */
    .form-pane {
      display: grid;
      grid-template-rows: auto 1fr auto;
      padding: clamp(1.25rem, 3.5vw, 2.5rem);
      background: var(--bg);
    }
    .pane-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      max-width: 440px;
      width: 100%;
      margin-inline: auto;
    }
    .mobile-brand {
      display: flex;
      align-items: center;
      gap: 0.6rem;
    }
    .mobile-brand img {
      width: 34px;
      height: 34px;
      border-radius: 8px;
    }
    @media (min-width: 980px) {
      .mobile-brand {
        visibility: hidden;
      }
    }
    .card-wrap {
      align-self: center;
      width: 100%;
      max-width: 440px;
      margin-inline: auto;
      background: var(--bg-elevated);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      padding: clamp(1.4rem, 3.5vw, 2.1rem);
      box-shadow: var(--shadow);
      display: grid;
      gap: 1.25rem;
    }
    .card-head {
      display: grid;
      gap: 0.35rem;
    }
    .card-head h2 {
      font-size: 1.45rem;
      font-weight: 800;
      letter-spacing: -0.02em;
    }
    .card-head p {
      font-size: 0.88rem;
      color: var(--text-muted);
    }

    /* Segmented Login Mode Switch */
    .mode-switch {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.35rem;
      padding: 0.3rem;
      border-radius: var(--radius);
      background: var(--bg-subtle);
      border: 1px solid var(--border);
    }
    .mode-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.45rem;
      min-height: 40px;
      border: 0;
      border-radius: var(--radius-sm);
      background: transparent;
      color: var(--text-muted);
      font-size: 0.85rem;
      font-weight: 600;
      transition: background-color 150ms, color 150ms, box-shadow 150ms;
    }
    .mode-btn.active {
      background: var(--bg-elevated);
      color: var(--brand-500);
      box-shadow: var(--shadow-sm);
    }

    /* Phone with Country Code */
    .phone-row {
      display: grid;
      grid-template-columns: 138px 1fr;
      gap: 0.5rem;
    }
    .auth-input {
      min-height: 48px;
      font-size: max(1rem, 1em);
    }

    /* Channel Radio Group */
    .channel-fieldset {
      border: 0;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 0.45rem;
    }
    .channel-fieldset legend {
      font-size: 0.82rem;
      font-weight: 600;
      margin-bottom: 0.2rem;
    }
    .channel-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.5rem;
    }
    .channel-option {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.35rem;
      min-height: 44px;
      padding: 0.45rem 0.6rem;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-strong);
      background: var(--bg-elevated);
      font-size: 0.83rem;
      font-weight: 600;
      cursor: pointer;
    }
    .channel-option.selected {
      border-color: var(--brand-500);
      background: var(--info-bg);
      color: var(--brand-500);
    }
    .channel-option.disabled {
      opacity: 0.52;
      cursor: not-allowed;
      background: var(--bg-subtle);
      border-style: dashed;
      color: var(--text-muted);
    }
    .channel-option.disabled input {
      cursor: not-allowed;
    }
    .channel-disabled-tag {
      font-size: 0.68rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.03em;
      padding: 0.05rem 0.32rem;
      border-radius: 999px;
      background: var(--border);
      color: var(--text-muted);
    }
    .channel-option input {
      margin: 0;
    }

    /* 6-Box Visual OTP Input backed by a single semantic <input autocomplete="one-time-code"> */
    .otp-boxes-wrapper {
      position: relative;
      display: block;
      width: 100%;
    }
    .otp-boxes-grid {
      display: grid;
      grid-template-columns: repeat(6, 1fr);
      gap: 0.55rem;
      pointer-events: none;
    }
    .otp-digit-box {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 54px;
      border-radius: var(--radius);
      border: 1.5px solid var(--border-strong);
      background: var(--bg-elevated);
      font-family: var(--font-mono);
      font-size: 1.35rem;
      font-weight: 700;
      color: var(--text);
      transition: border-color 140ms ease, box-shadow 140ms ease, background-color 140ms ease;
    }
    .otp-digit-box.placeholder {
      color: color-mix(in srgb, var(--text-muted) 45%, transparent);
    }
    .otp-digit-box.filled {
      border-color: color-mix(in srgb, var(--brand-500) 55%, var(--border-strong));
    }
    .otp-boxes-wrapper:focus-within .otp-digit-box.active {
      border-color: var(--brand-500);
      box-shadow: var(--ring);
      background: var(--bg-elevated);
    }
    .otp-boxes-wrapper:focus-within .otp-digit-box.active.empty::after {
      content: '';
      width: 2px;
      height: 1.35rem;
      border-radius: 1px;
      background: var(--brand-500);
      animation: otp-caret-blink 1s step-end infinite;
    }
    @keyframes otp-caret-blink {
      0%, 100% { opacity: 1; }
      50% { opacity: 0; }
    }
    .otp-overlay-input {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      min-height: 54px;
      opacity: 0;
      font-size: max(1rem, 16px);
      caret-color: transparent;
      cursor: text;
      z-index: 2;
    }

    .demo-code-banner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.75rem 0.95rem;
      border-radius: var(--radius);
      background: var(--info-bg);
      border: 1px solid color-mix(in srgb, var(--info) 30%, transparent);
      font-size: 0.83rem;
    }
    .demo-code-banner code {
      font-family: var(--font-mono);
      font-size: 1rem;
      font-weight: 800;
      letter-spacing: 0.12em;
      color: var(--brand-500);
    }
    .error-banner {
      padding: 0.75rem 0.95rem;
      border-radius: var(--radius-sm);
      background: var(--danger-bg);
      color: var(--danger);
      font-size: 0.84rem;
      font-weight: 600;
    }
    .presets {
      border-top: 1px solid var(--border);
      padding-top: 0.95rem;
      display: grid;
      gap: 0.45rem;
    }
    .presets-title {
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }
    .preset-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 0.45rem;
    }
    .pane-foot {
      text-align: center;
      font-size: 0.78rem;
      color: var(--text-muted);
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.35rem;
    }
  `,
  template: `
    <div class="auth-shell">
      <!-- Left Brand Hero -->
      <section class="hero-pane">
        <div class="hero-brand">
          <img src="opm-emblem.svg" alt="OPM Emblem" width="46" height="46" />
          <div class="hero-brand-text">
            <strong>Staff Out-of-Office Tracker</strong>
          </div>
        </div>

        <div class="hero-pitch">
          <span class="hero-eyebrow">
            <app-icon name="sparkles" />
            Staff Out-of-Office Tracker
          </span>
          <h1>Staff Out-of-Office Tracker</h1>
          <p>
            A simple platform for tracking staff movements, official duties, activity reports and
            management actions.
          </p>
          <ul class="hero-points">
            <li><app-icon name="plane" /> Out-of-Office travel, meetings, workshops &amp; field trips</li>
            <li><app-icon name="clipboard" /> Official duties, progress &amp; follow-up tracking</li>
            <li><app-icon name="dashboard" /> Live management notification dashboard &amp; support</li>
          </ul>
        </div>

        <div class="hero-copy">© 2026 Staff Out-of-Office Tracker.</div>
      </section>

      <!-- Right Form Panel -->
      <section class="form-pane">
        <div class="pane-top">
          <div class="mobile-brand">
            <img src="opm-emblem.svg" alt="OPM Emblem" width="38" height="38" />
            <div class="brand-text">
              <strong style="font-size: 0.9rem; line-height: 1.25">
                Staff Out-of-Office Tracker
              </strong>
            </div>
          </div>

          <button
            type="button"
            class="btn btn-sm"
            (click)="theme.toggle()"
            [attr.aria-label]="theme.theme() === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'"
          >
            <app-icon [name]="theme.theme() === 'dark' ? 'sun' : 'moon'" />
          </button>
        </div>

        <div class="card-wrap">
          @if (!auth.challenge()) {
            <!-- STEP 1: Phone Number or Email Entry -->
            <div class="card-head">
              <h2 id="login-form-heading">Welcome to Staff Out-of-Office Tracker</h2>
            </div>

            <!-- Switch between Email and Phone Number -->
            <div class="mode-switch" role="tablist" aria-label="Sign-in identifier type">
              <button
                type="button"
                role="tab"
                class="mode-btn"
                [class.active]="loginMode() === 'email'"
                [attr.aria-selected]="loginMode() === 'email'"
                (click)="setLoginMode('email')"
              >
                <app-icon name="mail" />
                <span>Email address</span>
              </button>
              <button
                type="button"
                role="tab"
                class="mode-btn"
                [class.active]="loginMode() === 'phone'"
                [attr.aria-selected]="loginMode() === 'phone'"
                (click)="setLoginMode('phone')"
              >
                <app-icon name="phone" />
                <span>Phone number</span>
              </button>
            </div>

            @if (errorMessage()) {
              <div class="error-banner" role="alert" aria-live="polite">
                {{ errorMessage() }}
              </div>
            }

            <form
              action="/login/request-otp"
              method="POST"
              aria-labelledby="login-form-heading"
              (ngSubmit)="sendOtp()"
              style="display: grid; gap: 1.05rem"
            >
              @if (loginMode() === 'email') {
                <div class="field">
                  <label for="login-email">Email Address *</label>
                  <small id="email-hint">Enter your email.</small>
                  <input
                    id="login-email"
                    name="email"
                    type="email"
                    class="input auth-input"
                    autocomplete="username"
                    autocorrect="off"
                    autocapitalize="none"
                    spellcheck="false"
                    enterkeyhint="next"
                    required
                    [attr.aria-invalid]="errorMessage() ? 'true' : null"
                    aria-describedby="email-hint"
                    placeholder="name@organisation.go.ug"
                    [ngModel]="emailInput()"
                    (ngModelChange)="onIdentifierInput($event)"
                    (blur)="validateOnBlur()"
                  />
                </div>
              } @else {
                <div class="field">
                  <label for="login-phone">Mobile Phone Number *</label>
                  <small id="phone-hint">Select your country code and enter your mobile number.</small>
                  <div class="phone-row">
                    <select
                      id="login-country-code"
                      name="countryDial"
                      class="select auth-input"
                      aria-label="Country dial code"
                      [ngModel]="countryDial()"
                      (ngModelChange)="countryDial.set($event)"
                    >
                      @for (c of countries; track c.code) {
                        <option [value]="c.dial">{{ c.flag }} {{ c.dial }} ({{ c.code }})</option>
                      }
                    </select>
                    <input
                      id="login-phone"
                      name="phone"
                      type="tel"
                      inputmode="tel"
                      autocomplete="tel-national"
                      autocorrect="off"
                      autocapitalize="none"
                      spellcheck="false"
                      enterkeyhint="next"
                      class="input auth-input"
                      required
                      [attr.aria-invalid]="errorMessage() ? 'true' : null"
                      aria-describedby="phone-hint"
                      placeholder="700 000 000"
                      [ngModel]="phoneInput()"
                      (ngModelChange)="onIdentifierInput($event)"
                      (blur)="validateOnBlur()"
                    />
                  </div>
                </div>
              }

              <!-- Delivery Channel (dynamically reflects active channels from Configurations) -->
              <fieldset class="channel-fieldset">
                <legend>Send my one-time code via</legend>
                <div class="channel-grid">
                  @for (ch of store.channelConfigs(); track ch.channelCode) {
                    @let isAllowed = ch.active && ch.otpEnabled;
                    <label
                      class="channel-option"
                      [class.selected]="isAllowed && channel() === ch.channelCode"
                      [class.disabled]="!isAllowed"
                      [attr.aria-disabled]="!isAllowed ? 'true' : null"
                      [attr.title]="!isAllowed ? ch.displayName + ' is currently disabled in Configurations' : null"
                    >
                      <input
                        type="radio"
                        name="otpChannel"
                        [value]="ch.channelCode"
                        [disabled]="!isAllowed"
                        [ngModel]="channel()"
                        (ngModelChange)="isAllowed && channel.set($event)"
                      />
                      <span>{{ ch.displayName }}</span>
                      @if (!isAllowed) {
                        <span class="channel-disabled-tag">Off</span>
                      }
                    </label>
                  }
                </div>
              </fieldset>

              <button
                type="submit"
                class="btn btn-primary"
                style="min-height: 48px; width: 100%"
                [disabled]="auth.loading()"
              >
                <span>{{ auth.loading() ? 'Sending Verification Code...' : 'Send Verification Code' }}</span>
                <app-icon name="arrowRight" />
              </button>
            </form>
          } @else {
            <!-- STEP 2: 6-Digit OTP Verification -->
            <div class="card-head">
              <h2 id="otp-form-heading">Verify one-time code</h2>
              <p>
                We sent a 6-digit verification code via
                <strong>{{ auth.challenge()?.channel }}</strong> to
                <strong>{{ auth.challenge()?.maskedIdentifier }}</strong>.
              </p>
            </div>

            @if (errorMessage()) {
              <div class="error-banner" role="alert" aria-live="polite">
                {{ errorMessage() }}
              </div>
            }

            <form
              #otpFormEl
              id="otp-form"
              action="/login/verify-otp"
              method="POST"
              aria-labelledby="otp-form-heading"
              (ngSubmit)="submitOtp()"
              style="display: grid; gap: 1.1rem"
            >
              <div class="field">
                <label for="otp-input">Verification code *</label>
                <!-- Instructional hint placed visually above the input per sms-otp-form guide -->
                <small id="otp-format-hint">
                  Enter the 6-digit numeric code sent to
                  <strong>{{ auth.challenge()?.maskedIdentifier }}</strong>.
                </small>
                <div
                  class="otp-boxes-wrapper"
                  [style.--value-length]="otpCode().length"
                >
                  <div class="otp-boxes-grid" aria-hidden="true">
                    @for (slot of otpSlots; track slot) {
                      <div
                        class="otp-digit-box"
                        [class.filled]="otpDigitAt(slot) !== ''"
                        [class.empty]="otpDigitAt(slot) === ''"
                        [class.placeholder]="otpDigitAt(slot) === '' && activeOtpSlot() !== slot"
                        [class.active]="activeOtpSlot() === slot"
                      >
                        @if (otpDigitAt(slot); as digit) {
                          {{ digit }}
                        } @else if (activeOtpSlot() !== slot) {
                          0
                        }
                      </div>
                    }
                  </div>
                  <input
                    #otpInputEl
                    type="text"
                    id="otp-input"
                    name="otp"
                    class="otp-overlay-input"
                    inputmode="numeric"
                    autocomplete="one-time-code"
                    autocorrect="off"
                    autocapitalize="none"
                    spellcheck="false"
                    enterkeyhint="done"
                    pattern="\\d{6}"
                    maxlength="6"
                    aria-describedby="otp-format-hint"
                    [attr.aria-invalid]="errorMessage() ? 'true' : null"
                    required
                    [ngModel]="otpCode()"
                    (ngModelChange)="onOtpInput($event)"
                    (focus)="otpFocused.set(true)"
                    (blur)="otpFocused.set(false)"
                  />
                </div>
              </div>

              <button
                type="submit"
                class="btn btn-primary"
                style="min-height: 48px; width: 100%"
                [disabled]="auth.loading()"
              >
                <app-icon name="check" />
                <span>{{ auth.loading() ? 'Verifying...' : 'Verify & Continue' }}</span>
              </button>

              <div style="display: flex; justify-content: space-between; gap: 0.5rem; flex-wrap: wrap">
                <button
                  type="button"
                  class="btn btn-sm btn-ghost"
                  [disabled]="auth.loading()"
                  (click)="changeIdentifier()"
                >
                  Use different email or phone
                </button>
                <button
                  type="button"
                  class="btn btn-sm"
                  [disabled]="auth.loading()"
                  (click)="resendOtp()"
                >
                  Resend code
                </button>
              </div>
            </form>
          }
        </div>

        <div class="pane-foot">
          <app-icon name="shield" />
          <span>Protected with two-step OTP verification</span>
        </div>
      </section>
    </div>
  `,
})
export class Login implements OnDestroy {
  protected readonly auth = inject(AuthService);
  protected readonly store = inject(StaffTrackStore);
  protected readonly theme = inject(ThemeService);
  private readonly router = inject(Router);

  protected readonly countries = COUNTRY_DIAL_CODES;
  protected readonly loginMode = signal<LoginMode>('email');
  protected readonly emailInput = signal('');
  protected readonly countryDial = signal('+256');
  protected readonly phoneInput = signal('');
  protected readonly channel = signal<OtpChannel>('EMAIL');
  protected readonly otpCode = signal('');
  protected readonly otpFocused = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly otpSlots = [0, 1, 2, 3, 4, 5] as const;

  constructor() {
    this.store.loadChannelConfigs();
    effect(() => {
      const configs = this.store.channelConfigs();
      const current = this.channel();
      const currentCfg = configs.find((c) => c.channelCode === current);
      if (currentCfg && (!currentCfg.active || !currentCfg.otpEnabled)) {
        const fallback = configs.find((c) => c.active && c.otpEnabled);
        if (fallback) {
          this.channel.set(fallback.channelCode);
        }
      }
    });
  }

  private readonly otpFormRef = viewChild<ElementRef<HTMLFormElement>>('otpFormEl');
  private readonly otpInputRef = viewChild<ElementRef<HTMLInputElement>>('otpInputEl');
  private otpAbortController: AbortController | null = null;

  protected readonly activeOtpSlot = computed(() => {
    if (!this.otpFocused()) return -1;
    return Math.min(this.otpCode().length, 5);
  });

  protected otpDigitAt(index: number): string {
    return this.otpCode()[index] ?? '';
  }

  protected readonly fullIdentifier = computed(() => {
    if (this.loginMode() === 'email') {
      return this.emailInput().trim();
    }
    const raw = this.phoneInput().trim().replace(/[\s\-()]/g, '');
    if (!raw) return '';
    if (raw.startsWith('+')) return raw;
    const dialDigits = this.countryDial().replace('+', '');
    const stripped = raw.startsWith(dialDigits) && raw.length > 9
      ? raw.slice(dialDigits.length)
      : raw.replace(/^0+/, '');
    return stripped ? `${this.countryDial()}${stripped}` : '';
  });

  protected setLoginMode(mode: LoginMode): void {
    this.loginMode.set(mode);
    this.errorMessage.set('');
    if (mode === 'email' && this.store.isOtpChannelActive('EMAIL')) {
      this.channel.set('EMAIL');
    } else if (mode === 'phone') {
      if (this.store.isOtpChannelActive('WHATSAPP')) {
        this.channel.set('WHATSAPP');
      } else if (this.store.isOtpChannelActive('SMS')) {
        this.channel.set('SMS');
      } else {
        this.channel.set('EMAIL');
      }
    }
  }

  protected onIdentifierInput(val: string): void {
    if (this.loginMode() === 'email') {
      this.emailInput.set(val);
    } else {
      this.phoneInput.set(val);
    }
    if (this.errorMessage()) {
      this.errorMessage.set('');
    }
  }

  protected validateOnBlur(): void {
    const id = this.fullIdentifier();
    if (!id) return;
    if (this.loginMode() === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(id)) {
      this.errorMessage.set('Please enter a valid email address (for example, name@organisation.go.ug).');
    } else if (this.loginMode() === 'phone' && !/^\+\d{9,15}$/.test(id)) {
      this.errorMessage.set('Please enter a valid phone number after the country code.');
    }
  }

  protected fillDemo(mode: LoginMode, value: string): void {
    this.setLoginMode(mode);
    if (mode === 'email') {
      this.emailInput.set(value);
    } else {
      this.countryDial.set('+256');
      this.phoneInput.set(value);
    }
    this.errorMessage.set('');
  }

  protected sendOtp(): void {
    const id = this.fullIdentifier();
    if (this.loginMode() === 'email') {
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(id)) {
        this.errorMessage.set('Please enter a valid email address.');
        return;
      }
    } else {
      if (!/^\+\d{9,15}$/.test(id)) {
        this.errorMessage.set('Please enter a valid mobile number (e.g., 700 000 000).');
        return;
      }
    }

    this.errorMessage.set('');
    this.otpCode.set('');
    this.auth.requestOtp(id, this.loginMode(), this.channel(), (res) => {
      if (!res.ok) {
        this.errorMessage.set(res.error || 'Unable to send OTP verification code.');
        return;
      }
      setTimeout(() => this.otpInputRef()?.nativeElement.focus(), 50);
      void this.startSmsOtpListener();
    });
  }

  protected onOtpInput(value: string): void {
    const digitsOnly = value.replace(/\D/g, '').slice(0, 6);
    this.otpCode.set(digitsOnly);
    if (this.errorMessage()) {
      this.errorMessage.set('');
    }
  }

  protected resendOtp(): void {
    const current = this.auth.challenge();
    if (!current) return;
    this.errorMessage.set('');
    this.otpCode.set('');
    this.auth.requestOtp(current.identifier, current.loginMode, current.channel, (res) => {
      if (!res.ok) {
        this.errorMessage.set(res.error || 'Unable to resend OTP code.');
        return;
      }
      void this.startSmsOtpListener();
    });
  }

  protected changeIdentifier(): void {
    this.stopSmsOtpListener();
    this.errorMessage.set('');
    this.otpCode.set('');
    this.auth.clearChallenge();
  }

  protected submitOtp(): void {
    this.stopSmsOtpListener();
    this.auth.verifyOtp(this.otpCode(), (res) => {
      if (!res.ok) {
        this.errorMessage.set(res.error || 'Verification failed.');
        return;
      }
      void this.router.navigateByUrl(
        this.auth.isStaffOfficer() ? '/out-of-office' : '/dashboard'
      );
    });
  }

  private stopSmsOtpListener(): void {
    if (this.otpAbortController) {
      this.otpAbortController.abort();
      this.otpAbortController = null;
    }
  }

  private async startSmsOtpListener(): Promise<void> {
    if (typeof window === 'undefined' || !('OTPCredential' in window)) {
      return;
    }
    this.stopSmsOtpListener();
    const ac = new AbortController();
    this.otpAbortController = ac;

    try {
      const credential = (await (navigator.credentials as any).get({
        otp: { transport: ['sms'] },
        signal: ac.signal,
      })) as { code?: string } | null;

      if (credential?.code) {
        this.otpCode.set(credential.code);
        const formEl = this.otpFormRef()?.nativeElement;
        if (formEl) {
          formEl.requestSubmit();
        } else {
          this.submitOtp();
        }
      }
    } catch (err: any) {
      if (err?.name === 'AbortError') {
        return;
      }
    }
  }

  ngOnDestroy(): void {
    this.stopSmsOtpListener();
  }
}

