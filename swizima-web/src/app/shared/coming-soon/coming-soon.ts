import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Icon, IconName } from '../icon/icon';

export interface PlannedFeature {
  icon: IconName;
  title: string;
  description: string;
}

/** Polished placeholder for modules whose backend API is not built yet. */
@Component({
  selector: 'app-coming-soon',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="hero card">
      <div class="glow" aria-hidden="true"></div>
      <div class="hero-icon"><app-icon [name]="icon()" /></div>
      <span class="badge badge-info">In development</span>
      <h2>{{ heading() }}</h2>
      <p>{{ message() }}</p>
    </section>

    <h3 class="section-title">What's planned</h3>
    <ul class="features">
      @for (feature of features(); track feature.title) {
        <li class="card feature">
          <div class="feature-icon"><app-icon [name]="feature.icon" /></div>
          <div>
            <h4>{{ feature.title }}</h4>
            <p>{{ feature.description }}</p>
          </div>
        </li>
      }
    </ul>
  `,
  styles: `
    :host { display: grid; gap: 1.25rem; }
    .hero {
      position: relative;
      overflow: hidden;
      display: grid;
      justify-items: start;
      gap: 0.75rem;
      padding: clamp(1.5rem, 4vw, 2.5rem);
      p { max-width: 60ch; color: var(--text-muted); }
      h2 { font-size: 1.35rem; }
    }
    .glow {
      position: absolute;
      inset: -40% -10% auto auto;
      width: 380px;
      height: 380px;
      background: var(--gradient-brand);
      filter: blur(90px);
      opacity: 0.18;
      pointer-events: none;
    }
    .hero-icon {
      display: grid;
      place-items: center;
      width: 52px;
      height: 52px;
      border-radius: 14px;
      color: #fff;
      background: var(--gradient-brand);
      box-shadow: 0 10px 24px -10px rgb(99 102 241 / 0.8);
      app-icon { width: 26px; height: 26px; }
    }
    .section-title { color: var(--text-muted); font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.06em; }
    .features {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(min(100%, 260px), 1fr));
      gap: 1rem;
    }
    .feature {
      display: flex;
      gap: 0.9rem;
      padding: 1.1rem;
      transition: transform var(--duration) var(--ease), box-shadow var(--duration) var(--ease);
      &:hover { transform: translateY(-2px); box-shadow: var(--shadow); }
      h4 { font-size: 0.95rem; font-weight: 600; margin: 0 0 0.25rem; }
      p { font-size: 0.85rem; color: var(--text-muted); }
    }
    .feature-icon {
      display: grid;
      place-items: center;
      flex: none;
      width: 38px;
      height: 38px;
      border-radius: 10px;
      background: color-mix(in srgb, var(--brand-500) 12%, transparent);
      color: var(--brand-500);
    }
  `,
})
export class ComingSoon {
  readonly icon = input.required<IconName>();
  readonly heading = input.required<string>();
  readonly message = input.required<string>();
  readonly features = input<PlannedFeature[]>([]);
}
