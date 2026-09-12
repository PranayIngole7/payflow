package com.payflow.wallet.application;

import com.payflow.shared.application.TransactionRunner;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.domain.Wallet;
import com.payflow.wallet.domain.WalletId;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class WithdrawMoneyUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRunner transactionRunner;

    public WithdrawMoneyUseCase(
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
            WalletId walletId,
            BigDecimal amount,
            Currency currency
    ) {
        Objects.requireNonNull(
                walletId,
                "wallet id must not be null"
        );

        Objects.requireNonNull(
                amount,
                "withdrawal amount must not be null"
        );

        Objects.requireNonNull(
                currency,
                "withdrawal currency must not be null"
        );

        transactionRunner.execute(() -> {

            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "wallet not found: " + walletId.value()
                    ));

            wallet.debit(
                    new Money(amount, currency)
            );

            walletRepository.save(wallet);
        });

        return walletRepository.findById(walletId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "wallet was not persisted"
                        )
                );
    }
}
