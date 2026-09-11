package com.payflow.wallet.api;

import com.payflow.shared.domain.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositMoneyRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(
                value = "0.01",
                message = "amount must be greater than zero"
        )
        BigDecimal amount,

        @NotNull(message = "currency is required")
        Currency currency
) {}
