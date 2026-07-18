package com.teleconnect.billing_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public class DisputeReviewRequest {

    @NotBlank(message = "Assigned to is required")
    private String assignedTo;

    private String notes;

    public DisputeReviewRequest() {}

    public DisputeReviewRequest(String assignedTo, String notes) {
        this.assignedTo = assignedTo;
        this.notes = notes;
    }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
