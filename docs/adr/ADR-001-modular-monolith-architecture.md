# ADR-001: Modular Monolith Architecture

- Status: Accepted
- Date: 2026-09-01

## Context

PayFlow is a financial application that requires clear domain boundaries,
strong consistency for financial state, and the ability to evolve toward
independently deployable services if that becomes necessary.

Starting directly with microservices would introduce unnecessary operational
and distributed-system complexity at the current stage of the project.

## Decision

PayFlow will initially be implemented as a modular monolith.

The application will be divided into explicit business modules:

- account
- wallet
- transaction
- ledger
- notification

A small shared module will contain only genuinely cross-cutting concepts.

Each business module owns:

- its domain model
- its business rules
- its application use cases
- its persistence implementation

Modules must communicate through explicit contracts rather than directly
accessing another module's repositories, persistence entities, or internal
implementation details.

## Financial Source of Truth

PostgreSQL is the authoritative source of truth for financial state.

Kafka is used for asynchronous event propagation and integration between
components. Kafka is not considered the source of truth for balances,
transactions, or ledger state.

## Module Responsibilities

### Account

Owns customer/account identity and account lifecycle.

### Wallet

Owns wallets, wallet state, currency, and balance-related invariants.

### Transaction

Owns financial operation workflows such as transfers, deposits, and
withdrawals.

### Ledger

Owns accounting records and double-entry bookkeeping.

Ledger records are treated as immutable financial history. Corrections are
represented through compensating/reversal entries rather than modifying
historical financial records.

### Notification

Owns notification processing and delivery. Notification failures must not
invalidate a successfully committed financial operation.

## Package Structure

Each business module follows:

```text
api/
application/
domain/
infrastructure/
```
### The layers have the following responsibilities:
- api: external interfaces, controllers, requests, and responses
- application: use cases and application orchestration
- domain: business rules, entities, value objects, and domain contracts
- infrastructure: persistence, messaging, and external integrations

## Consequences

### Positive
- Clear business boundaries
- Simple deployment model
- Strong transactional consistency
- Easier local development
- Lower operational complexity than microservices
- Future service extraction remains possible

### Negative
- Module boundaries must be enforced through discipline and architecture tests
- The application still shares a runtime and database
- Incorrect dependencies could gradually turn the monolith into a tightly
coupled system

### Future Evolution
- If a module develops independent scaling, deployment, ownership, or
availability requirements, it may be extracted into a separate service.
- The modular boundaries established now are intended to make such extraction possible without requiring a complete rewrite.


### 2. Create the package structure

Run:

```bash
mkdir -p src/main/java/com/payflow/{account,wallet,transaction,ledger,notification}/{api,application,domain,infrastructure}

mkdir -p src/main/java/com/payflow/shared/{domain,infrastructure}
```

