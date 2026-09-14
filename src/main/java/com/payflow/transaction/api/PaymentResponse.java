package com.payflow.transaction.api;

import com.payflow.transaction.domain.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String reference,
        UUID accountId,
        UUID sourceWalletId,
        UUID destinationWalletId,
        String type,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.id().value(),
                payment.reference(),
                payment.accountId().value(),
                payment.sourceWalletId() == null
                        ? null
                        : payment.sourceWalletId().value(),
                payment.destinationWalletId() == null
                        ? null
                        : payment.destinationWalletId().value(),
                payment.type().name(),
                payment.amount().amount(),
                payment.currency().name(),
                payment.status().name(),
                payment.createdAt(),
                payment.updatedAt()
        );
    }
}
