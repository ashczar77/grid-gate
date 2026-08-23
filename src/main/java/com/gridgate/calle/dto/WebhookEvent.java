package com.gridgate.calle.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import java.util.Map;

/**
 * Terminal webhook payload from CALL-E after a call task completes.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WebhookEvent(
        String eventId,
        String callId,
        String status,
        Boolean taskCompleted,
        CompletionConfidence completionConfidence,
        List<String> evidence,
        Map<String, Object> structuredResult,
        List<CallRecipientResponse> recipients,
        Map<String, String> metadata) {}
