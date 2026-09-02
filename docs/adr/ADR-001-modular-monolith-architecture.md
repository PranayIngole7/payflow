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
# PayFlow — Modular Monolith Architecture

## Status

**Architecture:** Modular Monolith  
**Phase:** Phase 4 — Architecture  
**Status:** Complete  
**Checkpoint:** 2 September 2026  
**Next Phase:** Phase 5 — Database Design

---

## 1. Architectural Decision

PayFlow is intentionally designed as a **modular monolith**.

The application is deployed as a single Spring Boot application, but internally it is divided into clear business domains with explicit responsibilities and dependency boundaries.

The current domains are:

- `account`
- `wallet`
- `transaction`
- `ledger`
- `notification`
- `shared`

The goal is to achieve strong modular boundaries without introducing the operational and consistency complexity of microservices before it is justified.

---

## 2. Why a Modular Monolith?

PayFlow is a financial system.

The most important requirement at this stage is **financial correctness and consistency**, not independent deployment of every domain.

A microservices architecture would immediately introduce additional concerns:

- Network communication
- Distributed transactions
- Eventual consistency
- Service discovery
- Distributed tracing
- Inter-service authentication
- Retry handling
- Message duplication
- Operational complexity

A modular monolith allows PayFlow to maintain strong domain boundaries while keeping important financial operations inside a single application and database transaction.

### Core principle

> **Establish correct financial boundaries and transactional consistency before introducing distributed-system complexity.**

---

## 3. Modular Monolith vs Traditional Monolith

A traditional monolith often grows around technical layers:

```text
controllers/
services/
repositories/
models/
utils/
```

As the system grows, unrelated business functionality can become tightly coupled.

PayFlow instead organizes primarily by **business domain**:

```text
account/
wallet/
transaction/
ledger/
notification/
shared/
```

Each domain owns its concepts and responsibilities.

The internal architecture then separates concerns within each domain:

```text
domain/
application/
api/
infrastructure/
```

This creates both:

1. **Business boundaries**
2. **Technical boundaries**

---

## 4. High-Level Architecture

```text
                         +----------------+
                         |     Client     |
                         +----------------+
                                  |
                                  | HTTP
                                  v
                    +-------------------------+
                    |       API Layer         |
                    | Controllers / DTOs      |
                    +-------------------------+
                                  |
                                  v
                    +-------------------------+
                    |   Application Layer     |
                    |       Use Cases         |
                    +-------------------------+
                                  |
                                  v
                    +-------------------------+
                    |       Domain Layer      |
                    |     Business Rules      |
                    +-------------------------+
                                  |
                         Repository Ports
                                  |
                                  v
                    +-------------------------+
                    | Infrastructure Layer    |
                    | Adapters / Persistence  |
                    +-------------------------+
                                  |
                    +-------------+-------------+
                    |             |             |
                    v             v             v
              PostgreSQL       Redis         Kafka
              Source of       Supporting     Async
                Truth         Services       Events
```

---

## 5. Package-by-Domain Structure

The intended structure is:

```text
com.payflow
│
├── account
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── wallet
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── transaction
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── ledger
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── notification
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
└── shared
    ├── application
    ├── domain
    └── infrastructure
```

Not every domain must contain every layer.

For example, the ledger does not expose generic public CRUD APIs.

The structure should reflect actual responsibility rather than following a rigid template.

---

# 6. Domain Boundaries

## Account

Responsible for account identity and account lifecycle.

Current concepts include:

```text
Account
AccountId
AccountStatus
```

Account creation produces an `ACTIVE` account.

---

## Wallet

Responsible for monetary balances associated with an account.

Current concepts include:

```text
Wallet
WalletId
Money
Currency
```

Wallet operations include:

```text
credit(...)
debit(...)
```

Wallet business rules protect balance integrity.

A wallet cannot be created for an account that does not exist.

---

## Transaction

Responsible for transfer intent and transfer lifecycle.

