# Users API (`usersapi`)

Spring Boot service for managing Swizima users and authentication.

- **Base package:** `com.swizima.usersapi`
- **Tech stack:** Java, Spring Boot 4.1.2 (Web MVC, Data JPA), PostgreSQL, Maven
- **Status:** 🚧 Scaffolded — entities and endpoints not yet implemented

## Package structure

```text
com.swizima.usersapi/
├── config/        # Spring configuration (e.g. JPA auditing)
├── controller/    # REST controllers
├── dto/           # Request/response objects
├── entity/        # JPA entities (extend ParentEntity for audit columns)
├── exception/     # Custom exceptions & global handler
├── repository/    # Spring Data JPA repositories
├── service/       # Service interfaces (+ impl/)
└── UsersapiApplication.java
```

## Conventions

Follow the same patterns as [`clientapi`](../clientapi/README.md):

- Entities extend a `ParentEntity` with `created_by`, `created_at`, `updated_by`, `updated_at`, `record_status`.
- Deletes are soft deletes (`record_status = DELETED`).
- Endpoints are versioned under `/api/v1/...`.
- Errors are returned as RFC 9457 `ProblemDetail` responses.

## Endpoints

_To be documented as they are implemented._

## Running locally

1. Install a JDK matching `java.version` in [`pom.xml`](pom.xml).
2. Create a PostgreSQL database and configure `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/swizima_users
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   spring.jpa.hibernate.ddl-auto=update
   ```
3. Start the service:
   ```bash
   ./mvnw spring-boot:run
   ```
