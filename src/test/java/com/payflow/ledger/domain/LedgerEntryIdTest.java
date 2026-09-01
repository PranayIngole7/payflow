package com.payflow.ledger.domain;

import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LedgerEntryTest {

    private final LedgerEntryId ledgerEntryId = LedgerEntryId.generate();
    private final TransactionId transactionId = TransactionId.generate();
    private final WalletId walletId = WalletId.generate();
    private final Instant createdAt = Instant.now();

    @Test
    void shouldCreateCreditEntry() {
        Money amount = new Money(
                new BigDecimal("500.00"),
                Currency.INR
        );

        LedgerEntry entry = LedgerEntry.create(
                ledgerEntryId,
                transactionId,
                walletId,
                amount,
                LedgerEntryType.CREDIT,
                createdAt
        );

        assertEquals(ledgerEntryId, entry.id());
        assertEquals(transactionId, entry.transactionId());
        assertEquals(walletId, entry.walletId());
        assertEquals(amount, entry.amount());
        assertEquals(Currency.INR, entry.currency());
        assertEquals(LedgerEntryType.CREDIT, entry.type());
        assertEquals(createdAt, entry.createdAt());
    }

    @Test
    void shouldCreateDebitEntry() {
        LedgerEntry entry = LedgerEntry.create(
                ledgerEntryId,
                transactionId,
                walletId,
                new Money(
                        new BigDecimal("250.00"),
                        Currency.INR
                ),
                LedgerEntryType.DEBIT,
                createdAt
        );

        assertEquals(LedgerEntryType.DEBIT, entry.type());
        assertEquals(
                new BigDecimal("250.00"),
                entry.amount().amount()
        );
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LedgerEntry.create(
                        ledgerEntryId,
                        transactionId,
                        walletId,
                        new Money(BigDecimal.ZERO, Currency.INR),
                        LedgerEntryType.CREDIT,
                        createdAt
                )
        );
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LedgerEntry.create(
                        ledgerEntryId,
                        transactionId,
                        walletId,
                        new Money(
                                new BigDecimal("-100.00"),
                                Currency.INR
                        ),
                        LedgerEntryType.CREDIT,
                        createdAt
                )
        );
    }

    @Test
    void shouldRejectNullLedgerEntryId() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        null,
                        transactionId,
                        walletId,
                        new Money(
                                new BigDecimal("100.00"),
                                Currency.INR
                        ),
                        LedgerEntryType.CREDIT,
                        createdAt
                )
        );
    }

    @Test
    void shouldRejectNullTransactionId() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        ledgerEntryId,
                        null,
                        walletId,
                        new Money(
                                new BigDecimal("100.00"),
                                Currency.INR
                        ),
                        LedgerEntryType.CREDIT,
                        createdAt
                )
        );
    }

    @Test
    void shouldRejectNullWalletId() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        ledgerEntryId,
                        transactionId,
                        null,
                        new Money(
                                new BigDecimal("100.00"),
                                Currency.INR
                        ),
                        LedgerEntryType.CREDIT,
                        createdAt
                )
        );
    }

    @Test
    void shouldRejectNullAmount() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        ledgerEntryId,
                        transactionId,
                        walletId,
                        null,
                        LedgerEntryType.CREDIT,
                        createdAt
                )
        );
    }

    @Test
    void shouldRejectNullType() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        ledgerEntryId,
                        transactionId,
                        walletId,
                        new Money(
                                new BigDecimal("100.00"),
                                Currency.INR
                        ),
                        null,
                        createdAt
                )
        );
    }

    @Test
    void shouldRejectNullCreatedAt() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        ledgerEntryId,
                        transactionId,
                        walletId,
                        new Money(
                                new BigDecimal("100.00"),
                                Currency.INR
                        ),
                        LedgerEntryType.CREDIT,
                        null
                )
        );
    }
}
