import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MOBILE_APP_DOWNLOAD_URL } from '../../../core/config/api.config';
import { Icon } from '../../../shared/icon/icon';
import { ThemeToggle } from '../../../shared/theme-toggle/theme-toggle';

/** Split-screen frame for login / OTP: brand hero on large screens, form card everywhere. */
@Component({
  selector: 'app-auth-layout',
  imports: [RouterLink, RouterOutlet, Icon, ThemeToggle],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="hero">
      <img src="images/auth-hero.webp" alt="" class="hero-img" fetchpriority="high" />
      <div class="hero-shade"></div>
      <div class="hero-content">
        <a routerLink="/welcome" class="brand" id="auth-hero-home">
          <span class="logo">S</span>
          <strong>Swizima</strong>
        </a>
        <div class="pitch">
          <span class="eyebrow"><app-icon name="sparkles" /> Admin portal</span>
          <p class="headline">Run every car park, wash bay and workshop from one place.</p>
          <ul class="points">
            <li><app-icon name="clients" /> Onboard and manage clients</li>
            <li><app-icon name="car" /> Publish the services they offer</li>
            <li><app-icon name="payments" /> Track payments end to end</li>
          </ul>
        </div>
        <small class="copyright">© {{ year }} Swizima. All rights reserved.</small>
      </div>
    </section>

    <section class="panel">
      <div class="panel-top">
        <a routerLink="/welcome" class="btn btn-ghost home-link" id="auth-back-home">
          <app-icon name="arrowLeft" />
          <span>Home</span>
        </a>
        <app-theme-toggle />
      </div>
      <div class="panel-inner">
        <a routerLink="/welcome" class="mobile-brand" id="auth-mobile-home">
          <span class="logo">S</span>
          <strong>Swizima</strong>
        </a>
        <router-outlet />
      </div>
      <div class="panel-footer">
        <a routerLink="/welcome" class="app-download" id="auth-view-pricing">
          <span class="app-download-icon" aria-hidden="true">
            <app-icon name="wallet" />
          </span>
          <span class="app-download-text">
            <strong>About Swizima &amp; Pricing Model</strong>
            <small>UGX 50k / 200k onboarding · UGX 10k free wallet · 5% per car</small>
          </span>
          <app-icon name="chevronRight" class="app-download-action" />
        </a>
        <a
          class="app-download"
          [href]="mobileAppUrl"
          target="_blank"
          rel="noopener noreferrer"
          id="auth-download-mobile-app"
        >
          <span class="app-download-icon" aria-hidden="true">
            <app-icon name="sms" />
          </span>
          <span class="app-download-text">
            <strong>Get the Swizima Mobile App</strong>
            <small>Download Android APK for field &amp; station staff</small>
          </span>
          <app-icon name="download" class="app-download-action" />
        </a>
        <p class="secure"><app-icon name="shield" /> Protected with two-step verification</p>
      </div>
    </section>
  `,
  styles: `
    :host {
      display: grid;
      min-height: 100svh;
      background: var(--bg);
    }
    @media (min-width: 1024px) {
      :host { grid-template-columns: minmax(0, 1.1fr) minmax(440px, 1fr); }
    }

    .hero { display: none; }
    @media (min-width: 1024px) {
      .hero {
        display: block;
        position: relative;
        overflow: hidden;
        background: var(--gradient-hero);
        color: #fff;
      }
    }
    .hero-img {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
      animation: hero-zoom 24s ease-in-out infinite alternate;
    }
    .hero-shade {
      position: absolute;
      inset: 0;
      background:
        linear-gradient(180deg, rgb(11 16 36 / 0.55) 0%, rgb(11 16 36 / 0.1) 40%, rgb(11 16 36 / 0.85) 100%);
    }
    .hero-content {
      position: relative;
      display: grid;
      grid-template-rows: auto 1fr auto;
      height: 100%;
      padding: 2.5rem 3rem;
    }
    .pitch {
      align-self: end;
      display: grid;
      gap: 1.1rem;
      max-width: 34rem;
      padding-bottom: 2rem;
    }
    .eyebrow {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      justify-self: start;
      padding: 0.35rem 0.75rem;
      border-radius: 999px;
      font-size: 0.8rem;
      font-weight: 600;
      background: rgb(255 255 255 / 0.12);
      border: 1px solid rgb(255 255 255 / 0.18);
      backdrop-filter: blur(8px);
      app-icon { width: 16px; height: 16px; }
    }
    .headline {
      font-size: clamp(1.8rem, 2.6vw, 2.5rem);
      font-weight: 700;
      line-height: 1.15;
      letter-spacing: -0.02em;
      text-wrap: balance;
    }
    .points {
      display: grid;
      gap: 0.6rem;
      color: rgb(255 255 255 / 0.85);
      li { display: flex; align-items: center; gap: 0.6rem; }
      app-icon { color: var(--accent-400); }
    }
    .copyright { color: rgb(255 255 255 / 0.6); }

    .brand, .mobile-brand {
      display: inline-flex;
      align-items: center;
      gap: 0.65rem;
      width: fit-content;
      text-decoration: none;
      strong { font-size: 1.25rem; letter-spacing: -0.01em; }
    }
    .logo {
      display: grid;
      place-items: center;
      width: 40px;
      height: 40px;
      border-radius: 12px;
      color: #fff;
      font-weight: 800;
      font-size: 1.2rem;
      background: var(--gradient-brand);
      box-shadow: 0 10px 24px -10px rgb(99 102 241 / 0.9);
    }
    .mobile-brand { margin-bottom: 2rem; }
    @media (min-width: 1024px) { .mobile-brand { display: none; } }

    .panel {
      position: relative;
      display: grid;
      grid-template-rows: auto 1fr auto;
      justify-items: center;
      padding: clamp(1.25rem, 4vw, 2.5rem) clamp(1.25rem, 5vw, 3rem);
      overflow: hidden;
      &::before {
        content: '';
        position: absolute;
        inset: -30% -30% auto auto;
        width: 420px;
        height: 420px;
        background: var(--gradient-brand);
        filter: blur(110px);
        opacity: 0.14;
        pointer-events: none;
      }
    }
    .panel-top {
      position: relative;
      z-index: 1;
      width: 100%;
      max-width: 420px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 0.5rem;
    }
    .home-link {
      min-height: 40px;
      padding: 0.45rem 0.75rem;
      margin-inline-start: -0.5rem;
      color: var(--text-muted);
      &:hover { color: var(--text); }
    }
    .panel-inner {
      position: relative;
      align-self: center;
      width: 100%;
      max-width: 420px;
    }
    .panel-footer {
      position: relative;
      display: grid;
      justify-items: center;
      gap: 0.9rem;
      width: 100%;
      max-width: 420px;
      margin-top: 2rem;
    }
    .app-download {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      width: 100%;
      padding: 0.75rem 0.95rem;
      border-radius: 12px;
      background: var(--surface-subtle, rgb(99 102 241 / 0.06));
      border: 1px solid var(--border, rgb(99 102 241 / 0.18));
      color: var(--text);
      text-decoration: none;
      transition: transform var(--duration) var(--ease), border-color var(--duration), background-color var(--duration);
      &:hover {
        transform: translateY(-1px);
        border-color: var(--brand-500, #6366f1);
        background: rgb(99 102 241 / 0.1);
      }
    }
    .app-download-icon {
      display: grid;
      place-items: center;
      flex: none;
      width: 38px;
      height: 38px;
      border-radius: 10px;
      background: var(--gradient-brand);
      color: #fff;
    }
    .app-download-text {
      display: grid;
      min-width: 0;
      line-height: 1.25;
      strong { font-size: 0.875rem; font-weight: 600; color: var(--text); }
      small { font-size: 0.75rem; color: var(--text-subtle); }
    }
    .app-download-action {
      margin-inline-start: auto;
      color: var(--brand-500, #6366f1);
    }
    .secure {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      color: var(--text-subtle);
      font-size: 0.8rem;
      app-icon { width: 16px; height: 16px; color: var(--success); }
    }

    @keyframes hero-zoom {
      from { transform: scale(1); }
      to { transform: scale(1.08); }
    }
  `,
})
export class AuthLayout {
  protected readonly year = new Date().getFullYear();
  protected readonly mobileAppUrl = inject(MOBILE_APP_DOWNLOAD_URL);
}
