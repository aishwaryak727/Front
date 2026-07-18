package com.teleconnect.analytics_service.dto.response;

import java.time.LocalDate;

public class SubscriberGrowthResponse {

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private long grossAdds;
    private long terminations;
    private long netAdds;
    private long activeSIMActivations;
    private long prepaidAdds;
    private long postpaidAdds;
    private long enterpriseAdds;

    public SubscriberGrowthResponse() {}

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public long getGrossAdds() { return grossAdds; }
    public void setGrossAdds(long grossAdds) { this.grossAdds = grossAdds; }

    public long getTerminations() { return terminations; }
    public void setTerminations(long terminations) { this.terminations = terminations; }

    public long getNetAdds() { return netAdds; }
    public void setNetAdds(long netAdds) { this.netAdds = netAdds; }

    public long getActiveSIMActivations() { return activeSIMActivations; }
    public void setActiveSIMActivations(long activeSIMActivations) { this.activeSIMActivations = activeSIMActivations; }

    public long getPrepaidAdds() { return prepaidAdds; }
    public void setPrepaidAdds(long prepaidAdds) { this.prepaidAdds = prepaidAdds; }

    public long getPostpaidAdds() { return postpaidAdds; }
    public void setPostpaidAdds(long postpaidAdds) { this.postpaidAdds = postpaidAdds; }

    public long getEnterpriseAdds() { return enterpriseAdds; }
    public void setEnterpriseAdds(long enterpriseAdds) { this.enterpriseAdds = enterpriseAdds; }
}
