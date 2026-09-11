package com.payflow.account.infrastructure.persistence;

import com.payflow.account.application.AccountRepository;
import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import com.payflow.account.domain.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AccountRepositoryAdapterTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SpringDataAccountRepository springDataRepository;

    @Test
    void shouldSaveAndLoadActiveAccount() {
        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Instant createdAt = Instant.parse(
                "2026-09-02T10:00:00Z"
        );

        Account account = Account.create(
                accountId,
                "alice@example.com",
                "Alice",
                "Smith",
                createdAt
        );

        accountRepository.save(
                account,
                "test-password-hash"
        );

        Optional<Account> result =
                accountRepository.findById(accountId);

        assertThat(result).isPresent();

        Account loaded = result.orElseThrow();

        assertThat(loaded.id())
                .isEqualTo(accountId);

        assertThat(loaded.createdAt())
                .isEqualTo(createdAt);

        assertThat(loaded.status())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldPreserveSuspendedAccountStatus() {
        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Instant createdAt = Instant.parse(
                "2026-09-02T10:30:00Z"
        );

        Account account = Account.create(
                accountId,
                "alice@example.com",
                "Alice",
                "Smith",
                createdAt
        );

        account.suspend();

        accountRepository.save(
                account,
                "test-password-hash"
        );

        Account loaded =
                accountRepository.findById(accountId)
                        .orElseThrow();

        assertThat(loaded.status())
                .isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    void shouldReturnEmptyWhenAccountDoesNotExist() {
        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Optional<Account> result =
                accountRepository.findById(accountId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldPersistExpectedEntityValues() {
        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Instant createdAt = Instant.parse(
                "2026-09-02T11:00:00Z"
        );

        Account account = Account.create(
                accountId,
                "alice@example.com",
                "Alice",
                "Smith",
                createdAt
        );

        accountRepository.save(
                account,
                "test-password-hash"
        );

        AccountEntity entity =
                springDataRepository.findById(accountId.value())
                        .orElseThrow();

        assertThat(entity.getId())
                .isEqualTo(accountId.value());

        assertThat(entity.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);

        assertThat(entity.getCreatedAt())
                .isEqualTo(createdAt);

        assertThat(entity.getPasswordHash())
                .isEqualTo("test-password-hash");
    }

    @Test
    void shouldRoundTripSuspendedAccountWithoutChangingDomainState() {
        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Instant createdAt = Instant.parse(
                "2026-09-02T12:00:00Z"
        );

        Account original = Account.create(
                accountId,
                "alice@example.com",
                "Alice",
                "Smith",
                createdAt
        );

        original.suspend();

        accountRepository.save(
                original,
                "test-password-hash"
        );

        Account restored =
                accountRepository.findById(accountId)
                        .orElseThrow();

        assertThat(restored.id())
                .isEqualTo(original.id());

        assertThat(restored.createdAt())
                .isEqualTo(original.createdAt());

        assertThat(restored.status())
                .isEqualTo(original.status());
    }

    @Test
    void shouldFindPasswordHashForExistingAccount() {
        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Instant createdAt = Instant.parse(
                "2026-09-02T13:00:00Z"
        );

        String passwordHash =
                "$2a$10$existing-password-hash";

        Account account = Account.create(
                accountId,
                "password@example.com",
                "Password",
                "Test",
                createdAt
        );

        accountRepository.save(
                account,
                passwordHash
        );

        Optional<String> result =
                accountRepository.findPasswordHash(accountId);

        assertThat(result)
                .isPresent()
                .contains(passwordHash);
    }

    @Test
    void shouldReturnEmptyPasswordHashWhenAccountDoesNotExist() {
        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Optional<String> result =
                accountRepository.findPasswordHash(accountId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldUpdatePasswordWithoutChangingOtherAccountFields() {
        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Instant createdAt = Instant.parse(
                "2026-09-02T13:30:00Z"
        );

        String originalPasswordHash =
                "original-password-hash";

        String newPasswordHash =
                "new-password-hash";

        Account account = Account.create(
                accountId,
                "password-update@example.com",
                "Alice",
                "Smith",
                createdAt
        );

        accountRepository.save(
                account,
                originalPasswordHash
        );

        AccountEntity beforeUpdate =
                springDataRepository.findById(accountId.value())
                        .orElseThrow();

        Instant originalUpdatedAt =
                beforeUpdate.getUpdatedAt();

        accountRepository.updatePassword(
                accountId,
                newPasswordHash
        );

        springDataRepository.flush();

        AccountEntity updated =
                springDataRepository.findById(accountId.value())
                        .orElseThrow();

        assertThat(updated.getPasswordHash())
                .isEqualTo(newPasswordHash);

        assertThat(updated.getEmail())
                .isEqualTo("password-update@example.com");

        assertThat(updated.getFirstName())
                .isEqualTo("Alice");

        assertThat(updated.getLastName())
                .isEqualTo("Smith");

        assertThat(updated.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);

        assertThat(updated.getCreatedAt())
                .isEqualTo(createdAt);

        assertThat(updated.getUpdatedAt())
                .isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    void shouldPersistUpdatedPasswordWhenReadThroughRepository() {
        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Instant createdAt = Instant.parse(
                "2026-09-02T14:00:00Z"
        );

        Account account = Account.create(
                accountId,
                "password-roundtrip@example.com",
                "Alice",
                "Smith",
                createdAt
        );

        accountRepository.save(
                account,
                "old-password-hash"
        );

        accountRepository.updatePassword(
                accountId,
                "new-password-hash"
        );

        Optional<String> result =
                accountRepository.findPasswordHash(accountId);

        assertThat(result)
                .isPresent()
                .contains("new-password-hash");
    }
}
