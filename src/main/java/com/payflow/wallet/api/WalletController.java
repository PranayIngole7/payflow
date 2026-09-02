package com.payflow.wallet.api;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Money;
import com.payflow.wallet.application.CreateWalletUseCase;
import com.payflow.wallet.application.GetWalletUseCase;
import com.payflow.wallet.domain.WalletId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final CreateWalletUseCase createWalletUseCase;
    private final GetWalletUseCase getWalletUseCase;

    public WalletController(
            CreateWalletUseCase createWalletUseCase,
            GetWalletUseCase getWalletUseCase
    ) {
        this.createWalletUseCase = Objects.requireNonNull(
                createWalletUseCase,
                "create wallet use case must not be null"
        );

        this.getWalletUseCase = Objects.requireNonNull(
                getWalletUseCase,
                "get wallet use case must not be null"
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet(
            @Valid @RequestBody CreateWalletRequest request
    ) {
        return WalletResponse.from(
                createWalletUseCase.execute(
                        new AccountId(request.accountId()),
                        new Money(
                                request.initialBalance(),
                                request.currency()
                        )
                )
        );
    }

    @GetMapping("/{walletId}")
    public WalletResponse getWallet(
            @PathVariable UUID walletId
    ) {
        return WalletResponse.from(
                getWalletUseCase.execute(
                        new WalletId(walletId)
                )
        );
    }
}