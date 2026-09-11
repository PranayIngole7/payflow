package com.payflow.account.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a PayFlow account.
 *
 * <p>An account represents the user-facing identity within PayFlow.
 * Authentication credentials are deliberately outside this aggregate
 * and will be handled by the security boundary in a later phase.</p>
 */
public final class Account {

    private final AccountId id;
    private final String email;
    private String firstName;
    private String lastName;
    private final Instant createdAt;
    private AccountStatus status;

    private Account(
            AccountId id,
            String email,
            String firstName,
            String lastName,
            Instant createdAt,
            AccountStatus status
    ) {
        this.id = Objects.requireNonNull(
                id,
                "account id must not be null"
        );

        this.email = requireNotBlank(
                email,
                "email must not be blank"
        );

        this.firstName = requireNotBlank(
                firstName,
                "first name must not be blank"
        );

        this.lastName = requireNotBlank(
                lastName,
                "last name must not be blank"
        );

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
            String email,
            String firstName,
            String lastName,
            Instant createdAt
    ) {
        return new Account(
                id,
                email,
                firstName,
                lastName,
                createdAt,
                AccountStatus.ACTIVE
        );
    }

    public static Account reconstitute(
            AccountId id,
            String email,
            String firstName,
            String lastName,
            Instant createdAt,
            AccountStatus status
    ) {
        return new Account(
                id,
                email,
                firstName,
                lastName,
                createdAt,
                status
        );
    }

    public AccountId id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
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

    public void updateProfile(
            String firstName,
            String lastName
    ) {
        this.firstName = requireNotBlank(
                firstName,
                "first name must not be blank"
        );

        this.lastName = requireNotBlank(
                lastName,
                "last name must not be blank"
        );
    }

    private static String requireNotBlank(
            String value,
            String message
    ) {
        Objects.requireNonNull(value, message);

        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}