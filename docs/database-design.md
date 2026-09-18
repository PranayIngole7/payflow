# PayFlow Database Design

## 1. Overview

PayFlow uses PostgreSQL as the authoritative source of truth for financial state.

The application currently follows a **modular monolith** architecture. Account, Wallet, Payment, Transaction, and Ledger domains share one PostgreSQL database while maintaining clear domain boundaries.

The database is designed around these principles:

- Financial data must be strongly consistent.
- Monetary values use exact decimal types.
- Financial history must remain auditable.
- Database constraints protect fundamental data integrity.
- Cross-record business rules are enforced through application logic and database transactions.
- Ledger records are treated as immutable accounting history.
- Indexes are driven by actual query patterns.
- Database separation is deferred until there is a concrete architectural reason.

### Current database approach

```text
                    PostgreSQL
                 Financial Source
                    of Truth
                       |
        +--------------+--------------+
        |              |              |
     Account         Wallet         Payment
                                       |
                                       v
                                  Transaction
                                       |
                                       v
                                    Ledger
                                       |
                         +-------------+-------------+
                         |                           |
                  Ledger Accounts              Ledger Entries
```

Redis and Kafka are supporting infrastructure. They are not authoritative stores for financial state.

---

## 2. Database Design Principles

### 2.1 PostgreSQL is the financial source of truth

Financial state is persisted in PostgreSQL.

| Component | Responsibility |
|---|---|
| PostgreSQL | Durable financial state and accounting history |
| Redis | Caching and other non-authoritative supporting use cases |
| Kafka | Asynchronous event distribution |

Redis and Kafka must not become the source of truth for wallet balances or ledger history.

### 2.2 Monetary values

Financial amounts use:

```text
PostgreSQL: NUMERIC(19,2)
Java:       BigDecimal
```

Floating-point types such as `FLOAT` and `DOUBLE` are not used for monetary values.

Currency is stored explicitly using:

```text
CHAR(3)
```

Example:

```text
amount   = 1000.00
currency = INR
```

### 2.3 Time

Timestamp fields use:

```text
TIMESTAMPTZ
```

This represents an absolute point in time while preserving timezone-aware semantics.

### 2.4 Identifiers

Main entities use UUID primary keys.

UUIDs provide application-level identifiers suitable for the current modular monolith and future distributed components.

### 2.5 Financial history

Financial records should not be casually deleted.

Completed payments, transactions, and ledger entries must remain available for auditing and reconciliation.

State transitions such as `FAILED`, `CANCELLED`, or `CLOSED` are preferred over destructive deletion where appropriate.

---

# 3. Schema Evolution

The database schema is version-controlled with Flyway migrations.

The current migration history is:

```text
V1__initial_schema.sql
        |
        v
V2__ledger_account_model.sql
        |
        v
V3__ledger_immutability.sql
```

### V1 — Initial schema

V1 establishes the original PayFlow relational model, including:

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

The original ledger model associated ledger entries directly with wallets.

### V2 — Ledger account model

V2 evolves ledger ownership from direct wallet ownership to explicit accounting-account ownership.

The ledger now distinguishes:

```text
LedgerAccountId
LedgerAccountType
```

Wallet accounts use the wallet UUID as their stable ledger account identifier.

External funding and settlement movements use stable predefined ledger account identifiers.

This allows the ledger to represent accounting movements that are not limited to one application wallet.

### V3 — Ledger immutability

V3 adds database-level protection against modification and deletion of ledger entries.

The ledger is therefore protected at both application and database levels.

**The migration files themselves are the authoritative implementation history and should not be rewritten merely to update documentation.**

---

# 4. Core Tables

## 4.1 users

Represents the PayFlow account owner.

Important fields include:

- `id`
- `email`
- `password_hash`
- `first_name`
- `last_name`
- `status`
- `created_at`
- `updated_at`

The email is unique.

Raw passwords are never stored.

## 4.2 roles

Represents authorization roles.

Supported roles include:

```text
CUSTOMER
SUPPORT
ADMIN
```

The role name is unique.

## 4.3 user_roles

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

## 4.4 wallets

Represents the user's current operational wallet state.

Important fields include:

- `id`
- `user_id`
- `currency`
- `balance`
- `status`
- `version`
- `created_at`
- `updated_at`

The MVP maintains:

```text
User 1 ---- 1 Wallet
```

This is enforced through the unique wallet ownership constraint.

The database protects the basic invariant:

```text
balance >= 0
```

The `version` column supports optimistic concurrency control.

### Wallet balance vs ledger

`wallets.balance` represents the current operational state.

The ledger represents the immutable accounting history behind financial movements.

They are deliberately separate concepts.

---

# 5. Payments

## 5.1 payments

A payment represents the business operation/request initiated by a user.

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

Important fields include:

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

Payment-specific structural rules are enforced by application logic, including:

- valid source/destination combinations
- wallet ownership
- transfer source/destination requirements
- authorization
- business validation

