package com.teleconnect.analytics_service.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.analytics_service.dto.request.ReportGenerationRequest;
import com.teleconnect.analytics_service.dto.response.ApiResponse;
import com.teleconnect.analytics_service.dto.response.TelecomReportResponse;
import com.teleconnect.analytics_service.enums.ReportScope;
import com.teleconnect.analytics_service.service.ReportService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditClient;
import com.teleconnect.common.audit.AuditModule;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final AuditClient auditClient;

    public ReportController(ReportService reportService, AuditClient auditClient) {
        this.reportService = reportService;
        this.auditClient = auditClient;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized ReportController");
    }

    /**
     * POST /api/reports/generate
     * Trigger on-demand report generation.
     * Roles: Admin, Billing, NetworkOps
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<TelecomReportResponse>> generateReport(
            @Valid @RequestBody ReportGenerationRequest request,
            HttpServletRequest httpReq) {
        log.info("Generating report scope={} scopeValue={} periodStart={} periodEnd={} generatedBy={}",
                request.getScope(), request.getScopeValue(), request.getPeriodStart(), request.getPeriodEnd(), request.getGeneratedBy());
        TelecomReportResponse result = reportService.generateReport(request);
        auditClient.record(AuditAction.GENERATE_REPORT, AuditModule.ANALYTICS, httpReq);
        log.info("Report generated reportId={}", result.getReportId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Report generated successfully", result));
    }

    /**
     * GET /api/reports/{reportId}
     * Fetch a previously stored TelecomReport by ID.
     * Roles: All privileged roles
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<TelecomReportResponse>> getReport(@PathVariable Long reportId) {
        log.info("Fetching report reportId={}", reportId);
        TelecomReportResponse result = reportService.getReportById(reportId);
        log.debug("Fetched report reportId={}", reportId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/reports?scope=&from=&to=&page=&size=
     * Paginated list of historical report snapshots.
     * Roles: Admin, Compliance
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TelecomReportResponse>>> listReports(
            @RequestParam(required = false) ReportScope scope,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Listing reports scope={} from={} to={} page={} size={}", scope, from, to, page, size);
        Page<TelecomReportResponse> result = reportService.listReports(scope, from, to, page, size);
        log.info("Listed {} reports", result.getNumberOfElements());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
