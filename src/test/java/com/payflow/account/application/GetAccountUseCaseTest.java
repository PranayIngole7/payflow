package com.payflow.account.application;

import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import com.payflow.account.domain.AccountStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetAccountUseCaseTest {

    @Test
    void shouldReturnExistingAccount() {
        AccountId accountId = AccountId.generate();

        Account account = Account.create(
                accountId,
                Instant.now()
        );

        AccountRepository repository = new AccountRepository() {
            @Override
            public Optional<Account> findById(AccountId id) {
                return Optional.of(account);
            }

            @Override
            public void save(Account account) {
                // Not used by this test.
            }
        };

        GetAccountUseCase useCase =
                new GetAccountUseCase(repository);

        Account result = useCase.execute(accountId);

        assertEquals(account, result);
        assertEquals(accountId, result.id());
        assertEquals(AccountStatus.ACTIVE, result.status());
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
            public void save(Account account) {
                // Not used by this test.
            }
        };

        GetAccountUseCase useCase =
                new GetAccountUseCase(repository);

        var exception = assertThrows(
                java.util.NoSuchElementException.class,
                () -> useCase.execute(accountId)
        );

        assertTrue(
                exception.getMessage().contains(
                        accountId.value().toString()
                )
        );
    }

    @Test
    void shouldRejectNullAccountId() {
        AccountRepository repository = new AccountRepository() {
            @Override
            public Optional<Account> findById(AccountId id) {
                return Optional.empty();
            }

            @Override
            public void save(Account account) {
                // Not used by this test.
            }
        };

        GetAccountUseCase useCase =
                new GetAccountUseCase(repository);

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(null)
        );
    }
}