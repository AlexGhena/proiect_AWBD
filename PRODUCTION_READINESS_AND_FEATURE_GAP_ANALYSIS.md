# proiect_AWBD - Production Readiness and Banking Feature Gap Analysis

**Assessment date:** 15 August 2026  
**Scope:** `userService`, `bankingService`, `transactionService`, and `frontend`  
**Assessment type:** source-code, configuration, schema, test, and build review

> This is an engineering assessment, not a legal, regulatory, PCI, or security certification. A real Romanian/EU bank launch would also require review by the institution's security, risk, compliance, legal, privacy, fraud, and operations teams.

## Executive conclusion

**The application is not ready for production or real money.** It is a useful coursework/demo prototype with several good foundations, but it contains release-blocking authorization and financial-integrity defects and has no production deployment or operational platform.

Recommended classification:

- **Coursework/demo readiness:** good, after documenting local setup and known limitations.
- **Internal non-production pilot:** not yet; first fix the P0 security and money-integrity issues below.
- **Public production banking:** not ready.

The most serious blockers are:

1. Any normal authenticated JWT can call the banking service's `/internal/accounts/**` endpoints if that service is reachable, including debit, credit, compensation, and account provisioning.
2. A customer can use the public account API to create an account for themselves with a caller-supplied opening balance and IBAN.
3. A customer can create transaction-history records and change the status of transactions they can access, so the transaction store is not an immutable source of truth.
4. There is no immutable double-entry ledger, reconciliation process, available/pending balance model, or durable workflow recovery suitable for real money.
5. Strong customer authentication/MFA and transaction signing are absent.
6. Main Flyway migrations seed known users, passwords, an administrator, balances, cards, and transactions; the default runtime profile is `dev`, with several insecure fallback secrets.
7. Docker/Kubernetes/Ingress/TLS, CI/CD, Actuator health, metrics, tracing, centralized logs, alerts, backups, and disaster recovery are absent.
8. KYC/customer identity verification, AML/sanctions/PEP screening, fraud controls, payment limits, and regulatory audit capabilities are absent.

## What is currently implemented

### `userService`

- Registration followed by administrator approval/rejection.
- Username/password login with BCrypt, short-lived RSA-signed JWTs, remember-me cookies, logout, and JWKS publication.
- CSRF protection, explicit CORS origins, login-attempt throttling, role checks, and ownership checks.
- User, profile, address, and role CRUD with pagination and RFC 7807-style errors.
- Soft deletion of users.
- Approval calls `bankingService` to provision an account.

### `bankingService`

- Accounts, cards, and beneficiaries with Flyway-managed PostgreSQL schemas.
- Per-user ownership checks for public resources.
- Account pagination and "my accounts" view.
- Card issue, block/unblock, report lost/stolen, reveal details/PIN, and change PIN.
- AES-GCM field encryption for PAN/CVV/PIN.
- Optimistic version column and idempotency records for internal debit/credit/compensation operations.

### `transactionService`

- Transaction/category/scheduled-transaction persistence and CRUD.
- Customer transaction history based on owned account IDs.
- A synchronous transfer Saga: validate, debit, credit, and compensate debit when credit fails.
- Idempotent calls from the Saga to `bankingService`.
- Retry, circuit breaker, connect timeout, and read timeout on the transaction-to-banking call path.

### `frontend`

- Angular login, registration, session restoration, guards, and HTTP interceptors.
- Access token kept in memory rather than browser persistent storage.
- Dashboard, account list, card controls, transfer form with confirmation, transaction history, pagination, and admin user/role screens.
- Custom 404 and 500 screens.

These are valuable foundations, but passing tests and having CRUD screens do not make a financial system production-safe.

## Verification performed

The following commands were run during this assessment:

| Component | Result |
|---|---:|
| `userService` tests | 32 passed; 0 failed/skipped |
| `bankingService` tests | 47 passed; 0 failed/skipped |
| `transactionService` tests | 23 passed; 0 failed/skipped |
| Angular unit tests | 2 passed |
| Angular production build | passed |

The Angular build reported one warning: `transactions.scss` is 4.43 kB, exceeding the configured 4 kB component-style warning budget by 432 bytes.

Important test limitations:

