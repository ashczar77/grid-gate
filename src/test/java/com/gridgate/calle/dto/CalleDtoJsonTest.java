package com.gridgate.calle.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CalleDtoJsonTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
    }

    @Test
    void serializesCreateCallRequestWithSnakeCaseKeys() throws Exception {
        CreateCallRequest request = new CreateCallRequest(
                "Call +14155550101 and ask about generator availability.",
                List.of(new RecipientInput(List.of("+14155550101"), "ZA", "en-ZA")),
                Map.of("type", "object"),
                Map.of("type", "object"),
                Map.of("gridgate_run_id", "run-123"),
                "https://example.com/calle/webhook");

        String json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"task\":"));
        assertTrue(json.contains("\"recipient_result_schema\":"));
        assertTrue(json.contains("\"webhook_url\":"));
        assertTrue(json.contains("\"gridgate_run_id\""));
    }

    @Test
    void deserializesCallTaskResponseFromSnakeCaseJson() throws Exception {
        String json =
                """
                {
                  "id": "call_123",
                  "status": "completed",
                  "task_completed": true,
                  "completion_confidence": { "score": 0.92, "label": "high" },
                  "evidence": ["The recipient said they can help."],
                  "structured_result": { "completed_count": 1 },
                  "recipients": [
                    {
                      "structured_result": { "disposition": "success", "can_service": "yes" },
                      "attempts": [
                        {
                          "transcript_turns": [
                            { "offset_seconds": 0, "speaker": "bot", "text": "Hello" },
                            { "offset_seconds": 4, "speaker": "user", "text": "Yes" }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        CallTaskResponse response = mapper.readValue(json, CallTaskResponse.class);

        assertEquals("call_123", response.id());
        assertEquals("completed", response.status());
        assertEquals(true, response.taskCompleted());
        assertEquals(0.92, response.completionConfidence().score());
        assertEquals("success", response.recipients().getFirst().structuredResult().get("disposition"));
        assertEquals("bot", response.recipients().getFirst().attempts().getFirst().transcriptTurns().getFirst().speaker());
    }

    @Test
    void deserializesWebhookEventFromSnakeCaseJson() throws Exception {
        String json =
                """
                {
                  "event_id": "evt_456",
                  "call_id": "call_123",
                  "status": "completed",
                  "task_completed": true,
                  "metadata": { "gridgate_run_id": "run-123", "provider_id": "provider-a" }
                }
                """;

        WebhookEvent event = mapper.readValue(json, WebhookEvent.class);

        assertEquals("evt_456", event.eventId());
        assertEquals("call_123", event.callId());
        assertEquals("run-123", event.metadata().get("gridgate_run_id"));
    }
}
