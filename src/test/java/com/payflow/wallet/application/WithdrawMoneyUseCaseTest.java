package com.payflow.wallet.application;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Currency;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WithdrawMoneyUseCaseTest {

    private WalletRepository walletRepository;
    private TransactionRunner transactionRunner;
    private WithdrawMoneyUseCase useCase;

    private WalletId walletId;
    private AccountId accountId;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        transactionRunner = mock(TransactionRunner.class);

        useCase = new WithdrawMoneyUseCase(
                walletRepository,
                transactionRunner
        );

        walletId = WalletId.generate();
        accountId = AccountId.generate();
    }

    @Test
    void shouldWithdrawMoneyFromWallet() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        executeTransactionImmediately();

        Wallet result = useCase.execute(
                walletId,
                new BigDecimal("40.00"),
                Currency.INR
        );

        assertEquals(
                new BigDecimal("60.00"),
                result.balance().amount()
        );

        assertEquals(
                Currency.INR,
                result.balance().currency()
        );

        verify(walletRepository).save(wallet);
        verify(transactionRunner).execute(any(Runnable.class));
    }

    @Test
    void shouldWithdrawEntireWalletBalance() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        executeTransactionImmediately();

        Wallet result = useCase.execute(
                walletId,
                new BigDecimal("100.00"),
                Currency.INR
        );

        assertEquals(
                0,
                result.balance().amount().compareTo(BigDecimal.ZERO)
        );

        verify(walletRepository).save(wallet);
    }

    @Test
    void shouldRejectWithdrawalWhenWalletDoesNotExist() {
        when(walletRepository.findById(walletId))
                .thenReturn(Optional.empty());

        executeTransactionImmediately();

        assertThrows(
                NoSuchElementException.class,
                () -> useCase.execute(
                        walletId,
                        new BigDecimal("50.00"),
                        Currency.INR
                )
        );

        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldRejectZeroWithdrawal() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        executeTransactionImmediately();

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        walletId,
                        BigDecimal.ZERO,
                        Currency.INR
                )
        );

        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldRejectNegativeWithdrawal() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        executeTransactionImmediately();

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        walletId,
                        new BigDecimal("-10.00"),
                        Currency.INR
                )
        );

        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldRejectWithdrawalWhenCurrencyDoesNotMatchWallet() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        executeTransactionImmediately();

        Currency differentCurrency = Currency.values()[0];

        if (differentCurrency == Currency.INR) {
            differentCurrency = Currency.values()[1];
        }

        Currency withdrawalCurrency = differentCurrency;

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        walletId,
                        new BigDecimal("50.00"),
                        withdrawalCurrency
                )
        );

        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldRejectWithdrawalWhenFundsAreInsufficient() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        executeTransactionImmediately();

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        walletId,
                        new BigDecimal("100.01"),
                        Currency.INR
                )
        );

        assertEquals(
                new BigDecimal("100.00"),
                wallet.balance().amount()
        );

        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldRejectNullWalletId() {
        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(
                        null,
                        new BigDecimal("50.00"),
                        Currency.INR
                )
        );

        verifyNoInteractions(
                walletRepository,
                transactionRunner
        );
    }

    @Test
    void shouldRejectNullAmount() {
        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(
                        walletId,
                        null,
                        Currency.INR
                )
        );

        verifyNoInteractions(
                walletRepository,
                transactionRunner
        );
    }

    @Test
    void shouldRejectNullCurrency() {
        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(
                        walletId,
                        new BigDecimal("50.00"),
                        null
                )
        );

        verifyNoInteractions(
                walletRepository,
                transactionRunner
        );
    }

    @Test
    void shouldInvokeTransactionRunner() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        executeTransactionImmediately();

        useCase.execute(
                walletId,
                new BigDecimal("25.00"),
                Currency.INR
        );

        verify(transactionRunner).execute(any(Runnable.class));
    }

    @Test
    void shouldSaveWalletAfterSuccessfulWithdrawal() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        executeTransactionImmediately();

        useCase.execute(
                walletId,
                new BigDecimal("25.00"),
                Currency.INR
        );

        verify(walletRepository).save(wallet);
    }

    @Test
    void shouldNotSaveWalletWhenWithdrawalFails() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        executeTransactionImmediately();

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        walletId,
                        new BigDecimal("100.01"),
                        Currency.INR
                )
        );

        verify(walletRepository, never()).save(any());
    }

    private Wallet createWalletWithBalance(String amount) {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        wallet.credit(
                new com.payflow.shared.domain.Money(
                        new BigDecimal(amount),
                        Currency.INR
                )
        );

        return wallet;
    }

    private void executeTransactionImmediately() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactionRunner).execute(any(Runnable.class));
    }
}
