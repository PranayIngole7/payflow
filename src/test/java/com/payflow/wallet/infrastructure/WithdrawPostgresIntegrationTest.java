package com.payflow.wallet.infrastructure;

import com.payflow.account.domain.AccountStatus;
import com.payflow.account.infrastructure.persistence.AccountEntity;
import com.payflow.account.infrastructure.persistence.SpringDataAccountRepository;
import com.payflow.ledger.domain.LedgerAccountId;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.ledger.infrastructure.persistence.LedgerEntryEntity;
import com.payflow.ledger.infrastructure.persistence.SpringDataLedgerEntryRepository;
import com.payflow.shared.domain.Currency;
import com.payflow.transaction.domain.TransactionStatus;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.transaction.infrastructure.persistence.SpringDataIdempotencyKeyRepository;
import com.payflow.transaction.infrastructure.persistence.SpringDataPaymentRepository;
import com.payflow.transaction.infrastructure.persistence.SpringDataTransactionRepository;
import com.payflow.transaction.infrastructure.persistence.TransactionEntity;
import com.payflow.wallet.api.WithdrawMoneyRequest;
import com.payflow.wallet.infrastructure.persistence.SpringDataWalletRepository;
import com.payflow.wallet.infrastructure.persistence.WalletEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.config.name=application",
                "spring.profiles.active=postgres"
        }
)
@AutoConfigureTestRestTemplate
class WithdrawPostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SpringDataAccountRepository accountRepository;

    @Autowired
    private SpringDataWalletRepository walletRepository;

    @Autowired
    private SpringDataTransactionRepository transactionRepository;

    @Autowired
    private SpringDataLedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private SpringDataPaymentRepository paymentRepository;

    @Autowired
    private SpringDataIdempotencyKeyRepository idempotencyKeyRepository;

    private UUID walletId;

    @BeforeEach
    void setUp() {

        ledgerEntryRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentRepository.deleteAll();
        walletRepository.deleteAll();
        accountRepository.deleteAll();

        UUID accountId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        AccountEntity account =
                new AccountEntity(
                        accountId,
                        "withdraw@example.com",
                        "Withdraw",
                        "Test",
                        "UNUSED",
                        AccountStatus.ACTIVE,
                        createdAt,
                        createdAt
                );

        accountRepository.save(account);

        walletId = UUID.randomUUID();

        walletRepository.save(
                new WalletEntity(
                        walletId,
                        accountId,
                        Currency.INR,
                        new BigDecimal("100.00")
                )
        );
    }

    @Test
    void shouldExecuteRealWithdrawalAgainstPostgres() {

        WithdrawMoneyRequest request =
                new WithdrawMoneyRequest(
                        new BigDecimal("40.00"),
                        Currency.INR
                );

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<WithdrawMoneyRequest> entity =
                new HttpEntity<>(
                        request,
                        headers
                );

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url(),
                        HttpMethod.POST,
                        entity,
                        String.class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        // ------------------------------------------------------------
        // Wallet balance verification
        // ------------------------------------------------------------

        WalletEntity persistedWallet =
                walletRepository.findById(walletId)
                        .orElseThrow();

        assertEquals(
                new BigDecimal("60.00"),
                persistedWallet.getBalance()
        );

        // ------------------------------------------------------------
        // Transaction verification
        // ------------------------------------------------------------

        List<TransactionEntity> transactions =
                transactionRepository.findAll();

        assertEquals(
                1,
                transactions.size()
        );

        TransactionEntity transaction =
                transactions.getFirst();

        assertEquals(
                TransactionType.WITHDRAWAL.name(),
                transaction.getType()
        );

        assertEquals(
                TransactionStatus.PENDING,
                transaction.getStatus()
        );

        assertEquals(
                walletId,
                transaction.getSourceWalletId()
        );

        assertNull(
                transaction.getDestinationWalletId()
        );

        assertEquals(
                new BigDecimal("40.00"),
                transaction.getAmount()
        );

        assertEquals(
                Currency.INR,
                transaction.getCurrency()
        );

        // ------------------------------------------------------------
        // Ledger verification
        // ------------------------------------------------------------

        List<LedgerEntryEntity> ledgerEntries =
                ledgerEntryRepository
                        .findByTransactionIdOrderByCreatedAtAsc(
                                transaction.getId()
                        );

        assertEquals(
                2,
                ledgerEntries.size()
        );

        // Wallet account -> DEBIT
        LedgerEntryEntity debitEntry =
                ledgerEntries.get(0);

        assertEquals(
                LedgerEntryType.DEBIT,
                debitEntry.getType()
        );

        assertEquals(
                walletId,
                debitEntry.getLedgerAccountId()
        );

        assertEquals(
                new BigDecimal("40.00"),
                debitEntry.getAmount()
        );

        assertEquals(
                Currency.INR,
                debitEntry.getCurrency()
        );

        assertEquals(
                transaction.getId(),
                debitEntry.getTransactionId()
        );

        // External settlement account -> CREDIT
        LedgerEntryEntity creditEntry =
                ledgerEntries.get(1);

        assertEquals(
                LedgerEntryType.CREDIT,
                creditEntry.getType()
        );

        assertEquals(
                LedgerAccountId.externalSettlement().value(),
                creditEntry.getLedgerAccountId()
        );

        assertEquals(
                new BigDecimal("40.00"),
                creditEntry.getAmount()
        );

        assertEquals(
                Currency.INR,
                creditEntry.getCurrency()
        );

        assertEquals(
                transaction.getId(),
                creditEntry.getTransactionId()
        );
    }

    private String url() {
        return "http://localhost:" + port
                + "/api/v1/wallets/"
                + walletId
                + "/withdrawals";
    }
}

