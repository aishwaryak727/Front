package com.teleconnect.billing_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CollectionReportResponse {

    private LocalDate fromDate;
    private LocalDate toDate;
    private String region;
    private BigDecimal totalBilled;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    private double collectionEfficiency;
    private int invoicesPaid;
    private int invoicesOverdue;

    public CollectionReportResponse() {}

    public CollectionReportResponse(LocalDate fromDate, LocalDate toDate, String region,
                                    BigDecimal totalBilled, BigDecimal totalCollected,
                                    BigDecimal totalOutstanding, double collectionEfficiency,
                                    int invoicesPaid, int invoicesOverdue) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.region = region;
        this.totalBilled = totalBilled;
        this.totalCollected = totalCollected;
        this.totalOutstanding = totalOutstanding;
        this.collectionEfficiency = collectionEfficiency;
        this.invoicesPaid = invoicesPaid;
        this.invoicesOverdue = invoicesOverdue;
    }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public BigDecimal getTotalBilled() { return totalBilled; }
    public void setTotalBilled(BigDecimal totalBilled) { this.totalBilled = totalBilled; }

    public BigDecimal getTotalCollected() { return totalCollected; }
    public void setTotalCollected(BigDecimal totalCollected) { this.totalCollected = totalCollected; }

    public BigDecimal getTotalOutstanding() { return totalOutstanding; }
    public void setTotalOutstanding(BigDecimal totalOutstanding) { this.totalOutstanding = totalOutstanding; }

    public double getCollectionEfficiency() { return collectionEfficiency; }
    public void setCollectionEfficiency(double collectionEfficiency) { this.collectionEfficiency = collectionEfficiency; }

    public int getInvoicesPaid() { return invoicesPaid; }
    public void setInvoicesPaid(int invoicesPaid) { this.invoicesPaid = invoicesPaid; }

    public int getInvoicesOverdue() { return invoicesOverdue; }
    public void setInvoicesOverdue(int invoicesOverdue) { this.invoicesOverdue = invoicesOverdue; }
}
