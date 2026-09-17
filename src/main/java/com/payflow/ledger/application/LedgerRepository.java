package com.payflow.ledger.application;

import com.payflow.ledger.domain.Ledger;
import com.payflow.ledger.domain.LedgerAccountId;
import com.payflow.ledger.domain.LedgerEntry;
import com.payflow.transaction.domain.TransactionId;

import java.util.List;
import java.util.Optional;

public interface LedgerRepository {

    Optional<Ledger> findByTransactionId(TransactionId transactionId);

    List<LedgerEntry> findByAccountId(LedgerAccountId ledgerAccountId);

    void save(Ledger ledger);
}