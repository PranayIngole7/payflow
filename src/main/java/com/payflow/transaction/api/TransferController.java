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

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final InitiateTransferUseCase initiateTransferUseCase;
    private final ExecuteTransferUseCase executeTransferUseCase;

    public TransferController(
            InitiateTransferUseCase initiateTransferUseCase,
            ExecuteTransferUseCase executeTransferUseCase
    ) {
        this.initiateTransferUseCase = Objects.requireNonNull(
                initiateTransferUseCase,
                "initiate transfer use case must not be null"
        );

        this.executeTransferUseCase = Objects.requireNonNull(
                executeTransferUseCase,
                "execute transfer use case must not be null"
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InitiateTransferResponse initiateTransfer(
            @Valid @RequestBody InitiateTransferRequest request,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey
    ) {
        InitiateTransferUseCase.InitiationResult result =
                initiateTransferUseCase.execute(
                        new WalletId(request.sourceWalletId()),
                        new WalletId(request.destinationWalletId()),
                        new Money(request.amount(), request.currency()),
                        idempotencyKey
                );

        if (result.created()) {
            executeTransferUseCase.execute(result.transactionId());
        }

        return InitiateTransferResponse.from(result.transactionId());
    }
}