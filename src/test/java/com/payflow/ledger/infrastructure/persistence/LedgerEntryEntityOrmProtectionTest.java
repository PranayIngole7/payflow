package com.payflow.ledger.infrastructure.persistence;

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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.config.name=application",
                "spring.profiles.active=postgres"
        }
)
@AutoConfigureTestRestTemplate
class LedgerEntryEntityOrmProtectionTest {

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

    @Autowired
    private TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID sourceWalletId;
    private UUID destinationWalletId;

    @BeforeEach
    void setUp() {
        ledgerEntryRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentRepository.deleteAll();
        walletRepository.deleteAll();
        accountRepository.deleteAll();

        UUID sourceAccountId = UUID.randomUUID();
        UUID destinationAccountId = UUID.randomUUID();

        accountRepository.save(
                new AccountEntity(
                        sourceAccountId,
                        "source-" + UUID.randomUUID() + "@example.com",
                        "Source",
                        "User",
                        "password-hash",
                        AccountStatus.ACTIVE,
                        Instant.now(),
                        Instant.now()
                )
        );

        accountRepository.save(
                new AccountEntity(
                        destinationAccountId,
                        "destination-" + UUID.randomUUID() + "@example.com",
                        "Destination",
                        "User",
                        "password-hash",
                        AccountStatus.ACTIVE,
                        Instant.now(),
                        Instant.now()
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
    void shouldIgnoreMutationOfManagedLedgerEntry() throws Exception {

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
                "orm-protection-" + UUID.randomUUID()
        );

        HttpEntity<InitiateTransferRequest> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<InitiateTransferResponse> response =
                restTemplate.exchange(
                        url(),
                        HttpMethod.POST,
                        entity,
                        InitiateTransferResponse.class
                );

        assertEquals(
                201,
                response.getStatusCode().value(),
                "Transfer failed: " + response.getBody()
        );

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().transactionId());

        UUID transactionId =
                response.getBody().transactionId();

        LedgerEntryEntity originalEntry =
                ledgerEntryRepository
                        .findByTransactionIdOrderByCreatedAtAsc(transactionId)
                        .stream()
                        .filter(entry ->
                                entry.getType() == LedgerEntryType.DEBIT
                                        && entry.getAccountType()
                                        == LedgerAccountType.WALLET)
                        .findFirst()
                        .orElseThrow();

        UUID ledgerEntryId = originalEntry.getId();

        BigDecimal originalAmount = originalEntry.getAmount();

        assertEquals(
                new BigDecimal("250.00"),
                originalAmount
        );

        BigDecimal mutatedAmount =
                new BigDecimal("999.99");

        transactionTemplate.executeWithoutResult(status -> {
            try {
                LedgerEntryEntity managedEntry =
                        ledgerEntryRepository
                                .findById(ledgerEntryId)
                                .orElseThrow();

                assertEquals(
                        originalAmount,
                        managedEntry.getAmount()
                );

                Field amountField =
                        LedgerEntryEntity.class
                                .getDeclaredField("amount");

                amountField.setAccessible(true);

                amountField.set(
                        managedEntry,
                        mutatedAmount
                );

                assertEquals(
                        mutatedAmount,
                        managedEntry.getAmount()
                );

                entityManager.flush();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        LedgerEntryEntity reloadedEntry =
                transactionTemplate.execute(status ->
                        ledgerEntryRepository
                                .findById(ledgerEntryId)
                                .orElseThrow()
                );

        assertNotNull(reloadedEntry);

        assertEquals(
                originalAmount,
                reloadedEntry.getAmount(),
                "Hibernate @Immutable must prevent UPDATE of an existing ledger entry"
        );

        assertNotEquals(
                mutatedAmount,
                reloadedEntry.getAmount(),
                "Mutated amount must not be persisted"
        );
    }

    private String url() {
        return "http://localhost:" + port + "/api/v1/transfers";
    }
}
