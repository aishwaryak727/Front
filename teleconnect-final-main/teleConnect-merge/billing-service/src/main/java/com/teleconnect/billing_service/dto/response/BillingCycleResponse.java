package com.teleconnect.billing_service.dto.response;

import com.teleconnect.billing_service.enums.BillingCycleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BillingCycleResponse {

    private Long cycleId;
    private Long accountId;
    private LocalDate cycleStart;
    private LocalDate cycleEnd;
    private LocalDate generatedDate;
    private BillingCycleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BillingCycleResponse() {}

    public BillingCycleResponse(Long cycleId, Long accountId, LocalDate cycleStart, LocalDate cycleEnd,
                                LocalDate generatedDate, BillingCycleStatus status,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.cycleId = cycleId;
        this.accountId = accountId;
        this.cycleStart = cycleStart;
        this.cycleEnd = cycleEnd;
        this.generatedDate = generatedDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder cycleId(Long cycleId) { this.cycleId = cycleId; return this; }
        public Builder accountId(Long accountId) { this.accountId = accountId; return this; }
        public Builder cycleStart(LocalDate cycleStart) { this.cycleStart = cycleStart; return this; }
        public Builder cycleEnd(LocalDate cycleEnd) { this.cycleEnd = cycleEnd; return this; }
        public Builder generatedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; return this; }
        public Builder status(BillingCycleStatus status) { this.status = status; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public BillingCycleResponse build() {
            return new BillingCycleResponse(cycleId, accountId, cycleStart, cycleEnd, generatedDate,
                    status, createdAt, updatedAt);
        }
    }
}
