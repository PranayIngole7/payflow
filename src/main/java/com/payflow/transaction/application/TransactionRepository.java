package com.payflow.transaction.application;

import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;

import java.util.Optional;

/**
 * Persistence port for transaction aggregates.
 */
public interface TransactionRepository {

    Optional<Transaction> findById(TransactionId transactionId);

    void save(Transaction transaction);
}