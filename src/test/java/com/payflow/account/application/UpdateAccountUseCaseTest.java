package com.payflow.account.application;

import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import com.payflow.shared.application.TransactionRunner;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UpdateAccountUseCaseTest {

    @Test
    void shouldUpdateAccountAndPersistChanges() {
        AccountId accountId = AccountId.generate();

        Account account = Account.create(
                accountId,
                "alice@example.com",
                "Alice",
                "Smith",
                Instant.now()
        );

        AtomicReference<Account> persistedAccount =
                new AtomicReference<>();

        AtomicBoolean transactionExecuted =
                new AtomicBoolean(false);

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
            public Optional<String> findPasswordHash(AccountId accountId) {
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
                persistedAccount.set(account);
            }

            @Override
            public void updatePassword(
                    AccountId accountId,
                    String passwordHash
            ) {
                // Not used by this test.
            }
        };

        TransactionRunner transactionRunner = work -> {
            transactionExecuted.set(true);
            work.run();
        };

        UpdateAccountUseCase useCase =
                new UpdateAccountUseCase(
                        repository,
                        transactionRunner
                );

        Account result = useCase.execute(
                new UpdateAccountCommand(
                        accountId,
                        "Alicia",
                        "Johnson"
                )
        );

        assertSame(account, result);
        assertEquals("Alicia", result.firstName());
        assertEquals("Johnson", result.lastName());
        assertSame(account, persistedAccount.get());
        assertTrue(transactionExecuted.get());
    }

    @Test
    void shouldThrowWhenAccountDoesNotExist() {
        AccountId accountId = AccountId.generate();

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
            public Optional<String> findPasswordHash(AccountId accountId) {
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
                fail("update must not be called");
            }

            @Override
            public void updatePassword(
                    AccountId accountId,
                    String passwordHash
            ) {
                // Not used by this test.
            }
        };

        TransactionRunner transactionRunner =
                work -> fail("transaction must not be executed");

        UpdateAccountUseCase useCase =
                new UpdateAccountUseCase(
                        repository,
                        transactionRunner
                );

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> useCase.execute(
                        new UpdateAccountCommand(
                                accountId,
                                "Alicia",
                                "Johnson"
                        )
                )
        );

        assertEquals(
                "account not found: " + accountId.value(),
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullCommand() {
        AccountRepository repository = null;
        TransactionRunner transactionRunner = work -> {
        };

        assertThrows(
                NullPointerException.class,
                () -> new UpdateAccountUseCase(
                        repository,
                        transactionRunner
                )
        );
    }
}