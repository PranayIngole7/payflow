package com.payflow.transaction.api;

import com.payflow.transaction.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        UUID sourceWalletId,
        UUID destinationWalletId,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.id().value(),
                transaction.sourceWalletId().value(),
                transaction.destinationWalletId().value(),
                transaction.amount().amount(),
                transaction.currency().name(),
                transaction.status().name(),
                transaction.createdAt()
        );
    }
}