package com.gridgate.ledger;

import com.gridgate.domain.RunStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "runs")
public class RunEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "stage", nullable = false)
    private int stage;

    @Column(name = "area", nullable = false)
    private String area;

    @Column(name = "need", nullable = false, length = 1000)
    private String need;

    @Column(name = "deadline", nullable = false)
    private ZonedDateTime deadline;

    @Column(name = "budget_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal budgetAmount;

    @Column(name = "budget_currency", nullable = false, length = 3)
    private String budgetCurrency;

    @Column(name = "dry_run", nullable = false)
    private boolean dryRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RunStatus status;

    @Column(name = "next_provider_index", nullable = false)
    private int nextProviderIndex;

    @Column(name = "winner_provider_id")
    private String winnerProviderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("positionIndex ASC")
    private List<ProviderSpecEntity> providers = new ArrayList<>();

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sequenceIndex ASC")
    private List<ProviderAttemptEntity> attempts = new ArrayList<>();

    public RunEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getNeed() {
        return need;
    }

    public void setNeed(String need) {
        this.need = need;
    }

    public ZonedDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(ZonedDateTime deadline) {
        this.deadline = deadline;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public String getBudgetCurrency() {
        return budgetCurrency;
    }

    public void setBudgetCurrency(String budgetCurrency) {
        this.budgetCurrency = budgetCurrency;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public int getNextProviderIndex() {
        return nextProviderIndex;
    }

    public void setNextProviderIndex(int nextProviderIndex) {
        this.nextProviderIndex = nextProviderIndex;
    }

    public String getWinnerProviderId() {
        return winnerProviderId;
    }

    public void setWinnerProviderId(String winnerProviderId) {
        this.winnerProviderId = winnerProviderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ProviderSpecEntity> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderSpecEntity> providers) {
        this.providers = providers;
    }

    public List<ProviderAttemptEntity> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<ProviderAttemptEntity> attempts) {
        this.attempts = attempts;
    }
}
