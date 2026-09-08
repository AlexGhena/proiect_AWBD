# proiect_AWBD

Backend-focused banking coursework project built as three independent Spring Boot 4.1 microservices (Java 25) and one Angular frontend.

> Status: the backend is implemented — the three services expose full CRUD, JWT-based security, Flyway-managed schemas, the transfer Saga and Resilience4j.

## Components

| Component | Port | Responsibility | Owned entities |
|---|---|---|---|
| `userService` | 8081 | Authentication (JDBC + JWT issuance), users, profiles, roles, addresses | `AppUser`, `UserProfile`, `Role`, `Address` |
| `bankingService` | 8082 | Bank accounts, cards and saved beneficiaries; validates userService JWTs | `BankAccount`, `BankCard`, `Beneficiary` |
| `transactionService` | 8083 | Transfers (Saga), transaction classification and recurring transfers | `BankTransaction`, `TransactionCategory`, `ScheduledTransaction` |
| `frontend` | 4200 | Angular login, CRUD pages, validation and pagination | No database |

The three services share a single PostgreSQL database (`awbd`), each owning a **separate schema** — `users`, `banking`, `transactions` — never crossing schema boundaries with a FK or a shared connection.

Detailed documents:

- [Backend architecture](docs/BACKEND_ARCHITECTURE.md)
- [Database architecture](docs/DATABASE_ARCHITECTURE.md)
- [User database SQL](database/user-service-schema.sql)
- [Banking database SQL](database/banking-service-schema.sql)
- [Transaction database SQL](database/transaction-service-schema.sql)

## Backend architecture

Current, implemented topology. Callers hit each service's port directly (no gateway/Ingress yet); every service is a stateless resource server that validates the RSA-signed JWT issued by `userService`.

```mermaid
flowchart LR
    Browser["Browser"] --> Angular["Angular frontend<br/>(localhost:4200)"]

    Angular -->|"/api/auth, /api/users,<br/>/api/profiles, /api/roles,<br/>/api/addresses"| US["userService<br/>:8081"]
    Angular -->|"/api/accounts, /api/cards,<br/>/api/beneficiaries"| BS["bankingService<br/>:8082"]
    Angular -->|"/api/transactions, /api/categories,<br/>/api/scheduled-transactions"| TS["transactionService<br/>:8083"]

    US -->|"issues RSA-signed JWT<br/>+ exposes JWKS"| JWKS(["GET /api/auth/jwks.json"])
    BS -. "validate JWT (JWKS)" .-> JWKS
    TS -. "validate JWT (JWKS)" .-> JWKS

    TS ==>|"Saga: debit/credit/compensate<br/>RestClient + Bearer forward<br/>Resilience4j retry + CB"| BS
    BS -->|"REST: validate user_id<br/>(logical reference)"| US

    US --> UDB[("schema: users")]
    BS --> BDB[("schema: banking")]
    TS --> TDB[("schema: transactions")]
    UDB & BDB & TDB --- PG[("PostgreSQL: awbd<br/>Flyway per schema")]
```

Each service owns its schema and never reads another service's tables directly; cross-service links (`user_id`, account IDs) are logical references checked over REST. `userService` is the sole authentication authority — `bankingService` and `transactionService` validate tokens against its public JWKS endpoint. The `transactionService → bankingService` transfer leg forwards the caller's Bearer token and is wrapped in Resilience4j retry + circuit breaker.

### Hexagonal layering (identical in all three services)

```mermaid
flowchart TB
    subgraph in["adapter/in/web"]
        C["REST controllers + DTOs<br/>GlobalExceptionHandler → RFC 7807"]
    end
    subgraph app["application/service"]
        S["use-case implementations<br/>(business logic, @Transactional)"]
    end
    subgraph domain["domain"]
        PIN["port/in (use-case interfaces)"]
        M["model + exceptions"]
        POUT["port/out (repo/client interfaces)"]
    end
    subgraph out["adapter/out"]
        P["persistence: JPA entities + repositories"]
        CL["client / security / idempotency"]
    end

    C --> PIN
    PIN --- S
    S --> POUT
    S --> M
    POUT --- P
    POUT --- CL
```

