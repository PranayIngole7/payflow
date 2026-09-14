package com.payflow.transaction.domain;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.WalletId;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a payment.
 *
 * <p>A payment represents the business-level request to
 * perform a financial operation. The associated transaction
 * represents the actual financial execution.</p>
 */
public final class Payment {

    private final PaymentId id;
    private final String reference;
    private final AccountId accountId;
    private final WalletId sourceWalletId;
    private final WalletId destinationWalletId;
    private final TransactionType type;
    private final Money amount;
    private final Instant createdAt;

    private PaymentStatus status;
    private Instant updatedAt;

    private Payment(
            PaymentId id,
            String reference,
            AccountId accountId,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            TransactionType type,
            Money amount,
            Instant createdAt,
            Instant updatedAt,
            PaymentStatus status
    ) {
        this.id = Objects.requireNonNull(
                id,
                "payment id must not be null"
        );

        this.reference = requireReference(reference);

        this.accountId = Objects.requireNonNull(
                accountId,
                "account id must not be null"
        );

        this.sourceWalletId = sourceWalletId;
        this.destinationWalletId = destinationWalletId;

        this.type = Objects.requireNonNull(
                type,
                "payment type must not be null"
        );

        this.amount = Objects.requireNonNull(
                amount,
                "amount must not be null"
        );

        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    "payment amount must be greater than zero"
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

        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "updatedAt must not be null"
        );

        this.status = Objects.requireNonNull(
                status,
                "payment status must not be null"
        );
    }

    public static Payment create(
            PaymentId id,
            String reference,
            AccountId accountId,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt,
            TransactionType type
    ) {
        return new Payment(
                id,
                reference,
                accountId,
                sourceWalletId,
                destinationWalletId,
                type,
                amount,
                createdAt,
                createdAt,
                PaymentStatus.PENDING
        );
    }

    public static Payment reconstitute(
            PaymentId id,
            String reference,
            AccountId accountId,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            Instant createdAt,
            Instant updatedAt,
            TransactionType type,
            PaymentStatus status
    ) {
        return new Payment(
                id,
                reference,
                accountId,
                sourceWalletId,
                destinationWalletId,
                type,
                amount,
                createdAt,
                updatedAt,
                status
        );
    }

    public void complete(Instant updatedAt) {
        requirePending("complete");

        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = requireUpdatedAt(updatedAt);
    }

    public void fail(Instant updatedAt) {
        requirePending("fail");

        this.status = PaymentStatus.FAILED;
        this.updatedAt = requireUpdatedAt(updatedAt);
    }

    public void cancel(Instant updatedAt) {
        requirePending("cancel");

        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = requireUpdatedAt(updatedAt);
    }

    public PaymentId id() {
        return id;
    }

    public String reference() {
        return reference;
    }

    public AccountId accountId() {
        return accountId;
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

    public Instant updatedAt() {
        return updatedAt;
    }

    public TransactionType type() {
        return type;
    }

    public PaymentStatus status() {
        return status;
    }

    private void requirePending(String operation) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "cannot " + operation
                            + " payment in status " + status
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

    private static String requireReference(String reference) {
        Objects.requireNonNull(
                reference,
                "payment reference must not be null"
        );

        String normalized = reference.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "payment reference must not be blank"
            );
        }

        if (normalized.length() > 100) {
            throw new IllegalArgumentException(
                    "payment reference must not exceed 100 characters"
            );
        }

        return normalized;
    }

    private static Instant requireUpdatedAt(Instant updatedAt) {
        return Objects.requireNonNull(
                updatedAt,
                "updatedAt must not be null"
        );
    }
}
