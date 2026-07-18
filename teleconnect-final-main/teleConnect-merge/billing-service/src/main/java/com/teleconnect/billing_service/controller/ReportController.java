package com.teleconnect.billing_service.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.billing_service.dto.response.CollectionReportResponse;
import com.teleconnect.billing_service.dto.response.DisputeSummaryResponse;
import com.teleconnect.billing_service.dto.response.OverdueReportResponse;
import com.teleconnect.billing_service.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/billing/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized ReportController");
    }

    /**
     * GET /teleConnect/billing/reports/overdue
     * Returns overdue invoices grouped by aging buckets: 0-30, 31-60, 61-90, 90+ days past DueDate.
     * Query: region=South, agingBucket=0-30
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('BILLING_REPORT')")
    public ResponseEntity<OverdueReportResponse> getOverdueReport(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String agingBucket) {
        return ResponseEntity.ok(reportService.getOverdueReport(region, agingBucket));
    }

    /**
     * GET /teleConnect/billing/reports/collection
     * Returns collection efficiency metrics for a given period and region.
     * Query: fromDate=2026-05-01, toDate=2026-05-31, region=South
     */
    @GetMapping("/collection")
    @PreAuthorize("hasAuthority('BILLING_REPORT')")
    public ResponseEntity<CollectionReportResponse> getCollectionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String region) {
        return ResponseEntity.ok(reportService.getCollectionReport(fromDate, toDate, region));
    }

    /**
     * GET /teleConnect/billing/reports/disputes/summary
     * Returns dispute SLA compliance summary.
     * Query: fromDate=2026-05-01, toDate=2026-05-31
     */
    @GetMapping("/disputes/summary")
    @PreAuthorize("hasAuthority('BILLING_REPORT')")
    public ResponseEntity<DisputeSummaryResponse> getDisputeSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(reportService.getDisputeSummary(fromDate, toDate));
    }
}
