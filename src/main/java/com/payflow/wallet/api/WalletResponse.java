package com.payflow.wallet.api;

import com.payflow.wallet.domain.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
        UUID walletId,
        UUID accountId,
        BigDecimal balance,
        String currency
) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.id().value(),
                wallet.accountId().value(),
                wallet.balance().amount(),
                wallet.currency().name()
        );
    }
}