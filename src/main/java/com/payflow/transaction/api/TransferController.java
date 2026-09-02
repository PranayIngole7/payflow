package com.payflow.transaction.api;

import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.ExecuteTransferUseCase;
import com.payflow.transaction.application.InitiateTransferUseCase;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.domain.WalletId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * REST API for transfer operations.
 */
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private static final String IDEMPOTENCY_KEY_HEADER =
            "Idempotency-Key";

    private final InitiateTransferUseCase initiateTransferUseCase;
    private final ExecuteTransferUseCase executeTransferUseCase;

    public TransferController(
            InitiateTransferUseCase initiateTransferUseCase,
            ExecuteTransferUseCase executeTransferUseCase
    ) {
        this.initiateTransferUseCase =
                Objects.requireNonNull(
                        initiateTransferUseCase,
                        "initiate transfer use case must not be null"
                );

        this.executeTransferUseCase =
                Objects.requireNonNull(
                        executeTransferUseCase,
                        "execute transfer use case must not be null"
                );
    }

    /**
     * Initiates and executes a wallet-to-wallet transfer.
     *
     * <p>The idempotency key makes retries safe. If the transaction was
     * already completed, the existing transaction ID is returned.</p>
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InitiateTransferResponse initiateTransfer(
            @Valid @RequestBody InitiateTransferRequest request,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER)
            String idempotencyKey
    ) {
        TransactionId transactionId =
                initiateTransferUseCase.execute(
                        new WalletId(request.sourceWalletId()),
                        new WalletId(request.destinationWalletId()),
                        new Money(
                                request.amount(),
                                request.currency()
                        ),
                        idempotencyKey
                );

        executeTransferUseCase.execute(transactionId);

        return InitiateTransferResponse.from(transactionId);
    }
}