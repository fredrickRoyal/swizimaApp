# Swizima Web

Public landing page and admin portal for **Swizima** (*Explore your world for less*), built with **Angular 22**: standalone components, signals, zoneless change detection, and vanilla CSS.

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
| `API_GATEWAY_URL` | `http://localhost:8080` (dev) / `http://swizima-apigateway:9080` (Docker) | Where `/api/**` requests are proxied ([proxy.conf.mjs](proxy.conf.mjs) in dev, Nginx in Docker) |
| `MOBILE_APP_DOWNLOAD_URL` | `https://drive.google.com/drive/folders/1cLdYhVLrO4Oyh1xLOhFIJIWCW-99WSg4?usp=sharing` | Runtime URL for the **Download Mobile App** buttons (injected into `/env.js` at container startup) |
| `WEB_PORT` | `4200` | Host port exposed by `swizima-web/docker-compose.yml` |

```bash
API_GATEWAY_URL=http://localhost:9080 npm start   # point dev server to gateway on 9080
```

Other commands:

| Command | What it does |
|---|---|
| `npm run build` | Production build → `dist/swizima-web/browser` |
| `npm test` | Unit tests (Vitest) |

### Running with Docker

`swizima-web` includes a multi-stage [Dockerfile](Dockerfile) (Node 24 build → Nginx 1.27 Alpine runtime) and [docker-compose.yml](docker-compose.yml):

```bash
cd swizima-web
docker build -t fredrickkasoma/swizima-web:latest .
docker compose up -d
```

To override the mobile app download link or gateway URL at runtime without rebuilding the image:

```bash
MOBILE_APP_DOWNLOAD_URL="https://drive.google.com/drive/folders/..." \
API_GATEWAY_URL="http://swizima-apigateway:9080" \
docker compose up -d
```

## Features

- **Public Landing Page & Pricing Modal** (`/welcome` and `/pricing`):
  - Accessible without signing in; linked from the sign-in screen (`Explore Swizima & Pricing`).
  - Showcases the tagline *"Explore your world for less"*, core platform capabilities (Car Park, Car Wash, Workshop & Service Bay, Prepaid Wallet), and direct Android app download.
  - **Customer Benefits & Trust:** Explains how printed/WhatsApp receipts give motorists assurance that they handed their vehicle to an authorized attendant, boost customer confidence, and keep the vehicle owner's phone number on record for easy contact — featuring a customer testimony from **Abija Manfred**.
  - **Onboarding Packages & Ongoing Billing Options:**
    - **With Bluetooth Printer (`UGX 200,000`):** Full onboarding, portable 58mm Bluetooth thermal printer, and `UGX 10,000` free starter wallet credit.
    - **Without Printer (`UGX 50,000`):** Digital-only onboarding with `UGX 10,000` free starter wallet credit.
    - **Option A — 5% Transaction Commission (Prepaid Wallet):** `5%` deducted automatically from the prepaid Swizima Wallet on every car parked or washed; top up anytime.
    - **Option B — Custom End-of-Month Fee (`MONTHLY_FLAT`):** Agree on a custom fixed monthly amount paid after the end of every month with `0%` commission deducted per car.
    - **Interactive Pricing & Billing Calculator Modal:** Lets visitors toggle between onboarding packages (`UGX 200,000` / `UGX 50,000`) and ongoing billing models (`5% Per-Transaction Commission` vs `Custom End-of-Month Fee` with a customizable monthly fee and daily car volume estimator).
- **Mobile App Download (`MOBILE_APP_DOWNLOAD_URL`):**
  - Available on the public landing page (`/welcome`), the authentication layout (`/auth/login`), and the authenticated sidebar/drawer (`nav-menu`).
- **Sign in in two steps** (usersapi via the gateway):
  1. `/auth/login`: email or phone plus password. The user picks where the code is sent: Email (default), SMS or WhatsApp.
  2. `/auth/verify-otp`: segmented 6-digit OTP input that auto-submits once 6 digits are entered, with expiry/resend countdowns and channel switching.
- **Layout:**
  - Desktop (≥1024px): fixed left sidebar.
  - Mobile: hamburger button that opens a swipeable drawer.
- **Menu:** Dashboard · Clients · Services offered · Client transactions · Payments · Users · Configurations.
- **Client Billing & Configurations (`/configurations` and `/clients/:id/billing`):**
  - Configure default and per-client billing plans using two supported billing models:
    - **Per-transaction Commission (`COMMISSION`):** Deducts a configurable percentage (default `5%`) from the client's prepaid wallet on every transaction.
    - **End-of-Month Fixed Fee (`MONTHLY_FLAT`):** Client pays an agreed fixed monthly amount (`monthlyAmount`, e.g. `UGX 150,000 / month`) at the end of every month instead of per-transaction commission (`0%` deducted per transaction; transactions are never blocked by wallet balance).
- **Register client** (`/clients/new`, opened from *Register client* on `/clients`): page form posting to `POST /api/v1/clients` with multi-select industry sectors and Uganda district/town selectors.
- **Lists** (Clients, Services offered, Client transactions, Payments, Users):
  - Debounced search, filters and server-side pagination.
  - Responsive tables that turn into cards on small screens.
- **Light and dark mode** follow the OS setting. Every page sets a title (`<Page> · Swizima`).

## Structure

```text
src/app/
├── core/
│   ├── api/          SwizimaApi – typed calls for clients, catalogs, transactions, billing, users
│   ├── auth/         AuthService (signals + sessionStorage), route guards
│   ├── config/       API_BASE_URL & MOBILE_APP_DOWNLOAD_URL tokens (reads window.__SWIZIMA_ENV__)
│   ├── data/         remoteData() loader, debounced() helper, Uganda locations
│   ├── http/         interceptor (base URL, X-Request-Id with HTTP fallback), ProblemDetail → message
│   ├── models/       DTO types mirroring the Java records
│   └── title/        title strategy (document title + top-bar title)
├── layout/           shell (sidebar, top bar, drawer), nav-menu, nav-items
├── shared/           icon, page-header, status-badge, pagination, empty-state, coming-soon
└── features/
    ├── landing/      Public landing page, customer benefits, testimony & pricing calculator modal
    ├── auth/         auth-layout, login, verify-otp, change-password
    ├── dashboard/  clients/  services/  transactions/  payments/  users/  configurations/
```

To add a menu item, add it to `layout/nav-items.ts` and add a lazy route in `app.routes.ts`.
