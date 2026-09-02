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
                createdAt
        );

        accountRepository.save(account);

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

        Account account = Account.create(
                accountId,
                Instant.parse("2026-09-02T10:15:00Z")
        );

        account.suspend();

        accountRepository.save(account);

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
                createdAt
        );

        accountRepository.save(account);

        AccountEntity entity =
                springDataRepository.findById(accountId.value())
                        .orElseThrow();

        assertThat(entity.getId())
                .isEqualTo(accountId.value());

        assertThat(entity.getStatus())
                .isEqualTo(AccountStatus.ACTIVE);

        assertThat(entity.getCreatedAt())
                .isEqualTo(createdAt);
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
                createdAt
        );

        original.suspend();

        accountRepository.save(original);

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
}