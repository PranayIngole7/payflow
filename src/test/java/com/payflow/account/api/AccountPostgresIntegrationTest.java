package com.payflow.account.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;

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
class AccountPostgresIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateAccount() {
        AccountController.RegisterAccountRequest request =
                new AccountController.RegisterAccountRequest(
                        "pranay-" + UUID.randomUUID() + "@example.com",
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

        AccountResponse account = response.getBody();

        assertNotNull(account.accountId());
        assertEquals("ACTIVE", account.status());
        assertNotNull(account.createdAt());
    }

    @Test
    void shouldCreateAndGetAccount() {
        String email =
                "pranay-" + UUID.randomUUID() + "@example.com";

        AccountController.RegisterAccountRequest request =
                new AccountController.RegisterAccountRequest(
                        email,
                        "Pranay",
                        "Ingole",
                        "StrongPassword123"
                );

        ResponseEntity<AccountResponse> createResponse =
                restTemplate.postForEntity(
                        "/api/v1/accounts",
                        request,
                        AccountResponse.class
                );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());

        UUID accountId =
                createResponse.getBody().accountId();

        ResponseEntity<AccountResponse> getResponse =
                restTemplate.getForEntity(
                        "/api/v1/accounts/" + accountId,
                        AccountResponse.class
                );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());

        AccountResponse account =
                getResponse.getBody();

        assertEquals(accountId, account.accountId());
        assertEquals("ACTIVE", account.status());
        assertNotNull(account.createdAt());
    }

    @Test
    void shouldReturnNotFoundForUnknownAccount() {
        UUID accountId = UUID.randomUUID();

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "/api/v1/accounts/" + accountId,
                        String.class
                );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        assertTrue(
                response.getBody().contains(
                        "account not found: " + accountId
                )
        );
    }

    @Test
    void shouldRejectInvalidRegistrationRequest() {
        AccountController.RegisterAccountRequest request =
                new AccountController.RegisterAccountRequest(
                        "invalid-email",
                        "",
                        "",
                        "short"
                );

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        "/api/v1/accounts",
                        request,
                        String.class
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );
    }
    
    @Test
    void shouldRejectDuplicateEmail() {
        String email =
                "duplicate-" + UUID.randomUUID() + "@example.com";

        AccountController.RegisterAccountRequest request =
                new AccountController.RegisterAccountRequest(
                        email,
                        "Pranay",
                        "Ingole",
                        "StrongPassword123"
                );

        ResponseEntity<AccountResponse> firstResponse =
                restTemplate.postForEntity(
                        "/api/v1/accounts",
                        request,
                        AccountResponse.class
                );

        assertEquals(
                HttpStatus.CREATED,
                firstResponse.getStatusCode()
        );

        ResponseEntity<String> secondResponse =
                restTemplate.postForEntity(
                        "/api/v1/accounts",
                        request,
                        String.class
                );

        assertEquals(
                HttpStatus.CONFLICT,
                secondResponse.getStatusCode()
        );

        assertNotNull(secondResponse.getBody());

        assertTrue(
                secondResponse.getBody().contains(
                        "account already exists for email: " + email
                )
        );
    }
    
    @Test
    void shouldRejectInvalidAccountId() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "/api/v1/accounts/not-a-uuid",
                        String.class
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
    }
    
}
