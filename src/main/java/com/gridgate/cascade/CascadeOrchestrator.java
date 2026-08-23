package com.gridgate.cascade;

import com.gridgate.domain.ProviderAttempt;
import com.gridgate.domain.ProviderResult;
import com.gridgate.domain.Run;
import com.gridgate.domain.RunStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Sequential dial logic with early exit when hard success criteria match.
 */
public class CascadeOrchestrator {

    /**
     * Starts the next provider dial when the run can still continue.
     * Marks the run exhausted when no providers remain.
     */
    public Optional<ProviderAttempt> startNextDial(Run run, Instant now) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(now, "now");

        if (isTerminal(run.getStatus())) {
            return Optional.empty();
        }
        if (run.isDryRun()) {
            throw new IllegalStateException("Cannot dial while run is in dry-run plan mode");
        }
        if (!run.hasMoreProviders()) {
            run.markExhausted(now);
            return Optional.empty();
        }
        return Optional.of(run.startNextAttempt(now));
    }

    /**
     * Records a provider result and updates run status.
     */
    public CascadeStep recordAttemptResult(
            Run run, ProviderAttempt attempt, ProviderResult result, Instant now) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(attempt, "attempt");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(now, "now");

        validateActiveAttempt(run, attempt);
        attempt.complete(result, now);

        if (SuccessCriteria.isHardSuccess(result, run.getBudget())) {
            run.markFulfilled(attempt.getProviderId(), now);
            return CascadeStep.FULFILLED;
        }
        if (SuccessCriteria.isAmbiguousHalt(result)) {
            run.markHaltedAmbiguous(now);
            return CascadeStep.HALTED_AMBIGUOUS;
        }
        if (!run.hasMoreProviders()) {
            run.markExhausted(now);
            return CascadeStep.EXHAUSTED;
        }
        return CascadeStep.CONTINUE;
    }

    private static void validateActiveAttempt(Run run, ProviderAttempt attempt) {
        if (run.getAttempts().isEmpty()) {
            throw new IllegalStateException("Run has no attempts");
        }
        ProviderAttempt latest = run.getAttempts().getLast();
        if (!latest.getId().equals(attempt.getId())) {
            throw new IllegalArgumentException("attempt is not the active attempt");
        }
        if (attempt.isComplete()) {
            throw new IllegalStateException("attempt is already complete");
        }
    }

    private static boolean isTerminal(RunStatus status) {
        return status == RunStatus.FULFILLED
                || status == RunStatus.EXHAUSTED
                || status == RunStatus.HALTED_AMBIGUOUS
                || status == RunStatus.CANCELLED;
    }
}
