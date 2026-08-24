package com.gridgate.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.gridgate.api.PhoneMasker;
import com.gridgate.domain.ProviderAttempt;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProviderAttemptResponse(
        UUID id,
        String providerId,
        String providerName,
        String phoneE164,
        String maskedPhone,
        int sequenceIndex,
        String calleCallId,
        Instant startedAt,
        Instant completedAt,
        ProviderResultResponse result) {

    public static ProviderAttemptResponse fromDomain(ProviderAttempt attempt) {
        if (attempt == null) {
            return null;
        }

        ProviderResultResponse resultResponse = attempt.getResult()
                .map(ProviderResultResponse::fromDomain)
                .orElse(null);

        return new ProviderAttemptResponse(
                attempt.getId(),
                attempt.getProviderId(),
                attempt.getProviderName(),
                attempt.getPhoneE164(),
                PhoneMasker.mask(attempt.getPhoneE164()),
                attempt.getSequenceIndex(),
                attempt.getCalleCallId().orElse(null),
                attempt.getStartedAt().orElse(null),
                attempt.getCompletedAt().orElse(null),
                resultResponse);
    }
}
