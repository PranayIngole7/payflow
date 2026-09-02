package com.payflow.account.infrastructure.persistence;

import com.payflow.account.application.AccountRepository;
import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountRepositoryAdapter
        implements AccountRepository {

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
    public void save(Account account) {
        repository.save(toEntity(account));
    }

    private Account toDomain(AccountEntity entity) {
        return Account.reconstitute(
                new AccountId(entity.getId()),
                entity.getCreatedAt(),
                entity.getStatus()
        );
    }

    private AccountEntity toEntity(Account account) {
        return new AccountEntity(
                account.id().value(),
                account.status(),
                account.createdAt()
        );
    }
}