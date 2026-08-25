package com.gridgate.cascade;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridgate.api.RunEventHub;
import com.gridgate.calle.RecipientResultMapper;
import com.gridgate.domain.Money;
import com.gridgate.domain.ProviderAttempt;
import com.gridgate.domain.ProviderResult;
import com.gridgate.domain.ProviderSpec;
import com.gridgate.domain.Run;
import com.gridgate.ledger.RunLedger;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Executes a simulated dry-run cascade using fixture data from {@code providers.example.json}.
 * Advances through simulated load-shedding provider dispositions without consuming CALL-E credits.
 */
@Service
public class RunSimulator {

    private static final Logger log = LoggerFactory.getLogger(RunSimulator.class);
    private static final String EXAMPLE_FIXTURE = "/providers.example.json";

    private final RunLedger ledger;
    private final CascadeOrchestrator orchestrator;
    private final RunEventHub eventHub;
    private final ObjectMapper objectMapper;

    public RunSimulator(
            RunLedger ledger,
            CascadeOrchestrator orchestrator,
            RunEventHub eventHub,
            ObjectMapper objectMapper) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
        this.eventHub = Objects.requireNonNull(eventHub, "eventHub");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Creates and executes a complete simulated cascade run from {@code providers.example.json}.
     *
     * @return the resulting {@link Run} after simulated execution reaches a terminal status
     */
    public Run executeSimulation() {
        Map<String, Object> fixture = loadFixture(EXAMPLE_FIXTURE);

        int stage = ((Number) fixture.getOrDefault("stage", 6)).intValue();
        String area = (String) fixture.getOrDefault("area", "Sandton");
        String need = (String) fixture.getOrDefault("need", "Emergency backup power assistance");
        ZonedDateTime deadline = ZonedDateTime.parse((String) fixture.get("deadline"));
        BigDecimal budgetAmount = BigDecimal.valueOf(((Number) fixture.getOrDefault("budget_amount", 1800.00)).doubleValue());
        String budgetCurrency = (String) fixture.getOrDefault("budget_currency", "ZAR");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawProviders = (List<Map<String, Object>>) fixture.get("providers");

        List<ProviderSpec> providerSpecs = new ArrayList<>();
        Map<String, ProviderResult> simulatedResults = new HashMap<>();

        for (Map<String, Object> p : rawProviders) {
            String id = (String) p.get("id");
            String name = (String) p.get("name");
            String phone = (String) p.get("phone_e164");
            providerSpecs.add(new ProviderSpec(id, name, phone));

            @SuppressWarnings("unchecked")
            Map<String, Object> rawResult = (Map<String, Object>) p.get("simulated_result");
            if (rawResult != null) {
                Map<String, Object> structured = new HashMap<>(rawResult);
                structured.putIfAbsent("provider_name", name);
                ProviderResult providerResult = RecipientResultMapper.fromStructuredResult(structured);
                simulatedResults.put(id, providerResult);
            }
        }

        Run run = Run.create(
                stage,
                area,
                need,
                deadline,
                Money.of(budgetAmount, budgetCurrency),
                true,
                providerSpecs);

        ledger.save(run);
        eventHub.publishUpdate(run);

        Instant now = Instant.now();
        run.armLive(now);
        ledger.save(run);
        eventHub.publishUpdate(run);

        while (run.hasMoreProviders()) {
            ProviderAttempt attempt = orchestrator.startNextDial(run, now)
                    .orElseThrow(() -> new IllegalStateException("Simulation failed to start dial"));

            attempt.markStarted("sim-call-" + attempt.getSequenceIndex(), now);
            ledger.save(run);
            eventHub.publishUpdate(run);

            ProviderResult result = simulatedResults.get(attempt.getProviderId());
            if (result == null) {
                throw new IllegalStateException("No simulated result found for provider " + attempt.getProviderId());
            }

            CascadeStep step = orchestrator.recordAttemptResult(run, attempt, result, now);
            ledger.save(run);
            eventHub.publishUpdate(run);

            log.info("Simulation step for provider '{}': result={}, step={}",
                    attempt.getProviderId(), result.outcome(), step);

            if (step != CascadeStep.CONTINUE) {
                break;
            }
        }

        return ledger.save(run);
    }

    private Map<String, Object> loadFixture(String classpathResource) {
        try (InputStream in = RunSimulator.class.getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("Simulation fixture not found: " + classpathResource);
            }
            return objectMapper.readValue(in, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse simulation fixture: " + classpathResource, e);
        }
    }
}
