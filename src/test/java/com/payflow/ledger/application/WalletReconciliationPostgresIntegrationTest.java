package com.payflow.ledger.application;

import com.payflow.account.domain.AccountStatus;
import com.payflow.account.infrastructure.persistence.AccountEntity;
import com.payflow.account.infrastructure.persistence.SpringDataAccountRepository;
import com.payflow.ledger.domain.LedgerAccountId;
import com.payflow.ledger.domain.LedgerAccountType;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.ledger.infrastructure.persistence.LedgerEntryEntity;
import com.payflow.ledger.infrastructure.persistence.LedgerRepositoryAdapter;
import com.payflow.ledger.infrastructure.persistence.SpringDataLedgerEntryRepository;
import com.payflow.transaction.infrastructure.persistence.PaymentEntity;
import com.payflow.transaction.infrastructure.persistence.SpringDataPaymentRepository;
import com.payflow.shared.domain.Currency;
import com.payflow.transaction.domain.TransactionStatus;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.transaction.infrastructure.persistence.SpringDataTransactionRepository;
import com.payflow.transaction.infrastructure.persistence.TransactionEntity;
import com.payflow.wallet.domain.WalletId;
import com.payflow.wallet.infrastructure.persistence.SpringDataWalletRepository;
import com.payflow.wallet.infrastructure.persistence.WalletEntity;
import com.payflow.wallet.infrastructure.persistence.WalletRepositoryAdapter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("postgres")
class WalletReconciliationPostgresIntegrationTest {

    @Autowired
    private SpringDataWalletRepository walletRepository;

    @Autowired
    private SpringDataLedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private SpringDataAccountRepository accountRepository;

    @Autowired
    private SpringDataTransactionRepository transactionRepository;

    @Autowired
    private SpringDataPaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE ledger_entries");

        jdbcTemplate.execute(
                "TRUNCATE TABLE idempotency_keys, " +
                "payment_attempts, payments, transactions, " +
                "wallets, users CASCADE"
        );
    }

    @Test
    void shouldReportMismatchWhenWalletHasOpeningBalanceButLedgerHasNoEntries() {
        UUID accountId = UUID.randomUUID();
        UUID walletUuid = UUID.randomUUID();

        createAccount(accountId);

        walletRepository.save(
                new WalletEntity(
                        walletUuid,
                        accountId,
                        Currency.INR,
                        new BigDecimal("100.00")
                )
        );

        WalletReconciliationService service =
                new WalletReconciliationService(
                        new WalletRepositoryAdapter(walletRepository),
                        new LedgerRepositoryAdapter(
                                ledgerEntryRepository,
                                entityManager
                        )
                );

        WalletReconciliationResult result =
                service.reconcile(new WalletId(walletUuid));

        assertEquals(
                0,
                result.walletBalance().compareTo(new BigDecimal("100.00"))
        );

        assertEquals(
                0,
                result.ledgerBalance().compareTo(BigDecimal.ZERO)
        );

        assertEquals(
                0,
                result.difference().compareTo(new BigDecimal("100.00"))
        );

        assertFalse(result.reconciled());
    }

    @Test
    void shouldReconcileWhenLedgerCreditsAndDebitsMatchWalletBalance() {
        UUID accountId = UUID.randomUUID();
        UUID walletUuid = UUID.randomUUID();

        createAccount(accountId);

        walletRepository.save(
                new WalletEntity(
                        walletUuid,
                        accountId,
                        Currency.INR,
                        new BigDecimal("60.00")
                )
        );

        UUID transactionId = UUID.randomUUID();
        UUID paymentId = transactionId;

        createTransactionAndPayment(
                accountId,
                walletUuid,
                transactionId,
                paymentId
        );

        LedgerAccountId ledgerAccountId =
                LedgerAccountId.forWallet(
                        new WalletId(walletUuid)
                );

        Instant now = Instant.now();

        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> {
                    entityManager.persist(
                            new LedgerEntryEntity(
                                    UUID.randomUUID(),
                                    transactionId,
                                    ledgerAccountId.value(),
                                    LedgerAccountType.WALLET,
                                    new BigDecimal("100.00"),
                                    Currency.INR,
                                    LedgerEntryType.CREDIT,
                                    now
                            )
                    );

                    entityManager.persist(
                            new LedgerEntryEntity(
                                    UUID.randomUUID(),
                                    transactionId,
                                    ledgerAccountId.value(),
                                    LedgerAccountType.WALLET,
                                    new BigDecimal("40.00"),
                                    Currency.INR,
                                    LedgerEntryType.DEBIT,
                                    now.plusSeconds(1)
                            )
                    );

                    entityManager.flush();
                });

        entityManager.clear();

        WalletReconciliationService service =
                new WalletReconciliationService(
                        new WalletRepositoryAdapter(walletRepository),
                        new LedgerRepositoryAdapter(
                                ledgerEntryRepository,
                                entityManager
                        )
                );

        WalletReconciliationResult result =
                service.reconcile(new WalletId(walletUuid));

        assertEquals(
                new BigDecimal("60.00"),
                result.walletBalance()
        );

        assertEquals(
                new BigDecimal("60.00"),
                result.ledgerBalance()
        );

        assertEquals(
                0,
                result.difference().compareTo(BigDecimal.ZERO)
        );

        assertTrue(result.reconciled());
    }

    private void createAccount(UUID accountId) {
        Instant now = Instant.now();

        accountRepository.save(
                new AccountEntity(
                        accountId,
                        "reconciliation-" + accountId + "@test.local",
                        "Reconciliation",
                        "Test",
                        "test-password-hash",
                        AccountStatus.ACTIVE,
                        now,
                        now
                )
        );
    }

    private void createTransactionAndPayment(
            UUID accountId,
            UUID walletUuid,
            UUID transactionId,
            UUID paymentId
    ) {
        Instant now = Instant.now();

        TransactionEntity transaction =
                new TransactionEntity(
                        transactionId,
                        transactionId.toString(),
                        paymentId,
                        walletUuid,
                        walletUuid,
                        TransactionType.TRANSFER.name(),
                        new BigDecimal("60.00"),
                        Currency.INR,
                        now,
                        TransactionStatus.COMPLETED,
                        now
                );

        PaymentEntity payment =
                new PaymentEntity(
                        paymentId,
                        transactionId.toString(),
                        accountId,
                        walletUuid,
                        walletUuid,
                        TransactionType.TRANSFER.name(),
                        new BigDecimal("60.00"),
                        Currency.INR,
                        PaymentEntity.PaymentStatusValue.COMPLETED,
                        now,
                        now
                );

        paymentRepository.save(payment);
        transactionRepository.save(transaction);
    }
}