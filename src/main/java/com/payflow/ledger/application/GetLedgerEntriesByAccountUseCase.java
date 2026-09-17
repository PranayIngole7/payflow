package com.payflow.ledger.application;

import com.payflow.ledger.domain.LedgerAccountId;
import com.payflow.ledger.domain.LedgerEntry;

import java.util.List;
import java.util.Objects;

public final class GetLedgerEntriesByAccountUseCase {

    private final LedgerRepository ledgerRepository;

    public GetLedgerEntriesByAccountUseCase(
            LedgerRepository ledgerRepository
    ) {
        this.ledgerRepository = ledgerRepository;
    }

    public List<LedgerEntry> execute(
            LedgerAccountId ledgerAccountId
    ) {
        Objects.requireNonNull(
                ledgerAccountId,
                "ledger account id must not be null"
        );

        return ledgerRepository.findByAccountId(
                ledgerAccountId
        );
    }
}
