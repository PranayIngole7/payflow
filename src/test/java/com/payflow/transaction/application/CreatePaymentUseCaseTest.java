package com.payflow.transaction.application;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentStatus;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private PaymentRequestHasher paymentRequestHasher;

    private CreatePaymentUseCase useCase;

    private AccountId accountId;
    private WalletId sourceWalletId;
    private WalletId destinationWalletId;
    private CreatePaymentCommand command;

    @BeforeEach
    void setUp() {
        useCase = new CreatePaymentUseCase(
                paymentRepository,
                idempotencyKeyRepository,
                paymentRequestHasher
        );

        accountId = new AccountId(UUID.randomUUID());
        sourceWalletId = new WalletId(UUID.randomUUID());
        destinationWalletId = new WalletId(UUID.randomUUID());

        command = new CreatePaymentCommand(
                "PAY-001",
                accountId,
                sourceWalletId,
                destinationWalletId,
                new Money(
                        new BigDecimal("100.00"),
                        Currency.INR
                ),
                TransactionType.TRANSFER
        );
    }

    @Test
    void shouldCreateAndSavePayment() {
        when(idempotencyKeyRepository.findByAccountIdAndKey(
                accountId,
                "KEY-001"
        )).thenReturn(Optional.empty());

        when(paymentRequestHasher.hash(any(Payment.class)))
                .thenReturn("hash-001");

        Payment result =
                useCase.execute(command, "KEY-001");

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.status());

        verify(paymentRepository).save(result);

        verify(idempotencyKeyRepository).save(
                eq(accountId),
                eq("KEY-001"),
                eq("hash-001"),
                eq(result.id().value()),
                eq(result.createdAt()),
                any()
        );
    }

    @Test
    void shouldReturnExistingPaymentForSameIdempotencyKey() {
        Payment existingPayment = Payment.create(
                com.payflow.transaction.domain.PaymentId.generate(),
                "PAY-001",
                accountId,
                sourceWalletId,
                destinationWalletId,
                new Money(
                        new BigDecimal("100.00"),
                        Currency.INR
                ),
                java.time.Instant.parse(
                        "2026-09-14T10:00:00Z"
                ),
                TransactionType.TRANSFER
        );

        when(idempotencyKeyRepository.findByAccountIdAndKey(
                accountId,
                "KEY-001"
        )).thenReturn(
                Optional.of(
                        new IdempotencyKeyRepository.IdempotencyRecord(
                                accountId,
                                "KEY-001",
                                "hash-001",
                                existingPayment.id().value(),
                                existingPayment.createdAt(),
                                existingPayment.createdAt()
                                        .plus(java.time.Duration.ofDays(1))
                        )
                )
        );

        when(paymentRequestHasher.hash(any(Payment.class)))
                .thenReturn("hash-001");

        when(paymentRepository.findById(existingPayment.id()))
                .thenReturn(Optional.of(existingPayment));

        Payment result =
                useCase.execute(command, "KEY-001");

        assertEquals(existingPayment.id(), result.id());
        assertEquals(existingPayment.reference(), result.reference());

        verify(paymentRepository, never()).save(any());
        verify(idempotencyKeyRepository, never()).save(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldRejectSameKeyForDifferentRequest() {
        when(idempotencyKeyRepository.findByAccountIdAndKey(
                accountId,
                "KEY-001"
        )).thenReturn(
                Optional.of(
                        new IdempotencyKeyRepository.IdempotencyRecord(
                                accountId,
                                "KEY-001",
                                "different-hash",
                                UUID.randomUUID(),
                                java.time.Instant.parse(
                                        "2026-09-14T10:00:00Z"
                                ),
                                java.time.Instant.parse(
                                        "2026-09-15T10:00:00Z"
                                )
                        )
                )
        );

        when(paymentRequestHasher.hash(any(Payment.class)))
                .thenReturn("hash-001");

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> useCase.execute(
                                command,
                                "KEY-001"
                        )
                );

        assertEquals(
                "idempotency key has already been used for a different payment",
                exception.getMessage()
        );

        verify(paymentRepository, never()).save(any());
        verify(idempotencyKeyRepository, never()).save(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldNormalizeIdempotencyKey() {
        when(idempotencyKeyRepository.findByAccountIdAndKey(
                accountId,
                "KEY-001"
        )).thenReturn(Optional.empty());

        when(paymentRequestHasher.hash(any(Payment.class)))
                .thenReturn("hash-001");

        useCase.execute(command, "  KEY-001  ");

        verify(idempotencyKeyRepository).findByAccountIdAndKey(
                accountId,
                "KEY-001"
        );

        verify(idempotencyKeyRepository).save(
                eq(accountId),
                eq("KEY-001"),
                eq("hash-001"),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldRejectNullIdempotencyKey() {
        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(command, null)
        );

        verifyNoInteractions(
                paymentRepository,
                idempotencyKeyRepository,
                paymentRequestHasher
        );
    }

    @Test
    void shouldRejectBlankIdempotencyKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(command, "   ")
        );

        verifyNoInteractions(
                paymentRepository,
                idempotencyKeyRepository,
                paymentRequestHasher
        );
    }

    @Test
    void shouldRejectIdempotencyKeyLongerThan100Characters() {
        String key = "a".repeat(101);

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(command, key)
        );

        verifyNoInteractions(
                paymentRepository,
                idempotencyKeyRepository,
                paymentRequestHasher
        );
    }

    @Test
    void shouldCreateSeparatePaymentForDifferentKey() {
        when(idempotencyKeyRepository.findByAccountIdAndKey(
                accountId,
                "KEY-001"
        )).thenReturn(Optional.empty());

        when(idempotencyKeyRepository.findByAccountIdAndKey(
                accountId,
                "KEY-002"
        )).thenReturn(Optional.empty());

        when(paymentRequestHasher.hash(any(Payment.class)))
                .thenReturn("hash-001", "hash-002");

        Payment first =
                useCase.execute(command, "KEY-001");

        Payment second =
                useCase.execute(command, "KEY-002");

        assertNotEquals(first.id(), second.id());

        verify(paymentRepository, times(2))
                .save(any(Payment.class));

        verify(idempotencyKeyRepository, times(2))
                .save(
                        eq(accountId),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        any()
                );
    }
}