package com.gridgate.calle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gridgate.domain.Money;
import com.gridgate.domain.ProviderAttempt;
import com.gridgate.domain.ProviderSpec;
import com.gridgate.domain.Run;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalleMetadataTest {

    @Test
    void buildsMetadataFromRunAndAttempt() {
        Run run = Run.create(
                6,
                "Sandton",
                "Fix borehole pump during outage",
                ZonedDateTime.now().plusHours(2),
                Money.of(1200, "ZAR"),
                true,
                List.of(new ProviderSpec("p1", "Fast Plumb", "+14155550101")));
        ProviderAttempt attempt = run.startNextAttempt(Instant.now());

        CalleMetadata metadata = CalleMetadata.forAttempt(run, attempt);

        assertEquals(run.getId(), metadata.runId());
        assertEquals("p1", metadata.providerId());
        assertEquals(6, metadata.stage());
        assertEquals("Sandton", metadata.area());

        Map<String, String> map = metadata.toMap();
        assertEquals(run.getId().toString(), map.get("gridgate_run_id"));
        assertEquals("p1", map.get("provider_id"));
        assertEquals("6", map.get("stage"));
        assertEquals("Sandton", map.get("area"));
    }

    @Test
    void roundTripMapSerializationAndParsing() {
        UUID runId = UUID.randomUUID();
        CalleMetadata original = new CalleMetadata(runId, "prov_emergency_01", 4, "Bryanston");

        Map<String, String> map = original.toMap();
        Optional<CalleMetadata> parsed = CalleMetadata.fromMap(map);

        assertTrue(parsed.isPresent());
        assertEquals(original, parsed.get());
    }

    @Test
    void rejectsInvalidConstruction() {
        UUID runId = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> new CalleMetadata(null, "p1", 2, "Sandton"));
        assertThrows(NullPointerException.class, () -> new CalleMetadata(runId, null, 2, "Sandton"));
        assertThrows(IllegalArgumentException.class, () -> new CalleMetadata(runId, " ", 2, "Sandton"));
        assertThrows(IllegalArgumentException.class, () -> new CalleMetadata(runId, "p1", -1, "Sandton"));
        assertThrows(NullPointerException.class, () -> new CalleMetadata(runId, "p1", 2, null));
        assertThrows(IllegalArgumentException.class, () -> new CalleMetadata(runId, "p1", 2, " "));
    }

    @Test
    void parsesGracefullyOnMissingOrMalformedKeys() {
        assertFalse(CalleMetadata.fromMap(null).isPresent());
        assertFalse(CalleMetadata.fromMap(Map.of()).isPresent());

        // Missing gridgate_run_id
        assertFalse(CalleMetadata.fromMap(Map.of(
                "provider_id", "p1",
                "stage", "6",
                "area", "Sandton"
        )).isPresent());

        // Invalid UUID
        assertFalse(CalleMetadata.fromMap(Map.of(
                "gridgate_run_id", "not-a-uuid",
                "provider_id", "p1",
                "stage", "6",
                "area", "Sandton"
        )).isPresent());

        // Non-numeric stage
        assertFalse(CalleMetadata.fromMap(Map.of(
                "gridgate_run_id", UUID.randomUUID().toString(),
                "provider_id", "p1",
                "stage", "stage_six",
                "area", "Sandton"
        )).isPresent());

        // Negative stage
        assertFalse(CalleMetadata.fromMap(Map.of(
                "gridgate_run_id", UUID.randomUUID().toString(),
                "provider_id", "p1",
                "stage", "-2",
                "area", "Sandton"
        )).isPresent());
    }
}
