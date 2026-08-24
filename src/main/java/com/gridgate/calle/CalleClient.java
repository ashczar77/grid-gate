package com.gridgate.calle;

import com.gridgate.calle.dto.CallTaskResponse;
import com.gridgate.calle.dto.CreateCallRequest;

/**
 * Client for the CALL-E Developer API.
 */
public interface CalleClient {

    /**
     * Creates a call task via {@code POST /v1/calls}.
     *
     * @param request body for the call task
     * @param idempotencyKey stable key to prevent duplicate dials on retry
     */
    CallTaskResponse createCall(CreateCallRequest request, String idempotencyKey);

    /**
     * Reads a call task via {@code GET /v1/calls/{callId}}.
     */
    CallTaskResponse getCall(String callId);
}
