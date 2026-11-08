# Payments API (`paymentsapi`)

Spring Boot service for handling Swizima client prepaid wallets, top-ups, and billing ledger entries.

- **Port:** `8083`
- **Base package:** `com.swizima.paymentsapi`
- **Tech stack:** Java 21, Spring Boot 4.1.2 (Web MVC, Data JPA, Validation), PostgreSQL (`swizima_payments`), Maven
- **Swagger UI:** http://localhost:8083/swagger-ui.html

## Billing & Wallet Behaviour

`paymentsapi` fetches each client's effective `BillingPlan` from `configurationsapi` (`/api/v1/configurations/billing-plans/client/{clientId}`):

- **`COMMISSION` model:** Calculates `commission = transactionAmount × (commissionRatePercent / 100)` and deducts it from the client's prepaid wallet balance. If the wallet has insufficient funds and `allowNegativeBalance` is `false`, the transaction check/charge is rejected.
- **`MONTHLY_FLAT` model:** The client pays an agreed fixed amount (`monthlyAmount`) at the end of every month. Per-transaction commission is `0`, `BillingEligibilityResponse` always permits the transaction regardless of wallet balance, and `chargeCommission` skips wallet deduction when the calculated commission is `0`.

## Endpoints (`/api/v1/payments`)

- `GET /api/v1/payments/wallets` — List all client billing wallets enriched with their active `billingModel`, `commissionRatePercent`, and `monthlyAmount`.
- `GET /api/v1/payments/wallets/client/{clientId}` — Get or initialize a client's billing wallet.
- `GET /api/v1/payments/wallets/client/{clientId}/eligibility?transactionAmount=...` — Check whether a client can process a transaction of a given amount under their active billing model.
- `POST /api/v1/payments/wallets/client/{clientId}/top-up` — Credit a client's wallet (Mobile Money, Bank, Cash, etc.).
- `POST /api/v1/payments/wallets/client/{clientId}/charge-commission` — Deduct per-transaction commission from a client's wallet (idempotent by `serviceTransactionId`).
- `GET /api/v1/payments/ledger` — Paginated wallet ledger entries (`TOP_UP`, `COMMISSION_CHARGE`, `ADJUSTMENT`) with optional `clientId` and `entryType` filters.

## Running locally

1. Install a JDK matching `java.version` in [`pom.xml`](pom.xml).
2. Create a PostgreSQL database and configure `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/swizima_payments
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   spring.jpa.hibernate.ddl-auto=update
   ```
3. Start the service:
   ```bash
   ./mvnw spring-boot:run
   ```
