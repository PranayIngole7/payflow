package com.payflow.transaction.domain;

import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.WalletId;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a financial transaction.
 *
 * <p>A transaction may represent a transfer, deposit, or withdrawal.</p>
 */
public final class Transaction {

    private final TransactionId id;
    private final WalletId sourceWalletId;
    private final WalletId destinationWalletId;
    private final Money amount;
    private final Instant createdAt;
    private final String idempotencyKey;
    private final TransactionType type;

    private TransactionStatus status;

    private Transaction(
            TransactionId id,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt,
            String idempotencyKey,
            TransactionType type,
            TransactionStatus status
    ) {
        this.id = Objects.requireNonNull(
                id,
                "transaction id must not be null"
        );

        this.type = Objects.requireNonNull(
                type,
                "transaction type must not be null"
        );

        this.sourceWalletId = sourceWalletId;
        this.destinationWalletId = destinationWalletId;

        this.amount = Objects.requireNonNull(
                amount,
                "amount must not be null"
        );

        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    "transaction amount must be greater than zero"
            );
        }

        validateWallets(
                type,
                sourceWalletId,
                destinationWalletId
        );

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

    public static Transaction create(
            TransactionId id,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt,
            String idempotencyKey
    ) {
        return create(
                id,
                sourceWalletId,
                destinationWalletId,
                amount,
                createdAt,
                idempotencyKey,
                TransactionType.TRANSFER
        );
    }

    public static Transaction create(
            TransactionId id,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt,
            String idempotencyKey,
            TransactionType type
    ) {
        return new Transaction(
                id,
                sourceWalletId,
                destinationWalletId,
                amount,
                createdAt,
                idempotencyKey,
                type,
                TransactionStatus.PENDING
        );
    }

    /**
     * Backward-compatible transfer creation.
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
                id.value().toString(),
                TransactionType.TRANSFER
        );
    }

    public static Transaction createDeposit(
            TransactionId id,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt,
            String idempotencyKey
    ) {
        return create(
                id,
                null,
                destinationWalletId,
                amount,
                createdAt,
                idempotencyKey,
                TransactionType.DEPOSIT
        );
    }

    public static Transaction createWithdrawal(
            TransactionId id,
            WalletId sourceWalletId,
            Money amount,
            Instant createdAt,
            String idempotencyKey
    ) {
        return create(
                id,
                sourceWalletId,
                null,
                amount,
                createdAt,
                idempotencyKey,
                TransactionType.WITHDRAWAL
        );
    }

    public static Transaction reconstitute(
            TransactionId id,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt,
            String idempotencyKey,
            TransactionType type,
            TransactionStatus status
    ) {
        return new Transaction(
                id,
                sourceWalletId,
                destinationWalletId,
                amount,
                createdAt,
                idempotencyKey,
                type,
                status
        );
    }

    /**
     * Backward-compatible transfer reconstitution with idempotency key.
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
        return reconstitute(
                id,
                sourceWalletId,
                destinationWalletId,
                amount,
                createdAt,
                idempotencyKey,
                TransactionType.TRANSFER,
                status
        );
    }

    /**
     * Backward-compatible transfer reconstitution.
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
                TransactionType.TRANSFER,
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

    public TransactionType type() {
        return type;
    }

    public TransactionStatus status() {
        return status;
    }

    private void requirePending(String operation) {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "cannot " + operation
                            + " transaction in status " + status
            );
        }
    }

    private static void validateWallets(
            TransactionType type,
            WalletId sourceWalletId,
            WalletId destinationWalletId
    ) {
        switch (type) {
            case TRANSFER -> {
                Objects.requireNonNull(
                        sourceWalletId,
                        "source wallet id must not be null"
                );
                Objects.requireNonNull(
                        destinationWalletId,
                        "destination wallet id must not be null"
                );

                if (sourceWalletId.equals(destinationWalletId)) {
                    throw new IllegalArgumentException(
                            "source and destination wallets must be different"
                    );
                }
            }

            case DEPOSIT -> {
                if (sourceWalletId != null) {
                    throw new IllegalArgumentException(
                            "deposit must not have a source wallet"
                    );
                }

                Objects.requireNonNull(
                        destinationWalletId,
                        "deposit destination wallet must not be null"
                );
            }

            case WITHDRAWAL -> {
                Objects.requireNonNull(
                        sourceWalletId,
                        "withdrawal source wallet must not be null"
                );

                if (destinationWalletId != null) {
                    throw new IllegalArgumentException(
                            "withdrawal must not have a destination wallet"
                    );
                }
            }
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