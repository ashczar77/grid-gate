package com.gridgate.api;

import com.gridgate.api.dto.RunResponse;
import com.gridgate.domain.Run;
import com.gridgate.domain.RunStatus;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * In-memory Server-Sent Events (SSE) hub for streaming real-time run progress to web clients.
 */
@Component
public class RunEventHub {

    private static final Logger log = LoggerFactory.getLogger(RunEventHub.class);
    private static final long DEFAULT_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    private final Map<UUID, List<SseEmitter>> subscriptions = new ConcurrentHashMap<>();

    /**
     * Subscribes an SSE client to updates for a specific run.
     * Emits the current state immediately and completes if the run is already terminal.
     */
    public SseEmitter subscribe(UUID runId, Run currentRun) {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(currentRun, "currentRun");

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        List<SseEmitter> runEmitters = subscriptions.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>());
        runEmitters.add(emitter);

        Runnable cleanup = () -> {
            runEmitters.remove(emitter);
            if (runEmitters.isEmpty()) {
                subscriptions.remove(runId, runEmitters);
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // Send initial state
        try {
            emitter.send(SseEmitter.event()
                    .name("run_update")
                    .data(RunResponse.fromDomain(currentRun), MediaType.APPLICATION_JSON));
            if (isTerminal(currentRun.getStatus())) {
                emitter.complete();
            }
        } catch (IOException e) {
            log.debug("Failed to send initial SSE event for run {}: {}", runId, e.getMessage());
            cleanup.run();
        }

        return emitter;
    }

    /**
     * Broadcasts the latest run state to all connected SSE clients for this run.
     * Completes and cleans up emitters if the run has reached a terminal status.
     */
    public void publishUpdate(Run run) {
        Objects.requireNonNull(run, "run");

        List<SseEmitter> runEmitters = subscriptions.get(run.getId());
        if (runEmitters == null || runEmitters.isEmpty()) {
            return;
        }

        RunResponse response = RunResponse.fromDomain(run);
        boolean terminal = isTerminal(run.getStatus());

        for (SseEmitter emitter : runEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("run_update")
                        .data(response, MediaType.APPLICATION_JSON));
                if (terminal) {
                    emitter.complete();
                }
            } catch (Exception e) {
                log.debug("Failed to send SSE event for run {}: {}", run.getId(), e.getMessage());
                emitter.completeWithError(e);
            }
        }

        if (terminal) {
            subscriptions.remove(run.getId());
        }
    }

    private static boolean isTerminal(RunStatus status) {
        return status == RunStatus.FULFILLED
                || status == RunStatus.EXHAUSTED
                || status == RunStatus.HALTED_AMBIGUOUS
                || status == RunStatus.CANCELLED;
    }
}
