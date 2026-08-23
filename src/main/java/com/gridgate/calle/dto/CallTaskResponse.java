package com.gridgate.calle.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CallTaskResponse(
        String id,
        String status,
        Boolean taskCompleted,
        CompletionConfidence completionConfidence,
        List<String> evidence,
        Map<String, Object> structuredResult,
        List<CallRecipientResponse> recipients) {}
