package com.gridgate.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.gridgate.domain.CommitmentMade;
import com.gridgate.domain.Outcome;
import com.gridgate.domain.ProviderResult;
import com.gridgate.domain.TriState;
import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProviderResultResponse(
        String providerName,
        TriState canService,
        TriState operatingDuringLoadShedding,
        BigDecimal quotedPriceAmount,
        String quotedPriceCurrency,
        Integer etaMinutes,
        String deliveryCutoffSpoken,
        String spokenEvidence,
        CommitmentMade commitmentMade,
        Outcome outcome) {

    public static ProviderResultResponse fromDomain(ProviderResult result) {
        if (result == null) {
            return null;
        }

        BigDecimal priceAmount = result.quotedPriceOptional().map(p -> p.amount()).orElse(null);
        String priceCurrency = result.quotedPriceOptional().map(p -> p.currencyCode()).orElse(null);

        return new ProviderResultResponse(
                result.providerName(),
                result.canService(),
                result.operatingDuringLoadShedding(),
                priceAmount,
                priceCurrency,
                result.etaMinutesOptional().orElse(null),
                result.deliveryCutoffSpokenOptional().orElse(null),
                result.spokenEvidence(),
                result.commitmentMade(),
                result.outcome());
    }
}