Current concepts include:

```text
Transaction
TransactionId
TransactionStatus
```

Transaction states:

```text
PENDING
COMPLETED
FAILED
```

A transaction starts as `PENDING`.

Only a pending transaction can transition to a terminal state.

---

## Ledger

Responsible for authoritative financial history.

The ledger records financial entries such as:

```text
DEBIT
CREDIT
```

A transfer produces corresponding debit and credit entries.

The ledger must remain balanced:

```text
Total Debit = Total Credit
```

The ledger is not exposed as arbitrary public CRUD.

---

## Notification

Reserved for future notification-related behavior.

Kafka can eventually be used to deliver asynchronous events to this domain.

---

## Shared

Contains genuinely shared infrastructure or concepts that do not belong to one specific business domain.

Shared code should be kept deliberately small.

A `shared` package should not become a dumping ground for unrelated business logic.

---

# 7. Layer Responsibilities

Each domain can use four architectural layers.

```text
API
Application
Domain
Infrastructure
```

---

## API Layer

Responsible for external communication.

Examples:

- REST controllers
- Request DTOs
- Response DTOs
- HTTP-level validation
- HTTP response mapping

Controllers should remain thin.

A controller should delegate to a use case rather than implement financial workflows.

### Good

```text
Controller
    |
    v
Use Case
```

### Avoid

```text
Controller
    |
    +--> debit wallet
    +--> credit wallet
    +--> create ledger
    +--> update transaction
```

---

# 8. Application Layer

The application layer represents system operations and orchestrates workflows.

Examples:

```text
CreateAccountUseCase
GetAccountUseCase

CreateWalletUseCase
GetWalletUseCase

InitiateTransferUseCase
ExecuteTransferUseCase
TransferMoneyUseCase
GetTransactionUseCase
```

The application layer coordinates:

- Domain objects
- Repository ports
- Transaction boundaries
- Business workflows

The application layer answers:

> **What operation is the system performing?**

---

# 9. Domain Layer

The domain layer contains business concepts and business rules.

Examples:

```text
Account
Wallet
Transaction
Ledger
Money
```

Examples of domain rules:

```text
Wallet balance cannot become negative.

Transaction source and destination must differ.

Transfer amount must be positive.

Transaction must be PENDING before execution.

Ledger must be balanced.
```

The domain should not depend on:

- Spring Data
- JPA
- Hibernate
- PostgreSQL
- Redis
- Kafka
- HTTP

Business concepts should remain independent from infrastructure technology.

---

# 10. Infrastructure Layer

Infrastructure contains technical implementations.

Examples:

```text
AccountEntity
WalletEntity
TransactionEntity
LedgerEntryEntity
```

and:

```text
SpringDataAccountRepository
SpringDataWalletRepository
SpringDataTransactionRepository
SpringDataLedgerEntryRepository
```

and repository adapters:

```text
AccountRepositoryAdapter
WalletRepositoryAdapter
TransactionRepositoryAdapter
LedgerRepositoryAdapter
```

Infrastructure is responsible for translating between the application/domain model and persistence technology.

---

# 11. Dependency Direction

The desired dependency direction is:

```text
External World
      |
      v
     API
      |
      v
 Application
      |
      v
   Domain
```

Persistence is accessed through ports:

```text
Application
     |
     v
Repository Port
     ^
     |
Repository Adapter
     |
     v
Spring Data / JPA
     |
     v
PostgreSQL
```

The important rule is:

> **Business logic should not depend directly on infrastructure technology.**

---

# 12. Repository Ports

PayFlow uses repository interfaces as persistence ports.

Example:

```java
public interface WalletRepository {

    Optional<Wallet> findById(WalletId walletId);

    void save(Wallet wallet);
}
```

Transaction repository:

```java
public interface TransactionRepository {

    Optional<Transaction> findById(TransactionId transactionId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    void save(Transaction transaction);
}
```

Ledger repository:

