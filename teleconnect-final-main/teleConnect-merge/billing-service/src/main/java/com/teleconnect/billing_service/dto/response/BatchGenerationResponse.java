package com.teleconnect.billing_service.dto.response;

import java.time.LocalDateTime;

public class BatchGenerationResponse {

    private int cyclesProcessed;
    private int invoicesGenerated;
    private int skipped;
    private int errors;
    private boolean dryRun;
    private LocalDateTime runDate;

    public BatchGenerationResponse() {}

    public BatchGenerationResponse(int cyclesProcessed, int invoicesGenerated, int skipped,
                                   int errors, boolean dryRun, LocalDateTime runDate) {
        this.cyclesProcessed = cyclesProcessed;
        this.invoicesGenerated = invoicesGenerated;
        this.skipped = skipped;
        this.errors = errors;
        this.dryRun = dryRun;
        this.runDate = runDate;
    }

    public int getCyclesProcessed() { return cyclesProcessed; }
    public void setCyclesProcessed(int cyclesProcessed) { this.cyclesProcessed = cyclesProcessed; }

    public int getInvoicesGenerated() { return invoicesGenerated; }
    public void setInvoicesGenerated(int invoicesGenerated) { this.invoicesGenerated = invoicesGenerated; }

    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }

    public int getErrors() { return errors; }
    public void setErrors(int errors) { this.errors = errors; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public LocalDateTime getRunDate() { return runDate; }
    public void setRunDate(LocalDateTime runDate) { this.runDate = runDate; }
}
