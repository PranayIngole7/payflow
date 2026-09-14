package com.payflow.transaction.api;

import com.payflow.transaction.application.CreatePaymentUseCase;
import com.payflow.transaction.application.FailPaymentUseCase;
import com.payflow.transaction.application.GetPaymentHistoryUseCase;
import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;
    private final GetPaymentHistoryUseCase getPaymentHistoryUseCase;
    private final FailPaymentUseCase failPaymentUseCase;

    public PaymentController(
            CreatePaymentUseCase createPaymentUseCase,
            GetPaymentHistoryUseCase getPaymentHistoryUseCase,
            FailPaymentUseCase failPaymentUseCase
    ) {
        this.createPaymentUseCase = createPaymentUseCase;
        this.getPaymentHistoryUseCase = getPaymentHistoryUseCase;
        this.failPaymentUseCase = failPaymentUseCase;
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
                        .execute(new com.payflow.account.domain.AccountId(accountId))
                        .stream()
                        .map(PaymentResponse::from)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/failure")
    public ResponseEntity<PaymentResponse> failPayment(
            @PathVariable UUID paymentId
    ) {
        Payment payment =
                failPaymentUseCase.execute(
                        new PaymentId(paymentId)
                );

        return ResponseEntity.ok(
                PaymentResponse.from(payment)
        );
    }
}