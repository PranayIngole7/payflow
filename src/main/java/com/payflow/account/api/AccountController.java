package com.payflow.account.api;

import com.payflow.account.application.CreateAccountUseCase;
import com.payflow.account.application.GetAccountUseCase;
import com.payflow.account.domain.AccountId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;

    public AccountController(
            CreateAccountUseCase createAccountUseCase,
            GetAccountUseCase getAccountUseCase
    ) {
        this.createAccountUseCase = Objects.requireNonNull(
                createAccountUseCase,
                "create account use case must not be null"
        );

        this.getAccountUseCase = Objects.requireNonNull(
                getAccountUseCase,
                "get account use case must not be null"
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount() {
        return AccountResponse.from(
                createAccountUseCase.execute()
        );
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccount(
            @PathVariable UUID accountId
    ) {
        return AccountResponse.from(
                getAccountUseCase.execute(
                        new AccountId(accountId)
                )
        );
    }
}