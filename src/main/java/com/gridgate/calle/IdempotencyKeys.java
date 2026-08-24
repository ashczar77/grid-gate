package com.gridgate.calle;

import java.util.Objects;
import java.util.UUID;

/**
 * Helper to build and validate standard CALL-E Idempotency-Key values for GridGate dials.
 * Format: {@code gridgate_{runId}_{providerId}}
 */
public final class IdempotencyKeys {

    public static final String PREFIX = "gridgate";

    private IdempotencyKeys() {
    }

    /**
     * Builds an idempotency key from a {@link UUID} run ID and string provider ID.
     */
    public static String forAttempt(UUID runId, String providerId) {
        Objects.requireNonNull(runId, "runId");
        return forAttempt(runId.toString(), providerId);
    }

    /**
     * Builds an idempotency key from string identifiers.
     */
    public static String forAttempt(String runId, String providerId) {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(providerId, "providerId");

        if (runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be blank");
        }

        return String.format("%s_%s_%s", PREFIX, runId.trim(), providerId.trim());
    }
}
