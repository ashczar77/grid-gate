package com.gridgate.api;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gridgate.calle.CalleClient;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "calle.webhook-secret=secret_xyz")
class WebhookSecretAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalleClient calleClient;

    @Test
    void rejectsWebhookWhenSecretMissing() throws Exception {
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Test
    void rejectsWebhookWhenSecretInvalid() throws Exception {
        String eventId = "evt-" + UUID.randomUUID();
        String payload = """
                {
                  "call_id": "call-123",
                  "status": "completed"
                }
                """;

        mockMvc.perform(post("/calle/webhook")
                        .header("CALL-E-Event-Id", eventId)
                        .header("X-CALL-E-Signature", "wrong_secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Test
    void acceptsWebhookWhenSecretValid() throws Exception {
        String eventId = "evt-" + UUID.randomUUID();
        String payload = """
                {
                  "call_id": "call-123",
                  "status": "completed"
                }
                """;

        // With valid secret, it passes auth check (fails at metadata check, proving auth passed)
        mockMvc.perform(post("/calle/webhook")
                        .header("CALL-E-Event-Id", eventId)
                        .header("X-CALL-E-Signature", "secret_xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("invalid_metadata")));
    }
}
