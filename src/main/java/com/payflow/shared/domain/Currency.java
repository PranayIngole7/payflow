package com.payflow.shared.domain;

import java.util.Objects;

/**
 * Supported monetary currency.
 */
public enum Currency {

    INR(2),
    USD(2),
    EUR(2);

    private final int defaultScale;

    Currency(int defaultScale) {
        this.defaultScale = defaultScale;
    }

    public int defaultScale() {
        return defaultScale;
    }
}