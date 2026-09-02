package com.payflow.transaction.application;

import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.domain.WalletId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

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

    public InitiationResult execute(
            WalletId sourceWalletId,
            WalletId destinationWalletId,
            Money amount,
            String idempotencyKey
    ) {
        Objects.requireNonNull(sourceWalletId, "source wallet id must not be null");
        Objects.requireNonNull(destinationWalletId, "destination wallet id must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

        InitiationResult[] result = new InitiationResult[1];

        transactionRunner.execute(() -> {
            Optional<Transaction> existing =
                    transactionRepository.findByIdempotencyKey(normalizedIdempotencyKey);

            if (existing.isPresent()) {
                Transaction transaction = existing.get();

                validateSameRequest(
                        transaction,
                        sourceWalletId,
                        destinationWalletId,
                        amount
                );

                result[0] = new InitiationResult(transaction.id(), false);
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

            result[0] = new InitiationResult(transaction.id(), true);
        });

        return Objects.requireNonNull(
                result[0],
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
                existing.amount().amount().compareTo(amount.amount()) == 0;

        boolean sameCurrency =
                existing.currency().equals(amount.currency());

        if (!sameSource || !sameDestination || !sameAmount || !sameCurrency) {
            throw new IllegalStateException(
                    "idempotency key has already been used for a different transaction"
            );
        }
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
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

    public record InitiationResult(
            TransactionId transactionId,
            boolean created
    ) {
        public InitiationResult {
            Objects.requireNonNull(
                    transactionId,
                    "transaction id must not be null"
            );
        }
    }
}