package com.teleconnect.analytics_service.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.analytics_service.dto.response.*;
import com.teleconnect.analytics_service.exception.AnalyticsException;
import com.teleconnect.analytics_service.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/reports")
public class ExportController {

    private final ExportService exportService;
    private final DashboardService dashboardService;
    private final ARPUService arpuService;
    private final ChurnService churnService;
    private final NetworkUtilisationService networkUtilisationService;
    private final SLAComplianceService slaComplianceService;
    private final CollectionEfficiencyService collectionEfficiencyService;
    private final SubscriberGrowthService subscriberGrowthService;

    public ExportController(ExportService exportService,
                            DashboardService dashboardService,
                            ARPUService arpuService,
                            ChurnService churnService,
                            NetworkUtilisationService networkUtilisationService,
                            SLAComplianceService slaComplianceService,
                            CollectionEfficiencyService collectionEfficiencyService,
                            SubscriberGrowthService subscriberGrowthService) {
        this.exportService = exportService;
        this.dashboardService = dashboardService;
        this.arpuService = arpuService;
        this.churnService = churnService;
        this.networkUtilisationService = networkUtilisationService;
        this.slaComplianceService = slaComplianceService;
        this.collectionEfficiencyService = collectionEfficiencyService;
        this.subscriberGrowthService = subscriberGrowthService;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized ExportController");
    }

    /**
     * Export a complete executive dashboard as PDF.
     * GET /api/reports/dashboard/export?cycleId=1&startDate=2024-06-01&endDate=2024-06-30
     */
    @GetMapping("/dashboard/export")
    public ResponseEntity<byte[]> exportDashboard(
            @RequestParam(required = false, defaultValue = "1") Long cycleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate from = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate to = endDate != null ? endDate : LocalDate.now();
        log.info("Exporting dashboard PDF for cycleId={} from={} to={}", cycleId, from, to);
        try {
            DashboardResponse dashboard = dashboardService.getDashboard(from, to, cycleId);
            byte[] pdf = exportService.exportDashboardAsPdf(dashboard);
            log.info("Exported dashboard PDF for cycleId={} sizeBytes={}", cycleId, pdf.length);
            return pdfResponse(pdf, "dashboard-report.pdf");
        } catch (IOException e) {
            log.error("Dashboard PDF export failed for cycleId={}", cycleId, e);
            throw new AnalyticsException("Export failed: " + e.getMessage());
        }
    }

    /**
     * Export a saved TelecomReport snapshot by ID.
     * GET /api/reports/{reportId}/export?format=pdf|csv
     */
    @GetMapping("/{reportId}/export")
    public ResponseEntity<byte[]> exportSavedReport(
            @PathVariable Long reportId,
            @RequestParam(defaultValue = "pdf") String format) {
        log.info("Exporting saved report reportId={} format={}", reportId, format);
        try {
            if ("csv".equalsIgnoreCase(format)) {
                byte[] data = exportService.exportReportAsCsv(reportId);
                log.info("Exported saved report CSV reportId={} sizeBytes={}", reportId, data.length);
                return csvResponse(data, "report-" + reportId + ".csv");
            }
            byte[] data = exportService.exportReportAsPdf(reportId);
            log.info("Exported saved report PDF reportId={} sizeBytes={}", reportId, data.length);
            return pdfResponse(data, "report-" + reportId + ".pdf");
        } catch (IOException e) {
            log.error("Saved report export failed reportId={} format={}", reportId, format, e);
            throw new AnalyticsException("Export failed: " + e.getMessage());
        }
    }

    /**
     * Export ARPU report.
     * GET /api/reports/arpu/export?cycleId=1&scope=PERIOD&scopeValue=ALL&format=pdf|csv
     */
    @GetMapping("/arpu/export")
    public ResponseEntity<byte[]> exportArpu(
            @RequestParam Long cycleId,
            @RequestParam(defaultValue = "PERIOD") String scope,
            @RequestParam(defaultValue = "ALL") String scopeValue,
            @RequestParam(defaultValue = "pdf") String format) {
        ARPUReportResponse data = arpuService.computeARPU(cycleId, scope, scopeValue);
        try {
            if ("csv".equalsIgnoreCase(format)) {
                return csvResponse(exportService.exportArpuAsCsv(data), "arpu-cycle-" + cycleId + ".csv");
            }
            return pdfResponse(exportService.exportArpuAsPdf(data), "arpu-cycle-" + cycleId + ".pdf");
        } catch (IOException e) {
            throw new AnalyticsException("Export failed: " + e.getMessage());
        }
    }

