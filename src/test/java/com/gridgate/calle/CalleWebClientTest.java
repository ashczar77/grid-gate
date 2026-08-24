package com.gridgate.calle;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gridgate.calle.dto.CreateCallRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class CalleWebClientTest {

    private final CalleWebClient client = new CalleWebClient(WebClient.builder().baseUrl("http://localhost").build());

    @Test
    void createCallRejectsBlankIdempotencyKey() {
        CreateCallRequest request = new CreateCallRequest(
                "task",
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                null);

        assertThrows(IllegalArgumentException.class, () -> client.createCall(request, " "));
    }

    @Test
    void getCallRejectsBlankCallId() {
        assertThrows(IllegalArgumentException.class, () -> client.getCall(" "));
    }

    @Test
    void createCallWrapsTransportErrors() {
        CreateCallRequest request = new CreateCallRequest(
                "task",
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                null);

        // Point to an unroutable port/host to trigger a transport exception
        CalleWebClient deadClient = new CalleWebClient(
                WebClient.builder().baseUrl("http://127.0.0.1:1").build());

        CalleApiException ex = assertThrows(
                CalleApiException.class,
                () -> deadClient.createCall(request, "idemp-test"));
        org.junit.jupiter.api.Assertions.assertEquals(0, ex.getStatusCode());
    }

    @Test
    void getCallWrapsTransportErrors() {
        CalleWebClient deadClient = new CalleWebClient(
                WebClient.builder().baseUrl("http://127.0.0.1:1").build());

        CalleApiException ex = assertThrows(
                CalleApiException.class,
                () -> deadClient.getCall("call-123"));
        org.junit.jupiter.api.Assertions.assertEquals(0, ex.getStatusCode());
    }
}
