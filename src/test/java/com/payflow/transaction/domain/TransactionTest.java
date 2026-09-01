package com.payflow.transaction.domain;

import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void shouldCreatePendingTransaction() {
        TransactionId transactionId = TransactionId.generate();
        WalletId sourceWalletId = WalletId.generate();
        WalletId destinationWalletId = WalletId.generate();
        Money amount = new Money(
                new BigDecimal("500.00"),
                Currency.INR
        );
        Instant createdAt = Instant.now();

        Transaction transaction = Transaction.create(
                transactionId,
                sourceWalletId,
                destinationWalletId,
                amount,
                createdAt
        );

        assertEquals(transactionId, transaction.id());
        assertEquals(sourceWalletId, transaction.sourceWalletId());
        assertEquals(
                destinationWalletId,
                transaction.destinationWalletId()
        );
        assertEquals(amount, transaction.amount());
        assertEquals(Currency.INR, transaction.currency());
        assertEquals(createdAt, transaction.createdAt());
        assertEquals(
                TransactionStatus.PENDING,
                transaction.status()
        );
    }

    @Test
    void shouldCompletePendingTransaction() {
        Transaction transaction = createTransaction();

        transaction.complete();

        assertEquals(
                TransactionStatus.COMPLETED,
                transaction.status()
        );
    }

    @Test
    void shouldFailPendingTransaction() {
        Transaction transaction = createTransaction();

        transaction.fail();

        assertEquals(
                TransactionStatus.FAILED,
                transaction.status()
        );
    }

    @Test
    void shouldRejectCompletingCompletedTransaction() {
        Transaction transaction = createTransaction();

        transaction.complete();

        assertThrows(
                IllegalStateException.class,
                transaction::complete
        );
    }

    @Test
    void shouldRejectFailingCompletedTransaction() {
        Transaction transaction = createTransaction();

        transaction.complete();

        assertThrows(
                IllegalStateException.class,
                transaction::fail
        );
    }

    @Test
    void shouldRejectCompletingFailedTransaction() {
        Transaction transaction = createTransaction();

        transaction.fail();

        assertThrows(
                IllegalStateException.class,
                transaction::complete
        );
    }

    @Test
    void shouldRejectFailingFailedTransaction() {
        Transaction transaction = createTransaction();

        transaction.fail();

        assertThrows(
                IllegalStateException.class,
                transaction::fail
        );
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Transaction.create(
                        TransactionId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        new Money(
                                BigDecimal.ZERO,
                                Currency.INR
                        ),
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Transaction.create(
                        TransactionId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        new Money(
                                new BigDecimal("-100.00"),
                                Currency.INR
                        ),
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectSameSourceAndDestinationWallet() {
        WalletId walletId = WalletId.generate();

        assertThrows(
                IllegalArgumentException.class,
                () -> Transaction.create(
                        TransactionId.generate(),
                        walletId,
                        walletId,
                        new Money(
                                new BigDecimal("100.00"),
                                Currency.INR
                        ),
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullTransactionId() {
        assertThrows(
                NullPointerException.class,
                () -> Transaction.create(
                        null,
                        WalletId.generate(),
                        WalletId.generate(),
                        validAmount(),
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullSourceWalletId() {
        assertThrows(
                NullPointerException.class,
                () -> Transaction.create(
                        TransactionId.generate(),
                        null,
                        WalletId.generate(),
                        validAmount(),
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullDestinationWalletId() {
        assertThrows(
                NullPointerException.class,
                () -> Transaction.create(
                        TransactionId.generate(),
                        WalletId.generate(),
                        null,
                        validAmount(),
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullAmount() {
        assertThrows(
                NullPointerException.class,
                () -> Transaction.create(
                        TransactionId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        null,
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectNullCreatedAt() {
        assertThrows(
                NullPointerException.class,
                () -> Transaction.create(
                        TransactionId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        validAmount(),
                        null
                )
        );
    }

    private Transaction createTransaction() {
        return Transaction.create(
                TransactionId.generate(),
                WalletId.generate(),
                WalletId.generate(),
                validAmount(),
                Instant.now()
        );
    }

    private Money validAmount() {
        return new Money(
                new BigDecimal("100.00"),
                Currency.INR
        );
    }
}
