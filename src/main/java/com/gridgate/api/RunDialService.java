package com.gridgate.api;

import com.gridgate.calle.CalleClient;
import com.gridgate.calle.CalleMetadata;
import com.gridgate.calle.IdempotencyKeys;
import com.gridgate.calle.RecipientResultSchemas;
import com.gridgate.calle.dto.CreateCallRequest;
import com.gridgate.calle.dto.RecipientInput;
import com.gridgate.cascade.CascadeOrchestrator;
import com.gridgate.cascade.TaskPromptBuilder;
import com.gridgate.domain.ProviderAttempt;
import com.gridgate.domain.ProviderSpec;
import com.gridgate.domain.Run;
import com.gridgate.domain.RunStatus;
import com.gridgate.ledger.RunLedger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Transitions a {@code PLAN_READY} run to {@code RUNNING} by dialling the first (or next)
 * provider via CALL-E and stamping the returned call-ID onto the attempt.
 *
 * <p>This is the second gate: the first gate is {@code POST /api/runs} (dry-run planning).
 * Calling {@code POST /api/runs/{id}/live} arms the cascade and fires the first dial.
 */
@Service
public class RunDialService {

    private static final Logger log = LoggerFactory.getLogger(RunDialService.class);

    private final RunLedger ledger;
    private final CalleClient calleClient;
    private final CascadeOrchestrator orchestrator;
    private final String webhookUrl;

    public RunDialService(
            RunLedger ledger,
            CalleClient calleClient,
            CascadeOrchestrator orchestrator,
            @Value("${gridgate.webhook-url:http://localhost:8080/calle/webhook}") String webhookUrl) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.calleClient = Objects.requireNonNull(calleClient, "calleClient");
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
        this.webhookUrl = Objects.requireNonNull(webhookUrl, "webhookUrl");
    }

    /**
     * Arms the run: loads it from the ledger, validates {@code PLAN_READY} status,
     * starts the first provider dial via CALL-E, and persists the updated run.
     *
     * @param runId the run to arm
     * @return the updated {@link Run} after the dial has been initiated
     * @throws RunNotFoundException    if no run exists with the given ID
     * @throws RunNotReadyException    if the run is not in {@code PLAN_READY} status
     */
    public Run armAndDial(UUID runId) {
        Objects.requireNonNull(runId, "runId");

        Run run = ledger.findById(runId)
                .orElseThrow(() -> new RunNotFoundException(runId));

        if (run.getStatus() != RunStatus.PLAN_READY) {
            throw new RunNotReadyException(runId, run.getStatus());
        }

        Instant now = Instant.now();
        run.armLive(now);
        dialNext(run, now);

        Run saved = ledger.save(run);
        log.info("Run {} transitioned to {}", runId, saved.getStatus());
        return saved;
    }

    /**
     * Dials the next provider in sequence for an active run.
     *
     * @param run the active run to advance
     * @param now current timestamp
     * @return the newly created and started {@link ProviderAttempt}, or empty if no dials started
     */
    public Optional<ProviderAttempt> dialNext(Run run, Instant now) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(now, "now");

        Optional<ProviderAttempt> attemptOpt = orchestrator.startNextDial(run, now);
        if (attemptOpt.isEmpty()) {
            return Optional.empty();
        }

        ProviderAttempt attempt = attemptOpt.get();
        ProviderSpec spec = run.getProviders().get(attempt.getSequenceIndex());
        String idempotencyKey = IdempotencyKeys.forAttempt(run.getId(), spec.id());
        String prompt = TaskPromptBuilder.build(run, spec);

        CreateCallRequest callRequest = new CreateCallRequest(
                prompt,
                List.of(new RecipientInput(List.of(spec.phoneE164()), "ZA", "en-ZA")),
                null,
                RecipientResultSchemas.recipientResultSchema(),
                CalleMetadata.forAttempt(run, attempt).toMap(),
                webhookUrl);

        log.info("Dialling provider '{}' for run {} (idempotencyKey={})",
                spec.id(), run.getId(), idempotencyKey);

        var callResponse = calleClient.createCall(callRequest, idempotencyKey);
        attempt.markStarted(callResponse.id(), now);
        return Optional.of(attempt);
    }

    public static final class RunNotFoundException extends RuntimeException {
        private final UUID runId;

        public RunNotFoundException(UUID runId) {
            super("Run not found: " + runId);
            this.runId = runId;
        }

        public UUID getRunId() {
            return runId;
        }
    }

    public static final class RunNotReadyException extends RuntimeException {
        private final UUID runId;
        private final RunStatus currentStatus;

        public RunNotReadyException(UUID runId, RunStatus currentStatus) {
            super("Run " + runId + " cannot be armed: status is " + currentStatus
                    + " (expected PLAN_READY)");
            this.runId = runId;
            this.currentStatus = currentStatus;
        }

        public UUID getRunId() {
            return runId;
        }

        public RunStatus getCurrentStatus() {
            return currentStatus;
        }
    }
}
