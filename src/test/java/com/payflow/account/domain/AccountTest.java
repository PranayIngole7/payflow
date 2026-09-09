package com.payflow.account.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void shouldCreateActiveAccount() {
        AccountId accountId = AccountId.generate();
        Instant createdAt = Instant.now();

        Account account = Account.create(
                accountId,
                "alice@example.com",
                "Alice",
                "Smith",
                createdAt
        );

        assertEquals(accountId, account.id());
        assertEquals("alice@example.com", account.email());
        assertEquals("Alice", account.firstName());
        assertEquals("Smith", account.lastName());
        assertEquals(AccountStatus.ACTIVE, account.status());
        assertEquals(createdAt, account.createdAt());
    }

    @Test
    void shouldRejectNullAccountId() {
        assertThrows(
                NullPointerException.class,
                () -> Account.create(
                        null,
                        "alice@example.com",
                        "Alice",
                        "Smith",
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullEmail() {
        assertThrows(
                NullPointerException.class,
                () -> Account.create(
                        AccountId.generate(),
                        null,
                        "Alice",
                        "Smith",
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectBlankEmail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Account.create(
                        AccountId.generate(),
                        "   ",
                        "Alice",
                        "Smith",
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullFirstName() {
        assertThrows(
                NullPointerException.class,
                () -> Account.create(
                        AccountId.generate(),
                        "alice@example.com",
                        null,
                        "Smith",
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectBlankFirstName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Account.create(
                        AccountId.generate(),
                        "alice@example.com",
                        "   ",
                        "Smith",
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullLastName() {
        assertThrows(
                NullPointerException.class,
                () -> Account.create(
                        AccountId.generate(),
                        "alice@example.com",
                        "Alice",
                        null,
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectBlankLastName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Account.create(
                        AccountId.generate(),
                        "alice@example.com",
                        "Alice",
                        "   ",
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullCreatedAt() {
        assertThrows(
                NullPointerException.class,
                () -> Account.create(
                        AccountId.generate(),
                        "alice@example.com",
                        "Alice",
                        "Smith",
                        null
                )
        );
    }

    @Test
    void shouldSuspendActiveAccount() {
        Account account = Account.create(
                AccountId.generate(),
                "alice@example.com",
                "Alice",
                "Smith",
                Instant.now()
        );

        account.suspend();

        assertEquals(AccountStatus.SUSPENDED, account.status());
    }

    @Test
    void shouldNotSuspendAlreadySuspendedAccount() {
        Account account = Account.create(
                AccountId.generate(),
                "alice@example.com",
                "Alice",
                "Smith",
                Instant.now()
        );

        account.suspend();

        assertThrows(
                IllegalStateException.class,
                account::suspend
        );
    }
}