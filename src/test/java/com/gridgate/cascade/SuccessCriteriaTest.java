package com.gridgate.cascade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gridgate.domain.CommitmentMade;
import com.gridgate.domain.Money;
import com.gridgate.domain.Outcome;
import com.gridgate.domain.ProviderResult;
import com.gridgate.domain.TriState;
import org.junit.jupiter.api.Test;

class SuccessCriteriaTest {

    private static final Money BUDGET = Money.of(800, "ZAR");

    @Test
    void hardSuccessWhenAllFieldsMatchAndPriceWithinBudget() {
        ProviderResult result = baseResult()
                .quotedPrice(Money.of(750, "ZAR"))
                .build();

        assertTrue(SuccessCriteria.isHardSuccess(result, BUDGET));
    }

    @Test
    void hardSuccessWhenQuotedPriceMissing() {
        ProviderResult result = baseResult()
                .quotedPrice(null)
                .build();

        assertTrue(SuccessCriteria.isHardSuccess(result, BUDGET));
    }

    @Test
    void failsWhenCanServiceIsNo() {
        ProviderResult result = baseResult()
                .canService(TriState.NO)
                .build();

        assertFalse(SuccessCriteria.isHardSuccess(result, BUDGET));
    }

    @Test
    void failsWhenCanServiceIsUnknown() {
        ProviderResult result = baseResult()
                .canService(TriState.UNKNOWN)
                .build();

        assertFalse(SuccessCriteria.isHardSuccess(result, BUDGET));
    }

    @Test
    void failsWhenOperatingDuringOutageIsUnknown() {
        ProviderResult result = baseResult()
                .operatingDuringLoadShedding(TriState.UNKNOWN)
                .build();

        assertFalse(SuccessCriteria.isHardSuccess(result, BUDGET));
    }

    @Test
    void failsWhenPriceExceedsBudget() {
        ProviderResult result = baseResult()
                .quotedPrice(Money.of(801, "ZAR"))
                .build();

        assertFalse(SuccessCriteria.isHardSuccess(result, BUDGET));
    }

    @Test
    void failsWhenCurrencyDiffersFromBudget() {
        ProviderResult result = baseResult()
                .quotedPrice(Money.of(500, "USD"))
                .build();

        assertFalse(SuccessCriteria.isHardSuccess(result, BUDGET));
    }

    @Test
    void failsWhenSpokenEvidenceBlank() {
        ProviderResult result = baseResult()
                .spokenEvidence("   ")
                .build();

        assertFalse(SuccessCriteria.isHardSuccess(result, BUDGET));
    }

    @Test
    void failsWhenOutcomeIsVoicemail() {
        ProviderResult result = baseResult()
                .outcome(Outcome.VOICEMAIL)
                .build();

        assertFalse(SuccessCriteria.isHardSuccess(result, BUDGET));
    }

    @Test
    void failsWhenOutcomeIsAmbiguous() {
        ProviderResult result = baseResult()
                .outcome(Outcome.AMBIGUOUS)
                .build();

        assertFalse(SuccessCriteria.isHardSuccess(result, BUDGET));
        assertTrue(SuccessCriteria.isAmbiguousHalt(result));
    }

    @Test
    void failsWhenBookingCommitmentMade() {
        ProviderResult result = baseResult()
                .commitmentMade(CommitmentMade.BOOKING)
                .build();

        assertFalse(SuccessCriteria.isHardSuccess(result, BUDGET));
    }

    private static Builder baseResult() {
        return new Builder();
    }

    private static final class Builder {
        private String providerName = "GenHire Joburg";
        private TriState canService = TriState.YES;
        private TriState operatingDuringLoadShedding = TriState.YES;
        private Money quotedPrice = Money.of(750, "ZAR");
        private String spokenEvidence = "Yes, we can deliver before 6pm during Stage 6.";
        private CommitmentMade commitmentMade = CommitmentMade.NONE;
        private Outcome outcome = Outcome.SUCCESS;

        Builder quotedPrice(Money value) {
            this.quotedPrice = value;
            return this;
        }

        Builder canService(TriState value) {
            this.canService = value;
            return this;
        }

        Builder operatingDuringLoadShedding(TriState value) {
            this.operatingDuringLoadShedding = value;
            return this;
        }

        Builder spokenEvidence(String value) {
            this.spokenEvidence = value;
            return this;
        }

        Builder commitmentMade(CommitmentMade value) {
            this.commitmentMade = value;
            return this;
        }

        Builder outcome(Outcome value) {
            this.outcome = value;
            return this;
        }

        ProviderResult build() {
            return new ProviderResult(
                    providerName,
                    canService,
                    operatingDuringLoadShedding,
                    quotedPrice,
                    null,
                    null,
                    spokenEvidence,
                    commitmentMade,
                    outcome);
        }
    }
}
