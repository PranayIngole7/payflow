package com.payflow.account.application;

import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;

import java.util.NoSuchElementException;
import java.util.Objects;

public final class GetAccountUseCase {

    private final AccountRepository accountRepository;

    public GetAccountUseCase(AccountRepository accountRepository) {
        this.accountRepository = Objects.requireNonNull(
                accountRepository,
                "account repository must not be null"
        );
    }

    public Account execute(AccountId accountId) {
        Objects.requireNonNull(
                accountId,
                "account id must not be null"
        );

        return accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException(
                        "account not found: " + accountId.value()
                ));
    }
}