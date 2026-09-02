package com.payflow.transaction.application;

import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;

import java.util.NoSuchElementException;
import java.util.Objects;

public final class GetTransactionUseCase {

    private final TransactionRepository transactionRepository;

    public GetTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(
                transactionRepository,
                "transaction repository must not be null"
        );
    }

    public Transaction execute(TransactionId transactionId) {
        Objects.requireNonNull(
                transactionId,
                "transaction id must not be null"
        );

        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NoSuchElementException(
                        "transaction not found: " + transactionId.value()
                ));
    }
}