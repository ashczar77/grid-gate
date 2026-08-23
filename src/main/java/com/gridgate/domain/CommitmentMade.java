package com.gridgate.domain;

/**
 * Whether the callee agreed to any commitment during the call.
 */
public enum CommitmentMade {
    NONE,
    HOLD_ONLY,
    BOOKING;

    public static CommitmentMade fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        return switch (value.trim().toLowerCase()) {
            case "hold_only" -> HOLD_ONLY;
            case "booking" -> BOOKING;
            default -> NONE;
        };
    }

    public String toApiValue() {
        return switch (this) {
            case NONE -> "none";
            case HOLD_ONLY -> "hold_only";
            case BOOKING -> "booking";
        };
    }
}
