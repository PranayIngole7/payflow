package com.payflow.transaction.api;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.CreatePaymentUseCase;
import com.payflow.transaction.application.GetPaymentHistoryUseCase;
import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentId;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class PaymentApiTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private CreatePaymentUseCase createPaymentUseCase;

    @MockitoBean
    private GetPaymentHistoryUseCase getPaymentHistoryUseCase;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void shouldReturnPaymentHistory() {
        AccountId accountId =
                new AccountId(UUID.randomUUID());

        Payment payment = Payment.create(
                PaymentId.generate(),
                "PAY-001",
                accountId,
                new WalletId(UUID.randomUUID()),
                new WalletId(UUID.randomUUID()),
                new Money(
                        new BigDecimal("100.00"),
                        Currency.INR
                ),
                Instant.parse("2026-09-14T10:00:00Z"),
                TransactionType.TRANSFER
        );

        getPaymentHistoryUseCase.execute(accountId);

        // Configure the mock after obtaining the payment.
        org.mockito.Mockito.when(
                getPaymentHistoryUseCase.execute(accountId)
        ).thenReturn(List.of(payment));

        PaymentResponse[] response =
                restClient()
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .path("/api/v1/payments")
                                        .queryParam(
                                                "accountId",
                                                accountId.value()
                                        )
                                        .build()
                        )
                        .retrieve()
                        .body(PaymentResponse[].class);

        assertNotNull(response);
        assertEquals(1, response.length);

        assertEquals(
                payment.id().value(),
                response[0].id()
        );
        assertEquals("PAY-001", response[0].reference());
        assertEquals(
                accountId.value(),
                response[0].accountId()
        );
        assertEquals(
                new BigDecimal("100.00"),
                response[0].amount()
        );
        assertEquals("INR", response[0].currency());
        assertEquals("PENDING", response[0].status());
    }

    @Test
    void shouldReturnEmptyHistory() {
        UUID accountId = UUID.randomUUID();

        org.mockito.Mockito.when(
                getPaymentHistoryUseCase.execute(
                        new AccountId(accountId)
                )
        ).thenReturn(List.of());

        PaymentResponse[] response =
                restClient()
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .path("/api/v1/payments")
                                        .queryParam(
                                                "accountId",
                                                accountId
                                        )
                                        .build()
                        )
                        .retrieve()
                        .body(PaymentResponse[].class);

        assertNotNull(response);
        assertEquals(0, response.length);
    }
}