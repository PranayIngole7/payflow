package com.payflow.ledger.api;

import com.payflow.account.domain.AccountStatus;
import com.payflow.account.infrastructure.persistence.AccountEntity;
import com.payflow.account.infrastructure.persistence.SpringDataAccountRepository;
import com.payflow.ledger.domain.LedgerAccountType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.config.name=application",
                "spring.profiles.active=postgres"
        }
)
@AutoConfigureTestRestTemplate
class LedgerApiPostgresIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

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
    private JdbcTemplate jdbcTemplate;

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

        Instant now = Instant.now();

        accountRepository.save(
                new AccountEntity(
                        sourceAccountId,
                        "ledger-api-source-" + UUID.randomUUID()
                                + "@example.com",
                        "Source",
                        "Account",
                        "UNUSED",
                        AccountStatus.ACTIVE,
                        now,
                        now
                )
        );

        accountRepository.save(
                new AccountEntity(
                        destinationAccountId,
                        "ledger-api-destination-" + UUID.randomUUID()
                                + "@example.com",
                        "Destination",
                        "Account",
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
    }

    @Test
    void shouldReadLedgerEntriesByTransaction() {
        UUID transactionId = executeTransfer();

        ResponseEntity<LedgerEntryResponse[]> response =
                restTemplate.getForEntity(
                        "/api/v1/ledger/transactions/" + transactionId,
                        LedgerEntryResponse[].class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        LedgerEntryResponse[] entries = response.getBody();

        assertEquals(2, entries.length);

        LedgerEntryResponse debit =
                List.of(entries)
                        .stream()
                        .filter(entry ->
                                entry.entryType() == LedgerEntryType.DEBIT
                        )
                        .findFirst()
                        .orElseThrow();

        LedgerEntryResponse credit =
                List.of(entries)
                        .stream()
                        .filter(entry ->
                                entry.entryType() == LedgerEntryType.CREDIT
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                transactionId,
                debit.transactionId()
        );

        assertEquals(
                transactionId,
                credit.transactionId()
        );

        assertEquals(
                sourceWalletId,
                debit.ledgerAccountId()
        );

        assertEquals(
                destinationWalletId,
                credit.ledgerAccountId()
        );

        assertEquals(
                LedgerAccountType.WALLET,
                debit.accountType()
        );

        assertEquals(
                LedgerAccountType.WALLET,
                credit.accountType()
        );

        assertEquals(
                0,
                new BigDecimal("250.00")
                        .compareTo(debit.amount())
        );

        assertEquals(
                0,
                new BigDecimal("250.00")
                        .compareTo(credit.amount())
        );

        assertEquals(
                Currency.INR,
                debit.currency()
        );

        assertEquals(
                Currency.INR,
                credit.currency()
        );

        assertNotNull(debit.createdAt());
        assertNotNull(credit.createdAt());
    }

    @Test
    void shouldReadLedgerEntriesByWalletLedgerAccount() {
        UUID transactionId = executeTransfer();

        ResponseEntity<LedgerEntryResponse[]> response =
                restTemplate.getForEntity(
                        "/api/v1/ledger/accounts/" + sourceWalletId,
                        LedgerEntryResponse[].class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        LedgerEntryResponse[] entries = response.getBody();

        assertEquals(1, entries.length);

        LedgerEntryResponse entry = entries[0];

        assertEquals(
                transactionId,
                entry.transactionId()
        );

        assertEquals(
                sourceWalletId,
                entry.ledgerAccountId()
        );

        assertEquals(
                LedgerAccountType.WALLET,
                entry.accountType()
        );

        assertEquals(
                LedgerEntryType.DEBIT,
                entry.entryType()
        );

        assertEquals(
                0,
                new BigDecimal("250.00")
                        .compareTo(entry.amount())
        );

        assertEquals(
                Currency.INR,
                entry.currency()
        );
    }

    @Test
    void shouldReturnEmptyListForTransactionWithoutLedgerEntries() {
        UUID transactionId = UUID.randomUUID();

        ResponseEntity<LedgerEntryResponse[]> response =
                restTemplate.getForEntity(
                        "/api/v1/ledger/transactions/" + transactionId,
                        LedgerEntryResponse[].class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                0,
                response.getBody().length
        );
    }

    @Test
    void shouldReturnEmptyListForLedgerAccountWithoutEntries() {
        UUID ledgerAccountId = UUID.randomUUID();

        ResponseEntity<LedgerEntryResponse[]> response =
                restTemplate.getForEntity(
                        "/api/v1/ledger/accounts/" + ledgerAccountId,
                        LedgerEntryResponse[].class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                0,
                response.getBody().length
        );
    }

    @Test
    void shouldNotExposeLedgerMutationEndpoints() {
        ResponseEntity<String> postResponse =
                restTemplate.postForEntity(
                        "http://localhost:" + port + "/api/v1/ledger",
                        "{}",
                        String.class
                );

        assertFalse(
                postResponse.getStatusCode().is2xxSuccessful()
        );

        ResponseEntity<String> deleteResponse =
                restTemplate.exchange(
                        "http://localhost:" + port
                                + "/api/v1/ledger/accounts/"
                                + sourceWalletId,
                        HttpMethod.DELETE,
                        null,
                        String.class
                );

        assertFalse(
                deleteResponse.getStatusCode().is2xxSuccessful()
        );
    }

    private UUID executeTransfer() {
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
                "ledger-api-transfer-" + UUID.randomUUID()
        );

        HttpEntity<InitiateTransferRequest> entity =
                new HttpEntity<>(
                        request,
                        headers
                );

        ResponseEntity<InitiateTransferResponse> response =
                restTemplate.exchange(
                        "http://localhost:" + port + "/api/v1/transfers",
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

        return transactionId;
    }
}

