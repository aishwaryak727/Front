package com.teleconnect.analytics_service.dto.response;

import java.math.BigDecimal;

public class CollectionEfficiencyResponse {

    private Long cycleId;
    private BigDecimal totalInvoiced;
    private BigDecimal totalCollected;
    private double collectionEfficiencyPct;
    private long overdueCount0to30;
    private long overdueCount31to60;
    private long overdueCount60plus;
    private BigDecimal overdueAmount0to30;
    private BigDecimal overdueAmount31to60;
    private BigDecimal overdueAmount60plus;

    public CollectionEfficiencyResponse() {}

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public BigDecimal getTotalInvoiced() { return totalInvoiced; }
    public void setTotalInvoiced(BigDecimal totalInvoiced) { this.totalInvoiced = totalInvoiced; }

    public BigDecimal getTotalCollected() { return totalCollected; }
    public void setTotalCollected(BigDecimal totalCollected) { this.totalCollected = totalCollected; }

    public double getCollectionEfficiencyPct() { return collectionEfficiencyPct; }
    public void setCollectionEfficiencyPct(double collectionEfficiencyPct) { this.collectionEfficiencyPct = collectionEfficiencyPct; }

    public long getOverdueCount0to30() { return overdueCount0to30; }
    public void setOverdueCount0to30(long overdueCount0to30) { this.overdueCount0to30 = overdueCount0to30; }

    public long getOverdueCount31to60() { return overdueCount31to60; }
    public void setOverdueCount31to60(long overdueCount31to60) { this.overdueCount31to60 = overdueCount31to60; }

    public long getOverdueCount60plus() { return overdueCount60plus; }
    public void setOverdueCount60plus(long overdueCount60plus) { this.overdueCount60plus = overdueCount60plus; }

    public BigDecimal getOverdueAmount0to30() { return overdueAmount0to30; }
    public void setOverdueAmount0to30(BigDecimal overdueAmount0to30) { this.overdueAmount0to30 = overdueAmount0to30; }

    public BigDecimal getOverdueAmount31to60() { return overdueAmount31to60; }
    public void setOverdueAmount31to60(BigDecimal overdueAmount31to60) { this.overdueAmount31to60 = overdueAmount31to60; }

    public BigDecimal getOverdueAmount60plus() { return overdueAmount60plus; }
    public void setOverdueAmount60plus(BigDecimal overdueAmount60plus) { this.overdueAmount60plus = overdueAmount60plus; }
}
