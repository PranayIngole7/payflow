package com.payflow.ledger.application;

import com.payflow.ledger.domain.LedgerEntry;
import com.payflow.transaction.domain.TransactionId;

import java.util.List;
import java.util.Objects;

public final class GetLedgerEntriesByTransactionUseCase {

    private final LedgerRepository ledgerRepository;

    public GetLedgerEntriesByTransactionUseCase(
            LedgerRepository ledgerRepository
    ) {
        this.ledgerRepository = ledgerRepository;
    }

    public List<LedgerEntry> execute(TransactionId transactionId) {
        Objects.requireNonNull(
                transactionId,
                "transaction id must not be null"
        );

        return ledgerRepository
                .findByTransactionId(transactionId)
                .map(ledger -> ledger.entries())
                .orElseGet(List::of);
    }
}
