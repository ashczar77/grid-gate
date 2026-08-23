package com.gridgate.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Monetary amount with ISO 4217 currency code (e.g. ZAR, NGN, USD).
 */
public record Money(BigDecimal amount, String currencyCode) {

    private static final String ISO_4217 = "[A-Z]{3}";

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currencyCode, "currencyCode");
        amount = amount.stripTrailingZeros();
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        String normalized = currencyCode.trim().toUpperCase();
        if (!normalized.matches(ISO_4217)) {
            throw new IllegalArgumentException("currencyCode must be a 3-letter ISO 4217 code");
        }
        currencyCode = normalized;
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, currencyCode);
    }

    public static Money of(long amount, String currencyCode) {
        return new Money(BigDecimal.valueOf(amount), currencyCode);
    }

    public boolean isSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        return currencyCode.equals(other.currencyCode);
    }

    /**
     * True when this amount is less than or equal to {@code limit} in the same currency.
     */
    public boolean isWithinLimit(Money limit) {
        Objects.requireNonNull(limit, "limit");
        if (!isSameCurrency(limit)) {
            return false;
        }
        return amount.compareTo(limit.amount) <= 0;
    }
}
