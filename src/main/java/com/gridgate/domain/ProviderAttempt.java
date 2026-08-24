package com.gridgate.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One dial attempt against a provider within a run.
 */
public class ProviderAttempt {

    private final UUID id;
    private final String providerId;
    private final String providerName;
    private final String phoneE164;
    private final int sequenceIndex;
    private String calleCallId;
    private ProviderResult result;
    private Instant startedAt;
    private Instant completedAt;

    public ProviderAttempt(
            UUID id,
            String providerId,
            String providerName,
            String phoneE164,
            int sequenceIndex) {
        this.id = Objects.requireNonNull(id, "id");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.providerName = Objects.requireNonNull(providerName, "providerName");
        this.phoneE164 = Objects.requireNonNull(phoneE164, "phoneE164");
        if (sequenceIndex < 0) {
            throw new IllegalArgumentException("sequenceIndex must be >= 0");
        }
        this.sequenceIndex = sequenceIndex;
    }

    public static ProviderAttempt fromSpec(ProviderSpec spec, int sequenceIndex) {
        return new ProviderAttempt(
                UUID.randomUUID(),
                spec.id(),
                spec.name(),
                spec.phoneE164(),
                sequenceIndex);
    }

    public UUID getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getPhoneE164() {
        return phoneE164;
    }

    public int getSequenceIndex() {
        return sequenceIndex;
    }

    public Optional<String> getCalleCallId() {
        return Optional.ofNullable(calleCallId);
    }

    public void markStarted(String calleCallId, Instant startedAt) {
        this.calleCallId = Objects.requireNonNull(calleCallId, "calleCallId");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    }

    public Optional<Instant> getStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<ProviderResult> getResult() {
        return Optional.ofNullable(result);
    }

    public boolean isComplete() {
        return result != null;
    }

    public void complete(ProviderResult result, Instant completedAt) {
        this.result = Objects.requireNonNull(result, "result");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt");
    }

    public Optional<Instant> getCompletedAt() {
        return Optional.ofNullable(completedAt);
    }

    public static ProviderAttempt restore(
            UUID id,
            String providerId,
            String providerName,
            String phoneE164,
            int sequenceIndex,
            String calleCallId,
            ProviderResult result,
            Instant startedAt,
            Instant completedAt) {
        ProviderAttempt attempt = new ProviderAttempt(id, providerId, providerName, phoneE164, sequenceIndex);
        attempt.calleCallId = calleCallId;
        attempt.result = result;
        attempt.startedAt = startedAt;
        attempt.completedAt = completedAt;
        return attempt;
    }
}
