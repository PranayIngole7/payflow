package com.payflow.ledger.domain;

import com.payflow.wallet.domain.WalletId;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for an accounting ledger account.
 *
 * <p>Wallet ledger accounts use the wallet's identity directly.
 * System counterparty accounts use stable predefined identifiers.</p>
 */
public record LedgerAccountId(UUID value) {

    private static final UUID EXTERNAL_FUNDING_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final UUID EXTERNAL_SETTLEMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    public LedgerAccountId {
        Objects.requireNonNull(
                value,
                "ledger account id must not be null"
        );
    }

    /**
     * Creates the ledger account identity for a wallet.
     */
    public static LedgerAccountId forWallet(WalletId walletId) {
        Objects.requireNonNull(
                walletId,
                "wallet id must not be null"
        );

        return new LedgerAccountId(walletId.value());
    }

    /**
     * Returns the stable system account for external funding.
     */
    public static LedgerAccountId externalFunding() {
        return new LedgerAccountId(EXTERNAL_FUNDING_ID);
    }

    /**
     * Returns the stable system account for external settlement.
     */
    public static LedgerAccountId externalSettlement() {
        return new LedgerAccountId(EXTERNAL_SETTLEMENT_ID);
    }
}
