package com.payflow.account.application;

import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import com.payflow.shared.application.TransactionRunner;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ChangePasswordUseCaseTest {

    @Test
    void shouldChangePasswordWhenCurrentPasswordIsCorrect() {
        AccountId accountId = AccountId.generate();

        String existingHash = "existing-hash";
        AtomicReference<String> updatedPasswordHash =
                new AtomicReference<>();

        AccountRepository repository = new AccountRepository() {

            @Override
            public Optional<Account> findById(
                    AccountId id
            ) {
                return Optional.empty();
            }

            @Override
            public Optional<Account> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public Optional<String> findPasswordHash(
                    AccountId id
            ) {
                return Optional.of(existingHash);
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

            @Override
            public void updatePassword(
                    AccountId id,
                    String passwordHash
            ) {
                updatedPasswordHash.set(passwordHash);
            }
        };

        PasswordHasher passwordHasher = new PasswordHasher() {

            @Override
            public String hash(String rawPassword) {
                assertEquals("NewPassword123!", rawPassword);
                return "new-password-hash";
            }

            @Override
            public boolean matches(
                    String rawPassword,
                    String passwordHash
            ) {
                assertEquals("CurrentPassword123!", rawPassword);
                assertEquals(existingHash, passwordHash);
                return true;
            }
        };

        TransactionRunner transactionRunner =
                operation -> operation.run();

        ChangePasswordUseCase useCase =
                new ChangePasswordUseCase(
                        repository,
                        passwordHasher,
                        transactionRunner
                );

        ChangePasswordCommand command =
                new ChangePasswordCommand(
                        accountId,
                        "CurrentPassword123!",
                        "NewPassword123!"
                );

        useCase.execute(command);

        assertEquals(
                "new-password-hash",
                updatedPasswordHash.get()
        );
    }

    @Test
    void shouldRejectUnknownAccount() {
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
            public Optional<String> findPasswordHash(
                    AccountId id
            ) {
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

            @Override
            public void updatePassword(
                    AccountId id,
                    String passwordHash
            ) {
                updateCalled.set(true);
            }
        };

        PasswordHasher passwordHasher = new PasswordHasher() {

            @Override
            public String hash(String rawPassword) {
                fail("password should not be hashed");
                return null;
            }

            @Override
            public boolean matches(
                    String rawPassword,
                    String passwordHash
            ) {
                fail("password should not be matched");
                return false;
            }
        };

        TransactionRunner transactionRunner =
                operation -> operation.run();

        ChangePasswordUseCase useCase =
                new ChangePasswordUseCase(
                        repository,
                        passwordHasher,
                        transactionRunner
                );

        ChangePasswordCommand command =
                new ChangePasswordCommand(
                        accountId,
                        "CurrentPassword123!",
                        "NewPassword123!"
                );

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> useCase.execute(command)
        );

        assertEquals(
                "account not found: " + accountId.value(),
                exception.getMessage()
        );

        assertFalse(updateCalled.get());
    }

    @Test
    void shouldRejectIncorrectCurrentPassword() {
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
            public Optional<String> findPasswordHash(
                    AccountId id
            ) {
                return Optional.of("existing-hash");
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

            @Override
            public void updatePassword(
                    AccountId id,
                    String passwordHash
            ) {
                updateCalled.set(true);
            }
        };

        PasswordHasher passwordHasher = new PasswordHasher() {

            @Override
            public String hash(String rawPassword) {
                fail("new password must not be hashed");
                return null;
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

        ChangePasswordUseCase useCase =
                new ChangePasswordUseCase(
                        repository,
                        passwordHasher,
                        transactionRunner
                );

        ChangePasswordCommand command =
                new ChangePasswordCommand(
                        accountId,
                        "WrongPassword123!",
                        "NewPassword123!"
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> useCase.execute(command)
        );

        assertEquals(
                "current password is incorrect",
                exception.getMessage()
        );

        assertFalse(updateCalled.get());
    }

    @Test
    void shouldRejectBlankNewPassword() {
        AccountId accountId = AccountId.generate();

        AccountRepository repository = passwordRepository(
                accountId,
                "existing-hash"
        );

        PasswordHasher passwordHasher = new PasswordHasher() {

            @Override
            public String hash(String rawPassword) {
                fail("blank password must not be hashed");
                return null;
            }

            @Override
            public boolean matches(
                    String rawPassword,
                    String passwordHash
            ) {
                return true;
            }
        };

        TransactionRunner transactionRunner =
                operation -> operation.run();

        ChangePasswordUseCase useCase =
                new ChangePasswordUseCase(
                        repository,
                        passwordHasher,
                        transactionRunner
                );

        ChangePasswordCommand command =
                new ChangePasswordCommand(
                        accountId,
                        "CurrentPassword123!",
                        "   "
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(command)
        );

        assertEquals(
                "new password must not be blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNewPasswordShorterThanEightCharacters() {
        AccountId accountId = AccountId.generate();

        AccountRepository repository = passwordRepository(
                accountId,
                "existing-hash"
        );

        PasswordHasher passwordHasher = new PasswordHasher() {

            @Override
            public String hash(String rawPassword) {
                fail("short password must not be hashed");
                return null;
            }

            @Override
            public boolean matches(
                    String rawPassword,
                    String passwordHash
            ) {
                return true;
            }
        };

        TransactionRunner transactionRunner =
                operation -> operation.run();

        ChangePasswordUseCase useCase =
                new ChangePasswordUseCase(
                        repository,
                        passwordHasher,
                        transactionRunner
                );

        ChangePasswordCommand command =
                new ChangePasswordCommand(
                        accountId,
                        "CurrentPassword123!",
                        "Short1"
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(command)
        );

        assertEquals(
                "new password must be at least 8 characters",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNewPasswordLongerThan255Characters() {
        AccountId accountId = AccountId.generate();

        AccountRepository repository = passwordRepository(
                accountId,
                "existing-hash"
        );

        PasswordHasher passwordHasher = new PasswordHasher() {

            @Override
            public String hash(String rawPassword) {
                fail("long password must not be hashed");
                return null;
            }

            @Override
            public boolean matches(
                    String rawPassword,
                    String passwordHash
            ) {
                return true;
            }
        };

        TransactionRunner transactionRunner =
                operation -> operation.run();

        ChangePasswordUseCase useCase =
                new ChangePasswordUseCase(
                        repository,
                        passwordHasher,
                        transactionRunner
                );

        String longPassword = "a".repeat(256);

        ChangePasswordCommand command =
                new ChangePasswordCommand(
                        accountId,
                        "CurrentPassword123!",
                        longPassword
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(command)
        );

        assertEquals(
                "new password must not exceed 255 characters",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullCommand() {
        AccountRepository repository = new AccountRepository() {

            @Override
            public Optional<Account> findById(AccountId accountId) {
                return Optional.empty();
            }

            @Override
            public Optional<Account> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public Optional<String> findPasswordHash(
                    AccountId accountId
            ) {
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

            @Override
            public void updatePassword(
                    AccountId accountId,
                    String passwordHash
            ) {
                // Not used by this test.
            }
        };

        PasswordHasher passwordHasher = new PasswordHasher() {

            @Override
            public String hash(String rawPassword) {
                return "hash";
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

        ChangePasswordUseCase useCase =
                new ChangePasswordUseCase(
                        repository,
                        passwordHasher,
                        transactionRunner
                );

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(null)
        );
    }

    private AccountRepository passwordRepository(
            AccountId accountId,
            String passwordHash
    ) {
        return new AccountRepository() {

            @Override
            public Optional<Account> findById(AccountId id) {
                return Optional.empty();
            }

            @Override
            public Optional<Account> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public Optional<String> findPasswordHash(
                    AccountId id
            ) {
                if (accountId.equals(id)) {
                    return Optional.of(passwordHash);
                }

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

            @Override
            public void updatePassword(
                    AccountId id,
                    String passwordHash
            ) {
                // Not used by this test.
            }
        };
    }
}