```java
public interface LedgerRepository {

    Optional<Ledger> findByTransactionId(TransactionId transactionId);

    void save(Ledger ledger);
}
```

The application depends on these abstractions rather than Spring Data.

---

# 13. Repository Adapters

Infrastructure implements the repository ports.

Conceptually:

```text
                 Application
                      |
                      v
             WalletRepository
                      ^
                      |
                      |
          WalletRepositoryAdapter
                      |
                      v
        SpringDataWalletRepository
                      |
                      v
                  PostgreSQL
```

The adapter is responsible for mapping:

```text
Domain Object
     ↕
Persistence Entity
```

This keeps persistence details outside the domain model.

---

# 14. Domain Objects vs Persistence Entities

PayFlow deliberately separates domain objects from JPA entities.

Example:

```text
Domain:

Wallet
```

Persistence:

```text
WalletEntity
```

The flow is:

```text
Wallet
  |
  | mapping
  v
WalletEntity
  |
  v
JPA
  |
  v
PostgreSQL
```

This prevents persistence concerns from leaking into the business model.

---

# 15. Why Not Put @Entity on Domain Objects?

JPA annotations directly on domain classes would couple the domain to persistence technology.

For example:

```java
@Entity
public class Wallet {
}
```

would make the domain aware of:

```text
JPA
Hibernate
Database Mapping
```

PayFlow instead prefers:

```text
Wallet
```

for business behavior and:

```text
WalletEntity
```

for persistence.

This produces stronger separation of concerns.

---

# 16. Account Architecture

Account flow:

```text
POST /api/v1/accounts
        |
        v
AccountController
        |
        v
CreateAccountUseCase
        |
        v
Account.create(...)
        |
        v
AccountRepository
        |
        v
AccountRepositoryAdapter
        |
        v
AccountEntity
        |
        v
PostgreSQL
```

The account domain creates:

```text
AccountId
CreatedAt
ACTIVE
```

---

# 17. Wallet Architecture

Wallet relationship:

```text
Account
   |
   | owns
   v
Wallet
```

Wallet creation validates that the referenced account exists.

Flow:

```text
CreateWalletUseCase
        |
        v
AccountRepository.findById(...)
        |
        +---- Not Found ----> Reject
        |
        +---- Found --------> Create Wallet
```

This prevents orphan wallets.

Wallet creation also supports an initial balance.

The wallet starts from zero and applies the initial credit when the supplied amount is positive.

---

# 18. Money as a Domain Concept

Financial amounts should not be treated as an unqualified `BigDecimal`.

Money conceptually consists of:

```text
amount
currency
```

For example:

```text
Money(100.00, INR)
```

and:

```text
Money(100.00, USD)
```

are different monetary values.

A dedicated money abstraction keeps amount and currency together and reduces the risk of currency-related mistakes.

---

# 19. Transaction Architecture

Transaction represents a transfer.

It contains:

```text
TransactionId
SourceWalletId
DestinationWalletId
Money
CreatedAt
IdempotencyKey
TransactionStatus
```

Status:

```text
PENDING
COMPLETED
FAILED
```

State transition:

```text
             +----------+
             | PENDING  |
             +----------+
               /      \
              /        \
             v          v
     +-----------+   +---------+
     | COMPLETED |   | FAILED  |
     +-----------+   +---------+
```

A completed transaction must not be executed again.

---

# 20. Transfer Initiation vs Transfer Execution

PayFlow separates:

```text
Transfer initiation
```

from:

```text
Transfer execution
```

Initiation creates the transfer intent:

```text
InitiateTransferUseCase
        |
        v
PENDING transaction
```

Execution performs the actual financial movement:

```text
ExecuteTransferUseCase
        |
        v
TransferMoneyUseCase
        |
        v
Financial movement
        |
        v
COMPLETED
```

This separation provides a clean foundation for future concerns such as:

- Risk checks
- Authorization
- Fraud detection
- Payment-provider integration
- Asynchronous processing
- Retry workflows

