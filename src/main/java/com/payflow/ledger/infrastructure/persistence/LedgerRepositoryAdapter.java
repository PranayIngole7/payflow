package com.payflow.ledger.infrastructure.persistence;

import com.payflow.ledger.application.LedgerRepository;
import com.payflow.ledger.domain.Ledger;
import com.payflow.ledger.domain.LedgerEntry;
import com.payflow.ledger.domain.LedgerEntryId;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.domain.WalletId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LedgerRepositoryAdapter implements LedgerRepository {

    private final SpringDataLedgerEntryRepository repository;

    public LedgerRepositoryAdapter(
            SpringDataLedgerEntryRepository repository
    ) {
        this.repository = repository;
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
            repository.save(toEntity(entry));
        }
    }

    private LedgerEntry toDomain(LedgerEntryEntity entity) {
        return LedgerEntry.create(
                new LedgerEntryId(entity.getId()),
                new TransactionId(entity.getTransactionId()),
                new WalletId(entity.getWalletId()),
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
                entry.walletId().value(),
                entry.amount().amount(),
                entry.currency(),
                entry.type(),
                entry.createdAt()
        );
    }
}