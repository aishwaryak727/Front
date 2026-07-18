package com.teleconnect.analytics_service.service;

import com.teleconnect.analytics_service.dto.response.DashboardResponse;
import java.time.LocalDate;

public interface DashboardService {
    
    /**
     * Get comprehensive dashboard aggregating all analytics data
     * Combines data from all 5 peer modules
     */
    DashboardResponse getDashboard(LocalDate startDate, LocalDate endDate, Long cycleId);
    
    /**
     * Get KPI metrics only
     */
    DashboardResponse.KPIMetrics getKPIMetrics(LocalDate startDate, LocalDate endDate, Long cycleId);
    
    /**
     * Get chart data for visualizations
     */
    DashboardResponse.ChartData getChartData(LocalDate startDate, LocalDate endDate, Long cycleId);
}
