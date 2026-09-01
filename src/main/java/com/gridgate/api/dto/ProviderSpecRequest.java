package com.gridgate.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProviderSpecRequest(
        @NotBlank(message = "id is required") @Size(max = 64, message = "id must not exceed 64 characters") String id,
        @NotBlank(message = "name is required") @Size(max = 255, message = "name must not exceed 255 characters") String name,
        @NotBlank(message = "phone_e164 is required")
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "phone_e164 must be a valid E.164 phone number starting with + followed by 7-15 digits")
        String phoneE164) {}
