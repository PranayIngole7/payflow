package com.payflow.account.infrastructure;

import com.payflow.account.application.AccountRepository;
import com.payflow.account.application.CreateAccountUseCase;
import com.payflow.account.application.GetAccountUseCase;
import com.payflow.shared.application.TransactionRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountApplicationConfiguration {

    @Bean
    public CreateAccountUseCase createAccountUseCase(
            AccountRepository accountRepository,
            TransactionRunner transactionRunner
    ) {
        return new CreateAccountUseCase(
                accountRepository,
                transactionRunner
        );
    }

    @Bean
    public GetAccountUseCase getAccountUseCase(
            AccountRepository accountRepository
    ) {
        return new GetAccountUseCase(accountRepository);
    }
}