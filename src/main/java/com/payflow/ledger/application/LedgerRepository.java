package com.payflow.ledger.application;

import com.payflow.ledger.domain.Ledger;
import com.payflow.transaction.domain.TransactionId;

import java.util.Optional;

/**
 * Persistence port for ledger aggregates.
 */
public interface LedgerRepository {

    Optional<Ledger> findByTransactionId(TransactionId transactionId);

    void save(Ledger ledger);
}