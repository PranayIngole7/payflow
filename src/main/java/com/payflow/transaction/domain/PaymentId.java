package com.payflow.transaction.domain;

import com.payflow.shared.domain.DomainId;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for a payment.
 */
public record PaymentId(UUID value) implements DomainId {

    public PaymentId {
        Objects.requireNonNull(
                value,
                "payment id must not be null"
        );
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }
}