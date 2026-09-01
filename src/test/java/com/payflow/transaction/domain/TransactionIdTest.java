package com.payflow.transaction.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionIdTest {

    @Test
    void shouldCreateTransactionId() {
        UUID value = UUID.randomUUID();

        TransactionId transactionId = new TransactionId(value);

        assertEquals(value, transactionId.value());
    }

    @Test
    void shouldGenerateTransactionId() {
        TransactionId transactionId = TransactionId.generate();

        assertNotNull(transactionId.value());
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(
                NullPointerException.class,
                () -> new TransactionId(null)
        );
    }
}
