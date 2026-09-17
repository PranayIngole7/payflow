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
        LedgerAccountId ledgerAccountId = LedgerAccountId.forWallet(WalletId.generate());
        Money amount = new Money(new BigDecimal("100.00"), Currency.INR);
        Instant createdAt = Instant.now();

        LedgerEntry entry = LedgerEntry.create(
                id,
                transactionId,
                ledgerAccountId,
                LedgerAccountType.WALLET,
                amount,
                LedgerEntryType.CREDIT,
                createdAt
        );

        assertEquals(id, entry.id());
        assertEquals(transactionId, entry.transactionId());
        assertEquals(ledgerAccountId, entry.ledgerAccountId());
        assertEquals(LedgerAccountType.WALLET, entry.accountType());
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
                        LedgerAccountId.forWallet(WalletId.generate()),
                        LedgerAccountType.WALLET,
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
                        LedgerAccountId.forWallet(WalletId.generate()),
                        LedgerAccountType.WALLET,
                        money("100.00"),
                        LedgerEntryType.CREDIT,
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullLedgerAccountId() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        LedgerEntryId.generate(),
                        TransactionId.generate(),
                        null,
                        LedgerAccountType.WALLET,
                        money("100.00"),
                        LedgerEntryType.CREDIT,
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullLedgerAccountType() {
        assertThrows(
                NullPointerException.class,
                () -> LedgerEntry.create(
                        LedgerEntryId.generate(),
                        TransactionId.generate(),
                        LedgerAccountId.forWallet(WalletId.generate()),
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
                        LedgerAccountId.forWallet(WalletId.generate()),
                        LedgerAccountType.WALLET,
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
                        LedgerAccountId.forWallet(WalletId.generate()),
                        LedgerAccountType.WALLET,
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
                        LedgerAccountId.forWallet(WalletId.generate()),
                        LedgerAccountType.WALLET,
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
                        LedgerAccountId.forWallet(WalletId.generate()),
                        LedgerAccountType.WALLET,
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
                        LedgerAccountId.forWallet(WalletId.generate()),
                        LedgerAccountType.WALLET,
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
                LedgerAccountId.forWallet(WalletId.generate()),
                LedgerAccountType.WALLET,
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
                LedgerAccountId.forWallet(WalletId.generate()),
                LedgerAccountType.WALLET,
                money("100.00"),
                LedgerEntryType.CREDIT,
                Instant.now()
        );

        assertEquals(LedgerEntryType.CREDIT, entry.type());
    }

    @Test
    void shouldSupportExternalFundingAccount() {
        LedgerEntry entry = LedgerEntry.create(
                LedgerEntryId.generate(),
                TransactionId.generate(),
                LedgerAccountId.externalFunding(),
                LedgerAccountType.EXTERNAL_FUNDING,
                money("1000.00"),
                LedgerEntryType.DEBIT,
                Instant.now()
        );

        assertEquals(
                LedgerAccountType.EXTERNAL_FUNDING,
                entry.accountType()
        );
    }

    @Test
    void shouldSupportExternalSettlementAccount() {
        LedgerEntry entry = LedgerEntry.create(
                LedgerEntryId.generate(),
                TransactionId.generate(),
                LedgerAccountId.externalSettlement(),
                LedgerAccountType.EXTERNAL_SETTLEMENT,
                money("500.00"),
                LedgerEntryType.CREDIT,
                Instant.now()
        );

        assertEquals(
                LedgerAccountType.EXTERNAL_SETTLEMENT,
                entry.accountType()
        );
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), Currency.INR);
    }
}
