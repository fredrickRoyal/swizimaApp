# Client API (`clientapi`)

Spring Boot service for managing Swizima clients (customers and organisations).

- **Base package:** `com.swizima.clientapi`
- **Tech stack:** Java 21, Spring Boot 4.1.2 (Web MVC, Data JPA, Validation), PostgreSQL, Lombok, springdoc-openapi (Swagger), Maven
- **Port:** `8081` · **Swagger UI:** http://localhost:8081/swagger-ui.html

## Package structure

```text
com.swizima.clientapi/
├── config/        # JpaAuditingConfig (auto-fills audit fields)
├── controller/    # ClientController
├── dto/           # ClientRequest, ClientResponse, ClientStatusRequest, PageResponse
├── entity/        # ParentEntity, Client
│   └── enums/     # ClientType, ClientStatus, RecordStatus
├── exception/     # ResourceNotFoundException, DuplicateResourceException, GlobalExceptionHandler
├── repository/    # ClientRepository
├── service/       # ClientService
│   └── impl/      # ClientServiceImpl
└── ClientapiApplication.java
```

## Data model

### `ParentEntity` (shared primary key and audit columns)
| Column | Description |
|---|---|
| `id` | UUID primary key (generated) — inherited by every entity |
| `created_by` | User who created the record (auto) |
| `created_at` | Creation timestamp (auto) |
| `updated_by` | User who last updated the record (auto) |
| `updated_at` | Last update timestamp (auto) |
| `record_status` | `ACTIVE`, `INACTIVE`, `DELETED` |

> The auditor is currently hard-coded to `system` in `JpaAuditingConfig` until authentication is added.

### `Client` (table `clients`)
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key (from `ParentEntity`) |
| `client_code` | varchar(50) | Required, unique |
| `client_name` | varchar(255) | Required |
| `trading_name` | varchar(255) | |
| `client_type` | enum | `INDIVIDUAL`, `COMPANY`, `GOVERNMENT`, `NGO`, `OTHER` |
| `industry_sector` | varchar(150) | |
| `description` | text | |
| `email` | varchar(150) | Validated email |
| `phone_number` | varchar(30) | |
| `alternative_phone` | varchar(30) | |
| `address` | varchar(255) | |
| `country` | varchar(100) | |
| `district` | varchar(100) | |
| `city` | varchar(100) | |
| `contact_person` | varchar(150) | |
| `contact_person_title` | varchar(100) | |
| `contact_person_email` | varchar(150) | Validated email |
| `contact_person_phone` | varchar(30) | |
| `status` | enum | `PENDING` (default), `ACTIVE`, `INACTIVE`, `SUSPENDED` |

## Endpoints

Base path: `/api/v1/clients`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/` | Create a client (`201 Created` + `Location` header) |
| `GET` | `/?search=&status=&page=&size=&sort=` | List clients (paginated; default `size=20`, `sort=createdAt,desc`) |
| `GET` | `/{id}` | Get client by ID |
| `GET` | `/code/{clientCode}` | Get client by code |
| `PUT` | `/{id}` | Update a client |
| `PATCH` | `/{id}/status` | Update client status |
| `DELETE` | `/{id}` | Soft delete (`record_status = DELETED`, `204 No Content`) |

`search` matches client name, code, trading name, email or phone number (case-insensitive). When `search` is provided, the `status` filter is ignored.

### Example: create a client
```bash
curl -X POST http://localhost:8080/api/v1/clients \
  -H "Content-Type: application/json" \
  -d '{
        "clientCode": "CL-0001",
        "clientName": "Acme Motors Ltd",
        "tradingName": "Acme Motors",
        "clientType": "COMPANY",
        "industrySector": "Automotive",
        "email": "info@acmemotors.com",
        "phoneNumber": "+256700000000",
        "country": "Uganda",
        "district": "Kampala",
        "city": "Kampala",
        "contactPerson": "Jane Doe",
        "contactPersonTitle": "Operations Manager",
        "contactPersonEmail": "jane@acmemotors.com",
        "contactPersonPhone": "+256700000001"
      }'
```

### Example: update status
```bash
curl -X PATCH http://localhost:8080/api/v1/clients/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "ACTIVE"}'
```

## Service Catalog

Services offered by a client (car park, car wash, repair, etc.) with pricing.

### `ServiceCatalog` (table `service_catalogs`)
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `client_id` | UUID | FK → `clients.id`, required |
| `service_code` | varchar(50) | Required, unique per client |
| `service_name` | varchar(255) | Required |
| `category` | enum | `CAR_PARK`, `CAR_WASH`, `REPAIR`, `MAINTENANCE`, `OTHER` |
| `description` | text | |
| `price` | numeric(15,2) | Required, ≥ 0 |
| `currency` | char(3) | ISO code, default `UGX` |
| `duration_minutes` | int | Optional, > 0 |
| `status` | enum | `ACTIVE` (default), `INACTIVE` |

Plus the shared `ParentEntity` audit columns.

### Endpoints

Base path: `/api/v1/service-catalogs`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/` | Create a catalog item |
| `GET` | `/?clientId=&category=&status=&search=&page=&size=&sort=` | List items (all filters optional; default `sort=serviceName,asc`) |
| `GET` | `/{id}` | Get item by ID |
| `PUT` | `/{id}` | Update an item |
| `PATCH` | `/{id}/status` | Update item status |
| `DELETE` | `/{id}` | Soft delete |

```bash
curl -X POST http://localhost:8081/api/v1/service-catalogs \
  -H "Content-Type: application/json" \
  -d '{
        "clientId": "<client-uuid>",
        "serviceCode": "WASH-FULL",
        "serviceName": "Full Car Wash",
        "category": "CAR_WASH",
        "price": 25000,
        "currency": "UGX",
        "durationMinutes": 45
      }'
```

## Error responses

Errors are returned as RFC 9457 `ProblemDetail` JSON:

| Status | When |
|---|---|
| `400 Bad Request` | Validation failed (field errors under `errors`) |
| `404 Not Found` | Client does not exist or is deleted |
| `409 Conflict` | Client code already exists |

## Running locally

1. Install a JDK matching `java.version` in [`pom.xml`](pom.xml).
2. Create a PostgreSQL database and configure `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/swizima_client
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   spring.jpa.hibernate.ddl-auto=update
   ```
3. Start the service:
   ```bash
   ./mvnw spring-boot:run
   ```
