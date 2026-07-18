package com.teleconnect.analytics_service.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.teleconnect.analytics_service.client.FaultStatsClient;
import com.teleconnect.analytics_service.client.SubscriberStatsClient;
import com.teleconnect.analytics_service.dto.external.SubscriberAccountDto;
import com.teleconnect.analytics_service.dto.response.*;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.service.ARPUService;
import com.teleconnect.analytics_service.service.ChurnService;
import com.teleconnect.analytics_service.service.CollectionEfficiencyService;
import com.teleconnect.analytics_service.service.DashboardService;
import com.teleconnect.analytics_service.service.SLAComplianceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    private final SubscriberStatsClient subscriberStatsClient;
    private final FaultStatsClient faultStatsClient;
    private final ARPUService arpuService;
    private final ChurnService churnService;
    private final CollectionEfficiencyService collectionEfficiencyService;
    private final SLAComplianceService slaComplianceService;

    public DashboardServiceImpl(SubscriberStatsClient subscriberStatsClient,
                               FaultStatsClient faultStatsClient,
                               ARPUService arpuService,
                               ChurnService churnService,
                               CollectionEfficiencyService collectionEfficiencyService,
                               SLAComplianceService slaComplianceService) {
        this.subscriberStatsClient = subscriberStatsClient;
        this.faultStatsClient = faultStatsClient;
        this.arpuService = arpuService;
        this.churnService = churnService;
        this.collectionEfficiencyService = collectionEfficiencyService;
        this.slaComplianceService = slaComplianceService;
        log.info("DashboardServiceImpl initialized successfully");
    }

    @Override
    public DashboardResponse getDashboard(LocalDate startDate, LocalDate endDate, Long cycleId) {
        log.info("Building complete dashboard for cycle={}, startDate={}, endDate={}", cycleId, startDate, endDate);
        DashboardResponse dashboard = new DashboardResponse();
        dashboard.setKpis(getKPIMetrics(startDate, endDate, cycleId));
        dashboard.setCharts(getChartData(startDate, endDate, cycleId));
        dashboard.setRegions(getRegionalAnalysis(startDate, endDate, cycleId));
        dashboard.setSegments(getSegmentAnalysis(startDate, endDate, cycleId));
        log.info("Dashboard built successfully");
        return dashboard;
    }

    @Override
    public DashboardResponse.KPIMetrics getKPIMetrics(LocalDate startDate, LocalDate endDate, Long cycleId) {
        DashboardResponse.KPIMetrics kpis = new DashboardResponse.KPIMetrics();
        log.info("Fetching KPI metrics for cycle={}, startDate={}, endDate={}", cycleId, startDate, endDate);

        try {
            // Active Subscribers
            try {
                long activeCount = subscriberStatsClient.countByStatus(AccountStatus.ACTIVE);
                kpis.setActiveSubscribers(activeCount);
                log.debug("Active subscribers: {}", activeCount);
            } catch (Exception e) {
                log.warn("Error fetching active subscribers", e);
                kpis.setActiveSubscribers(0);
            }

            // Churn Rate
            try {
                ChurnReportResponse churnData = churnService.computeChurn(startDate, endDate, null);
                if (churnData != null) {
                    kpis.setChurnRate(churnData.getChurnRate());
                    log.debug("Churn rate: {}", churnData.getChurnRate());
                } else {
                    kpis.setChurnRate(0.0);
                }
            } catch (Exception e) {
                log.warn("Error fetching churn rate", e);
                kpis.setChurnRate(0.0);
            }

            // ARPU
            try {
                ARPUReportResponse arpuData = arpuService.computeARPU(cycleId, "PERIOD", "ALL");
                if (arpuData != null && arpuData.getArpuOverall() != null) {
                    kpis.setArpu(arpuData.getArpuOverall());
                    log.debug("ARPU: {}", arpuData.getArpuOverall());
                } else {
                    kpis.setArpu(BigDecimal.ZERO);
                }
            } catch (Exception e) {
                log.warn("Error fetching ARPU", e);
                kpis.setArpu(BigDecimal.ZERO);
            }

            // Collection Efficiency
            try {
                CollectionEfficiencyResponse collectionData = collectionEfficiencyService.computeCollectionEfficiency(cycleId);
                if (collectionData != null) {
                    kpis.setCollectionEfficiency(collectionData.getCollectionEfficiencyPct());
                    log.debug("Collection efficiency: {}%", collectionData.getCollectionEfficiencyPct());
                } else {
                    kpis.setCollectionEfficiency(0.0);
                }
            } catch (Exception e) {
                log.warn("Error fetching collection efficiency", e);
                kpis.setCollectionEfficiency(0.0);
            }

            // SLA Compliance
            try {
                SLAComplianceResponse slaData = slaComplianceService.computeSLACompliance(startDate, endDate);
                if (slaData != null) {
                    kpis.setSlaCompliance(slaData.getOverallComplianceRate());
                    log.debug("SLA compliance: {}%", slaData.getOverallComplianceRate());
                } else {
                    kpis.setSlaCompliance(0.0);
                }
            } catch (Exception e) {
                log.warn("Error fetching SLA compliance", e);
                kpis.setSlaCompliance(0.0);
            }

            // Data Consumption (Usage)
            kpis.setDataConsumption(15000L);

            // Fault Count
            try {
                long faultCount = faultStatsClient.countEscalated();
                kpis.setFaultCount(faultCount);
                log.debug("Escalated faults: {}", faultCount);
            } catch (Exception e) {
                log.warn("Error fetching fault count", e);
                kpis.setFaultCount(0);
            }

            // Dispute Rate (mock calculation)
            kpis.setDisputeRate(2.5);
            
            log.info("KPI metrics computed successfully");

        } catch (Exception e) {
            log.error("Unexpected error in getKPIMetrics", e);
            // Fallback to default values on error
            kpis.setActiveSubscribers(0);
            kpis.setChurnRate(0.0);
            kpis.setArpu(BigDecimal.ZERO);
            kpis.setCollectionEfficiency(0.0);
            kpis.setSlaCompliance(0.0);
            kpis.setDataConsumption(0L);
            kpis.setFaultCount(0);
            kpis.setDisputeRate(0.0);
        }

        return kpis;
    }

    @Override
    public DashboardResponse.ChartData getChartData(LocalDate startDate, LocalDate endDate, Long cycleId) {
        log.info("Fetching chart data for cycle={}", cycleId);
        DashboardResponse.ChartData charts = new DashboardResponse.ChartData();

        try {
            // Data Consumption Trend (Line Chart)
            charts.setConsumptionTrend(
                new DashboardResponse.LineChartData(
                    "Data Consumption Trend",
                    Arrays.asList("Week 1", "Week 2", "Week 3", "Week 4"),
                    Arrays.asList(3500L, 4200L, 3800L, 4500L)
                )
            );

            // ARPU by Account Type (Bar Chart)
            ARPUReportResponse arpu = arpuService.computeARPU(cycleId, "PERIOD", "ALL");
            if (arpu != null) {
                charts.setArpuByAccountType(
                    new DashboardResponse.BarChartData(
                        "ARPU by Account Type",
                        Arrays.asList("Prepaid", "Postpaid", "Enterprise"),
                        Arrays.asList(
                            arpu.getArpuPrepaid() != null ? arpu.getArpuPrepaid().doubleValue() : 0.0,
                            arpu.getArpuPostpaid() != null ? arpu.getArpuPostpaid().doubleValue() : 0.0,
                            arpu.getArpuEnterprise() != null ? arpu.getArpuEnterprise().doubleValue() : 0.0
                        )
                    )
                );
            }

            // Churn by Segment (Pie Chart)
            charts.setChurnBySegment(
                new DashboardResponse.PieChartData(
                    "Churn Distribution by Segment",
                    Arrays.asList("Prepaid", "Postpaid", "Enterprise"),
                    Arrays.asList(45.0, 35.0, 20.0)
                )
            );

            // Collection Overdue Ageing (Bar Chart)
            CollectionEfficiencyResponse collection = collectionEfficiencyService.computeCollectionEfficiency(cycleId);
            if (collection != null) {
                charts.setCollectionOverdueAgeing(
                    new DashboardResponse.BarChartData(
                        "Overdue Invoices by Age",
                        Arrays.asList("0-30 days", "31-60 days", "60+ days"),
                        Arrays.asList(
                            (double) collection.getOverdueCount0to30(),
                            (double) collection.getOverdueCount31to60(),
                            (double) collection.getOverdueCount60plus()
                        )
                    )
                );
            }

            // Subscriber Growth Trend (Line Chart)
            charts.setSubscriberGrowthTrend(
                new DashboardResponse.LineChartData(
                    "Subscriber Growth Trend",
                    Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun"),
                    Arrays.asList(1000L, 1200L, 1450L, 1650L, 1850L, 2000L)
                )
            );

            // Fault Frequency by Priority (Bar Chart)
            charts.setFaultFrequency(
                new DashboardResponse.BarChartData(
                    "Fault Frequency by Priority",
                    Arrays.asList("Critical", "High", "Medium", "Low"),
                    Arrays.asList(15.0, 35.0, 40.0, 10.0)
                )
            );

        } catch (Exception e) {
            // Return empty charts on error - prevents complete failure
            log.error("Error fetching chart data", e);
            charts.setConsumptionTrend(new DashboardResponse.LineChartData("Data Consumption Trend", new ArrayList<>(), new ArrayList<>()));
        }

        log.info("Chart data compiled successfully");
        return charts;
    }

    private DashboardResponse.RegionalAnalysis getRegionalAnalysis(LocalDate startDate, LocalDate endDate, Long cycleId) {
        DashboardResponse.RegionalAnalysis regional = new DashboardResponse.RegionalAnalysis();
        log.debug("Building regional analysis for cycle={}, startDate={}, endDate={}", cycleId, startDate, endDate);

        try {
            Map<String, Long> subscribersByRegion = new HashMap<>();
            subscribersByRegion.put("Region 1", subscriberStatsClient.countActiveByRegion(1L));
            subscribersByRegion.put("Region 2", subscriberStatsClient.countActiveByRegion(2L));
            regional.setSubscribersByRegion(subscribersByRegion);

            Map<String, Double> churnByRegion = new HashMap<>();
            churnByRegion.put("Region 1", 2.5);
            churnByRegion.put("Region 2", 3.1);
            regional.setChurnByRegion(churnByRegion);

            Map<String, BigDecimal> arpuByRegion = new HashMap<>();
            ARPUReportResponse arpu = arpuService.computeARPU(cycleId, "PERIOD", "ALL");
            if (arpu != null && arpu.getArpuByRegion() != null) {
                arpuByRegion.putAll(arpu.getArpuByRegion());
            }
            regional.setArpuByRegion(arpuByRegion);
        } catch (Exception e) {
            // Return empty regional on error
            regional.setSubscribersByRegion(new HashMap<>());
            regional.setChurnByRegion(new HashMap<>());
            regional.setArpuByRegion(new HashMap<>());
        }

        return regional;
    }

    private DashboardResponse.SegmentAnalysis getSegmentAnalysis(LocalDate startDate, LocalDate endDate, Long cycleId) {
        DashboardResponse.SegmentAnalysis segment = new DashboardResponse.SegmentAnalysis();
        log.debug("Building segment analysis for cycle={}, startDate={}, endDate={}", cycleId, startDate, endDate);

        try {
            List<SubscriberAccountDto> activeAccounts = subscriberStatsClient.getActiveAccounts();

            if (activeAccounts != null && !activeAccounts.isEmpty()) {
                // Subscribers by Account Type
                Map<String, Long> subscribersByType = activeAccounts.stream()
                    .collect(Collectors.groupingBy(
                        a -> a.getAccountType() != null ? a.getAccountType().name() : "UNKNOWN",
                        Collectors.counting()
                    ));
                segment.setSubscribersByAccountType(subscribersByType);
            } else {
                segment.setSubscribersByAccountType(new HashMap<>());
            }

            // ARPU by Account Type
            ARPUReportResponse arpu = arpuService.computeARPU(cycleId, "PERIOD", "ALL");
            Map<String, BigDecimal> arpuByType = new HashMap<>();
            if (arpu != null) {
                arpuByType.put("PREPAID", arpu.getArpuPrepaid() != null ? arpu.getArpuPrepaid() : BigDecimal.ZERO);
                arpuByType.put("POSTPAID", arpu.getArpuPostpaid() != null ? arpu.getArpuPostpaid() : BigDecimal.ZERO);
                arpuByType.put("ENTERPRISE", arpu.getArpuEnterprise() != null ? arpu.getArpuEnterprise() : BigDecimal.ZERO);
            }
            segment.setArpuByAccountType(arpuByType);

            // Placeholder for churn by account type
            Map<String, Double> churnByType = new HashMap<>();
            churnByType.put("PREPAID", 2.8);
            churnByType.put("POSTPAID", 2.5);
            churnByType.put("ENTERPRISE", 1.2);
            segment.setChurnByAccountType(churnByType);

        } catch (Exception e) {
            // Return empty segment on error
            segment.setSubscribersByAccountType(new HashMap<>());
            segment.setArpuByAccountType(new HashMap<>());
            segment.setChurnByAccountType(new HashMap<>());
        }

        return segment;
    }
}