Controllers never accept or return JPA entities — only DTOs, translated by a mapper. Domain exceptions become RFC 7807 `ProblemDetail` responses (400 validation, 404 not found, 409 conflict, 401/403 auth, 500 fallback).

## Conceptual diagram — bounded contexts

High-level domain view: three bounded contexts, each around its aggregate roots, connected only by logical references over REST.

```mermaid
flowchart TB
    subgraph IAM["User / Identity context — userService"]
        direction TB
        AU["AppUser<br/><i>aggregate root</i>"]
        UP["UserProfile"]
        AD["Address"]
        RO["Role"]
        AU --- UP
        UP --- AD
        AU --- RO
    end

    subgraph BANK["Banking context — bankingService"]
        direction TB
        BA["BankAccount<br/><i>aggregate root</i>"]
        BC["BankCard"]
        BE["Beneficiary"]
        BA --- BC
        BA --- BE
    end

    subgraph TX["Transaction context — transactionService"]
        direction TB
        BT["BankTransaction<br/><i>aggregate root</i>"]
        TC["TransactionCategory"]
        ST["ScheduledTransaction"]
        TC --- BT
        TC --- ST
        ST --- BT
    end

    AU -. "user_id (REST)" .-> BA
    BA -. "account IDs (REST, via Saga)" .-> BT
    BA -. "account IDs (REST)" .-> ST
```

## ER diagram — 10 interconnected entities

Fields below match the JPA entities and Flyway migrations. `//` marks the schema each entity lives in.

```mermaid
erDiagram
    APP_USER ||--|| USER_PROFILE : "has (1:1)"
    APP_USER ||--o{ USER_ROLE : ""
    ROLE ||--o{ USER_ROLE : ""
    USER_PROFILE ||--o{ ADDRESS : "has"
    APP_USER ||--o{ BANK_ACCOUNT : "owns (logical user_id)"
    BANK_ACCOUNT ||--o{ BANK_CARD : "contains"
    BANK_ACCOUNT ||--o{ BENEFICIARY : "saves"
    BANK_ACCOUNT ||--o{ BANK_TRANSACTION : "participates (logical account IDs)"
    TRANSACTION_CATEGORY ||--o{ BANK_TRANSACTION : "classifies"
    TRANSACTION_CATEGORY ||--o{ SCHEDULED_TRANSACTION : "classifies"
    SCHEDULED_TRANSACTION ||--o{ BANK_TRANSACTION : "generates (executions)"

    APP_USER {
        uuid id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        boolean enabled
        varchar approval_status "PENDING|APPROVED|REJECTED"
        timestamptz created_at
        timestamptz updated_at
        timestamptz deleted_at "soft delete"
    }
    USER_PROFILE {
        uuid id PK
        uuid user_id FK,UK
        varchar first_name
        varchar last_name
        varchar phone
        timestamptz created_at
        timestamptz updated_at
    }
    ROLE {
        uuid id PK
        varchar name UK
        varchar description
    }
    USER_ROLE {
        uuid user_id PK,FK
        uuid role_id PK,FK
    }
    ADDRESS {
        uuid id PK
        uuid profile_id FK
        varchar label "HOME|BILLING|WORK|OTHER"
        varchar street
        varchar city
        varchar postal_code
        varchar country "ISO-2"
        boolean is_default
        timestamptz created_at
        timestamptz updated_at
    }
    BANK_ACCOUNT {
        uuid id PK
        uuid user_id "logical reference"
        varchar iban UK
        varchar currency "ISO-4217"
        decimal balance
        varchar status "ACTIVE|BLOCKED|CLOSED"
        bigint version "optimistic lock"
        timestamptz created_at
        timestamptz updated_at
    }
    BANK_CARD {
        uuid id PK
        uuid account_id FK
        varchar card_reference UK
        varchar last_four
        varchar cardholder_name
        smallint expiry_month
        smallint expiry_year
        varchar status "ACTIVE|BLOCKED|EXPIRED|LOST_STOLEN"
        varchar card_number_encrypted "AES-256-GCM"
        varchar cvv_encrypted "AES-256-GCM"
        varchar pin_encrypted "AES-256-GCM"
        timestamptz created_at
        timestamptz updated_at
    }
    BENEFICIARY {
        uuid id PK
        uuid owner_account_id FK
        varchar beneficiary_name
        varchar beneficiary_iban
        varchar nickname
        timestamptz created_at
        timestamptz updated_at
    }
    TRANSACTION_CATEGORY {
        uuid id PK
        varchar name UK
        varchar description
        timestamptz created_at
        timestamptz updated_at
    }
    SCHEDULED_TRANSACTION {
        uuid id PK
        uuid category_id FK "nullable"
        uuid source_account_id "logical reference"
        uuid destination_account_id "logical reference"
        decimal amount
        varchar currency
        varchar frequency "DAILY|WEEKLY|MONTHLY"
        date next_execution_date
        varchar status "ACTIVE|PAUSED|CANCELLED"
        varchar description
        timestamptz created_at
        timestamptz updated_at
    }
    BANK_TRANSACTION {
        uuid id PK
        uuid category_id FK "nullable"
        uuid scheduled_transaction_id FK "nullable"
        uuid source_account_id "logical reference"
        uuid destination_account_id "logical reference"
        uuid saga_id UK "idempotency"
        decimal amount
        varchar currency
        varchar type "TRANSFER|DEPOSIT|WITHDRAWAL"
        varchar status "PENDING|COMPLETED|FAILED|COMPENSATED"
        varchar description
        varchar failure_reason
        timestamptz created_at
        timestamptz updated_at
    }
```

