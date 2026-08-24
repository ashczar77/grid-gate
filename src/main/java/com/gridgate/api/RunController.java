package com.gridgate.api;

import com.gridgate.api.dto.CreateRunRequest;
import com.gridgate.api.dto.RunResponse;
import com.gridgate.domain.Money;
import com.gridgate.domain.ProviderSpec;
import com.gridgate.domain.Run;
import com.gridgate.ledger.RunLedger;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final RunLedger ledger;
    private final boolean defaultDryRun;

    public RunController(
            RunLedger ledger,
            @Value("${gridgate.dry-run-default:true}") boolean defaultDryRun) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
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
}
