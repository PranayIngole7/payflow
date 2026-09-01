package com.payflow.shared.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void shouldCreateMoney() {
        Money money = new Money(
                new BigDecimal("100.00"),
                Currency.INR
        );

        assertEquals(new BigDecimal("100.00"), money.amount());
        assertEquals(Currency.INR, money.currency());
    }

    @Test
    void shouldAddSameCurrency() {
        Money first = new Money(
                new BigDecimal("100.00"),
                Currency.INR
        );

        Money second = new Money(
                new BigDecimal("50.00"),
                Currency.INR
        );

        assertEquals(
                new BigDecimal("150.00"),
                first.add(second).amount()
        );
    }

    @Test
    void shouldSubtractSameCurrency() {
        Money first = new Money(
                new BigDecimal("100.00"),
                Currency.INR
        );

        Money second = new Money(
                new BigDecimal("40.00"),
                Currency.INR
        );

        assertEquals(
                new BigDecimal("60.00"),
                first.subtract(second).amount()
        );
    }

    @Test
    void shouldRejectDifferentCurrencies() {
        Money inr = new Money(
                new BigDecimal("100.00"),
                Currency.INR
        );

        Money usd = new Money(
                new BigDecimal("50.00"),
                Currency.USD
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> inr.add(usd)
        );
    }



    @Test
    void shouldIdentifyZeroPositiveAndNegativeAmounts() {
        assertTrue(new Money(
                new BigDecimal("0.00"), Currency.INR
        ).isZero());

        assertTrue(new Money(
                new BigDecimal("10.00"), Currency.INR
        ).isPositive());

        assertTrue(new Money(
                new BigDecimal("-10.00"), Currency.INR
        ).isNegative());
    }

    @Test
    void shouldAllowBigDecimalScaleWithoutChangingMoneyValue() {
        Money money = new Money(
                new BigDecimal("100.000"),
                Currency.INR
        );

        assertEquals(new BigDecimal("100.000"), money.amount());
        assertEquals(Currency.INR, money.currency());
    }
}