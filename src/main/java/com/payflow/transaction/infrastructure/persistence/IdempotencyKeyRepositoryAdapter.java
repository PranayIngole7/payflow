package com.payflow.transaction.infrastructure.persistence;

import com.payflow.account.domain.AccountId;
import com.payflow.transaction.application.IdempotencyKeyRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class IdempotencyKeyRepositoryAdapter
        implements IdempotencyKeyRepository {

    private final SpringDataIdempotencyKeyRepository repository;

    public IdempotencyKeyRepositoryAdapter(
            SpringDataIdempotencyKeyRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Optional<IdempotencyRecord> findByAccountIdAndKey(
            AccountId accountId,
            String key
    ) {
        return repository
                .findByUserIdAndKey(accountId.value(), key)
                .map(entity ->
                        new IdempotencyRecord(
                                new AccountId(entity.getUserId()),
                                entity.getKey(),
                                entity.getRequestHash(),
                                entity.getPaymentId(),
                                entity.getCreatedAt(),
                                entity.getExpiresAt()
                        )
                );
    }

    @Override
    public void save(
            AccountId accountId,
            String key,
            String requestHash,
            UUID paymentId,
            Instant createdAt,
            Instant expiresAt
    ) {
        IdempotencyKeyEntity entity =
                new IdempotencyKeyEntity(
                        UUID.nameUUIDFromBytes(
                                (accountId.value() + ":" + key)
                                        .getBytes(
                                                java.nio.charset.StandardCharsets.UTF_8
                                        )
                        ),
                        accountId.value(),
                        key,
                        requestHash,
                        paymentId,
                        createdAt,
                        expiresAt
                );

        repository.save(entity);
    }
}
