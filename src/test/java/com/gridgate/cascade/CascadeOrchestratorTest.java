package com.gridgate.cascade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gridgate.domain.CommitmentMade;
import com.gridgate.domain.Money;
import com.gridgate.domain.Outcome;
import com.gridgate.domain.ProviderAttempt;
import com.gridgate.domain.ProviderResult;
import com.gridgate.domain.ProviderSpec;
import com.gridgate.domain.Run;
import com.gridgate.domain.RunStatus;
import com.gridgate.domain.TriState;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CascadeOrchestratorTest {

    private static final Instant NOW = Instant.parse("2026-08-23T12:00:00Z");
    private static final Money BUDGET = Money.of(800, "ZAR");

    private CascadeOrchestrator orchestrator;
    private Run run;

    @BeforeEach
    void setUp() {
        orchestrator = new CascadeOrchestrator();
        run = Run.create(
                6,
                "Sandton",
                "5kVA generator rental",
                ZonedDateTime.parse("2026-08-23T18:00:00+02:00"),
                BUDGET,
                false,
                List.of(
                        new ProviderSpec("provider-a", "Provider A", "+14155550101"),
                        new ProviderSpec("provider-b", "Provider B", "+14155550102"),
                        new ProviderSpec("provider-c", "Provider C", "+14155550103")));
    }

    @Test
    void stopsEarlyOnThirdProviderAfterTwoFailures() {
        dialAndRecord(failureResult("Provider A", Outcome.REJECTED));
        assertEquals(CascadeStep.CONTINUE, lastStep());

        dialAndRecord(failureResult("Provider B", Outcome.UNREACHABLE));
        assertEquals(CascadeStep.CONTINUE, lastStep());

        dialAndRecord(successResult("Provider C"));
        assertEquals(CascadeStep.FULFILLED, lastStep());

        assertEquals(RunStatus.FULFILLED, run.getStatus());
        assertEquals("provider-c", run.getWinnerProviderId().orElseThrow());
        assertEquals(3, run.getAttempts().size());
        assertFalse(run.hasMoreProviders());
    }

    @Test
    void exhaustsWhenNoProviderSucceeds() {
        dialAndRecord(failureResult("Provider A", Outcome.REJECTED));
        dialAndRecord(failureResult("Provider B", Outcome.VOICEMAIL));
        dialAndRecord(failureResult("Provider C", Outcome.REFUSED));

        assertEquals(RunStatus.EXHAUSTED, run.getStatus());
        assertTrue(run.getWinnerProviderId().isEmpty());
        assertEquals(3, run.getAttempts().size());
    }

    @Test
    void haltsOnAmbiguousOutcomeWithoutDialingRemainingProviders() {
        dialAndRecord(failureResult("Provider A", Outcome.REJECTED));

        ProviderAttempt second = orchestrator.startNextDial(run, NOW).orElseThrow();
        CascadeStep step = orchestrator.recordAttemptResult(
                run, second, failureResult("Provider B", Outcome.AMBIGUOUS), NOW);

        assertEquals(CascadeStep.HALTED_AMBIGUOUS, step);
        assertEquals(RunStatus.HALTED_AMBIGUOUS, run.getStatus());
        assertEquals(2, run.getAttempts().size());
        assertTrue(run.hasMoreProviders());
        assertTrue(orchestrator.startNextDial(run, NOW).isEmpty());
    }

    @Test
    void rejectsDialDuringDryRunPlan() {
        Run dryRun = Run.create(
                6,
                "Sandton",
                "Generator rental",
                ZonedDateTime.parse("2026-08-23T18:00:00+02:00"),
                BUDGET,
                true,
                List.of(new ProviderSpec("provider-a", "Provider A", "+14155550101")));

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, () -> orchestrator.startNextDial(dryRun, NOW));
    }

    private CascadeStep lastStep() {
        return lastRecordedStep;
    }

    private CascadeStep lastRecordedStep;

    private void dialAndRecord(ProviderResult result) {
        ProviderAttempt attempt = orchestrator.startNextDial(run, NOW).orElseThrow();
        lastRecordedStep = orchestrator.recordAttemptResult(run, attempt, result, NOW);
    }

    private static ProviderResult successResult(String providerName) {
        return new ProviderResult(
                providerName,
                TriState.YES,
                TriState.YES,
                Money.of(750, "ZAR"),
                45,
                "Before 18:00",
                "Yes, we can help during Stage 6.",
                CommitmentMade.NONE,
                Outcome.SUCCESS);
    }

    private static ProviderResult failureResult(String providerName, Outcome outcome) {
        return new ProviderResult(
                providerName,
                TriState.NO,
                TriState.UNKNOWN,
                null,
                null,
                null,
                outcome == Outcome.AMBIGUOUS ? "Could not get a clear answer." : "No.",
                CommitmentMade.NONE,
                outcome);
    }
}
