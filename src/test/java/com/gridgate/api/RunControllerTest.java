package com.gridgate.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gridgate.ledger.JpaRunLedger;
import com.gridgate.ledger.RunMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RunControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
}
