package com.gridgate.domain;

/**
 * Yes / no / unknown answers extracted from a provider call.
 */
public enum TriState {
    YES,
    NO,
    UNKNOWN;

    public static TriState fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value.trim().toLowerCase()) {
            case "yes" -> YES;
            case "no" -> NO;
            default -> UNKNOWN;
        };
    }

    public String toApiValue() {
        return name().toLowerCase();
    }
}
