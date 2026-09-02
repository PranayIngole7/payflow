package com.payflow.transaction.application;

import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;
import java.util.NoSuchElementException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetTransactionUseCaseTest {

    @Test
    void shouldReturnExistingTransaction() {
        TransactionRepository repository = mock(TransactionRepository.class);

        TransactionId transactionId = TransactionId.generate();
        WalletId sourceWalletId = new WalletId(UUID.randomUUID());
        WalletId destinationWalletId = new WalletId(UUID.randomUUID());

        Transaction transaction = Transaction.create(
                transactionId,
                sourceWalletId,
                destinationWalletId,
                new Money(new BigDecimal("250.00"), Currency.INR),
                Instant.parse("2026-09-02T10:00:00Z"),
                "get-test-001"
        );

        when(repository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        GetTransactionUseCase useCase =
                new GetTransactionUseCase(repository);

        Transaction result = useCase.execute(transactionId);

        assertSame(transaction, result);
        verify(repository).findById(transactionId);
    }

    @Test
    void shouldRejectUnknownTransaction() {
        TransactionRepository repository = mock(TransactionRepository.class);

        TransactionId transactionId = TransactionId.generate();

        when(repository.findById(transactionId))
                .thenReturn(Optional.empty());

        GetTransactionUseCase useCase =
                new GetTransactionUseCase(repository);

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> useCase.execute(transactionId)
        );

        assertTrue(exception.getMessage().contains("transaction not found"));
    }

    @Test
    void shouldRejectNullTransactionId() {
        TransactionRepository repository = mock(TransactionRepository.class);

        GetTransactionUseCase useCase =
                new GetTransactionUseCase(repository);

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(null)
        );

        verifyNoInteractions(repository);
    }
}