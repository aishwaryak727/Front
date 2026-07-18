package com.teleconnect.billing_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public class LateFeeWaiverRequest {

    @NotBlank(message = "Waiver reason is required")
    private String waiverReason;

    @NotBlank(message = "Authorised by is required")
    private String authorisedBy;

    public LateFeeWaiverRequest() {}

    public LateFeeWaiverRequest(String waiverReason, String authorisedBy) {
        this.waiverReason = waiverReason;
        this.authorisedBy = authorisedBy;
    }

    public String getWaiverReason() { return waiverReason; }
    public void setWaiverReason(String waiverReason) { this.waiverReason = waiverReason; }

    public String getAuthorisedBy() { return authorisedBy; }
    public void setAuthorisedBy(String authorisedBy) { this.authorisedBy = authorisedBy; }
}
