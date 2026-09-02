package com.payflow.transaction.api;

import com.payflow.shared.domain.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HTTP request for initiating a wallet-to-wallet transfer.
 */
public record InitiateTransferRequest(

        @NotNull(message = "sourceWalletId is required")
        UUID sourceWalletId,

        @NotNull(message = "destinationWalletId is required")
        UUID destinationWalletId,

        @NotNull(message = "amount is required")
        @DecimalMin(
                value = "0.01",
                message = "amount must be greater than zero"
        )
        BigDecimal amount,

        @NotNull(message = "currency is required")
        Currency currency
) {
}