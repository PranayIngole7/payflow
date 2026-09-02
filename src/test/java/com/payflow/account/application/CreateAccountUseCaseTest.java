package com.payflow.account.application;

import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountStatus;
import com.payflow.shared.application.TransactionRunner;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CreateAccountUseCaseTest {

    @Test
    void shouldCreateAndPersistActiveAccount() {
        AtomicReference<Account> saved = new AtomicReference<>();

        AccountRepository repository = new AccountRepository() {
            @Override
            public Optional<Account> findById(com.payflow.account.domain.AccountId accountId) {
                return Optional.ofNullable(saved.get());
            }

            @Override
            public void save(Account account) {
                saved.set(account);
            }
        };

        TransactionRunner transactionRunner = operation -> operation.run();

        CreateAccountUseCase useCase =
                new CreateAccountUseCase(repository, transactionRunner);

        Account account = useCase.execute();

        assertNotNull(account);
        assertNotNull(account.id());
        assertNotNull(account.createdAt());
        assertEquals(AccountStatus.ACTIVE, account.status());

        assertNotNull(saved.get());
        assertEquals(account.id(), saved.get().id());
        assertEquals(account.status(), saved.get().status());
    }
}