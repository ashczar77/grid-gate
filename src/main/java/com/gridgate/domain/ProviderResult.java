package com.gridgate.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * Structured outcome from one provider call, aligned with the recipient result schema.
 */
public record ProviderResult(
        String providerName,
        TriState canService,
        TriState operatingDuringLoadShedding,
        Money quotedPrice,
        Integer etaMinutes,
        String deliveryCutoffSpoken,
        String spokenEvidence,
        CommitmentMade commitmentMade,
        Outcome outcome) {

    public ProviderResult {
        Objects.requireNonNull(providerName, "providerName");
        Objects.requireNonNull(canService, "canService");
        Objects.requireNonNull(operatingDuringLoadShedding, "operatingDuringLoadShedding");
        Objects.requireNonNull(spokenEvidence, "spokenEvidence");
        Objects.requireNonNull(commitmentMade, "commitmentMade");
        Objects.requireNonNull(outcome, "outcome");
    }

    public Optional<Money> quotedPriceOptional() {
        return Optional.ofNullable(quotedPrice);
    }

    public Optional<Integer> etaMinutesOptional() {
        return Optional.ofNullable(etaMinutes);
    }

    public Optional<String> deliveryCutoffSpokenOptional() {
        return Optional.ofNullable(deliveryCutoffSpoken);
    }
}
