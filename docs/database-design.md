# PayFlow Database Design

## 1. Overview

PayFlow uses PostgreSQL as the authoritative source of truth for financial state.

The current application follows a modular-monolith architecture. Account, Wallet, Transaction, and Ledger domains share one PostgreSQL database while maintaining clear domain boundaries.

The database is designed around these principles:

- Financial data must be strongly consistent.
- Monetary values must use exact decimal types.
- Financial history must be preserved and auditable.
- Database constraints should protect fundamental data integrity.
- Cross-record business rules are enforced through application logic and database transactions.
- Indexes are driven by actual query patterns.
- The schema should support future evolution toward independently deployable services if justified.

### Current database approach

```text
Modular Monolith
       |
       v
   PostgreSQL
       |
       +-- Account
       +-- Wallet
       +-- Transaction
       +-- Ledger
       +-- Idempotency
       +-- Fraud Assessment
```

The current design intentionally uses one PostgreSQL database. Separate databases per future microservice are not introduced at this stage.

---

## 2. Database Design Principles

### 2.1 PostgreSQL is the financial source of truth

Wallet balances, payments, transactions, and ledger records are persisted in PostgreSQL.

Redis and Kafka are not authoritative stores for financial state.

- PostgreSQL: durable financial state
- Kafka: asynchronous event distribution
- Redis: caching, rate limiting, temporary/idempotency-related use cases where appropriate

### 2.2 Monetary values

Financial amounts use:

```text
PostgreSQL: NUMERIC(19,2)
Java:       BigDecimal
```

Floating-point types such as `FLOAT` or `DOUBLE` are not used for monetary values because binary floating-point representation can introduce precision errors.

Example:

```text
amount = 1000.00
currency = INR
```

The currency is stored explicitly using `CHAR(3)`.

### 2.3 Time

Timestamp fields use:

```text
TIMESTAMPTZ
```

This stores an absolute point in time while preserving correct timezone-aware semantics.

### 2.4 Identifiers

Tables use UUID primary keys.

UUIDs avoid exposing simple sequential identifiers and work well across future distributed components.

### 2.5 Financial history

Financial records should not be casually deleted.

User, wallet, payment, transaction, and ledger history should be preserved where possible. State changes such as `CLOSED`, `FAILED`, or `CANCELLED` are preferred over destructive deletion for financial records.

---

## 3. Initial Schema

The initial Phase 5 schema contains:

```text
users
roles
user_roles
wallets
payments
payment_attempts
transactions
ledger_entries
idempotency_keys
fraud_assessments
```

### 3.1 users

Represents the PayFlow account owner.

Important fields:

- `id` — UUID primary key
- `email` — unique user email
- `password_hash` — hashed password; raw passwords are never stored
- `first_name`
- `last_name`
- `status`
- `created_at`
- `updated_at`

Allowed user states:

```text
ACTIVE
SUSPENDED
LOCKED
CLOSED
```

The email has a UNIQUE constraint.

User state and wallet state are intentionally separate concepts.

---

### 3.2 roles

Represents authorization roles.

Supported roles:

```text
CUSTOMER
SUPPORT
ADMIN
```

The role name is unique.

---

### 3.3 user_roles

Join table implementing the many-to-many relationship between users and roles.

Primary key:

```text
(user_id, role_id)
```

Foreign keys reference:

```text
users(id)
roles(id)
```

This prevents duplicate user-role assignments.

---

### 3.4 wallets

Represents the user's current operational wallet state.

Important fields:

- `id`
- `user_id`
- `currency`
- `balance`
- `status`
- `version`
- `created_at`
- `updated_at`

MVP relationship:

```text
User 1 ---- 1 Wallet
```

This is enforced with a UNIQUE constraint on `user_id`.

Allowed wallet states:

```text
ACTIVE
SUSPENDED
BLOCKED
CLOSED
```

The database also enforces:

```text
balance >= 0
```

The `version` column supports optimistic concurrency control.

### Wallet balance vs ledger

The wallet balance represents current operational state.

The ledger represents immutable accounting history.

They serve different purposes and should not be treated as interchangeable.

