package com.payflow.wallet.api;

import com.payflow.shared.domain.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateWalletRequest(
        @NotNull(message = "accountId is required")
        UUID accountId,

        @NotNull(message = "currency is required")
        Currency currency,

        @NotNull(message = "initialBalance is required")
        @DecimalMin(value = "0.00", message = "initialBalance must not be negative")
        BigDecimal initialBalance
) {}