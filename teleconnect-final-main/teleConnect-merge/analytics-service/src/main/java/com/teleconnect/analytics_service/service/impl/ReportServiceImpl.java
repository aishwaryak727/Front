package com.teleconnect.analytics_service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.analytics_service.dto.request.ReportGenerationRequest;
import com.teleconnect.analytics_service.dto.response.*;
import com.teleconnect.analytics_service.entity.TelecomReport;
import com.teleconnect.analytics_service.enums.ReportScope;
import com.teleconnect.analytics_service.exception.AnalyticsException;
import com.teleconnect.analytics_service.exception.ResourceNotFoundException;
import com.teleconnect.analytics_service.repository.TelecomReportRepository;
import com.teleconnect.analytics_service.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private final TelecomReportRepository telecomReportRepository;
    private final ARPUService arpuService;
    private final ChurnService churnService;
    private final NetworkUtilisationService networkUtilisationService;
    private final SLAComplianceService slaComplianceService;
    private final CollectionEfficiencyService collectionEfficiencyService;
    private final SubscriberGrowthService subscriberGrowthService;
    private final ObjectMapper objectMapper;

    public ReportServiceImpl(TelecomReportRepository telecomReportRepository,
                             ARPUService arpuService,
                             ChurnService churnService,
                             NetworkUtilisationService networkUtilisationService,
                             SLAComplianceService slaComplianceService,
                             CollectionEfficiencyService collectionEfficiencyService,
                             SubscriberGrowthService subscriberGrowthService,
                             ObjectMapper objectMapper) {
        this.telecomReportRepository = telecomReportRepository;
        this.arpuService = arpuService;
        this.churnService = churnService;
        this.networkUtilisationService = networkUtilisationService;
        this.slaComplianceService = slaComplianceService;
        this.collectionEfficiencyService = collectionEfficiencyService;
        this.subscriberGrowthService = subscriberGrowthService;
        this.objectMapper = objectMapper;
    }

    @Override
    public TelecomReportResponse generateReport(ReportGenerationRequest request) {
        log.info("Generating report request scope={} scopeValue={} from {} to {}",
                request.getScope(), request.getScopeValue(),
                request.getPeriodStart(), request.getPeriodEnd());

        Map<String, Object> metricsMap = new HashMap<>();

        ChurnReportResponse churn = churnService.computeChurn(
                request.getPeriodStart(), request.getPeriodEnd(), request.getScopeValue());
        log.debug("Churn metrics computed for report: churnRate={} grossChurned={} atRiskCount={}",
                churn.getChurnRate(), churn.getGrossChurned(), churn.getAtRiskCount());
        metricsMap.put("ChurnRate", churn.getChurnRate());
        metricsMap.put("GrossChurned", churn.getGrossChurned());
        metricsMap.put("AtRiskCount", churn.getAtRiskCount());

        SLAComplianceResponse sla = slaComplianceService.computeSLACompliance(
                request.getPeriodStart(), request.getPeriodEnd());
        log.debug("SLA metrics computed for report: complianceRate={} totalBreaches={} avgResolutionHours={}",
                sla.getOverallComplianceRate(), sla.getTotalBreaches(), sla.getAvgResolutionHours());
        metricsMap.put("FaultResolutionRate", sla.getOverallComplianceRate());
        metricsMap.put("SLABreachCount", sla.getTotalBreaches());
        metricsMap.put("AvgResolutionHours", sla.getAvgResolutionHours());

        SubscriberGrowthResponse growth = subscriberGrowthService.computeGrowth(
                request.getPeriodStart(), request.getPeriodEnd());
        log.debug("Subscriber growth metrics computed for report: grossAdds={} netAdds={}",
                growth.getGrossAdds(), growth.getNetAdds());
        metricsMap.put("ActiveSubscribers", growth.getGrossAdds());
        metricsMap.put("NetAdds", growth.getNetAdds());

        String metricsJson;
        try {
            metricsJson = objectMapper.writeValueAsString(metricsMap);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metrics for report scope={} scopeValue={}", request.getScope(), request.getScopeValue(), e);
            throw new AnalyticsException("Failed to serialize metrics: " + e.getMessage());
        }

        TelecomReport report = TelecomReport.builder()
                .scope(request.getScope())
                .scopeValue(request.getScopeValue())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .metrics(metricsJson)
                .generatedBy(request.getGeneratedBy())
                .build();

        TelecomReport saved = telecomReportRepository.save(report);
        log.info("Report persisted with ID={}", saved.getReportId());

        return TelecomReportResponse.from(saved);
    }

    @Override
    public TelecomReportResponse getReportById(Long reportId) {
        log.debug("Fetching report by id={}", reportId);
        TelecomReport report = telecomReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));
        log.debug("Report retrieved id={}", reportId);
        return TelecomReportResponse.from(report);
    }

    @Override
    public Page<TelecomReportResponse> listReports(ReportScope scope, LocalDate from, LocalDate to,
                                                    int page, int size) {
        log.debug("Listing reports scope={} from={} to={} page={} size={}", scope, from, to, page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("generatedDate").descending());
        return telecomReportRepository.findByFilters(scope, from, to, pageable)
                .map(TelecomReportResponse::from);
    }
}
