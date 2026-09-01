package com.payflow.account.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a PayFlow account.
 */
public final class Account {

    private final AccountId id;
    private final Instant createdAt;
    private AccountStatus status;

    private Account(
            AccountId id,
            Instant createdAt,
            AccountStatus status
    ) {
        this.id = Objects.requireNonNull(id, "account id must not be null");
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );
        this.status = Objects.requireNonNull(
                status,
                "status must not be null"
        );
    }

    public static Account create(
            AccountId id,
            Instant createdAt
    ) {
        return new Account(
                id,
                createdAt,
                AccountStatus.ACTIVE
        );
    }

    public AccountId id() {
        return id;
    }

    public AccountStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public void suspend() {
        if (status == AccountStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "account is already suspended"
            );
        }

        status = AccountStatus.SUSPENDED;
    }
}