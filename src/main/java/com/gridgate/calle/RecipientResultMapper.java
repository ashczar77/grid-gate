package com.gridgate.calle;

import com.gridgate.domain.CommitmentMade;
import com.gridgate.domain.Money;
import com.gridgate.domain.Outcome;
import com.gridgate.domain.ProviderResult;
import com.gridgate.domain.TriState;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Maps CALL-E structured recipient JSON into domain {@link ProviderResult} values.
 */
public final class RecipientResultMapper {

    private RecipientResultMapper() {}

    public static ProviderResult fromStructuredResult(Map<String, Object> structured) {
        Objects.requireNonNull(structured, "structured");

        String providerName = requiredString(structured, "provider_name");
        TriState canService = TriState.fromApiValue(requiredString(structured, "can_service"));
        TriState operatingDuringLoadShedding =
                TriState.fromApiValue(requiredString(structured, "operating_during_load_shedding"));
        Money quotedPrice = readQuotedPrice(structured);
        Integer etaMinutes = readInteger(structured.get("eta_minutes"));
        String deliveryCutoffSpoken = readString(structured.get("delivery_cutoff_spoken"));
        String spokenEvidence = requiredString(structured, "spoken_evidence");
        CommitmentMade commitmentMade =
                CommitmentMade.fromApiValue(requiredString(structured, "commitment_made"));
        Outcome outcome = Outcome.fromDisposition(requiredString(structured, "disposition"));

        return new ProviderResult(
                providerName,
                canService,
                operatingDuringLoadShedding,
                quotedPrice,
                etaMinutes,
                deliveryCutoffSpoken,
                spokenEvidence,
                commitmentMade,
                outcome);
    }

    private static Money readQuotedPrice(Map<String, Object> structured) {
        Object amountValue = structured.get("price_amount");
        if (amountValue == null) {
            return null;
        }
        BigDecimal amount = switch (amountValue) {
            case Number number -> BigDecimal.valueOf(number.doubleValue()).stripTrailingZeros();
            case String text when text.isBlank() -> null;
            case String text -> new BigDecimal(text).stripTrailingZeros();
            default -> throw new IllegalArgumentException("price_amount must be a number or null");
        };
        if (amount == null) {
            return null;
        }
        String currency = readString(structured.get("currency"));
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required when price_amount is set");
        }
        return Money.of(amount, currency);
    }

    private static String requiredString(Map<String, Object> structured, String field) {
        String value = readString(structured.get(field));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String readString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer readInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        throw new IllegalArgumentException("expected integer or null");
    }
}
