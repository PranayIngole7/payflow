package com.payflow.transaction.api;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.api.GlobalExceptionHandler;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({
        PaymentController.class,
        GlobalExceptionHandler.class
})
class PaymentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatePaymentUseCase createPaymentUseCase;

    @MockitoBean
    private GetPaymentHistoryUseCase getPaymentHistoryUseCase;

    @Test
    void shouldReturnPaymentHistory() throws Exception {
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

        when(getPaymentHistoryUseCase.execute(accountId))
                .thenReturn(List.of(payment));

        mockMvc.perform(
                        get("/api/v1/payments")
                                .param(
                                        "accountId",
                                        accountId.value().toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(
                        jsonPath("$[0].id")
                                .value(
                                        payment.id()
                                                .value()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$[0].reference")
                                .value("PAY-001")
                )
                .andExpect(
                        jsonPath("$[0].accountId")
                                .value(
                                        accountId.value()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$[0].amount")
                                .value(100.00)
                )
                .andExpect(
                        jsonPath("$[0].currency")
                                .value("INR")
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value("PENDING")
                );
    }

    @Test
    void shouldReturnEmptyHistory() throws Exception {
        UUID accountId = UUID.randomUUID();

        when(
                getPaymentHistoryUseCase.execute(
                        new AccountId(accountId)
                )
        ).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/payments")
                                .param(
                                        "accountId",
                                        accountId.toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnSamePaymentForDuplicateRequest()
            throws Exception {

        UUID accountId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destinationWalletId = UUID.randomUUID();

        String idempotencyKey =
                "duplicate-request-001";

        Payment payment = Payment.create(
                PaymentId.generate(),
                "PAY-002",
                new AccountId(accountId),
                new WalletId(sourceWalletId),
                new WalletId(destinationWalletId),
                new Money(
                        new BigDecimal("250.00"),
                        Currency.INR
                ),
                Instant.parse("2026-09-14T11:00:00Z"),
                TransactionType.TRANSFER
        );

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        "PAY-002",
                        accountId,
                        sourceWalletId,
                        destinationWalletId,
                        new BigDecimal("250.00"),
                        Currency.INR,
                        TransactionType.TRANSFER
                );

        when(
                createPaymentUseCase.execute(
                        request.toCommand(),
                        idempotencyKey
                )
        ).thenReturn(payment);

        String requestBody = """
                {
                  "reference": "PAY-002",
                  "accountId": "%s",
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "amount": 250.00,
                  "currency": "INR",
                  "type": "TRANSFER"
                }
                """.formatted(
                accountId,
                sourceWalletId,
                destinationWalletId
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                                .contentType(APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        payment.id()
                                                .value()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.reference")
                                .value("PAY-002")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING")
                );

        verify(createPaymentUseCase)
                .execute(
                        request.toCommand(),
                        idempotencyKey
                );

        verifyNoMoreInteractions(createPaymentUseCase);
    }

    @Test
    void shouldReturnConflictWhenSameKeyIsUsedForDifferentRequest()
            throws Exception {

        UUID accountId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destinationWalletId = UUID.randomUUID();

        String idempotencyKey =
                "duplicate-request-002";

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        "PAY-003",
                        accountId,
                        sourceWalletId,
                        destinationWalletId,
                        new BigDecimal("100.00"),
                        Currency.INR,
                        TransactionType.TRANSFER
                );

        when(
                createPaymentUseCase.execute(
                        request.toCommand(),
                        idempotencyKey
                )
        ).thenThrow(
                new IllegalStateException(
                        "idempotency key has already been used for a different payment"
                )
        );

        String requestBody = """
                {
                  "reference": "PAY-003",
                  "accountId": "%s",
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "amount": 100.00,
                  "currency": "INR",
                  "type": "TRANSFER"
                }
                """.formatted(
                accountId,
                sourceWalletId,
                destinationWalletId
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                                .contentType(APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Conflict")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "idempotency key has already been used for a different payment"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/v1/payments")
                );

        verify(createPaymentUseCase)
                .execute(
                        request.toCommand(),
                        idempotencyKey
                );

        verifyNoMoreInteractions(createPaymentUseCase);
    }
    
    @Test
    void shouldRejectBlankReference() throws Exception {
        String accountId = UUID.randomUUID().toString();

        String body = """
                {
                  "reference": "",
                  "accountId": "%s",
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "amount": 100.00,
                  "currency": "INR",
                  "type": "TRANSFER"
                }
                """.formatted(
                accountId,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header("Idempotency-Key", "validation-001")
                                .contentType(APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingAmount() throws Exception {
        String accountId = UUID.randomUUID().toString();

        String body = """
                {
                  "reference": "PAY-VALIDATION-002",
                  "accountId": "%s",
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "currency": "INR",
                  "type": "TRANSFER"
                }
                """.formatted(
                accountId,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header("Idempotency-Key", "validation-002")
                                .contentType(APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectZeroAmount() throws Exception {
        String accountId = UUID.randomUUID().toString();

        String body = """
                {
                  "reference": "PAY-VALIDATION-003",
                  "accountId": "%s",
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "amount": 0,
                  "currency": "INR",
                  "type": "TRANSFER"
                }
                """.formatted(
                accountId,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header("Idempotency-Key", "validation-003")
                                .contentType(APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingCurrency() throws Exception {
        String accountId = UUID.randomUUID().toString();

        String body = """
                {
                  "reference": "PAY-VALIDATION-004",
                  "accountId": "%s",
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "amount": 100.00,
                  "type": "TRANSFER"
                }
                """.formatted(
                accountId,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header("Idempotency-Key", "validation-004")
                                .contentType(APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }
    
}