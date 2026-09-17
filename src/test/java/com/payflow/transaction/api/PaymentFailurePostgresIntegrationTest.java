package com.payflow.transaction.api;

import com.payflow.account.domain.AccountStatus;

import com.payflow.account.infrastructure.persistence.AccountEntity;
import com.payflow.account.infrastructure.persistence.SpringDataAccountRepository;
import com.payflow.shared.domain.Currency;
import com.payflow.transaction.infrastructure.persistence.PaymentEntity;
import com.payflow.transaction.infrastructure.persistence.PaymentEntity.PaymentStatusValue;
import com.payflow.transaction.infrastructure.persistence.SpringDataPaymentRepository;
import com.payflow.wallet.infrastructure.persistence.SpringDataWalletRepository;
import com.payflow.wallet.infrastructure.persistence.WalletEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.payflow.ledger.infrastructure.persistence.SpringDataLedgerEntryRepository;
import com.payflow.transaction.infrastructure.persistence.SpringDataTransactionRepository;
import com.payflow.transaction.infrastructure.persistence.SpringDataIdempotencyKeyRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.config.name=application",
                "spring.profiles.active=postgres"
        }
)
@AutoConfigureTestRestTemplate
class PaymentFailurePostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SpringDataAccountRepository accountRepository;

    @Autowired
    private SpringDataWalletRepository walletRepository;

    @Autowired
    private SpringDataPaymentRepository paymentRepository;
    
    @Autowired
    private SpringDataTransactionRepository transactionRepository;
    
    @Autowired
    private SpringDataLedgerEntryRepository ledgerEntryRepository;
    
    @Autowired
    private SpringDataIdempotencyKeyRepository idempotencyKeyRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID accountId;
    private UUID walletId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {

    	jdbcTemplate.execute("TRUNCATE TABLE ledger_entries");
    	idempotencyKeyRepository.deleteAll();
    	transactionRepository.deleteAll();
    	paymentRepository.deleteAll();
    	walletRepository.deleteAll();
    	accountRepository.deleteAll();

        accountId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        Instant now = Instant.now();

        accountRepository.save(
                new AccountEntity(
                        accountId,
                        "failure-test@example.com",
                        "Failure",
                        "Test",
                        "UNUSED",
                        AccountStatus.ACTIVE,
                        now,
                        now
                )
        );

        walletRepository.save(
                new WalletEntity(
                        walletId,
                        accountId,
                        Currency.INR,
                        new BigDecimal("1000.00")
                )
        );

        paymentRepository.save(
                new PaymentEntity(
                        paymentId,
                        "PAY-FAIL-POSTGRES-001",
                        accountId,
                        walletId,
                        null,
                        "WITHDRAWAL",
                        new BigDecimal("100.00"),
                        Currency.INR,
                        PaymentStatusValue.PENDING,
                        now,
                        now
                )
        );
    }

    @Test
    void shouldFailPaymentAgainstPostgres() {

        ResponseEntity<PaymentResponse> response =
                restTemplate.postForEntity(
                        url(),
                        null,
                        PaymentResponse.class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                paymentId,
                response.getBody().id()
        );

        assertEquals(
                "FAILED",
                response.getBody().status()
        );

        PaymentEntity persistedPayment =
                paymentRepository.findById(paymentId)
                        .orElseThrow();

        assertEquals(
                PaymentStatusValue.FAILED,
                persistedPayment.getStatus()
        );
    }

    private String url() {
        return "http://localhost:"
                + port
                + "/api/v1/payments/"
                + paymentId
                + "/failure";
    }
}
