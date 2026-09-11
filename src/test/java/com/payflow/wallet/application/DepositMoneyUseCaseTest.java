package com.payflow.wallet.application;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DepositMoneyUseCaseTest {

    private WalletRepository walletRepository;
    private TransactionRunner transactionRunner;
    private DepositMoneyUseCase useCase;

    private WalletId walletId;
    private AccountId accountId;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        transactionRunner = mock(TransactionRunner.class);

        useCase = new DepositMoneyUseCase(
                walletRepository,
                transactionRunner
        );

        walletId = WalletId.generate();
        accountId = new AccountId(java.util.UUID.randomUUID());
    }

    @Test
    void shouldDepositMoneyIntoWallet() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        wallet.credit(
                new Money(
                        new BigDecimal("100.00"),
                        Currency.INR
                )
        );

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactionRunner).execute(any(Runnable.class));

        Wallet result = useCase.execute(
                walletId,
                new BigDecimal("50.00"),
                Currency.INR
        );

        assertEquals(
                new BigDecimal("150.00"),
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
    void shouldRejectDepositWhenWalletDoesNotExist() {
        when(walletRepository.findById(walletId))
                .thenReturn(Optional.empty());

        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactionRunner).execute(any(Runnable.class));

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
    void shouldRejectZeroDeposit() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactionRunner).execute(any(Runnable.class));

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
    void shouldRejectNegativeDeposit() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactionRunner).execute(any(Runnable.class));

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
    void shouldRejectDepositWhenCurrencyDoesNotMatchWallet() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactionRunner).execute(any(Runnable.class));

        Currency differentCurrency = Currency.values()[0];

        if (differentCurrency == Currency.INR) {
            differentCurrency = Currency.values()[1];
        }

        Currency depositCurrency = differentCurrency;

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        walletId,
                        new BigDecimal("50.00"),
                        depositCurrency
                )
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
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactionRunner).execute(any(Runnable.class));

        useCase.execute(
                walletId,
                new BigDecimal("25.00"),
                Currency.INR
        );

        verify(transactionRunner).execute(any(Runnable.class));
    }

    @Test
    void shouldSaveWalletAfterSuccessfulDeposit() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactionRunner).execute(any(Runnable.class));

        useCase.execute(
                walletId,
                new BigDecimal("75.00"),
                Currency.INR
        );

        verify(walletRepository).save(wallet);
    }

    @Test
    void shouldNotSaveWalletWhenDepositFails() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactionRunner).execute(any(Runnable.class));

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
}