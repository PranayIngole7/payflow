package com.payflow.transaction.application;

import com.payflow.account.domain.AccountStatus;

import com.payflow.account.infrastructure.persistence.AccountEntity;
import com.payflow.account.infrastructure.persistence.SpringDataAccountRepository;
import com.payflow.ledger.infrastructure.persistence.LedgerEntryEntity;
import com.payflow.ledger.infrastructure.persistence.SpringDataLedgerEntryRepository;
import com.payflow.shared.domain.Currency;
import com.payflow.transaction.domain.Transaction;
import com.payflow.transaction.domain.TransactionId;
import com.payflow.transaction.domain.TransactionStatus;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.transaction.infrastructure.persistence.IdempotencyKeyEntity;
import com.payflow.transaction.infrastructure.persistence.PaymentEntity;
import com.payflow.transaction.infrastructure.persistence.SpringDataIdempotencyKeyRepository;
import com.payflow.transaction.infrastructure.persistence.SpringDataPaymentRepository;
import com.payflow.transaction.infrastructure.persistence.SpringDataTransactionRepository;
import com.payflow.transaction.infrastructure.persistence.TransactionEntity;
import com.payflow.wallet.infrastructure.persistence.SpringDataWalletRepository;
import com.payflow.wallet.infrastructure.persistence.WalletEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        properties = {
                "spring.config.name=application",
                "spring.profiles.active=postgres"
        }
)
class TransferTransactionRollbackPostgresIntegrationTest {

    @Autowired
    private TransferMoneyUseCase transferMoneyUseCase;

    @Autowired
    private SpringDataAccountRepository accountRepository;

    @Autowired
    private SpringDataWalletRepository walletRepository;

    @Autowired
    private SpringDataTransactionRepository transactionRepository;

    @Autowired
    private SpringDataPaymentRepository paymentRepository;

    @Autowired
    private SpringDataIdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private SpringDataLedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID sourceWalletId;
    private UUID destinationWalletId;
    private UUID transactionId;
    private UUID paymentId;
    private UUID sourceAccountId;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {

        jdbcTemplate.execute("TRUNCATE TABLE ledger_entries");
        idempotencyKeyRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentRepository.deleteAll();
        walletRepository.deleteAll();
        accountRepository.deleteAll();

        sourceAccountId = UUID.randomUUID();
        UUID destinationAccountId = UUID.randomUUID();

        Instant now = Instant.now();

        accountRepository.save(
                new AccountEntity(
                        sourceAccountId,
                        "rollback-source@example.com",
                        "Rollback",
                        "Source",
                        "UNUSED",
                        AccountStatus.ACTIVE,
                        now,
                        now
                )
        );

        accountRepository.save(
                new AccountEntity(
                        destinationAccountId,
                        "rollback-destination@example.com",
                        "Rollback",
                        "Destination",
                        "UNUSED",
                        AccountStatus.ACTIVE,
                        now,
                        now
                )
        );

        sourceWalletId = UUID.randomUUID();
        destinationWalletId = UUID.randomUUID();

        walletRepository.save(
                new WalletEntity(
                        sourceWalletId,
                        sourceAccountId,
                        Currency.INR,
                        new BigDecimal("1000.00")
                )
        );

        walletRepository.save(
                new WalletEntity(
                        destinationWalletId,
                        destinationAccountId,
                        Currency.INR,
                        new BigDecimal("100.00")
                )
        );

        transactionId = UUID.randomUUID();
        paymentId = transactionId;
        idempotencyKey = "rollback-transfer-001";

        TransactionEntity transaction =
                new TransactionEntity(
                        transactionId,
                        transactionId.toString(),
                        paymentId,
                        sourceWalletId,
                        destinationWalletId,
                        TransactionType.TRANSFER.name(),
                        new BigDecimal("250.00"),
                        Currency.INR,
                        now,
                        TransactionStatus.PENDING,
                        null
                );

        PaymentEntity payment =
                new PaymentEntity(
                        paymentId,
                        transactionId.toString(),
                        sourceAccountId,
                        sourceWalletId,
                        destinationWalletId,
                        TransactionType.TRANSFER.name(),
                        new BigDecimal("250.00"),
                        Currency.INR,
                        PaymentEntity.PaymentStatusValue.PENDING,
                        now,
                        now
                );

        String requestHash =
                "preexisting-request-hash-for-rollback-test";

        IdempotencyKeyEntity idempotency =
                new IdempotencyKeyEntity(
                        UUID.randomUUID(),
                        sourceAccountId,
                        idempotencyKey,
                        requestHash,
                        paymentId,
                        now,
                        now.plusSeconds(3600)
                );

        paymentRepository.save(payment);
        transactionRepository.save(transaction);
        idempotencyKeyRepository.save(idempotency);
    }

    @Test
    void shouldRollbackEntireTransferWhenFinalPersistenceFails() {

        TransactionId id = new TransactionId(transactionId);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> transferMoneyUseCase.execute(id)
        );

        WalletEntity sourceWallet =
                walletRepository.findById(sourceWalletId)
                        .orElseThrow();

        WalletEntity destinationWallet =
                walletRepository.findById(destinationWalletId)
                        .orElseThrow();

        assertEquals(
                new BigDecimal("1000.00"),
                sourceWallet.getBalance()
        );

        assertEquals(
                new BigDecimal("100.00"),
                destinationWallet.getBalance()
        );

        TransactionEntity transaction =
                transactionRepository.findById(transactionId)
                        .orElseThrow();

        assertEquals(
                TransactionStatus.PENDING,
                transaction.getStatus()
        );

        List<LedgerEntryEntity> ledgerEntries =
                ledgerEntryRepository
                        .findByTransactionIdOrderByCreatedAtAsc(
                                transactionId
                        );

        assertTrue(ledgerEntries.isEmpty());

        assertTrue(
                paymentRepository.findById(paymentId).isPresent()
        );

        assertTrue(
                idempotencyKeyRepository
                        .findByKey(idempotencyKey)
                        .isPresent()
        );
    }
}
