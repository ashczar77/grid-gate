package com.gridgate.cascade;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gridgate.domain.Money;
import com.gridgate.domain.ProviderSpec;
import com.gridgate.domain.Run;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskPromptBuilderTest {

    private Run run;
    private ProviderSpec spec;

    @BeforeEach
    void setUp() {
        spec = new ProviderSpec("prov-1", "FastSpark Electrical", "+14155550101");
        run = Run.create(
                6,
                "Sandton",
                "Emergency generator repair",
                ZonedDateTime.parse("2026-08-25T18:00:00+02:00"),
                Money.of(1500, "ZAR"),
                true,
                List.of(spec));
    }

    @Test
    void promptIncludesAiDisclosureFirst() {
        String prompt = TaskPromptBuilder.build(run, spec);

        assertNotNull(prompt);
        assertTrue(prompt.startsWith("IMPORTANT: You are an AI assistant"),
                "Prompt must start with AI disclosure statement");
    }

    @Test
    void promptIncludesStageAreaNeedDeadlineAndBudget() {
        String prompt = TaskPromptBuilder.build(run, spec);

        assertTrue(prompt.contains("FastSpark Electrical"));
        assertTrue(prompt.contains("Sandton"));
        assertTrue(prompt.contains("Stage 6"));
        assertTrue(prompt.contains("Emergency generator repair"));
        assertTrue(prompt.contains("1500.00 ZAR") || prompt.contains("1500 ZAR"));
        assertTrue(prompt.contains("2026"));
    }

    @Test
    void promptEnforcesNoAutoBookingAndFailClosedRules() {
        String prompt = TaskPromptBuilder.build(run, spec);

        assertTrue(prompt.contains("no auto-booking"));
        assertTrue(prompt.contains("do not commit to any payment"));
        assertTrue(prompt.contains("Do not guess"));
        assertTrue(prompt.contains("record it as unknown"));
    }

    @Test
    void sanitizesControlCharactersAndNewlinesInSingleLineFields() {
        ProviderSpec messySpec = new ProviderSpec("prov-1", "FastSpark\n\rElectrical\t", "+14155550101");
        Run messyRun = Run.create(
                6,
                "Sandton\r\nArea",
                "Emergency need\u0000with null byte",
                ZonedDateTime.parse("2026-08-25T18:00:00+02:00"),
                Money.of(1500, "ZAR"),
                true,
                List.of(messySpec));

        String prompt = TaskPromptBuilder.build(messyRun, messySpec);
        assertTrue(prompt.contains("FastSpark  Electrical"));
        assertTrue(prompt.contains("Sandton  Area"));
        assertTrue(!prompt.contains("\u0000"));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> TaskPromptBuilder.build(null, spec));
        assertThrows(NullPointerException.class, () -> TaskPromptBuilder.build(run, null));
    }
}
