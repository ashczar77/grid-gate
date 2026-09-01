package com.gridgate.ledger;

import com.gridgate.domain.Money;
import com.gridgate.domain.ProviderAttempt;
import com.gridgate.domain.ProviderResult;
import com.gridgate.domain.ProviderSpec;
import com.gridgate.domain.Run;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RunMapper {

    public RunEntity toEntity(Run run) {
        Objects.requireNonNull(run, "run");

        RunEntity entity = new RunEntity();
        entity.setId(run.getId());
        entity.setCreatedAt(run.getCreatedAt());

        List<ProviderSpecEntity> providerEntities = new ArrayList<>();
        List<ProviderSpec> providers = run.getProviders();
        for (int i = 0; i < providers.size(); i++) {
            ProviderSpec spec = providers.get(i);
            providerEntities.add(new ProviderSpecEntity(
                    entity,
                    spec.id(),
                    spec.name(),
                    spec.phoneE164(),
                    i));
        }
        entity.setProviders(providerEntities);

        updateEntity(entity, run);
        return entity;
    }

    public void updateEntity(RunEntity entity, Run run) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(run, "run");

        entity.setStage(run.getStage());
        entity.setArea(run.getArea());
        entity.setNeed(run.getNeed());
        entity.setDeadline(run.getDeadline());
        entity.setBudgetAmount(run.getBudget().amount());
        entity.setBudgetCurrency(run.getBudget().currencyCode());
        entity.setDryRun(run.isDryRun());
        entity.setStatus(run.getStatus());
        entity.setNextProviderIndex(run.getNextProviderIndex());
        entity.setWinnerProviderId(run.getWinnerProviderId().orElse(null));
        entity.setUpdatedAt(run.getUpdatedAt());

        // Update or append attempts
        List<ProviderAttempt> domainAttempts = run.getAttempts();
        List<ProviderAttemptEntity> existingEntities = entity.getAttempts();

        for (int i = 0; i < domainAttempts.size(); i++) {
            ProviderAttempt domainAttempt = domainAttempts.get(i);
            if (i < existingEntities.size()) {
                ProviderAttemptEntity existing = existingEntities.get(i);
                updateAttemptEntity(existing, domainAttempt);
            } else {
                existingEntities.add(toAttemptEntity(entity, domainAttempt));
            }
        }
    }

    private void updateAttemptEntity(ProviderAttemptEntity entity, ProviderAttempt attempt) {
        entity.setCalleCallId(attempt.getCalleCallId().orElse(null));
        entity.setStartedAt(attempt.getStartedAt().orElse(null));
        entity.setCompletedAt(attempt.getCompletedAt().orElse(null));

        attempt.getResult().ifPresentOrElse(res -> {
            entity.setResultProviderName(res.providerName());
            entity.setCanService(res.canService());
            entity.setOperatingDuringLoadShedding(res.operatingDuringLoadShedding());
            res.quotedPriceOptional().ifPresentOrElse(price -> {
                entity.setQuotedPriceAmount(price.amount());
                entity.setQuotedPriceCurrency(price.currencyCode());
            }, () -> {
                entity.setQuotedPriceAmount(null);
                entity.setQuotedPriceCurrency(null);
            });
            entity.setEtaMinutes(res.etaMinutesOptional().orElse(null));
            entity.setDeliveryCutoffSpoken(res.deliveryCutoffSpokenOptional().orElse(null));
            entity.setSpokenEvidence(res.spokenEvidence());
            entity.setCommitmentMade(res.commitmentMade());
            entity.setOutcome(res.outcome());
        }, () -> {
            entity.setResultProviderName(null);
            entity.setCanService(null);
            entity.setOperatingDuringLoadShedding(null);
            entity.setQuotedPriceAmount(null);
            entity.setQuotedPriceCurrency(null);
            entity.setEtaMinutes(null);
            entity.setDeliveryCutoffSpoken(null);
            entity.setSpokenEvidence(null);
            entity.setCommitmentMade(null);
            entity.setOutcome(null);
        });
    }

    public Run toDomain(RunEntity entity) {
        Objects.requireNonNull(entity, "entity");

        List<ProviderSpec> providerSpecs = entity.getProviders().stream()
                .map(p -> new ProviderSpec(p.getProviderId(), p.getName(), p.getPhoneE164()))
                .toList();

        List<ProviderAttempt> attempts = entity.getAttempts().stream()
                .map(this::toAttemptDomain)
                .toList();

        Money budget = Money.of(entity.getBudgetAmount(), entity.getBudgetCurrency());

        return Run.restore(
                entity.getId(),
                entity.getStage(),
                entity.getArea(),
                entity.getNeed(),
                entity.getDeadline(),
                budget,
                entity.isDryRun(),
                providerSpecs,
                attempts,
                entity.getStatus(),
                entity.getNextProviderIndex(),
                entity.getWinnerProviderId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private ProviderAttemptEntity toAttemptEntity(RunEntity runEntity, ProviderAttempt attempt) {
        ProviderAttemptEntity entity = new ProviderAttemptEntity(
                attempt.getId(),
                runEntity,
                attempt.getProviderId(),
                attempt.getProviderName(),
                attempt.getPhoneE164(),
                attempt.getSequenceIndex());

        entity.setCalleCallId(attempt.getCalleCallId().orElse(null));
        entity.setStartedAt(attempt.getStartedAt().orElse(null));
        entity.setCompletedAt(attempt.getCompletedAt().orElse(null));

        attempt.getResult().ifPresent(res -> {
            entity.setResultProviderName(res.providerName());
            entity.setCanService(res.canService());
            entity.setOperatingDuringLoadShedding(res.operatingDuringLoadShedding());
            res.quotedPriceOptional().ifPresent(price -> {
                entity.setQuotedPriceAmount(price.amount());
                entity.setQuotedPriceCurrency(price.currencyCode());
            });
            entity.setEtaMinutes(res.etaMinutesOptional().orElse(null));
            entity.setDeliveryCutoffSpoken(res.deliveryCutoffSpokenOptional().orElse(null));
            entity.setSpokenEvidence(res.spokenEvidence());
            entity.setCommitmentMade(res.commitmentMade());
            entity.setOutcome(res.outcome());
        });

        return entity;
    }

    private ProviderAttempt toAttemptDomain(ProviderAttemptEntity entity) {
        ProviderResult result = null;
        if (entity.getOutcome() != null && entity.getResultProviderName() != null) {
            Money quotedPrice = entity.getQuotedPriceAmount() != null && entity.getQuotedPriceCurrency() != null
                    ? Money.of(entity.getQuotedPriceAmount(), entity.getQuotedPriceCurrency())
                    : null;

            result = new ProviderResult(
                    entity.getResultProviderName(),
                    entity.getCanService(),
                    entity.getOperatingDuringLoadShedding(),
                    quotedPrice,
                    entity.getEtaMinutes(),
                    entity.getDeliveryCutoffSpoken(),
                    entity.getSpokenEvidence(),
                    entity.getCommitmentMade(),
                    entity.getOutcome());
        }

        return ProviderAttempt.restore(
                entity.getId(),
                entity.getProviderId(),
                entity.getProviderName(),
                entity.getPhoneE164(),
                entity.getSequenceIndex(),
                entity.getCalleCallId(),
                result,
                entity.getStartedAt(),
                entity.getCompletedAt());
    }
}
