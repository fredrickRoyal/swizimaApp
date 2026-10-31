# apigateway

Single entry point for all Swizima APIs. Built on **Spring Cloud Gateway Server Web MVC**
(Spring Boot 4.1, Spring Cloud 2025.1, Java 21).

Clients (web / mobile) should call **only the gateway** – `http://localhost:8080` locally.

## Routes

| Path                                              | Service             | Default URL             | Env var                  |
|---------------------------------------------------|---------------------|-------------------------|--------------------------|
| `/api/v1/clients/**`, `/api/v1/service-catalogs/**` | clientapi           | `http://localhost:8081` | `CLIENT_API_URL`         |
| `/api/v1/configurations/**`                       | configurationsapi   | `http://localhost:8082` | `CONFIGURATIONS_API_URL` |
| `/api/v1/payments/**`                             | paymentsapi         | `http://localhost:8083` | `PAYMENTS_API_URL`       |
| `/api/v1/services/**`                             | serviceapi          | `http://localhost:8084` | `SERVICE_API_URL`        |
| `/api/v1/users/**`, `/api/v1/auth/**`             | usersapi            | `http://localhost:8085` | `USERS_API_URL`          |
| `/docs/<service>/**`                              | that service's docs | (same as above)         |                          |

Paths are forwarded unchanged (e.g. `GET :8080/api/v1/clients` → `GET :8081/api/v1/clients`).
`/docs/<service>/v3/api-docs` is rewritten to `<service>/v3/api-docs` (`StripPrefix=2`).

> When adding a new controller to a service, make sure its base path is covered by a
> route predicate in `application.yml`, otherwise the gateway returns 404.

## Features

- **Aggregated Swagger UI** – `http://localhost:8080/swagger-ui.html` (also `/`, `/docs`, `/swagger` redirect there).
  Pick a service from the **"Select a definition"** dropdown. Each service publishes a relative server URL (`/`),
  so **"Try it out" calls go through the gateway** (port 8080). Set `SWAGGER_UI_ENABLED=false` to hide it in production.
- **CORS** handled centrally – `CORS_ALLOWED_ORIGINS` (comma-separated, default `http://localhost:4200,http://localhost:3000,http://localhost:5173`; `4200` is the Angular web portal). Requests from an unlisted origin get `403 Invalid CORS request`.
- **Request correlation** – every request gets an `X-Request-Id` (kept if supplied by the client),
  forwarded downstream, returned in the response and written to the access log.
- **Timeouts** – `GATEWAY_CONNECT_TIMEOUT` (default `5s`), `GATEWAY_READ_TIMEOUT` (default `30s`).
- **Actuator** – `/actuator/health`, `/actuator/info`.
- **Downstream unavailable** – if a service is down or times out, the gateway returns
  `503` with an RFC 7807 `application/problem+json` body (same format as the services).

## Configuration

| Env var        | Default | Description  |
|----------------|---------|--------------|
| `SERVER_PORT`  | `8080`  | Gateway port |

See [`application.yml`](src/main/resources/application.yml) for everything else.

## Run

```bash
cd swizima-api/apigateway
./mvnw spring-boot:run
```

Start the downstream services too; a route to a service that is not running returns `5xx`.

## Planned

- JWT validation at the gateway (once usersapi issues tokens after OTP verification).
- Rate limiting on `/api/v1/auth/**`.
