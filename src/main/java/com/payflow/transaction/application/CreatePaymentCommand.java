package com.payflow.transaction.application;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Money;
import com.payflow.transaction.domain.TransactionType;
import com.payflow.wallet.domain.WalletId;

public record CreatePaymentCommand(
        String reference,
        AccountId accountId,
        WalletId sourceWalletId,
        WalletId destinationWalletId,
        Money amount,
        TransactionType type
) {
}
