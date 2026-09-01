package com.gridgate.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateRunRequest(
        @Min(value = 0, message = "stage must be >= 0") int stage,
        @NotBlank(message = "area is required") @Size(max = 255, message = "area must not exceed 255 characters") String area,
        @NotBlank(message = "need is required") @Size(max = 1000, message = "need must not exceed 1000 characters") String need,
        @NotNull(message = "deadline is required") ZonedDateTime deadline,
        @NotNull(message = "budget_amount is required") @DecimalMin(value = "0.0", inclusive = true, message = "budget_amount must be >= 0") BigDecimal budgetAmount,
        @NotBlank(message = "budget_currency is required") @Pattern(regexp = "^[A-Za-z]{3}$", message = "budget_currency must be a 3-letter ISO code") String budgetCurrency,
        Boolean dryRun,
        @NotEmpty(message = "providers must not be empty") @Valid List<ProviderSpecRequest> providers) {

    public boolean isDryRunOrDefault(boolean defaultValue) {
        return dryRun != null ? dryRun : defaultValue;
    }
}
