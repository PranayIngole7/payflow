package com.payflow.transaction.application;

import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentId;

import java.util.Optional;

public interface PaymentRepository {

    void save(Payment payment);

    Optional<Payment> findById(PaymentId paymentId);
}
