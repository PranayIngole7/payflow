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

    @Test
    void shouldCreateLedgerEntry() {
        LedgerEntryId id = LedgerEntryId.generate();
        TransactionId transactionId = TransactionId.generate();
        WalletId walletId = WalletId.generate();
        Money amount = new Money(new BigDecimal("100.00"), Currency.INR);
        Instant createdAt = Instant.now();

        LedgerEntry entry = LedgerEntry.create(
                id,
                transactionId,
                walletId,
                amount,
                LedgerEntryType.CREDIT,
                createdAt
        );

        assertEquals(id, entry.id());
        assertEquals(transactionId, entry.transactionId());
        assertEquals(walletId, entry.walletId());
        assertEquals(amount, entry.amount());
        assertEquals(Currency.INR, entry.currency());
        assertEquals(LedgerEntryType.CREDIT, entry.type());
        assertEquals(createdAt, entry.createdAt());
    }

    @Test
    void shouldRejectNullId() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        null,
                        TransactionId.generate(),
                        WalletId.generate(),
                        money("100.00"),
                        LedgerEntryType.CREDIT,
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullTransactionId() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        LedgerEntryId.generate(),
                        null,
                        WalletId.generate(),
                        money("100.00"),
                        LedgerEntryType.CREDIT,
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullWalletId() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        LedgerEntryId.generate(),
                        TransactionId.generate(),
                        null,
                        money("100.00"),
                        LedgerEntryType.CREDIT,
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullAmount() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        LedgerEntryId.generate(),
                        TransactionId.generate(),
                        WalletId.generate(),
                        null,
                        LedgerEntryType.CREDIT,
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullType() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        LedgerEntryId.generate(),
                        TransactionId.generate(),
                        WalletId.generate(),
                        money("100.00"),
                        null,
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullCreatedAt() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        LedgerEntryId.generate(),
                        TransactionId.generate(),
                        WalletId.generate(),
                        money("100.00"),
                        LedgerEntryType.CREDIT,
                        null
                )
        );
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LedgerEntry.create(
                        LedgerEntryId.generate(),
                        TransactionId.generate(),
                        WalletId.generate(),
                        money("0.00"),
                        LedgerEntryType.CREDIT,
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LedgerEntry.create(
                        LedgerEntryId.generate(),
                        TransactionId.generate(),
                        WalletId.generate(),
                        money("-10.00"),
                        LedgerEntryType.CREDIT,
                        Instant.now()
                )
        );
    }

    @Test
    void shouldSupportDebitEntry() {
        LedgerEntry entry = LedgerEntry.create(
                LedgerEntryId.generate(),
                TransactionId.generate(),
                WalletId.generate(),
                money("100.00"),
                LedgerEntryType.DEBIT,
                Instant.now()
        );

        assertEquals(LedgerEntryType.DEBIT, entry.type());
    }

    @Test
    void shouldSupportCreditEntry() {
        LedgerEntry entry = LedgerEntry.create(
                LedgerEntryId.generate(),
                TransactionId.generate(),
                WalletId.generate(),
                money("100.00"),
                LedgerEntryType.CREDIT,
                Instant.now()
        );

        assertEquals(LedgerEntryType.CREDIT, entry.type());
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), Currency.INR);
    }
}