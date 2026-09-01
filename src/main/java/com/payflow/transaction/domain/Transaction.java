package com.payflow.transaction.domain;

import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.WalletId;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a financial transaction.
 *
 * <p>The transaction represents the intent and lifecycle of a financial
 * operation between wallets. It does not directly modify wallet balances.
 * Wallet state changes will later be coordinated by the application layer
 * within an appropriate transaction boundary.</p>
 *
 * <p>A newly created transaction starts in {@link TransactionStatus#PENDING}.
 * Only a pending transaction may transition to a terminal state.</p>
 */
public final class Transaction {

    private final TransactionId id;
    private final WalletId sourceWalletId;
    private final WalletId destinationWalletId;
    private final Money amount;
    private final Instant createdAt;

    private TransactionStatus status;

    private Transaction(
            TransactionId id,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(
                id,
                "transaction id must not be null"
        );
        this.sourceWalletId = Objects.requireNonNull(
                sourceWalletId,
                "source wallet id must not be null"
        );
        this.destinationWalletId = Objects.requireNonNull(
                destinationWalletId,
                "destination wallet id must not be null"
        );
        this.amount = Objects.requireNonNull(
                amount,
                "amount must not be null"
        );
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );

        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    "transaction amount must be greater than zero"
            );
        }

        if (sourceWalletId.equals(destinationWalletId)) {
            throw new IllegalArgumentException(
                    "source and destination wallets must be different"
            );
        }

        this.status = TransactionStatus.PENDING;
    }

    /**
     * Creates a new pending transaction.
     *
     * @param id unique transaction identifier
     * @param sourceWalletId wallet from which funds originate
     * @param destinationWalletId wallet receiving the funds
     * @param amount positive monetary amount
     * @param createdAt transaction creation timestamp
     * @return newly created pending transaction
     */
    public static Transaction create(
            TransactionId id,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt
    ) {
        return new Transaction(
                id,
                sourceWalletId,
                destinationWalletId,
                amount,
                createdAt
        );
    }

    /**
     * Marks the transaction as successfully completed.
     *
     * <p>Only a pending transaction may be completed.</p>
     */
    public void complete() {
        requirePending("complete");

        status = TransactionStatus.COMPLETED;
    }

    /**
     * Marks the transaction as failed.
     *
     * <p>Only a pending transaction may be failed.</p>
     */
    public void fail() {
        requirePending("fail");

        status = TransactionStatus.FAILED;
    }

    public TransactionId id() {
        return id;
    }

    public WalletId sourceWalletId() {
        return sourceWalletId;
    }

    public WalletId destinationWalletId() {
        return destinationWalletId;
    }

    public Money amount() {
        return amount;
    }

    public Currency currency() {
        return amount.currency();
    }

    public Instant createdAt() {
        return createdAt;
    }

    public TransactionStatus status() {
        return status;
    }

    /**
     * Ensures lifecycle transitions can only occur from PENDING.
     */
    private void requirePending(String operation) {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "cannot " + operation
                            + " transaction in status " + status
            );
        }
    }
}