---

### 3.5 payments

Represents a business operation/request.

Supported payment types:

```text
TRANSFER
DEPOSIT
WITHDRAWAL
```

Supported statuses:

```text
PENDING
COMPLETED
FAILED
CANCELLED
```

Important fields:

- `id`
- `reference`
- `user_id`
- `source_wallet_id`
- `destination_wallet_id`
- `type`
- `amount`
- `currency`
- `status`
- `created_at`
- `updated_at`

The payment reference is unique.

The amount must be greater than zero.

Some payment rules are intentionally application-level rather than simple database CHECK constraints:

- A transfer requires both source and destination wallets.
- A deposit requires a destination wallet.
- A withdrawal requires a source wallet.
- Source and destination wallets must differ for transfers.
- Wallet ownership and authorization must be validated by the application.

---

### 3.6 payment_attempts

Represents an individual processing attempt for a payment.

One payment can have multiple attempts.

Supported attempt statuses:

```text
INITIATED
COMPLETED
FAILED
```

Important fields:

- `id`
- `payment_id`
- `attempt_number`
- `status`
- `failure_reason`
- `started_at`
- `completed_at`

The combination:

```text
(payment_id, attempt_number)
```

is unique.

This separates the lifecycle of the overall payment from individual processing attempts.

---

### 3.7 transactions

Represents the financial event resulting from a payment.

The relationship is:

```text
Payment 1 ---- 0..1 Transaction
```

In the MVP, one payment can produce at most one transaction.

Important fields:

- `id`
- `reference`
- `payment_id`
- `source_wallet_id`
- `destination_wallet_id`
- `type`
- `amount`
- `currency`
- `status`
- `created_at`
- `completed_at`

The payment ID is unique in the transactions table.

Supported transaction types:

```text
TRANSFER
DEPOSIT
WITHDRAWAL
```

Supported statuses:

```text
PENDING
COMPLETED
FAILED
```

---

### 3.8 ledger_entries

Represents an individual accounting movement.

For a transfer such as:

```text
Wallet A -> Wallet B
Amount: ₹1,000
```

the ledger contains:

```text
Wallet A   DEBIT    ₹1,000
Wallet B   CREDIT   ₹1,000
```

Important fields:

- `id`
- `transaction_id`
- `wallet_id`
- `entry_type`
- `amount`
- `currency`
- `created_at`

Supported entry types:

```text
DEBIT
CREDIT
```

The amount must be greater than zero.

A ledger entry references both the transaction and wallet.

### Ledger invariant

For every completed transaction:

```text
SUM(DEBIT) = SUM(CREDIT)
```

This is a cross-row accounting invariant. A simple row-level CHECK constraint cannot enforce it.

The application/service transaction and automated tests must enforce this invariant.

The ledger is intended to be immutable financial history.

---

### 3.9 idempotency_keys

Protects financial operations from duplicate client requests.

Example:

```text
POST /api/payments
Idempotency-Key: ABC123
```

If the request times out and the client retries with the same key, PayFlow should return the original payment instead of creating another financial operation.

Important fields:

- `id`
- `user_id`
- `key`
- `request_hash`
- `payment_id`
- `created_at`
- `expires_at`

The combination:

```text
(user_id, key)
```

is unique.

The request hash allows the application to detect a dangerous case where the same idempotency key is reused with a different request payload.

The idempotency record points to the original payment.

### What idempotency protects

It protects against duplicate client requests/retries.

It does not replace:

- wallet concurrency control
- database transaction management
- Kafka duplicate-event handling
- deadlock handling
- ledger balancing
- authorization

These are separate reliability concerns.

---

### 3.10 fraud_assessments

Stores the risk assessment associated with a transaction.

Important fields:

- `id`
- `transaction_id`
- `risk_score`
- `risk_level`
- `decision`
- `reasons`
- `model_version`
- `created_at`

Risk score:

```text
0 <= risk_score <= 100
```

Supported risk levels:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

Supported assessment decisions:

```text
ALLOW
REVIEW
BLOCK
```

`reasons` uses PostgreSQL `JSONB` because a fraud model can produce a variable number of explanatory reasons.

