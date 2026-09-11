package com.payflow.wallet.application;

import com.payflow.account.application.AccountRepository;
import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Currency;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateWalletUseCaseTest {

    @Test
    void shouldCreateWalletWithZeroInitialBalance() {

        WalletRepository walletRepository =
                mock(WalletRepository.class);

        AccountRepository accountRepository =
                mock(AccountRepository.class);

        TransactionRunner transactionRunner =
                Runnable::run;

        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Account account = Account.create(
                accountId,
                "test@example.com",
                "Test",
                "User",
                Instant.now()
        );

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(walletRepository.findByAccountId(accountId))
                .thenReturn(Optional.empty());

        AtomicReference<Wallet> savedWallet =
                new AtomicReference<>();

        doAnswer(invocation -> {
            savedWallet.set(invocation.getArgument(0));
            return null;
        }).when(walletRepository).save(any(Wallet.class));

        when(walletRepository.findById(any()))
                .thenAnswer(invocation ->
                        Optional.ofNullable(savedWallet.get())
                );

        CreateWalletUseCase useCase =
                new CreateWalletUseCase(
                        walletRepository,
                        accountRepository,
                        transactionRunner
                );

        Wallet result =
                useCase.execute(
                        accountId,
                        Currency.INR
                );

        assertThat(result).isNotNull();
        assertThat(result.accountId())
                .isEqualTo(accountId);
        assertThat(result.currency())
                .isEqualTo(Currency.INR);
        assertThat(result.balance().amount())
                .isEqualByComparingTo("0.00");

        verify(accountRepository)
                .findById(accountId);

        verify(walletRepository)
                .findByAccountId(accountId);

        verify(walletRepository)
                .save(any(Wallet.class));
    }

    @Test
    void shouldRejectWalletCreationWhenAccountDoesNotExist() {

        WalletRepository walletRepository =
                mock(WalletRepository.class);

        AccountRepository accountRepository =
                mock(AccountRepository.class);

        TransactionRunner transactionRunner =
                Runnable::run;

        AccountId accountId =
                new AccountId(UUID.randomUUID());

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.empty());

        CreateWalletUseCase useCase =
                new CreateWalletUseCase(
                        walletRepository,
                        accountRepository,
                        transactionRunner
                );

        assertThatThrownBy(() ->
                useCase.execute(
                        accountId,
                        Currency.INR
                )
        )
                .isInstanceOf(
                        java.util.NoSuchElementException.class
                )
                .hasMessage(
                        "account not found: " + accountId.value()
                );

        verify(accountRepository)
                .findById(accountId);

        verify(walletRepository, never())
                .findByAccountId(any());

        verify(walletRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectWalletCreationWhenWalletAlreadyExists() {

        WalletRepository walletRepository =
                mock(WalletRepository.class);

        AccountRepository accountRepository =
                mock(AccountRepository.class);

        TransactionRunner transactionRunner =
                Runnable::run;

        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Account account = Account.create(
                accountId,
                "test@example.com",
                "Test",
                "User",
                Instant.now()
        );

        Wallet existingWallet =
                Wallet.create(
                        WalletId.generate(),
                        accountId,
                        Currency.INR
                );

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(walletRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(existingWallet));

        CreateWalletUseCase useCase =
                new CreateWalletUseCase(
                        walletRepository,
                        accountRepository,
                        transactionRunner
                );

        assertThatThrownBy(() ->
                useCase.execute(
                        accountId,
                        Currency.INR
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "wallet already exists for account: "
                                + accountId.value()
                );

        verify(accountRepository)
                .findById(accountId);

        verify(walletRepository)
                .findByAccountId(accountId);

        verify(walletRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectNullAccountId() {

        WalletRepository walletRepository =
                mock(WalletRepository.class);

        AccountRepository accountRepository =
                mock(AccountRepository.class);

        TransactionRunner transactionRunner =
                Runnable::run;

        CreateWalletUseCase useCase =
                new CreateWalletUseCase(
                        walletRepository,
                        accountRepository,
                        transactionRunner
                );

        assertThatThrownBy(() ->
                useCase.execute(
                        null,
                        Currency.INR
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage(
                        "account id must not be null"
                );

        verifyNoInteractions(accountRepository);
        verifyNoInteractions(walletRepository);
    }

    @Test
    void shouldRejectNullCurrency() {

        WalletRepository walletRepository =
                mock(WalletRepository.class);

        AccountRepository accountRepository =
                mock(AccountRepository.class);

        TransactionRunner transactionRunner =
                Runnable::run;

        AccountId accountId =
                new AccountId(UUID.randomUUID());

        CreateWalletUseCase useCase =
                new CreateWalletUseCase(
                        walletRepository,
                        accountRepository,
                        transactionRunner
                );

        assertThatThrownBy(() ->
                useCase.execute(
                        accountId,
                        null
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage(
                        "currency must not be null"
                );

        verifyNoInteractions(accountRepository);
        verifyNoInteractions(walletRepository);
    }
}
