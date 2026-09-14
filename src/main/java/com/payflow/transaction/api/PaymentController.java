package com.payflow.transaction.api;

import com.payflow.account.domain.AccountId;
import com.payflow.transaction.application.CreatePaymentUseCase;
import com.payflow.transaction.application.GetPaymentHistoryUseCase;
import com.payflow.transaction.domain.Payment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;
    private final GetPaymentHistoryUseCase getPaymentHistoryUseCase;

    public PaymentController(
            CreatePaymentUseCase createPaymentUseCase,
            GetPaymentHistoryUseCase getPaymentHistoryUseCase
    ) {
        this.createPaymentUseCase = createPaymentUseCase;
        this.getPaymentHistoryUseCase = getPaymentHistoryUseCase;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        Payment payment =
                createPaymentUseCase.execute(
                        request.toCommand(),
                        idempotencyKey
                );

        PaymentResponse response =
                PaymentResponse.from(payment);

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/v1/payments/" + payment.id().value()
                        )
                )
                .body(response);
    }
    
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(
            @RequestParam UUID accountId
    ) {
        List<PaymentResponse> response =
                getPaymentHistoryUseCase
                        .execute(new AccountId(accountId))
                        .stream()
                        .map(PaymentResponse::from)
                        .toList();

        return ResponseEntity.ok(response);
    }
}