package com.payflow.transaction.application;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentId;
import com.payflow.transaction.domain.PaymentStatus;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FailPaymentUseCaseTest {

    private PaymentRepository paymentRepository;

    private FailPaymentUseCase useCase;

    private PaymentId paymentId;
    private AccountId accountId;
    private WalletId sourceWalletId;
    private WalletId destinationWalletId;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);

        useCase = new FailPaymentUseCase(
                paymentRepository
        );

        paymentId = PaymentId.generate();
        accountId = new AccountId(UUID.randomUUID());
        sourceWalletId = new WalletId(UUID.randomUUID());
        destinationWalletId = new WalletId(UUID.randomUUID());
    }

    @Test
    void shouldFailPendingPayment() {
        Payment payment = pendingPayment();

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        Payment result = useCase.execute(paymentId);

        assertThat(result.status())
                .isEqualTo(PaymentStatus.FAILED);

        assertThat(result.id())
                .isEqualTo(paymentId);

        verify(paymentRepository)
                .findById(paymentId);

        verify(paymentRepository)
                .save(payment);
    }

    @Test
    void shouldReturnFailedPayment() {
        Payment payment = pendingPayment();

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        Payment result = useCase.execute(paymentId);

        assertThat(result.status())
                .isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void shouldUpdatePaymentTimestampWhenFailed() {
        Payment payment = pendingPayment();

        Instant before = payment.updatedAt();

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        Payment result = useCase.execute(paymentId);

        assertThat(result.updatedAt())
                .isAfterOrEqualTo(before);
    }

    @Test
    void shouldNotSaveWhenPaymentDoesNotExist() {
        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                useCase.execute(paymentId)
        )
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("payment not found");

        verify(paymentRepository)
                .findById(paymentId);

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    @Test
    void shouldRejectNullPaymentId() {
        assertThatThrownBy(() ->
                useCase.execute(null)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage(
                        "payment id must not be null"
                );

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void shouldPropagateFailureWhenPaymentIsAlreadyCompleted() {
        Payment payment = pendingPayment();

        payment.complete(Instant.now());

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() ->
                useCase.execute(paymentId)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "cannot fail payment in status COMPLETED"
                );

        verify(paymentRepository)
                .findById(paymentId);

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    @Test
    void shouldRejectAlreadyFailedPayment() {
        Payment payment = pendingPayment();

        payment.fail(Instant.now());

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() ->
                useCase.execute(paymentId)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "cannot fail payment in status FAILED"
                );

        verify(paymentRepository)
                .findById(paymentId);

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    @Test
    void shouldRejectCancelledPayment() {
        Payment payment = pendingPayment();

        payment.cancel(Instant.now());

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() ->
                useCase.execute(paymentId)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "cannot fail payment in status CANCELLED"
                );

        verify(paymentRepository)
                .findById(paymentId);

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    @Test
    void shouldSaveSamePaymentInstanceAfterFailure() {
        Payment payment = pendingPayment();

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        useCase.execute(paymentId);

        ArgumentCaptor<Payment> captor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository)
                .save(captor.capture());

        assertThat(captor.getValue())
                .isSameAs(payment);

        assertThat(captor.getValue().status())
                .isEqualTo(PaymentStatus.FAILED);
    }

    private Payment pendingPayment() {
        return Payment.create(
                paymentId,
                "PAY-FAIL-001",
                accountId,
                sourceWalletId,
                destinationWalletId,
                new Money(
                        new BigDecimal("100.00"),
                        Currency.INR
                ),
                Instant.now(),
                TransactionType.TRANSFER
        );
    }
}