## 5.2 payment_attempts

Represents an individual processing attempt for a payment.

A payment can have multiple attempts.

Supported attempt statuses include:

```text
INITIATED
COMPLETED
FAILED
```

The combination:

```text
(payment_id, attempt_number)
```

is unique.

This separates the overall payment lifecycle from individual processing attempts.

---

# 6. Transactions

A transaction represents the financial event resulting from a payment.

Relationship:

```text
Payment 1 ---- 0..1 Transaction
```

In the MVP, `payment_id` is unique in the transaction table.

Important fields include:

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

Supported transaction types:

```text
TRANSFER
DEPOSIT
WITHDRAWAL
```

Supported statuses include:

```text
PENDING
COMPLETED
FAILED
```

A transaction is the bridge between the payment operation and its accounting representation.

---

# 7. Ledger Model

## 7.1 Why the ledger is separate

PayFlow uses three related but distinct concepts:

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
    | How that event was accounted for
    v
Ledger Entries
```

This separation makes financial history explicit and auditable.

## 7.2 Ledger accounts

The Phase 10 ledger uses an explicit accounting-account model.

A ledger account consists conceptually of:

```text
LedgerAccountId
LedgerAccountType
```

For wallet accounts:

```text
LedgerAccountId   = WalletId
LedgerAccountType = WALLET
```

External funding or settlement movements use stable predefined account identifiers.

This avoids treating every accounting movement as if it must belong to a user wallet.

## 7.3 ledger_entries

A ledger entry represents one immutable accounting movement.

Important fields include:

- `id`
- `transaction_id`
- `ledger_account_id`
- `ledger_account_type`
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

Each ledger entry references the transaction that produced the accounting movement.

---

# 8. Double-Entry Accounting

Every completed financial transaction must have balanced accounting entries.

For a transfer:

```text
Wallet A -> Wallet B
Amount: ₹1,000
```

the ledger contains:

```text
DEBIT   Wallet A   ₹1,000
CREDIT  Wallet B   ₹1,000
```

Therefore:

```text
Total Debit  = ₹1,000
Total Credit = ₹1,000
```

For a deposit:

```text
External Funding -> Wallet A
```

the ledger represents:

```text
DEBIT   External Funding Account   ₹1,000
CREDIT  Wallet A                   ₹1,000
```

For a withdrawal:

```text
Wallet A -> External Settlement
```

the ledger represents:

```text
DEBIT   Wallet A                    ₹1,000
CREDIT  External Settlement Account ₹1,000
```

The accounting model therefore remains balanced for wallet inflows and outflows as well as transfers.

---

# 9. Ledger Invariants

For every completed transaction:

```text
SUM(DEBIT) = SUM(CREDIT)
```

This is a cross-row invariant and cannot be enforced by a simple row-level CHECK constraint.

PayFlow enforces the invariant through:

- ledger posting logic
- application transaction boundaries
- automated tests
- database persistence
- reconciliation

A failed posting must not leave partial financial state behind.

---

# 10. Ledger Immutability

Ledger entries represent historical accounting facts.

Once posted, they must not be modified or deleted.

The immutability design has two layers.

### Application layer

The ledger domain does not expose normal update/delete operations for posted entries.

### Database layer

V3 adds database-level protection against modification and deletion of ledger entries.

Therefore:

```text
Ledger Entry
     |
     v
INSERT allowed
     |
     v
Historical record
     |
     +---- UPDATE blocked
     |
     +---- DELETE blocked
```

This provides defense in depth against accidental ORM updates, application bugs, or direct database modification.

---

# 11. Transactional Consistency

Financial state changes are performed within database transaction boundaries.

A typical financial operation must keep related changes atomic:

```text
Wallet balance change
        +
Transaction
        +
Ledger entries
        +
Payment state
```

must not partially succeed.

If ledger posting fails, the surrounding financial operation must roll back rather than leaving an inconsistent wallet or transaction state.

Automated tests verify this rollback behavior.

---

# 12. Wallet Reconciliation

Wallet reconciliation compares the operational wallet balance with the balance implied by its ledger history.

Conceptually:

```text
Expected balance from ledger
             |
             v
        Reconciliation
             |
             v
Compare with wallets.balance
```

For a wallet:

```text
Ledger Credits
    -
Ledger Debits
    =
