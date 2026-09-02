package com.payflow.ledger.domain;

import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import com.payflow.transaction.domain.Transaction;

class LedgerTest {

    @Test
    void shouldStartEmpty() {
        Ledger ledger = Ledger.create();

        assertTrue(ledger.entries().isEmpty());
    }

    @Test
    void shouldAddEntry() {
        Ledger ledger = Ledger.create();

        LedgerEntry entry = createCreditEntry();

        ledger.add(entry);

        assertEquals(1, ledger.entries().size());
        assertEquals(entry, ledger.entries().getFirst());
    }

    @Test
    void shouldPreserveEntryOrder() {
        Ledger ledger = Ledger.create();
        TransactionId transactionId = TransactionId.generate();

        LedgerEntry first = createCreditEntry(transactionId);
        LedgerEntry second = createDebitEntry(transactionId);

        ledger.add(first);
        ledger.add(second);

        assertEquals(2, ledger.entries().size());
        assertEquals(first, ledger.entries().get(0));
        assertEquals(second, ledger.entries().get(1));
    }

    @Test
    void shouldRejectNullEntry() {
        Ledger ledger = Ledger.create();

        assertThrows(
                NullPointerException.class,
                () -> ledger.add(null)
        );
    }

    @Test
    void shouldNotAllowExternalModificationOfEntries() {
        Ledger ledger = Ledger.create();

        LedgerEntry entry = createCreditEntry();
        ledger.add(entry);

        assertThrows(
                UnsupportedOperationException.class,
                () -> ledger.entries().clear()
        );
    }

    @Test
    void shouldBeBalancedWhenDebitEqualsCredit() {
        Ledger ledger = Ledger.create();
        TransactionId transactionId = TransactionId.generate();

        LedgerEntry debit = createDebitEntry(transactionId);
        LedgerEntry credit = createCreditEntry(transactionId);

        ledger.add(debit);
        ledger.add(credit);

        assertTrue(ledger.isBalanced());
    }

    @Test
    void shouldNotBeBalancedWhenDebitAndCreditAreDifferent() {
        Ledger ledger = Ledger.create();
        TransactionId transactionId = TransactionId.generate();

        LedgerEntry debit = LedgerEntry.create(
                LedgerEntryId.generate(),
                transactionId,
                WalletId.generate(),
                new Money(
                        new BigDecimal("500.00"),
                        Currency.INR
                ),
                LedgerEntryType.DEBIT,
                Instant.now()
        );

        LedgerEntry credit = LedgerEntry.create(
                LedgerEntryId.generate(),
                transactionId,
                WalletId.generate(),
                new Money(
                        new BigDecimal("300.00"),
                        Currency.INR
                ),
                LedgerEntryType.CREDIT,
                Instant.now()
        );

        ledger.add(debit);
        ledger.add(credit);

        assertFalse(ledger.isBalanced());
    }

    @Test
    void shouldNotBeBalancedWhenLedgerHasOnlyDebitEntries() {
        Ledger ledger = Ledger.create();

        ledger.add(createDebitEntry());

        assertFalse(ledger.isBalanced());
    }

    @Test
    void shouldNotBeBalancedWhenLedgerHasOnlyCreditEntries() {
        Ledger ledger = Ledger.create();

        ledger.add(createCreditEntry());

        assertFalse(ledger.isBalanced());
    }

    @Test
    void shouldNotBeBalancedWhenLedgerIsEmpty() {
        Ledger ledger = Ledger.create();

        assertFalse(ledger.isBalanced());
    }

    @Test
    void shouldNotBeBalancedWhenDebitAndCreditHaveDifferentCurrencies() {
        Ledger ledger = Ledger.create();
        TransactionId transactionId = TransactionId.generate();

        LedgerEntry debit = LedgerEntry.create(
                LedgerEntryId.generate(),
                transactionId,
                WalletId.generate(),
                new Money(
                        new BigDecimal("500.00"),
                        Currency.INR
                ),
                LedgerEntryType.DEBIT,
                Instant.now()
        );

        LedgerEntry credit = LedgerEntry.create(
                LedgerEntryId.generate(),
                transactionId,
                WalletId.generate(),
                new Money(
                        new BigDecimal("500.00"),
                        Currency.USD
                ),
                LedgerEntryType.CREDIT,
                Instant.now()
        );

        ledger.add(debit);
        ledger.add(credit);

        assertFalse(ledger.isBalanced());
    }

    @Test
    void shouldBelongToSingleTransaction() {
        TransactionId transactionId = TransactionId.generate();

        Ledger ledger = Ledger.create();

        ledger.add(createDebitEntry(transactionId));
        ledger.add(createCreditEntry(transactionId));

        assertEquals(
                transactionId,
                ledger.transactionId().orElseThrow()
        );
    }

