package com.payflow.transaction.application;

import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.transaction.domain.TransactionStatus;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InitiateTransferUseCaseTest {

    private TransactionRepository transactionRepository;
    private TransactionRunner transactionRunner;
    private InitiateTransferUseCase useCase;

    private WalletId sourceWalletId;
    private WalletId destinationWalletId;
    private Money amount;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        transactionRunner = mock(TransactionRunner.class);

        useCase = new InitiateTransferUseCase(
                transactionRepository,
                transactionRunner
        );

        sourceWalletId = new WalletId(UUID.randomUUID());
        destinationWalletId = new WalletId(UUID.randomUUID());

        amount = new Money(
                new BigDecimal("250.00"),
                Currency.INR
        );
    }

    @Test
    void shouldCreatePendingTransaction() {

        executeRunnableImmediately();

        TransactionId transactionId = useCase.execute(
                sourceWalletId,
                destinationWalletId,
                amount,
                "transfer-123"
        );

        assertNotNull(transactionId);

        verify(transactionRepository)
                .findByIdempotencyKey("transfer-123");

        verify(transactionRepository)
                .save(argThat(transaction ->
                        transaction.id().equals(transactionId)
                                && transaction.sourceWalletId()
                                .equals(sourceWalletId)
                                && transaction.destinationWalletId()
                                .equals(destinationWalletId)
                                && transaction.amount()
                                .equals(amount)
                                && transaction.idempotencyKey()
                                .equals("transfer-123")
                                && transaction.status()
                                == TransactionStatus.PENDING
                ));
    }

    @Test
    void shouldReturnExistingTransactionForSameIdempotencyKey() {

        executeRunnableImmediately();

        TransactionId existingId = TransactionId.generate();

        Transaction existing = Transaction.reconstitute(
                existingId,
                sourceWalletId,
                destinationWalletId,
                amount,
                Instant.now(),
                "transfer-123",
                TransactionStatus.PENDING
        );

        when(transactionRepository.findByIdempotencyKey("transfer-123"))
                .thenReturn(Optional.of(existing));

        TransactionId result = useCase.execute(
                sourceWalletId,
                destinationWalletId,
                amount,
                "transfer-123"
        );

        assertEquals(existingId, result);

        verify(transactionRepository)
                .findByIdempotencyKey("transfer-123");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void shouldReturnExistingTransactionForCompletedRequest() {

        executeRunnableImmediately();

        TransactionId existingId = TransactionId.generate();

        Transaction existing = Transaction.reconstitute(
                existingId,
                sourceWalletId,
                destinationWalletId,
                amount,
                Instant.now(),
                "transfer-123",
                TransactionStatus.COMPLETED
        );

        when(transactionRepository.findByIdempotencyKey("transfer-123"))
                .thenReturn(Optional.of(existing));

        TransactionId result = useCase.execute(
                sourceWalletId,
                destinationWalletId,
                amount,
                "transfer-123"
        );

        assertEquals(existingId, result);

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void shouldRejectIdempotencyKeyUsedForDifferentRequest() {

        executeRunnableImmediately();

        Transaction existing = Transaction.reconstitute(
                TransactionId.generate(),
                sourceWalletId,
                destinationWalletId,
                new Money(
                        new BigDecimal("500.00"),
                        Currency.INR
                ),
                Instant.now(),
                "transfer-123",
                TransactionStatus.PENDING
        );

        when(transactionRepository.findByIdempotencyKey("transfer-123"))
                .thenReturn(Optional.of(existing));

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> useCase.execute(
                                sourceWalletId,
                                destinationWalletId,
                                amount,
                                "transfer-123"
                        )
                );

        assertEquals(
                "idempotency key has already been used "
                        + "for a different transaction",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void shouldNormalizeIdempotencyKey() {

        executeRunnableImmediately();

        TransactionId transactionId = useCase.execute(
                sourceWalletId,
                destinationWalletId,
                amount,
                "  transfer-123  "
        );

        assertNotNull(transactionId);

        verify(transactionRepository)
                .findByIdempotencyKey("transfer-123");

        verify(transactionRepository)
                .save(argThat(transaction ->
                        transaction.idempotencyKey()
                                .equals("transfer-123")
                ));
    }

    @Test
    void shouldRejectNullIdempotencyKey() {

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(
                        sourceWalletId,
                        destinationWalletId,
                        amount,
                        null
                )
        );

        verifyNoInteractions(
                transactionRepository,
                transactionRunner
        );
    }

    @Test
    void shouldRejectBlankIdempotencyKey() {

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        sourceWalletId,
                        destinationWalletId,
                        amount,
                        "   "
                )
        );

        verifyNoInteractions(
                transactionRepository,
                transactionRunner
        );
    }

    @Test
    void shouldRejectIdempotencyKeyLongerThan100Characters() {

        String longKey = "a".repeat(101);

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        sourceWalletId,
                        destinationWalletId,
                        amount,
                        longKey
                )
        );

        verifyNoInteractions(
                transactionRepository,
                transactionRunner
        );
    }

    @Test
    void shouldRejectNullSourceWallet() {

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(
                        null,
                        destinationWalletId,
                        amount,
                        "transfer-123"
                )
        );

        verifyNoInteractions(
                transactionRepository,
                transactionRunner
        );
    }

    @Test
    void shouldRejectNullDestinationWallet() {

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(
                        sourceWalletId,
                        null,
                        amount,
                        "transfer-123"
                )
        );

        verifyNoInteractions(
                transactionRepository,
                transactionRunner
        );
    }

    @Test
    void shouldRejectNullAmount() {

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(
                        sourceWalletId,
                        destinationWalletId,
                        null,
                        "transfer-123"
                )
        );

        verifyNoInteractions(
                transactionRepository,
                transactionRunner
        );
    }

    private void executeRunnableImmediately() {

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(transactionRunner)
                .execute(any(Runnable.class));
    }
}