package com.teleconnect.billing_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class LateFeeRequest {

    @NotNull(message = "Fee amount is required")
    @DecimalMin(value = "0.01", message = "Fee amount must be greater than 0")
    private BigDecimal feeAmount;

    private String reason;

    public LateFeeRequest() {}

    public LateFeeRequest(BigDecimal feeAmount, String reason) {
        this.feeAmount = feeAmount;
        this.reason = reason;
    }

    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
