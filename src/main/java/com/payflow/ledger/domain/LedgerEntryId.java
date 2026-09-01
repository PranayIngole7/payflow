package com.payflow.ledger.domain;

import com.payflow.shared.domain.DomainId;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for an immutable ledger entry.
 *
 * <p>A separate identifier type prevents accidental mixing of ledger
 * identifiers with account, wallet, or transaction identifiers.</p>
 */
public record LedgerEntryId(UUID value) implements DomainId {

    public LedgerEntryId {
        Objects.requireNonNull(
                value,
                "ledger entry id must not be null"
        );
    }

    /**
     * Generates a new unique ledger entry identifier.
     */
    public static LedgerEntryId generate() {
        return new LedgerEntryId(UUID.randomUUID());
    }
}