    /**
     * Export Churn report.
     * GET /api/reports/churn/export?periodStart=&periodEnd=&region=&format=pdf|csv
     */
    @GetMapping("/churn/export")
    public ResponseEntity<byte[]> exportChurn(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "pdf") String format) {
        ChurnReportResponse data = churnService.computeChurn(periodStart, periodEnd, region);
        try {
            if ("csv".equalsIgnoreCase(format)) {
                return csvResponse(exportService.exportChurnAsCsv(data), "churn-report.csv");
            }
            return pdfResponse(exportService.exportChurnAsPdf(data), "churn-report.pdf");
        } catch (IOException e) {
            throw new AnalyticsException("Export failed: " + e.getMessage());
        }
    }

    /**
     * Export Network Utilisation report.
     * GET /api/reports/network-utilisation/export?cycleId=&region=&format=pdf|csv
     */
    @GetMapping("/network-utilisation/export")
    public ResponseEntity<byte[]> exportNetworkUtilisation(
            @RequestParam Long cycleId,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "pdf") String format) {
        NetworkUtilisationResponse data = networkUtilisationService.computeUtilisation(cycleId, region);
        try {
            if ("csv".equalsIgnoreCase(format)) {
                return csvResponse(exportService.exportNetworkUtilisationAsCsv(data), "network-utilisation-cycle-" + cycleId + ".csv");
            }
            return pdfResponse(exportService.exportNetworkUtilisationAsPdf(data), "network-utilisation-cycle-" + cycleId + ".pdf");
        } catch (IOException e) {
            throw new AnalyticsException("Export failed: " + e.getMessage());
        }
    }

    /**
     * Export SLA Compliance report.
     * GET /api/reports/sla-compliance/export?periodStart=&periodEnd=&format=pdf|csv
     */
    @GetMapping("/sla-compliance/export")
    public ResponseEntity<byte[]> exportSLACompliance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestParam(defaultValue = "pdf") String format) {
        SLAComplianceResponse data = slaComplianceService.computeSLACompliance(periodStart, periodEnd);
        try {
            if ("csv".equalsIgnoreCase(format)) {
                return csvResponse(exportService.exportSLAComplianceAsCsv(data), "sla-compliance.csv");
            }
            return pdfResponse(exportService.exportSLAComplianceAsPdf(data), "sla-compliance.pdf");
        } catch (IOException e) {
            throw new AnalyticsException("Export failed: " + e.getMessage());
        }
    }

    /**
     * Export Collection Efficiency report.
     * GET /api/reports/collection-efficiency/export?cycleId=&format=pdf|csv
     */
    @GetMapping("/collection-efficiency/export")
    public ResponseEntity<byte[]> exportCollectionEfficiency(
            @RequestParam Long cycleId,
            @RequestParam(defaultValue = "pdf") String format) {
        CollectionEfficiencyResponse data = collectionEfficiencyService.computeCollectionEfficiency(cycleId);
        try {
            if ("csv".equalsIgnoreCase(format)) {
                return csvResponse(exportService.exportCollectionEfficiencyAsCsv(data), "collection-efficiency-cycle-" + cycleId + ".csv");
            }
            return pdfResponse(exportService.exportCollectionEfficiencyAsPdf(data), "collection-efficiency-cycle-" + cycleId + ".pdf");
        } catch (IOException e) {
            throw new AnalyticsException("Export failed: " + e.getMessage());
        }
    }

    /**
     * Export Subscriber Growth report.
     * GET /api/reports/subscriber-growth/export?periodStart=&periodEnd=&format=pdf|csv
     */
    @GetMapping("/subscriber-growth/export")
    public ResponseEntity<byte[]> exportSubscriberGrowth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestParam(defaultValue = "pdf") String format) {
        SubscriberGrowthResponse data = subscriberGrowthService.computeGrowth(periodStart, periodEnd);
        try {
            if ("csv".equalsIgnoreCase(format)) {
                return csvResponse(exportService.exportSubscriberGrowthAsCsv(data), "subscriber-growth.csv");
            }
            return pdfResponse(exportService.exportSubscriberGrowthAsPdf(data), "subscriber-growth.pdf");
        } catch (IOException e) {
            throw new AnalyticsException("Export failed: " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> pdfResponse(byte[] data, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(data.length);
        return ResponseEntity.ok().headers(headers).body(data);
    }

    private ResponseEntity<byte[]> csvResponse(byte[] data, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(data.length);
        return ResponseEntity.ok().headers(headers).body(data);
    }
}
