package com.payflow.ledger.infrastructure.persistence;

import com.payflow.ledger.application.LedgerRepository;
import com.payflow.ledger.domain.Ledger;
import com.payflow.ledger.domain.LedgerAccountId;
import com.payflow.ledger.domain.LedgerEntry;
import com.payflow.ledger.domain.LedgerEntryId;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.TransactionId;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LedgerRepositoryAdapter implements LedgerRepository {

    private final SpringDataLedgerEntryRepository repository;
    private final EntityManager entityManager;

    public LedgerRepositoryAdapter(
            SpringDataLedgerEntryRepository repository,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Ledger> findByTransactionId(
            TransactionId transactionId
    ) {
        List<LedgerEntryEntity> entities =
                repository.findByTransactionIdOrderByCreatedAtAsc(
                        transactionId.value()
                );

        if (entities.isEmpty()) {
            return Optional.empty();
        }

        List<LedgerEntry> entries = entities.stream()
                .map(this::toDomain)
                .toList();

        return Optional.of(
                Ledger.reconstitute(transactionId, entries)
        );
    }

    @Override
    public void save(Ledger ledger) {
        for (LedgerEntry entry : ledger.entries()) {
            entityManager.persist(toEntity(entry));
        }
    }

    private LedgerEntry toDomain(LedgerEntryEntity entity) {
        return LedgerEntry.create(
                new LedgerEntryId(entity.getId()),
                new TransactionId(entity.getTransactionId()),
                new LedgerAccountId(entity.getLedgerAccountId()),
                entity.getAccountType(),
                new Money(
                        entity.getAmount(),
                        entity.getCurrency()
                ),
                entity.getType(),
                entity.getCreatedAt()
        );
    }

    private LedgerEntryEntity toEntity(LedgerEntry entry) {
        return new LedgerEntryEntity(
                entry.id().value(),
                entry.transactionId().value(),
                entry.ledgerAccountId().value(),
                entry.accountType(),
                entry.amount().amount(),
                entry.currency(),
                entry.type(),
                entry.createdAt()
        );
    }
}