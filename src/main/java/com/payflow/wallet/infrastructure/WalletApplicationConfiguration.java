package com.payflow.wallet.infrastructure;

import com.payflow.account.application.AccountRepository;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.wallet.application.CreateWalletUseCase;
import com.payflow.wallet.application.GetWalletUseCase;
import com.payflow.wallet.application.WalletRepository;
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
}