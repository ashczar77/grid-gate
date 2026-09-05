package com.gridgate.api;

public final class PhoneMasker {

    private PhoneMasker() {}

    /**
     * Masks an E.164 phone number for privacy, e.g. {@code +14155550101} -> {@code +1415****101}.
     */
    public static String mask(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String cleaned = phone.trim();
        if (cleaned.length() <= 6) {
            return "***";
        }

        // Keep first 5 chars (e.g. "+1415") and last 3 chars (e.g. "101"), mask the middle
        int len = cleaned.length();
        String prefix = cleaned.substring(0, Math.min(5, len - 3));
        String suffix = cleaned.substring(len - 3);
        String middle = "*".repeat(len - prefix.length() - suffix.length());

        return prefix + middle + suffix;
    }
}
