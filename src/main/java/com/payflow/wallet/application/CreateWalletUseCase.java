package com.payflow.wallet.application;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;

import java.util.Objects;

public final class CreateWalletUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRunner transactionRunner;

    public CreateWalletUseCase(
            WalletRepository walletRepository,
            TransactionRunner transactionRunner
    ) {
        this.walletRepository = Objects.requireNonNull(
                walletRepository,
                "wallet repository must not be null"
        );
        this.transactionRunner = Objects.requireNonNull(
                transactionRunner,
                "transaction runner must not be null"
        );
    }

    public Wallet execute(
            AccountId accountId,
            Money initialBalance
    ) {
        Objects.requireNonNull(
                accountId,
                "account id must not be null"
        );

        Objects.requireNonNull(
                initialBalance,
                "initial balance must not be null"
        );

        WalletId walletId = WalletId.generate();

        transactionRunner.execute(() -> {
            Wallet wallet = Wallet.create(
                    walletId,
                    accountId,
                    initialBalance.currency()
            );

            if (initialBalance.amount().signum() > 0) {
                wallet.credit(initialBalance);
            }

            walletRepository.save(wallet);
        });

        return walletRepository.findById(walletId)
                .orElseThrow(() ->
                        new IllegalStateException("wallet was not persisted")
                );
    }
}