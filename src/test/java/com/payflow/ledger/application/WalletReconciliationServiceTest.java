package com.payflow.ledger.application;

import com.payflow.account.domain.AccountId;
import com.payflow.ledger.domain.LedgerAccountId;
import com.payflow.ledger.domain.LedgerAccountType;
import com.payflow.ledger.domain.LedgerEntry;
import com.payflow.ledger.domain.LedgerEntryId;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.wallet.application.WalletRepository;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletReconciliationServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    private WalletReconciliationService service;

    private WalletId walletId;
    private AccountId accountId;
    private TransactionId transactionId;

    @BeforeEach
    void setUp() {
        service = new WalletReconciliationService(
                walletRepository,
                ledgerRepository
        );

        walletId = new WalletId(UUID.randomUUID());
        accountId = new AccountId(UUID.randomUUID());
        transactionId = new TransactionId(UUID.randomUUID());
    }

    @Test
    void shouldReconcileEmptyWalletWithZeroLedgerBalance() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByAccountId(
                LedgerAccountId.forWallet(walletId)
        )).thenReturn(List.of());

        WalletReconciliationResult result =
                service.reconcile(walletId);

        assertEquals(walletId, result.walletId());
        assertEquals(Currency.INR, result.currency());

        assertEquals(
                0,
                result.walletBalance().compareTo(new BigDecimal("0.00"))
        );

        assertEquals(
                0,
                result.ledgerBalance().compareTo(BigDecimal.ZERO)
        );

        assertEquals(
                0,
                result.difference().compareTo(BigDecimal.ZERO)
        );
      
        assertTrue(result.reconciled());
    }

    @Test
    void shouldReconcileWalletWhenLedgerMatchesBalance() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        wallet.credit(money("1000.00"));

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByAccountId(
                LedgerAccountId.forWallet(walletId)
        )).thenReturn(List.of(
                ledgerEntry("600.00", LedgerEntryType.CREDIT),
                ledgerEntry("400.00", LedgerEntryType.CREDIT)
        ));

        WalletReconciliationResult result =
                service.reconcile(walletId);

        assertEquals(
                new BigDecimal("1000.00"),
                result.walletBalance()
        );
        assertEquals(
                new BigDecimal("1000.00"),
                result.ledgerBalance()
        );
        assertEquals(
                new BigDecimal("0.00"),
                result.difference()
        );
        assertTrue(result.reconciled());
    }

    @Test
    void shouldCalculateLedgerBalanceAsCreditsMinusDebits() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        wallet.credit(money("750.00"));

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByAccountId(
                LedgerAccountId.forWallet(walletId)
        )).thenReturn(List.of(
                ledgerEntry("1000.00", LedgerEntryType.CREDIT),
                ledgerEntry("250.00", LedgerEntryType.DEBIT)
        ));

        WalletReconciliationResult result =
                service.reconcile(walletId);

        assertEquals(
                new BigDecimal("750.00"),
                result.ledgerBalance()
        );
        assertEquals(
                new BigDecimal("750.00"),
                result.walletBalance()
        );
        assertEquals(
                new BigDecimal("0.00"),
                result.difference()
        );
        assertTrue(result.reconciled());
    }

    @Test
    void shouldReportMismatchWhenWalletBalanceDiffersFromLedger() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        wallet.credit(money("750.00"));

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByAccountId(
                LedgerAccountId.forWallet(walletId)
        )).thenReturn(List.of(
                ledgerEntry("700.00", LedgerEntryType.CREDIT)
        ));

        WalletReconciliationResult result =
                service.reconcile(walletId);

        assertEquals(
                new BigDecimal("750.00"),
                result.walletBalance()
        );
        assertEquals(
                new BigDecimal("700.00"),
                result.ledgerBalance()
        );
        assertEquals(
                new BigDecimal("50.00"),
                result.difference()
        );
        assertFalse(result.reconciled());
    }

    @Test
    void shouldReportNegativeDifferenceWhenLedgerExceedsWalletBalance() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        wallet.credit(money("700.00"));

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByAccountId(
                LedgerAccountId.forWallet(walletId)
        )).thenReturn(List.of(
                ledgerEntry("750.00", LedgerEntryType.CREDIT)
        ));

        WalletReconciliationResult result =
                service.reconcile(walletId);

        assertEquals(
                new BigDecimal("-50.00"),
                result.difference()
        );
        assertFalse(result.reconciled());
    }

    @Test
    void shouldRejectMissingWallet() {
        when(walletRepository.findById(walletId))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.reconcile(walletId)
                );

        assertTrue(
                exception.getMessage().contains("wallet not found")
        );

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void shouldNotModifyWalletDuringReconciliation() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        wallet.credit(money("750.00"));

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        when(ledgerRepository.findByAccountId(
                LedgerAccountId.forWallet(walletId)
        )).thenReturn(List.of(
                ledgerEntry("750.00", LedgerEntryType.CREDIT)
        ));

        BigDecimal originalBalance =
                wallet.balance().amount();

        WalletReconciliationResult result =
                service.reconcile(walletId);

        assertTrue(result.reconciled());
        assertEquals(
                originalBalance,
                wallet.balance().amount()
        );

        verify(walletRepository, times(1))
                .findById(walletId);

        verify(walletRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectLedgerEntryWithDifferentCurrency() {
        Wallet wallet = Wallet.create(
                walletId,
                accountId,
                Currency.INR
        );

        wallet.credit(money("100.00"));

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        LedgerEntry usdEntry = LedgerEntry.create(
                LedgerEntryId.generate(),
                transactionId,
                LedgerAccountId.forWallet(walletId),
                LedgerAccountType.WALLET,
                new Money(
                        new BigDecimal("100.00"),
                        Currency.USD
                ),
                LedgerEntryType.CREDIT,
                Instant.now()
        );

        when(ledgerRepository.findByAccountId(
                LedgerAccountId.forWallet(walletId)
        )).thenReturn(List.of(usdEntry));

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.reconcile(walletId)
                );

        assertEquals(
                "ledger entry currency does not match wallet currency",
                exception.getMessage()
        );
    }

    private LedgerEntry ledgerEntry(
            String amount,
            LedgerEntryType type
    ) {
        return LedgerEntry.create(
                LedgerEntryId.generate(),
                transactionId,
                LedgerAccountId.forWallet(walletId),
                LedgerAccountType.WALLET,
                money(amount),
                type,
                Instant.now()
        );
    }

    private Money money(String amount) {
        return new Money(
                new BigDecimal(amount),
                Currency.INR
        );
    }
}
