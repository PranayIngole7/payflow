package com.payflow.transaction.infrastructure;

import com.payflow.shared.application.TransactionRunner;
import com.payflow.transaction.application.InitiateTransferUseCase;
import com.payflow.transaction.application.TransactionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for transaction application services.
 */
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
}