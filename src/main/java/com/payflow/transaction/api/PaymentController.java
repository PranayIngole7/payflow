package com.payflow.transaction.api;

import com.payflow.transaction.application.CreatePaymentUseCase;
import com.payflow.transaction.domain.Payment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;

    public PaymentController(
            CreatePaymentUseCase createPaymentUseCase
    ) {
        this.createPaymentUseCase = createPaymentUseCase;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request
    ) {
        Payment payment =
                createPaymentUseCase.execute(request.toCommand());

        PaymentResponse response =
                PaymentResponse.from(payment);

        return ResponseEntity
                .created(URI.create("/api/v1/payments/" + payment.id().value()))
                .body(response);
    }
}