- Backend tests depend on a locally running PostgreSQL database and fixed test schemas; they are not isolated/portable H2 or Testcontainers tests as the root README suggests.
- There is no JaCoCo coverage report or enforced coverage threshold.
- There are no full end-to-end tests across all three live services and the frontend.
- There are no load, concurrency, soak, failover, chaos, penetration, dependency-vulnerability, accessibility, or browser E2E suites.
- Two frontend unit tests are far too little for the implemented authentication, administration, card, and transfer flows.
- The tests do not cover the most dangerous authorization cases identified in this report.

## Production readiness findings

### P0 - release blockers

#### 1. Internal money APIs do not authenticate a service identity

Evidence:

- `bankingService/.../SecurityConfig.java` requires only `.anyRequest().authenticated()`.
- CSRF is explicitly ignored for `/internal/**`.
- `InternalAccountController` exposes provision, snapshot, debit, credit, and compensate operations.
- Inter-service clients forward the end user's bearer token; there is no service credential, audience/scope separation, mTLS identity, or network policy in the repository.

Impact: a normal authenticated customer token is sufficient for these endpoints. Hiding them from a future Ingress is helpful but is not an authorization boundary. Direct service access, SSRF, lateral movement, a routing mistake, or an internal attacker could change balances.

Required fix:

- Give services distinct machine identities and scopes/audiences, for example mTLS plus OAuth2 client credentials/workload identity.
- Require explicit authorities such as `SCOPE_balance:write` and `SCOPE_account:provision` on internal methods.
- Do not propagate a customer token as the service credential. Carry customer context separately and verify both service and user authorization where needed.
- Add Kubernetes NetworkPolicies/firewalls as defense in depth, not as the only control.
- Add tests proving a customer/admin user token cannot call internal endpoints.

#### 2. Customers can create money through the public account API

Evidence:

- `CreateAccountRequest` accepts `userId`, `iban`, `currency`, and `balance`.
- `AccountService.createAccount` allows an owner to create an account and preserves a caller-supplied non-negative balance.
- The same endpoint accepts a caller-selected IBAN and automatically issues a card.

Impact: an authenticated user can create a new account with an arbitrary positive opening balance. This is direct money creation.

Required fix:

- Remove public direct account creation, or change it to a controlled product-application workflow.
- Generate IBANs server-side only.
- Always open with a zero ledger balance. Initial funding must be a posted, authorized ledger transaction.
- Restrict account/card issuance to bank workflows and audited operator/service roles.

#### 3. Financial history is mutable and can be fabricated

Evidence:

- `POST /api/transactions` allows a user owning either referenced account to create a transaction record without moving money.
- `PUT /api/transactions/{id}` allows an owner to change transaction status, description, and failure reason.
- Administrators can delete transaction records.
- Administrators can directly overwrite account balances through the account update API.

Impact: transaction history can disagree with balances, a customer can make a failed item look completed, and privileged users can erase or overwrite financial evidence.

Required fix:

- Make posted financial records append-only. Corrections must be reversing/adjusting entries, never updates or deletes.
- Remove generic public transaction CRUD. Expose purpose-specific commands such as transfer initiation/cancellation.
- Remove direct balance updates; derive balances from ledger postings or restrict exceptional adjustments to dual-controlled, fully audited workflows.

#### 4. No production-grade ledger or reconciliation

The current model stores one mutable `balance` per account and a separate transaction history in another service/database. The Saga improves a demo transfer, but it is not a core ledger.

Missing controls include:

- balanced debit/credit journal entries and an immutable posting sequence;
- ledger balance versus available balance, holds, reservations, pending and reversed entries;
- value date, booking date, fees, exchange rates, settlement identifiers, and payment references;
- deterministic reconciliation between the transaction service, account balances, payment rails, and the general ledger;
- durable outbox/inbox or workflow state machine, recovery workers, dead-letter/manual repair queues, and operator tooling;
- public request idempotency. Retrying `POST /api/transactions/transfers` creates a new Saga ID and can initiate a second transfer.

Required fix: design the ledger and payment state machine first, then make every money-moving API post balanced, immutable entries with an externally supplied idempotency key and durable recovery/reconciliation.

#### 5. Strong customer authentication is absent

