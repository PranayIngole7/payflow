package com.payflow.transaction.application;

import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;

    public CreatePaymentUseCase(
            PaymentRepository paymentRepository
    ) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment execute(
            CreatePaymentCommand command
    ) {
        Payment payment = Payment.create(
                PaymentId.generate(),
                command.reference(),
                command.accountId(),
                command.sourceWalletId(),
                command.destinationWalletId(),
                command.amount(),
                Instant.now(),
                command.type()
        );

        paymentRepository.save(payment);

        return payment;
    }
}