    @Test
    void shouldRejectEntryFromDifferentTransaction() {
        TransactionId firstTransaction = TransactionId.generate();
        TransactionId secondTransaction = TransactionId.generate();

        Ledger ledger = Ledger.create();

        ledger.add(createDebitEntry(firstTransaction));

        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.add(createCreditEntry(secondTransaction))
        );
    }

    @Test
    void emptyLedgerShouldNotHaveTransactionId() {
        Ledger ledger = Ledger.create();

        assertTrue(ledger.transactionId().isEmpty());
    }
    @Test
    void shouldCreateBalancedLedgerFromTransaction() {
        Transaction transaction = createTransaction();

        Ledger ledger = Ledger.from(transaction);

        assertTrue(ledger.isBalanced());
        assertEquals(2, ledger.entries().size());
        assertEquals(
                transaction.id(),
                ledger.transactionId().orElseThrow()
        );
    }

    @Test
    void shouldCreateDebitForSourceWallet() {
        Transaction transaction = createTransaction();

        Ledger ledger = Ledger.from(transaction);

        LedgerEntry debit = ledger.entries()
                .stream()
                .filter(entry -> entry.type() == LedgerEntryType.DEBIT)
                .findFirst()
                .orElseThrow();

        assertEquals(
                transaction.sourceWalletId(),
                debit.walletId()
        );

        assertEquals(
                transaction.amount(),
                debit.amount()
        );
    }

    @Test
    void shouldCreateCreditForDestinationWallet() {
        Transaction transaction = createTransaction();

        Ledger ledger = Ledger.from(transaction);

        LedgerEntry credit = ledger.entries()
                .stream()
                .filter(entry -> entry.type() == LedgerEntryType.CREDIT)
                .findFirst()
                .orElseThrow();

        assertEquals(
                transaction.destinationWalletId(),
                credit.walletId()
        );

        assertEquals(
                transaction.amount(),
                credit.amount()
        );
    }

    @Test
    void shouldUseSameTransactionIdForBothEntries() {
        Transaction transaction = createTransaction();

        Ledger ledger = Ledger.from(transaction);

        assertEquals(
                2,
                ledger.entries()
                        .stream()
                        .filter(entry ->
                                entry.transactionId().equals(transaction.id()))
                        .count()
        );
    }

    @Test
    void shouldCreateEntriesWithSameCurrencyAsTransaction() {
        Transaction transaction = createTransaction();

        Ledger ledger = Ledger.from(transaction);

        assertTrue(
                ledger.entries()
                        .stream()
                        .allMatch(entry ->
                                entry.currency() == transaction.currency())
        );
    }

    @Test
    void shouldRejectNullTransaction() {
        assertThrows(
                NullPointerException.class,
                () -> Ledger.from(null)
        );
    }

    @Test
    void shouldReconstituteLedgerWithPersistedEntries() {
        TransactionId transactionId = TransactionId.generate();
        LedgerEntry debit = createDebitEntry(transactionId);
        LedgerEntry credit = createCreditEntry(transactionId);

        Ledger ledger = Ledger.reconstitute(
                transactionId,
                java.util.List.of(debit, credit)
        );

        assertEquals(
                transactionId,
                ledger.transactionId().orElseThrow()
        );
        assertEquals(2, ledger.entries().size());
        assertEquals(debit, ledger.entries().get(0));
        assertEquals(credit, ledger.entries().get(1));
        assertTrue(ledger.isBalanced());
    }

    @Test
    void shouldRejectReconstitutedLedgerEntryFromDifferentTransaction() {
        TransactionId transactionId = TransactionId.generate();

        LedgerEntry validEntry = createDebitEntry(transactionId);
        LedgerEntry invalidEntry = createCreditEntry(
                TransactionId.generate()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Ledger.reconstitute(
                        transactionId,
                        java.util.List.of(validEntry, invalidEntry)
                )
        );
    }

    @Test
    void shouldRejectNullReconstitutedLedgerTransactionId() {
        assertThrows(
                NullPointerException.class,
                () -> Ledger.reconstitute(
                        null,
                        java.util.List.of()
                )
        );
    }

    @Test
    void shouldRejectNullReconstitutedLedgerEntries() {
        assertThrows(
                NullPointerException.class,
                () -> Ledger.reconstitute(
                        TransactionId.generate(),
                        null
                )
        );
    }

    @Test
    void shouldRejectNullEntryDuringLedgerReconstitution() {
        assertThrows(
                NullPointerException.class,
                () -> Ledger.reconstitute(
                        TransactionId.generate(),
                        java.util.List.of((LedgerEntry) null)
                )
        );
    }

    private Transaction createTransaction() {
        return Transaction.create(
                TransactionId.generate(),
                WalletId.generate(),
                WalletId.generate(),
                new Money(
                        new BigDecimal("500.00"),
                        Currency.INR
                ),
                Instant.now()
        );
    }

    private LedgerEntry createCreditEntry() {
        return createCreditEntry(TransactionId.generate());
    }

    private LedgerEntry createCreditEntry(TransactionId transactionId) {
        return LedgerEntry.create(
                LedgerEntryId.generate(),
                transactionId,
                WalletId.generate(),
                new Money(
                        new BigDecimal("500.00"),
                        Currency.INR
                ),
                LedgerEntryType.CREDIT,
                Instant.now()
        );
    }

    private LedgerEntry createDebitEntry() {
        return createDebitEntry(TransactionId.generate());
    }

    private LedgerEntry createDebitEntry(TransactionId transactionId) {
        return LedgerEntry.create(
                LedgerEntryId.generate(),
                transactionId,
                WalletId.generate(),
                new Money(
                        new BigDecimal("500.00"),
                        Currency.INR
                ),
                LedgerEntryType.DEBIT,
                Instant.now()
        );
    }
}