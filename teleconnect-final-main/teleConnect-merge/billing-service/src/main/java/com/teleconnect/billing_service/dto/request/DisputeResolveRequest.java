package com.teleconnect.billing_service.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class DisputeResolveRequest {

    @NotBlank(message = "Resolution is required")
    private String resolution;

    private BigDecimal creditAmount;

    private String resolutionNotes;

    public DisputeResolveRequest() {}

    public DisputeResolveRequest(String resolution, BigDecimal creditAmount, String resolutionNotes) {
        this.resolution = resolution;
        this.creditAmount = creditAmount;
        this.resolutionNotes = resolutionNotes;
    }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public BigDecimal getCreditAmount() { return creditAmount; }
    public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}
