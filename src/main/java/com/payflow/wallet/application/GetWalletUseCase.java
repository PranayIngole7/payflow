package com.payflow.wallet.application;

import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;

import java.util.Objects;

public final class GetWalletUseCase {

    private final WalletRepository walletRepository;

    public GetWalletUseCase(WalletRepository walletRepository) {
        this.walletRepository = Objects.requireNonNull(
                walletRepository,
                "wallet repository must not be null"
        );
    }

    public Wallet execute(WalletId walletId) {
        Objects.requireNonNull(
                walletId,
                "wallet id must not be null"
        );

        return walletRepository.findById(walletId)
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "wallet not found: " + walletId.value()
                ));
    }
}