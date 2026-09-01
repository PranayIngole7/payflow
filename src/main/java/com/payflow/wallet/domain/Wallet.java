package com.payflow.wallet.domain;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Aggregate root representing a customer's wallet.
 */
public final class Wallet {

    private final WalletId id;
    private final AccountId accountId;
    private final Currency currency;
    private Money balance;

    private Wallet(
            WalletId id,
            AccountId accountId,
            Currency currency,
            Money balance
    ) {
        this.id = Objects.requireNonNull(id, "wallet id must not be null");
        this.accountId = Objects.requireNonNull(
                accountId,
                "account id must not be null"
        );
        this.currency = Objects.requireNonNull(
                currency,
                "currency must not be null"
        );
        this.balance = Objects.requireNonNull(
                balance,
                "balance must not be null"
        );

        if (!currency.equals(balance.currency())) {
            throw new IllegalArgumentException(
                    "wallet currency must match balance currency"
            );
        }

        if (balance.isNegative()) {
            throw new IllegalArgumentException(
                    "wallet balance must not be negative"
            );
        }
    }

    public static Wallet create(
            WalletId id,
            AccountId accountId,
            Currency currency
    ) {
        return new Wallet(
                id,
                accountId,
                currency,
                new Money(BigDecimal.ZERO, currency)
        );
    }

    /**
     * Reconstructs an existing wallet from persistent state.
     *
     * <p>Intentionally package-private so persistence/application code
     * cannot freely bypass the aggregate API.</p>
     */
  public static Wallet reconstitute(
            WalletId id,
            AccountId accountId,
            Currency currency,
            Money balance
    ) {
        return new Wallet(
                id,
                accountId,
                currency,
                balance
        );
    }

    public void credit(Money amount) {
        requireValidAmount(amount, "credit");
        balance = balance.add(amount);
    }

    public void debit(Money amount) {
        requireValidAmount(amount, "debit");

        Money newBalance = balance.subtract(amount);

        if (newBalance.isNegative()) {
            throw new IllegalArgumentException(
                    "insufficient funds for wallet " + id
            );
        }

        balance = newBalance;
    }

    public WalletId id() {
        return id;
    }

    public AccountId accountId() {
        return accountId;
    }

    public Currency currency() {
        return currency;
    }

    public Money balance() {
        return balance;
    }

    private void requireValidAmount(
            Money amount,
            String operation
    ) {
        Objects.requireNonNull(
                amount,
                operation + " amount must not be null"
        );

        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    operation + " amount must be greater than zero"
            );
        }
    }
}