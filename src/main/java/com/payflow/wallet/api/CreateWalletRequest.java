package com.payflow.wallet.api;

import com.payflow.shared.domain.Currency;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateWalletRequest(
        @NotNull(message = "accountId is required")
        UUID accountId,

        @NotNull(message = "currency is required")
        Currency currency
) {}