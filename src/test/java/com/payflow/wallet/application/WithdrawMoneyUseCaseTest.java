package com.payflow.wallet.application;

import com.payflow.account.domain.AccountId;
import com.payflow.ledger.application.LedgerRepository;
import com.payflow.ledger.domain.Ledger;
import com.payflow.ledger.domain.LedgerAccountId;
import com.payflow.ledger.domain.LedgerAccountType;
import com.payflow.ledger.domain.LedgerEntry;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.TransactionRepository;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.transaction.domain.TransactionStatus;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WithdrawMoneyUseCaseTest {

    private WalletRepository walletRepository;
    private TransactionRepository transactionRepository;
    private LedgerRepository ledgerRepository;
    private TransactionRunner transactionRunner;
    private WithdrawMoneyUseCase useCase;

    private WalletId walletId;
    private AccountId accountId;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        ledgerRepository = mock(LedgerRepository.class);
        transactionRunner = mock(TransactionRunner.class);

        useCase = new WithdrawMoneyUseCase(
                walletRepository,
                transactionRepository,
                ledgerRepository,
                transactionRunner
        );

        walletId = WalletId.generate();
        accountId = AccountId.generate();
    }

    @Test
    void shouldWithdrawMoneyFromWallet() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

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
        verify(transactionRepository).save(any(Transaction.class));
        verify(ledgerRepository).save(any(Ledger.class));
        verify(transactionRunner).execute(any(Runnable.class));
    }

    @Test
    void shouldWithdrawEntireWalletBalance() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

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
        verify(transactionRepository).save(any(Transaction.class));
        verify(ledgerRepository).save(any(Ledger.class));
    }

    @Test
    void shouldCreateWithdrawalTransaction() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

        AtomicReference<Transaction> savedTransaction =
                new AtomicReference<>();

        doAnswer(invocation -> {
            savedTransaction.set(invocation.getArgument(0));
            return null;
        }).when(transactionRepository).save(any(Transaction.class));

        executeTransactionImmediately();

        useCase.execute(
                walletId,
                new BigDecimal("40.00"),
                Currency.INR
        );

        Transaction transaction = savedTransaction.get();

        assertNotNull(transaction);

        assertEquals(
                TransactionType.WITHDRAWAL,
                transaction.type()
        );

        assertEquals(
                walletId,
                transaction.sourceWalletId()
        );

        assertNull(
                transaction.destinationWalletId()
        );

        assertEquals(
                new BigDecimal("40.00"),
                transaction.amount().amount()
        );

        assertEquals(
                Currency.INR,
                transaction.currency()
        );

        assertEquals(
                TransactionStatus.PENDING,
                transaction.status()
        );
    }

    @Test
    void shouldCreateDebitLedgerEntry() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

        AtomicReference<Ledger> savedLedger =
                new AtomicReference<>();

        doAnswer(invocation -> {
            savedLedger.set(invocation.getArgument(0));
            return null;
        }).when(ledgerRepository).save(any(Ledger.class));

        executeTransactionImmediately();

        useCase.execute(
                walletId,
                new BigDecimal("40.00"),
                Currency.INR
        );

        Ledger ledger = savedLedger.get();

        assertNotNull(ledger);

        assertEquals(
                2,
                ledger.entries().size()
        );

        LedgerEntry walletEntry = ledger.entries().stream()
                .filter(entry ->
                        entry.accountType() == LedgerAccountType.WALLET
                )
                .findFirst()
                .orElseThrow();

        assertEquals(
                LedgerEntryType.DEBIT,
                walletEntry.type()
        );

        assertEquals(
                LedgerAccountId.forWallet(walletId),
                walletEntry.ledgerAccountId()
        );

        assertEquals(
                new BigDecimal("40.00"),
                walletEntry.amount().amount()
        );

        assertEquals(
                Currency.INR,
                walletEntry.currency()
        );
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
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
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
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
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
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
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
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
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
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

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
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByTransactionId(any(TransactionId.class)))
                .thenReturn(Optional.empty());

        executeTransactionImmediately();

        useCase.execute(
                walletId,
                new BigDecimal("25.00"),
                Currency.INR
        );

        verify(walletRepository).save(wallet);
    }

    @Test
    void shouldSaveTransactionBeforeLedger() {
        Wallet wallet = createWalletWithBalance("100.00");

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

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

        executeTransactionImmediately();

        useCase.execute(
                walletId,
                new BigDecimal("25.00"),
                Currency.INR
        );

        assertEquals(
                "TL",
                order.get()
        );
    }

    @Test
    void shouldNotPersistFinancialRecordsWhenWithdrawalFails() {
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
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
    }

    private Wallet createWalletWithBalance(String amount) {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        wallet.credit(
                new Money(
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