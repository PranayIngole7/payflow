package com.payflow.transaction.api;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.application.CreatePaymentCommand;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        String reference,
        UUID accountId,
        UUID sourceWalletId,
        UUID destinationWalletId,
        BigDecimal amount,
        Currency currency,
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
