import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Icon } from '../../../shared/icon/icon';

/** Split-screen frame for login / OTP: brand hero on large screens, form card everywhere. */
@Component({
  selector: 'app-auth-layout',
  imports: [RouterOutlet, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="hero" aria-hidden="true">
      <img src="images/auth-hero.webp" alt="" class="hero-img" fetchpriority="high" />
      <div class="hero-shade"></div>
      <div class="hero-content">
        <div class="brand">
          <span class="logo">S</span>
          <strong>Swizima</strong>
        </div>
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
      <div class="panel-inner">
        <div class="mobile-brand">
          <span class="logo">S</span>
          <strong>Swizima</strong>
        </div>
        <router-outlet />
      </div>
      <p class="secure"><app-icon name="shield" /> Protected with two-step verification</p>
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
      display: flex;
      align-items: center;
      gap: 0.65rem;
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
      grid-template-rows: 1fr auto;
      justify-items: center;
      padding: clamp(1.5rem, 5vw, 3rem) clamp(1.25rem, 5vw, 3rem);
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
    .panel-inner {
      position: relative;
      align-self: center;
      width: 100%;
      max-width: 420px;
    }
    .secure {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      margin-top: 2rem;
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
}
