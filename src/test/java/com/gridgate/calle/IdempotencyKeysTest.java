package com.gridgate.calle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotencyKeysTest {

    @Test
    void buildsKeyFromUuidAndProviderId() {
        UUID runId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String providerId = "prov_fastplumb_01";

        String key = IdempotencyKeys.forAttempt(runId, providerId);

        assertEquals("gridgate_123e4567-e89b-12d3-a456-426614174000_prov_fastplumb_01", key);
    }

    @Test
    void buildsKeyFromStringIdentifiers() {
        String key = IdempotencyKeys.forAttempt("run_abc", "prov_123");
        assertEquals("gridgate_run_abc_prov_123", key);
    }

    @Test
    void rejectsNullOrBlankInputs() {
        UUID runId = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> IdempotencyKeys.forAttempt((UUID) null, "p1"));
        assertThrows(NullPointerException.class, () -> IdempotencyKeys.forAttempt(runId, null));
        assertThrows(IllegalArgumentException.class, () -> IdempotencyKeys.forAttempt(runId, " "));

        assertThrows(NullPointerException.class, () -> IdempotencyKeys.forAttempt((String) null, "p1"));
        assertThrows(NullPointerException.class, () -> IdempotencyKeys.forAttempt("r1", null));
        assertThrows(IllegalArgumentException.class, () -> IdempotencyKeys.forAttempt(" ", "p1"));
        assertThrows(IllegalArgumentException.class, () -> IdempotencyKeys.forAttempt("r1", " "));
    }
}
