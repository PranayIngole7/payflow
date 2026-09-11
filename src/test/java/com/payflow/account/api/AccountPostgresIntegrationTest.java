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

    @Test
    void shouldSuspendAccount() {
        String email =
                "suspend-" + UUID.randomUUID() + "@example.com";

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

        assertEquals(
                HttpStatus.CREATED,
                createResponse.getStatusCode()
        );
        assertNotNull(createResponse.getBody());

        UUID accountId =
                createResponse.getBody().accountId();

        ResponseEntity<Void> suspendResponse =
                restTemplate.exchange(
                        "/api/v1/accounts/" + accountId + "/suspend",
                        HttpMethod.PATCH,
                        HttpEntity.EMPTY,
                        Void.class
                );

        assertEquals(
                HttpStatus.NO_CONTENT,
                suspendResponse.getStatusCode()
        );

        ResponseEntity<AccountResponse> getResponse =
                restTemplate.getForEntity(
                        "/api/v1/accounts/" + accountId,
                        AccountResponse.class
                );

        assertEquals(
                HttpStatus.OK,
                getResponse.getStatusCode()
        );
        assertNotNull(getResponse.getBody());

        assertEquals(
                "SUSPENDED",
                getResponse.getBody().status()
        );
    }

    @Test
    void shouldRejectSuspendingAlreadySuspendedAccount() {
        String email =
                "already-suspended-" + UUID.randomUUID() + "@example.com";

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

        assertEquals(
                HttpStatus.CREATED,
                createResponse.getStatusCode()
        );
        assertNotNull(createResponse.getBody());

        UUID accountId =
                createResponse.getBody().accountId();

        ResponseEntity<Void> firstSuspendResponse =
                restTemplate.exchange(
                        "/api/v1/accounts/" + accountId + "/suspend",
                        HttpMethod.PATCH,
                        HttpEntity.EMPTY,
                        Void.class
                );

        assertEquals(
                HttpStatus.NO_CONTENT,
                firstSuspendResponse.getStatusCode()
        );

        ResponseEntity<String> secondSuspendResponse =
                restTemplate.exchange(
                        "/api/v1/accounts/" + accountId + "/suspend",
                        HttpMethod.PATCH,
                        HttpEntity.EMPTY,
                        String.class
                );

        assertEquals(
                HttpStatus.CONFLICT,
                secondSuspendResponse.getStatusCode()
        );

        assertNotNull(secondSuspendResponse.getBody());

        assertTrue(
                secondSuspendResponse.getBody().contains(
                        "account is already suspended"
                )
        );
    }

    @Test
    void shouldReturnNotFoundWhenSuspendingUnknownAccount() {
        UUID accountId = UUID.randomUUID();

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/accounts/" + accountId + "/suspend",
                        HttpMethod.PATCH,
                        HttpEntity.EMPTY,
                        String.class
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertTrue(
                response.getBody().contains(
                        "account not found: " + accountId
                )
        );
    }

    @Test
    void shouldRejectInvalidAccountIdWhenSuspending() {
        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/accounts/not-a-uuid/suspend",
                        HttpMethod.PATCH,
                        HttpEntity.EMPTY,
                        String.class
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
    }


    @Test
    void shouldUpdateAccountProfile() {
        String email =
                "update-" + UUID.randomUUID() + "@example.com";

        AccountController.RegisterAccountRequest createRequest =
                new AccountController.RegisterAccountRequest(
                        email,
                        "Pranay",
                        "Ingole",
                        "StrongPassword123"
                );

        ResponseEntity<AccountResponse> createResponse =
                restTemplate.postForEntity(
                        "/api/v1/accounts",
                        createRequest,
                        AccountResponse.class
                );

        assertEquals(
                HttpStatus.CREATED,
                createResponse.getStatusCode()
        );
        assertNotNull(createResponse.getBody());

        UUID accountId =
                createResponse.getBody().accountId();

        AccountController.UpdateAccountRequest updateRequest =
                new AccountController.UpdateAccountRequest(
                        "Alicia",
                        "Johnson"
                );

        ResponseEntity<AccountResponse> updateResponse =
                restTemplate.exchange(
                        "/api/v1/accounts/" + accountId,
                        HttpMethod.PATCH,
                        new HttpEntity<>(updateRequest),
                        AccountResponse.class
                );

        assertEquals(
                HttpStatus.OK,
                updateResponse.getStatusCode()
        );
        assertNotNull(updateResponse.getBody());

        assertEquals(
                accountId,
                updateResponse.getBody().accountId()
        );
        assertEquals(
                "ACTIVE",
                updateResponse.getBody().status()
        );

        ResponseEntity<AccountResponse> getResponse =
                restTemplate.getForEntity(
                        "/api/v1/accounts/" + accountId,
                        AccountResponse.class
                );

        assertEquals(
                HttpStatus.OK,
                getResponse.getStatusCode()
        );
        assertNotNull(getResponse.getBody());

        assertEquals(
                accountId,
                getResponse.getBody().accountId()
        );
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingUnknownAccount() {
        UUID accountId = UUID.randomUUID();

        AccountController.UpdateAccountRequest request =
                new AccountController.UpdateAccountRequest(
                        "Alicia",
                        "Johnson"
                );

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/accounts/" + accountId,
                        HttpMethod.PATCH,
                        new HttpEntity<>(request),
                        String.class
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertTrue(
                response.getBody().contains(
                        "account not found: " + accountId
                )
        );
    }

    @Test
    void shouldRejectInvalidAccountIdWhenUpdating() {
        AccountController.UpdateAccountRequest request =
                new AccountController.UpdateAccountRequest(
                        "Alicia",
                        "Johnson"
                );

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/accounts/not-a-uuid",
                        HttpMethod.PATCH,
                        new HttpEntity<>(request),
                        String.class
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
    }

    @Test
    void shouldRejectInvalidProfileUpdateRequest() {
        AccountController.UpdateAccountRequest request =
                new AccountController.UpdateAccountRequest(
                        "",
                        ""
                );

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/accounts/" + UUID.randomUUID(),
                        HttpMethod.PATCH,
                        new HttpEntity<>(request),
                        String.class
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
    }

}
