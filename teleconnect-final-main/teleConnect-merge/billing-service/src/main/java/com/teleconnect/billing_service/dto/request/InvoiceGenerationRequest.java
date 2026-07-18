package com.teleconnect.billing_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class InvoiceGenerationRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Cycle ID is required")
    private Long cycleId;

    @NotNull(message = "Plan charges are required")
    @DecimalMin(value = "0.0", message = "Plan charges must be 0 or more")
    private BigDecimal planCharges;

    @NotNull(message = "Excess charges are required")
    @DecimalMin(value = "0.0", message = "Excess charges must be 0 or more")
    private BigDecimal excessCharges;

    @NotNull(message = "Add-on charges are required")
    @DecimalMin(value = "0.0", message = "Add-on charges must be 0 or more")
    private BigDecimal addOnCharges;

    @NotNull(message = "Taxes are required")
    @DecimalMin(value = "0.0", message = "Taxes must be 0 or more")
    private BigDecimal taxes;

    public InvoiceGenerationRequest() {}

    public InvoiceGenerationRequest(Long accountId, Long cycleId, BigDecimal planCharges,
                                    BigDecimal excessCharges, BigDecimal addOnCharges, BigDecimal taxes) {
        this.accountId = accountId;
        this.cycleId = cycleId;
        this.planCharges = planCharges;
        this.excessCharges = excessCharges;
        this.addOnCharges = addOnCharges;
        this.taxes = taxes;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public BigDecimal getPlanCharges() { return planCharges; }
    public void setPlanCharges(BigDecimal planCharges) { this.planCharges = planCharges; }

    public BigDecimal getExcessCharges() { return excessCharges; }
    public void setExcessCharges(BigDecimal excessCharges) { this.excessCharges = excessCharges; }

    public BigDecimal getAddOnCharges() { return addOnCharges; }
    public void setAddOnCharges(BigDecimal addOnCharges) { this.addOnCharges = addOnCharges; }

    public BigDecimal getTaxes() { return taxes; }
    public void setTaxes(BigDecimal taxes) { this.taxes = taxes; }
}