- **`users` schema:** `APP_USER`, `USER_PROFILE`, `ROLE`, `USER_ROLE` (join table for the `AppUser` ↔ `Role` many-to-many), `ADDRESS`. A `persistent_logins` table (remember-me) also lives here but is managed directly by Spring Security, not a JPA entity.
- **`banking` schema:** `BANK_ACCOUNT`, `BANK_CARD`, `BENEFICIARY`.
- **`transactions` schema:** `BANK_TRANSACTION`, `TRANSACTION_CATEGORY`, `SCHEDULED_TRANSACTION`.

Physical foreign keys exist only inside a single schema. `BANK_ACCOUNT.user_id`, `BANK_TRANSACTION.source_account_id`/`destination_account_id`, and `SCHEDULED_TRANSACTION.source_account_id`/`destination_account_id` are logical references validated through REST APIs, not database foreign keys.

### Required JPA relationship examples

| Requirement | Implementation |
|---|---|
| `@OneToOne` | `AppUser` ↔ `UserProfile` |
| `@OneToMany / @ManyToOne` | `BankAccount` ↔ `BankCard`, `BankAccount` ↔ `Beneficiary`, `UserProfile` ↔ `Address`, `TransactionCategory` ↔ `BankTransaction`, `TransactionCategory` ↔ `ScheduledTransaction`, `ScheduledTransaction` ↔ `BankTransaction` |
| `@ManyToMany` | `AppUser` ↔ `Role`, using `user_roles` |

## Grading implementation map

| Requirement | Minimal implementation that demonstrates it |
|---|---|
| Complete CRUD | Controller → service → Spring Data JPA repository for all ten entities; DTOs prevent exposing JPA entities |
| Exceptions | RFC 7807 `ProblemDetail`: 400 validation, 404 read/update/delete missing, 409 duplicate or unsafe delete, 500 fallback |
| Multi-environment | `application-dev.yml` with PostgreSQL and `application-test.yml` with H2 for every service |
| Testing | JUnit 5 + Mockito service tests (70%+ service coverage), plus one integration scenario per microservice |
| Angular views | Login plus list/create/edit/details/delete pages; reactive forms and custom 404/500 routes |
| Validation | Bean Validation in request DTOs and matching Angular validators/messages |
| Logging | SLF4J and `logback-spring.xml`; normal and error log files; correlation ID propagated between services |
| Pagination | Users, accounts and transactions; default 20, maximum 100, two or more allowed sort fields each |
| Security | JDBC-backed authentication, BCrypt, `USER`/`ADMIN`, JWT propagation, CSRF cookie/header, logout and remember-me option |
| Resilience | Resilience4j circuit breaker, retry and fallback on the transaction→banking transfer leg |
| Pattern | Saga orchestration for transfers with a compensating credit when a later step fails |

