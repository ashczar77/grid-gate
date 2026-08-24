package com.gridgate.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.gridgate.domain.Run;
import com.gridgate.domain.RunStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RunResponse(
        UUID id,
        int stage,
        String area,
        String need,
        ZonedDateTime deadline,
        BigDecimal budgetAmount,
        String budgetCurrency,
        boolean dryRun,
        RunStatus status,
        int nextProviderIndex,
        String winnerProviderId,
        List<ProviderSpecResponse> providers,
        List<ProviderAttemptResponse> attempts,
        Instant createdAt,
        Instant updatedAt) {

    public static RunResponse fromDomain(Run run) {
        List<ProviderSpecResponse> providerResponses = run.getProviders().stream()
                .map(ProviderSpecResponse::fromDomain)
                .toList();

        List<ProviderAttemptResponse> attemptResponses = run.getAttempts().stream()
                .map(ProviderAttemptResponse::fromDomain)
                .toList();

        return new RunResponse(
                run.getId(),
                run.getStage(),
                run.getArea(),
                run.getNeed(),
                run.getDeadline(),
                run.getBudget().amount(),
                run.getBudget().currencyCode(),
                run.isDryRun(),
                run.getStatus(),
                run.getNextProviderIndex(),
                run.getWinnerProviderId().orElse(null),
                providerResponses,
                attemptResponses,
                run.getCreatedAt(),
                run.getUpdatedAt());
    }
}