`model_version` records which model/ruleset produced the assessment.

The transaction ID is unique in the MVP, allowing one fraud assessment per transaction.

### Fraud architecture principle

Fraud assessment advises the payment system.

The AI/model does not directly own or execute financial operations.

The backend remains responsible for:

- authentication
- authorization
- business validation
- balance checks
- transaction boundaries
- idempotency
- financial state changes

---

## 4. Entity Relationships

The initial relationships are:

```text
User
 |
 +---- 1:1 ---- Wallet
 |
 +---- 1:N ---- Payment
 |
 +---- 1:N ---- Idempotency Key
 |
 +---- M:N ---- Role
              via User Role

Wallet
 |
 +---- 1:N ---- Payment
 |
 +---- 1:N ---- Ledger Entry

Payment
 |
 +---- 1:N ---- Payment Attempt
 |
 +---- 1:0..1 ---- Transaction

Transaction
 |
 +---- 1:N ---- Ledger Entry
 |
 +---- 1:0..1 ---- Fraud Assessment
```

### Conceptual flow

```text
User
  |
  v
Wallet
  |
  v
Payment
  |
  +---- Payment Attempts
  |
  v
Transaction
  |
  +---- Ledger Entries
  |
  +---- Fraud Assessment
```

---

## 5. Database Constraints

Constraints protect fundamental data integrity.

### Primary keys

Every main entity has a UUID primary key.

### Foreign keys

Foreign keys preserve valid relationships between domains.

Examples:

```text
wallets.user_id -> users.id
payments.user_id -> users.id
transactions.payment_id -> payments.id
ledger_entries.transaction_id -> transactions.id
ledger_entries.wallet_id -> wallets.id
fraud_assessments.transaction_id -> transactions.id
```

### Unique constraints

Important unique constraints include:

```text
users.email
roles.name
wallets.user_id
payments.reference
transactions.reference
transactions.payment_id
payment_attempts(payment_id, attempt_number)
idempotency_keys(user_id, key)
fraud_assessments.transaction_id
```

### CHECK constraints

Examples include:

```text
wallet.balance >= 0
payment.amount > 0
transaction.amount > 0
ledger_entry.amount > 0
fraud_assessment.risk_score BETWEEN 0 AND 100
```

---

## 6. Index Strategy

Indexes are created based on expected query patterns.

Primary keys and UNIQUE constraints already create indexes, so duplicate indexes are avoided.

### Payments

Expected query:

```sql
SELECT *
FROM payments
WHERE user_id = ?
ORDER BY created_at DESC;
```

Index:

```text
idx_payments_user_created_at
(user_id, created_at DESC)
```

### Transactions by source wallet

Expected query:

```sql
SELECT *
FROM transactions
WHERE source_wallet_id = ?
ORDER BY created_at DESC;
```

Index:

```text
idx_transactions_source_wallet_created_at
(source_wallet_id, created_at DESC)
```

### Transactions by destination wallet

Expected query:

```sql
SELECT *
FROM transactions
WHERE destination_wallet_id = ?
ORDER BY created_at DESC;
```

Index:

```text
idx_transactions_destination_wallet_created_at
(destination_wallet_id, created_at DESC)
```

### Ledger history

Expected query:

```sql
SELECT *
FROM ledger_entries
WHERE wallet_id = ?
ORDER BY created_at DESC;
```

Index:

```text
idx_ledger_entries_wallet_created_at
(wallet_id, created_at DESC)
```

### Indexing principle

Do not index every column.

Indexes should be justified by:

- filtering
- joining
- sorting
- pagination
- actual application query patterns

Indexes improve read performance but add storage and write/update overhead.

---

## 7. Database vs Application Responsibilities

Not every business rule belongs in a database CHECK constraint.

### Database responsibilities

PostgreSQL should protect:

- primary key uniqueness
- foreign key relationships
- unique business identifiers
- valid enumerated states where appropriate
- positive monetary amounts
- non-negative wallet balance
- valid risk score range
- basic structural integrity

### Application responsibilities

The application/service layer must enforce:

