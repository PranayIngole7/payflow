package com.payflow.account.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountIdTest {

    @Test
    void shouldCreateAccountId() {
        UUID value = UUID.randomUUID();

        AccountId accountId = new AccountId(value);

        assertEquals(value, accountId.value());
    }

    @Test
    void shouldGenerateAccountId() {
        AccountId accountId = AccountId.generate();

        assertNotNull(accountId.value());
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(
                NullPointerException.class,
                () -> new AccountId(null)
        );
    }
}