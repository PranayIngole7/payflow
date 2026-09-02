package com.payflow.transaction.api;

import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import com.payflow.account.infrastructure.persistence.AccountEntity;
import com.payflow.account.infrastructure.persistence.SpringDataAccountRepository;
import com.payflow.shared.domain.Currency;
import com.payflow.transaction.domain.TransactionStatus;
import com.payflow.transaction.infrastructure.persistence.SpringDataTransactionRepository;
import com.payflow.transaction.infrastructure.persistence.TransactionEntity;
import com.payflow.wallet.infrastructure.persistence.SpringDataWalletRepository;
import com.payflow.wallet.infrastructure.persistence.WalletEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.boot.resttestclient.TestRestTemplate;
import com.payflow.shared.api.ApiErrorResponse;
import com.payflow.transaction.api.TransactionResponse;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
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
class TransferPostgresIntegrationTest {

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

    private UUID sourceWalletId;
    private UUID destinationWalletId;

    @BeforeEach
    void setUp() {

        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        accountRepository.deleteAll();

        AccountEntity sourceAccount =
                new AccountEntity(
                        UUID.randomUUID(),
                        com.payflow.account.domain.AccountStatus.ACTIVE,
                        Instant.now()
                );

        AccountEntity destinationAccount =
                new AccountEntity(
                        UUID.randomUUID(),
                        com.payflow.account.domain.AccountStatus.ACTIVE,
                        Instant.now()
                );

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        sourceWalletId = UUID.randomUUID();
        destinationWalletId = UUID.randomUUID();

        walletRepository.save(
                new WalletEntity(
                        sourceWalletId,
                        sourceAccount.getId(),
                        Currency.INR,
                        new BigDecimal("1000.00")
                )
        );

        walletRepository.save(
                new WalletEntity(
                        destinationWalletId,
                        destinationAccount.getId(),
                        Currency.INR,
                        new BigDecimal("100.00")
                )
        );
    }

    @Test
    void shouldExecuteRealTransferAgainstPostgres() {

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
                "postgres-transfer-001"
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

        TransactionEntity transaction =
                transactionRepository.findById(transactionId)
                        .orElseThrow();

        assertEquals(
                TransactionStatus.COMPLETED,
                transaction.getStatus()
        );

        assertEquals(
                "postgres-transfer-001",
                transaction.getIdempotencyKey()
        );

        assertEquals(
                new BigDecimal("250.00"),
                transaction.getAmount()
        );
    }

    @Test
    void shouldPersistTransactionWithIdempotencyKey() {

        InitiateTransferRequest request =
                new InitiateTransferRequest(
                        sourceWalletId,
                        destinationWalletId,
                        new BigDecimal("100.00"),
                        Currency.INR
                );

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(
                "Idempotency-Key",
                "postgres-idempotency-001"
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

        UUID transactionId =
                response.getBody().transactionId();

        TransactionEntity transaction =
                transactionRepository
                        .findById(transactionId)
                        .orElseThrow();

        assertEquals(
                "postgres-idempotency-001",
                transaction.getIdempotencyKey()
        );
    }
    @Test
    void shouldReturnSameTransactionForRepeatedIdempotentRequest() {

        InitiateTransferRequest request =
                new InitiateTransferRequest(
                        sourceWalletId,
                        destinationWalletId,
                        new BigDecimal("250.00"),
                        Currency.INR
                );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "postgres-retry-001");

        HttpEntity<InitiateTransferRequest> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<InitiateTransferResponse> firstResponse =
                restTemplate.exchange(
                        url(),
                        HttpMethod.POST,
                        entity,
                        InitiateTransferResponse.class
                );

        ResponseEntity<InitiateTransferResponse> secondResponse =
                restTemplate.exchange(
                        url(),
                        HttpMethod.POST,
                        entity,
                        InitiateTransferResponse.class
                );

        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());
        assertEquals(HttpStatus.CREATED, secondResponse.getStatusCode());

        assertNotNull(firstResponse.getBody());
        assertNotNull(secondResponse.getBody());

        UUID firstTransactionId =
                firstResponse.getBody().transactionId();

        UUID secondTransactionId =
                secondResponse.getBody().transactionId();

        assertEquals(firstTransactionId, secondTransactionId);

        assertEquals(1, transactionRepository.count());

