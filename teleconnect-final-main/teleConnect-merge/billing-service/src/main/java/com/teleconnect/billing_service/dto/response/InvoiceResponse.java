package com.teleconnect.billing_service.dto.response;

import com.teleconnect.billing_service.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvoiceResponse {

    private Long invoiceId;
    private Long accountId;
    private Long cycleId;
    private BigDecimal planCharges;
    private BigDecimal excessCharges;
    private BigDecimal addOnCharges;
    private BigDecimal taxes;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal lateFee;
    private LocalDate dueDate;
    private InvoiceStatus status;

    public InvoiceResponse() {}

    public InvoiceResponse(Long invoiceId, Long accountId, Long cycleId, BigDecimal planCharges,
                           BigDecimal excessCharges, BigDecimal addOnCharges, BigDecimal taxes,
                           BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal lateFee,
                           LocalDate dueDate, InvoiceStatus status) {
        this.invoiceId = invoiceId;
        this.accountId = accountId;
        this.cycleId = cycleId;
        this.planCharges = planCharges;
        this.excessCharges = excessCharges;
        this.addOnCharges = addOnCharges;
        this.taxes = taxes;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.lateFee = lateFee;
        this.dueDate = dueDate;
        this.status = status;
    }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

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

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getLateFee() { return lateFee; }
    public void setLateFee(BigDecimal lateFee) { this.lateFee = lateFee; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long invoiceId;
        private Long accountId;
        private Long cycleId;
        private BigDecimal planCharges;
        private BigDecimal excessCharges;
        private BigDecimal addOnCharges;
        private BigDecimal taxes;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal lateFee;
        private LocalDate dueDate;
        private InvoiceStatus status;

        public Builder invoiceId(Long invoiceId) { this.invoiceId = invoiceId; return this; }
        public Builder accountId(Long accountId) { this.accountId = accountId; return this; }
        public Builder cycleId(Long cycleId) { this.cycleId = cycleId; return this; }
        public Builder planCharges(BigDecimal planCharges) { this.planCharges = planCharges; return this; }
        public Builder excessCharges(BigDecimal excessCharges) { this.excessCharges = excessCharges; return this; }
        public Builder addOnCharges(BigDecimal addOnCharges) { this.addOnCharges = addOnCharges; return this; }
        public Builder taxes(BigDecimal taxes) { this.taxes = taxes; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder paidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; return this; }
        public Builder lateFee(BigDecimal lateFee) { this.lateFee = lateFee; return this; }
        public Builder dueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
        public Builder status(InvoiceStatus status) { this.status = status; return this; }

        public InvoiceResponse build() {
            return new InvoiceResponse(invoiceId, accountId, cycleId, planCharges, excessCharges,
                    addOnCharges, taxes, totalAmount, paidAmount, lateFee, dueDate, status);
        }
    }
}
