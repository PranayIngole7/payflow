package com.payflow.transaction.application;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentStatus;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreatePaymentUseCaseTest {

    @Test
    void shouldCreateAndSavePayment() {

        PaymentRepository repository =
                mock(PaymentRepository.class);

        CreatePaymentUseCase useCase =
                new CreatePaymentUseCase(repository);

        AccountId accountId =
                new AccountId(UUID.randomUUID());

        WalletId sourceWalletId =
                new WalletId(UUID.randomUUID());

        WalletId destinationWalletId =
                new WalletId(UUID.randomUUID());

        Money amount =
                new Money(
                        new BigDecimal("100.00"),
                        Currency.INR
                );

        CreatePaymentCommand command =
                new CreatePaymentCommand(
                        "PAY-001",
                        accountId,
                        sourceWalletId,
                        destinationWalletId,
                        amount,
                        TransactionType.TRANSFER
                );

        Payment result = useCase.execute(command);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("PAY-001", result.reference());
        assertEquals(accountId, result.accountId());
        assertEquals(sourceWalletId, result.sourceWalletId());
        assertEquals(destinationWalletId, result.destinationWalletId());
        assertEquals(amount, result.amount());
        assertEquals(
                TransactionType.TRANSFER,
                result.type()
        );
        assertEquals(
                PaymentStatus.PENDING,
                result.status()
        );

        verify(repository).save(result);
    }

    @Test
    void shouldCreatePaymentWithPendingStatus() {

        PaymentRepository repository =
                mock(PaymentRepository.class);

        CreatePaymentUseCase useCase =
                new CreatePaymentUseCase(repository);

        CreatePaymentCommand command =
                new CreatePaymentCommand(
                        "PAY-002",
                        new AccountId(UUID.randomUUID()),
                        new WalletId(UUID.randomUUID()),
                        new WalletId(UUID.randomUUID()),
                        new Money(
                                new BigDecimal("50.00"),
                                Currency.INR
                        ),
                        TransactionType.TRANSFER
                );

        Payment result = useCase.execute(command);

        assertEquals(
                PaymentStatus.PENDING,
                result.status()
        );
    }

    @Test
    void shouldNotSaveWhenPaymentCreationFails() {

        PaymentRepository repository =
                mock(PaymentRepository.class);

        CreatePaymentUseCase useCase =
                new CreatePaymentUseCase(repository);

        CreatePaymentCommand command =
                new CreatePaymentCommand(
                        "PAY-003",
                        new AccountId(UUID.randomUUID()),
                        new WalletId(UUID.randomUUID()),
                        new WalletId(UUID.randomUUID()),
                        new Money(
                                BigDecimal.ZERO,
                                Currency.INR
                        ),
                        TransactionType.TRANSFER
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(command)
        );

        verify(repository, never()).save(any());
    }
}