        TransactionEntity transaction =
                transactionRepository.findById(firstTransactionId)
                        .orElseThrow();

        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
        assertEquals("postgres-retry-001", transaction.getIdempotencyKey());
        assertEquals(new BigDecimal("250.00"), transaction.getAmount());

        WalletEntity sourceWallet =
                walletRepository.findById(sourceWalletId)
                        .orElseThrow();

        WalletEntity destinationWallet =
                walletRepository.findById(destinationWalletId)
                        .orElseThrow();

        assertEquals(
                new BigDecimal("750.00"),
                sourceWallet.getBalance()
        );

        assertEquals(
                new BigDecimal("350.00"),
                destinationWallet.getBalance()
        );
    }
    @Test
    void shouldRejectDifferentRequestUsingSameIdempotencyKey() {

        InitiateTransferRequest firstRequest =
                new InitiateTransferRequest(
                        sourceWalletId,
                        destinationWalletId,
                        new BigDecimal("250.00"),
                        Currency.INR
                );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "postgres-conflict-001");

        HttpEntity<InitiateTransferRequest> firstEntity =
                new HttpEntity<>(firstRequest, headers);

        ResponseEntity<InitiateTransferResponse> firstResponse =
                restTemplate.exchange(
                        url(),
                        HttpMethod.POST,
                        firstEntity,
                        InitiateTransferResponse.class
                );

        assertEquals(
                HttpStatus.CREATED,
                firstResponse.getStatusCode()
        );

        assertNotNull(firstResponse.getBody());

        UUID originalTransactionId =
                firstResponse.getBody().transactionId();

        InitiateTransferRequest secondRequest =
                new InitiateTransferRequest(
                        sourceWalletId,
                        destinationWalletId,
                        new BigDecimal("500.00"),
                        Currency.INR
                );

        HttpEntity<InitiateTransferRequest> secondEntity =
                new HttpEntity<>(secondRequest, headers);

        ResponseEntity<ApiErrorResponse> secondResponse =
                restTemplate.exchange(
                        url(),
                        HttpMethod.POST,
                        secondEntity,
                        ApiErrorResponse.class
                );

        assertEquals(
                HttpStatus.CONFLICT,
                secondResponse.getStatusCode()
        );

        assertNotNull(secondResponse.getBody());

        assertEquals(
                HttpStatus.CONFLICT.value(),
                secondResponse.getBody().status()
        );

        assertEquals(
                "idempotency key has already been used for a different transaction",
                secondResponse.getBody().message()
        );

        assertEquals(
                1,
                transactionRepository.count()
        );

        assertTrue(
                transactionRepository.findById(originalTransactionId).isPresent()
        );
    }

    @Test
    void shouldGetCompletedTransferById() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "postgres-get-transfer-001");

        String requestBody = """
        {
          "sourceWalletId": "%s",
          "destinationWalletId": "%s",
          "amount": 250.00,
          "currency": "INR"
        }
        """.formatted(sourceWalletId, destinationWalletId);

        ResponseEntity<InitiateTransferResponse> createResponse =
                restTemplate.exchange(
                        "/api/v1/transfers",
                        HttpMethod.POST,
                        new HttpEntity<>(requestBody, headers),
                        InitiateTransferResponse.class
                );

        assertThat(createResponse.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        UUID transactionId =
                createResponse.getBody().transactionId();

        ResponseEntity<TransactionResponse> getResponse =
                restTemplate.getForEntity(
                        "/api/v1/transfers/" + transactionId,
                        TransactionResponse.class
                );

        assertThat(getResponse.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        TransactionResponse response = getResponse.getBody();

        assertThat(response).isNotNull();
        assertThat(response.transactionId())
                .isEqualTo(transactionId);
        assertThat(response.sourceWalletId())
                .isEqualTo(sourceWalletId);
        assertThat(response.destinationWalletId())
                .isEqualTo(destinationWalletId);
        assertThat(response.amount())
                .isEqualByComparingTo("250.00");
        assertThat(response.currency())
                .isEqualTo("INR");
        assertThat(response.status())
                .isEqualTo("COMPLETED");
    }

    @Test
    void shouldReturnNotFoundForUnknownTransfer() {
        UUID transactionId = UUID.randomUUID();

        ResponseEntity<ApiErrorResponse> response =
                restTemplate.getForEntity(
                        "/api/v1/transfers/" + transactionId,
                        ApiErrorResponse.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .contains("transaction not found");
    }


    private String url() {
        return "http://localhost:"
                + port
                + "/api/v1/transfers";
    }
}