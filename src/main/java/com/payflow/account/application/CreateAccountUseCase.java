package com.payflow.account.application;

import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import com.payflow.shared.application.TransactionRunner;

import java.time.Instant;
import java.util.Objects;

public final class CreateAccountUseCase {

    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final TransactionRunner transactionRunner;

    public CreateAccountUseCase(
            AccountRepository accountRepository,
            PasswordHasher passwordHasher,
            TransactionRunner transactionRunner
    ) {
        this.accountRepository = Objects.requireNonNull(
                accountRepository,
                "account repository must not be null"
        );

        this.passwordHasher = Objects.requireNonNull(
                passwordHasher,
                "password hasher must not be null"
        );

        this.transactionRunner = Objects.requireNonNull(
                transactionRunner,
                "transaction runner must not be null"
        );
    }

    public Account execute(RegisterAccountCommand command) {
        Objects.requireNonNull(
                command,
                "register account command must not be null"
        );

        AccountId accountId = AccountId.generate();
        Instant createdAt = Instant.now();

        String passwordHash =
                passwordHasher.hash(command.password());

        Account account = Account.create(
                accountId,
                command.email(),
                command.firstName(),
                command.lastName(),
                createdAt
        );

        transactionRunner.execute(() ->
                accountRepository.save(account, passwordHash)
        );

        return account;
    }
}