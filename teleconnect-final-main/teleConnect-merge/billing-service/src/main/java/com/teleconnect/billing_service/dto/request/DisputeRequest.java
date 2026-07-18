package com.teleconnect.billing_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class DisputeRequest {

    @NotNull(message = "Invoice ID is required")
    private Long invoiceId;

    private Long subscriberId;

    @NotBlank(message = "Dispute reason is required")
    private String disputeReason;

    @NotNull(message = "Disputed amount is required")
    @DecimalMin(value = "0.01", message = "Disputed amount must be greater than 0")
    private BigDecimal disputedAmount;

    private String description;

    public DisputeRequest() {}

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public Long getSubscriberId() { return subscriberId; }
    public void setSubscriberId(Long subscriberId) { this.subscriberId = subscriberId; }

    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }

    public BigDecimal getDisputedAmount() { return disputedAmount; }
    public void setDisputedAmount(BigDecimal disputedAmount) { this.disputedAmount = disputedAmount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
