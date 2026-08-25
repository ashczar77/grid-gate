package com.gridgate.cascade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridgate.api.RunEventHub;
import com.gridgate.domain.Run;
import com.gridgate.domain.RunStatus;
import com.gridgate.ledger.RunLedger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RunSimulatorTest {

    @Autowired
    private RunSimulator runSimulator;

    @Autowired
    private RunLedger ledger;

    @Test
    void executeSimulationRunsFullDryRunCascadeToSuccess() {
        Run run = runSimulator.executeSimulation();

        assertNotNull(run);
        assertNotNull(run.getId());
        assertEquals(RunStatus.FULFILLED, run.getStatus());
        assertEquals("prov-sandton-inverter", run.getWinnerProviderId().orElse(null));
        assertEquals(3, run.getAttempts().size());

        // Verify attempt sequence and outcomes
        assertEquals("prov-fastspark", run.getAttempts().get(0).getProviderId());
        assertEquals("rejected", run.getAttempts().get(0).getResult().orElseThrow().outcome().toDispositionValue());

        assertEquals("prov-poweron", run.getAttempts().get(1).getProviderId());
        assertEquals("unreachable", run.getAttempts().get(1).getResult().orElseThrow().outcome().toDispositionValue());

        assertEquals("prov-sandton-inverter", run.getAttempts().get(2).getProviderId());
        assertEquals("success", run.getAttempts().get(2).getResult().orElseThrow().outcome().toDispositionValue());

        // Verify persisted state in ledger
        Run persisted = ledger.findById(run.getId()).orElseThrow();
        assertEquals(RunStatus.FULFILLED, persisted.getStatus());
    }
}
