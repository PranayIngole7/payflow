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
                    com.payflow.account.domain.AccountId accountId
            ) {
                return Optional.ofNullable(savedAccount.get());
            }

            @Override
            public Optional<Account> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public Optional<String> findPasswordHash(
                    com.payflow.account.domain.AccountId accountId
            ) {
                return Optional.empty();
            }

            @Override
            public void save(
                    Account account,
                    String passwordHash
            ) {
                savedAccount.set(account);
                savedPasswordHash.set(passwordHash);
            }

            @Override
            public void update(Account account) {
                // Not used by this test.
            }

            @Override
            public void updatePassword(
                    com.payflow.account.domain.AccountId accountId,
                    String passwordHash
            ) {
                // Not used by this test.
            }
        };

        PasswordHasher passwordHasher = new PasswordHasher() {

            @Override
            public String hash(String rawPassword) {
                return "hashed-password";
            }

            @Override
            public boolean matches(
                    String rawPassword,
                    String passwordHash
            ) {
                return false;
            }
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

    @Test
    void shouldRejectDuplicateEmail() {
        Account existingAccount = Account.create(
                com.payflow.account.domain.AccountId.generate(),
                "alice@example.com",
                "Alice",
                "Smith",
                java.time.Instant.now()
        );

        AtomicReference<Boolean> saveCalled =
                new AtomicReference<>(false);

        AccountRepository repository = new AccountRepository() {

            @Override
            public Optional<Account> findById(
                    com.payflow.account.domain.AccountId accountId
            ) {
                return Optional.empty();
            }

            @Override
            public Optional<Account> findByEmail(String email) {
                return Optional.of(existingAccount);
            }

            @Override
            public Optional<String> findPasswordHash(
                    com.payflow.account.domain.AccountId accountId
            ) {
                return Optional.empty();
            }

            @Override
            public void save(
                    Account account,
                    String passwordHash
            ) {
                saveCalled.set(true);
            }

            @Override
            public void update(Account account) {
                // Not used by this test.
            }

            @Override
            public void updatePassword(
                    com.payflow.account.domain.AccountId accountId,
                    String passwordHash
            ) {
                // Not used by this test.
            }
        };

        PasswordHasher passwordHasher = new PasswordHasher() {

            @Override
            public String hash(String rawPassword) {
                return "hashed-password";
            }

            @Override
            public boolean matches(
                    String rawPassword,
                    String passwordHash
            ) {
                return false;
            }
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
                        "Bob",
                        "Jones",
                        "Password123"
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> useCase.execute(command)
        );

        assertEquals(
                "account already exists for email: alice@example.com",
                exception.getMessage()
        );

        assertFalse(saveCalled.get());
    }
}