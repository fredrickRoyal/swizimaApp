# Swizima Monorepo Project

Welcome to the **Swizima** multi-platform automotive management suite.

## 🏗️ Project Structure & Recent Reorganization

The project has been reorganized into a unified workspace under the `swizima-app` directory with Git version control settings managed at the workspace root:

```text
swizima-app/
├── .git/                 # Git version control settings (repository root)
├── .gitignore            # Multi-project ignore configuration
├── swizima-mobile/       # Native Android Application (Jetpack Compose, Kotlin, Material 3)
├── swizima-api/          # Backend API services
└── swizima-web/          # Web frontend application
```

### Recent Changes & Reorganization
- **Workspace Reorganization:** Consolidated project structure into `swizima-app` containing mobile, web, and API subprojects.
- **Git Settings Relocation:** Moved the `.git` repository folder to `swizima-app/.git` so that all repository tracking and settings reside at the workspace level.
- **Enhanced `.gitignore`:** Reconfigured `.gitignore` to comprehensively ignore build outputs, caches (`.gradle`, `.idea`), local properties, node modules, and virtual environments across all subprojects (`swizima-mobile`, `swizima-api`, `swizima-web`).

---

## 📱 Subprojects

### 1. Swizima Mobile (`swizima-mobile`)
Native Android automotive management application built with Jetpack Compose and Material 3.
- **Features:** Authentication (Phone/Email with country codes and OTP), Automotive Dashboard (Car Park, Car Wash, Repair, Review), Service Records Management, PDF Receipt Generation, and WhatsApp Ticket Sender.
- **Tech Stack:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, Coroutines.

### 2. Swizima API (`swizima-api`)
Backend services and database integration for syncing vehicle records, user authentication, and notifications.

- **Tech Stack:** Java, Spring Boot 4.1.2 (Web MVC, Data JPA, Validation), PostgreSQL, Maven.
- **Architecture:** Independent Spring Boot services, each with its own Maven build, behind a single **API gateway**.

> **Clients (mobile/web) call only the gateway: `http://localhost:8080`.** Aggregated Swagger UI: `http://localhost:8080/swagger-ui.html`.

| Service | Port | Base package | Responsibility | Docs |
|---|---|---|---|---|
| `apigateway` | 8080 | `com.swizima.apigateway` | Single entry point: routing, CORS, aggregated Swagger, request IDs | [README](swizima-api/apigateway/README.md) |
| `clientapi` | 8081 | `com.swizima.clientapi` | Client (customer/organisation) management | [README](swizima-api/clientapi/README.md) |
| `configurationsapi` | 8082 | `com.swizima.configurationsapi` | System configurations and settings | [README](swizima-api/configurationsapi/README.md) |
| `paymentsapi` | 8083 | `com.swizima.paymentsapi` | Payments and transactions | [README](swizima-api/paymentsapi/README.md) |
| `serviceapi` | 8084 | `com.swizima.serviceapi` | Automotive services (car park, wash, repair) | [README](swizima-api/serviceapi/README.md) |
| `usersapi` | 8085 | `com.swizima.usersapi` | Users and authentication | [README](swizima-api/usersapi/README.md) |

All services share the same package layout (`config`, `controller`, `dto`, `entity`, `exception`, `repository`, `service`), audit columns via `ParentEntity`, and soft deletes. See each service's README for its data model, endpoints and setup.

#### API documentation (Swagger UI)

| Service | Swagger UI | OpenAPI JSON |
|---|---|---|
| **All services (via gateway)** | http://localhost:8080/swagger-ui.html | — (pick a service in the dropdown) |
| `usersapi` | http://localhost:8085/swagger-ui.html | http://localhost:8085/v3/api-docs |
| `clientapi` | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| `configurationsapi` | http://localhost:8082/swagger-ui.html *(not yet available)* | http://localhost:8082/v3/api-docs |
| `paymentsapi` | http://localhost:8083/swagger-ui.html *(not yet available)* | http://localhost:8083/v3/api-docs |
| `serviceapi` | http://localhost:8084/swagger-ui.html *(not yet available)* | http://localhost:8084/v3/api-docs |

> The service must be running for its link to work. `/swagger-ui.html` redirects to `/swagger-ui/index.html`.

### 3. Swizima Web (`swizima-web`)
- Administrative dashboard and web client for managing swizima automotive operations.

---

## 🚀 Getting Started

1. Clone or open the workspace at `swizima-app`.
2. Open `swizima-mobile` in Android Studio.
3. Configure API endpoints and dependencies as needed.

### Local infrastructure (Docker Compose)
[`swizima-api/docker-compose.yml`](swizima-api/docker-compose.yml) runs **PostgreSQL 18.6** on `localhost:5432`
(user/password `postgres`/`postgres`). On first start it creates one database per service:
`swizima_users`, `swizima_client`, `swizima_configurations`, `swizima_payments`, `swizima_service`.

```bash
cd swizima-api
cp .env.example .env        # optional - override credentials/port/data dir
docker compose up -d        # start
docker compose ps           # wait for "healthy"
docker compose down         # stop (data is kept)
```

Database files are stored on the host in **`/home/kasoma/swizima/Projects/Swizima/dbdata`**
(volume `swizima-postgres-data`, override with `POSTGRES_DATA_DIR`). The folder must exist before
`docker compose up`. Data survives `docker compose down -v`; to reset the database, stop the container
and empty that folder (`sudo rm -rf /home/kasoma/swizima/Projects/Swizima/dbdata/*`).

> The database init script only runs when the data folder is empty. To add a database later, either
> reset the data folder or run `CREATE DATABASE ...` manually.

### Running an API service
Start the database first (above). Each service runs independently. See the **Running locally** section in the service's README, e.g. [clientapi](swizima-api/clientapi/README.md#running-locally):

```bash
cd swizima-api/clientapi
./mvnw spring-boot:run
```

---

## 📝 Changelog

All notable changes and new features are documented in [CHANGELOG.md](CHANGELOG.md).