---

# 21. Idempotency

Financial APIs must be safe against client retries.

A network timeout can occur after the server has successfully processed a request.

Without idempotency:

```text
First request
    ↓
Transfer ₹100

Response lost

Retry
    ↓
Transfer ₹100 again
```

Result:

```text
₹200 moved
```

This is unacceptable.

---

# 22. Idempotency-Key

Clients provide:

```text
Idempotency-Key: payment-123
```

First request:

```text
payment-123
     |
     v
No existing transaction
     |
     v
Create transaction
```

Retry:

```text
payment-123
     |
     v
Existing transaction
     |
     v
Return existing logical operation
```

The goal is:

```text
Same key
+
Same logical request
=
Same financial effect
```

---

# 23. Idempotency-Key Mismatch

A key cannot be reused for a different financial operation.

Example:

First request:

```text
Key: payment-123
Source: A
Destination: B
Amount: ₹100
```

Second request:

```text
Key: payment-123
Source: A
Destination: C
Amount: ₹500
```

This must be rejected.

Conceptually:

```text
Same key
+
Different financial parameters
=
Conflict
```

This prevents accidental or malicious key reuse.

---

# 24. Database Support for Idempotency

The `transactions` table contains a unique constraint on:

```text
idempotency_key
```

Conceptually:

```text
transactions
------------------------------------------------
id
source_wallet_id
destination_wallet_id
amount
currency
created_at
idempotency_key  <-- UNIQUE
status
------------------------------------------------
```

This provides defense in depth:

```text
Application-level idempotency logic
                +
Database uniqueness constraint
```

The database remains the final integrity boundary for uniqueness.

---

# 25. Atomic Transfer Execution

Transfer execution is the most important financial workflow.

The operation is:

```text
Load transaction
      |
      v
Verify PENDING
      |
      v
Load source wallet
      |
      v
Load destination wallet
      |
      v
Debit source
      |
      v
Credit destination
      |
      v
Write ledger entries
      |
      v
Verify ledger balance
      |
      v
Mark transaction COMPLETED
      |
      v
Persist changes
```

These operations belong to one database transaction.

---

# 26. Financial Transaction Boundary

The core transfer is:

```text
BEGIN TRANSACTION

    Debit source wallet

    Credit destination wallet

    Create ledger entries

    Verify ledger balance

    Mark transaction COMPLETED

COMMIT
```

If an operation fails:

```text
ROLLBACK
```

This prevents partial financial state.

---

# 27. Why Atomicity Matters

Consider:

```text
Source balance       = ₹1000
Destination balance  = ₹500
```

If we perform:

```text
Debit source
COMMIT

Credit destination
ERROR
```

we could end with:

```text
Source      = ₹900
Destination = ₹500
```

The ₹100 has disappeared from the system.

Instead:

```text
BEGIN

Debit source
Credit destination
Write ledger
Complete transaction

COMMIT
```

If credit fails:

```text
ROLLBACK
```

The original state is restored.

---

# 28. TransactionRunner

PayFlow uses a shared:

```text
TransactionRunner
```

abstraction to make transactional execution explicit at the application boundary.

Conceptually:

```java
transactionRunner.execute(() -> {
    // transactional operation
});
```

The application use case expresses:

> This workflow must execute atomically.

Spring provides the underlying transaction implementation.

---

# 29. Spring Transaction Proxy Consideration

Spring transaction management commonly relies on proxies.

Therefore transaction behavior depends not only on annotations but also on how Spring invokes the target object.

During Phase 4, transaction proxy behavior was investigated and corrected.

Important lesson:

> **Understanding Spring transaction annotations also requires understanding Spring's proxy-based execution model.**

This is an important production and interview topic.

---

# 30. Ledger Architecture

The ledger represents financial history.

For a transfer of ₹100:

```text
DEBIT  ₹100
CREDIT ₹100
```

