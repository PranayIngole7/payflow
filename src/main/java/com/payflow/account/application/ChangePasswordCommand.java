package com.payflow.account.application;

import com.payflow.account.domain.AccountId;

public record ChangePasswordCommand(
        AccountId accountId,
        String currentPassword,
        String newPassword
) {
}


