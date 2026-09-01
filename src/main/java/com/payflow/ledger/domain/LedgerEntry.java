package com.payflow.ledger.domain;

import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.domain.WalletId;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable accounting entry representing one side of a financial transaction.
 *
 * <p>Ledger entries are permanent financial history. Once created, an entry
 * cannot be modified. Corrections must be represented by compensating entries
 * rather than changing historical records.</p>
 */
public final class LedgerEntry {

    private final LedgerEntryId id;
    private final TransactionId transactionId;
    private final WalletId walletId;
    private final Money amount;
    private final LedgerEntryType type;
    private final Instant createdAt;

    private LedgerEntry(
            LedgerEntryId id,
            TransactionId transactionId,
            WalletId walletId,
            Money amount,
            LedgerEntryType type,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(
                id,
                "ledger entry id must not be null"
        );
        this.transactionId = Objects.requireNonNull(
                transactionId,
                "transaction id must not be null"
        );
        this.walletId = Objects.requireNonNull(
                walletId,
                "wallet id must not be null"
        );
        this.amount = Objects.requireNonNull(
                amount,
                "amount must not be null"
        );
        this.type = Objects.requireNonNull(
                type,
                "entry type must not be null"
        );
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );

        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    "ledger entry amount must be greater than zero"
            );
        }
    }

    /**
     * Creates an immutable ledger entry.
     */
    public static LedgerEntry create(
            LedgerEntryId id,
            TransactionId transactionId,
            WalletId walletId,
            Money amount,
            LedgerEntryType type,
            Instant createdAt
    ) {
        return new LedgerEntry(
                id,
                transactionId,
                walletId,
                amount,
                type,
                createdAt
        );
    }

    public LedgerEntryId id() {
        return id;
    }

    public TransactionId transactionId() {
        return transactionId;
    }

    public WalletId walletId() {
        return walletId;
    }

    public Money amount() {
        return amount;
    }

    public Currency currency() {
        return amount.currency();
    }

    public LedgerEntryType type() {
        return type;
    }

    public Instant createdAt() {
        return createdAt;
    }
}