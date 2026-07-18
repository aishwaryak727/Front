package com.teleconnect.billing_service.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CycleGenerationRequest {

    @NotNull(message = "Cycle date is required")
    private LocalDate cycleDate;

    private boolean dryRun = false;

    public CycleGenerationRequest() {}

    public CycleGenerationRequest(LocalDate cycleDate, boolean dryRun) {
        this.cycleDate = cycleDate;
        this.dryRun = dryRun;
    }

    public LocalDate getCycleDate() { return cycleDate; }
    public void setCycleDate(LocalDate cycleDate) { this.cycleDate = cycleDate; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }
}
