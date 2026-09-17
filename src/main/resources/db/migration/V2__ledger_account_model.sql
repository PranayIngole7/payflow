-- ============================================================
-- PHASE 10.3 — LEDGER ACCOUNT MODEL
-- ============================================================

-- Add the new accounting-account identity.
ALTER TABLE ledger_entries
    ADD COLUMN ledger_account_id UUID;

-- Add the accounting-account classification.
ALTER TABLE ledger_entries
    ADD COLUMN account_type VARCHAR(30);

-- Preserve existing ledger history.
-- Every existing ledger entry was previously associated with
-- a wallet, so its wallet_id becomes its ledger_account_id.
UPDATE ledger_entries
SET ledger_account_id = wallet_id,
    account_type = 'WALLET';

-- The new ledger model requires both fields for every entry.
ALTER TABLE ledger_entries
    ALTER COLUMN ledger_account_id SET NOT NULL,
    ALTER COLUMN account_type SET NOT NULL;

-- Restrict account_type to the domain's supported values.
ALTER TABLE ledger_entries
    ADD CONSTRAINT chk_ledger_entries_account_type
    CHECK (
        account_type IN (
            'WALLET',
            'EXTERNAL_FUNDING',
            'EXTERNAL_SETTLEMENT'
        )
    );

-- The ledger account identity now replaces wallet ownership.
ALTER TABLE ledger_entries
    DROP CONSTRAINT fk_ledger_entries_wallet;

ALTER TABLE ledger_entries
    DROP COLUMN wallet_id;

-- Replace the old wallet-based index with the new accounting-account index.
-- DROP INDEX idx_ledger_entries_wallet_created_at;

CREATE INDEX idx_ledger_entries_account_created_at
    ON ledger_entries (ledger_account_id, created_at DESC);