Therefore:

```text
Total Debit  = ₹100
Total Credit = ₹100
```

The ledger must remain balanced.

---

# 31. Why the Ledger Exists

A wallet balance represents current state:

```text
Current balance = ₹800
```

The ledger explains how that state was produced.

For example:

```text
+ ₹1000 initial credit
- ₹100 transfer
- ₹100 transfer
```

Therefore:

```text
Wallet balance
```

and:

```text
Financial history
```

are separate concepts.

The ledger provides the financial history.

---

# 32. Double-Entry Principle

A transfer is represented by corresponding entries:

```text
Source Wallet
    |
    +---- DEBIT ₹250
```

and:

```text
Destination Wallet
    |
    +---- CREDIT ₹250
```

Therefore:

```text
Total Debits = Total Credits
```

This balance is a fundamental financial invariant.

---

# 33. Ledger Is Not Generic CRUD

PayFlow deliberately does not expose arbitrary endpoints such as:

```text
POST /ledger
PUT /ledger/{id}
DELETE /ledger/{id}
```

The ledger represents authoritative financial history.

Allowing arbitrary clients to mutate it could corrupt the financial record.

Instead:

```text
Authorized Financial Workflow
           |
           v
      Ledger Update
```

Ledger changes occur as part of controlled financial operations.

---

# 34. Ledger Repository

The ledger repository exposes business-oriented operations such as:

```text
findByTransactionId(...)
save(...)
```

Persistence retrieves ledger entries and reconstructs the domain ledger.

Conceptually:

```text
PostgreSQL
    |
    v
LedgerEntryEntity
    |
    v
LedgerRepositoryAdapter
    |
    v
Ledger.reconstitute(...)
    |
    v
Ledger
```

---

# 35. PostgreSQL as Source of Truth

PostgreSQL is the authoritative financial data store.

The core rule is:

```text
Financial State
      |
      v
PostgreSQL
```

Redis and Kafka are not authoritative sources for financial state.

This is important because financial state must remain durable, transactional, and reconstructable.

---

# 36. Redis Responsibility

Redis is supporting infrastructure.

Potential future responsibilities include:

- Caching
- Rate limiting
- Temporary data
- Fast lookups
- Supporting coordination

Redis must not become the authoritative wallet balance store.

If Redis disappears, financial state must still be recoverable from PostgreSQL.

---

# 37. Kafka Responsibility

Kafka is intended for asynchronous events and integrations.

Future example:

```text
Transfer COMPLETED
       |
       v
Publish Event
       |
       v
Kafka
       |
       +----> Notification
       |
       +----> Analytics
       |
       +----> Other consumers
```

Kafka does not replace PostgreSQL as the financial source of truth.

---

# 38. Why Kafka Was Not Integrated Everywhere

Kafka was deliberately provisioned without making every operation event-driven.

Adding Kafka to every method would introduce unnecessary complexity:

- Message retries
- Duplicate messages
- Ordering concerns
- Consumer management
- Event contracts
- Operational overhead

The architecture first establishes:

```text
Correct financial transaction
```

and can later add:

```text
Asynchronous side effects
```

where requirements justify them.

---

# 39. API Error Handling

PayFlow uses centralized error handling.

Current mappings include:

```text
Validation error
        ↓
400 Bad Request

Missing required header
        ↓
400 Bad Request

IllegalArgumentException
        ↓
400 Bad Request

IllegalStateException
        ↓
409 Conflict

NoSuchElementException
        ↓
404 Not Found
```

This produces consistent API error responses without duplicating error-handling logic across controllers.

---

# 40. Validation vs Business Rules

These are different responsibilities.

### API Validation

Examples:

```text
amount must not be null
amount must be >= 0.01
currency must not be null
accountId must not be null
```

### Business Rules

Examples:

```text
source wallet != destination wallet

wallet cannot become negative

transaction must be PENDING

ledger must balance
```

Both levels are necessary.

---

