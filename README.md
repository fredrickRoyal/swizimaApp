# Swizima Monorepo Project

Welcome to the **Swizima** multi-platform automotive management suite — *Explore your world for less*.

## 🏗️ Project Structure & Recent Reorganization

The project is organized into a unified workspace with Git version control and Docker orchestration managed at the workspace root:

```text
swizimaApp/
├── .git/                 # Git version control settings (repository root)
├── .gitignore            # Multi-project ignore configuration
├── Dockerfile            # Multi-stage Maven + JRE 21 build for all 6 backend API services
├── docker-bake.hcl       # Docker Buildx Bake definition for building/pushing all images
├── docker-compose.yml    # Backend APIs stack (pulls fredrickkasoma/swizima-*:latest)
├── swizima-mobile/       # Native Android Application (Jetpack Compose, Kotlin, Material 3)
├── swizima-api/          # Backend API microservices + PostgreSQL docker-compose.yml
└── swizima-web/          # Angular 22 Web portal + Nginx Dockerfile & docker-compose.yml
```

### Recent Highlights
- **Public Landing Page & Pricing Modal (`/welcome`, `/pricing`):** Public showcase with tagline *"Explore your world for less"*, onboarding packages (`UGX 200,000` with Bluetooth thermal printer or `UGX 50,000` without printer, each including `UGX 10,000` free starter wallet credit), ongoing billing options (**5% per-transaction wallet commission** or **Custom Agreed End-of-Month Fee**), interactive Pricing & Billing Calculator modal for both models, customer benefits section (verified handover receipts, increased customer confidence, instant owner phone contact), and customer testimony from **Abija Manfred**.
- **Flexible Client Billing Models (`COMMISSION` vs `MONTHLY_FLAT`):** Clients can be configured in `configurationsapi` and `paymentsapi` with either a per-transaction percentage commission deducted from their prepaid wallet (`COMMISSION`) or an agreed fixed end-of-month fee (`MONTHLY_FLAT`) with `0%` per-transaction deduction.
- **Configurable Mobile App Download (`MOBILE_APP_DOWNLOAD_URL`):** Direct Android APK download button on the public landing page, sign-in screen, and portal sidebar, configurable at container startup via the `MOBILE_APP_DOWNLOAD_URL` environment variable.
- **3-Stack Docker Deployment:** Independent Docker Compose stacks for PostgreSQL (`swizima-api/docker-compose.yml`), Backend APIs (`docker-compose.yml`), and Web Frontend (`swizima-web/docker-compose.yml`) connected via `swizima-network`.

---

## 📱 Subprojects

### 1. Swizima Mobile (`swizima-mobile`)
Native Android automotive management application built with Jetpack Compose and Material 3.
- **Features:** Authentication (Phone/Email with country codes and OTP), Automotive Dashboard (Car Park, Car Wash, Repair, Review), Service Records Management, PDF/Bluetooth Thermal Receipt Printing, and WhatsApp Ticket Sender.
- **Tech Stack:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, Coroutines.

### 2. Swizima API (`swizima-api`)
Backend services and database integration for syncing vehicle records, client billing & prepaid wallets, user authentication, and notifications.

- **Tech Stack:** Java 21, Spring Boot 4.1.2 (Web MVC, Data JPA, Validation), PostgreSQL 18.6, Maven.
- **Architecture:** Independent Spring Boot services, each with its own Maven build and database, behind a single **API gateway**.

> **Clients (mobile/web) call only the gateway (`http://localhost:9080` in Docker / `http://localhost:8080` locally).** Aggregated Swagger UI: `/swagger-ui.html`.

| Service | Port | Base package | Responsibility | Docs |
|---|---|---|---|---|
| `apigateway` | 9080 (local 8080) | `com.swizima.apigateway` | Single entry point: routing, CORS, aggregated Swagger, request IDs | [README](swizima-api/apigateway/README.md) |
| `clientapi` | 8081 | `com.swizima.clientapi` | Client (customer/organisation) & service catalog management | [README](swizima-api/clientapi/README.md) |
| `configurationsapi` | 8082 | `com.swizima.configurationsapi` | System configurations & client billing plans (`COMMISSION` / `MONTHLY_FLAT`) | [README](swizima-api/configurationsapi/README.md) |
| `paymentsapi` | 8083 | `com.swizima.paymentsapi` | Payments, client prepaid wallets, top-ups, and commission / monthly billing | [README](swizima-api/paymentsapi/README.md) |
| `clienttransactionsapi` | 8084 | `com.swizima.clienttransactionsapi` | Client transactions (parking, car wash, repairs) | [README](swizima-api/clienttransactionsapi/README.md) |
| `usersapi` | 8085 | `com.swizima.usersapi` | Users, roles, and two-step OTP authentication | [README](swizima-api/usersapi/README.md) |

