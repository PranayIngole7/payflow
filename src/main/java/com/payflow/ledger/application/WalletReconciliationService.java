package com.payflow.ledger.application;

import com.payflow.ledger.domain.LedgerAccountId;
import com.payflow.ledger.domain.LedgerEntry;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.shared.domain.Currency;
import com.payflow.wallet.application.WalletRepository;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class WalletReconciliationService {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;

    public WalletReconciliationService(
            WalletRepository walletRepository,
            LedgerRepository ledgerRepository
    ) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
    }

    public WalletReconciliationResult reconcile(WalletId walletId) {
        Objects.requireNonNull(
                walletId,
                "wallet id must not be null"
        );

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "wallet not found: " + walletId
                ));

        List<LedgerEntry> entries =
                ledgerRepository.findByAccountId(
                        LedgerAccountId.forWallet(walletId)
                );

        BigDecimal ledgerBalance = calculateLedgerBalance(
                entries,
                wallet.currency()
        );

        BigDecimal walletBalance = wallet.balance().amount();

        BigDecimal difference =
                walletBalance.subtract(ledgerBalance);

        return new WalletReconciliationResult(
                wallet.id(),
                wallet.currency(),
                walletBalance,
                ledgerBalance,
                difference,
                difference.compareTo(BigDecimal.ZERO) == 0
        );
    }

    private BigDecimal calculateLedgerBalance(
            List<LedgerEntry> entries,
            Currency walletCurrency
    ) {
        BigDecimal balance = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            if (!entry.currency().equals(walletCurrency)) {
                throw new IllegalStateException(
                        "ledger entry currency does not match wallet currency"
                );
            }

            BigDecimal amount = entry.amount().amount();

            if (entry.type() == LedgerEntryType.CREDIT) {
                balance = balance.add(amount);
            } else {
                balance = balance.subtract(amount);
            }
        }

        return balance;
    }
}
