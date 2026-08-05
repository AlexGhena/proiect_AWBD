# proiect_AWBD

Backend-focused banking coursework project built as three independent Spring Boot microservices and one Angular frontend.

> Status: this repository currently contains the initial Spring Boot skeletons. The documents and SQL files below define the target architecture to implement.

## Components

| Component | Responsibility | Owned entities |
|---|---|---|
| `userService` | Authentication, users, profiles, roles and addresses | `AppUser`, `UserProfile`, `Role`, `Address` |
| `bankingService` | Bank accounts, cards and saved beneficiaries | `BankAccount`, `BankCard`, `Beneficiary` |
| `transactionService` | Transfers, transaction classification and recurring transfers | `BankTransaction`, `TransactionCategory`, `ScheduledTransaction` |
| `frontend` | Angular login, CRUD pages, validation and pagination | No database |

Detailed documents:

- [Backend architecture](docs/BACKEND_ARCHITECTURE.md)
- [Database architecture](docs/DATABASE_ARCHITECTURE.md)
- [User database SQL](database/user-service-schema.sql)
- [Banking database SQL](database/banking-service-schema.sql)
- [Transaction database SQL](database/transaction-service-schema.sql)

## Backend architecture

```mermaid
flowchart LR
    Browser["Browser"] --> Angular["Angular frontend\nserved by NGINX"]
    Angular --> Ingress["Kubernetes Ingress\nTLS, routing, rate limit, request ID"]

    Ingress -->|"/api/auth, /api/users, /api/profiles, /api/roles, /api/addresses"| US["userService"]
    Ingress -->|"/api/accounts, /api/cards, /api/beneficiaries"| BS["bankingService"]
    Ingress -->|"/api/transactions, /api/categories, /api/scheduled-transactions"| TS["transactionService"]

    TS -->|"REST + service discovery + load balancing"| BS
    BS -->|"REST: validate user"| US

    US --> UDB[("user_db")]
    BS --> BDB[("banking_db")]
    TS --> TDB[("transaction_db")]

    Config["Central configuration\nSpring Cloud Config + K8s Secrets"] -.-> US
    Config -.-> BS
    Config -.-> TS

    US -.-> Obs["Actuator + Prometheus\nGrafana + Zipkin"]
    BS -.-> Obs
    TS -.-> Obs
```

Each service owns its database. A service never reads another service's tables directly.

## ER diagram — 10 interconnected entities

```mermaid
erDiagram
    APP_USER ||--|| USER_PROFILE : "has"
    APP_USER }o--o{ ROLE : "has"
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
    }
    USER_PROFILE {
        uuid id PK
        uuid user_id FK,UK
        varchar first_name
        varchar last_name
        varchar phone
    }
    ROLE {
        uuid id PK
        varchar name UK
    }
    ADDRESS {
        uuid id PK
        uuid profile_id FK
        varchar label
        varchar street
        varchar city
        varchar country
        boolean is_default
    }
    BANK_ACCOUNT {
        uuid id PK
        uuid user_id "logical reference"
        varchar iban UK
        varchar currency
        decimal balance
        varchar status
    }
    BANK_CARD {
        uuid id PK
        uuid account_id FK
        varchar card_reference UK
        varchar last_four
        varchar status
    }
    BENEFICIARY {
        uuid id PK
        uuid owner_account_id FK
        varchar beneficiary_name
        varchar beneficiary_iban
        varchar nickname
    }
    TRANSACTION_CATEGORY {
        uuid id PK
        varchar name UK
        varchar description
    }
    SCHEDULED_TRANSACTION {
        uuid id PK
        uuid category_id FK
        uuid source_account_id "logical reference"
        uuid destination_account_id "logical reference"
        decimal amount
        varchar frequency
        date next_execution_date
        varchar status
    }
    BANK_TRANSACTION {
        uuid id PK
        uuid category_id FK
        uuid scheduled_transaction_id FK
        uuid source_account_id "logical reference"
        uuid destination_account_id "logical reference"
        decimal amount
        varchar status
    }
```

Physical foreign keys exist only inside a service boundary. `BANK_ACCOUNT.user_id`, `BANK_TRANSACTION.source_account_id`/`destination_account_id`, and `SCHEDULED_TRANSACTION.source_account_id`/`destination_account_id` are logical references checked through REST APIs.

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
| Central config | Spring Cloud Config deployed in Kubernetes; sensitive values come from Kubernetes Secrets; `/actuator/refresh` demo |
| Discovery and REST | Kubernetes service names + Spring Cloud Kubernetes Discovery; load-balanced `RestTemplate` calls |
| Scaling | Two replicas for each business service; pod name returned in a diagnostic response header for demonstration |
| API Gateway | Kubernetes Ingress for central routing, rate limiting, request ID and security headers |
| Monitoring | Actuator health/metrics, Prometheus scraping, Grafana dashboard and Zipkin tracing |
| Resilience | Resilience4j circuit breaker, retry and fallback on transaction→banking and banking→user calls |
| Pattern | Saga orchestration for transfers with a compensating credit when a later step fails |
| CI/CD | GitHub Actions: test, package, Docker build/push, deploy to a staging namespace |
| AI agents | Optional backlog only; implement after the graded backend and DevOps requirements are complete |

## Recommended implementation order

1. Implement the ten entities, Flyway migrations and CRUD endpoints.
2. Add validation, exceptions, profiles, tests, pagination, sorting, logging and security.
3. Build the small Angular CRUD frontend.
4. Add Dockerfiles, Kubernetes manifests, Ingress, configuration and two replicas.
5. Add resilience, Saga behavior, monitoring/tracing and CI/CD.
6. Add an AI feature only if time remains.

