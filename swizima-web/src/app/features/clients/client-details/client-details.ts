import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Icon } from '../../../shared/icon/icon';
import { humanize } from '../../../shared/status-badge/status-badge';
import { ClientLayout } from '../client-layout/client-layout';

/** /clients/:id/details — full client record loaded by ClientLayout from the API. */
@Component({
  selector: 'app-client-details',
  imports: [DatePipe, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let state = layout.client.state();
    @if (state.data; as c) {
      <div class="grid page-enter" [style.opacity]="state.loading ? 0.6 : 1">
        <section class="card">
          <header class="card-header"><h2><app-icon name="clients" /> Business</h2></header>
          <dl class="card-body">
            <div><dt>Client name</dt><dd>{{ c.clientName }}</dd></div>
            <div><dt>Client code</dt><dd class="cell-mono">{{ c.clientCode }}</dd></div>
            <div><dt>Trading name</dt><dd>{{ c.tradingName || '—' }}</dd></div>
            <div><dt>Client type</dt><dd>{{ humanize(c.clientType) || '—' }}</dd></div>
            <div class="wide">
              <dt>Industry sectors</dt>
              <dd>
                @for (s of c.industrySectors ?? []; track s) { <span class="chip">{{ s }}</span> }
                @empty { — }
              </dd>
            </div>
            <div class="wide"><dt>Description</dt><dd class="pre">{{ c.description || '—' }}</dd></div>
          </dl>
        </section>

        <section class="card">
          <header class="card-header"><h2><app-icon name="mail" /> Contact</h2></header>
          <dl class="card-body">
            <div><dt>Email</dt><dd>@if (c.email) { <a [href]="'mailto:' + c.email">{{ c.email }}</a> } @else { — }</dd></div>
            <div><dt>Phone</dt><dd>@if (c.phoneNumber) { <a [href]="'tel:' + c.phoneNumber">{{ c.phoneNumber }}</a> } @else { — }</dd></div>
            <div><dt>Alternative phone</dt><dd>{{ c.alternativePhone || '—' }}</dd></div>
            <div class="wide"><dt>Address</dt><dd>{{ c.address || '—' }}</dd></div>
            <div><dt>City</dt><dd>{{ c.city || '—' }}</dd></div>
            <div><dt>District</dt><dd>{{ c.district || '—' }}</dd></div>
            <div><dt>Country</dt><dd>{{ c.country || '—' }}</dd></div>
          </dl>
        </section>

        <section class="card">
          <header class="card-header"><h2><app-icon name="users" /> Contact person</h2></header>
          <dl class="card-body">
            <div><dt>Name</dt><dd>{{ c.contactPerson || '—' }}</dd></div>
            <div><dt>Title</dt><dd>{{ c.contactPersonTitle || '—' }}</dd></div>
            <div><dt>Email</dt><dd>{{ c.contactPersonEmail || '—' }}</dd></div>
            <div><dt>Phone</dt><dd>{{ c.contactPersonPhone || '—' }}</dd></div>
          </dl>
        </section>

        <section class="card">
          <header class="card-header"><h2><app-icon name="clock" /> Record</h2></header>
          <dl class="card-body">
            <div><dt>Status</dt><dd>{{ humanize(c.status) }}</dd></div>
            <div><dt>Record status</dt><dd>{{ humanize(c.recordStatus) }}</dd></div>
            <div><dt>Registered</dt><dd>{{ c.createdAt | date: 'medium' }}</dd></div>
            <div><dt>Registered by</dt><dd>{{ c.createdBy || '—' }}</dd></div>
            <div><dt>Last updated</dt><dd>{{ c.updatedAt ? (c.updatedAt | date: 'medium') : '—' }}</dd></div>
            <div><dt>Updated by</dt><dd>{{ c.updatedBy || '—' }}</dd></div>
          </dl>
        </section>
      </div>
    } @else if (state.loading) {
      <div class="grid" aria-busy="true" aria-label="Loading client details">
        @for (i of [1, 2, 3, 4]; track i) {
          <div class="card skeleton-rows"><span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span></div>
        }
      </div>
    }
  `,
  styles: `
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 1.25rem; }
    .card-header h2 { display: flex; align-items: center; gap: 0.5rem; font-size: 1rem; }
    .card-header app-icon { width: 18px; height: 18px; color: var(--brand-500); }
    dl { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem 1.25rem; margin: 0; }
    dl > div { min-width: 0; }
    .wide { grid-column: 1 / -1; }
    dt { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-subtle); margin-bottom: 0.2rem; }
    dd { margin: 0; overflow-wrap: anywhere; }
    dd a { color: var(--brand-500); text-decoration: none; }
    dd a:hover { text-decoration: underline; }
    .pre { white-space: pre-line; }
    .chip {
      display: inline-block; margin: 0 0.35rem 0.35rem 0; padding: 0.2rem 0.6rem; border-radius: 999px;
      font-size: 0.8rem; background: var(--bg-subtle); border: 1px solid var(--border);
    }
    @media (max-width: 480px) { dl { grid-template-columns: 1fr; } }
  `,
})
export class ClientDetails {
  protected readonly layout = inject(ClientLayout);
  protected readonly humanize = humanize;
}
