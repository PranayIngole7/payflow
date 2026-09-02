package com.payflow.wallet.application;

import com.payflow.account.application.AccountRepository;
import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.Wallet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CreateWalletUseCaseTest {

    @Test
    void shouldCreateWalletWithInitialBalance() {
        WalletRepository walletRepository =
                mock(WalletRepository.class);

        AccountRepository accountRepository =
                mock(AccountRepository.class);

        TransactionRunner runner = Runnable::run;

        CreateWalletUseCase useCase =
                new CreateWalletUseCase(
                        walletRepository,
                        accountRepository,
                        runner
                );

        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Account account = Account.create(
                accountId,
                Instant.now()
        );

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        Money initialBalance =
                new Money(
                        new BigDecimal("250.00"),
                        Currency.INR
                );

        ArgumentCaptor<Wallet> captor =
                ArgumentCaptor.forClass(Wallet.class);

        doNothing()
                .when(walletRepository)
                .save(captor.capture());

        when(walletRepository.findById(any()))
                .thenAnswer(invocation ->
                        Optional.of(captor.getValue())
                );

        Wallet wallet = useCase.execute(
                accountId,
                initialBalance
        );

        assertThat(wallet.accountId())
                .isEqualTo(accountId);

        assertThat(wallet.currency())
                .isEqualTo(Currency.INR);

        assertThat(wallet.balance().amount())
                .isEqualByComparingTo("250.00");

        verify(accountRepository)
                .findById(accountId);

        verify(walletRepository)
                .save(any(Wallet.class));
    }

    @Test
    void shouldRejectWalletCreationWhenAccountDoesNotExist() {
        WalletRepository walletRepository =
                mock(WalletRepository.class);

        AccountRepository accountRepository =
                mock(AccountRepository.class);

        TransactionRunner runner = Runnable::run;

        CreateWalletUseCase useCase =
                new CreateWalletUseCase(
                        walletRepository,
                        accountRepository,
                        runner
                );

        AccountId accountId =
                new AccountId(UUID.randomUUID());

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.empty());

        var exception = assertThrows(
                java.util.NoSuchElementException.class,
                () -> useCase.execute(
                        accountId,
                        new Money(
                                new BigDecimal("100.00"),
                                Currency.INR
                        )
                )
        );

        assertThat(exception.getMessage())
                .isEqualTo(
                        "account not found: " + accountId.value()
                );

        verify(accountRepository)
                .findById(accountId);

        verifyNoInteractions(walletRepository);
    }

    @Test
    void shouldRejectNullAccountId() {
        WalletRepository walletRepository =
                mock(WalletRepository.class);

        AccountRepository accountRepository =
                mock(AccountRepository.class);

        TransactionRunner runner = Runnable::run;

        CreateWalletUseCase useCase =
                new CreateWalletUseCase(
                        walletRepository,
                        accountRepository,
                        runner
                );

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(
                        null,
                        new Money(
                                new BigDecimal("100.00"),
                                Currency.INR
                        )
                )
        );

        verifyNoInteractions(
                walletRepository,
                accountRepository
        );
    }

    @Test
    void shouldRejectNullInitialBalance() {
        WalletRepository walletRepository =
                mock(WalletRepository.class);

        AccountRepository accountRepository =
                mock(AccountRepository.class);

        TransactionRunner runner = Runnable::run;

        CreateWalletUseCase useCase =
                new CreateWalletUseCase(
                        walletRepository,
                        accountRepository,
                        runner
                );

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(
                        new AccountId(UUID.randomUUID()),
                        null
                )
        );

        verifyNoInteractions(
                walletRepository,
                accountRepository
        );
    }
}