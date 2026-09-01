package com.payflow.wallet.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletIdTest {

    @Test
    void shouldCreateWalletId() {
        UUID value = UUID.randomUUID();

        WalletId walletId = new WalletId(value);

        assertEquals(value, walletId.value());
    }

    @Test
    void shouldGenerateWalletId() {
        WalletId walletId = WalletId.generate();

        assertNotNull(walletId.value());
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(
                NullPointerException.class,
                () -> new WalletId(null)
        );
    }
}
