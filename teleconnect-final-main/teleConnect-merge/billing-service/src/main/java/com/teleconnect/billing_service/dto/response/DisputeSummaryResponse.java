package com.teleconnect.billing_service.dto.response;

import java.time.LocalDate;

public class DisputeSummaryResponse {

    private LocalDate fromDate;
    private LocalDate toDate;
    private int totalDisputes;
    private int acknowledgedWithin24h;
    private int resolvedWithin5Days;
    private int slaBreaches;
    private double slaComplianceRate;
    private int resolved;
    private int rejected;
    private int open;
    private int underReview;

    public DisputeSummaryResponse() {}

    public DisputeSummaryResponse(LocalDate fromDate, LocalDate toDate, int totalDisputes,
                                  int acknowledgedWithin24h, int resolvedWithin5Days, int slaBreaches,
                                  double slaComplianceRate, int resolved, int rejected,
                                  int open, int underReview) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.totalDisputes = totalDisputes;
        this.acknowledgedWithin24h = acknowledgedWithin24h;
        this.resolvedWithin5Days = resolvedWithin5Days;
        this.slaBreaches = slaBreaches;
        this.slaComplianceRate = slaComplianceRate;
        this.resolved = resolved;
        this.rejected = rejected;
        this.open = open;
        this.underReview = underReview;
    }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public int getTotalDisputes() { return totalDisputes; }
    public void setTotalDisputes(int totalDisputes) { this.totalDisputes = totalDisputes; }

    public int getAcknowledgedWithin24h() { return acknowledgedWithin24h; }
    public void setAcknowledgedWithin24h(int acknowledgedWithin24h) { this.acknowledgedWithin24h = acknowledgedWithin24h; }

    public int getResolvedWithin5Days() { return resolvedWithin5Days; }
    public void setResolvedWithin5Days(int resolvedWithin5Days) { this.resolvedWithin5Days = resolvedWithin5Days; }

    public int getSlaBreaches() { return slaBreaches; }
    public void setSlaBreaches(int slaBreaches) { this.slaBreaches = slaBreaches; }

    public double getSlaComplianceRate() { return slaComplianceRate; }
    public void setSlaComplianceRate(double slaComplianceRate) { this.slaComplianceRate = slaComplianceRate; }

    public int getResolved() { return resolved; }
    public void setResolved(int resolved) { this.resolved = resolved; }

    public int getRejected() { return rejected; }
    public void setRejected(int rejected) { this.rejected = rejected; }

    public int getOpen() { return open; }
    public void setOpen(int open) { this.open = open; }

    public int getUnderReview() { return underReview; }
    public void setUnderReview(int underReview) { this.underReview = underReview; }
}
