package com.payflow.wallet.api;

import com.payflow.account.api.AccountController;
import com.payflow.account.api.AccountResponse;
import com.payflow.shared.domain.Currency;
import com.payflow.wallet.infrastructure.persistence.SpringDataWalletRepository;
import com.payflow.wallet.infrastructure.persistence.WalletEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.config.name=application",
                "spring.profiles.active=postgres"
        }
)
@AutoConfigureTestRestTemplate
class WalletApiPostgresIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SpringDataWalletRepository walletRepository;

    @Test
    void shouldDepositThroughApi() {
        UUID walletId = createWallet("1000.00");

        DepositMoneyRequest request =
                new DepositMoneyRequest(
                        new BigDecimal("500.00"),
                        Currency.INR
                );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/wallets/" + walletId + "/deposits",
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertBalance(walletId, "1500.00");
    }

    @Test
    void shouldWithdrawThroughApi() {
        UUID walletId = createWallet("1000.00");

        WithdrawMoneyRequest request =
                new WithdrawMoneyRequest(
                        new BigDecimal("400.00"),
                        Currency.INR
                );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/wallets/" + walletId + "/withdrawals",
                request,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertBalance(walletId, "600.00");
    }

    @Test
    void shouldRejectInvalidDepositAmount() {
        UUID walletId = createWallet("1000.00");

        DepositMoneyRequest request =
                new DepositMoneyRequest(
                        BigDecimal.ZERO,
                        Currency.INR
                );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/wallets/" + walletId + "/deposits",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertBalance(walletId, "1000.00");
    }

    @Test
    void shouldRejectMissingWithdrawalCurrency() {
        UUID walletId = createWallet("1000.00");

        String request = """
                {
                    "amount": 100.00
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/wallets/" + walletId + "/withdrawals",
                new HttpEntity<>(request, headers),
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertBalance(walletId, "1000.00");
    }

    @Test
    void shouldRejectWithdrawalWhenBalanceIsInsufficient() {
        UUID walletId = createWallet("1000.00");

        WithdrawMoneyRequest request =
                new WithdrawMoneyRequest(
                        new BigDecimal("1500.00"),
                        Currency.INR
                );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/wallets/" + walletId + "/withdrawals",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertThat(response.getBody())
                .contains("Insufficient wallet balance");

        assertBalance(walletId, "1000.00");
    }

    @Test
    void shouldReturnNotFoundForUnknownWallet() {
        UUID walletId = UUID.randomUUID();

        DepositMoneyRequest request =
                new DepositMoneyRequest(
                        new BigDecimal("100.00"),
                        Currency.INR
                );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/wallets/" + walletId + "/deposits",
                request,
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertThat(response.getBody())
                .contains("wallet not found: " + walletId);
    }

    private UUID createWallet(String initialBalance) {
        UUID accountId = createAccount();
        UUID walletId = UUID.randomUUID();

        walletRepository.save(new WalletEntity(
                walletId,
                accountId,
                Currency.INR,
                new BigDecimal(initialBalance)
        ));

        return walletId;
    }

    private UUID createAccount() {
        AccountController.RegisterAccountRequest request =
                new AccountController.RegisterAccountRequest(
                        "wallet-api-" + UUID.randomUUID() + "@example.com",
                        "Pranay",
                        "Ingole",
                        "StrongPassword123"
                );

        ResponseEntity<AccountResponse> response =
                restTemplate.postForEntity(
                        "/api/v1/accounts",
                        request,
                        AccountResponse.class
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().accountId());

        return response.getBody().accountId();
    }

    private void assertBalance(UUID walletId, String expected) {
        WalletEntity wallet = walletRepository.findById(walletId).orElseThrow();

        assertThat(wallet.getBalance())
                .isEqualByComparingTo(expected);
    }
}
