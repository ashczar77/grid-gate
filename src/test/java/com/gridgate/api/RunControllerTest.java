package com.gridgate.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class RunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Mocked so tests never attempt a live HTTP call to CALL-E. */
    @MockitoBean
    private CalleClient calleClient;

    @Test
    void createRunDefaultsToDryRunTrueAndPlanReady() throws Exception {
        String payload = """
                {
                  "stage": 6,
                  "area": "Sandton",
                  "need": "Find plumber with backup power",
                  "deadline": "2026-08-24T18:00:00+02:00",
                  "budget_amount": 1000.00,
                  "budget_currency": "ZAR",
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

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/runs/")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.stage", is(6)))
                .andExpect(jsonPath("$.area", is("Sandton")))
                .andExpect(jsonPath("$.need", is("Find plumber with backup power")))
                .andExpect(jsonPath("$.dry_run", is(true)))
                .andExpect(jsonPath("$.status", is("PLAN_READY")))
                .andExpect(jsonPath("$.budget_amount", is(1000.0)))
                .andExpect(jsonPath("$.budget_currency", is("ZAR")))
                .andExpect(jsonPath("$.providers", hasSize(2)))
                .andExpect(jsonPath("$.providers[0].name", is("Fast Plumb")))
                .andExpect(jsonPath("$.providers[0].masked_phone", is("+1415****101")));
    }

    @Test
    void createRunExplicitLiveDryRunFalseSetsPendingStatus() throws Exception {
        String payload = """
                {
                  "stage": 4,
                  "area": "Rosebank",
                  "need": "Generator diesel refill",
                  "deadline": "2026-08-24T20:00:00+02:00",
                  "budget_amount": 2000.00,
                  "budget_currency": "ZAR",
                  "dry_run": false,
                  "providers": [
                    {
                      "id": "p1",
                      "name": "Emergency Diesel Direct",
                      "phone_e164": "+14155550103"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.dry_run", is(false)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.providers", hasSize(1)));
    }

    @Test
    void createRunRejectsInvalidInput() throws Exception {
        String invalidPayload = """
                {
                  "stage": -1,
                  "area": "",
                  "need": "",
                  "budget_amount": -50.00,
                  "budget_currency": "INVALID",
                  "providers": []
                }
                """;

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRunReturnsRunDetailsWithMaskedPhones() throws Exception {
        String payload = """
                {
                  "stage": 2,
                  "area": "Fourways",
                  "need": "Electrician for inverter switchover",
                  "deadline": "2026-08-24T19:00:00+02:00",
                  "budget_amount": 1800.00,
                  "budget_currency": "ZAR",
                  "dry_run": true,
                  "providers": [
                    {
                      "id": "p1",
                      "name": "Sparky Pro",
                      "phone_e164": "+14155550104"
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
                createResult.getResponse().getContentAsString(),
                RunResponse.class);

        mockMvc.perform(get("/api/runs/" + created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.id().toString())))
                .andExpect(jsonPath("$.stage", is(2)))
                .andExpect(jsonPath("$.area", is("Fourways")))
                .andExpect(jsonPath("$.status", is("PLAN_READY")))
                .andExpect(jsonPath("$.providers", hasSize(1)))
                .andExpect(jsonPath("$.providers[0].name", is("Sparky Pro")))
                .andExpect(jsonPath("$.providers[0].masked_phone", is("+1415****104")));
    }

    @Test
    void getRunReturns404ForUnknownId() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/runs/" + unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listRunsReturnsAllCreatedRuns() throws Exception {
        mockMvc.perform(get("/api/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    void armLiveTransitionsPlanReadyRunToRunning() throws Exception {
        // Arrange: stub CalleClient to return a fake call ID
        String fakeCallId = "calle-call-" + UUID.randomUUID();
        when(calleClient.createCall(any(), any()))
                .thenReturn(new CallTaskResponse(fakeCallId, "queued", null, null, null, null, null));

        // Create a dry-run (PLAN_READY) run
        String payload = """
                {
                  "stage": 3,
                  "area": "Midrand",
                  "need": "Emergency electrician during Stage 3",
                  "deadline": "2026-08-25T08:00:00+02:00",
                  "budget_amount": 2500.00,
                  "budget_currency": "ZAR",
                  "dry_run": true,
                  "providers": [
                    {
                      "id": "elec1",
                      "name": "GridSafe Electrical",
                      "phone_e164": "+14155550105"
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

        // Act: arm the run live
        mockMvc.perform(post("/api/runs/" + created.id() + "/live"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id", is(created.id().toString())))
                .andExpect(jsonPath("$.status", is("RUNNING")))
                .andExpect(jsonPath("$.attempts", hasSize(1)));
    }

    @Test
    void armLiveReturns409WhenRunIsAlreadyRunning() throws Exception {
        // Arrange: stub CalleClient
        String fakeCallId = "calle-call-" + UUID.randomUUID();
        when(calleClient.createCall(any(), any()))
                .thenReturn(new CallTaskResponse(fakeCallId, "queued", null, null, null, null, null));

        // Create a dry-run run and arm it once
        String payload = """
                {
                  "stage": 1,
                  "area": "Bryanston",
                  "need": "Generator hire",
                  "deadline": "2026-08-25T10:00:00+02:00",
                  "budget_amount": 5000.00,
                  "budget_currency": "ZAR",
                  "dry_run": true,
                  "providers": [
                    {
                      "id": "gen1",
                      "name": "Power4You",
                      "phone_e164": "+14155550106"
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

        // First arm — succeeds
        mockMvc.perform(post("/api/runs/" + created.id() + "/live"))
                .andExpect(status().isAccepted());

        // Second arm — conflict: run is now RUNNING, not PLAN_READY
        mockMvc.perform(post("/api/runs/" + created.id() + "/live"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("run_not_ready")))
                .andExpect(jsonPath("$.current_status", is("RUNNING")));
    }

    @Test
    void armLiveReturns404ForUnknownId() throws Exception {
        mockMvc.perform(post("/api/runs/" + UUID.randomUUID() + "/live"))
                .andExpect(status().isNotFound());
    }
}
