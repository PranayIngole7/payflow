package com.payflow.transaction.application;

import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;

import java.util.Optional;

public interface TransactionRepository {

    Optional<Transaction> findById(TransactionId transactionId);

    Optional<Transaction> findByIdempotencyKey(
            String idempotencyKey
    );

    void save(Transaction transaction);
}