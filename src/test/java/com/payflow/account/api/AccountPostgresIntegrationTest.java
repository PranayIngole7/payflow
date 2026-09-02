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
        ResponseEntity<AccountResponse> response =
                restTemplate.postForEntity(
                        "/api/v1/accounts",
                        null,
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
        ResponseEntity<AccountResponse> createResponse =
                restTemplate.postForEntity(
                        "/api/v1/accounts",
                        null,
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
}