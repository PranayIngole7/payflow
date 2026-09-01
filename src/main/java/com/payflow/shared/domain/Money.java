package com.payflow.shared.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable representation of a monetary amount and its currency.
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
    }

    public Money add(Money other) {
        requireSameCurrency(other);

        return new Money(
                amount.add(other.amount),
                currency
        );
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);

        return new Money(
                amount.subtract(other.amount),
                currency
        );
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");

        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "cannot operate on different currencies: "
                            + currency + " and " + other.currency
            );
        }
    }
}