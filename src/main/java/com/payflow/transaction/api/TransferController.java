package com.payflow.transaction.api;

import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.InitiateTransferUseCase;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.domain.WalletId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * REST API for transfer operations.
 */
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private static final String IDEMPOTENCY_KEY_HEADER =
            "Idempotency-Key";

    private final InitiateTransferUseCase initiateTransferUseCase;

    public TransferController(
            InitiateTransferUseCase initiateTransferUseCase
    ) {
        this.initiateTransferUseCase =
                Objects.requireNonNull(
                        initiateTransferUseCase,
                        "initiate transfer use case must not be null"
                );
    }

    /**
     * Initiates a new transfer.
     *
     * <p>The request is protected by an idempotency key so clients can
     * safely retry the same request without creating duplicate
     * transactions.</p>
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

        return InitiateTransferResponse.from(transactionId);
    }
}