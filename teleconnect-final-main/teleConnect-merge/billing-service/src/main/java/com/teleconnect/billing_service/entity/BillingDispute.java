package com.teleconnect.billing_service.entity;

import com.teleconnect.billing_service.enums.DisputeStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_disputes")
public class BillingDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long disputeId;

    @Column(nullable = false)
    private Long invoiceId;

    @Column(nullable = false)
    private Long subscriberId;

    @Column(nullable = false, length = 1000)
    private String disputeReason;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal disputedAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal resolvedAmount;

    @Column(nullable = false)
    private LocalDate raisedDate;

    @Column
    private LocalDateTime acknowledgedDate;

    @Column
    private LocalDateTime resolvedDate;

    @Column(length = 500)
    private String assignedTo;

    @Column(length = 2000)
    private String resolutionNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status;

    public BillingDispute() {}

    public BillingDispute(Long disputeId, Long invoiceId, Long subscriberId, String disputeReason,
                          String description, BigDecimal disputedAmount, BigDecimal resolvedAmount,
                          LocalDate raisedDate, LocalDateTime acknowledgedDate, LocalDateTime resolvedDate,
                          String assignedTo, String resolutionNotes, DisputeStatus status) {
        this.disputeId = disputeId;
        this.invoiceId = invoiceId;
        this.subscriberId = subscriberId;
        this.disputeReason = disputeReason;
        this.description = description;
        this.disputedAmount = disputedAmount;
        this.resolvedAmount = resolvedAmount;
        this.raisedDate = raisedDate;
        this.acknowledgedDate = acknowledgedDate;
        this.resolvedDate = resolvedDate;
        this.assignedTo = assignedTo;
        this.resolutionNotes = resolutionNotes;
        this.status = status;
    }

    public Long getDisputeId() { return disputeId; }
    public void setDisputeId(Long disputeId) { this.disputeId = disputeId; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public Long getSubscriberId() { return subscriberId; }
    public void setSubscriberId(Long subscriberId) { this.subscriberId = subscriberId; }

    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getDisputedAmount() { return disputedAmount; }
    public void setDisputedAmount(BigDecimal disputedAmount) { this.disputedAmount = disputedAmount; }

    public BigDecimal getResolvedAmount() { return resolvedAmount; }
    public void setResolvedAmount(BigDecimal resolvedAmount) { this.resolvedAmount = resolvedAmount; }

    public LocalDate getRaisedDate() { return raisedDate; }
    public void setRaisedDate(LocalDate raisedDate) { this.raisedDate = raisedDate; }

    public LocalDateTime getAcknowledgedDate() { return acknowledgedDate; }
    public void setAcknowledgedDate(LocalDateTime acknowledgedDate) { this.acknowledgedDate = acknowledgedDate; }

    public LocalDateTime getResolvedDate() { return resolvedDate; }
    public void setResolvedDate(LocalDateTime resolvedDate) { this.resolvedDate = resolvedDate; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public DisputeStatus getStatus() { return status; }
    public void setStatus(DisputeStatus status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long disputeId;
        private Long invoiceId;
        private Long subscriberId;
        private String disputeReason;
        private String description;
        private BigDecimal disputedAmount;
        private BigDecimal resolvedAmount;
        private LocalDate raisedDate;
        private LocalDateTime acknowledgedDate;
        private LocalDateTime resolvedDate;
        private String assignedTo;
        private String resolutionNotes;
        private DisputeStatus status;

        public Builder disputeId(Long disputeId) { this.disputeId = disputeId; return this; }
        public Builder invoiceId(Long invoiceId) { this.invoiceId = invoiceId; return this; }
        public Builder subscriberId(Long subscriberId) { this.subscriberId = subscriberId; return this; }
        public Builder disputeReason(String disputeReason) { this.disputeReason = disputeReason; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder disputedAmount(BigDecimal disputedAmount) { this.disputedAmount = disputedAmount; return this; }
        public Builder resolvedAmount(BigDecimal resolvedAmount) { this.resolvedAmount = resolvedAmount; return this; }
        public Builder raisedDate(LocalDate raisedDate) { this.raisedDate = raisedDate; return this; }
        public Builder acknowledgedDate(LocalDateTime acknowledgedDate) { this.acknowledgedDate = acknowledgedDate; return this; }
        public Builder resolvedDate(LocalDateTime resolvedDate) { this.resolvedDate = resolvedDate; return this; }
        public Builder assignedTo(String assignedTo) { this.assignedTo = assignedTo; return this; }
        public Builder resolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; return this; }
        public Builder status(DisputeStatus status) { this.status = status; return this; }

        public BillingDispute build() {
            return new BillingDispute(disputeId, invoiceId, subscriberId, disputeReason, description,
                    disputedAmount, resolvedAmount, raisedDate, acknowledgedDate, resolvedDate,
                    assignedTo, resolutionNotes, status);
        }
    }
}
