package com.teleconnect.analytics_service.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.analytics_service.dto.response.ApiResponse;
import com.teleconnect.analytics_service.dto.response.DashboardResponse;
import com.teleconnect.analytics_service.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized DashboardController");
    }

    /**
     * Get comprehensive executive dashboard
     * Aggregates all analytics from 5 modules (Subscriber, Plan, Usage, Billing, Fault)
     * 
     * GET /api/dashboard?cycleId=1&startDate=2024-06-01&endDate=2024-06-30
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "1") Long cycleId) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();
        log.info("Getting dashboard for cycleId={} from={} to={}", cycleId, startDate, endDate);
        DashboardResponse dashboard = dashboardService.getDashboard(startDate, endDate, cycleId);
        log.info("Dashboard data retrieved for cycleId={}", cycleId);
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved", dashboard));
    }

    /**
     * Get KPI metrics only (faster endpoint for quick metrics)
     * 
     * GET /api/dashboard/kpis?cycleId=1&startDate=2024-06-01&endDate=2024-06-30
     */
    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<DashboardResponse.KPIMetrics>> getKPIMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "1") Long cycleId) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();
        log.info("Getting dashboard KPIs for cycleId={} from={} to={}", cycleId, startDate, endDate);
        DashboardResponse.KPIMetrics kpis = dashboardService.getKPIMetrics(startDate, endDate, cycleId);
        log.info("KPI metrics retrieved for cycleId={}", cycleId);
        return ResponseEntity.ok(ApiResponse.success("KPI metrics retrieved", kpis));
    }

    /**
     * Get chart data for visualizations (graphs, dashboards)
     * Includes: Line charts, Bar charts, Pie charts
     * 
     * GET /api/dashboard/charts?cycleId=1&startDate=2024-06-01&endDate=2024-06-30
     */
    @GetMapping("/charts")
    public ResponseEntity<ApiResponse<DashboardResponse.ChartData>> getChartData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "1") Long cycleId) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();
        log.info("Getting dashboard charts for cycleId={} from={} to={}", cycleId, startDate, endDate);
        DashboardResponse.ChartData charts = dashboardService.getChartData(startDate, endDate, cycleId);
        log.info("Dashboard charts retrieved for cycleId={}", cycleId);
        return ResponseEntity.ok(ApiResponse.success("Chart data retrieved", charts));
    }

    /**
     * Get regional breakdown
     * Aggregates metrics by region
     * 
     * GET /api/dashboard/regions?cycleId=1&startDate=2024-06-01&endDate=2024-06-30
     */
    @GetMapping("/regions")
    public ResponseEntity<ApiResponse<DashboardResponse.RegionalAnalysis>> getRegionalAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "1") Long cycleId) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();
        log.info("Getting regional analysis for cycleId={} from={} to={}", cycleId, startDate, endDate);
        DashboardResponse dashboard = dashboardService.getDashboard(startDate, endDate, cycleId);
        log.info("Regional analysis retrieved for cycleId={}", cycleId);
        return ResponseEntity.ok(ApiResponse.success("Regional analysis retrieved", dashboard.getRegions()));
    }

    /**
     * Get segment breakdown (by account type)
     * Aggregates metrics by subscriber segment
     * 
     * GET /api/dashboard/segments?cycleId=1&startDate=2024-06-01&endDate=2024-06-30
     */
    @GetMapping("/segments")
    public ResponseEntity<ApiResponse<DashboardResponse.SegmentAnalysis>> getSegmentAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "1") Long cycleId) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();
        log.info("Getting segment analysis for cycleId={} from={} to={}", cycleId, startDate, endDate);
        DashboardResponse dashboard = dashboardService.getDashboard(startDate, endDate, cycleId);
        log.info("Segment analysis retrieved for cycleId={}", cycleId);
        return ResponseEntity.ok(ApiResponse.success("Segment analysis retrieved", dashboard.getSegments()));
    }
}
