package com.teleconnect.analytics_service.dto.external;

import com.teleconnect.analytics_service.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lightweight projection of Invoice, as returned by the Billing &
 * Invoice Management module's analytics-facing API.
 */
public class InvoiceDto {

    private Long invoiceId;
    private Long accountId;
    private Long cycleId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private LocalDate dueDate;
    private InvoiceStatus status;

    public InvoiceDto() {}

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
}