The application uses only username/password for login and password re-entry for card secrets. It has no MFA/passkeys/TOTP, trusted-device lifecycle, step-up authentication, transaction signing, or dynamic binding of approval to amount and payee.

For an EU payment application, strong customer authentication is expected for online account access and payment initiation, with dynamic linking for remote electronic payments unless a valid exemption applies. Password-only transfer confirmation is not enough.

Required fix: implement SCA with two independent factors, step-up for risky operations, server-side challenges bound to transaction amount/payee, recovery controls, and an auditable exemption/risk-decision engine.

#### 6. Production data and secrets are mixed with development defaults

Evidence:

- All common `application.yml` files force `spring.profiles.active: dev`.
- Database credentials default to `postgres/admin`.
- `REMEMBER_ME_KEY` has a known development fallback.
- `CARD_ENCRYPTION_KEY` has a committed, known fallback key.
- secure remember-me cookies default to `false`.
- JWKS and service URLs default to plain HTTP localhost endpoints.
- `V2__seed_test_data.sql` is in each service's main Flyway migration path and contains known user/admin credentials, personal-looking data, balances, cards, and transactions.
- `baseline-on-migrate: true` is enabled in development configuration and should not silently become a production migration policy.

Impact: a misconfigured deployment can start in development mode with known privileged credentials and cryptographic keys.

Required fix:

- Never declare `dev` as the default active profile. Fail startup when a production profile and required secrets are absent.
- Remove seed/demo data from main migrations; load it only through an explicit local/demo profile.
- Use a secrets manager/KMS/HSM, rotation procedures, least-privileged DB users, and TLS between every component.
- Add automated secret scanning and configuration tests that reject insecure production values.

#### 7. Card-secret design requires specialist redesign and assessment

AES-256-GCM with random IVs is a sound primitive, but the complete design is not production-grade:

- a single environment key decrypts every PAN, CVV, and PIN;
- there is no key ID, envelope encryption, rotation, HSM/KMS, split knowledge, or dual control;
- PIN and CVV are reversibly stored in the ordinary application database and returned by APIs;
- the frontend keeps revealed values in component memory without automatic timeout/re-masking;
- there is no PCI scope definition, retention policy, access audit, or issuer-specific justification.

PCI SSC treats card verification codes and PIN/PIN-block data as sensitive authentication data and generally prohibits retaining it after authorization even when encrypted. Issuers can have a limited legitimate-issuing exception, but that must be justified, secured, and assessed; this repository does not demonstrate those controls.

Required fix: use a qualified card processor/tokenization platform and HSM-backed PIN/card operations. Minimize storage, access, and display; obtain a PCI specialist/QSA assessment before treating this feature as deployable.

### P1 - required before a production pilot

#### 8. No real production deployment platform

There are no Dockerfiles, Compose environment, Kubernetes manifests, Ingress/API gateway, TLS configuration, health probes, autoscaling, resource limits, network policies, or separate production configuration. The provided scripts open local PowerShell windows and assume a local PostgreSQL Windows service.

The root README describes Kubernetes, Ingress, Config Server, Prometheus, Grafana, Zipkin, replicas, and CI/CD as a target, but those artifacts do not exist. It also still says the repository contains only skeletons, which is now stale.

#### 9. Observability and auditability are insufficient

- The POM files do not include Actuator, Prometheus/Micrometer tracing, or an OpenTelemetry/Zipkin exporter, so `/actuator/**` security matchers currently protect endpoints that do not exist.
- Logs rotate locally, but there is no centralized structured logging, correlation/request ID implementation, trace propagation, dashboard, alert, or SIEM integration.
- Business logs are not a tamper-evident audit trail and do not capture approvals, before/after values, SCA evidence, device/IP context, or operator reason codes consistently.
- Amounts, account IDs, IBANs, user identifiers, and downstream error text need a formal log data-classification/redaction policy.

Required fix: add health/readiness/liveness, metrics, distributed tracing, structured redacted logs, immutable audit events, alerts, and operational runbooks with service-level objectives.

#### 10. Resilience is incomplete

