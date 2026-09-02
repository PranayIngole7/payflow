package com.payflow.account.application;

import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import com.payflow.shared.application.TransactionRunner;

import java.time.Instant;
import java.util.Objects;

public final class CreateAccountUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRunner transactionRunner;

    public CreateAccountUseCase(
            AccountRepository accountRepository,
            TransactionRunner transactionRunner
    ) {
        this.accountRepository = Objects.requireNonNull(
                accountRepository,
                "account repository must not be null"
        );

        this.transactionRunner = Objects.requireNonNull(
                transactionRunner,
                "transaction runner must not be null"
        );
    }

    public Account execute() {
        AccountId accountId = AccountId.generate();

        Account account = Account.create(
                accountId,
                Instant.now()
        );

        transactionRunner.execute(() ->
                accountRepository.save(account)
        );

        return account;
    }
}