All services share the same package layout (`config`, `controller`, `dto`, `entity`, `exception`, `repository`, `service`), audit columns via `ParentEntity`, and soft deletes. See each service's README for its data model, endpoints and setup.

#### API documentation (Swagger UI)

| Service | Swagger UI | OpenAPI JSON |
|---|---|---|
| **All services (via gateway)** | http://localhost:9080/swagger-ui.html | — (pick a service in the dropdown) |
| `usersapi` | http://localhost:8085/swagger-ui.html | http://localhost:8085/v3/api-docs |
| `clientapi` | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| `configurationsapi` | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| `paymentsapi` | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| `clienttransactionsapi` | http://localhost:8084/swagger-ui.html | http://localhost:8084/v3/api-docs |

> The service must be running for its link to work. `/swagger-ui.html` redirects to `/swagger-ui/index.html`.

### 3. Swizima Web (`swizima-web`)
Public landing page and admin portal built with **Angular 22** (standalone components, signals, zoneless) and vanilla CSS.
- **Public Landing & Pricing (`/welcome`, `/pricing`):** System overview, onboarding pricing (`UGX 200,000` with Bluetooth printer / `UGX 50,000` without printer + `UGX 10,000` starter wallet), `5%` commission calculator modal, customer benefits & Abija Manfred testimony, and mobile APK download link.
- **Auth:** Two-step sign-in — email/phone + password, then a 6-digit OTP (Email / SMS / WhatsApp).
- **Layout:** Fixed left sidebar on desktop; swipeable drawer on mobile.
- **Menu:** Dashboard · Clients · Services offered · Client transactions · Payments · Users · Configurations.
- **Run locally:** `cd swizima-web && npm install && npm start` → http://localhost:4200.
  `/api` is proxied to the gateway on `http://localhost:8080` (override with `API_GATEWAY_URL`).
- See [swizima-web/README.md](swizima-web/README.md) for details.

---

## 🐳 Docker Build & Deployment (3 Separated Stacks)

All three stacks communicate over the shared external bridge network `swizima-network` (created by the database stack).

### 1. Start the Database Stack (`swizima-api/docker-compose.yml`)
Runs **PostgreSQL 18.6** on `localhost:5432` and creates `swizima_users`, `swizima_client`, `swizima_configurations`, `swizima_payments`, and `swizima_service`:

```bash
mkdir -p /home/kasoma/swizima/Projects/Swizima/dbdata
cd swizima-api
docker compose up -d
docker compose ps   # wait for "healthy"
```

### 2. Build/Push & Start the Backend APIs Stack (`docker-compose.yml`)
Build and push all 6 backend microservices (`usersapi`, `clientapi`, `configurationsapi`, `paymentsapi`, `clienttransactionsapi`, `apigateway`) to Docker Hub using `docker buildx bake`, then start them:

```bash
# From the repository root (swizimaApp/)
docker buildx bake --push
docker compose pull
docker compose up -d
```

### 3. Build/Push & Start the Web Stack (`swizima-web/docker-compose.yml`)
Build and push `fredrickkasoma/swizima-web:latest`, or run it via Compose on port `4200`:

```bash
cd swizima-web
docker build -t fredrickkasoma/swizima-web:latest .
docker push fredrickkasoma/swizima-web:latest
docker compose up -d
```

Runtime environment variables for `swizima-web`:
- `WEB_PORT` (default `4200`): Host port mapped to container port `80`.
- `API_GATEWAY_URL` (default `http://swizima-apigateway:9080`): Upstream gateway URL used by Nginx for `/api/` proxy requests.
- `MOBILE_APP_DOWNLOAD_URL` (default `https://drive.google.com/drive/folders/1cLdYhVLrO4Oyh1xLOhFIJIWCW-99WSg4?usp=sharing`): URL injected at container startup into `/env.js` for the "Download Mobile App" buttons.

### Running an API service locally (without Docker for APIs)
Start the database first (step 1 above). Each service can run independently via Maven:

```bash
cd swizima-api/clientapi
./mvnw spring-boot:run
```

---

## 📝 Changelog

All notable changes and new features are documented in [CHANGELOG.md](CHANGELOG.md).
