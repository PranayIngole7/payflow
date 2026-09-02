package com.payflow.transaction.application;

import com.payflow.transaction.domain.TransactionId;

import java.util.Objects;

/**
 * Application use case responsible for executing an initiated transfer.
 *
 * <p>The actual financial coordination remains inside
 * {@link TransferMoneyUseCase}. This class provides the application
 * boundary used by external adapters such as REST controllers.</p>
 */
public final class ExecuteTransferUseCase {

    private final TransferMoneyUseCase transferMoneyUseCase;

    public ExecuteTransferUseCase(
            TransferMoneyUseCase transferMoneyUseCase
    ) {
        this.transferMoneyUseCase = Objects.requireNonNull(
                transferMoneyUseCase,
                "transfer money use case must not be null"
        );
    }

    /**
     * Executes the transfer identified by the transaction ID.
     */
    public void execute(TransactionId transactionId) {
        Objects.requireNonNull(
                transactionId,
                "transaction id must not be null"
        );

        transferMoneyUseCase.execute(transactionId);
    }
}