- Retry/circuit breaker/timeouts exist only for `transactionService -> bankingService`.
- `userService -> bankingService` account provisioning and `bankingService -> userService` checks have no explicit timeouts/circuit breakers.
- Login throttling is in process memory, so it resets on restart and is inconsistent across replicas. Using `request.getRemoteAddr()` without a trusted-proxy strategy may throttle the Ingress address instead of the customer or allow spoofing if configured incorrectly.
- There are no bulkheads, load shedding, queue backpressure, distributed rate limits, or capacity tests.
- Approval/account provisioning is not idempotent. A remote account may be created even if the user transaction later rolls back, and a retry can create another account.
- If Saga compensation also fails, the system only logs "manual reconciliation required"; there is no durable repair queue or operator workflow.

#### 11. Scheduled payments are definitions only

`ScheduledTransactionService` provides CRUD, but there is no scheduler/worker, due-item locking, execution, retry policy, next-date calculation, holiday/cutoff handling, duplicate prevention, or notification. The frontend has no standing-order management screen.

Therefore "scheduled transactions" should not be advertised as working until execution and recovery are implemented.

#### 12. Account and card lifecycle rules are unsafe/incomplete

- Account owners can change account currency/status and hard-delete their accounts; deletion cascades to cards and beneficiaries.
- There is no check for non-zero balance, pending payments, legal hold, retention, or a controlled close/reopen process.
- Changing the currency of an account with a non-zero balance has no FX conversion/posting.
- Customers can directly request card creation through the API.
- Card controls lack spending/contactless/e-commerce/international/ATM limits and replacement/reissue workflows.

#### 13. Data protection, governance, and operational resilience are not implemented

Missing items include data classification, retention/deletion schedules, privacy notices and consent/legal-basis records, subject-rights workflows, database/backup encryption, tested restore, multi-region/zone strategy, recovery objectives, incident response, breach workflow, dependency/SBOM management, change approvals, vulnerability management, and third-party risk controls.

For an EU financial entity, DORA has applied since 17 January 2025 and makes ICT risk management, incident handling, resilience testing, and third-party risk part of the production baseline. GDPR security-of-processing and privacy-by-design obligations also need a documented implementation.

#### 14. API and frontend production quality gaps

- No OpenAPI specification, versioning strategy, deprecation policy, or generated contract tests.
- No gateway rate limits, request-size limits, security headers, CSP, HSTS, or production web-server configuration.
- The "external" transfer UI accepts a destination internal UUID, not an IBAN; it cannot make an external bank payment.
- Beneficiary APIs exist but have no frontend flow and are not connected to transfer initiation.
- Transaction history supports only sorting/pagination: no date/status/amount/text filters, detail/receipt view, statement, or export.
- There are no profile/address/self-service security settings or password reset screens.
- Accessibility and responsive/mobile behavior are not covered by automated tests.

## Must-have banking features that are missing

The following are must-haves for a real-money retail banking application, not optional polish.

### A. Verified onboarding and compliance

- Customer identity/profile collection beyond username and email.
- Email and phone verification.
- KYC identity-document/liveness verification and evidence retention.
- AML risk rating; sanctions, PEP, and adverse-media screening.
- Source-of-funds/purpose information where required and ongoing customer due diligence.
- Terms/privacy disclosures, consent or legal-basis records, and application/product agreements.
- Manual review/case-management workflow with reason codes and four-eyes approval for high-risk cases.

Current gap: administrator approval is a boolean workflow, not KYC/AML onboarding. Approval can create an account/card even when no user profile exists, falling back to `"Card Holder"`.

### B. Secure authentication and account recovery

- MFA/SCA using two independent factors; preferably passkeys/WebAuthn plus a controlled fallback.
- Step-up authentication for transfers, new beneficiaries, card/PIN reveal, profile/security changes, and new devices.
- Transaction approval dynamically bound to amount and payee.
- Forgotten-password/reset flow, verified contact recovery, recovery codes, lock/unlock, and compromise response.
- Device and session list, remote logout/revocation, refresh-token rotation, and risk-based session controls.
- Distributed brute-force/credential-stuffing defenses, CAPTCHA/risk challenge where appropriate, and customer security alerts.

### C. Correct ledger and balance management

