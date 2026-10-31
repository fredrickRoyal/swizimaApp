# Changelog

All notable changes to the **Swizima** monorepo are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Entries are grouped by version, then by type of change, and prefixed with the affected component:
`[api]`, `[clientapi]`, `[usersapi]`, `[configurationsapi]`, `[paymentsapi]`, `[clienttransactionsapi]` (formerly `[serviceapi]`), `[mobile]`, `[web]`, `[repo]`.

**Change types:** `Added` · `Changed` · `Deprecated` · `Removed` · `Fixed` · `Security`

---

## [Unreleased]

### Changed
- **[clienttransactionsapi]** Renamed `serviceapi` → `clienttransactionsapi` (folder, Maven artifact, package `com.swizima.clienttransactionsapi`, main class `ClienttransactionsapiApplication`, `spring.application.name`). Port stays 8084.
- **[apigateway]** Route renamed: `/api/v1/services/**` → `/api/v1/client-transactions/**`; docs path `/docs/serviceapi` → `/docs/clienttransactionsapi`; env var `SERVICE_API_URL` → `CLIENT_TRANSACTIONS_API_URL`.

### Fixed
- **[apigateway]** Web portal login returned `403 Invalid CORS request`: the Angular dev server origin `http://localhost:4200` is now in the default `CORS_ALLOWED_ORIGINS`.
- **[web]** OTP countdown showed `NaN:NaN` when usersapi did not return `expiresInSeconds` / `resendAvailableInSeconds` (older build); it now falls back to `expiresAt` / `resendAvailableAt`, then to 5 min / 60 s.

### Changed
- **[web]** OTP page uses six segmented code boxes (active-box caret, fill animation, error shake). A single hidden `one-time-code` input still drives them, so paste, SMS/password-manager autofill and screen readers keep working.
- **[web]** Register client: *Industry / sector* is now a multi-select of automotive sector chips (`INDUSTRY_SECTORS` in `client-form.ts`). An *Other…* chip lets users type extra sectors (comma separated). Picks are de-duplicated and sent as `industrySectors: string[]`.
- **[web]** Register client, *Contact & location*:
  - *Email* is labelled as optional (the form and API never required it).
  - *District* is a dropdown of Uganda districts grouped by region, and *City / town* is a dropdown filtered by the chosen district. Choosing a town first fills in its district.
  - Both dropdowns have an *Other…* option for free text, and both become plain text inputs when *Country* is not Uganda.
  - The data lives in `src/app/core/data/uganda-locations.ts`. The API is unchanged and still stores plain strings.
- **[clientapi]** **Breaking:** `industrySector` (string) is replaced by `industrySectors` (array) in `ClientRequest`/`ClientResponse` (max 20 entries, each ≤150 chars). Values are trimmed and de-duplicated, then stored as a JSON array string in a new `clients.industry_sectors` text column via `StringListJsonConverter`, e.g. `["Car wash & detailing","Car park & parking"]`. `ddl-auto=update` adds the new column. The old `industry_sector` column is no longer used; to carry its data over, run `UPDATE clients SET industry_sectors = json_build_array(industry_sector)::text WHERE industry_sector <> '' AND industry_sectors IS NULL;` and then optionally `ALTER TABLE clients DROP COLUMN industry_sector;`.

