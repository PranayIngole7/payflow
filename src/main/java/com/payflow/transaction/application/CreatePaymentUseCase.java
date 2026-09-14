package com.payflow.transaction.application;

import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

@Service
public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final PaymentRequestHasher paymentRequestHasher;

    public CreatePaymentUseCase(
            PaymentRepository paymentRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            PaymentRequestHasher paymentRequestHasher
    ) {
        this.paymentRepository = Objects.requireNonNull(
                paymentRepository,
                "payment repository must not be null"
        );
        this.idempotencyKeyRepository =
                Objects.requireNonNull(
                        idempotencyKeyRepository,
                        "idempotency key repository must not be null"
                );
        this.paymentRequestHasher =
                Objects.requireNonNull(
                        paymentRequestHasher,
                        "payment request hasher must not be null"
                );
    }

    @Transactional
    public Payment execute(
            CreatePaymentCommand command,
            String idempotencyKey
    ) {
        Objects.requireNonNull(
                command,
                "create payment command must not be null"
        );

        String normalizedKey =
                normalizeIdempotencyKey(idempotencyKey);

        Optional<IdempotencyKeyRepository.IdempotencyRecord> existing =
                idempotencyKeyRepository.findByAccountIdAndKey(
                        command.accountId(),
                        normalizedKey
                );

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

        String requestHash =
                paymentRequestHasher.hash(payment);

        if (existing.isPresent()) {
            IdempotencyKeyRepository.IdempotencyRecord record =
                    existing.get();

            if (!record.requestHash().equals(requestHash)) {
                throw new IllegalStateException(
                        "idempotency key has already been used for a different payment"
                );
            }

            return paymentRepository
                    .findById(new PaymentId(record.paymentId()))
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "payment referenced by idempotency key was not found"
                            )
                    );
        }

        paymentRepository.save(payment);

        Instant expiresAt =
                payment.createdAt().plus(1, ChronoUnit.DAYS);

        idempotencyKeyRepository.save(
                command.accountId(),
                normalizedKey,
                requestHash,
                payment.id().value(),
                payment.createdAt(),
                expiresAt
        );

        return payment;
    }

    private String normalizeIdempotencyKey(
            String idempotencyKey
    ) {
        Objects.requireNonNull(
                idempotencyKey,
                "idempotency key must not be null"
        );

        String normalized = idempotencyKey.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "idempotency key must not be blank"
            );
        }

        if (normalized.length() > 100) {
            throw new IllegalArgumentException(
                    "idempotency key must not exceed 100 characters"
            );
        }

        return normalized;
    }
}