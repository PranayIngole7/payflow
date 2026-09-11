package com.payflow.account.application;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.application.TransactionRunner;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public final class ChangePasswordUseCase {

    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final TransactionRunner transactionRunner;

    public ChangePasswordUseCase(
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

    public void execute(ChangePasswordCommand command) {
        Objects.requireNonNull(
                command,
                "change password command must not be null"
        );

        AccountId accountId = command.accountId();

        String passwordHash =
                accountRepository.findPasswordHash(accountId)
                        .orElseThrow(() -> new NoSuchElementException(
                                "account not found: " + accountId.value()
                        ));

        if (!passwordHasher.matches(
                command.currentPassword(),
                passwordHash
        )) {
            throw new IllegalStateException(
                    "current password is incorrect"
            );
        }

        if (command.newPassword() == null
                || command.newPassword().isBlank()) {
            throw new IllegalArgumentException(
                    "new password must not be blank"
            );
        }

        if (command.newPassword().length() < 8) {
            throw new IllegalArgumentException(
                    "new password must be at least 8 characters"
            );
        }

        if (command.newPassword().length() > 255) {
            throw new IllegalArgumentException(
                    "new password must not exceed 255 characters"
            );
        }

        String newPasswordHash =
                passwordHasher.hash(command.newPassword());

        transactionRunner.execute(
                () -> accountRepository.updatePassword(
                        accountId,
                        newPasswordHash
                )
        );
    }
}

