# Swizima Web

Admin portal for Swizima, built with **Angular 22**: standalone components, signals, zoneless change detection, and vanilla CSS.

## Quick start

**Requires Node `^22.22.3 || ^24.15.0 || >=26`** (pinned in [.nvmrc](.nvmrc)). Always use the project's
local CLI (`npm start` / `npx ng …`), not a globally installed `ng`.

```bash
cd swizima-web
nvm use                   # picks Node 24 from .nvmrc
npm install
npm start                 # http://localhost:4200
```

> [!WARNING]
> `ERR_MODULE_NOT_FOUND … @angular/build/src/builders/application/index` means an old Node / global
> Angular CLI is being used (e.g. in a `sudo su` root shell). Switch to Node 24 and run `npm start`.

The dev server proxies `/api/**` to the API gateway, so the browser only talks to one origin and CORS is not needed in development.

| Variable | Default | Purpose |
|---|---|---|
| `API_GATEWAY_URL` | `http://localhost:8080` | Where the dev server proxies `/api/**` ([proxy.conf.mjs](proxy.conf.mjs)) |

```bash
API_GATEWAY_URL=http://localhost:8090 npm start   # if 8080 is taken
```

Other commands:

| Command | What it does |
|---|---|
| `npm run build` | Production build → `dist/swizima-web/browser` |
| `npm test` | Unit tests (Vitest) |

> [!NOTE]
> In production, serve the built files behind the same host as the gateway, or set the `API_BASE_URL`
> token in `src/app/core/config/api.config.ts` and allow the origin with the gateway's `CORS_ALLOWED_ORIGINS`.

## Features

- **Sign in in two steps** (usersapi via the gateway):
  1. `/auth/login`: email or phone plus password. The user picks where the code is sent: Email (default), SMS or WhatsApp.
  2. `/auth/verify-otp`: one `one-time-code` input that submits by itself once 6 digits are entered.
     - Shows expiry and resend countdowns, plus resend and switch-channel buttons.
     - Fills the code automatically from SMS where WebOTP is supported.
- **Layout:**
  - Desktop (≥1024px): fixed left sidebar.
  - Mobile: hamburger button that opens a swipeable drawer. The drawer closes on Escape, a backdrop tap or a link click.
- **Menu:** Dashboard · Clients · Services offered · Payments · Users · Configurations.
- **Register client** (`/clients/new`, opened from *Register client* on `/clients`): page form posting to `POST /api/v1/clients`.
  - Leaving with unsaved input asks for confirmation (`core/forms/unsaved-changes.guard.ts`).
  - The client code is suggested from the name and can be edited. A duplicate code (`409`) is shown on the code field.
  - Validation errors from the API (`400` `errors`) appear under the matching fields.
- **Lists** (Clients, Services offered, Users):
  - Debounced search, filters and server-side pagination.
  - Tables turn into cards on small screens.
  - Skeleton, empty and error states.
- **Payments / Configurations** show planned features until `paymentsapi` and `configurationsapi` have endpoints.
- **Light and dark mode** follow the OS setting. Every page sets a title (`<Page> · Swizima`).

## Structure

```text
src/app/
├── core/
│   ├── api/          SwizimaApi – typed calls for clients, service catalogs, users
│   ├── auth/         AuthService (signals + sessionStorage), route guards
│   ├── data/         remoteData() loader, debounced() helper
│   ├── http/         interceptor (base URL, X-Request-Id), ProblemDetail → message
│   ├── models/       DTO types mirroring the Java records
│   └── title/        title strategy (document title + top-bar title)
├── layout/           shell (sidebar, top bar, drawer), nav-menu, nav-items
├── shared/           icon, page-header, status-badge, pagination, empty-state, coming-soon
└── features/
    ├── auth/         auth-layout, login, verify-otp
    ├── dashboard/  clients/  services/  users/  payments/  configurations/
```

To add a menu item, add it to `layout/nav-items.ts` and add a lazy route in `app.routes.ts`.

## Known limitations

> [!IMPORTANT]
> usersapi does not issue tokens (JWT) yet. After OTP verification, the portal only stores the signed-in user
> in `sessionStorage`, and the API endpoints are not protected. Once tokens exist:
> - store the token in `AuthService`;
> - attach it in `core/http/api.interceptor.ts`;
> - on `401`, log out and redirect to the login page.

- The dashboard's "Payments today" figure and the Payments and Configurations pages wait on their backend services.
- "Change password" (for `mustChangePassword` users) is not built in the UI yet. The dashboard shows a reminder instead.
