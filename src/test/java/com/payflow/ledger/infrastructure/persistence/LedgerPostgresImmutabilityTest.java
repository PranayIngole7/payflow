package com.payflow.ledger.infrastructure.persistence;

import com.payflow.account.domain.AccountStatus;
import com.payflow.account.infrastructure.persistence.AccountEntity;
import com.payflow.account.infrastructure.persistence.SpringDataAccountRepository;
import com.payflow.ledger.domain.LedgerEntryType;
import com.payflow.shared.domain.Currency;
import com.payflow.transaction.api.InitiateTransferRequest;
import com.payflow.transaction.api.InitiateTransferResponse;
import com.payflow.transaction.infrastructure.persistence.SpringDataIdempotencyKeyRepository;
import com.payflow.transaction.infrastructure.persistence.SpringDataPaymentRepository;
import com.payflow.transaction.infrastructure.persistence.SpringDataTransactionRepository;
import com.payflow.wallet.infrastructure.persistence.SpringDataWalletRepository;
import com.payflow.wallet.infrastructure.persistence.WalletEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

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
class LedgerPostgresImmutabilityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private UUID sourceWalletId;
    private UUID destinationWalletId;

    @BeforeEach
    void setUp() {

        jdbcTemplate.execute("TRUNCATE TABLE ledger_entries");
        idempotencyKeyRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentRepository.deleteAll();
        walletRepository.deleteAll();
        accountRepository.deleteAll();

        UUID sourceAccountId = UUID.randomUUID();
        UUID destinationAccountId = UUID.randomUUID();

        Instant createdAt = Instant.now();

        accountRepository.save(
                new AccountEntity(
                        sourceAccountId,
                        "immutability-source-" + UUID.randomUUID() + "@example.com",
                        "Source",
                        "Account",
                        "UNUSED",
                        AccountStatus.ACTIVE,
                        createdAt,
                        createdAt
                )
        );

        accountRepository.save(
                new AccountEntity(
                        destinationAccountId,
                        "immutability-destination-" + UUID.randomUUID() + "@example.com",
                        "Destination",
                        "Account",
                        "UNUSED",
                        AccountStatus.ACTIVE,
                        createdAt,
                        createdAt
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
    }

    @Test
    void shouldRejectDirectUpdateAndDeleteOfLedgerEntry() {

        UUID transactionId = createTransfer();

        List<LedgerEntryEntity> entries =
                ledgerEntryRepository
                        .findByTransactionIdOrderByCreatedAtAsc(
                                transactionId
                        );

        assertEquals(2, entries.size());

        LedgerEntryEntity entry = entries.get(0);

        UUID ledgerEntryId = entry.getId();

        BigDecimal originalAmount = entry.getAmount();

        // ------------------------------------------------------------
        // PostgreSQL UPDATE protection
        // ------------------------------------------------------------

        DataAccessException updateException =
                assertThrows(
                        DataAccessException.class,
                        () -> jdbcTemplate.update(
                                """
                                UPDATE ledger_entries
                                SET amount = ?
                                WHERE id = ?
                                """,
                                new BigDecimal("999.99"),
                                ledgerEntryId
                        )
                );

        assertTrue(
                updateException.getMessage()
                        .contains("ledger_entries are immutable")
        );

        // ------------------------------------------------------------
        // PostgreSQL DELETE protection
        // ------------------------------------------------------------

        DataAccessException deleteException =
                assertThrows(
                        DataAccessException.class,
                        () -> jdbcTemplate.update(
                                """
                                DELETE FROM ledger_entries
                                WHERE id = ?
                                """,
                                ledgerEntryId
                        )
                );

        assertTrue(
                deleteException.getMessage()
                        .contains("ledger_entries are immutable")
        );

        // ------------------------------------------------------------
        // Original ledger entry remains unchanged
        // ------------------------------------------------------------

        LedgerEntryEntity persistedEntry =
                ledgerEntryRepository.findById(ledgerEntryId)
                        .orElseThrow();

        assertEquals(
                originalAmount,
                persistedEntry.getAmount()
        );

        assertEquals(
                LedgerEntryType.DEBIT,
                persistedEntry.getType()
        );

        assertEquals(
                transactionId,
                persistedEntry.getTransactionId()
        );
    }

    private UUID createTransfer() {

        InitiateTransferRequest request =
                new InitiateTransferRequest(
                        sourceWalletId,
                        destinationWalletId,
                        new BigDecimal("250.00"),
                        Currency.INR
                );

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(
                "Idempotency-Key",
                "immutability-" + UUID.randomUUID()
        );

        HttpEntity<InitiateTransferRequest> entity =
                new HttpEntity<>(
                        request,
                        headers
                );

        ResponseEntity<InitiateTransferResponse> response =
                restTemplate.exchange(
                        url(),
                        HttpMethod.POST,
                        entity,
                        InitiateTransferResponse.class
                );

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        UUID transactionId =
                response.getBody().transactionId();

        assertNotNull(transactionId);

        assertTrue(
                transactionRepository.findById(transactionId).isPresent()
        );

        return transactionId;
    }

    private String url() {
        return "http://localhost:" + port + "/api/v1/transfers";
    }
}
