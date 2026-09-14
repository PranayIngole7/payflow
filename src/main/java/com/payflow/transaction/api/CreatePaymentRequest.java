package com.payflow.transaction.api;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.CreatePaymentCommand;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(

        @NotBlank(message = "reference must not be blank")
        @Size(
                max = 100,
                message = "reference must not exceed 100 characters"
        )
        String reference,

        @NotNull(message = "accountId must not be null")
        UUID accountId,

        UUID sourceWalletId,

        UUID destinationWalletId,

        @NotNull(message = "amount must not be null")
        @DecimalMin(
                value = "0.01",
                message = "amount must be greater than zero"
        )
        BigDecimal amount,

        @NotNull(message = "currency must not be null")
        Currency currency,

        @NotNull(message = "type must not be null")
        TransactionType type
) {

    public CreatePaymentCommand toCommand() {
        return new CreatePaymentCommand(
                reference,
                new AccountId(accountId),
                sourceWalletId == null
                        ? null
                        : new WalletId(sourceWalletId),
                destinationWalletId == null
                        ? null
                        : new WalletId(destinationWalletId),
                new Money(amount, currency),
                type
        );
    }
}