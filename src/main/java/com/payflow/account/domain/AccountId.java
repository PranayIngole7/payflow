package com.payflow.account.domain;

import com.payflow.shared.domain.DomainId;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for an account.
 */
public record AccountId(UUID value) implements DomainId {

    public AccountId {
        Objects.requireNonNull(value, "account id must not be null");
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }
}