### Added
- **[web]** **Register client** page at `/clients/new` (button on `/clients`), posting to `POST /api/v1/clients` via the gateway. Sections for business details, contact & location and primary contact person (side-by-side intro + fields on wide screens); client code auto-suggested from the name (editable, regenerate button); client-side validation mirroring `ClientRequest`; server `400` field errors and `409` duplicate code mapped onto the fields; sticky action bar; unsaved-changes protection (`unsavedChangesGuard` canDeactivate + `beforeunload`); returns to the list with a success notice and the new row highlighted.
- **[web]** Angular 22 admin portal in `swizima-web/` (standalone, signals, zoneless, vanilla CSS design system with light/dark support, Inter font).
- **[web]** Responsive app shell: fixed left sidebar (≥1024px), sticky glass top bar with page title, and a swipeable mobile navigation drawer (popover + scroll-snap, Escape / backdrop / link close, focus and `inert` handling).
- **[web]** Two-step sign-in: `/auth/login` (email or phone + password, OTP channel picker, show-password) and `/auth/verify-otp` (single `one-time-code` input, auto-submit on 6 digits, expiry and resend countdowns, resend / switch channel, WebOTP auto-fill). Route guards for authenticated, guest and pending-OTP states; `returnUrl` restricted to in-app paths.
- **[web]** Pages: Dashboard (KPI counts, services by category, quick actions), Clients, Services offered and Users (search, filters, server-side pagination, responsive tables that become cards on mobile, skeleton/empty/error states), and Payments / Configurations previews until their APIs exist.
- **[web]** HTTP interceptor adding `X-Request-Id`; RFC 9457 ProblemDetail errors shown as friendly messages; dev proxy `/api` → gateway (`API_GATEWAY_URL`, default `http://localhost:8080`).
- **[usersapi]** `OtpChallengeResponse` now includes `expiresInSeconds` and `resendAvailableInSeconds` so clients can run countdowns without depending on clock/timezone.
- **[infra]** `swizima-api/docker-compose.yml` with **PostgreSQL 18.6** (pinned; latest stable), health check, persistent volume `swizima-postgres-data` bound to host folder `/home/kasoma/swizima/Projects/Swizima/dbdata` (override `POSTGRES_DATA_DIR`), network `swizima-network`; init script `docker/postgres/init/01-create-databases.sh` creates `swizima_users`, `swizima_client`, `swizima_configurations`, `swizima_payments`, `swizima_service`; `.env.example` for overrides.
- **[usersapi]** `SuperAdminSeeder`: creates the initial super admin on start-up (`kfredrick35@gmail.com`, `+256700408387`, `ACTIVE`, OTP via email); idempotent; configurable via `app.seed.super-admin.*` / `SUPER_ADMIN_*` env vars; password change prompted on first login by default.
- **[usersapi]** `UserRole` (`SUPER_ADMIN`, `ADMIN`, `USER`) and `role` column on `system_users` (DB default `USER`); returned in user responses.
- **[apigateway]** API gateway (Spring Cloud Gateway Server Web MVC, Spring Cloud `2025.1.4`) on port `8080` as the single entry point for all services.
- **[apigateway]** Routes: `/api/v1/clients/**`, `/api/v1/service-catalogs/**` → clientapi (8081); `/api/v1/configurations/**` → configurationsapi (8082); `/api/v1/payments/**` → paymentsapi (8083); `/api/v1/services/**` → serviceapi (8084); `/api/v1/users/**`, `/api/v1/auth/**` → usersapi (8085). Downstream URLs overridable via `*_API_URL` env vars.
- **[apigateway]** Aggregated Swagger UI at `/swagger-ui.html` (service docs proxied via `/docs/<service>/v3/api-docs`); `/`, `/docs`, `/swagger` redirect to it; sorting, filter, request duration; toggle with `SWAGGER_UI_ENABLED`.
- **[clientapi] [usersapi]** OpenAPI `servers` set to relative `/` (override with `app.openapi.server-url`) so Swagger "Try it out" works through the gateway as well as directly.
- **[apigateway]** Central CORS config (`CORS_ALLOWED_ORIGINS`), `X-Request-Id` correlation header (generated/propagated/logged), downstream timeouts, and `503` ProblemDetail when a service is unavailable.
- **[usersapi]** `SystemUser` entity (`system_users` table) with `username`, `username_type`, `password` (BCrypt hash), `enabled`, `activation_status`, `password_system_generated`, `password_changed_at`, `last_login_at`.
- **[usersapi]** `ParentEntity` base class with audit columns (`created_by`, `created_at`, `updated_by`, `updated_at`, `record_status`) via Spring Data JPA auditing.
- **[usersapi]** Username can be an **email or phone number**; type is auto-detected and the value is normalised (lower-cased email / E.164 phone, e.g. `0700123456` → `+256700123456`) via `UsernameNormalizer`.
- **[usersapi]** System-generated temporary passwords (`PasswordGenerator`); users with a generated password get `mustChangePassword = true` on login.
- **[usersapi]** Account lifecycle: `PENDING_ACTIVATION` → `ACTIVE` on first password change; also `SUSPENDED`, `LOCKED`, `DEACTIVATED`.
- **[usersapi]** User management endpoints under `/api/v1/users`: create, list/search/filter, get by ID, get by username, update, change activation status, enable/disable, reset password, soft delete.
- **[usersapi]** Authentication endpoints under `/api/v1/auth`: `login`, `change-password`.
- **[usersapi]** Password policy (8–100 chars, at least one letter and one digit).
- **[usersapi]** Global error handling with RFC 9457 `ProblemDetail` (`400`, `401`, `403`, `404`, `409`).
- **[usersapi]** PostgreSQL configuration (`swizima_users`, port `8085`), HikariCP pool, env-var overrides (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `DEFAULT_COUNTRY_CODE`).
- **[usersapi]** Dependencies: Lombok, springdoc-openapi (Swagger UI), `spring-security-crypto` (BCrypt), Validation, Actuator.
- **[clientapi]** Swagger UI via springdoc-openapi at `/swagger-ui.html`, `OpenApiConfig`, and `@Tag` groupings on controllers.
- **[usersapi]** Swagger UI at `/swagger-ui.html`.
- **[usersapi]** **OTP login (two-step)**: `POST /auth/login` checks the password and sends a 6-digit OTP; `POST /auth/verify-otp` completes login; `POST /auth/resend-otp` issues a new code (optionally on another channel).
- **[usersapi]** OTP channels `EMAIL` (default), `SMS`, `WHATSAPP`; user can choose per login via `otpChannel`, or set `preferredOtpChannel` on their profile. Falls back to an available channel when the default has no destination.
- **[usersapi]** `OtpChallenge` entity (`otp_challenges`) storing hashed codes with expiry (5 min), max attempts (5), resend cooldown (60 s), single use and invalidation of older codes.
- **[usersapi]** `email`, `phone_number` and `preferred_otp_channel` on `SystemUser` (contact field defaults from the username).
- **[usersapi]** Email OTP delivery via SMTP (`spring-boot-starter-mail`) with HTML + plain-text template.
- **[usersapi]** Email configuration (`spring.mail.*`, `app.mail.from`, `app.mail.from-name`) overridable via `MAIL_*` env vars; Gmail defaults.
- **[usersapi]** OTP settings under `app.otp.*` and a dev-only `OTP_LOG_CODE` flag.
- **[usersapi]** Pluggable `OtpSender` interface with `EmailOtpSender`, and placeholder `SmsOtpSender` / `WhatsAppOtpSender`.
- **[usersapi]** `429 Too Many Requests` (resend cooldown) and `503 Service Unavailable` (delivery failure) error responses.