# 41. Account → Wallet → Transaction → Ledger

The main financial relationship is:

```text
+-----------+
|  Account  |
+-----------+
      |
      | owns
      v
+-----------+
|  Wallet   |
+-----------+
      |
      | participates in
      v
+----------------+
|  Transaction   |
+----------------+
      |
      | produces
      v
+----------------+
| Ledger Entries |
+----------------+
```

More specifically:

```text
Account
   |
   +---- Wallet A
   |
   +---- Wallet B

Wallet A --------+
                 |
                 v
            Transaction
                 |
                 v
              Ledger
                 ^
                 |
Wallet B --------+
```

---

# 42. Complete Transfer Architecture

```text
                         CLIENT
                           |
                           | HTTP
                           v
                  +------------------+
                  | TransferController|
                  +------------------+
                           |
                           v
                +-----------------------+
                | InitiateTransferUseCase|
                +-----------------------+
                           |
                           | PENDING
                           v
                   +---------------+
                   |  Transaction  |
                   +---------------+
                           |
                           v
                +-----------------------+
                | ExecuteTransferUseCase|
                +-----------------------+
                           |
                           v
                +-----------------------+
                | TransferMoneyUseCase  |
                +-----------------------+
                           |
                           | BEGIN TX
                           v
                 +--------------------+
                 | Load Transaction   |
                 +--------------------+
                           |
                           v
                 +--------------------+
                 | Verify PENDING     |
                 +--------------------+
                           |
                           v
                 +--------------------+
                 | Load Source Wallet |
                 +--------------------+
                           |
                           v
                 +-------------------------+
                 | Load Destination Wallet |
                 +-------------------------+
                           |
                           v
                 +--------------------+
                 | Debit Source       |
                 +--------------------+
                           |
                           v
                 +--------------------+
                 | Credit Destination |
                 +--------------------+
                           |
                           v
                 +--------------------+
                 | Write Ledger       |
                 +--------------------+
                           |
                           v
                 +--------------------+
                 | Verify Balanced    |
                 +--------------------+
                           |
                           v
                 +--------------------+
                 | Mark COMPLETED     |
                 +--------------------+
                           |
                           v
                         COMMIT
```

---

# 43. Full Application-to-Database Flow

The general architecture is:

```text
CLIENT
  |
  v
HTTP Request
  |
  v
Controller
  |
  | DTO validation
  v
Application Use Case
  |
  | orchestration
  v
Domain
  |
  | business rules
  v
Repository Port
  |
  v
Repository Adapter
  |
  | mapping
  v
JPA Entity
  |
  v
Spring Data JPA
  |
  v
PostgreSQL
```

This flow is the core architectural pattern of PayFlow.

---

# 44. Important Architectural Invariants

PayFlow currently protects several important invariants.

### Wallet

```text
balance >= 0
```

### Transaction

```text
sourceWalletId != destinationWalletId
```

```text
amount > 0
```

```text
Only PENDING transactions can be completed or failed.
```

### Idempotency

```text
One idempotency key
=
One logical transaction
```

### Ledger

```text
Total Debit = Total Credit
```

### Financial Atomicity

```text
Debit
+
Credit
+
Ledger
+
Transaction Status
```

must succeed or roll back together.

---

# 45. Testing Architecture

The architecture is tested at multiple levels:

```text
                Tests
                  |
       +----------+----------+
       |          |          |
       v          v          v
    Domain   Application  Integration
                              |
                              v
                          PostgreSQL
```

The project includes:

- Domain tests
- Use-case tests
- REST tests
- Account integration tests
- Wallet tests
- Transfer tests
- Idempotency tests
- PostgreSQL integration tests

The Phase 4 checkpoint has:

```text
157 tests
0 failures
0 errors
0 skipped
```

---

# 46. H2 and PostgreSQL Testing

H2 is used for fast automated tests.

PostgreSQL is the authoritative production-oriented database and is also tested through integration tests.

