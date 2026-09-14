package com.payflow.transaction.application;

import com.payflow.transaction.domain.Payment;

public interface PaymentRequestHasher {

    String hash(Payment payment);
}
