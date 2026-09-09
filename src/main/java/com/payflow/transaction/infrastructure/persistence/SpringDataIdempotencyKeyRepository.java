package com.payflow.transaction.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataIdempotencyKeyRepository
        extends JpaRepository<IdempotencyKeyEntity, UUID> {

    Optional<IdempotencyKeyEntity> findByKey(String key);

    Optional<IdempotencyKeyEntity> findByPaymentId(UUID paymentId);
}