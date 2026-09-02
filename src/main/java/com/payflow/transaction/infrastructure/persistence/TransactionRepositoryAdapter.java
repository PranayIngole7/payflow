package com.payflow.transaction.infrastructure.persistence;

import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.TransactionRepository;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.domain.WalletId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TransactionRepositoryAdapter
        implements TransactionRepository {

    private final SpringDataTransactionRepository repository;

    public TransactionRepositoryAdapter(
            SpringDataTransactionRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Optional<Transaction> findById(
            TransactionId transactionId
    ) {
        return repository.findById(transactionId.value())
                .map(this::toDomain);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(
            String idempotencyKey
    ) {
        return repository.findByIdempotencyKey(idempotencyKey)
                .map(this::toDomain);
    }

    @Override
    public void save(Transaction transaction) {
        repository.save(toEntity(transaction));
    }

    private Transaction toDomain(
            TransactionEntity entity
    ) {
        return Transaction.reconstitute(
                new TransactionId(entity.getId()),
                new WalletId(entity.getSourceWalletId()),
                new WalletId(entity.getDestinationWalletId()),
                new Money(
                        entity.getAmount(),
                        entity.getCurrency()
                ),
                entity.getCreatedAt(),
                entity.getIdempotencyKey(),
                entity.getStatus()
        );
    }

    private TransactionEntity toEntity(
            Transaction transaction
    ) {
        return new TransactionEntity(
                transaction.id().value(),
                transaction.sourceWalletId().value(),
                transaction.destinationWalletId().value(),
                transaction.amount().amount(),
                transaction.currency(),
                transaction.createdAt(),
                transaction.idempotencyKey(),
                transaction.status()
        );
    }
}