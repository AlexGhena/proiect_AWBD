# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Backend-focused banking coursework project: three independent Spring Boot 4.1 microservices (Java 25) plus a planned Angular frontend (not yet scaffolded — see `docs/BACKEND_ARCHITECTURE.md` §7 for its intended structure). `README.md` and `docs/` define the target architecture; not every item in the grading map there is implemented yet (see "Known gaps" below).

| Service | Port | Owns | Responsibility |
|---|---|---|---|
| `userService` | 8081 | `AppUser`, `UserProfile`, `Role`, `Address` | Auth (JDBC + JWT issuance), users, profiles, roles, addresses |
| `bankingService` | 8082 | `BankAccount`, `BankCard`, `Beneficiary` | Accounts, cards, beneficiaries; validates JWTs issued by userService |
| `transactionService` | 8083 | `BankTransaction`, `TransactionCategory`, `ScheduledTransaction` | Transactions, categories, scheduled transfers; Saga orchestrator for transfers |

No API gateway/Ingress/docker-compose exists yet — during local dev, callers (including a future frontend) hit each service's port directly.

## Commands

Each service is a standalone Maven project (own `mvnw`) — always run Maven from inside the service directory, e.g. `cd userService`.

```bash
# build / run (per service, from inside userService/ bankingService/ transactionService/)
./mvnw clean install
./mvnw spring-boot:run

# tests
./mvnw test                                    # full suite, `test` Spring profile + H2
./mvnw test -Dtest=AuthenticationFlowTests      # single test class
./mvnw test -Dtest=AuthenticationFlowTests#loginSucceeds   # single test method
```

On Windows use `mvnw.cmd` instead of `./mvnw` outside of the Bash tool.

