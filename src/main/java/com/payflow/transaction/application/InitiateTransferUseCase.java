package com.payflow.transaction.application;

import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.domain.WalletId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Application use case responsible for creating a new transfer transaction.
 *
 * <p>The idempotency key guarantees that retrying the same client request
 * returns the original transaction instead of creating a duplicate.</p>
 */
public final class InitiateTransferUseCase {

    private final TransactionRepository transactionRepository;
    private final TransactionRunner transactionRunner;

    public InitiateTransferUseCase(
            TransactionRepository transactionRepository,
            TransactionRunner transactionRunner
    ) {
        this.transactionRepository = Objects.requireNonNull(
                transactionRepository,
                "transaction repository must not be null"
        );

        this.transactionRunner = Objects.requireNonNull(
                transactionRunner,
                "transaction runner must not be null"
        );
    }

    /**
     * Initiates a new wallet-to-wallet transfer.
     *
     * <p>If the idempotency key has already been used for the same request,
     * the existing transaction identifier is returned.</p>
     *
     * @return the existing or newly created transaction identifier
     */
    public TransactionId execute(
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            String idempotencyKey
    ) {
        Objects.requireNonNull(
                sourceWalletId,
                "source wallet id must not be null"
        );

        Objects.requireNonNull(
                destinationWalletId,
                "destination wallet id must not be null"
        );

        Objects.requireNonNull(
                amount,
                "amount must not be null"
        );

        String normalizedIdempotencyKey =
                normalizeIdempotencyKey(idempotencyKey);

        AtomicReference<TransactionId> result =
                new AtomicReference<>();

        transactionRunner.execute(() -> {
            Optional<Transaction> existing =
                    transactionRepository.findByIdempotencyKey(
                            normalizedIdempotencyKey
                    );

            if (existing.isPresent()) {
                Transaction transaction = existing.get();

                validateSameRequest(
                        transaction,
                        sourceWalletId,
                        destinationWalletId,
                        amount
                );

                result.set(transaction.id());
                return;
            }

            Transaction transaction = Transaction.create(
                    TransactionId.generate(),
                    sourceWalletId,
                    destinationWalletId,
                    amount,
                    Instant.now(),
                    normalizedIdempotencyKey
            );

            transactionRepository.save(transaction);

            result.set(transaction.id());
        });

        return Objects.requireNonNull(
                result.get(),
                "transaction runner did not execute the operation"
        );
    }

    private void validateSameRequest(
            Transaction existing,
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount
    ) {
        boolean sameSource =
                existing.sourceWalletId().equals(sourceWalletId);

        boolean sameDestination =
                existing.destinationWalletId().equals(destinationWalletId);

        boolean sameAmount =
                existing.amount()
                        .amount()
                        .compareTo(amount.amount()) == 0;

        boolean sameCurrency =
                existing.currency().equals(amount.currency());

        if (!sameSource
                || !sameDestination
                || !sameAmount
                || !sameCurrency) {

            throw new IllegalStateException(
                    "idempotency key has already been used "
                            + "for a different transaction"
            );
        }
    }

    private String normalizeIdempotencyKey(
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