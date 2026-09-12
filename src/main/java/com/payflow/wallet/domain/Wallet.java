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
    private final long version;

    private Wallet(
            WalletId id,
            AccountId accountId,
            Currency currency,
            Money balance,
            long version
    ) {
        this.id = Objects.requireNonNull(id, "wallet id must not be null");
        this.accountId = Objects.requireNonNull(accountId, "account id must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.balance = Objects.requireNonNull(balance, "balance must not be null");

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

        if (version < 0) {
            throw new IllegalArgumentException(
                    "wallet version must not be negative"
            );
        }

        this.version = version;
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
                new Money(BigDecimal.ZERO, currency),
                0L
        );
    }

    /**
     * Reconstructs an existing wallet from persistent state.
     *
     * <p>The version is persistence state used for optimistic
     * concurrency control. It is not modified by domain operations;
     * the persistence layer/database owns version advancement.</p>
     */
    public static Wallet reconstitute(
            WalletId id,
            AccountId accountId,
            Currency currency,
            Money balance,
            long version
    ) {
        return new Wallet(
                id,
                accountId,
                currency,
                balance,
                version
        );
    }

    public void credit(Money amount) {
        requireValidAmount(amount, "credit");
        balance = balance.add(amount);
    }

    public void debit(Money amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (!currency.equals(amount.currency())) {
            throw new IllegalArgumentException("Currency mismatch");
        }

        if (amount.amount().compareTo(balance.amount()) > 0) {
            throw new IllegalArgumentException(
                    "Insufficient wallet balance"
            );
        }

        balance = balance.subtract(amount);
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

    public long version() {
        return version;
    }

    private void requireValidAmount(Money amount, String operation) {
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