- authentication
- authorization
- wallet ownership
- payment workflow
- transfer source/destination rules
- sufficient balance
- concurrency control
- idempotency request handling
- transaction boundaries
- ledger balancing
- fraud decision workflow
- domain-specific business rules

This separation keeps the database responsible for fundamental integrity while keeping domain behavior in the application.

---

## 8. Financial Invariants

PayFlow must preserve several important invariants.

### Wallet balance

```text
balance >= 0
```

A database CHECK constraint provides a basic safety boundary, but concurrency control is still required to prevent two concurrent operations from spending the same balance.

### Positive financial amounts

```text
payment.amount > 0
transaction.amount > 0
ledger_entry.amount > 0
```

### Balanced ledger

For every completed transaction:

```text
total debit = total credit
```

### Idempotency

For a given user:

```text
(user_id, idempotency_key)
```

identifies one logical client operation.

### Financial history

Completed financial records should remain auditable and should not be casually deleted.

---

## 9. Concurrency Considerations

The wallet `version` column prepares the design for optimistic locking.

For example, if a wallet contains:

```text
₹10,000
```

and two concurrent withdrawals attempt:

```text
₹8,000
₹7,000
```

the system must not allow both operations to succeed based on the same starting balance.

Database transactions and an appropriate locking/concurrency strategy will be implemented and tested in later PayFlow phases.

The database CHECK constraint:

```text
balance >= 0
```

is necessary but not sufficient for concurrency safety.

---

## 10. Persistence and Docker

Local PostgreSQL runs in Docker.

The PostgreSQL data directory:

```text
/var/lib/postgresql/data
```

is backed by the named Docker volume:

```text
payflow_payflow_postgres_data
```

Therefore, stopping or recreating the PostgreSQL container does not inherently remove the database data, provided the persistent volume is retained.

For example:

```bash
docker stop payflow-postgres
docker start payflow-postgres
```

does not remove the persisted database.

However:

```bash
docker compose down -v
```

removes Docker volumes and can destroy the local database data.

### Persistence is not backup

A Docker volume provides local persistence, not a backup strategy.

Production environments will require proper PostgreSQL backup and recovery mechanisms.

---

## 11. Migration Strategy

During Phase 5, the schema was created manually in PostgreSQL using `psql`.

This was intentional for learning and direct verification of:

- tables
- relationships
- constraints
- indexes
- PostgreSQL data types

The final application should use version-controlled database migrations so a fresh environment can reproduce the schema automatically.

Migration tooling will be introduced during Phase 6 — Spring Boot Foundation.

The database schema should eventually be reproducible from Git without relying on a developer's manually configured local database.

---

## 12. Deliberately Deferred Tables

The following tables are not part of the initial Phase 5 schema:

```text
notifications
ai_insights
transaction_categories
audit_logs
```

They will be introduced only when the corresponding features are designed and implemented.

This avoids speculative database design and keeps the initial schema focused on the core financial domain.

---

## 13. Future Evolution

The current schema supports the modular-monolith architecture.

If scaling requirements later justify microservices, domain boundaries can evolve toward separate services and potentially separate databases.

Potential future ownership:

```text
Account       -> User data
Wallet        -> Wallet state
Payment       -> Payment operations
Transaction   -> Financial transactions
Ledger        -> Accounting history
Notification  -> Notification state
AI            -> AI/fraud/insight data
```

Database separation is intentionally deferred until there is a real architectural reason.

---

## 14. Phase 5 Database Design Checkpoint

Phase 5 database design is complete at the conceptual and manually verified PostgreSQL level.

Completed:

- Database architecture
- Initial schema
- Entity relationships
- PostgreSQL data types
- Monetary representation
- Primary keys
- Foreign keys
- Unique constraints
- CHECK constraints
- Ledger design
- Idempotency design
- Fraud assessment design
- Query-driven indexes
- Financial invariants
- Database/application responsibility boundaries
- Docker PostgreSQL persistence understanding

Next major phase:

```text
Phase 6 — Spring Boot Foundation
```

The next persistence milestone is to introduce version-controlled migrations and connect the Spring Boot application to PostgreSQL.
