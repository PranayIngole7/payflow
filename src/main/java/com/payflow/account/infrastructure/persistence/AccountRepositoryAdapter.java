package com.payflow.account.infrastructure.persistence;

import com.payflow.account.application.AccountRepository;
import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
public class AccountRepositoryAdapter implements AccountRepository {

    private final SpringDataAccountRepository repository;

    public AccountRepositoryAdapter(
            SpringDataAccountRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Optional<Account> findById(AccountId accountId) {
        return repository.findById(accountId.value())
                .map(this::toDomain);
    }

    @Override
    public Optional<Account> findByEmail(String email) {
        return repository.findByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public Optional<String> findPasswordHash(AccountId accountId) {
        return repository.findById(accountId.value())
                .map(AccountEntity::getPasswordHash);
    }

    @Override
    public void save(Account account, String passwordHash) {
        Instant updatedAt = account.createdAt();

        repository.save(
                new AccountEntity(
                        account.id().value(),
                        account.email(),
                        account.firstName(),
                        account.lastName(),
                        passwordHash,
                        account.status(),
                        account.createdAt(),
                        updatedAt
                )
        );
    }

    @Override
    public void update(Account account) {
        AccountEntity entity = repository.findById(account.id().value())
                .orElseThrow(() -> new NoSuchElementException(
                        "account not found: " + account.id().value()
                ));

        entity.update(
                account.firstName(),
                account.lastName(),
                account.status(),
                Instant.now()
        );

        repository.save(entity);
    }

    @Override
    public void updatePassword(
            AccountId accountId,
            String passwordHash
    ) {
        AccountEntity entity = repository.findById(accountId.value())
                .orElseThrow(() -> new NoSuchElementException(
                        "account not found: " + accountId.value()
                ));

        entity.updatePassword(
                passwordHash,
                Instant.now()
        );

        repository.save(entity);
    }

    private Account toDomain(AccountEntity entity) {
        return Account.reconstitute(
                new AccountId(entity.getId()),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getCreatedAt(),
                entity.getStatus()
        );
    }
}