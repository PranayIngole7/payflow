package com.payflow.wallet.application;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.Wallet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CreateWalletUseCaseTest {

    @Test
    void shouldCreateWalletWithInitialBalance() {
        WalletRepository repository = mock(WalletRepository.class);
        TransactionRunner runner = Runnable::run;

        CreateWalletUseCase useCase =
                new CreateWalletUseCase(repository, runner);

        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Money initialBalance =
                new Money(new BigDecimal("250.00"), Currency.INR);

        ArgumentCaptor<Wallet> captor =
                ArgumentCaptor.forClass(Wallet.class);

        doNothing().when(repository).save(captor.capture());

        when(repository.findById(any()))
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

        verify(repository).save(any(Wallet.class));
    }

    @Test
    void shouldRejectNullAccountId() {
        WalletRepository repository = mock(WalletRepository.class);
        TransactionRunner runner = Runnable::run;

        CreateWalletUseCase useCase =
                new CreateWalletUseCase(repository, runner);

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

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectNullInitialBalance() {
        WalletRepository repository = mock(WalletRepository.class);
        TransactionRunner runner = Runnable::run;

        CreateWalletUseCase useCase =
                new CreateWalletUseCase(repository, runner);

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(
                        new AccountId(UUID.randomUUID()),
                        null
                )
        );

        verifyNoInteractions(repository);
    }
}