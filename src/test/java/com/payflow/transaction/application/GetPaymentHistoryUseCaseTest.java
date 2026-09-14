package com.payflow.transaction.application;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentId;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetPaymentHistoryUseCaseTest {

    @Test
    void shouldReturnPaymentHistoryForAccount() {
        PaymentRepository repository = mock(PaymentRepository.class);
        GetPaymentHistoryUseCase useCase =
                new GetPaymentHistoryUseCase(repository);

        AccountId accountId = new AccountId(UUID.randomUUID());

        Payment payment1 = Payment.create(
                PaymentId.generate(),
                "PAY-001",
                accountId,
                new WalletId(UUID.randomUUID()),
                new WalletId(UUID.randomUUID()),
                new Money(new BigDecimal("100.00"), Currency.INR),
                Instant.now(),
                TransactionType.TRANSFER
        );

        Payment payment2 = Payment.create(
                PaymentId.generate(),
                "PAY-002",
                accountId,
                new WalletId(UUID.randomUUID()),
                new WalletId(UUID.randomUUID()),
                new Money(new BigDecimal("200.00"), Currency.INR),
                Instant.now(),
                TransactionType.TRANSFER
        );

        when(repository.findByAccountId(accountId))
                .thenReturn(List.of(payment2, payment1));

        List<Payment> result = useCase.execute(accountId);

        assertEquals(2, result.size());
        assertEquals(payment2, result.get(0));
        assertEquals(payment1, result.get(1));

        verify(repository).findByAccountId(accountId);
    }

    @Test
    void shouldReturnEmptyListWhenAccountHasNoPayments() {
        PaymentRepository repository = mock(PaymentRepository.class);
        GetPaymentHistoryUseCase useCase =
                new GetPaymentHistoryUseCase(repository);

        AccountId accountId = new AccountId(UUID.randomUUID());

        when(repository.findByAccountId(accountId))
                .thenReturn(List.of());

        List<Payment> result = useCase.execute(accountId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(repository).findByAccountId(accountId);
    }

    @Test
    void shouldNotCallRepositoryMoreThanOnce() {
        PaymentRepository repository = mock(PaymentRepository.class);
        GetPaymentHistoryUseCase useCase =
                new GetPaymentHistoryUseCase(repository);

        AccountId accountId = new AccountId(UUID.randomUUID());

        when(repository.findByAccountId(accountId))
                .thenReturn(List.of());

        useCase.execute(accountId);

        verify(repository, times(1))
                .findByAccountId(accountId);
    }
}
