package com.gridgate.calle;

import com.gridgate.domain.ProviderAttempt;
import com.gridgate.domain.Run;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Encapsulates the metadata passed to CALL-E tasks and echoed in terminal webhooks.
 *
 * <p>Keys:
 * <ul>
 *   <li>{@code gridgate_run_id} - GridGate Run UUID</li>
 *   <li>{@code provider_id} - target provider identifier</li>
 *   <li>{@code stage} - Eskom load-shedding stage</li>
 *   <li>{@code area} - affected suburb/area</li>
 * </ul>
 */
public record CalleMetadata(
        UUID runId,
        String providerId,
        int stage,
        String area) {

    public static final String KEY_RUN_ID = "gridgate_run_id";
    public static final String KEY_PROVIDER_ID = "provider_id";
    public static final String KEY_STAGE = "stage";
    public static final String KEY_AREA = "area";

    public CalleMetadata {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(providerId, "providerId");
        if (providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
        if (stage < 0) {
            throw new IllegalArgumentException("stage must be >= 0");
        }
        Objects.requireNonNull(area, "area");
        if (area.isBlank()) {
            throw new IllegalArgumentException("area must not be blank");
        }
    }

    /**
     * Constructs metadata from an active {@link Run} and {@link ProviderAttempt}.
     */
    public static CalleMetadata forAttempt(Run run, ProviderAttempt attempt) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(attempt, "attempt");
        return new CalleMetadata(
                run.getId(),
                attempt.getProviderId(),
                run.getStage(),
                run.getArea());
    }

    /**
     * Converts this metadata into a string map for CALL-E request payloads.
     */
    public Map<String, String> toMap() {
        return Map.of(
                KEY_RUN_ID, runId.toString(),
                KEY_PROVIDER_ID, providerId,
                KEY_STAGE, String.valueOf(stage),
                KEY_AREA, area);
    }

    /**
     * Parses metadata from a key-value map (e.g. from {@code WebhookEvent.metadata()}).
     *
     * @return {@link Optional} containing parsed {@link CalleMetadata}, or {@code Optional.empty()} if missing or invalid.
     */
    public static Optional<CalleMetadata> fromMap(Map<String, String> map) {
        if (map == null) {
            return Optional.empty();
        }

        String runIdStr = map.get(KEY_RUN_ID);
        String providerId = map.get(KEY_PROVIDER_ID);
        String stageStr = map.get(KEY_STAGE);
        String area = map.get(KEY_AREA);

        if (runIdStr == null || runIdStr.isBlank()
                || providerId == null || providerId.isBlank()
                || stageStr == null || stageStr.isBlank()
                || area == null || area.isBlank()) {
            return Optional.empty();
        }

        try {
            UUID runId = UUID.fromString(runIdStr.trim());
            int stage = Integer.parseInt(stageStr.trim());
            if (stage < 0) {
                return Optional.empty();
            }
            return Optional.of(new CalleMetadata(runId, providerId.trim(), stage, area.trim()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
