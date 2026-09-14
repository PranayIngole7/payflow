package com.payflow.transaction.application;

import com.payflow.transaction.domain.PaymentId;

public class PaymentNotFoundException
        extends RuntimeException {

    public PaymentNotFoundException(PaymentId paymentId) {
        super(
                "payment not found: "
                        + paymentId.value()
        );
    }
}