- Immutable double-entry ledger with balanced journals and reversal entries.
- Available, ledger, reserved/held, pending, and overdraft/credit-limit balances.
- Atomic posting rules, deterministic references/sequences, idempotency at every command boundary, and concurrency tests.
- Reconciliation and exception-management jobs with an operator queue.
- Fees, taxes, exchange rates, booking/value dates, settlement states, and end-of-day controls.
- No API that directly sets a balance or deletes/rewrites posted history.

### D. Real payment/transfer capability

- Recipient by validated IBAN/account details, not internal database UUID.
- Integration with an actual/sandbox payment rail (for example SEPA/SEPA Instant in the EU) and inbound payment handling.
- Beneficiary ownership, verification, duplicate handling, SCA when adding/changing, and optional payee-name/IBAN confirmation.
- Payment limits, velocity limits, fees, cutoffs, holidays, sufficient available-funds checks, and fraud/sanctions decisions.
- Durable payment states such as initiated, authorized, submitted, accepted, settled, rejected, cancelled, returned, and reversed.
- Customer idempotency key, receipt/reference, status tracking, notifications, and reconciliation.
- Working standing orders/recurring payments with pause/cancel and execution history.

### E. Customer account and card servicing

- Controlled account application, activation, restriction, dormancy, closure, and statement delivery.
- Account details and downloadable monthly/on-demand statements (PDF/CSV), transaction search/filters, and transfer receipts.
- Card activation, replacement/renewal, lost/stolen replacement, secure PIN workflow, and processor integration.
- Per-channel card controls and limits: ATM, point of sale, e-commerce, contactless, international, merchant/category, and spending limits.
- In-app dispute/chargeback and unauthorized-transaction reporting.

### F. Fraud, notifications, support, and operations

- Real-time transaction risk scoring, velocity/anomaly rules, block/review decisions, and fraud case management.
- Push/SMS/email alerts for login, new device, beneficiary, payment, card status, and suspicious activity.
- Secure customer support messaging and complaint/dispute tracking.
- Immutable audit trail, operator RBAC with least privilege, maker-checker/dual approval, and periodic access review.
- Monitoring, alerts, incident response, backups/restores, disaster recovery, and tested business-continuity procedures.

## Nice-to-have features

Implement these only after the P0/P1 controls and must-have money flows are safe.

### High-value, reasonable next features

- Transaction search with filters, saved views, merchant/payee enrichment, and editable personal categories.
- PDF/CSV statements and shareable transfer receipts.
- Budgets by category, spending trends, monthly cash-flow charts, and unusual-spend insights.
- Savings goals/pots with automatic rules and round-ups.
- Push/email notification preferences and low-balance alerts.
- Frequently used beneficiaries, transfer templates, scheduled-transfer calendar, and bill reminders.
- Virtual cards and disposable virtual cards through a qualified card processor.
- More card controls: online/contactless/international switches and configurable limits.
- Romanian and English localization, RON-first formatting, time-zone correctness, dark mode, and improved accessibility.
- In-app secure support/chat with ticket status.

### Larger product features

- Currency exchange with transparent quotes, expiry, spread/fee disclosure, and ledgered FX postings.
- Multi-currency accounts.
- Request money, split bills, and QR-based payment initiation.
- Open Banking/PSD2 consented account-information and payment-initiation APIs.
- Apple Pay/Google Pay wallet provisioning.
- Cashback, rewards, referrals, and merchant offers.
- Savings interest, term deposits, lending/credit products, and repayment schedules.
- Personal-finance forecasting and opt-in AI categorization/assistant features with strict privacy controls and human-review boundaries.

## Recommended implementation roadmap

### Phase 0 - immediately stop unsafe behavior

1. Deny all `/internal/**` calls unless a valid service identity and required scope are present.
2. Remove public caller-supplied opening balances/IBANs and direct account/card issuance.
3. Remove customer transaction create/status-update and all posted-transaction deletion.
4. Remove direct balance editing; introduce controlled adjustment commands with dual approval as an interim measure.
5. Move seed data out of main Flyway migrations and remove every insecure production fallback.
6. Add regression tests for each exploit path above.

### Phase 1 - build the production foundation

