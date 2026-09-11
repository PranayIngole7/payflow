package com.payflow.wallet.application;

import com.payflow.account.domain.AccountId;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;

import java.util.Optional;

public interface WalletRepository {

    Optional<Wallet> findById(WalletId walletId);

    Optional<Wallet> findByAccountId(AccountId accountId);

    void save(Wallet wallet);
}