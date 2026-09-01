package com.payflow.wallet.application;

import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;

import java.util.Optional;

/**
 * Persistence port for wallet aggregates.
 */
public interface WalletRepository {

    Optional<Wallet> findById(WalletId walletId);

    void save(Wallet wallet);
}