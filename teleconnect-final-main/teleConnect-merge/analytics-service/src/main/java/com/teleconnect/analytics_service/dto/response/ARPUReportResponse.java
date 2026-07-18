package com.teleconnect.analytics_service.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public class ARPUReportResponse {

    private Long cycleId;
    private String scope;
    private String scopeValue;
    private BigDecimal arpuOverall;
    private BigDecimal arpuPrepaid;
    private BigDecimal arpuPostpaid;
    private BigDecimal arpuEnterprise;
    private Map<String, BigDecimal> arpuByRegion;
    private long activeSubscribers;
    private BigDecimal totalRevenue;

    public ARPUReportResponse() {}

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getScopeValue() { return scopeValue; }
    public void setScopeValue(String scopeValue) { this.scopeValue = scopeValue; }

    public BigDecimal getArpuOverall() { return arpuOverall; }
    public void setArpuOverall(BigDecimal arpuOverall) { this.arpuOverall = arpuOverall; }

    public BigDecimal getArpuPrepaid() { return arpuPrepaid; }
    public void setArpuPrepaid(BigDecimal arpuPrepaid) { this.arpuPrepaid = arpuPrepaid; }

    public BigDecimal getArpuPostpaid() { return arpuPostpaid; }
    public void setArpuPostpaid(BigDecimal arpuPostpaid) { this.arpuPostpaid = arpuPostpaid; }

    public BigDecimal getArpuEnterprise() { return arpuEnterprise; }
    public void setArpuEnterprise(BigDecimal arpuEnterprise) { this.arpuEnterprise = arpuEnterprise; }

    public Map<String, BigDecimal> getArpuByRegion() { return arpuByRegion; }
    public void setArpuByRegion(Map<String, BigDecimal> arpuByRegion) { this.arpuByRegion = arpuByRegion; }

    public long getActiveSubscribers() { return activeSubscribers; }
    public void setActiveSubscribers(long activeSubscribers) { this.activeSubscribers = activeSubscribers; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
}
