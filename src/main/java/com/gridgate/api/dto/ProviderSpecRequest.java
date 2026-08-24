package com.gridgate.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProviderSpecRequest(
        @NotBlank(message = "id is required") String id,
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "phone_e164 is required") String phoneE164) {}
