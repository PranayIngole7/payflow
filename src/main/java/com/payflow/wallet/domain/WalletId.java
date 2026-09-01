package com.payflow.wallet.domain;

import com.payflow.shared.domain.DomainId;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for a wallet.
 *
 * <p>A wallet ID is intentionally distinct from other domain identifiers,
 * even though all domain IDs use UUIDs internally. This prevents accidental
 * mixing of identifiers from different aggregates.</p>
 */
public record WalletId(UUID value) implements DomainId {

    public WalletId {
        Objects.requireNonNull(value, "wallet id must not be null");
    }

    /**
     * Creates a new unique wallet identifier.
     */
    public static WalletId generate() {
        return new WalletId(UUID.randomUUID());
    }
}
