package com.payflow.transaction.domain;

import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.WalletId;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a financial transfer transaction.
 */
public final class Transaction {

    private final TransactionId id;
    private final WalletId sourceWalletId;
    private final WalletId destinationWalletId;
    private final Money amount;
    private final Instant createdAt;
    private final String idempotencyKey;

    private TransactionStatus status;

    private Transaction(
            TransactionId id,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt,
            String idempotencyKey,
            TransactionStatus status
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

        if (sourceWalletId.equals(destinationWalletId)) {
            throw new IllegalArgumentException(
                    "source and destination wallets must be different"
            );
        }

        this.amount = Objects.requireNonNull(
                amount,
                "amount must not be null"
        );

        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    "transaction amount must be greater than zero"
            );
        }

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );

        this.idempotencyKey = requireIdempotencyKey(
                idempotencyKey
        );

        this.status = Objects.requireNonNull(
                status,
                "status must not be null"
        );
    }

    /**
     * Creates a new transaction using an explicit idempotency key.
     */
    public static Transaction create(
            TransactionId id,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt,
            String idempotencyKey
    ) {
        return new Transaction(
                id,
                sourceWalletId,
                destinationWalletId,
                amount,
                createdAt,
                idempotencyKey,
                TransactionStatus.PENDING
        );
    }

    /**
     * Backward-compatible factory for existing domain tests.
     *
     * <p>The transaction identifier is used as a deterministic fallback
     * idempotency key. Production application code should always provide
     * an explicit client idempotency key.</p>
     */
    public static Transaction create(
            TransactionId id,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt
    ) {
        Objects.requireNonNull(
                id,
                "transaction id must not be null"
        );

        return create(
                id,
                sourceWalletId,
                destinationWalletId,
                amount,
                createdAt,
                id.value().toString()
        );
    }

    /**
     * Reconstructs a persisted transaction with its original state.
     */
    public static Transaction reconstitute(
            TransactionId id,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt,
            String idempotencyKey,
            TransactionStatus status
    ) {
        return new Transaction(
                id,
                sourceWalletId,
                destinationWalletId,
                amount,
                createdAt,
                idempotencyKey,
                status
        );
    }

    /**
     * Backward-compatible reconstruction method for existing tests.
     */
    public static Transaction reconstitute(
            TransactionId id,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt,
            TransactionStatus status
    ) {
        Objects.requireNonNull(
                id,
                "transaction id must not be null"
        );

        return reconstitute(
                id,
                sourceWalletId,
                destinationWalletId,
                amount,
                createdAt,
                id.value().toString(),
                status
        );
    }

    public void complete() {
        requirePending("complete");

        status = TransactionStatus.COMPLETED;
    }

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

    public com.payflow.shared.domain.Currency currency() {
        return amount.currency();
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public TransactionStatus status() {
        return status;
    }

    private void requirePending(String operation) {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "cannot "
                            + operation
                            + " transaction in status "
                            + status
            );
        }
    }

    private static String requireIdempotencyKey(
            String idempotencyKey
    ) {
        Objects.requireNonNull(
                idempotencyKey,
                "idempotency key must not be null"
        );

        String normalized = idempotencyKey.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "idempotency key must not be blank"
            );
        }

        if (normalized.length() > 100) {
            throw new IllegalArgumentException(
                    "idempotency key must not exceed 100 characters"
            );
        }

        return normalized;
    }
}