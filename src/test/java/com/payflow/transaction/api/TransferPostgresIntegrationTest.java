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

    private String url() {
        return "http://localhost:"
                + port
                + "/api/v1/transfers";
    }
}