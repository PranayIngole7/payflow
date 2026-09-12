package com.payflow.wallet.infrastructure.persistence;

import com.payflow.account.api.AccountController;
import com.payflow.account.api.AccountResponse;
import com.payflow.shared.domain.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.config.name=application",
                "spring.profiles.active=postgres"
        }
)
@AutoConfigureTestRestTemplate 
class WalletOptimisticLockPostgresIntegrationTest {

	@Autowired
	private SpringDataWalletRepository walletRepository;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private UUID walletId;

	@BeforeEach
	void setUp() {

		AccountController.RegisterAccountRequest request = new AccountController.RegisterAccountRequest(
				"wallet-lock-" + UUID.randomUUID() + "@example.com", "Test", "Wallet", "StrongPassword123");

		ResponseEntity<AccountResponse> response =
		        restTemplate.postForEntity(
		                "/api/v1/accounts",
		                request,
		                AccountResponse.class
		        );

		assertEquals(
		        HttpStatus.CREATED,
		        response.getStatusCode()
		);

		assert response.getBody() != null;

		UUID accountId = response.getBody().accountId();

		walletId = UUID.randomUUID();

		walletRepository.saveAndFlush(new WalletEntity(walletId, accountId, Currency.INR, new BigDecimal("1000.00")));
	}

	@Test
	void shouldRejectStaleWalletUpdate() {

		WalletEntity firstRead = new TransactionTemplate(transactionManager)
				.execute(status -> walletRepository.findById(walletId).orElseThrow());

		WalletEntity secondRead = new TransactionTemplate(transactionManager)
				.execute(status -> walletRepository.findById(walletId).orElseThrow());

		assertEquals(firstRead.getVersion(), secondRead.getVersion());

		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {

			WalletEntity current = walletRepository.findById(walletId).orElseThrow();

			current.updateBalance(new BigDecimal("900.00"));
		});

		secondRead.updateBalance(new BigDecimal("800.00"));

		assertThrows(OptimisticLockingFailureException.class, () -> new TransactionTemplate(transactionManager)
				.executeWithoutResult(status -> walletRepository.saveAndFlush(secondRead)));
	}

}
