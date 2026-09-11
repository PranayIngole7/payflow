package com.payflow.account.application;

import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountStatus;
import com.payflow.shared.application.TransactionRunner;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CreateAccountUseCaseTest {

    @Test
    void shouldCreateAndPersistActiveAccountWithHashedPassword() {
        AtomicReference<Account> savedAccount = new AtomicReference<>();
        AtomicReference<String> savedPasswordHash = new AtomicReference<>();

        AccountRepository repository = new AccountRepository() {
            @Override
            public Optional<Account> findById(
                    com.payflow.account.domain.AccountId accountId) {
                return Optional.ofNullable(savedAccount.get());
            }

            @Override
            public void save(Account account, String passwordHash) {
                savedAccount.set(account);
                savedPasswordHash.set(passwordHash);
            }
        };

        PasswordHasher passwordHasher = rawPassword -> {
            assertEquals("Password123", rawPassword);
            return "hashed-password";
        };

        TransactionRunner transactionRunner =
                operation -> operation.run();

        CreateAccountUseCase useCase =
                new CreateAccountUseCase(
                        repository,
                        passwordHasher,
                        transactionRunner
                );

        RegisterAccountCommand command =
                new RegisterAccountCommand(
                        "alice@example.com",
                        "Alice",
                        "Smith",
                        "Password123"
                );

        Account account = useCase.execute(command);

        assertNotNull(account);
        assertNotNull(account.id());
        assertNotNull(account.createdAt());
        assertEquals(AccountStatus.ACTIVE, account.status());

        assertNotNull(savedAccount.get());
        assertEquals(account.id(), savedAccount.get().id());
        assertEquals(account.status(), savedAccount.get().status());

        assertEquals(
                "hashed-password",
                savedPasswordHash.get()
        );
    }
}