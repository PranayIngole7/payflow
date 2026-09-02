package com.payflow.account.api;

import com.payflow.account.domain.Account;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID accountId,
        String status,
        Instant createdAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.id().value(),
                account.status().name(),
                account.createdAt()
        );
    }
}