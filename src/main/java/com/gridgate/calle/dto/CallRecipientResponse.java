package com.gridgate.calle.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CallRecipientResponse(
        Map<String, Object> structuredResult, List<CallAttemptResponse> attempts) {}
