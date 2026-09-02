package com.payflow.transaction.application;

import com.payflow.account.domain.AccountId;
import com.payflow.ledger.application.LedgerRepository;
import com.payflow.ledger.domain.Ledger;
import com.payflow.ledger.domain.LedgerEntry;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.application.WalletRepository;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferMoneyUseCaseTest {


    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private TransactionRunner transactionRunner;

    private TransferMoneyUseCase useCase;

    private Wallet sourceWallet;
    private Wallet destinationWallet;
    private Transaction transaction;

    private TransactionId transactionId;
    private WalletId sourceWalletId;
    private WalletId destinationWalletId;

    private Money transferAmount;

    @BeforeEach
    void setUp() {
        useCase = new TransferMoneyUseCase(
                walletRepository,
                transactionRepository,
                ledgerRepository,
                transactionRunner
        );

        transactionId = TransactionId.generate();
        sourceWalletId = WalletId.generate();
        destinationWalletId = WalletId.generate();

        transferAmount =
                new Money(new BigDecimal("100.00"), Currency.INR);

        AccountId sourceAccountId = AccountId.generate();
        AccountId destinationAccountId = AccountId.generate();

        sourceWallet = Wallet.create(
                sourceWalletId,
                sourceAccountId,
                Currency.INR
        );

        destinationWallet = Wallet.create(
                destinationWalletId,
                destinationAccountId,
                Currency.INR
        );

        /*
         * Wallet.create() starts with zero balance.
         * Fund the source wallet through its public domain API.
         */
        sourceWallet.credit(
                new Money(new BigDecimal("1000.00"), Currency.INR)
        );

        transaction = Transaction.create(
                transactionId,
                sourceWalletId,
                destinationWalletId,
                transferAmount,
                Instant.now()
        );
    }

    @Test
    void shouldExecuteTransferInsideTransactionRunner() {
        givenTransactionExists();
        givenWalletsExist();
        givenNoExistingLedger();

        executeRunnableImmediately();

        useCase.execute(transactionId);

        verify(transactionRunner).execute(any(Runnable.class));

        verify(transactionRepository).findById(transactionId);
        verify(walletRepository).findById(sourceWalletId);
        verify(walletRepository).findById(destinationWalletId);

        verify(walletRepository).save(sourceWallet);
        verify(walletRepository).save(destinationWallet);

        verify(ledgerRepository).findByTransactionId(transactionId);
        verify(ledgerRepository).save(any(Ledger.class));

        verify(transactionRepository).save(transaction);

        assertEquals(
                new BigDecimal("900.00"),
                sourceWallet.balance().amount()
        );

        assertEquals(
                new BigDecimal("100.00"),
                destinationWallet.balance().amount()
        );

        assertEquals(
                com.payflow.transaction.domain.TransactionStatus.COMPLETED,
                transaction.status()
        );
    }

    @Test
    void shouldRejectNullTransactionId() {
        assertThrows(
                NullPointerException.class,
                () -> useCase.execute(null)
        );

        verifyNoInteractions(
                transactionRepository,
                walletRepository,
                ledgerRepository,
                transactionRunner
        );
    }

    @Test
    void shouldFailWhenTransactionDoesNotExist() {
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.empty());

        executeRunnableImmediately();

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(transactionId)
        );

        verify(transactionRunner).execute(any(Runnable.class));
        verify(transactionRepository).findById(transactionId);

        verifyNoInteractions(
                walletRepository,
                ledgerRepository
        );
    }

    @Test
    void shouldFailWhenSourceWalletDoesNotExist() {
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(walletRepository.findById(sourceWalletId))
                .thenReturn(Optional.empty());

        executeRunnableImmediately();

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(transactionId)
        );

        verify(transactionRunner).execute(any(Runnable.class));

        verify(transactionRepository).findById(transactionId);
        verify(walletRepository).findById(sourceWalletId);

        verify(walletRepository, never())
                .findById(destinationWalletId);

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void shouldFailWhenDestinationWalletDoesNotExist() {
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(walletRepository.findById(sourceWalletId))
                .thenReturn(Optional.of(sourceWallet));

        when(walletRepository.findById(destinationWalletId))
                .thenReturn(Optional.empty());

        executeRunnableImmediately();

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(transactionId)
        );

        verify(transactionRunner).execute(any(Runnable.class));

        verify(transactionRepository).findById(transactionId);
        verify(walletRepository).findById(sourceWalletId);
        verify(walletRepository).findById(destinationWalletId);

        verifyNoInteractions(ledgerRepository);

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenSourceWalletHasInsufficientFunds() {
        sourceWallet = Wallet.create(
                sourceWalletId,
                AccountId.generate(),
                Currency.INR
        );

        sourceWallet.credit(
                new Money(new BigDecimal("50.00"), Currency.INR)
        );

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(walletRepository.findById(sourceWalletId))
                .thenReturn(Optional.of(sourceWallet));

        when(walletRepository.findById(destinationWalletId))
                .thenReturn(Optional.of(destinationWallet));

        executeRunnableImmediately();

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(transactionId)
        );

        verify(transactionRunner).execute(any(Runnable.class));

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());

        assertEquals(
                new BigDecimal("50.00"),
                sourceWallet.balance().amount()
        );

        assertEquals(
                BigDecimal.ZERO,
                destinationWallet.balance().amount()
        );

        assertEquals(
                com.payflow.transaction.domain.TransactionStatus.PENDING,
                transaction.status()
        );
    }

    @Test
    void shouldCreateNewLedgerWhenNoneExists() {
        givenTransactionExists();
        givenWalletsExist();
        givenNoExistingLedger();

        executeRunnableImmediately();

        useCase.execute(transactionId);

        ArgumentCaptor<Ledger> ledgerCaptor =
                ArgumentCaptor.forClass(Ledger.class);

        verify(ledgerRepository).save(ledgerCaptor.capture());

        Ledger savedLedger = ledgerCaptor.getValue();

        assertNotNull(savedLedger);
        assertTrue(savedLedger.isBalanced());
        assertEquals(2, savedLedger.entries().size());

        assertEquals(
                LedgerEntryType.DEBIT,
                savedLedger.entries().get(0).type()
        );

        assertEquals(
                LedgerEntryType.CREDIT,
                savedLedger.entries().get(1).type()
        );
    }

    @Test
    void shouldUseExistingLedger() {
        Ledger existingLedger = Ledger.create();

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(walletRepository.findById(sourceWalletId))
                .thenReturn(Optional.of(sourceWallet));

        when(walletRepository.findById(destinationWalletId))
                .thenReturn(Optional.of(destinationWallet));

        when(ledgerRepository.findByTransactionId(transactionId))
                .thenReturn(Optional.of(existingLedger));

        executeRunnableImmediately();

        useCase.execute(transactionId);

        verify(ledgerRepository)
                .findByTransactionId(transactionId);

        verify(ledgerRepository)
                .save(existingLedger);

        assertTrue(existingLedger.isBalanced());
        assertEquals(2, existingLedger.entries().size());
    }

    @Test
    void shouldCreateDebitAndCreditLedgerEntries() {
        givenTransactionExists();
        givenWalletsExist();
        givenNoExistingLedger();

        executeRunnableImmediately();

        useCase.execute(transactionId);

        ArgumentCaptor<Ledger> ledgerCaptor =
                ArgumentCaptor.forClass(Ledger.class);

        verify(ledgerRepository).save(ledgerCaptor.capture());

        Ledger ledger = ledgerCaptor.getValue();

        assertEquals(2, ledger.entries().size());

        LedgerEntry debit = ledger.entries().get(0);
        LedgerEntry credit = ledger.entries().get(1);

        assertEquals(
                LedgerEntryType.DEBIT,
                debit.type()
        );

        assertEquals(
                LedgerEntryType.CREDIT,
                credit.type()
        );

        assertEquals(
                transferAmount,
                debit.amount()
        );

        assertEquals(
                transferAmount,
                credit.amount()
        );

        assertEquals(
                sourceWalletId,
                debit.walletId()
        );

        assertEquals(
                destinationWalletId,
                credit.walletId()
        );

        assertEquals(
                transactionId,
                debit.transactionId()
        );

        assertEquals(
                transactionId,
                credit.transactionId()
        );
    }

    @Test
    void shouldCompleteTransactionOnlyAfterLedgerIsBalanced() {
        givenTransactionExists();
        givenWalletsExist();
        givenNoExistingLedger();

        executeRunnableImmediately();

        useCase.execute(transactionId);

        assertEquals(
                com.payflow.transaction.domain.TransactionStatus.COMPLETED,
                transaction.status()
        );
    }

    @Test
    void shouldNotExecuteTransferWhenTransactionRunnerDoesNotRunWork() {
        /*
         * The transaction runner accepts the work but deliberately does not
         * execute it. Therefore, the use case must not touch any repository
         * or mutate any domain object.
         */
        doNothing()
                .when(transactionRunner)
                .execute(any(Runnable.class));


        useCase.execute(transactionId);

        verify(transactionRunner).execute(any(Runnable.class));

        verifyNoInteractions(
                transactionRepository,
                walletRepository,
                ledgerRepository
        );

        assertEquals(
                new BigDecimal("1000.00"),
                sourceWallet.balance().amount()
        );

        assertEquals(
                BigDecimal.ZERO,
                destinationWallet.balance().amount()
        );

        assertEquals(
                com.payflow.transaction.domain.TransactionStatus.PENDING,
                transaction.status()
        );


    }


    @Test
    void shouldNotSaveAnythingWhenTransferFails() {
        sourceWallet = Wallet.create(
                sourceWalletId,
                AccountId.generate(),
                Currency.INR
        );

        sourceWallet.credit(
                new Money(new BigDecimal("50.00"), Currency.INR)
        );

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(walletRepository.findById(sourceWalletId))
                .thenReturn(Optional.of(sourceWallet));

        when(walletRepository.findById(destinationWalletId))
                .thenReturn(Optional.of(destinationWallet));

        executeRunnableImmediately();

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(transactionId)
        );

        verify(transactionRunner).execute(any(Runnable.class));

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void shouldPropagateExceptionFromTransactionWork() {
        RuntimeException failure =
                new RuntimeException("transaction infrastructure failure");

        doThrow(failure)
                .when(transactionRunner)
                .execute(any(Runnable.class));

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> useCase.execute(transactionId)
        );

        assertSame(failure, thrown);

        verify(transactionRunner)
                .execute(any(Runnable.class));

        verifyNoInteractions(
                transactionRepository,
                walletRepository,
                ledgerRepository
        );
    }

    @Test
    void shouldRequireTransactionRunner() {
        assertThrows(
                NullPointerException.class,
                () -> new TransferMoneyUseCase(
                        walletRepository,
                        transactionRepository,
                        ledgerRepository,
                        null
                )
        );
    }

    @Test
    void shouldRequireRepositories() {
        assertThrows(
                NullPointerException.class,
                () -> new TransferMoneyUseCase(
                        null,
                        transactionRepository,
                        ledgerRepository,
                        transactionRunner
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new TransferMoneyUseCase(
                        walletRepository,
                        null,
                        ledgerRepository,
                        transactionRunner
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new TransferMoneyUseCase(
                        walletRepository,
                        transactionRepository,
                        null,
                        transactionRunner
                )
        );
    }
    @Test
    void shouldRejectAlreadyCompletedTransaction() {
        transaction.complete();

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        executeRunnableImmediately();

        assertThrows(
                IllegalStateException.class,
                () -> useCase.execute(transactionId)
        );

        verify(transactionRunner).execute(any(Runnable.class));
        verify(transactionRepository).findById(transactionId);

        verifyNoInteractions(walletRepository, ledgerRepository);
    }

    @Test
    void shouldRejectAlreadyFailedTransaction() {
        transaction.fail();

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        executeRunnableImmediately();

        assertThrows(
                IllegalStateException.class,
                () -> useCase.execute(transactionId)
        );

        verify(transactionRunner).execute(any(Runnable.class));
        verify(transactionRepository).findById(transactionId);

        verifyNoInteractions(walletRepository, ledgerRepository);
    }

    private void givenTransactionExists() {
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));
    }

    private void givenWalletsExist() {
        when(walletRepository.findById(sourceWalletId))
                .thenReturn(Optional.of(sourceWallet));

        when(walletRepository.findById(destinationWalletId))
                .thenReturn(Optional.of(destinationWallet));
    }

    private void givenNoExistingLedger() {
        when(ledgerRepository.findByTransactionId(transactionId))
                .thenReturn(Optional.empty());
    }

    /**
     * Makes the mocked transaction runner execute the Runnable supplied by
     * the use case.
     *
     * <p>TransactionRunner.execute(...) returns void, so we must execute
     * the captured Runnable explicitly rather than attempting to return it.</p>
     */
    private void executeRunnableImmediately() {
        doAnswer(invocation -> {
            Runnable work = invocation.getArgument(0);
            work.run();
            return null;
        }).when(transactionRunner).execute(any(Runnable.class));
    }


}
