package com.payflow.wallet.application;

import com.payflow.account.domain.AccountId;
import com.payflow.ledger.application.LedgerRepository;
import com.payflow.ledger.domain.Ledger;
import com.payflow.ledger.domain.LedgerEntry;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.TransactionRepository;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DepositMoneyUseCaseTest {

    private WalletRepository walletRepository;
    private TransactionRepository transactionRepository;
    private LedgerRepository ledgerRepository;
    private TransactionRunner transactionRunner;

    private DepositMoneyUseCase useCase;

    private WalletId walletId;
    private AccountId accountId;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        ledgerRepository = mock(LedgerRepository.class);
        transactionRunner = mock(TransactionRunner.class);

        useCase = new DepositMoneyUseCase(
                walletRepository,
                transactionRepository,
                ledgerRepository,
                transactionRunner
        );

        walletId = WalletId.generate();
        accountId = new AccountId(UUID.randomUUID());
    }

    @Test
    void shouldDepositMoneyIntoWallet() {
        Wallet wallet = wallet();

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

        runTransactionImmediately();

        Wallet result = useCase.execute(
                walletId,
                new BigDecimal("50.00"),
                Currency.INR
        );

        assertEquals(
                new BigDecimal("50.00"),
                result.balance().amount()
        );

        assertEquals(
                Currency.INR,
                result.balance().currency()
        );

        verify(walletRepository).save(wallet);
        verify(transactionRepository).save(any(Transaction.class));
        verify(ledgerRepository).save(any(Ledger.class));
        verify(transactionRunner).execute(any(Runnable.class));
    }

    @Test
    void shouldCreateDepositTransaction() {
        Wallet wallet = wallet();

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

        AtomicReference<Transaction> savedTransaction =
                new AtomicReference<>();

        doAnswer(invocation -> {
            savedTransaction.set(
                    invocation.getArgument(0)
            );
            return null;
        }).when(transactionRepository).save(any(Transaction.class));

        runTransactionImmediately();

        useCase.execute(
                walletId,
                new BigDecimal("75.00"),
                Currency.INR
        );

        Transaction transaction = savedTransaction.get();

        assertNotNull(transaction);
        assertEquals(TransactionType.DEPOSIT, transaction.type());
        assertEquals(walletId, transaction.destinationWalletId());
        assertNull(transaction.sourceWalletId());
        assertEquals(
                new BigDecimal("75.00"),
                transaction.amount().amount()
        );
        assertEquals(Currency.INR, transaction.currency());
        assertEquals(
                com.payflow.transaction.domain.TransactionStatus.PENDING,
                transaction.status()
        );
    }

    @Test
    void shouldCreateCreditLedgerEntry() {
        Wallet wallet = wallet();

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

        AtomicReference<Ledger> savedLedger =
                new AtomicReference<>();

        doAnswer(invocation -> {
            savedLedger.set(
                    invocation.getArgument(0)
            );
            return null;
        }).when(ledgerRepository).save(any(Ledger.class));

        runTransactionImmediately();

        useCase.execute(
                walletId,
                new BigDecimal("100.00"),
                Currency.INR
        );

        Ledger ledger = savedLedger.get();

        assertNotNull(ledger);
        assertEquals(1, ledger.entries().size());

        LedgerEntry entry = ledger.entries().getFirst();

        assertEquals(LedgerEntryType.CREDIT, entry.type());
        assertEquals(walletId, entry.walletId());
        assertEquals(
                new BigDecimal("100.00"),
                entry.amount().amount()
        );
        assertEquals(Currency.INR, entry.currency());
    }

    @Test
    void shouldRejectDepositWhenWalletDoesNotExist() {
        when(walletRepository.findById(walletId))
                .thenReturn(Optional.empty());

        runTransactionImmediately();

        assertThrows(
                NoSuchElementException.class,
                () -> useCase.execute(
                        walletId,
                        new BigDecimal("50.00"),
                        Currency.INR
                )
        );

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void shouldRejectZeroDeposit() {
        Wallet wallet = wallet();

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        runTransactionImmediately();

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        walletId,
                        BigDecimal.ZERO,
                        Currency.INR
                )
        );

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void shouldRejectNegativeDeposit() {
        Wallet wallet = wallet();

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        runTransactionImmediately();

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        walletId,
                        new BigDecimal("-10.00"),
                        Currency.INR
                )
        );

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void shouldRejectDepositWhenCurrencyDoesNotMatchWallet() {
        Wallet wallet = wallet();

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

        runTransactionImmediately();

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
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
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
                transactionRepository,
                ledgerRepository,
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
                transactionRepository,
                ledgerRepository,
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
                transactionRepository,
                ledgerRepository,
                transactionRunner
        );
    }

    @Test
    void shouldInvokeTransactionRunner() {
        Wallet wallet = wallet();

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

        runTransactionImmediately();

        useCase.execute(
                walletId,
                new BigDecimal("25.00"),
                Currency.INR
        );

        verify(transactionRunner).execute(any(Runnable.class));
    }

    @Test
    void shouldSaveWalletAfterSuccessfulDeposit() {
        Wallet wallet = wallet();

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

        runTransactionImmediately();

        useCase.execute(
                walletId,
                new BigDecimal("75.00"),
                Currency.INR
        );

        verify(walletRepository).save(wallet);
    }

    @Test
    void shouldSaveTransactionBeforeLedger() {
        Wallet wallet = wallet();

        when(walletRepository.findById(walletId))
                .thenReturn(
                        Optional.of(wallet),
                        Optional.of(wallet)
                );

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

        AtomicReference<String> order =
                new AtomicReference<>("");

        doAnswer(invocation -> {
            order.set(order.get() + "T");
            return null;
        }).when(transactionRepository).save(any(Transaction.class));

        doAnswer(invocation -> {
            order.set(order.get() + "L");
            return null;
        }).when(ledgerRepository).save(any(Ledger.class));

        runTransactionImmediately();

        useCase.execute(
                walletId,
                new BigDecimal("50.00"),
                Currency.INR
        );

        assertEquals("TL", order.get());
    }

    @Test
    void shouldNotPersistFinancialRecordsWhenDepositFails() {
        Wallet wallet = wallet();

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        runTransactionImmediately();

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        walletId,
                        BigDecimal.ZERO,
                        Currency.INR
                )
        );

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
    }

    private Wallet wallet() {
        return Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );
    }

    private void runTransactionImmediately() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactionRunner).execute(any(Runnable.class));
    }
}