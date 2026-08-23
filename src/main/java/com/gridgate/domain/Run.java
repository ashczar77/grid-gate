package com.gridgate.domain;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A single cascade request: call providers in order until one succeeds or the list is exhausted.
 */
public class Run {

    private final UUID id;
    private final int stage;
    private final String area;
    private final String need;
    private final ZonedDateTime deadline;
    private final Money budget;
    private final boolean dryRun;
    private final List<ProviderSpec> providers;
    private final List<ProviderAttempt> attempts;
    private final Instant createdAt;

    private RunStatus status;
    private int nextProviderIndex;
    private String winnerProviderId;
    private Instant updatedAt;

    public Run(
            UUID id,
            int stage,
            String area,
            String need,
            ZonedDateTime deadline,
            Money budget,
            boolean dryRun,
            List<ProviderSpec> providers,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        if (stage < 0) {
            throw new IllegalArgumentException("stage must be >= 0");
        }
        this.stage = stage;
        this.area = requireText(area, "area");
        this.need = requireText(need, "need");
        this.deadline = Objects.requireNonNull(deadline, "deadline");
        this.budget = Objects.requireNonNull(budget, "budget");
        this.dryRun = dryRun;
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("providers must not be empty");
        }
        this.providers = List.copyOf(providers);
        this.attempts = new ArrayList<>();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = dryRun ? RunStatus.PLAN_READY : RunStatus.PENDING;
        this.nextProviderIndex = 0;
        this.updatedAt = createdAt;
    }

    public static Run create(
            int stage,
            String area,
            String need,
            ZonedDateTime deadline,
            Money budget,
            boolean dryRun,
            List<ProviderSpec> providers) {
        Instant now = Instant.now();
        return new Run(
                UUID.randomUUID(),
                stage,
                area,
                need,
                deadline,
                budget,
                dryRun,
                providers,
                now);
    }

    public UUID getId() {
        return id;
    }

    public int getStage() {
        return stage;
    }

    public String getArea() {
        return area;
    }

    public String getNeed() {
        return need;
    }

    public ZonedDateTime getDeadline() {
        return deadline;
    }

    public Money getBudget() {
        return budget;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public List<ProviderSpec> getProviders() {
        return providers;
    }

    public List<ProviderAttempt> getAttempts() {
        return Collections.unmodifiableList(attempts);
    }

    public RunStatus getStatus() {
        return status;
    }

    public int getNextProviderIndex() {
        return nextProviderIndex;
    }

    public Optional<String> getWinnerProviderId() {
        return Optional.ofNullable(winnerProviderId);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean hasMoreProviders() {
        return nextProviderIndex < providers.size();
    }

    public Optional<ProviderSpec> peekNextProvider() {
        if (!hasMoreProviders()) {
            return Optional.empty();
        }
        return Optional.of(providers.get(nextProviderIndex));
    }

    public ProviderAttempt startNextAttempt(Instant now) {
        if (status == RunStatus.CANCELLED
                || status == RunStatus.FULFILLED
                || status == RunStatus.EXHAUSTED
                || status == RunStatus.HALTED_AMBIGUOUS) {
            throw new IllegalStateException("Cannot start attempt while run is " + status);
        }
        ProviderSpec spec = peekNextProvider()
                .orElseThrow(() -> new IllegalStateException("No providers left to call"));
        status = RunStatus.RUNNING;
        ProviderAttempt attempt = ProviderAttempt.fromSpec(spec, nextProviderIndex);
        attempts.add(attempt);
        nextProviderIndex++;
        touch(now);
        return attempt;
    }

    public void markFulfilled(String providerId, Instant now) {
        this.winnerProviderId = requireText(providerId, "providerId");
        this.status = RunStatus.FULFILLED;
        touch(now);
    }

    public void markExhausted(Instant now) {
        this.status = RunStatus.EXHAUSTED;
        touch(now);
    }

    public void markHaltedAmbiguous(Instant now) {
        this.status = RunStatus.HALTED_AMBIGUOUS;
        touch(now);
    }

    public void cancel(Instant now) {
        this.status = RunStatus.CANCELLED;
        touch(now);
    }

    private void touch(Instant now) {
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
