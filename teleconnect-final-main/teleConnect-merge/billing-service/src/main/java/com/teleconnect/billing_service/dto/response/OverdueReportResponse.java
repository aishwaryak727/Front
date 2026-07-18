package com.teleconnect.billing_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class OverdueReportResponse {

    private String agingBucket;
    private String region;
    private int totalOverdueCount;
    private BigDecimal totalOverdueAmount;
    private List<OverdueInvoiceItem> content;

    public OverdueReportResponse() {}

    public OverdueReportResponse(String agingBucket, String region, int totalOverdueCount,
                                 BigDecimal totalOverdueAmount, List<OverdueInvoiceItem> content) {
        this.agingBucket = agingBucket;
        this.region = region;
        this.totalOverdueCount = totalOverdueCount;
        this.totalOverdueAmount = totalOverdueAmount;
        this.content = content;
    }

    public String getAgingBucket() { return agingBucket; }
    public void setAgingBucket(String agingBucket) { this.agingBucket = agingBucket; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public int getTotalOverdueCount() { return totalOverdueCount; }
    public void setTotalOverdueCount(int totalOverdueCount) { this.totalOverdueCount = totalOverdueCount; }

    public BigDecimal getTotalOverdueAmount() { return totalOverdueAmount; }
    public void setTotalOverdueAmount(BigDecimal totalOverdueAmount) { this.totalOverdueAmount = totalOverdueAmount; }

    public List<OverdueInvoiceItem> getContent() { return content; }
    public void setContent(List<OverdueInvoiceItem> content) { this.content = content; }

    public static class OverdueInvoiceItem {
        private Long invoiceId;
        private Long accountId;
        private BigDecimal totalAmount;
        private LocalDate dueDate;
        private long daysOverdue;

        public OverdueInvoiceItem() {}

        public OverdueInvoiceItem(Long invoiceId, Long accountId, BigDecimal totalAmount,
                                  LocalDate dueDate, long daysOverdue) {
            this.invoiceId = invoiceId;
            this.accountId = accountId;
            this.totalAmount = totalAmount;
            this.dueDate = dueDate;
            this.daysOverdue = daysOverdue;
        }

        public Long getInvoiceId() { return invoiceId; }
        public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

        public long getDaysOverdue() { return daysOverdue; }
        public void setDaysOverdue(long daysOverdue) { this.daysOverdue = daysOverdue; }
    }
}
