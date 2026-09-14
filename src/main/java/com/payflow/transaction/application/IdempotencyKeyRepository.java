package com.payflow.transaction.application;

import com.payflow.account.domain.AccountId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository {

    Optional<IdempotencyRecord> findByAccountIdAndKey(
            AccountId accountId,
            String key
    );

    void save(
            AccountId accountId,
            String key,
            String requestHash,
            UUID paymentId,
            Instant createdAt,
            Instant expiresAt
    );

    record IdempotencyRecord(
            AccountId accountId,
            String key,
            String requestHash,
            UUID paymentId,
            Instant createdAt,
            Instant expiresAt
    ) {}
}