1. Design the immutable double-entry ledger, payment state machine, idempotency contract, durable outbox/inbox, recovery, and reconciliation.
2. Add service/workload identity, TLS/mTLS, gateway controls, secrets/KMS/HSM, key rotation, and production profiles.
3. Add SCA/MFA, step-up transaction authorization, recovery, and session/device controls.
4. Add container/deployment manifests, CI/CD, SBOM and vulnerability scanning, health probes, metrics, traces, centralized logs, alerts, backups, restore tests, and runbooks.
5. Establish ASVS-based threat modeling/security verification and commission penetration/PCI/compliance reviews.

### Phase 2 - complete the minimum banking product

1. Add verified onboarding/KYC/AML/fraud and operational case management.
2. Integrate an external payment sandbox/rail and beneficiary-by-IBAN workflow.
3. Implement limits, fees, payment lifecycle, receipts, notifications, reconciliation, and working standing orders.
4. Add safe account/card lifecycle, statements, filters/export, disputes, and customer support flows.
5. Add end-to-end, contract, concurrency, performance, failover, accessibility, and security tests with enforced coverage gates.

### Phase 3 - add product differentiation

Select nice-to-have features based on user research, operational capacity, risk, and regulatory review. Budgets/analytics, notification preferences, enhanced card controls, localization, and savings goals are the best initial candidates because they add visible customer value without introducing a new lending or payment rail.

## Minimum production exit criteria

Do not call the system production-ready until all of the following are evidenced:

- [ ] All P0 findings are fixed and regression-tested.
- [ ] Threat model completed; OWASP ASVS Level 3-oriented control set agreed for this high-value application.
- [ ] Independent security review and penetration test passed; critical/high findings closed.
- [ ] Compliance/privacy/PCI scope and control ownership formally approved.
- [ ] SCA and transaction signing work end to end.
- [ ] Immutable ledger, idempotency, reconciliation, and failure recovery are tested under concurrency and injected failures.
- [ ] KYC/AML/sanctions/fraud controls and operations queues are integrated.
- [ ] Production deployment uses TLS, service identity, least privilege, secret management, hardened images, and network segmentation.
- [ ] CI/CD runs unit, integration, contract, E2E, security, dependency, migration, and frontend tests with enforced gates.
- [ ] Health, metrics, traces, redacted logs, immutable audits, dashboards, and actionable alerts are operational.
- [ ] Capacity/load/soak tests meet defined SLOs.
- [ ] Backups, point-in-time recovery, disaster recovery, and incident-response exercises pass agreed RPO/RTO objectives.
- [ ] Runbooks, support procedures, on-call ownership, and rollback/forward-fix processes exist.
- [ ] API/documentation and the root README accurately describe the implemented system and deployment.

## Primary reference baseline

- [EU PSD2, Article 97 - strong customer authentication and dynamic linking](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A02015L2366-20250117)
- [EBA payment services and strong customer authentication material](https://www.eba.europa.eu/regulation-and-policy/payment-services-and-electronic-money)
- [EU AML Directive 2015/849 - customer due diligence](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A32015L0849)
- [EBA ML/TF risk-factor and customer-due-diligence guidelines](https://www.eba.europa.eu/legacy/regulation-and-policy/regulatory-activities/anti-money-laundering-and-countering-financing-1?version=2017)
- [PCI SSC FAQ 1280 - storage of card verification codes](https://www.pcisecuritystandards.org/faqs/1280/)
- [PCI SSC FAQ 1533 - sensitive authentication data, including PIN/PIN block](https://www.pcisecuritystandards.org/faqs/1533/)
- [EU Digital Operational Resilience Act (DORA)](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A32022R2554)
- [EU GDPR](https://eur-lex.europa.eu/eli/reg/2016/679/oj/eng)
- [OWASP Application Security Verification Standard 5.0](https://owasp.org/www-project-application-security-verification-standard/)

## Final assessment

The repository demonstrates meaningful progress beyond a skeleton: it has clear service ownership, Flyway migrations, DTO validation, authorization checks, JWT handling, a compensating transfer Saga, idempotent downstream balance operations, a usable Angular interface, and 104 passing automated tests. Those are good engineering foundations.

However, its current API surface can create or misrepresent money, internal balance operations lack service authorization, and the application has neither the core ledger/compliance controls nor the deployment/operational platform required for banking. Treat it as a **learning/demo application only** until the release blockers and minimum production exit criteria above are completed.
