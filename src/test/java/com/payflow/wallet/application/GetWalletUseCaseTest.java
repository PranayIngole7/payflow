package com.payflow.wallet.application;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class GetWalletUseCaseTest {

    @Test
    void shouldReturnExistingWallet() {
        WalletRepository repository = mock(WalletRepository.class);

        WalletId walletId = WalletId.generate();
        AccountId accountId = new AccountId(UUID.randomUUID());

        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        when(repository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        GetWalletUseCase useCase =
                new GetWalletUseCase(repository);

        Wallet result = useCase.execute(walletId);

        assertThat(result).isSameAs(wallet);
        verify(repository).findById(walletId);
    }

    @Test
    void shouldRejectUnknownWallet() {
        WalletRepository repository = mock(WalletRepository.class);

        WalletId walletId = WalletId.generate();

        when(repository.findById(walletId))
                .thenReturn(Optional.empty());

        GetWalletUseCase useCase =
                new GetWalletUseCase(repository);

        assertThrows(
                NoSuchElementException.class,
                () -> useCase.execute(walletId)
        );
    }

    @Test
    void shouldRejectNullWalletId() {
        WalletRepository repository = mock(WalletRepository.class);

        GetWalletUseCase useCase =
                new GetWalletUseCase(repository);

        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(null)
        );

        verifyNoInteractions(repository);
    }
}