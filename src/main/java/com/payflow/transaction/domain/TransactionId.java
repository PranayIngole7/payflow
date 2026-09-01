package com.payflow.transaction.domain;

import com.payflow.shared.domain.DomainId;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for a financial transaction.
 *
 * <p>Using a dedicated type prevents accidental mixing of identifiers
 * belonging to different domain concepts, such as AccountId, WalletId,
 * and TransactionId.</p>
 */
public record TransactionId(UUID value) implements DomainId {

    public TransactionId {
        Objects.requireNonNull(
                value,
                "transaction id must not be null"
        );
    }

    /**
     * Generates a new unique transaction identifier.
     */
    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID());
    }
}
