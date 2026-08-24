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
 *   <li>AI disclosure first — caller must identify as AI.</li>
 *   <li>State the Eskom load-shedding Stage N and suburb.</li>
 *   <li>State the service need and deadline clearly.</li>
 *   <li>Quote the budget ceiling; never discuss auto-booking or payment.</li>
 *   <li>Ask only: "Can you service during load-shedding? What is your price?"</li>
 *   <li>No guessing — if unclear, say unknown.</li>
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

        return """
                IMPORTANT: You are an AI assistant calling on behalf of a customer. \
                Identify yourself as an AI at the very start of the call.

                You are calling %s on behalf of a customer in %s who needs urgent assistance \
                during Eskom load-shedding Stage %d.

                SERVICE NEEDED: %s
                DEADLINE: %s
                BUDGET CEILING: %s (no auto-booking; do not commit to any payment)

                Ask the following questions only:
                1. Can you service this need during Eskom load-shedding Stage %d in %s?
                2. Can you be available before %s?
                3. What is your estimated price (must be within %s)?

                Guidelines:
                - If any answer is unclear or unknown, record it as unknown — do not guess.
                - Do NOT make or imply any booking or payment commitment.
                - End the call politely once all questions are answered.
                """.formatted(
                spec.name(),
                run.getArea(),
                run.getStage(),
                run.getNeed(),
                deadline,
                budget,
                run.getStage(),
                run.getArea(),
                deadline,
                budget);
    }
}
