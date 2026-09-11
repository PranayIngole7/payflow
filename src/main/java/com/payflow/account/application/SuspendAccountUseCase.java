package com.payflow.account.application;

import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import com.payflow.shared.application.TransactionRunner;

import java.util.Objects;

public final class SuspendAccountUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRunner transactionRunner;

    public SuspendAccountUseCase(
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

    public void execute(AccountId accountId) {
        Objects.requireNonNull(
                accountId,
                "account id must not be null"
        );

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "account not found: " + accountId.value()
                ));

        account.suspend();

        transactionRunner.execute(
                () -> accountRepository.update(account)
        );
    }
}
