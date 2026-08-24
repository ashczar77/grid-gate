package com.gridgate.ledger;

import com.gridgate.domain.CommitmentMade;
import com.gridgate.domain.Outcome;
import com.gridgate.domain.TriState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "run_attempts")
public class ProviderAttemptEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private RunEntity run;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "phone_e164", nullable = false)
    private String phoneE164;

    @Column(name = "sequence_index", nullable = false)
    private int sequenceIndex;

    @Column(name = "calle_call_id")
    private String calleCallId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // Structured result fields (populated upon completion)
    @Column(name = "result_provider_name")
    private String resultProviderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "can_service", length = 16)
    private TriState canService;

    @Enumerated(EnumType.STRING)
    @Column(name = "operating_during_load_shedding", length = 16)
    private TriState operatingDuringLoadShedding;

    @Column(name = "quoted_price_amount", precision = 19, scale = 4)
    private BigDecimal quotedPriceAmount;

    @Column(name = "quoted_price_currency", length = 3)
    private String quotedPriceCurrency;

    @Column(name = "eta_minutes")
    private Integer etaMinutes;

    @Column(name = "delivery_cutoff_spoken")
    private String deliveryCutoffSpoken;

    @Column(name = "spoken_evidence", length = 4000)
    private String spokenEvidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "commitment_made", length = 32)
    private CommitmentMade commitmentMade;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 32)
    private Outcome outcome;

    public ProviderAttemptEntity() {
    }

    public ProviderAttemptEntity(
            UUID id,
            RunEntity run,
            String providerId,
            String providerName,
            String phoneE164,
            int sequenceIndex) {
        this.id = id;
        this.run = run;
        this.providerId = providerId;
        this.providerName = providerName;
        this.phoneE164 = phoneE164;
        this.sequenceIndex = sequenceIndex;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RunEntity getRun() {
        return run;
    }

    public void setRun(RunEntity run) {
        this.run = run;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getPhoneE164() {
        return phoneE164;
    }

    public void setPhoneE164(String phoneE164) {
        this.phoneE164 = phoneE164;
    }

    public int getSequenceIndex() {
        return sequenceIndex;
    }

    public void setSequenceIndex(int sequenceIndex) {
        this.sequenceIndex = sequenceIndex;
    }

    public String getCalleCallId() {
        return calleCallId;
    }

    public void setCalleCallId(String calleCallId) {
        this.calleCallId = calleCallId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getResultProviderName() {
        return resultProviderName;
    }

    public void setResultProviderName(String resultProviderName) {
        this.resultProviderName = resultProviderName;
    }

    public TriState getCanService() {
        return canService;
    }

    public void setCanService(TriState canService) {
        this.canService = canService;
    }

    public TriState getOperatingDuringLoadShedding() {
        return operatingDuringLoadShedding;
    }

    public void setOperatingDuringLoadShedding(TriState operatingDuringLoadShedding) {
        this.operatingDuringLoadShedding = operatingDuringLoadShedding;
    }

    public BigDecimal getQuotedPriceAmount() {
        return quotedPriceAmount;
    }

    public void setQuotedPriceAmount(BigDecimal quotedPriceAmount) {
        this.quotedPriceAmount = quotedPriceAmount;
    }

    public String getQuotedPriceCurrency() {
        return quotedPriceCurrency;
    }

    public void setQuotedPriceCurrency(String quotedPriceCurrency) {
        this.quotedPriceCurrency = quotedPriceCurrency;
    }

    public Integer getEtaMinutes() {
        return etaMinutes;
    }

    public void setEtaMinutes(Integer etaMinutes) {
        this.etaMinutes = etaMinutes;
    }

    public String getDeliveryCutoffSpoken() {
        return deliveryCutoffSpoken;
    }

    public void setDeliveryCutoffSpoken(String deliveryCutoffSpoken) {
        this.deliveryCutoffSpoken = deliveryCutoffSpoken;
    }

    public String getSpokenEvidence() {
        return spokenEvidence;
    }

    public void setSpokenEvidence(String spokenEvidence) {
        this.spokenEvidence = spokenEvidence;
    }

    public CommitmentMade getCommitmentMade() {
        return commitmentMade;
    }

    public void setCommitmentMade(CommitmentMade commitmentMade) {
        this.commitmentMade = commitmentMade;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }
}
