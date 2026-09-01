package com.gridgate.calle;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.gridgate.calle.dto.CallTaskResponse;
import com.gridgate.calle.dto.CreateCallRequest;
import com.gridgate.calle.dto.RecipientInput;
import com.gridgate.config.CalleConfig;
import com.gridgate.config.CalleProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class CalleWireMockTest {

    private static WireMockServer wireMockServer;
    private CalleWebClient client;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        CalleProperties properties = new CalleProperties(
                "test-api-key-12345",
                wireMockServer.baseUrl(),
                "https://gridgate.example.com/calle/webhook",
                null);

        CalleConfig config = new CalleConfig();
        WebClient webClient = config.calleApiWebClient(properties, WebClient.builder());
        client = new CalleWebClient(webClient);
    }

    @Test
    void createCallSuccess() {
        String idempotencyKey = "gridgate_test-run-1_prov-1";

        wireMockServer.stubFor(post(urlEqualTo("/v1/calls"))
                .withHeader("Authorization", equalTo("Bearer test-api-key-12345"))
                .withHeader("Idempotency-Key", equalTo(idempotencyKey))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.task", equalTo("Check outage availability")))
                .withRequestBody(matchingJsonPath("$.recipients[0].phones[0]", equalTo("+14155550101")))
                .withRequestBody(matchingJsonPath("$.metadata.gridgate_run_id", equalTo("run-123")))
                .withRequestBody(matchingJsonPath("$.metadata.stage", equalTo("6")))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "call_task_abc123",
                                  "status": "queued",
                                  "task_completed": false,
                                  "completion_confidence": {
                                    "score": 0.0,
                                    "label": "unfulfilled"
                                  },
                                  "recipients": [
                                    {
                                      "structured_result": null,
                                      "attempts": []
                                    }
                                  ]
                                }
                                """)));

        CreateCallRequest request = new CreateCallRequest(
                "Check outage availability",
                List.of(new RecipientInput(List.of("+14155550101"), "ZA", "en-ZA")),
                null,
                RecipientResultSchemas.recipientResultSchema(),
                Map.of(
                        CalleMetadata.KEY_RUN_ID, "run-123",
                        CalleMetadata.KEY_PROVIDER_ID, "p1",
                        CalleMetadata.KEY_STAGE, "6",
                        CalleMetadata.KEY_AREA, "Sandton"),
                "https://gridgate.example.com/calle/webhook");

        CallTaskResponse response = client.createCall(request, idempotencyKey);

        assertNotNull(response);
        assertEquals("call_task_abc123", response.id());
        assertEquals("queued", response.status());
        assertNotNull(response.completionConfidence());
        assertEquals("unfulfilled", response.completionConfidence().label());
        assertEquals(0.0, response.completionConfidence().score());
        assertEquals(1, response.recipients().size());

        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/calls"))
                .withHeader("Idempotency-Key", equalTo(idempotencyKey))
                .withHeader("Authorization", equalTo("Bearer test-api-key-12345")));
    }

    @Test
    void createCallThrowsCalleApiExceptionOnHttpError() {
        wireMockServer.stubFor(post(urlEqualTo("/v1/calls"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "error": "Invalid phone number format"
                                }
                                """)));

        CreateCallRequest request = new CreateCallRequest(
                "Task",
                List.of(new RecipientInput(List.of("invalid-phone"), null, null)),
                null,
                null,
                null,
                null);

        CalleApiException ex = assertThrows(
                CalleApiException.class,
                () -> client.createCall(request, "idemp-key-1"));

        assertEquals(400, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Invalid phone number format"));
    }

    @Test
    void getCallSuccess() {
        String callId = "call_task_xyz789";

        wireMockServer.stubFor(get(urlEqualTo("/v1/calls/" + callId))
                .withHeader("Authorization", equalTo("Bearer test-api-key-12345"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "call_task_xyz789",
                                  "status": "completed",
                                  "task_completed": true,
                                  "completion_confidence": {
                                    "score": 0.98,
                                    "label": "fulfilled"
                                  },
                                  "evidence": [
                                    "Confirmed generator running and slot open at 2pm"
                                  ],
                                  "recipients": [
                                    {
                                      "structured_result": {
                                        "disposition": "success",
                                        "operating_during_load_shedding": "yes",
                                        "can_service": "yes",
                                        "price_quoted_zar": 850,
                                        "spoken_evidence": "Yes, we are on backup generator and can assist."
                                      },
                                      "attempts": []
                                    }
                                  ]
                                }
                                """)));

        CallTaskResponse response = client.getCall(callId);

        assertNotNull(response);
        assertEquals(callId, response.id());
        assertEquals("completed", response.status());
        assertTrue(response.taskCompleted());
        assertNotNull(response.completionConfidence());
        assertEquals("fulfilled", response.completionConfidence().label());
        assertEquals(0.98, response.completionConfidence().score());
        assertEquals(1, response.evidence().size());
        assertEquals(1, response.recipients().size());
        assertEquals("success", response.recipients().get(0).structuredResult().get("disposition"));

        wireMockServer.verify(getRequestedFor(urlEqualTo("/v1/calls/" + callId))
                .withHeader("Authorization", equalTo("Bearer test-api-key-12345")));
    }

    @Test
    void getCallThrowsCalleApiExceptionOn404() {
        wireMockServer.stubFor(get(urlEqualTo("/v1/calls/non-existent-call"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "error": "Call task not found"
                                }
                                """)));

        CalleApiException ex = assertThrows(
                CalleApiException.class,
                () -> client.getCall("non-existent-call"));

        assertEquals(404, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Call task not found"));
    }
}