### Changed
- **[clientapi] [usersapi]** `id` (UUID, generated) moved into `ParentEntity`; `Client`, `ServiceCatalog`, `SystemUser` and `OtpChallenge` now inherit it (no schema change).
- **[usersapi]** Java version set to `21` (was `27`, not installed locally).
- **[usersapi]** README rewritten with username strategy, account lifecycle, endpoints and setup.
- **[usersapi]** `POST /auth/login` now returns an `OtpChallengeResponse` instead of logging in directly; `last_login_at` is set after OTP verification.
- **[clientapi]** README updated with port and Swagger UI link.

### Removed
- **[usersapi]** Placeholder `.gitkeep` files from populated packages.

### Security
- **[usersapi]** Seeded super admin default password (`123456`) is for local development only — override `SUPER_ADMIN_PASSWORD` in shared/production environments.
- **[usersapi]** Passwords stored only as BCrypt hashes and never returned by the API.
- **[usersapi]** Login returns the same `401` for unknown usernames and wrong passwords to prevent username enumeration.

### Known gaps
- **[usersapi]** No JWT/token auth yet — `mustChangePassword` must be enforced by clients; `/api/v1/users` admin endpoints are unsecured.
- **[usersapi]** SMS and WhatsApp OTP providers not yet integrated (return `503`).
- **[usersapi]** No failed-login lockout or OTP-based activation/reset yet.

---

## [0.1.0] - 2026-10-29

### Added
- **[api]** Standard package layout (`config`, `controller`, `dto`, `entity`, `exception`, `repository`, `service`) in `clientapi`, `configurationsapi`, `paymentsapi`, `serviceapi` and `usersapi`.
- **[clientapi]** `ParentEntity` base class with JPA auditing (`created_by`, `created_at`, `updated_by`, `updated_at`, `record_status`).
- **[clientapi]** `Client` entity (`clients` table): code, name, trading name, type, industry sector, description, contact details, location, contact person and status.
- **[clientapi]** Client endpoints under `/api/v1/clients`: create, list/search (paginated), get by ID, get by code, update, update status, soft delete.
- **[clientapi]** `ServiceCatalog` entity (`service_catalogs` table) linked to `Client`, with per-client unique service code, category, price, currency (default `UGX`), duration and status.
- **[clientapi]** Service catalog endpoints under `/api/v1/service-catalogs` with filtering by client, category, status and keyword.
- **[clientapi]** Global error handling with RFC 9457 `ProblemDetail` (`400`, `404`, `409`).
- **[clientapi]** PostgreSQL configuration (`swizima_client`, port `8081`), HikariCP pool and env-var overrides.
- **[clientapi]** Lombok (`@Getter`, `@Setter`, `@SuperBuilder`, `@RequiredArgsConstructor`), Validation and Actuator dependencies.
- **[api]** Per-service `README.md` files for all five services.

### Changed
- **[clientapi]** Java version set to `21`.
- **[repo]** Root `README.md` now links to per-service READMEs.
- **[repo]** Root `.gitignore` extended for Java/Maven services (`target/`, `HELP.md`, Maven wrapper jar, logs), local env/secret files, and Eclipse/NetBeans/VS Code files.

### Removed
- **[api]** Duplicate per-service `.gitignore` files (now covered by the root `.gitignore`).

---

## [0.0.1] - 2026-10-28

### Added
- **[mobile]** Initial Android app (Jetpack Compose, Material 3): authentication (phone/email with OTP), automotive dashboard (car park, car wash, repair, review), service records, PDF receipts and WhatsApp ticket sender.
- **[repo]** Placeholder folders for `swizima-api` and `swizima-web`.

### Changed
- **[repo]** Restructured into a monorepo with `swizima-mobile`, `swizima-api` and `swizima-web`; Git root moved to the workspace level.

### Removed
- **[repo]** `.idea` folder from version control.

[Unreleased]: #unreleased
[0.1.0]: #010---2026-10-29
[0.0.1]: #001---2026-10-28
