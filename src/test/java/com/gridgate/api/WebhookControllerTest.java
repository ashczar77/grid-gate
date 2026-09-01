package com.gridgate.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridgate.api.dto.RunResponse;
import com.gridgate.calle.CalleClient;
import com.gridgate.calle.dto.CallTaskResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CalleClient calleClient;

    @Test
    void rejectsWebhookWithoutEventIdHeader() throws Exception {
        String payload = """
                {
                  "call_id": "call-123",
                  "status": "completed"
                }
                """;

        mockMvc.perform(post("/calle/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("missing_event_id")));
    }

    @Test
    void rejectsWebhookWithMissingMetadata() throws Exception {
        String eventId = "evt-" + UUID.randomUUID();
        String payload = """
                {
                  "call_id": "call-123",
                  "status": "completed"
                }
                """;

        mockMvc.perform(post("/calle/webhook")
                        .header("CALL-E-Event-Id", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("invalid_metadata")));
    }

    @Test
    void hardSuccessAdvancesCascadeToFulfilled() throws Exception {
        when(calleClient.createCall(any(), any()))
                .thenReturn(new CallTaskResponse("call-1", "queued", null, null, null, null, null));

        RunResponse run = createAndArmRun();

        String eventId = "evt-" + UUID.randomUUID();
        String webhookPayload = """
                {
                  "call_id": "call-1",
                  "status": "completed",
                  "structured_result": {
                    "provider_name": "Fast Plumb",
                    "can_service": "yes",
                    "operating_during_load_shedding": "yes",
                    "price_amount": 750.00,
                    "currency": "ZAR",
                    "eta_minutes": 45,
                    "delivery_cutoff_spoken": "Before 18:00",
                    "spoken_evidence": "Yes, we have backup power and can arrive in 45 minutes.",
                    "commitment_made": "none",
                    "disposition": "success"
                  },
                  "metadata": {
                    "gridgate_run_id": "%s",
                    "provider_id": "p1",
                    "stage": "6",
                    "area": "Sandton"
                  }
                }
                """.formatted(run.id());

        mockMvc.perform(post("/calle/webhook")
                        .header("CALL-E-Event-Id", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("processed")))
                .andExpect(jsonPath("$.step", is("FULFILLED")))
                .andExpect(jsonPath("$.run_status", is("FULFILLED")));

        // Verify run details in ledger
        mockMvc.perform(get("/api/runs/" + run.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FULFILLED")))
                .andExpect(jsonPath("$.winner_provider_id", is("p1")))
                .andExpect(jsonPath("$.attempts", hasSize(1)))
                .andExpect(jsonPath("$.attempts[0].result.outcome", is("SUCCESS")))
                .andExpect(jsonPath("$.attempts[0].result.quoted_price_amount", is(750.0)));

        // CalleClient was only called once (for p1), never for p2
        verify(calleClient, times(1)).createCall(any(), any());
    }

    @Test
    void failureAdvancesCascadeToNextProvider() throws Exception {
        when(calleClient.createCall(any(), any()))
                .thenReturn(new CallTaskResponse("call-1", "queued", null, null, null, null, null))
                .thenReturn(new CallTaskResponse("call-2", "queued", null, null, null, null, null));

        RunResponse run = createAndArmRun();

        String eventId1 = "evt-" + UUID.randomUUID();
        String p1FailurePayload = """
                {
                  "call_id": "call-1",
                  "status": "completed",
                  "structured_result": {
                    "provider_name": "Fast Plumb",
                    "can_service": "no",
                    "operating_during_load_shedding": "no",
                    "spoken_evidence": "Sorry, our equipment is down during Stage 6.",
                    "commitment_made": "none",
                    "disposition": "rejected"
                  },
                  "metadata": {
                    "gridgate_run_id": "%s",
                    "provider_id": "p1",
                    "stage": "6",
                    "area": "Sandton"
                  }
                }
                """.formatted(run.id());

        mockMvc.perform(post("/calle/webhook")
                        .header("CALL-E-Event-Id", eventId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(p1FailurePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("processed")))
                .andExpect(jsonPath("$.step", is("CONTINUE")))
                .andExpect(jsonPath("$.run_status", is("RUNNING")));

        // Verify that second provider p2 was dialed
        verify(calleClient, times(2)).createCall(any(), any());

        // Check run state: now has 2 attempts
        mockMvc.perform(get("/api/runs/" + run.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RUNNING")))
                .andExpect(jsonPath("$.attempts", hasSize(2)));
    }

    @Test
    void duplicateEventIsDeduplicatedWithoutSideEffects() throws Exception {
        when(calleClient.createCall(any(), any()))
                .thenReturn(new CallTaskResponse("call-1", "queued", null, null, null, null, null));

        RunResponse run = createAndArmRun();

        String eventId = "evt-" + UUID.randomUUID();
        String webhookPayload = """
                {
                  "call_id": "call-1",
                  "status": "completed",
                  "structured_result": {
                    "provider_name": "Fast Plumb",
                    "can_service": "yes",
                    "operating_during_load_shedding": "yes",
                    "price_amount": 600.00,
                    "currency": "ZAR",
                    "spoken_evidence": "Yes, available.",
                    "commitment_made": "none",
                    "disposition": "success"
                  },
                  "metadata": {
                    "gridgate_run_id": "%s",
                    "provider_id": "p1",
                    "stage": "6",
                    "area": "Sandton"
                  }
                }
                """.formatted(run.id());

        // First delivery
        mockMvc.perform(post("/calle/webhook")
                        .header("CALL-E-Event-Id", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("processed")));

        // Duplicate delivery with same eventId
        mockMvc.perform(post("/calle/webhook")
                        .header("CALL-E-Event-Id", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("duplicate_ignored")))
                .andExpect(jsonPath("$.event_id", is(eventId)));
    }

    private RunResponse createAndArmRun() throws Exception {
        String payload = """
                {
                  "stage": 6,
                  "area": "Sandton",
                  "need": "Emergency plumber",
                  "deadline": "2026-08-25T18:00:00+02:00",
                  "budget_amount": 1000.00,
                  "budget_currency": "ZAR",
                  "dry_run": true,
                  "providers": [
                    {
                      "id": "p1",
                      "name": "Fast Plumb",
                      "phone_e164": "+14155550101"
                    },
                    {
                      "id": "p2",
                      "name": "24/7 Power Plumbers",
                      "phone_e164": "+14155550102"
                    }
                  ]
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        RunResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), RunResponse.class);

        mockMvc.perform(post("/api/runs/" + created.id() + "/live"))
                .andExpect(status().isAccepted());

        return created;
    }
}
