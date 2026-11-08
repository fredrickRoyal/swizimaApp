# Configurations API (`configurationsapi`)

Spring Boot service for managing Swizima system configurations and client billing plans.

- **Port:** `8082`
- **Base package:** `com.swizima.configurationsapi`
- **Tech stack:** Java 21, Spring Boot 4.1.2 (Web MVC, Data JPA, Validation), PostgreSQL (`swizima_configurations`), Maven
- **Swagger UI:** http://localhost:8082/swagger-ui.html

## Billing Models (`BillingModel`)

Each client (and the system default plan) can be configured with one of two billing models in `ClientBillingPlan`:

| Model | Description | Commission Rate | Monthly Amount |
|---|---|---|---|
| `COMMISSION` *(default)* | Per-transaction percentage deducted from the client's prepaid Swizima wallet on every car parked or washed. | `0% – 100%` (default `5%`) | `0` |
| `MONTHLY_FLAT` | Client pays an agreed fixed amount at the end of every month instead of per-transaction commission. | Forced to `0%` | `> 0` (e.g. `UGX 150,000`) |

## Endpoints (`/api/v1/configurations`)

- `GET /api/v1/configurations/billing-plans/default` — Get the system-wide default billing plan.
- `PUT /api/v1/configurations/billing-plans/default` — Update the system-wide default billing plan (`billingModel`, `commissionRatePercent`, `monthlyAmount`, `currency`, `allowNegativeBalance`, `minimumTopUpAmount`).
- `GET /api/v1/configurations/billing-plans` — List all custom client billing plans.
- `GET /api/v1/configurations/billing-plans/client/{clientId}` — Get the effective billing plan for a client (returns custom plan if present, otherwise falls back to the default plan).
- `PUT /api/v1/configurations/billing-plans/client/{clientId}` — Create or update a custom billing plan for a specific client (`COMMISSION` or `MONTHLY_FLAT`).
- `DELETE /api/v1/configurations/billing-plans/client/{clientId}` — Remove a client's custom plan (reverts client to the system default plan).

## Running locally

1. Install a JDK matching `java.version` in [`pom.xml`](pom.xml).
2. Create a PostgreSQL database and configure `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/swizima_configurations
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   spring.jpa.hibernate.ddl-auto=update
   ```
3. Start the service:
   ```bash
   ./mvnw spring-boot:run
   ```
