package com.teleconnect.analytics_service.dto.response;

import com.teleconnect.analytics_service.entity.TelecomReport;
import com.teleconnect.analytics_service.enums.ReportScope;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TelecomReportResponse {

    private Long reportId;
    private ReportScope scope;
    private String scopeValue;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String metrics;
    private LocalDateTime generatedDate;
    private Long generatedBy;

    public TelecomReportResponse() {}

    public static TelecomReportResponse from(TelecomReport r) {
        TelecomReportResponse dto = new TelecomReportResponse();
        dto.reportId = r.getReportId();
        dto.scope = r.getScope();
        dto.scopeValue = r.getScopeValue();
        dto.periodStart = r.getPeriodStart();
        dto.periodEnd = r.getPeriodEnd();
        dto.metrics = r.getMetrics();
        dto.generatedDate = r.getGeneratedDate();
        dto.generatedBy = r.getGeneratedBy();
        return dto;
    }

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public ReportScope getScope() { return scope; }
    public void setScope(ReportScope scope) { this.scope = scope; }

    public String getScopeValue() { return scopeValue; }
    public void setScopeValue(String scopeValue) { this.scopeValue = scopeValue; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public String getMetrics() { return metrics; }
    public void setMetrics(String metrics) { this.metrics = metrics; }

    public LocalDateTime getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(LocalDateTime generatedDate) { this.generatedDate = generatedDate; }

    public Long getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(Long generatedBy) { this.generatedBy = generatedBy; }
}
