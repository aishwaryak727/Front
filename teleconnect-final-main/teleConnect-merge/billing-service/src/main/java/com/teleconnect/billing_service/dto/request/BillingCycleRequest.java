package com.teleconnect.billing_service.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class BillingCycleRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Cycle start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate cycleStart;

    @NotNull(message = "Cycle end date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate cycleEnd;

    public BillingCycleRequest() {}

    public BillingCycleRequest(Long accountId, LocalDate cycleStart, LocalDate cycleEnd) {
        this.accountId = accountId;
        this.cycleStart = cycleStart;
        this.cycleEnd = cycleEnd;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public LocalDate getCycleStart() { return cycleStart; }
    public void setCycleStart(LocalDate cycleStart) { this.cycleStart = cycleStart; }

    public LocalDate getCycleEnd() { return cycleEnd; }
    public void setCycleEnd(LocalDate cycleEnd) { this.cycleEnd = cycleEnd; }
}
