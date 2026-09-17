package com.payflow.ledger.api;

import com.payflow.ledger.domain.LedgerAccountType;
import com.payflow.ledger.domain.LedgerEntry;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.shared.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID transactionId,
        UUID ledgerAccountId,
        LedgerAccountType accountType,
        BigDecimal amount,
        Currency currency,
        LedgerEntryType entryType,
        Instant createdAt
) {

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.id().value(),
                entry.transactionId().value(),
                entry.ledgerAccountId().value(),
                entry.accountType(),
                entry.amount().amount(),
                entry.currency(),
                entry.type(),
                entry.createdAt()
        );
    }
}
