package com.teleconnect.billing_service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.teleconnect.billing_service.enums.PaymentMethod;
import com.teleconnect.billing_service.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    private Long paymentId;
    private Long invoiceId;
    private BigDecimal amountPaid;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentDate;

    private PaymentMethod paymentMethod;
    private String transactionRef;
    private PaymentStatus status;

    public PaymentResponse() {}

    public PaymentResponse(Long paymentId, Long invoiceId, BigDecimal amountPaid,
                           LocalDateTime paymentDate, PaymentMethod paymentMethod,
                           String transactionRef, PaymentStatus status) {
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.transactionRef = transactionRef;
        this.status = status;
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long paymentId;
        private Long invoiceId;
        private BigDecimal amountPaid;
        private LocalDateTime paymentDate;
        private PaymentMethod paymentMethod;
        private String transactionRef;
        private PaymentStatus status;

        public Builder paymentId(Long paymentId) { this.paymentId = paymentId; return this; }
        public Builder invoiceId(Long invoiceId) { this.invoiceId = invoiceId; return this; }
        public Builder amountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; return this; }
        public Builder paymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; return this; }
        public Builder paymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public Builder transactionRef(String transactionRef) { this.transactionRef = transactionRef; return this; }
        public Builder status(PaymentStatus status) { this.status = status; return this; }

        public PaymentResponse build() {
            return new PaymentResponse(paymentId, invoiceId, amountPaid, paymentDate,
                    paymentMethod, transactionRef, status);
        }
    }
}
