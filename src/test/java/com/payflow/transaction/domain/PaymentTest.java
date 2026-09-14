package com.payflow.transaction.domain;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    void shouldCreatePendingPayment() {
        PaymentId paymentId =
                PaymentId.generate();

        AccountId accountId =
                AccountId.generate();

        WalletId sourceWalletId =
                WalletId.generate();

        WalletId destinationWalletId =
                WalletId.generate();

        Money amount = new Money(
                new BigDecimal("500.00"),
                Currency.INR
        );

        Instant createdAt = Instant.now();

        Payment payment = Payment.create(
                paymentId,
                "PAY-001",
                accountId,
                sourceWalletId,
                destinationWalletId,
                amount,
                createdAt,
                TransactionType.TRANSFER
        );

        assertEquals(
                paymentId,
                payment.id()
        );

        assertEquals(
                "PAY-001",
                payment.reference()
        );

        assertEquals(
                accountId,
                payment.accountId()
        );

        assertEquals(
                sourceWalletId,
                payment.sourceWalletId()
        );

        assertEquals(
                destinationWalletId,
                payment.destinationWalletId()
        );

        assertEquals(
                amount,
                payment.amount()
        );

        assertEquals(
                Currency.INR,
                payment.currency()
        );

        assertEquals(
                TransactionType.TRANSFER,
                payment.type()
        );

        assertEquals(
                createdAt,
                payment.createdAt()
        );

        assertEquals(
                createdAt,
                payment.updatedAt()
        );

        assertEquals(
                PaymentStatus.PENDING,
                payment.status()
        );
    }

    @Test
    void shouldCompletePendingPayment() {
        Payment payment =
                createPayment();

        Instant updatedAt =
                Instant.now();

        payment.complete(updatedAt);

        assertEquals(
                PaymentStatus.COMPLETED,
                payment.status()
        );

        assertEquals(
                updatedAt,
                payment.updatedAt()
        );
    }

    @Test
    void shouldFailPendingPayment() {
        Payment payment =
                createPayment();

        Instant updatedAt =
                Instant.now();

        payment.fail(updatedAt);

        assertEquals(
                PaymentStatus.FAILED,
                payment.status()
        );

        assertEquals(
                updatedAt,
                payment.updatedAt()
        );
    }

    @Test
    void shouldCancelPendingPayment() {
        Payment payment =
                createPayment();

        Instant updatedAt =
                Instant.now();

        payment.cancel(updatedAt);

        assertEquals(
                PaymentStatus.CANCELLED,
                payment.status()
        );

        assertEquals(
                updatedAt,
                payment.updatedAt()
        );
    }

    @Test
    void shouldRejectCompletingCompletedPayment() {
        Payment payment =
                createPayment();

        payment.complete(Instant.now());

        assertThrows(
                IllegalStateException.class,
                () -> payment.complete(Instant.now())
        );
    }

    @Test
    void shouldRejectFailingCompletedPayment() {
        Payment payment =
                createPayment();

        payment.complete(Instant.now());

        assertThrows(
                IllegalStateException.class,
                () -> payment.fail(Instant.now())
        );
    }

    @Test
    void shouldRejectCancellingCompletedPayment() {
        Payment payment =
                createPayment();

        payment.complete(Instant.now());

        assertThrows(
                IllegalStateException.class,
                () -> payment.cancel(Instant.now())
        );
    }

    @Test
    void shouldRejectCompletingFailedPayment() {
        Payment payment =
                createPayment();

        payment.fail(Instant.now());

        assertThrows(
                IllegalStateException.class,
                () -> payment.complete(Instant.now())
        );
    }

    @Test
    void shouldRejectFailingFailedPayment() {
        Payment payment =
                createPayment();

        payment.fail(Instant.now());

        assertThrows(
                IllegalStateException.class,
                () -> payment.fail(Instant.now())
        );
    }

    @Test
    void shouldRejectCancellingFailedPayment() {
        Payment payment =
                createPayment();

        payment.fail(Instant.now());

        assertThrows(
                IllegalStateException.class,
                () -> payment.cancel(Instant.now())
        );
    }

    @Test
    void shouldRejectCompletingCancelledPayment() {
        Payment payment =
                createPayment();

        payment.cancel(Instant.now());

        assertThrows(
                IllegalStateException.class,
                () -> payment.complete(Instant.now())
        );
    }

    @Test
    void shouldRejectFailingCancelledPayment() {
        Payment payment =
                createPayment();

        payment.cancel(Instant.now());

        assertThrows(
                IllegalStateException.class,
                () -> payment.fail(Instant.now())
        );
    }

    @Test
    void shouldRejectCancellingCancelledPayment() {
        Payment payment =
                createPayment();

        payment.cancel(Instant.now());

        assertThrows(
                IllegalStateException.class,
                () -> payment.cancel(Instant.now())
        );
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Payment.create(
                        PaymentId.generate(),
                        "PAY-001",
                        AccountId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        new Money(
                                BigDecimal.ZERO,
                                Currency.INR
                        ),
                        Instant.now(),
                        TransactionType.TRANSFER
                )
        );
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Payment.create(
                        PaymentId.generate(),
                        "PAY-001",
                        AccountId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        new Money(
                                new BigDecimal("-100.00"),
                                Currency.INR
                        ),
                        Instant.now(),
                        TransactionType.TRANSFER
                )
        );
    }

    @Test
    void shouldRejectSameSourceAndDestinationWallet() {
        WalletId walletId =
                WalletId.generate();

        assertThrows(
                IllegalArgumentException.class,
                () -> Payment.create(
                        PaymentId.generate(),
                        "PAY-001",
                        AccountId.generate(),
                        walletId,
                        walletId,
                        validAmount(),
                        Instant.now(),
                        TransactionType.TRANSFER
                )
        );
    }

    @Test
    void shouldRejectNullPaymentId() {
        assertThrows(
                NullPointerException.class,
                () -> Payment.create(
                        null,
                        "PAY-001",
                        AccountId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        validAmount(),
                        Instant.now(),
                        TransactionType.TRANSFER
                )
        );
    }

    @Test
    void shouldRejectBlankReference() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Payment.create(
                        PaymentId.generate(),
                        "   ",
                        AccountId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        validAmount(),
                        Instant.now(),
                        TransactionType.TRANSFER
                )
        );
    }

    @Test
    void shouldRejectNullAccountId() {
        assertThrows(
                NullPointerException.class,
                () -> Payment.create(
                        PaymentId.generate(),
                        "PAY-001",
                        null,
                        WalletId.generate(),
                        WalletId.generate(),
                        validAmount(),
                        Instant.now(),
                        TransactionType.TRANSFER
                )
        );
    }

    @Test
    void shouldRejectNullAmount() {
        assertThrows(
                NullPointerException.class,
                () -> Payment.create(
                        PaymentId.generate(),
                        "PAY-001",
                        AccountId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        null,
                        Instant.now(),
                        TransactionType.TRANSFER
                )
        );
    }

    @Test
    void shouldRejectNullType() {
        assertThrows(
                NullPointerException.class,
                () -> Payment.create(
                        PaymentId.generate(),
                        "PAY-001",
                        AccountId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        validAmount(),
                        Instant.now(),
                        null
                )
        );
    }

    @Test
    void shouldRejectNullCreatedAt() {
        assertThrows(
                NullPointerException.class,
                () -> Payment.create(
                        PaymentId.generate(),
                        "PAY-001",
                        AccountId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        validAmount(),
                        null,
                        TransactionType.TRANSFER
                )
        );
    }

    @Test
    void shouldCreateDepositPaymentWithDestinationWalletOnly() {
        WalletId destinationWalletId =
                WalletId.generate();

        Payment payment = Payment.create(
                PaymentId.generate(),
                "PAY-001",
                AccountId.generate(),
                null,
                destinationWalletId,
                validAmount(),
                Instant.now(),
                TransactionType.DEPOSIT
        );

        assertNull(
                payment.sourceWalletId()
        );

        assertEquals(
                destinationWalletId,
                payment.destinationWalletId()
        );

        assertEquals(
                TransactionType.DEPOSIT,
                payment.type()
        );
    }

    @Test
    void shouldRejectDepositPaymentWithoutDestinationWallet() {
        assertThrows(
        		 NullPointerException.class,
                () -> Payment.create(
                        PaymentId.generate(),
                        "PAY-001",
                        AccountId.generate(),
                        null,
                        null,
                        validAmount(),
                        Instant.now(),
                        TransactionType.DEPOSIT
                )
        );
    }

    @Test
    void shouldRejectDepositPaymentWithSourceWallet() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Payment.create(
                        PaymentId.generate(),
                        "PAY-001",
                        AccountId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        validAmount(),
                        Instant.now(),
                        TransactionType.DEPOSIT
                )
        );
    }

    @Test
    void shouldCreateWithdrawalPaymentWithSourceWalletOnly() {
        WalletId sourceWalletId =
                WalletId.generate();

        Payment payment = Payment.create(
                PaymentId.generate(),
                "PAY-001",
                AccountId.generate(),
                sourceWalletId,
                null,
                validAmount(),
                Instant.now(),
                TransactionType.WITHDRAWAL
        );

        assertEquals(
                sourceWalletId,
                payment.sourceWalletId()
        );

        assertNull(
                payment.destinationWalletId()
        );

        assertEquals(
                TransactionType.WITHDRAWAL,
                payment.type()
        );
    }

    @Test
    void shouldRejectWithdrawalPaymentWithoutSourceWallet() {
        assertThrows(
        		NullPointerException.class,
                () -> Payment.create(
                        PaymentId.generate(),
                        "PAY-001",
                        AccountId.generate(),
                        null,
                        null,
                        validAmount(),
                        Instant.now(),
                        TransactionType.WITHDRAWAL
                )
        );
    }

    @Test
    void shouldRejectWithdrawalPaymentWithDestinationWallet() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Payment.create(
                        PaymentId.generate(),
                        "PAY-001",
                        AccountId.generate(),
                        WalletId.generate(),
                        WalletId.generate(),
                        validAmount(),
                        Instant.now(),
                        TransactionType.WITHDRAWAL
                )
        );
    }

    @Test
    void shouldRejectNullUpdatedAtWhenCompletingPayment() {
        Payment payment =
                createPayment();

        assertThrows(
                NullPointerException.class,
                () -> payment.complete(null)
        );
    }

    @Test
    void shouldRejectNullUpdatedAtWhenFailingPayment() {
        Payment payment =
                createPayment();

        assertThrows(
                NullPointerException.class,
                () -> payment.fail(null)
        );
    }

    @Test
    void shouldRejectNullUpdatedAtWhenCancellingPayment() {
        Payment payment =
                createPayment();

        assertThrows(
                NullPointerException.class,
                () -> payment.cancel(null)
        );
    }

    private Payment createPayment() {
        return Payment.create(
                PaymentId.generate(),
                "PAY-001",
                AccountId.generate(),
                WalletId.generate(),
                WalletId.generate(),
                validAmount(),
                Instant.now(),
                TransactionType.TRANSFER
        );
    }

    private Money validAmount() {
        return new Money(
                new BigDecimal("100.00"),
                Currency.INR
        );
    }
}
