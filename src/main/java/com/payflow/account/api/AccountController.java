package com.payflow.account.api;

import com.payflow.account.application.CreateAccountUseCase;
import com.payflow.account.application.GetAccountUseCase;
import com.payflow.account.application.RegisterAccountCommand;
import com.payflow.account.application.SuspendAccountUseCase;
import com.payflow.account.application.UpdateAccountCommand;
import com.payflow.account.application.UpdateAccountUseCase;
import com.payflow.account.domain.Account;
import com.payflow.account.domain.AccountId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final SuspendAccountUseCase suspendAccountUseCase;
    private final UpdateAccountUseCase updateAccountUseCase;

    public AccountController(
            CreateAccountUseCase createAccountUseCase,
            GetAccountUseCase getAccountUseCase,
            SuspendAccountUseCase suspendAccountUseCase,
            UpdateAccountUseCase updateAccountUseCase
    ) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountUseCase = getAccountUseCase;
        this.suspendAccountUseCase = suspendAccountUseCase;
        this.updateAccountUseCase = updateAccountUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(
            @Valid @RequestBody RegisterAccountRequest request
    ) {
        RegisterAccountCommand command = new RegisterAccountCommand(
                request.email(),
                request.firstName(),
                request.lastName(),
                request.password()
        );

        Account account = createAccountUseCase.execute(command);

        return AccountResponse.from(account);
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

    @PatchMapping("/{accountId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendAccount(
            @PathVariable UUID accountId
    ) {
        suspendAccountUseCase.execute(
                new AccountId(accountId)
        );
    }

    @PatchMapping("/{accountId}")
    public AccountResponse updateAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        Account account = updateAccountUseCase.execute(
                new UpdateAccountCommand(
                        new AccountId(accountId),
                        request.firstName(),
                        request.lastName()
                )
        );

        return AccountResponse.from(account);
    }

    public record UpdateAccountRequest(
            @NotBlank
            @Size(max = 100)
            String firstName,

            @NotBlank
            @Size(max = 100)
            String lastName
    ) {
    }

    public record RegisterAccountRequest(
            @NotBlank
            @Email
            @Size(max = 255)
            String email,

            @NotBlank
            @Size(max = 100)
            String firstName,

            @NotBlank
            @Size(max = 100)
            String lastName,

            @NotBlank
            @Size(min = 8, max = 255)
            String password
    ) {
    }
}