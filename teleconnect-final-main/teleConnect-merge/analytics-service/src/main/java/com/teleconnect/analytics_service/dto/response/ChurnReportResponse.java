package com.teleconnect.analytics_service.dto.response;

import java.time.LocalDate;
import java.util.List;

public class ChurnReportResponse {

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String region;
    private long subscribersAtPeriodStart;
    private long terminatedAccounts;
    private long portedOutLines;
    private long grossChurned;
    private double churnRate;
    private boolean highChurnAlert;
    private long atRiskCount;
    private List<Long> atRiskAccountIds;

    public ChurnReportResponse() {}

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public long getSubscribersAtPeriodStart() { return subscribersAtPeriodStart; }
    public void setSubscribersAtPeriodStart(long subscribersAtPeriodStart) { this.subscribersAtPeriodStart = subscribersAtPeriodStart; }

    public long getTerminatedAccounts() { return terminatedAccounts; }
    public void setTerminatedAccounts(long terminatedAccounts) { this.terminatedAccounts = terminatedAccounts; }

    public long getPortedOutLines() { return portedOutLines; }
    public void setPortedOutLines(long portedOutLines) { this.portedOutLines = portedOutLines; }

    public long getGrossChurned() { return grossChurned; }
    public void setGrossChurned(long grossChurned) { this.grossChurned = grossChurned; }

    public double getChurnRate() { return churnRate; }
    public void setChurnRate(double churnRate) { this.churnRate = churnRate; }

    public boolean isHighChurnAlert() { return highChurnAlert; }
    public void setHighChurnAlert(boolean highChurnAlert) { this.highChurnAlert = highChurnAlert; }

    public long getAtRiskCount() { return atRiskCount; }
    public void setAtRiskCount(long atRiskCount) { this.atRiskCount = atRiskCount; }

    public List<Long> getAtRiskAccountIds() { return atRiskAccountIds; }
    public void setAtRiskAccountIds(List<Long> atRiskAccountIds) { this.atRiskAccountIds = atRiskAccountIds; }
}
