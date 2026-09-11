package com.payflow.account.application;

import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import com.payflow.account.domain.AccountStatus;
import com.payflow.shared.application.TransactionRunner;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SuspendAccountUseCaseTest {

    @Test
    void shouldSuspendActiveAccountAndPersistUpdatedAccount() {
        AccountId accountId = AccountId.generate();

        Account account = Account.create(
                accountId,
                "alice@example.com",
                "Alice",
                "Smith",
                Instant.now()
        );

        AtomicReference<Account> updatedAccount =
                new AtomicReference<>();

        AccountRepository repository = new AccountRepository() {

            @Override
            public Optional<Account> findById(AccountId id) {
                return Optional.of(account);
            }

            @Override
            public Optional<Account> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public void save(
                    Account account,
                    String passwordHash
            ) {
                // Not used by this test.
            }

            @Override
            public void update(Account account) {
                updatedAccount.set(account);
            }
        };

        AtomicReference<Boolean> transactionExecuted =
                new AtomicReference<>(false);

        TransactionRunner transactionRunner = operation -> {
            transactionExecuted.set(true);
            operation.run();
        };

        SuspendAccountUseCase useCase =
                new SuspendAccountUseCase(
                        repository,
                        transactionRunner
                );

        useCase.execute(accountId);

        assertEquals(
                AccountStatus.SUSPENDED,
                account.status()
        );

        assertNotNull(updatedAccount.get());
        assertEquals(
                AccountStatus.SUSPENDED,
                updatedAccount.get().status()
        );

        assertEquals(
                accountId,
                updatedAccount.get().id()
        );

        assertTrue(transactionExecuted.get());
    }

    @Test
    void shouldThrowWhenAccountDoesNotExist() {
        AccountId accountId = AccountId.generate();

        AtomicReference<Boolean> updateCalled =
                new AtomicReference<>(false);

        AccountRepository repository = new AccountRepository() {

            @Override
            public Optional<Account> findById(AccountId id) {
                return Optional.empty();
            }

            @Override
            public Optional<Account> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public void save(
                    Account account,
                    String passwordHash
            ) {
                // Not used by this test.
            }

            @Override
            public void update(Account account) {
                updateCalled.set(true);
            }
        };

        TransactionRunner transactionRunner =
                operation -> operation.run();

        SuspendAccountUseCase useCase =
                new SuspendAccountUseCase(
                        repository,
                        transactionRunner
                );

        var exception = assertThrows(
                java.util.NoSuchElementException.class,
                () -> useCase.execute(accountId)
        );

        assertTrue(
                exception.getMessage().contains(
                        accountId.value().toString()
                )
        );

        assertFalse(updateCalled.get());
    }

    @Test
    void shouldRejectAlreadySuspendedAccount() {
        AccountId accountId = AccountId.generate();

        Account account = Account.create(
                accountId,
                "alice@example.com",
                "Alice",
                "Smith",
                Instant.now()
        );

        account.suspend();

        AtomicReference<Boolean> updateCalled =
                new AtomicReference<>(false);

        AccountRepository repository = new AccountRepository() {

            @Override
            public Optional<Account> findById(AccountId id) {
                return Optional.of(account);
            }

            @Override
            public Optional<Account> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public void save(
                    Account account,
                    String passwordHash
            ) {
                // Not used by this test.
            }

            @Override
            public void update(Account account) {
                updateCalled.set(true);
            }
        };

        TransactionRunner transactionRunner =
                operation -> operation.run();

        SuspendAccountUseCase useCase =
                new SuspendAccountUseCase(
                        repository,
                        transactionRunner
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> useCase.execute(accountId)
        );

        assertEquals(
                "account is already suspended",
                exception.getMessage()
        );

        assertFalse(updateCalled.get());
    }

    @Test
    void shouldRejectNullAccountId() {
        AccountRepository repository = new AccountRepository() {

            @Override
            public Optional<Account> findById(AccountId id) {
                return Optional.empty();
            }

            @Override
            public Optional<Account> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public void save(
                    Account account,
                    String passwordHash
            ) {
                // Not used by this test.
            }

            @Override
            public void update(Account account) {
                // Not used by this test.
            }
        };

        TransactionRunner transactionRunner =
                operation -> operation.run();

        SuspendAccountUseCase useCase =
                new SuspendAccountUseCase(
                        repository,
                        transactionRunner
                );

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(null)
        );
    }
}


