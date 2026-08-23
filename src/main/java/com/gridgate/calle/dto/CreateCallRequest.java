package com.gridgate.calle.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateCallRequest(
        String task,
        List<RecipientInput> recipients,
        Map<String, Object> resultSchema,
        Map<String, Object> recipientResultSchema,
        Map<String, String> metadata,
        String webhookUrl) {}
