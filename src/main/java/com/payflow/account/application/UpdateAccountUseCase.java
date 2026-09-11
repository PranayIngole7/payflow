package com.payflow.account.application;

import com.payflow.account.domain.Account;
import com.payflow.shared.application.TransactionRunner;

import java.util.NoSuchElementException;
import java.util.Objects;

public final class UpdateAccountUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRunner transactionRunner;

    public UpdateAccountUseCase(
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

    public Account execute(UpdateAccountCommand command) {
        Objects.requireNonNull(
                command,
                "update account command must not be null"
        );

        Account account = accountRepository.findById(
                command.accountId()
        ).orElseThrow(() -> new NoSuchElementException(
                "account not found: " + command.accountId().value()
        ));

        account.updateProfile(
                command.firstName(),
                command.lastName()
        );

        transactionRunner.execute(
                () -> accountRepository.update(account)
        );

        return account;
    }
}
