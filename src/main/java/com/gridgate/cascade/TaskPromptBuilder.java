package com.gridgate.cascade;

import com.gridgate.domain.ProviderSpec;
import com.gridgate.domain.Run;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Builds the task prompt sent to CALL-E for each provider dial.
 *
 * <p>Prompt rules (locked):
 * <ol>
 *   <li>AI disclosure first: caller must identify as AI.</li>
 *   <li>State the Eskom load-shedding Stage N and suburb.</li>
 *   <li>State the service need and deadline clearly.</li>
 *   <li>Quote the budget ceiling; never discuss auto-booking or payment.</li>
 *   <li>Ask only: "Can you service during load-shedding? What is your price?"</li>
 *   <li>No guessing: if unclear, say unknown.</li>
 * </ol>
 */
public final class TaskPromptBuilder {

    private static final DateTimeFormatter DEADLINE_FMT =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy HH:mm z");

    private TaskPromptBuilder() {}

    /**
     * Builds the CALL-E task prompt for the given run and specific provider.
     *
     * @param run  the cascade run containing stage, area, need, deadline, and budget
     * @param spec the specific provider being dialled
     * @return the fully formed task string
     */
    public static String build(Run run, ProviderSpec spec) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(spec, "spec");

        String deadline = DEADLINE_FMT.format(run.getDeadline());
        String budget = run.getBudget().amount().toPlainString()
                + " " + run.getBudget().currencyCode();

        String safeProviderName = sanitizeSingleLine(spec.name());
        String safeArea = sanitizeSingleLine(run.getArea());
        String safeNeed = sanitizeMultiLine(run.getNeed());

        return """
                IMPORTANT: You are an AI assistant calling on behalf of a customer. \
                Identify yourself as an AI at the very start of the call.

                You are calling %s on behalf of a customer at %s who needs urgent assistance \
                during Eskom load-shedding Stage %d.

                SERVICE NEEDED: %s
                LOCATION / ADDRESS: %s
                DEADLINE: %s
                INTERNAL BUDGET CEILING: %s (Do NOT reveal this budget upfront; ask for their standard quote first; no auto-booking; do not commit to any payment).

                Ask the following questions only:
                1. Can you service this need at %s during Eskom load-shedding Stage %d?
                2. Can you be available before %s?
                3. What is your estimated price or quote for this service?

                Negotiation & Budget Matching:
                - If the provider's quote exceeds %s, politely counter: "Our customer's approved budget ceiling for this job is %s. Would you be able to match %s for immediate confirmation?"
                - If they agree to match %s, record the agreed final price as %s.
                - If they decline to match, record their original quote.

                Guidelines:
                - Do NOT volunteer our budget ceiling before asking for their quote.
                - If any answer is unclear or unknown, record it as unknown. Do not guess.
                - Do NOT make or imply any booking or payment commitment.
                - End the call politely once all questions are answered.
                """.formatted(
                safeProviderName,
                safeArea,
                run.getStage(),
                safeNeed,
                safeArea,
                deadline,
                budget,
                safeArea,
                run.getStage(),
                deadline,
                budget,
                budget,
                budget,
                budget,
                budget);
    }

    private static String sanitizeSingleLine(String input) {
        if (input == null) return "";
        // Strip control characters and newlines
        return input.replaceAll("[\\r\\n\\t\\f\\v]", " ").trim();
    }

    private static String sanitizeMultiLine(String input) {
        if (input == null) return "";
        // Strip non-printable ASCII control characters except standard space/newline
        return input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", " ").trim();
    }
}