Ledger-derived balance
```

The result can then be compared with the current wallet balance.

A mismatch indicates that the wallet state and accounting history require investigation.

Reconciliation is a consistency check, not a replacement for the authoritative ledger.

---

# 13. Internal Ledger Read APIs

Phase 10 introduces internal read use cases for ledger history.

The ledger supports querying entries by:

```text
Ledger Account
Transaction
```

These read operations provide controlled access to accounting history without exposing persistence entities directly.

The API layer maps ledger information into response objects.

---

# 14. Idempotency

The `idempotency_keys` table protects financial operations from duplicate client requests.

Example:

```text
POST /api/payments
Idempotency-Key: ABC123
```

For a given user:

```text
(user_id, key)
```

identifies one logical client operation.

The request hash allows PayFlow to detect reuse of an existing key with a different request payload.

Idempotency protects against duplicate client retries.

It does not replace:

- database transactions
- wallet concurrency control
- ledger balancing
- authorization
- Kafka duplicate-event handling

These remain separate reliability concerns.

---

# 15. Fraud Assessments

The `fraud_assessments` table stores risk analysis associated with a transaction.

Important fields include:

- `id`
- `transaction_id`
- `risk_score`
- `risk_level`
- `decision`
- `reasons`
- `model_version`
- `created_at`

`reasons` uses PostgreSQL `JSONB` because the number and structure of explanatory reasons can vary.

The fraud model advises the payment system; it does not directly own financial state changes.

---

# 16. Important Constraints

The database protects fundamental integrity through primary keys, foreign keys, unique constraints, and CHECK constraints.

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

Financial range constraints include:

```text
wallet.balance >= 0
payment.amount > 0
transaction.amount > 0
ledger_entry.amount > 0
```

Fraud score is constrained to its valid range.

Cross-row financial rules remain application/service responsibilities.

---

# 17. Index Strategy

Indexes are driven by actual query patterns.

Primary keys and UNIQUE constraints already provide indexes where appropriate, so redundant indexes should be avoided.

Important query patterns include:

```text
payments by user and creation time
transactions by source wallet and creation time
transactions by destination wallet and creation time
ledger entries by ledger account and creation time
ledger entries by transaction
```

Indexes should support:

- filtering
- joins
- sorting
- pagination

Indexes also increase storage and write overhead, so indexing every column is intentionally avoided.

---

# 18. Database vs Application Responsibilities

### Database responsibilities

PostgreSQL protects:

- primary key uniqueness
- foreign key relationships
- unique business identifiers
- valid structural ranges
- positive monetary amounts
- non-negative wallet balance
- ledger-entry immutability
- basic relational integrity

### Application responsibilities

The application enforces:

- authentication
- authorization
- wallet ownership
- payment workflow
- transfer rules
- sufficient balance
- concurrency handling
- idempotency behavior
- transaction boundaries
- double-entry posting
- ledger balancing
- reconciliation workflow
- fraud decision workflow

This separation keeps structural integrity in the database and business behavior in the application.

---

# 19. Concurrency

Wallet balance changes require concurrency protection.

For example:

```text
Starting balance = ₹10,000

Withdrawal A = ₹8,000
Withdrawal B = ₹7,000
```

The system must not allow both operations to succeed based on the same starting balance.

The wallet `version` field supports optimistic concurrency control.

The non-negative balance constraint provides an additional database safety boundary, but it is not by itself a complete concurrency strategy.

---

# 20. Persistence and Docker

Local PostgreSQL runs through Docker.

The PostgreSQL data directory is:

```text
/var/lib/postgresql/data
```

and is backed by a named Docker volume.

Stopping or restarting the container does not inherently remove the persisted database.

For example:

```bash
docker stop payflow-postgres
docker start payflow-postgres
```

does not remove the volume.

However:

```bash
docker compose down -v
```

removes Docker volumes and can destroy local database data.

A Docker volume provides persistence, not a production backup strategy.

---

# 21. Migration Strategy

Flyway provides version-controlled schema evolution.

The current database history is:

```text
V1 — Initial relational schema
V2 — Ledger account model
V3 — Ledger immutability
```

The migration files are committed to Git and should be treated as historical schema changes.

They should not be rewritten simply to make the current schema documentation look cleaner.

A fresh environment should be able to reproduce the database schema by applying the migrations in order.

---

# 22. Deferred Database Areas

The following areas remain intentionally deferred until their corresponding features are designed:

```text
notifications
ai_insights
transaction_categories
audit_logs
```

This avoids speculative database design.

---

# 23. Current Architecture Boundary

PayFlow currently uses a modular monolith.

Conceptual ownership is:

```text
Account
    -> users / roles

Wallet
    -> wallets

Payment
    -> payments / payment_attempts / idempotency

Transaction
    -> transactions

Ledger
    -> ledger_entries / ledger accounts / reconciliation
```

The domains share PostgreSQL while maintaining application-level boundaries.

Future extraction into independently deployable services is possible, but database separation is intentionally deferred until justified by real scaling or operational requirements.

---

# 24. Phase 10 Database Checkpoint

Phase 10 establishes the ledger as a first-class financial component.

Completed database-level capabilities include:

- explicit ledger-account ownership
- wallet ledger accounts
- external settlement/funding accounts
- balanced double-entry accounting
- immutable ledger history
- transactional rollback protection
- wallet reconciliation support
- internal ledger read access
- version-controlled migrations
- PostgreSQL as financial source of truth

The current migration chain is:

```text
V1__initial_schema.sql
V2__ledger_account_model.sql
V3__ledger_immutability.sql
```

The migration files remain unchanged as part of this documentation update.
