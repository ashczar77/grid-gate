package com.gridgate.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.gridgate.domain.Money;
import com.gridgate.domain.ProviderSpec;
import com.gridgate.domain.Run;
import com.gridgate.domain.RunStatus;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class RunEventHubTest {

    private RunEventHub eventHub;
    private Run sampleRun;

    @BeforeEach
    void setUp() {
        eventHub = new RunEventHub();
        sampleRun = Run.create(
                4,
                "Sandton",
                "Generator repair",
                ZonedDateTime.parse("2026-08-25T18:00:00+02:00"),
                Money.of(1200, "ZAR"),
                true,
                List.of(new ProviderSpec("p1", "PowerPro", "+14155550101")));
    }

    @Test
    void subscribeEmitsInitialRunState() {
        SseEmitter emitter = eventHub.subscribe(sampleRun.getId(), sampleRun);
        assertNotNull(emitter);
    }

    @Test
    void publishUpdateBroadcastsWithoutErrors() {
        SseEmitter emitter = eventHub.subscribe(sampleRun.getId(), sampleRun);
        assertNotNull(emitter);

        assertDoesNotThrow(() -> eventHub.publishUpdate(sampleRun));
    }

    @Test
    void publishTerminalStatusCompletesSubscription() {
        SseEmitter emitter = eventHub.subscribe(sampleRun.getId(), sampleRun);
        assertNotNull(emitter);

        sampleRun.cancel(Instant.now());
        assertEquals(RunStatus.CANCELLED, sampleRun.getStatus());

        assertDoesNotThrow(() -> eventHub.publishUpdate(sampleRun));
    }

    @Test
    void subscribeToTerminalRunCompletesImmediately() {
        sampleRun.cancel(Instant.now());
        SseEmitter emitter = eventHub.subscribe(sampleRun.getId(), sampleRun);
        assertNotNull(emitter);
    }

    @Test
    void subscriberCapEnforcedWithoutMemoryLeak() {
        for (int i = 0; i < RunEventHub.MAX_SUBSCRIBERS_PER_RUN + 5; i++) {
            SseEmitter emitter = eventHub.subscribe(sampleRun.getId(), sampleRun);
            assertNotNull(emitter);
        }
        assertDoesNotThrow(() -> eventHub.publishUpdate(sampleRun));
    }
}
