package com.payflow.transaction.infrastructure;

import com.payflow.ledger.application.LedgerRepository;
import com.payflow.shared.application.TransactionRunner;
import com.payflow.transaction.application.ExecuteTransferUseCase;
import com.payflow.transaction.application.GetTransactionUseCase;
import com.payflow.transaction.application.InitiateTransferUseCase;
import com.payflow.transaction.application.TransactionRepository;
import com.payflow.transaction.application.TransferMoneyUseCase;
import com.payflow.wallet.application.WalletRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionApplicationConfiguration {

    @Bean
    public InitiateTransferUseCase initiateTransferUseCase(
            TransactionRepository transactionRepository,
            TransactionRunner transactionRunner
    ) {
        return new InitiateTransferUseCase(
                transactionRepository,
                transactionRunner
        );
    }

    @Bean
    public TransferMoneyUseCase transferMoneyUseCase(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            LedgerRepository ledgerRepository,
            TransactionRunner transactionRunner
    ) {
        return new TransferMoneyUseCase(
                walletRepository,
                transactionRepository,
                ledgerRepository,
                transactionRunner
        );
    }

    @Bean
    public ExecuteTransferUseCase executeTransferUseCase(
            TransferMoneyUseCase transferMoneyUseCase
    ) {
        return new ExecuteTransferUseCase(transferMoneyUseCase);
    }

    @Bean
    public GetTransactionUseCase getTransactionUseCase(
            TransactionRepository transactionRepository
    ) {
        return new GetTransactionUseCase(transactionRepository);
    }
}