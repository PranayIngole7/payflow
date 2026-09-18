# PayFlow

**PayFlow** is a portfolio-grade distributed digital wallet and intelligent payment platform built with Java and Spring Boot.

The project focuses on practical backend engineering for financial systems: wallet state management, payment processing, transactional consistency, idempotency, double-entry accounting, immutable ledger history, reconciliation, testing, and production-oriented architecture.

> **Educational project:** PayFlow is not intended for processing real financial transactions.

---

## Current Status

**Status: Core platform implemented and actively evolving**

PayFlow currently provides a working end-to-end financial workflow covering:

- Account management
- Wallet creation and balance operations
- Payment lifecycle and history
- Idempotent payment operations
- Transaction management
- Double-entry ledger accounting
- Transactional consistency and rollback
- Optimistic locking for concurrent wallet updates
- Input and business validation
- Payment failure handling
- PostgreSQL integration
- Automated test coverage
- Local development and deployment setup

The core Account → Wallet → Payment → Transaction → Ledger → PostgreSQL flow is implemented and tested.

### What's Next

The project will continue to evolve with additional capabilities such as:

- Security and authentication/authorization
- Further observability and operational improvements
- Additional payment/platform capabilities
- Advanced event-driven features where justified
- AI/**RAG** capabilities where they provide meaningful value
- Further production-oriented hardening

This project is intentionally being developed incrementally, with each major capability designed, implemented, tested, documented, and validated before moving to the next phase.
---

## What PayFlow Demonstrates

### Account and Wallet

- Account creation and lifecycle management
- BCrypt password hashing
- User status handling
- Wallet creation
- Currency-aware wallet balances
- Positive amount validation
- Non-negative balance protection
- Optimistic concurrency control

### Payments

- Transfer
- Deposit
- Withdrawal
- Payment lifecycle management
- Payment history
- Payment failure handling
- Request validation
- Semantic validation
- Idempotency
- Request-hash conflict detection

### Transactions and Ledger

- Financial transaction model
- Double-entry accounting
- Debit and credit entries
- Explicit ledger-account identity
- Wallet ledger accounts
- External funding/settlement accounts
- Immutable ledger entries
- Database-level immutability protection
- Transactional rollback
- Wallet reconciliation
- Internal ledger read APIs

### Reliability and Consistency

- PostgreSQL as the financial source of truth
- Atomic financial operations
- Optimistic locking
- Idempotent payment requests
- Database constraints
- Financial invariants
- Automated regression testing

---

## Architecture

PayFlow currently uses a **modular monolith** rather than prematurely splitting the application into microservices.

Conceptual modules:

```text
com.payflow
|
+-- account
|
+-- wallet
|
+-- payment
|
+-- transaction
|
+-- ledger
|
+-- shared
```

The domains share one PostgreSQL database while maintaining clear application-level boundaries.

The architecture can evolve toward independently deployable services later if actual scaling or operational requirements justify that change.

---

## Financial Model

PayFlow intentionally separates three concepts:

```text
Payment
    |
    | What the user requested
    v
Transaction
    |
    | What financial event occurred
    v
Ledger
    |
    | How it was accounted for
    v
Ledger Entries
```

### Double-entry accounting

For a transfer:

```text
Wallet A -> Wallet B
₹1,000
```

the ledger records:

```text
DEBIT   Wallet A   ₹1,000
CREDIT  Wallet B   ₹1,000
```

For a deposit:

```text
DEBIT   External Funding Account   ₹1,000
CREDIT  Wallet A                   ₹1,000
```

For a withdrawal:

```text
DEBIT   Wallet A                    ₹1,000
CREDIT  External Settlement Account ₹1,000
```

The fundamental accounting invariant is:

```text
Total Debit = Total Credit
```

---

## Ledger Account Model

Phase 10 evolved the ledger from direct wallet ownership to an explicit accounting-account model.

The ledger domain uses:

```text
LedgerAccountId
LedgerAccountType
```

Wallet ledger accounts use the wallet UUID:

```text
LedgerAccountId   = WalletId
LedgerAccountType = WALLET
```

External funding and settlement movements use stable predefined ledger-account identifiers.

There is intentionally **no separate `ledger_accounts` table** in the current schema.

---

## Ledger Immutability

Ledger entries represent historical accounting facts.

Once posted, they must not be updated or deleted.

The protection exists at two levels:

```text
Application/domain layer
        +
Database layer
```

The database-level protection was introduced through the V3 migration.

---

## Database

PostgreSQL is the authoritative source of truth for financial state.

Financial amounts use:

```text
PostgreSQL: NUMERIC(19,2)
Java:       BigDecimal
```

The current migration chain is:

```text
V1__initial_schema.sql
V2__ledger_account_model.sql
V3__ledger_immutability.sql
```

**The migration SQL files are version-controlled historical changes and should not be rewritten merely to update documentation.**

Supporting infrastructure:

```text
PostgreSQL -> financial source of truth
Redis      -> supporting cache/non-authoritative use cases
Kafka      -> asynchronous event distribution
```

---

## Technology Stack

- **Java 25**
- **Spring Boot 4**
- **Maven**
- **Spring MVC**
- **Spring Data JPA / Hibernate**
- **Spring Transactions**
- **PostgreSQL**
- **Flyway**
- **Redis**
- **Apache Kafka**
- **Docker / Docker Compose**
- **JUnit**
- **Testcontainers**
- **OpenAPI**
- **AI / RAG / LLM integration planned as the project evolves**

---

## Testing

PayFlow uses unit, integration, persistence, API, concurrency, and financial-consistency tests.

The current full regression result is:

```text
Tests run: 369
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Run the Maven test suite with:

```bash
./mvnw test
```

For a quiet test run:

```bash
./mvnw -q test
```

---

## Local Development

### Prerequisites

Install:

- Java 25
- Maven wrapper support
- Docker
- Docker Compose

The project uses the Maven wrapper:

```bash
./mvnw
```

### Build

```bash
./mvnw clean verify
```

### Run tests

```bash
./mvnw test
```

### Run the application

```bash
./mvnw spring-boot:run
```

The exact profile and infrastructure requirements should be checked in the project configuration and documentation before starting the application.

---

## Docker Infrastructure

PayFlow uses Docker Compose for local supporting infrastructure.

The intended local stack includes:

```text
PostgreSQL
Redis
Kafka
```

PostgreSQL data is stored in a Docker named volume.

Stopping a PostgreSQL container does not inherently delete its volume.

Be careful with:

```bash
docker compose down -v
```

because removing volumes can remove local database data.

A Docker volume is persistence, not a production backup strategy.

---

## Idempotency

Payment creation supports an idempotency key.

Example:

```http
POST /api/payments
Idempotency-Key: ABC123
```

For a user, the pair:

```text
(user_id, idempotency_key)
```

identifies one logical client operation.

PayFlow also hashes the request so reusing an existing idempotency key with a different request payload can be detected.

Idempotency protects against duplicate client retries. It does not replace transaction management, concurrency control, authorization, or ledger balancing.

---

## Reconciliation

Phase 10 introduces wallet reconciliation.

Conceptually:

```text
Ledger Credits
     -
Ledger Debits
     =
Ledger-derived balance
```

The ledger-derived balance is compared with the operational:

```text
wallets.balance
```

A mismatch identifies a consistency condition that requires investigation.

---

## Documentation

Project documentation is maintained under:

```text
docs/
```

Important areas include:

```text
docs/architecture/
docs/adr/
docs/api/
docs/diagrams/
docs/database-design.md
docs/er-diagram.md
docs/project-status.md
```

The documentation is intended to explain not only what PayFlow does, but also why important architectural and financial design decisions were made.

---

## Development Approach

PayFlow is being developed incrementally using the following workflow:

```text
Concept
   |
   v
Why
   |
   v
Design
   |
   v
Implement
   |
   v
Test
   |
   v
Debug
   |
   v
Improve
   |
   v
Document
```

The project favors a **workable-first** approach: establish a coherent, testable financial flow before adding unnecessary complexity.

---

## Phase History

The project has progressed through the following major areas:

```text
Phase 5  -> Database Design
Phase 6  -> Spring Boot Foundation
Phase 7  -> User / Account Service
Phase 8  -> Wallet Service
Phase 9  -> Payment Service
Phase 10 -> Ledger
```

### Phase 8 — Wallet

Completed wallet capabilities include:

- Wallet creation
- Deposit
- Withdrawal
- Balance validation
- Currency validation
- Optimistic locking
- Transactional consistency
- API hardening

### Phase 9 — Payment

Completed payment capabilities include:

- Payment lifecycle
- Payment creation
- Payment history
- Idempotency
- Request hashing
- Validation
- Failure handling
- Correct API error semantics
- Transaction and ledger integration
- PostgreSQL validation

### Phase 10 — Ledger

Completed ledger capabilities include:

- Ledger account model
- Double-entry accounting
- Deposit accounting
- Withdrawal accounting
- Transfer accounting
- Ledger immutability
- Transactional rollback protection
- Wallet reconciliation
- Internal ledger read APIs
- PostgreSQL migration evolution

---

## Project Structure

A simplified structure:

```text
payflow/
|
+-- src/
|   +-- main/
|   |   +-- java/com/payflow/
|   |   +-- resources/
|   |       +-- db/migration/
|   |
|   +-- test/
|
+-- docs/
|
+-- docker-compose.yml
+-- pom.xml
+-- README.md
```

Database migrations:

```text
src/main/resources/db/migration/
|
+-- V1__initial_schema.sql
+-- V2__ledger_account_model.sql
+-- V3__ledger_immutability.sql
```

---

## Portfolio Milestone

PayFlow has reached the agreed **workable milestone**:

```text
Account
   ↓
Wallet
   ↓
Payment
   ↓
Transaction
   ↓
Ledger
   ↓
PostgreSQL
```

with:

- core validation
- idempotency
- payment failure handling
- double-entry accounting
- immutable ledger history
- transactional rollback
- reconciliation
- automated tests
- local development setup
- project documentation

This is the point at which PayFlow can be presented as a substantial backend portfolio project and discussed in interviews.

---

## Future Direction

Future development may explore:

- Kafka-based domain events
- Redis-backed supporting capabilities
- observability
- authentication and authorization hardening
- fraud intelligence
- AI/RAG/LLM features
- Docker/AWS deployment
- CI/CD
- distributed-system patterns
- justified service extraction

These features will be added based on actual requirements rather than speculative complexity.

---

## Disclaimer

PayFlow is an educational and portfolio project.

It is **not intended for processing real financial transactions** and should not be used as a production financial system without substantial additional security, compliance, operational, and regulatory engineering.
