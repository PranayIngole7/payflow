package com.payflow.wallet.infrastructure;

import com.payflow.account.application.AccountRepository;
import com.payflow.ledger.application.LedgerRepository;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.transaction.application.TransactionRepository;
import com.payflow.wallet.application.CreateWalletUseCase;
import com.payflow.wallet.application.DepositMoneyUseCase;
import com.payflow.wallet.application.GetWalletUseCase;
import com.payflow.wallet.application.WalletRepository;
import com.payflow.wallet.application.WithdrawMoneyUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WalletApplicationConfiguration {

    @Bean
    public CreateWalletUseCase createWalletUseCase(
            WalletRepository walletRepository,
            AccountRepository accountRepository,
            TransactionRunner transactionRunner
    ) {
        return new CreateWalletUseCase(
                walletRepository,
                accountRepository,
                transactionRunner
        );
    }

    @Bean
    public GetWalletUseCase getWalletUseCase(
            WalletRepository walletRepository
    ) {
        return new GetWalletUseCase(walletRepository);
    }

    @Bean
    public DepositMoneyUseCase depositMoneyUseCase(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            LedgerRepository ledgerRepository,
            TransactionRunner transactionRunner
    ) {
        return new DepositMoneyUseCase(
                walletRepository,
                transactionRepository,
                ledgerRepository,
                transactionRunner
        );
    }

    @Bean
    public WithdrawMoneyUseCase withdrawMoneyUseCase(
            WalletRepository walletRepository,
            TransactionRunner transactionRunner
    ) {
        return new WithdrawMoneyUseCase(
                walletRepository,
                transactionRunner
        );
    }
}