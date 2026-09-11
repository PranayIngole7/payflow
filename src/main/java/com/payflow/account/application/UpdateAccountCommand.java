package com.payflow.account.application;

import com.payflow.account.domain.AccountId;

import java.util.Objects;

public record UpdateAccountCommand(
        AccountId accountId,
        String firstName,
        String lastName
) {

    public UpdateAccountCommand {
        Objects.requireNonNull(
                accountId,
                "account id must not be null"
        );

        Objects.requireNonNull(
                firstName,
                "first name must not be null"
        );

        Objects.requireNonNull(
                lastName,
                "last name must not be null"
        );
    }
}
