package com.gridgate.domain;

/**
 * How a single provider call attempt ended.
 */
public enum Outcome {
    SUCCESS,
    REJECTED,
    UNREACHABLE,
    REFUSED,
    VOICEMAIL,
    AMBIGUOUS;

    /**
     * Maps CALL-E recipient schema {@code disposition} values to domain outcomes.
     */
    public static Outcome fromDisposition(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("disposition is required");
        }
        return switch (value.trim().toLowerCase()) {
            case "success" -> SUCCESS;
            case "rejected" -> REJECTED;
            case "unreachable" -> UNREACHABLE;
            case "refused" -> REFUSED;
            case "voicemail" -> VOICEMAIL;
            case "ambiguous" -> AMBIGUOUS;
            default -> throw new IllegalArgumentException("unknown disposition: " + value);
        };
    }

    public String toDispositionValue() {
        return name().toLowerCase();
    }
}