The strategy is:

```text
H2
 ↓
Fast tests

PostgreSQL
 ↓
Real database integration verification
```

PostgreSQL integration tests are particularly valuable because database behavior can differ between database engines.

---

# 47. Database Constraints as Architectural Protection

Application validation alone is not sufficient for concurrent systems.

For example:

```text
Application
    |
    v
Check idempotency key
    |
    v
Database UNIQUE constraint
```

The database provides the final integrity boundary.

This is particularly important for financial operations where concurrent requests can race.

---

# 48. Concurrency Considerations

Phase 4 establishes the transaction boundary but does not attempt to solve every advanced concurrency scenario.

Future database/reliability work should consider:

- Optimistic locking
- Pessimistic locking
- Isolation levels
- Serialization failures
- Deadlocks
- Concurrent wallet updates
- Retry strategies

Example:

```text
Request A ----+
              |
              v
           Wallet A
              ^
              |
Request B ----+
```

The database design must eventually define how these concurrent operations are serialized safely.

---

# 49. What We Intentionally Did Not Do

Phase 4 deliberately did not:

- Convert the application into microservices
- Introduce distributed transactions
- Make Redis authoritative
- Make Kafka authoritative
- Add generic ledger CRUD
- Build a full notification platform
- Add complex fraud detection
- Add production authentication/authorization
- Solve every advanced concurrency scenario
- Prematurely optimize performance
- Over-engineer the database

These concerns belong in later phases where they are justified by requirements.

---

# 50. Why the Architecture Is Extractable Later

The modular-monolith design is not intended to prevent future service extraction.

The boundaries are designed to make extraction possible if future requirements justify it.

For example:

```text
Current:

                 PayFlow
                    |
        +-----------+-----------+
        |           |           |
     Account      Wallet    Transaction
                    |
                  Ledger
```

A future architecture could potentially become:

```text
Account Service
Wallet Service
Transaction Service
Ledger Service
Notification Service
```

with communication through APIs and/or events.

However, extraction should happen because of real requirements such as:

- Independent scaling
- Team ownership
- Deployment independence
- Isolation requirements
- Different availability requirements

and not simply because microservices are fashionable.

---

# 51. Architectural Decision: Keep Financial State Local

A critical design principle is to keep tightly coupled financial state inside the same transaction boundary while it is still a modular monolith.

For example:

```text
Transaction
Wallet
Ledger
```

participate in one financial workflow.

This allows:

```text
BEGIN
    update transaction
    update source wallet
    update destination wallet
    write ledger
COMMIT
```

without requiring a distributed transaction coordinator.

This is one of the strongest reasons the modular monolith is appropriate for the current stage of PayFlow.

---

# 52. Future Event-Driven Integration

Once the financial transaction is committed, asynchronous events can be published.

Conceptually:

```text
Database Transaction
        |
        v
Transfer COMPLETED
        |
        v
Event
        |
        v
Kafka
        |
        +----> Notification
        |
        +----> Analytics
        |
        +----> External Integration
```

The important distinction is:

```text
Financial state
    =
PostgreSQL
```

while:

```text
Asynchronous reactions
    =
Kafka
```

---

# 53. Architecture Principles

The following principles guide PayFlow:

### 1. Domain First

Business rules should remain close to the domain that owns them.

### 2. Thin Controllers

Controllers should translate HTTP requests into application calls.

### 3. Use Cases Orchestrate

Application use cases coordinate business workflows.

### 4. Infrastructure Is Replaceable

Persistence and infrastructure details should remain behind interfaces where appropriate.

### 5. PostgreSQL Is Authoritative

Financial state must have one authoritative source.

### 6. Financial Operations Are Atomic

Related financial changes must commit or roll back together.

### 7. Idempotency Is Mandatory

Retrying a financial request must not create duplicate financial effects.

### 8. Ledger History Is Protected

Financial history should not be arbitrary CRUD.

