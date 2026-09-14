package com.payflow.transaction.infrastructure.persistence;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentId;
import com.payflow.transaction.domain.PaymentStatus;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRepositoryAdapterTest {

    @Test
    void shouldSavePayment() {

        UUID paymentId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destinationWalletId = UUID.randomUUID();

        Instant createdAt = Instant.now();

        Payment payment = Payment.create(
                new PaymentId(paymentId),
                "PAY-001",
                new AccountId(accountId),
                new WalletId(sourceWalletId),
                new WalletId(destinationWalletId),
                new Money(
                        new BigDecimal("100.00"),
                        Currency.INR
                ),
                createdAt,
                TransactionType.TRANSFER
        );

        SpringDataPaymentRepository repository =
                org.mockito.Mockito.mock(
                        SpringDataPaymentRepository.class
                );

        PaymentRepositoryAdapter adapter =
                new PaymentRepositoryAdapter(repository);

        adapter.save(payment);

        org.mockito.Mockito.verify(repository)
                .save(org.mockito.ArgumentMatchers.argThat(
                        entity ->
                                entity.getId().equals(paymentId)
                                        && entity.getReference()
                                                .equals("PAY-001")
                                        && entity.getUserId()
                                                .equals(accountId)
                                        && entity.getSourceWalletId()
                                                .equals(sourceWalletId)
                                        && entity.getDestinationWalletId()
                                                .equals(destinationWalletId)
                                        && entity.getType()
                                                .equals("TRANSFER")
                                        && entity.getAmount()
                                                .equals(new BigDecimal("100.00"))
                                        && entity.getCurrency()
                                                == Currency.INR
                                        && entity.getStatus()
                                                == PaymentEntity.PaymentStatusValue.PENDING
                                        && entity.getCreatedAt()
                                                .equals(createdAt)
                                        && entity.getUpdatedAt()
                                                .equals(createdAt)
                ));
    }

    @Test
    void shouldFindPaymentById() {

        UUID paymentId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destinationWalletId = UUID.randomUUID();

        Instant createdAt = Instant.now();

        PaymentEntity entity = new PaymentEntity(
                paymentId,
                "PAY-002",
                accountId,
                sourceWalletId,
                destinationWalletId,
                "TRANSFER",
                new BigDecimal("250.00"),
                Currency.INR,
                PaymentEntity.PaymentStatusValue.PENDING,
                createdAt,
                createdAt
        );

        SpringDataPaymentRepository repository =
                org.mockito.Mockito.mock(
                        SpringDataPaymentRepository.class
                );

        org.mockito.Mockito.when(
                repository.findById(paymentId)
        ).thenReturn(Optional.of(entity));

        PaymentRepositoryAdapter adapter =
                new PaymentRepositoryAdapter(repository);

        Optional<Payment> result =
                adapter.findById(new PaymentId(paymentId));

        assertTrue(result.isPresent());

        Payment payment = result.get();

        assertEquals(
                new PaymentId(paymentId),
                payment.id()
        );
        assertEquals(
                "PAY-002",
                payment.reference()
        );
        assertEquals(
                new AccountId(accountId),
                payment.accountId()
        );
        assertEquals(
                new WalletId(sourceWalletId),
                payment.sourceWalletId()
        );
        assertEquals(
                new WalletId(destinationWalletId),
                payment.destinationWalletId()
        );
        assertEquals(
                TransactionType.TRANSFER,
                payment.type()
        );
        assertEquals(
                new Money(
                        new BigDecimal("250.00"),
                        Currency.INR
                ),
                payment.amount()
        );
        assertEquals(
                PaymentStatus.PENDING,
                payment.status()
        );
        assertEquals(
                createdAt,
                payment.createdAt()
        );
        assertEquals(
                createdAt,
                payment.updatedAt()
        );
    }

    @Test
    void shouldReturnEmptyWhenPaymentDoesNotExist() {

        UUID paymentId = UUID.randomUUID();

        SpringDataPaymentRepository repository =
                org.mockito.Mockito.mock(
                        SpringDataPaymentRepository.class
                );

        org.mockito.Mockito.when(
                repository.findById(paymentId)
        ).thenReturn(Optional.empty());

        PaymentRepositoryAdapter adapter =
                new PaymentRepositoryAdapter(repository);

        Optional<Payment> result =
                adapter.findById(new PaymentId(paymentId));

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldPersistCompletedPaymentStatus() {

        Payment payment = Payment.create(
                PaymentId.generate(),
                "PAY-003",
                new AccountId(UUID.randomUUID()),
                new WalletId(UUID.randomUUID()),
                new WalletId(UUID.randomUUID()),
                new Money(
                        new BigDecimal("50.00"),
                        Currency.INR
                ),
                Instant.now(),
                TransactionType.TRANSFER
        );

        Instant completedAt = Instant.now();
        payment.complete(completedAt);

        SpringDataPaymentRepository repository =
                org.mockito.Mockito.mock(
                        SpringDataPaymentRepository.class
                );

        PaymentRepositoryAdapter adapter =
                new PaymentRepositoryAdapter(repository);

        adapter.save(payment);

        org.mockito.Mockito.verify(repository)
                .save(org.mockito.ArgumentMatchers.argThat(
                        entity ->
                                entity.getStatus()
                                        == PaymentEntity.PaymentStatusValue.COMPLETED
                                        && entity.getUpdatedAt()
                                                .equals(completedAt)
                ));
    }

    @Test
    void shouldPersistFailedPaymentStatus() {

        Payment payment = Payment.create(
                PaymentId.generate(),
                "PAY-004",
                new AccountId(UUID.randomUUID()),
                new WalletId(UUID.randomUUID()),
                new WalletId(UUID.randomUUID()),
                new Money(
                        new BigDecimal("75.00"),
                        Currency.INR
                ),
                Instant.now(),
                TransactionType.TRANSFER
        );

        Instant failedAt = Instant.now();
        payment.fail(failedAt);

        SpringDataPaymentRepository repository =
                org.mockito.Mockito.mock(
                        SpringDataPaymentRepository.class
                );

        PaymentRepositoryAdapter adapter =
                new PaymentRepositoryAdapter(repository);

        adapter.save(payment);

        org.mockito.Mockito.verify(repository)
                .save(org.mockito.ArgumentMatchers.argThat(
                        entity ->
                                entity.getStatus()
                                        == PaymentEntity.PaymentStatusValue.FAILED
                                        && entity.getUpdatedAt()
                                                .equals(failedAt)
                ));
    }

    @Test
    void shouldPersistCancelledPaymentStatus() {

        Payment payment = Payment.create(
                PaymentId.generate(),
                "PAY-005",
                new AccountId(UUID.randomUUID()),
                new WalletId(UUID.randomUUID()),
                new WalletId(UUID.randomUUID()),
                new Money(
                        new BigDecimal("80.00"),
                        Currency.INR
                ),
                Instant.now(),
                TransactionType.TRANSFER
        );

        Instant cancelledAt = Instant.now();
        payment.cancel(cancelledAt);

        SpringDataPaymentRepository repository =
                org.mockito.Mockito.mock(
                        SpringDataPaymentRepository.class
                );

        PaymentRepositoryAdapter adapter =
                new PaymentRepositoryAdapter(repository);

        adapter.save(payment);

        org.mockito.Mockito.verify(repository)
                .save(org.mockito.ArgumentMatchers.argThat(
                        entity ->
                                entity.getStatus()
                                        == PaymentEntity.PaymentStatusValue.CANCELLED
                                        && entity.getUpdatedAt()
                                                .equals(cancelledAt)
                ));
    }
}
