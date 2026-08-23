package com.gridgate.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunTest {

    @Test
    void createDryRunStartsInPlanReady() {
        Run run = sampleRun(true);

        assertEquals(RunStatus.PLAN_READY, run.getStatus());
        assertTrue(run.isDryRun());
        assertEquals(2, run.getProviders().size());
        assertTrue(run.hasMoreProviders());
        assertEquals("ZAR", run.getBudget().currencyCode());
    }

    @Test
    void createLiveRunStartsPending() {
        Run run = sampleRun(false);

        assertEquals(RunStatus.PENDING, run.getStatus());
        assertFalse(run.isDryRun());
    }

    @Test
    void startNextAttemptAdvancesIndexAndRecordsAttempt() {
        Run run = sampleRun(false);
        run.startNextAttempt(java.time.Instant.parse("2026-08-23T11:00:00Z"));

        assertEquals(RunStatus.RUNNING, run.getStatus());
        assertEquals(1, run.getAttempts().size());
        assertEquals(1, run.getNextProviderIndex());
        assertEquals("genhire-jhb", run.getAttempts().getFirst().getProviderId());
    }

    @Test
    void rejectsEmptyProviderList() {
        assertThrows(IllegalArgumentException.class, () -> Run.create(
                6,
                "Sandton",
                "Generator rental",
                ZonedDateTime.parse("2026-08-23T18:00:00+02:00"),
                Money.of(800, "ZAR"),
                true,
                List.of()));
    }

    private static Run sampleRun(boolean dryRun) {
        return Run.create(
                6,
                "Sandton",
                "5kVA generator rental",
                ZonedDateTime.parse("2026-08-23T18:00:00+02:00"),
                Money.of(800, "ZAR"),
                dryRun,
                List.of(
                        new ProviderSpec("genhire-jhb", "GenHire Joburg", "+14155550101"),
                        new ProviderSpec("power-on-call", "Power On Call", "+14155550102")));
    }
}
