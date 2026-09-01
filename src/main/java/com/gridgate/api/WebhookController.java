package com.gridgate.api;

import com.gridgate.calle.CalleMetadata;
import com.gridgate.calle.RecipientResultMapper;
import com.gridgate.calle.dto.WebhookEvent;
import com.gridgate.cascade.CascadeOrchestrator;
import com.gridgate.cascade.CascadeStep;
import com.gridgate.domain.CommitmentMade;
import com.gridgate.domain.Outcome;
import com.gridgate.domain.ProviderAttempt;
import com.gridgate.domain.ProviderResult;
import com.gridgate.domain.Run;
import com.gridgate.domain.RunStatus;
import com.gridgate.domain.TriState;
import com.gridgate.ledger.ProcessedWebhookEventEntity;
import com.gridgate.ledger.ProcessedWebhookEventRepository;
import com.gridgate.ledger.RunLedger;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gridgate.config.CalleProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles incoming terminal webhook callbacks from CALL-E.
 * Deduplicates by CALL-E-Event-Id before advancing the cascade sequentially.
 */
@RestController
@RequestMapping("/calle/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final RunLedger ledger;
    private final RunDialService dialService;
    private final CascadeOrchestrator orchestrator;
    private final ProcessedWebhookEventRepository eventRepository;
    private final RunEventHub eventHub;
    private final CalleProperties properties;

    public WebhookController(
            RunLedger ledger,
            RunDialService dialService,
            CascadeOrchestrator orchestrator,
            ProcessedWebhookEventRepository eventRepository,
            RunEventHub eventHub,
            CalleProperties properties) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.dialService = Objects.requireNonNull(dialService, "dialService");
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
        this.eventHub = Objects.requireNonNull(eventHub, "eventHub");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> handleWebhook(
            @RequestHeader(value = "CALL-E-Event-Id", required = false) String eventIdHeader,
            @RequestHeader(value = "X-CALL-E-Signature", required = false) String signatureHeader,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secretHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody WebhookEvent event) {

        if (properties.hasWebhookSecret()) {
            String expectedSecret = properties.webhookSecret();
            boolean authorized = (secretHeader != null && secretHeader.trim().equals(expectedSecret))
                    || (signatureHeader != null && signatureHeader.trim().equals(expectedSecret))
                    || (authHeader != null && authHeader.trim().equals("Bearer " + expectedSecret));

            if (!authorized) {
                log.warn("Unauthorized webhook request rejected: missing or invalid secret/signature header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "unauthorized", "message", "Invalid or missing webhook signature"));
            }
        }

        String eventId = resolveEventId(eventIdHeader, event);
        if (eventId == null || eventId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "missing_event_id", "message", "CALL-E-Event-Id header is required"));
        }

        if (eventRepository.existsById(eventId)) {
            log.info("Duplicate webhook event ignored: {}", eventId);
            return ResponseEntity.ok(Map.of(
                    "status", "duplicate_ignored",
                    "event_id", eventId));
        }

        Optional<CalleMetadata> metadataOpt = CalleMetadata.fromMap(event.metadata());
        if (metadataOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "invalid_metadata", "message", "Missing or invalid GridGate metadata"));
        }
        CalleMetadata metadata = metadataOpt.get();

        Optional<Run> runOpt = ledger.findById(metadata.runId());
        if (runOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Run run = runOpt.get();

        Optional<ProviderAttempt> attemptOpt = findTargetAttempt(run, metadata.providerId(), event.callId());
        if (attemptOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "attempt_not_found", "message", "No matching attempt for provider " + metadata.providerId()));
        }
        ProviderAttempt attempt = attemptOpt.get();

        Instant now = Instant.now();
        ProviderResult result = mapProviderResult(event, attempt);

        if (run.getStatus() == RunStatus.CANCELLED) {
            if (!attempt.isComplete()) {
                attempt.complete(result, now);
                ledger.save(run);
                eventHub.publishUpdate(run);
            }
            eventRepository.save(new ProcessedWebhookEventEntity(eventId, now));
            return ResponseEntity.ok(Map.of(
                    "status", "processed_cancelled_run",
                    "run_id", run.getId().toString(),
                    "run_status", run.getStatus().name()));
        }

        CascadeStep step = orchestrator.recordAttemptResult(run, attempt, result, now);
        if (step == CascadeStep.CONTINUE) {
            dialService.dialNext(run, now);
        }

        ledger.save(run);
        eventHub.publishUpdate(run);

        try {
            eventRepository.save(new ProcessedWebhookEventEntity(eventId, now));
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent duplicate webhook event caught: {}", eventId);
            return ResponseEntity.ok(Map.of(
                    "status", "duplicate_ignored",
                    "event_id", eventId));
        }

        log.info("Webhook processed for run {} (step={}, status={})",
                run.getId(), step, run.getStatus());

        return ResponseEntity.ok(Map.of(
                "status", "processed",
                "run_id", run.getId().toString(),
                "step", step.name(),
                "run_status", run.getStatus().name()));
    }

    private static String resolveEventId(String header, WebhookEvent event) {
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        if (event != null && event.eventId() != null && !event.eventId().isBlank()) {
            return event.eventId().trim();
        }
        return null;
    }

    private static Optional<ProviderAttempt> findTargetAttempt(Run run, String providerId, String callId) {
        return run.getAttempts().stream()
                .filter(a -> a.getProviderId().equals(providerId))
                .reduce((first, second) -> second);
    }

    private static ProviderResult mapProviderResult(WebhookEvent event, ProviderAttempt attempt) {
        if (event.structuredResult() != null && !event.structuredResult().isEmpty()) {
            return RecipientResultMapper.fromStructuredResult(event.structuredResult());
        }

        Outcome outcome = parseFallbackOutcome(event.status());
        return new ProviderResult(
                attempt.getProviderName(),
                TriState.UNKNOWN,
                TriState.UNKNOWN,
                null,
                null,
                null,
                "Call ended with status: " + event.status(),
                CommitmentMade.NONE,
                outcome);
    }

    private static Outcome parseFallbackOutcome(String status) {
        if (status == null || status.isBlank()) {
            return Outcome.UNREACHABLE;
        }
        try {
            return Outcome.fromDisposition(status);
        } catch (IllegalArgumentException ex) {
            return Outcome.UNREACHABLE;
        }
    }
}
