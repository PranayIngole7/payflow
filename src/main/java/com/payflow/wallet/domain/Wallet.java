package com.payflow.wallet.domain;

import com.payflow.account.domain.AccountId;
import com.payflow.shared.domain.Currency;
import com.payflow.shared.domain.Money;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Aggregate root representing a customer's wallet.
 *
 * <p>A wallet owns its current balance and is responsible for enforcing
 * balance-related business rules. Balance changes must happen through
 * {@link #credit(Money)} and {@link #debit(Money)} so that invalid state
 * cannot be introduced accidentally.</p>
 */
public final class Wallet {

    private final WalletId id;
    private final AccountId accountId;
    private final Currency currency;

    /**
     * Current wallet balance.
     *
     * <p>The field is mutable because the wallet is a stateful aggregate,
     * but it is never exposed for direct modification.</p>
     */
    private Money balance;

    private Wallet(
            WalletId id,
            AccountId accountId,
            Currency currency
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

        // Every newly created wallet starts with no funds.
        this.balance = new Money(BigDecimal.ZERO, currency);
    }

    /**
     * Creates a new wallet with a zero balance.
     *
     * @param id unique wallet identifier
     * @param accountId account that owns the wallet
     * @param currency currency in which the wallet operates
     * @return newly created wallet
     */
    public static Wallet create(
            WalletId id,
            AccountId accountId,
            Currency currency
    ) {
        return new Wallet(id, accountId, currency);
    }

    /**
     * Adds funds to the wallet.
     *
     * <p>Only strictly positive amounts are accepted. The {@link Money}
     * value object also guarantees that the operation cannot mix currencies.</p>
     *
     * @param amount amount to credit
     * @throws NullPointerException if amount is null
     * @throws IllegalArgumentException if the amount is zero or negative
     * @throws IllegalArgumentException if the amount uses another currency
     */
    public void credit(Money amount) {
        requireValidAmount(amount, "credit");

        // Money.add() enforces currency compatibility.
        balance = balance.add(amount);
    }

    /**
     * Removes funds from the wallet.
     *
     * <p>The wallet must never have a negative balance. Therefore the debit
     * operation is rejected when the requested amount exceeds the current
     * balance.</p>
     *
     * @param amount amount to debit
     * @throws NullPointerException if amount is null
     * @throws IllegalArgumentException if the amount is zero or negative
     * @throws IllegalArgumentException if the amount uses another currency
     * @throws IllegalArgumentException if there are insufficient funds
     */
    public void debit(Money amount) {
        requireValidAmount(amount, "debit");

        // Money.subtract() enforces currency compatibility.
        Money newBalance = balance.subtract(amount);

        if (newBalance.isNegative()) {
            throw new IllegalArgumentException(
                    "insufficient funds for wallet " + id
            );
        }

        balance = newBalance;
    }

    /**
     * Returns the wallet's unique identifier.
     */
    public WalletId id() {
        return id;
    }

    /**
     * Returns the account that owns this wallet.
     */
    public AccountId accountId() {
        return accountId;
    }

    /**
     * Returns the wallet's configured currency.
     */
    public Currency currency() {
        return currency;
    }

    /**
     * Returns the current balance.
     *
     * <p>{@link Money} is immutable, so returning it does not allow callers
     * to mutate wallet state.</p>
     */
    public Money balance() {
        return balance;
    }

    /**
     * Validates common rules for credit and debit operations.
     */
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