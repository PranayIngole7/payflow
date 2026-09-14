package com.payflow.transaction.api;

import com.payflow.account.domain.AccountId;
import com.payflow.transaction.application.CreatePaymentCommand;
import com.payflow.transaction.application.CreatePaymentUseCase;
import com.payflow.transaction.application.GetPaymentHistoryUseCase;
import com.payflow.transaction.domain.Payment;
import com.payflow.transaction.domain.PaymentId;
import com.payflow.transaction.domain.PaymentStatus;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.WalletId;
import com.payflow.shared.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void shouldCreatePayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destinationWalletId = UUID.randomUUID();

        Instant createdAt = Instant.parse(
                "2026-09-14T10:00:00Z"
        );

        Payment payment = Payment.create(
                new PaymentId(paymentId),
                "PAY-API-001",
                new AccountId(accountId),
                new WalletId(sourceWalletId),
                new WalletId(destinationWalletId),
                new Money(
                        new BigDecimal("100.00"),
                        Currency.INR
                ),
                createdAt,
                TransactionType.TRANSFER
        );

        when(createPaymentUseCase.execute(
                any(CreatePaymentCommand.class),
                eq("api-key-001")
        )).thenReturn(payment);

        String body = """
                {
                  "reference": "PAY-API-001",
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
                                        "api-key-001"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(paymentId.toString())
                )
                .andExpect(
                        jsonPath("$.reference")
                                .value("PAY-API-001")
                )
                .andExpect(
                        jsonPath("$.accountId")
                                .value(accountId.toString())
                )
                .andExpect(
                        jsonPath("$.sourceWalletId")
                                .value(sourceWalletId.toString())
                )
                .andExpect(
                        jsonPath("$.destinationWalletId")
                                .value(destinationWalletId.toString())
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("TRANSFER")
                )
                .andExpect(
                        jsonPath("$.amount")
                                .value(100.00)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("INR")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING")
                );
    }

    @Test
    void shouldGetPaymentHistory() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destinationWalletId = UUID.randomUUID();

        Instant createdAt = Instant.parse(
                "2026-09-14T10:00:00Z"
        );

        Payment payment = Payment.create(
                new PaymentId(paymentId),
                "PAY-API-HISTORY-001",
                new AccountId(accountId),
                new WalletId(sourceWalletId),
                new WalletId(destinationWalletId),
                new Money(
                        new BigDecimal("250.00"),
                        Currency.INR
                ),
                createdAt,
                TransactionType.TRANSFER
        );

        when(getPaymentHistoryUseCase.execute(
                new AccountId(accountId)
        )).thenReturn(List.of(payment));

        mockMvc.perform(
                        get("/api/v1/payments")
                                .param(
                                        "accountId",
                                        accountId.toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(
                        jsonPath("$[0].id")
                                .value(paymentId.toString())
                )
                .andExpect(
                        jsonPath("$[0].reference")
                                .value("PAY-API-HISTORY-001")
                )
                .andExpect(
                        jsonPath("$[0].accountId")
                                .value(accountId.toString())
                )
                .andExpect(
                        jsonPath("$[0].amount")
                                .value(250.00)
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
    void shouldReturnEmptyPaymentHistory() throws Exception {
        UUID accountId = UUID.randomUUID();

        when(getPaymentHistoryUseCase.execute(
                new AccountId(accountId)
        )).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/payments")
                                .param(
                                        "accountId",
                                        accountId.toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldRejectMissingIdempotencyKey() throws Exception {
        UUID accountId = UUID.randomUUID();

        String body = """
                {
                  "reference": "PAY-API-004",
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBlankReference() throws Exception {
        String body = """
                {
                  "reference": "   ",
                  "accountId": "%s",
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "amount": 100.00,
                  "currency": "INR",
                  "type": "TRANSFER"
                }
                """.formatted(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header(
                                        "Idempotency-Key",
                                        "api-validation-001"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("request validation failed")
                );
    }

    @Test
    void shouldRejectMissingAmount() throws Exception {
        String body = """
                {
                  "reference": "PAY-API-VALIDATION-002",
                  "accountId": "%s",
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "currency": "INR",
                  "type": "TRANSFER"
                }
                """.formatted(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header(
                                        "Idempotency-Key",
                                        "api-validation-002"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("request validation failed")
                );
    }

    @Test
    void shouldRejectZeroAmount() throws Exception {
        String body = """
                {
                  "reference": "PAY-API-VALIDATION-003",
                  "accountId": "%s",
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "amount": 0,
                  "currency": "INR",
                  "type": "TRANSFER"
                }
                """.formatted(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header(
                                        "Idempotency-Key",
                                        "api-validation-003"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("request validation failed")
                );
    }

    @Test
    void shouldRejectMissingCurrency() throws Exception {
        String body = """
                {
                  "reference": "PAY-API-VALIDATION-004",
                  "accountId": "%s",
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "amount": 100.00,
                  "type": "TRANSFER"
                }
                """.formatted(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header(
                                        "Idempotency-Key",
                                        "api-validation-004"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("request validation failed")
                );
    }

    @Test
    void shouldRejectTransferWithoutSourceWallet() throws Exception {
        String accountId = UUID.randomUUID().toString();

        when(createPaymentUseCase.execute(
                any(CreatePaymentCommand.class),
                eq("api-semantic-001")
        )).thenThrow(
                new NullPointerException(
                        "source wallet id must not be null"
                )
        );

        String body = """
                {
                  "reference": "PAY-API-SEM-001",
                  "accountId": "%s",
                  "destinationWalletId": "%s",
                  "amount": 100.00,
                  "currency": "INR",
                  "type": "TRANSFER"
                }
                """.formatted(
                accountId,
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header(
                                        "Idempotency-Key",
                                        "api-semantic-001"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransferWithSameWallets() throws Exception {
        String accountId = UUID.randomUUID().toString();
        String walletId = UUID.randomUUID().toString();

        when(createPaymentUseCase.execute(
                any(CreatePaymentCommand.class),
                eq("api-semantic-002")
        )).thenThrow(
                new IllegalArgumentException(
                        "transfer source and destination wallets must be different"
                )
        );

        String body = """
                {
                  "reference": "PAY-API-SEM-002",
                  "accountId": "%s",
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "amount": 100.00,
                  "currency": "INR",
                  "type": "TRANSFER"
                }
                """.formatted(
                accountId,
                walletId,
                walletId
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header(
                                        "Idempotency-Key",
                                        "api-semantic-002"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDepositWithSourceWallet() throws Exception {
        String accountId = UUID.randomUUID().toString();

        when(createPaymentUseCase.execute(
                any(CreatePaymentCommand.class),
                eq("api-semantic-003")
        )).thenThrow(
                new IllegalArgumentException(
                        "deposit must not have a source wallet"
                )
        );

        String body = """
                {
                  "reference": "PAY-API-SEM-003",
                  "accountId": "%s",
                  "sourceWalletId": "%s",
                  "destinationWalletId": "%s",
                  "amount": 100.00,
                  "currency": "INR",
                  "type": "DEPOSIT"
                }
                """.formatted(
                accountId,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/payments")
                                .header(
                                        "Idempotency-Key",
                                        "api-semantic-003"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }
}
