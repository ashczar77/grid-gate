package com.gridgate.calle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gridgate.cascade.SuccessCriteria;
import com.gridgate.domain.Money;
import com.gridgate.domain.Outcome;
import com.gridgate.domain.ProviderResult;
import com.gridgate.domain.TriState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecipientResultSchemaTest {

    @Test
    void loadsRecipientResultSchemaFromClasspath() {
        Map<String, Object> schema = RecipientResultSchemas.recipientResultSchema();

        assertEquals("object", schema.get("type"));
        assertEquals(Boolean.FALSE, schema.get("additionalProperties"));

        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertTrue(required.contains("provider_name"));
        assertTrue(required.contains("can_service"));
        assertTrue(required.contains("operating_during_load_shedding"));
        assertTrue(required.contains("spoken_evidence"));
        assertTrue(required.contains("commitment_made"));
        assertTrue(required.contains("disposition"));
    }

    @Test
    void mapsStructuredResultIntoDomainProviderResult() {
        ProviderResult result = RecipientResultMapper.fromStructuredResult(Map.of(
                "provider_name", "Provider C",
                "can_service", "yes",
                "operating_during_load_shedding", "yes",
                "price_amount", 750,
                "currency", "ZAR",
                "eta_minutes", 45,
                "delivery_cutoff_spoken", "Before 18:00",
                "spoken_evidence", "Yes, we can help during Stage 6.",
                "commitment_made", "none",
                "disposition", "success"));

        assertEquals("Provider C", result.providerName());
        assertEquals(TriState.YES, result.canService());
        assertEquals(Money.of(750, "ZAR"), result.quotedPrice());
        assertEquals(Outcome.SUCCESS, result.outcome());
        assertTrue(SuccessCriteria.isHardSuccess(result, Money.of(800, "ZAR")));
    }

    @Test
    void mapsDispositionFieldToDomainOutcome() {
        ProviderResult result = RecipientResultMapper.fromStructuredResult(minimalResult(Map.of(
                "disposition", "voicemail",
                "spoken_evidence", "No answer.")));

        assertEquals(Outcome.VOICEMAIL, result.outcome());
        assertNotNull(result);
    }

    private static Map<String, Object> minimalResult(Map<String, Object> overrides) {
        Map<String, Object> base = Map.of(
                "provider_name", "Provider A",
                "can_service", "no",
                "operating_during_load_shedding", "unknown",
                "spoken_evidence", "No.",
                "commitment_made", "none",
                "disposition", "rejected");
        java.util.HashMap<String, Object> merged = new java.util.HashMap<>(base);
        merged.putAll(overrides);
        return merged;
    }
}
