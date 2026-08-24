package com.gridgate.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PhoneMaskerTest {

    @Test
    void masksStandardE164SouthAfricanPhone() {
        assertEquals("+1415****101", PhoneMasker.mask("+14155550101"));
        assertEquals("+1415****102", PhoneMasker.mask("+14155550101"));
    }

    @Test
    void handlesShortOrNullStringsSafely() {
        assertEquals("", PhoneMasker.mask(null));
        assertEquals("", PhoneMasker.mask("   "));
        assertEquals("***", PhoneMasker.mask("+1415"));
        assertEquals("***", PhoneMasker.mask("123456"));
    }
}
