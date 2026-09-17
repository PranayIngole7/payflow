package com.payflow.ledger.application;

import com.payflow.shared.domain.Currency;
import com.payflow.wallet.domain.WalletId;

import java.math.BigDecimal;
import java.util.Objects;

public record WalletReconciliationResult(
        WalletId walletId,
        Currency currency,
        BigDecimal walletBalance,
        BigDecimal ledgerBalance,
        BigDecimal difference,
        boolean reconciled
) {

    public WalletReconciliationResult {
        Objects.requireNonNull(walletId, "wallet id must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(
                walletBalance,
                "wallet balance must not be null"
        );
        Objects.requireNonNull(
                ledgerBalance,
                "ledger balance must not be null"
        );
        Objects.requireNonNull(
                difference,
                "difference must not be null"
        );
    }
}