### 9. Distributed Complexity Is Introduced Deliberately

Kafka and Redis should solve actual problems rather than exist merely because they are available.

### 10. Boundaries Should Support Future Evolution

The modular monolith should be capable of evolving into services if future requirements justify extraction.

---

# 54. Phase 4 Completion State

At the end of Phase 4, PayFlow has established:

```text
                    PayFlow
                       |
             Modular Monolith
                       |
      +----------------+----------------+
      |                |                |
   Account          Wallet         Transaction
                                       |
                                       v
                                    Ledger
```

With:

```text
API
 ↓
Application
 ↓
Domain
 ↓
Repository Ports
 ↓
Infrastructure Adapters
 ↓
PostgreSQL
```

And the key financial flow:

```text
Request
  |
  v
Idempotency Check
  |
  v
PENDING Transaction
  |
  v
Atomic Transfer
  |
  +--> Debit Source
  |
  +--> Credit Destination
  |
  +--> Write Ledger
  |
  +--> Verify Balance
  |
  v
COMPLETED
  |
  v
COMMIT
```

---

# 55. Phase 4 Interview Summary

If asked:

> **"Explain the architecture of PayFlow."**

A strong answer is:

> PayFlow uses a modular monolith organized around account, wallet, transaction, ledger, notification, and shared domains. Each domain separates API, application, domain, and infrastructure responsibilities where appropriate. Controllers are thin and delegate to application use cases. Use cases orchestrate workflows and depend on repository ports rather than Spring Data directly. Infrastructure adapters implement those ports and map domain objects to JPA entities backed by PostgreSQL.
>
> PostgreSQL is the authoritative financial source of truth. Transfers are idempotent using a persisted idempotency key and database uniqueness constraint. Financial execution happens atomically: the source wallet is debited, the destination wallet is credited, balanced ledger entries are written, and the transaction is completed within one database transaction. Redis is reserved for supporting workloads and Kafka for asynchronous events rather than treating either as financial sources of truth.
>
> The modular monolith gives us strong domain boundaries and transactional simplicity today while preserving the possibility of extracting individual modules into services later if scale, team ownership, or deployment requirements justify it.

---

# 56. Phase 4 Status

**COMPLETE**

The modular-monolith architecture is now established and verified through the Phase 4 implementation and test suite.

Current checkpoint:

```text
Phase 4 — Architecture
        |
        v
      COMPLETE
        |
        v
Phase 5 — Database Design
        |
        v
       NEXT
```

---

# 57. Quick Architecture Reference

```text
PAYFLOW
│
├── Architecture
│   └── Modular Monolith
│
├── Domains
│   ├── Account
│   ├── Wallet
│   ├── Transaction
│   ├── Ledger
│   ├── Notification
│   └── Shared
│
├── Layers
│   ├── API
│   ├── Application
│   ├── Domain
│   └── Infrastructure
│
├── Persistence
│   ├── PostgreSQL = Source of Truth
│   ├── Spring Data JPA
│   └── H2 = Test Database
│
├── Supporting Infrastructure
│   ├── Redis = Supporting Workloads
│   └── Kafka = Asynchronous Events
│
├── Financial Guarantees
│   ├── Atomic Transfers
│   ├── Idempotency
│   ├── Non-negative Wallet Balance
│   ├── Transaction State Control
│   └── Balanced Ledger
│
└── Testing
    ├── Unit Tests
    ├── Application Tests
    ├── REST Tests
    └── PostgreSQL Integration Tests
```

---

# Final Architectural Principle

> **PayFlow is a modular monolith because financial correctness and transactional consistency are the first priorities. Business domains are separated internally, application workflows depend on abstractions, infrastructure remains behind adapters, PostgreSQL is the financial source of truth, transfers are atomic and idempotent, and Redis/Kafka are supporting infrastructure rather than authoritative financial systems.**

**Phase 4 — Architecture: COMPLETE**

**Next: Phase 5 — Database Design**
