package com.payflow.account.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void shouldCreateActiveAccount() {
        AccountId accountId = AccountId.generate();
        Instant createdAt = Instant.now();

        Account account = Account.create(accountId, createdAt);

        assertEquals(accountId, account.id());
        assertEquals(AccountStatus.ACTIVE, account.status());
        assertEquals(createdAt, account.createdAt());
    }

    @Test
    void shouldRejectNullAccountId() {
        assertThrows(
                NullPointerException.class,
                () -> Account.create(null, Instant.now())
        );
    }

    @Test
    void shouldRejectNullCreatedAt() {
        assertThrows(
                NullPointerException.class,
                () -> Account.create(AccountId.generate(), null)
        );
    }

    @Test
    void shouldSuspendActiveAccount() {
        Account account =
                Account.create(
                        AccountId.generate(),
                        Instant.now()
                );

        account.suspend();

        assertEquals(AccountStatus.SUSPENDED, account.status());
    }

    @Test
    void shouldNotSuspendAlreadySuspendedAccount() {
        Account account =
                Account.create(
                        AccountId.generate(),
                        Instant.now()
                );

        account.suspend();

        assertThrows(
                IllegalStateException.class,
                account::suspend
        );
    }
}