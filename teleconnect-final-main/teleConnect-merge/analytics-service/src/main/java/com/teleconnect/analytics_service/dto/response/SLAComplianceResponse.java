package com.teleconnect.analytics_service.dto.response;

import java.time.LocalDate;
import java.util.Map;

public class SLAComplianceResponse {

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private double overallComplianceRate;
    private Map<String, PriorityStats> statsByPriority;
    private long totalTicketsClosed;
    private long totalBreaches;
    private long escalatedCount;
    private double avgResolutionHours;

    public SLAComplianceResponse() {}

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public double getOverallComplianceRate() { return overallComplianceRate; }
    public void setOverallComplianceRate(double overallComplianceRate) { this.overallComplianceRate = overallComplianceRate; }

    public Map<String, PriorityStats> getStatsByPriority() { return statsByPriority; }
    public void setStatsByPriority(Map<String, PriorityStats> statsByPriority) { this.statsByPriority = statsByPriority; }

    public long getTotalTicketsClosed() { return totalTicketsClosed; }
    public void setTotalTicketsClosed(long totalTicketsClosed) { this.totalTicketsClosed = totalTicketsClosed; }

    public long getTotalBreaches() { return totalBreaches; }
    public void setTotalBreaches(long totalBreaches) { this.totalBreaches = totalBreaches; }

    public long getEscalatedCount() { return escalatedCount; }
    public void setEscalatedCount(long escalatedCount) { this.escalatedCount = escalatedCount; }

    public double getAvgResolutionHours() { return avgResolutionHours; }
    public void setAvgResolutionHours(double avgResolutionHours) { this.avgResolutionHours = avgResolutionHours; }

    public static class PriorityStats {
        private long total;
        private long slaMet;
        private long slaBreached;
        private double complianceRate;
        private double avgResolutionHours;

        public PriorityStats() {}

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }

        public long getSlaMet() { return slaMet; }
        public void setSlaMet(long slaMet) { this.slaMet = slaMet; }

        public long getSlaBreached() { return slaBreached; }
        public void setSlaBreached(long slaBreached) { this.slaBreached = slaBreached; }

        public double getComplianceRate() { return complianceRate; }
        public void setComplianceRate(double complianceRate) { this.complianceRate = complianceRate; }

        public double getAvgResolutionHours() { return avgResolutionHours; }
        public void setAvgResolutionHours(double avgResolutionHours) { this.avgResolutionHours = avgResolutionHours; }
    }
}
