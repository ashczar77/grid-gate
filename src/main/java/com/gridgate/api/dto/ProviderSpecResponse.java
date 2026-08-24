package com.gridgate.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.gridgate.api.PhoneMasker;
import com.gridgate.domain.ProviderSpec;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProviderSpecResponse(
        String id,
        String name,
        String phoneE164,
        String maskedPhone) {

    public static ProviderSpecResponse fromDomain(ProviderSpec spec) {
        return new ProviderSpecResponse(
                spec.id(),
                spec.name(),
                spec.phoneE164(),
                PhoneMasker.mask(spec.phoneE164()));
    }
}
