package com.payflow.account.application;

import java.util.Objects;

public record RegisterAccountCommand(
        String email,
        String firstName,
        String lastName,
        String password
) {

    public RegisterAccountCommand {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(firstName, "first name must not be null");
        Objects.requireNonNull(lastName, "last name must not be null");
        Objects.requireNonNull(password, "password must not be null");
    }
}