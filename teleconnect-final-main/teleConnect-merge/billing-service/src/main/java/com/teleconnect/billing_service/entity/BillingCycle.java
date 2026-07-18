package com.teleconnect.billing_service.entity;

import com.teleconnect.billing_service.enums.BillingCycleStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_cycles")
public class BillingCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cycleId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private LocalDate cycleStart;

    @Column(nullable = false)
    private LocalDate cycleEnd;

    private LocalDate generatedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycleStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public BillingCycle() {}

    public BillingCycle(Long cycleId, Long accountId, LocalDate cycleStart, LocalDate cycleEnd,
                        LocalDate generatedDate, BillingCycleStatus status) {
        this.cycleId = cycleId;
        this.accountId = accountId;
        this.cycleStart = cycleStart;
        this.cycleEnd = cycleEnd;
        this.generatedDate = generatedDate;
        this.status = status;
    }

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public LocalDate getCycleStart() { return cycleStart; }
    public void setCycleStart(LocalDate cycleStart) { this.cycleStart = cycleStart; }

    public LocalDate getCycleEnd() { return cycleEnd; }
    public void setCycleEnd(LocalDate cycleEnd) { this.cycleEnd = cycleEnd; }

    public LocalDate getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }

    public BillingCycleStatus getStatus() { return status; }
    public void setStatus(BillingCycleStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long cycleId;
        private Long accountId;
        private LocalDate cycleStart;
        private LocalDate cycleEnd;
        private LocalDate generatedDate;
        private BillingCycleStatus status;

        public Builder cycleId(Long cycleId) { this.cycleId = cycleId; return this; }
        public Builder accountId(Long accountId) { this.accountId = accountId; return this; }
        public Builder cycleStart(LocalDate cycleStart) { this.cycleStart = cycleStart; return this; }
        public Builder cycleEnd(LocalDate cycleEnd) { this.cycleEnd = cycleEnd; return this; }
        public Builder generatedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; return this; }
        public Builder status(BillingCycleStatus status) { this.status = status; return this; }

        public BillingCycle build() {
            return new BillingCycle(cycleId, accountId, cycleStart, cycleEnd, generatedDate, status);
        }
    }
}
