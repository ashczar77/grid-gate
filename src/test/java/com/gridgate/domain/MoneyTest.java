package com.gridgate.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalizesCurrencyCodeToUpperCase() {
        Money money = Money.of(100, "zar");
        assertTrue(money.currencyCode().equals("ZAR"));
    }

    @Test
    void withinLimitWhenSameCurrencyAndAmountLower() {
        Money quoted = Money.of(750, "ZAR");
        Money budget = Money.of(800, "ZAR");
        assertTrue(quoted.isWithinLimit(budget));
    }

    @Test
    void notWithinLimitWhenCurrencyDiffers() {
        Money quoted = Money.of(750, "USD");
        Money budget = Money.of(800, "ZAR");
        assertFalse(quoted.isWithinLimit(budget));
    }

    @Test
    void rejectsInvalidCurrencyCode() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(100, "RAND"));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(BigDecimal.valueOf(-1), "ZAR"));
    }
}
