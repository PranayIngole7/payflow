-- PayFlow Phase 5 initial database schema
--
-- PostgreSQL is the authoritative source of truth for financial state.
-- Monetary values use NUMERIC(19,2).
-- UUIDs are used for entity identifiers.
-- TIMESTAMPTZ is used for timestamps.

-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE users (
    id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'LOCKED', 'CLOSED'))
);

-- ============================================================
-- ROLES
-- ============================================================

CREATE TABLE roles (
    id UUID NOT NULL,
    name VARCHAR(30) NOT NULL,

    CONSTRAINT roles_pkey PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT chk_roles_name
        CHECK (name IN ('CUSTOMER', 'SUPPORT', 'ADMIN'))
);

-- ============================================================
-- USER ROLES
-- ============================================================

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- ============================================================
-- WALLETS
-- ============================================================

CREATE TABLE wallets (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    currency CHAR(3) NOT NULL,
    balance NUMERIC(19,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT wallets_pkey PRIMARY KEY (id),
    CONSTRAINT uk_wallets_user UNIQUE (user_id),
    CONSTRAINT chk_wallets_balance CHECK (balance >= 0),
    CONSTRAINT chk_wallets_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'BLOCKED', 'CLOSED')),
    CONSTRAINT fk_wallets_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ============================================================
-- PAYMENTS
-- ============================================================

CREATE TABLE payments (
    id UUID NOT NULL,
    reference VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL,
    source_wallet_id UUID,
    destination_wallet_id UUID,
    type VARCHAR(30) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT payments_pkey PRIMARY KEY (id),
    CONSTRAINT uk_payments_reference UNIQUE (reference),
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_type
        CHECK (type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL')),
    CONSTRAINT chk_payments_status
        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT fk_payments_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_payments_user_created_at
    ON payments (user_id, created_at DESC);

-- ============================================================
-- PAYMENT ATTEMPTS
-- ============================================================

CREATE TABLE payment_attempts (
    id UUID NOT NULL,
    payment_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(500),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    CONSTRAINT payment_attempts_pkey PRIMARY KEY (id),
    CONSTRAINT uk_payment_attempt_number
        UNIQUE (payment_id, attempt_number),
    CONSTRAINT chk_payment_attempt_number
        CHECK (attempt_number > 0),
    CONSTRAINT chk_payment_attempt_status
        CHECK (status IN ('INITIATED', 'COMPLETED', 'FAILED')),
    CONSTRAINT fk_payment_attempt_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id)
);

-- ============================================================
-- TRANSACTIONS
-- ============================================================

CREATE TABLE transactions (
    id UUID NOT NULL,
    reference VARCHAR(100) NOT NULL,
    payment_id UUID NOT NULL,
    source_wallet_id UUID,
    destination_wallet_id UUID,
    type VARCHAR(30) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    CONSTRAINT transactions_pkey PRIMARY KEY (id),
    CONSTRAINT uk_transactions_reference UNIQUE (reference),
    CONSTRAINT uk_transactions_payment UNIQUE (payment_id),
    CONSTRAINT chk_transactions_amount CHECK (amount > 0),
    CONSTRAINT chk_transactions_type
        CHECK (type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL')),
    CONSTRAINT chk_transactions_status
        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    CONSTRAINT fk_transactions_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT fk_transactions_source_wallet
        FOREIGN KEY (source_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_transactions_destination_wallet
        FOREIGN KEY (destination_wallet_id) REFERENCES wallets(id)
);

CREATE INDEX idx_transactions_source_wallet_created_at
    ON transactions (source_wallet_id, created_at DESC);

CREATE INDEX idx_transactions_destination_wallet_created_at
    ON transactions (destination_wallet_id, created_at DESC);

-- ============================================================
-- LEDGER ENTRIES
-- ============================================================

CREATE TABLE ledger_entries (
    id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    wallet_id UUID NOT NULL,
    entry_type VARCHAR(10) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ledger_entries_pkey PRIMARY KEY (id),
    CONSTRAINT chk_ledger_entries_type
        CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_entries_amount
        CHECK (amount > 0),
    CONSTRAINT fk_ledger_entries_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_ledger_entries_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);

CREATE INDEX idx_ledger_entries_wallet_created_at
    ON ledger_entries (wallet_id, created_at DESC);

-- ============================================================
-- IDEMPOTENCY KEYS
-- ============================================================

CREATE TABLE idempotency_keys (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    payment_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT idempotency_keys_pkey PRIMARY KEY (id),
    CONSTRAINT uk_idempotency_user_key
        UNIQUE (user_id, key),
    CONSTRAINT chk_idempotency_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT fk_idempotency_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_idempotency_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id)
);

-- ============================================================
-- FRAUD ASSESSMENTS
-- ============================================================

CREATE TABLE fraud_assessments (
    id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    risk_score NUMERIC(5,2) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    reasons JSONB NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fraud_assessments_pkey PRIMARY KEY (id),
    CONSTRAINT uk_fraud_assessment_transaction
        UNIQUE (transaction_id),
    CONSTRAINT chk_fraud_risk_score
        CHECK (risk_score >= 0 AND risk_score <= 100),
    CONSTRAINT chk_fraud_risk_level
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_fraud_decision
        CHECK (decision IN ('ALLOW', 'REVIEW', 'BLOCK')),
    CONSTRAINT fk_fraud_assessment_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);