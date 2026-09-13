package com.payflow.wallet.infrastructure.persistence;

import com.payflow.account.api.AccountController;
import com.payflow.account.api.AccountResponse;
import com.payflow.shared.domain.Currency;
import com.payflow.wallet.application.DepositMoneyUseCase;
import com.payflow.wallet.application.WithdrawMoneyUseCase;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
class WalletBalanceConsistencyPostgresIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SpringDataWalletRepository walletRepository;

    @Autowired
    private DepositMoneyUseCase depositMoneyUseCase;

    @Autowired
    private WithdrawMoneyUseCase withdrawMoneyUseCase;

    @Test
    void depositAndWithdrawalShouldMaintainCorrectBalance() {
        UUID accountId = createAccount();

        UUID walletId = UUID.randomUUID();

        walletRepository.save(new WalletEntity(
                walletId,
                accountId,
                Currency.INR,
                new BigDecimal("1000.00")
        ));

        depositMoneyUseCase.execute(
                new WalletId(walletId),
                new BigDecimal("500.00"),
                Currency.INR
        );

        assertBalance(walletId, "1500.00");

        withdrawMoneyUseCase.execute(
                new WalletId(walletId),
                new BigDecimal("400.00"),
                Currency.INR
        );

        assertBalance(walletId, "1100.00");
    }

    private UUID createAccount() {
        AccountController.RegisterAccountRequest request =
                new AccountController.RegisterAccountRequest(
                        "wallet-balance-" + UUID.randomUUID() + "@example.com",
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

    private void assertBalance(UUID walletId, String expectedBalance) {
        WalletEntity wallet = walletRepository.findById(walletId).orElseThrow();

        assertThat(wallet.getBalance())
                .isEqualByComparingTo(expectedBalance);
    }
    
    @Test
    void failedWithdrawalShouldLeaveBalanceUnchanged() {
        UUID accountId = createAccount();
        UUID walletId = UUID.randomUUID();

        walletRepository.save(new WalletEntity(
                walletId,
                accountId,
                Currency.INR,
                new BigDecimal("1000.00")
        ));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                withdrawMoneyUseCase.execute(
                        new WalletId(walletId),
                        new BigDecimal("1500.00"),
                        Currency.INR
                )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Insufficient wallet balance");

        assertBalance(walletId, "1000.00");
    }
    
}
