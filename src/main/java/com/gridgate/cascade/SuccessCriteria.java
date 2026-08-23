package com.gridgate.cascade;

import com.gridgate.domain.CommitmentMade;
import com.gridgate.domain.Money;
import com.gridgate.domain.Outcome;
import com.gridgate.domain.ProviderResult;
import com.gridgate.domain.TriState;
import java.util.Objects;

/**
 * Fail-closed rules for when a provider attempt counts as cascade success.
 */
public final class SuccessCriteria {

    private SuccessCriteria() {}

    /**
     * True only when every required field is a firm yes, the quote fits the budget,
     * and the call ended with a clear success outcome.
     */
    public static boolean isHardSuccess(ProviderResult result, Money budget) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(budget, "budget");

        if (result.outcome() != Outcome.SUCCESS) {
            return false;
        }
        if (result.canService() != TriState.YES) {
            return false;
        }
        if (result.operatingDuringLoadShedding() != TriState.YES) {
            return false;
        }
        if (result.spokenEvidence().isBlank()) {
            return false;
        }
        if (result.commitmentMade() == CommitmentMade.BOOKING) {
            return false;
        }
        return priceWithinBudget(result, budget);
    }

    /**
     * True when the attempt should stop the cascade for human review rather than dial the next provider.
     */
    public static boolean isAmbiguousHalt(ProviderResult result) {
        Objects.requireNonNull(result, "result");
        return result.outcome() == Outcome.AMBIGUOUS;
    }

    private static boolean priceWithinBudget(ProviderResult result, Money budget) {
        return result.quotedPriceOptional()
                .map(quoted -> quoted.isWithinLimit(budget))
                .orElse(true);
    }
}
