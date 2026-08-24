package com.gridgate.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaRunLedger.class, RunMapper.class})
class JpaRunLedgerTest {

    @Autowired
    private RunLedger ledger;

    @Test
    void savesAndRetrievesRunSuccessfully() {
        Run run = Run.create(
                6,
                "Sandton",
                "Fix borehole pump during outage",
                ZonedDateTime.now().plusHours(3),
                Money.of(1500, "ZAR"),
                true,
                List.of(
                        new ProviderSpec("p1", "Alpha Plumb", "+14155550101"),
                        new ProviderSpec("p2", "Beta Electric", "+14155550102")));

        Run saved = ledger.save(run);
        assertNotNull(saved);
        assertEquals(run.getId(), saved.getId());
        assertEquals(RunStatus.PLAN_READY, saved.getStatus());
        assertTrue(saved.isDryRun());

        Optional<Run> retrieved = ledger.findById(run.getId());
        assertTrue(retrieved.isPresent());
        Run r = retrieved.get();
        assertEquals(6, r.getStage());
        assertEquals("Sandton", r.getArea());
        assertEquals("Fix borehole pump during outage", r.getNeed());
        assertEquals(2, r.getProviders().size());
        assertEquals("Alpha Plumb", r.getProviders().get(0).name());
        assertEquals("Beta Electric", r.getProviders().get(1).name());
    }

    @Test
    void savesAndRetrievesRunWithAttemptsAndResults() {
        Run run = Run.create(
                4,
                "Bryanston",
                "Emergency generator diesel delivery",
                ZonedDateTime.now().plusHours(2),
                Money.of(2500, "ZAR"),
                false,
                List.of(new ProviderSpec("p1", "QuickFuel", "+14155550101")));

        ProviderAttempt attempt = run.startNextAttempt(Instant.now());
        attempt.markStarted("call_xyz123", Instant.now());
        ProviderResult result = new ProviderResult(
                "QuickFuel",
                TriState.YES,
                TriState.YES,
                Money.of(2200, "ZAR"),
                45,
                "within 1 hour",
                "Confirmed diesel in stock and driver ready",
                CommitmentMade.HOLD_ONLY,
                Outcome.SUCCESS);
        attempt.complete(result, Instant.now());
        run.markFulfilled("p1", Instant.now());

        ledger.save(run);

        Optional<Run> retrieved = ledger.findById(run.getId());
        assertTrue(retrieved.isPresent());
        Run r = retrieved.get();
        assertEquals(RunStatus.FULFILLED, r.getStatus());
        assertEquals("p1", r.getWinnerProviderId().orElse(null));
        assertEquals(1, r.getAttempts().size());

        ProviderAttempt att = r.getAttempts().get(0);
        assertEquals("call_xyz123", att.getCalleCallId().orElse(null));
        assertTrue(att.isComplete());
        assertEquals(Outcome.SUCCESS, att.getResult().get().outcome());
        assertEquals(TriState.YES, att.getResult().get().canService());
        assertEquals(Money.of(2200, "ZAR"), att.getResult().get().quotedPrice());
    }
}
