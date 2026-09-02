package com.payflow.transaction.application;

import com.payflow.transaction.domain.TransactionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ExecuteTransferUseCaseTest {

    private TransferMoneyUseCase transferMoneyUseCase;
    private ExecuteTransferUseCase useCase;

    @BeforeEach
    void setUp() {
        transferMoneyUseCase = mock(TransferMoneyUseCase.class);

        useCase = new ExecuteTransferUseCase(
                transferMoneyUseCase
        );
    }

    @Test
    void shouldExecuteTransfer() {

        TransactionId transactionId =
                TransactionId.generate();

        useCase.execute(transactionId);

        verify(transferMoneyUseCase)
                .execute(transactionId);
    }

    @Test
    void shouldRejectNullTransactionId() {

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(null)
        );

        verifyNoInteractions(transferMoneyUseCase);
    }
}