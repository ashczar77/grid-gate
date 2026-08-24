package com.gridgate.calle;

import com.gridgate.calle.dto.CallTaskResponse;
import com.gridgate.calle.dto.CreateCallRequest;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * WebClient implementation of the CALL-E Developer API.
 */
@Component
public class CalleWebClient implements CalleClient {

    private final WebClient webClient;

    public CalleWebClient(@Qualifier("calleApiWebClient") WebClient calleApiWebClient) {
        this.webClient = Objects.requireNonNull(calleApiWebClient, "calleApiWebClient");
    }

    @Override
    public CallTaskResponse createCall(CreateCallRequest request, String idempotencyKey) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }

        try {
            return webClient
                    .post()
                    .uri("/v1/calls")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CallTaskResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new CalleApiException(
                    ex.getStatusCode().value(),
                    "CALL-E createCall failed: " + ex.getStatusCode().value() + " " + ex.getResponseBodyAsString(),
                    ex);
        } catch (WebClientRequestException ex) {
            throw new CalleApiException(
                    0,
                    "CALL-E createCall transport error: " + ex.getMessage(),
                    ex);
        } catch (WebClientException ex) {
            throw new CalleApiException(
                    0,
                    "CALL-E createCall client error: " + ex.getMessage(),
                    ex);
        }
    }

    @Override
    public CallTaskResponse getCall(String callId) {
        Objects.requireNonNull(callId, "callId");
        if (callId.isBlank()) {
            throw new IllegalArgumentException("callId must not be blank");
        }

        try {
            return webClient
                    .get()
                    .uri("/v1/calls/{callId}", callId)
                    .retrieve()
                    .bodyToMono(CallTaskResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new CalleApiException(
                    ex.getStatusCode().value(),
                    "CALL-E getCall failed: " + ex.getStatusCode().value() + " " + ex.getResponseBodyAsString(),
                    ex);
        } catch (WebClientRequestException ex) {
            throw new CalleApiException(
                    0,
                    "CALL-E getCall transport error: " + ex.getMessage(),
                    ex);
        } catch (WebClientException ex) {
            throw new CalleApiException(
                    0,
                    "CALL-E getCall client error: " + ex.getMessage(),
                    ex);
        }
    }
}