Postgres is required for the `dev` profile (default active profile in every service): single instance, database `awbd`, credentials default to `postgres`/`admin` (override via `DB_USERNAME`/`DB_PASSWORD`). Each service owns a separate **schema** in that one database, not a separate database: `users`, `banking`, `transactions` respectively (see each service's `application-dev.yml`). Flyway (`db/migration/V*.sql` per service) manages schema and is scoped with `flyway.schemas`/`default-schema`; `hibernate.ddl-auto` is `validate`, so Hibernate never generates DDL — new columns/tables require a new Flyway migration.

Tests run against the `test` Spring profile with H2 (`src/test/resources/application-test.yml`), so they don't need Postgres running.

There is no root/aggregator build — the three services and (eventually) the frontend are built independently, matching the CI plan in `docs/BACKEND_ARCHITECTURE.md` §13 (Maven test matrix + separate Angular job).

## Architecture

### Hexagonal layering (identical package shape in all three services)

```
<service>.demo
├── adapter/in/web        REST controllers, request/response DTOs, web-layer mappers
├── adapter/out/persistence   JPA entities, repositories, entity<->domain mappers
├── adapter/out/{client,security,idempotency}  outbound HTTP clients, JWT decoding, etc.
├── application/service   use-case implementations (business logic, transaction boundaries)
├── domain/model           persistence-agnostic domain model
├── domain/port/in         use-case interfaces implemented by application/service
├── domain/port/out        repository/client interfaces implemented by adapter/out
├── domain/exception        domain exceptions mapped to RFC 7807 by GlobalExceptionHandler
├── security                JWT filter/decoder wiring
└── config                  SecurityConfig, CorsProperties, pagination, RestClient config, etc.
```

Controllers never accept or return JPA entities — only DTOs under `adapter/in/web/dto`, translated by a mapper. `adapter/in/web/GlobalExceptionHandler` (`@RestControllerAdvice`) turns domain exceptions into RFC 7807 `ProblemDetail`: 400 validation, 404 not found, 409 duplicate/conflict, 401/403 auth, 500 fallback.

### Cross-service references are logical, not FK

Services never share a database connection or FK across schemas. E.g. `bank_accounts.user_id` in bankingService is a logical reference validated by calling userService over REST, not a foreign key — same pattern for account IDs referenced from transactionService. Keep this in mind when touching any "owner"/"account" field: validation happens via an HTTP call (see `adapter/out/client` / `security/UserServiceClient`-style classes), not a DB join.

### Auth and inter-service JWT

- `userService` is the sole authentication authority: JDBC-backed `UserDetailsService` against the `users` schema (BCrypt), `POST /api/auth/login` issues a short-lived RSA-signed JWT (`AuthController`, `AccessTokenIssuer`). Remember-me uses `PersistentTokenBasedRememberMeServices` (`persistent_logins` table, added by a later Flyway migration, not a JPA entity) — a valid remember-me cookie lets the client mint a new access token from `POST /api/auth/token` without re-entering credentials. Dev-mode RSA keys are generated and cached under `userService/.keys/` when `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` are unset.
- `bankingService` and `transactionService` never see the private key — they validate JWTs as OAuth2 resource servers against userService's public JWKS endpoint (`GET /api/auth/jwks.json`), configured via `app.security.jwk-set-uri` (defaults to `http://localhost:8081/api/auth/jwks.json`).
- Issuer/audience must match across all three services' config (`JWT_ISSUER`, `JWT_AUDIENCE`) or every token is rejected — see the inline warning comments in each `application.yml`.
- `transactionService → bankingService` calls forward the caller's Bearer token via a `ClientHttpRequestInterceptor` (see `BearerTokenPropagationInterceptorTests` in both services) so bankingService independently re-validates it rather than trusting the upstream call.
- CSRF stays enabled for browser-facing endpoints (`CookieCsrfTokenRepository`); Angular is expected to call `GET /api/auth/csrf` first and echo the token back as `X-XSRF-TOKEN`. `/internal/**` endpoints (service-to-service only, e.g. debit/credit/compensate) are excluded from CSRF and must never be routed by a public gateway.
- CORS is allow-listed per service via `app.security.cors*` config, defaulting to `http://localhost:4200`, override with `CORS_ALLOWED_ORIGINS`.
- Full design intent (roles matrix, required security tests, RSA claim list) is written up in `agent/SPRING_SECURITY.md` — read it before touching auth/JWT/CSRF code, since it's the spec the current implementation follows.

### Transfer Saga (transactionService orchestrates, bankingService executes)

`POST /api/transactions/transfers` drives a synchronous, REST-based Saga (no message broker): create transaction `PENDING` → validate both accounts in bankingService → debit source (with an `Idempotency-Key` header) → credit destination (same key/Saga ID) → mark `COMPLETED`, or on credit failure, compensate the source debit and mark `FAILED`. `bankingService`'s `/internal/accounts/{id}/{debit,credit,compensate}` endpoints back this; idempotency keys prevent double-processing on retry. `transactionService → bankingService` calls are wrapped in Resilience4j retry (3 attempts, exponential backoff, business exceptions like `BankingBusinessException` are never retried) and a circuit breaker — both configured under `resilience4j.*` in `transactionService/application.yml`. See `TransferSagaTests` and `InternalAccountOperationsTests` for the expected behavior contract.

### Pagination

`Pageable` is wired with an explicit sort-field allowlist for the three priority list endpoints — `UserController` (`username`/`email`/`createdAt`), `AccountController` (`iban`/`balance`/`createdAt`), `TransactionController` (`amount`/`status`/`createdAt`) — default size 20, max 100 (`app.pagination.*`), unknown sort fields rejected with 400. Other list endpoints (roles, profiles, addresses, cards, beneficiaries, categories, scheduled transactions) use plain `Pageable` without that allowlist.

### Soft delete

`AppUser` delete is a soft delete (flag + Flyway migration `V4__soft_delete_users.sql`), not a row removal — deleted users are excluded from default `GET /api/users` listing but retrievable via the admin-only `GET /api/users/deleted`. Don't assume delete removes the row when working in userService.

## Known gaps vs. `docs/BACKEND_ARCHITECTURE.md`'s grading map

Not yet implemented: Spring Cloud Config, Spring Cloud Kubernetes service discovery (inter-service URLs are plain config-driven `RestClient` base URLs, not `@LoadBalanced`/discovered), Actuator (referenced by `SecurityConfig` request matchers in every service but `spring-boot-starter-actuator` isn't a dependency yet, so those matchers are currently dead), Micrometer/Prometheus/Zipkin, Kubernetes manifests/Ingress, Dockerfiles, CI (GitHub Actions), JaCoCo coverage enforcement, and the Angular frontend itself. Resilience4j *is* implemented, but only for the transactionService→bankingService leg — the bankingService→userService leg (used to validate account ownership) doesn't have the same retry/circuit-breaker treatment yet.
