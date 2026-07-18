package com.teleconnect.analytics_service.entity;

import com.teleconnect.analytics_service.enums.ReportScope;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "telecom_reports", indexes = {
    @Index(name = "idx_report_scope_period", columnList = "scope, period_start, period_end")
})
public class TelecomReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportScope scope;

    @Column(nullable = false, length = 100)
    private String scopeValue;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String metrics;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedDate;

    @Column
    private Long generatedBy;

    @PrePersist
    protected void onCreate() {
        this.generatedDate = LocalDateTime.now();
    }

    public TelecomReport() {}

    public TelecomReport(Long reportId, ReportScope scope, String scopeValue,
                         LocalDate periodStart, LocalDate periodEnd, String metrics,
                         LocalDateTime generatedDate, Long generatedBy) {
        this.reportId = reportId;
        this.scope = scope;
        this.scopeValue = scopeValue;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.metrics = metrics;
        this.generatedDate = generatedDate;
        this.generatedBy = generatedBy;
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

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long reportId;
        private ReportScope scope;
        private String scopeValue;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String metrics;
        private LocalDateTime generatedDate;
        private Long generatedBy;

        public Builder reportId(Long reportId) { this.reportId = reportId; return this; }
        public Builder scope(ReportScope scope) { this.scope = scope; return this; }
        public Builder scopeValue(String scopeValue) { this.scopeValue = scopeValue; return this; }
        public Builder periodStart(LocalDate periodStart) { this.periodStart = periodStart; return this; }
        public Builder periodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; return this; }
        public Builder metrics(String metrics) { this.metrics = metrics; return this; }
        public Builder generatedDate(LocalDateTime generatedDate) { this.generatedDate = generatedDate; return this; }
        public Builder generatedBy(Long generatedBy) { this.generatedBy = generatedBy; return this; }

        public TelecomReport build() {
            return new TelecomReport(reportId, scope, scopeValue, periodStart, periodEnd,
                    metrics, generatedDate, generatedBy);
        }
    }
}
