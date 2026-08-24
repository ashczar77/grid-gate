package com.gridgate.api;

import com.gridgate.api.RunDialService.RunNotFoundException;
import com.gridgate.api.RunDialService.RunNotReadyException;
import com.gridgate.api.dto.CreateRunRequest;
import com.gridgate.api.dto.RunResponse;
import com.gridgate.domain.Money;
import com.gridgate.domain.ProviderSpec;
import com.gridgate.domain.Run;
import com.gridgate.ledger.RunLedger;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final RunLedger ledger;
    private final RunDialService dialService;
    private final boolean defaultDryRun;

    public RunController(
            RunLedger ledger,
            RunDialService dialService,
            @Value("${gridgate.dry-run-default:true}") boolean defaultDryRun) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.dialService = Objects.requireNonNull(dialService, "dialService");
        this.defaultDryRun = defaultDryRun;
    }

    @PostMapping
    public ResponseEntity<RunResponse> createRun(@Valid @RequestBody CreateRunRequest request) {
        boolean dryRun = request.isDryRunOrDefault(defaultDryRun);

        List<ProviderSpec> providerSpecs = request.providers().stream()
                .map(p -> new ProviderSpec(p.id(), p.name(), p.phoneE164()))
                .toList();

        Money budget = Money.of(request.budgetAmount(), request.budgetCurrency());

        Run run = Run.create(
                request.stage(),
                request.area(),
                request.need(),
                request.deadline(),
                budget,
                dryRun,
                providerSpecs);

        Run saved = ledger.save(run);

        return ResponseEntity
                .created(URI.create("/api/runs/" + saved.getId()))
                .body(RunResponse.fromDomain(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RunResponse> getRun(@PathVariable UUID id) {
        return ledger.findById(id)
                .map(run -> ResponseEntity.ok(RunResponse.fromDomain(run)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<RunResponse>> listRuns() {
        List<RunResponse> runs = ledger.findAll().stream()
                .map(RunResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(runs);
    }

    /**
     * Arms the run: transitions it from {@code PLAN_READY} to {@code RUNNING}
     * by firing the first CALL-E provider dial.
     *
     * <p>Returns {@code 202 Accepted} immediately — the call result arrives later via webhook.
     */
    @PostMapping("/{id}/live")
    public ResponseEntity<?> armLive(@PathVariable UUID id) {
        try {
            Run run = dialService.armAndDial(id);
            return ResponseEntity.accepted().body(RunResponse.fromDomain(run));
        } catch (RunNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (RunNotReadyException ex) {
            return ResponseEntity.status(409)
                    .body(Map.of(
                            "error", "run_not_ready",
                            "message", ex.getMessage(),
                            "current_status", ex.getCurrentStatus().name()));
        }
    }
}
