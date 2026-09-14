package com.payflow.transaction.application;

import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
public class FailPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public FailPaymentUseCase(
            PaymentRepository paymentRepository
    ) {
        this.paymentRepository = Objects.requireNonNull(
                paymentRepository,
                "payment repository must not be null"
        );
    }

    @Transactional
    public Payment execute(PaymentId paymentId) {
        Objects.requireNonNull(
                paymentId,
                "payment id must not be null"
        );

        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "payment not found: "
                                        + paymentId.value()
                        )
                );

        payment.fail(Instant.now());

        paymentRepository.save(payment);

        return payment;
    }
}
