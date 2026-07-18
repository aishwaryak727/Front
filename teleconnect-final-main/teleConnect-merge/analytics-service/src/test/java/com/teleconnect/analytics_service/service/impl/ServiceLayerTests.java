package com.teleconnect.analytics_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.analytics_service.client.BillingStatsClient;
import com.teleconnect.analytics_service.client.FaultStatsClient;
import com.teleconnect.analytics_service.client.SubscriberStatsClient;
import com.teleconnect.analytics_service.dto.external.InvoiceDto;
import com.teleconnect.analytics_service.dto.external.SubscriberAccountDto;
import com.teleconnect.analytics_service.dto.request.ReportGenerationRequest;
import com.teleconnect.analytics_service.dto.response.*;
import com.teleconnect.analytics_service.entity.TelecomReport;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.enums.AccountType;
import com.teleconnect.analytics_service.enums.InvoiceStatus;
import com.teleconnect.analytics_service.enums.ReportScope;
import com.teleconnect.analytics_service.repository.TelecomReportRepository;
import com.teleconnect.analytics_service.service.ARPUService;
import com.teleconnect.analytics_service.service.ChurnService;
import com.teleconnect.analytics_service.service.CollectionEfficiencyService;
import com.teleconnect.analytics_service.service.NetworkUtilisationService;
import com.teleconnect.analytics_service.service.SLAComplianceService;
import com.teleconnect.analytics_service.service.SubscriberGrowthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceLayerTests {

    @Mock
    private BillingStatsClient billingStatsClient;

    @Mock
    private SubscriberStatsClient subscriberStatsClient;

    @Mock
    private FaultStatsClient faultStatsClient;

    @Mock
    private TelecomReportRepository telecomReportRepository;

    @Mock
    private ARPUService arpuService;

    @Mock
    private ChurnService churnService;

    @Mock
    private CollectionEfficiencyService collectionEfficiencyService;

    @Mock
    private SLAComplianceService slaComplianceService;

    @Mock
    private NetworkUtilisationService networkUtilisationService;

    @Mock
    private SubscriberGrowthService subscriberGrowthService;

    private ARPUServiceImpl arpuServiceImpl;
    private CollectionEfficiencyServiceImpl collectionEfficiencyServiceImpl;
    private DashboardServiceImpl dashboardServiceImpl;
    private ReportServiceImpl reportServiceImpl;

    @BeforeEach
    void setUp() {
        arpuServiceImpl = new ARPUServiceImpl(billingStatsClient, subscriberStatsClient);
        collectionEfficiencyServiceImpl = new CollectionEfficiencyServiceImpl(billingStatsClient);
        dashboardServiceImpl = new DashboardServiceImpl(
                subscriberStatsClient,
                faultStatsClient,
                arpuService,
                churnService,
                collectionEfficiencyService,
                slaComplianceService
        );
        reportServiceImpl = new ReportServiceImpl(
                telecomReportRepository,
                arpuService,
                churnService,
                networkUtilisationService,
                slaComplianceService,
                collectionEfficiencyService,
                subscriberGrowthService,
                new ObjectMapper()
        );
    }

    @TestFactory
    Stream<DynamicTest> arpuServiceComputesExpectedMetricsAcrossScenarios() {
        return IntStream.range(0, 25)
                .mapToObj(index -> DynamicTest.dynamicTest("arpu-scenario-" + index, () -> {
                    Long cycleId = 100L + index;
                    int activeAccounts = Math.max(1, index + 1);
                    List<InvoiceDto> invoices = new ArrayList<>();
                    List<SubscriberAccountDto> accounts = new ArrayList<>();
                    BigDecimal totalRevenue = BigDecimal.ZERO;

                    for (int i = 0; i < activeAccounts; i++) {
                        SubscriberAccountDto account = new SubscriberAccountDto();
                        account.setAccountId((long) i + 1);
                        account.setSubscriberId((long) 200 + i);
                        account.setStatus(AccountStatus.ACTIVE);
                        account.setAccountType(i % 3 == 0 ? AccountType.PREPAID : i % 3 == 1 ? AccountType.POSTPAID : AccountType.ENTERPRISE);
                        account.setRegionId((long) ((i % 3) + 1));
                        accounts.add(account);

                        if (i < activeAccounts / 2 + 1) {
                            InvoiceDto invoice = new InvoiceDto();
                            invoice.setAccountId((long) i + 1);
                            invoice.setTotalAmount(BigDecimal.valueOf(100 + i * 10));
                            invoice.setStatus(InvoiceStatus.PAID);
                            invoices.add(invoice);
                            totalRevenue = totalRevenue.add(invoice.getTotalAmount());
                        }
                    }

                    when(billingStatsClient.getInvoicesByCycle(eq(cycleId), anyList())).thenReturn(invoices);
                    when(subscriberStatsClient.getActiveAccounts()).thenReturn(accounts);

                    ARPUReportResponse response = arpuServiceImpl.computeARPU(cycleId, "PERIOD", "ALL");

                    assertNotNull(response);
                    assertEquals(cycleId, response.getCycleId());
                    assertEquals("PERIOD", response.getScope());
                    assertEquals("ALL", response.getScopeValue());
                    assertEquals(activeAccounts, response.getActiveSubscribers());
                    assertEquals(totalRevenue, response.getTotalRevenue());
                    assertEquals(totalRevenue.divide(BigDecimal.valueOf(activeAccounts), 2, RoundingMode.HALF_UP), response.getArpuOverall());
                    assertFalse(response.getArpuByRegion().isEmpty());
                }));
    }

    @TestFactory
    Stream<DynamicTest> collectionEfficiencyServiceComputesBucketsAcrossScenarios() {
        return IntStream.range(0, 25)
                .mapToObj(index -> DynamicTest.dynamicTest("collection-scenario-" + index, () -> {
                    Long cycleId = 200L + index;
                    LocalDate today = LocalDate.now();
                    List<InvoiceDto> invoices = new ArrayList<>();
                    BigDecimal totalInvoiced = BigDecimal.ZERO;
                    BigDecimal totalCollected = BigDecimal.ZERO;
                    int overdue0to30 = 0;
                    int overdue31to60 = 0;
                    int overdue60plus = 0;

                    for (int i = 0; i < index + 1; i++) {
                        InvoiceDto invoice = new InvoiceDto();
                        invoice.setInvoiceId((long) i + 1);
                        invoice.setTotalAmount(BigDecimal.valueOf(50 + i));
                        invoice.setPaidAmount(BigDecimal.valueOf(40 + i));
                        invoice.setDueDate(today.minusDays(i % 3 == 0 ? 10 : i % 3 == 1 ? 45 : 75));
                        invoice.setStatus(i % 3 == 0 ? InvoiceStatus.PAID : i % 3 == 1 ? InvoiceStatus.OVERDUE : InvoiceStatus.SENT);
                        invoices.add(invoice);
                        totalInvoiced = totalInvoiced.add(invoice.getTotalAmount());
                        if (invoice.getStatus() == InvoiceStatus.PAID) {
                            totalCollected = totalCollected.add(invoice.getPaidAmount());
                        }
                        if (invoice.getStatus() == InvoiceStatus.OVERDUE) {
                            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), today);
                            if (daysOverdue <= 30) overdue0to30++;
                            else if (daysOverdue <= 60) overdue31to60++;
                            else overdue60plus++;
                        }
                    }

                    when(billingStatsClient.getInvoicesByCycle(eq(cycleId), anyList())).thenReturn(invoices);

                    CollectionEfficiencyResponse response = collectionEfficiencyServiceImpl.computeCollectionEfficiency(cycleId);

                    assertNotNull(response);
                    assertEquals(cycleId, response.getCycleId());
                    assertEquals(totalInvoiced, response.getTotalInvoiced());
                    assertEquals(totalCollected, response.getTotalCollected());
                    assertEquals(overdue0to30, response.getOverdueCount0to30());
                    assertEquals(overdue31to60, response.getOverdueCount31to60());
                    assertEquals(overdue60plus, response.getOverdueCount60plus());
                }));
    }

    @TestFactory
    Stream<DynamicTest> dashboardServiceBuildsExpectedKpisAcrossScenarios() {
        return IntStream.range(0, 25)
                .mapToObj(index -> DynamicTest.dynamicTest("dashboard-scenario-" + index, () -> {
                    Long cycleId = 300L + index;
                    long activeSubscribers = 10L + index;
                    long faultCount = 5L + index;
                    BigDecimal arpu = BigDecimal.valueOf(100 + index);
                    double churnRate = 1.5 + index * 0.1;
                    double slaCompliance = 90.0 + index;
                    double collectionEfficiency = 70.0 + index * 0.5;

                    when(subscriberStatsClient.countByStatus(AccountStatus.ACTIVE)).thenReturn(activeSubscribers);
                    when(subscriberStatsClient.countActiveByRegion(1L)).thenReturn(activeSubscribers / 2);
                    when(subscriberStatsClient.countActiveByRegion(2L)).thenReturn(activeSubscribers / 3);
                    when(subscriberStatsClient.getActiveAccounts()).thenReturn(List.of(activeAccount(1L, AccountType.PREPAID), activeAccount(2L, AccountType.POSTPAID)));
                    when(faultStatsClient.countEscalated()).thenReturn(faultCount);
                    when(churnService.computeChurn(any(), any(), any())).thenReturn(churnResponse(churnRate));
                    when(arpuService.computeARPU(eq(cycleId), anyString(), anyString())).thenReturn(arpuResponse(arpu));
                    when(collectionEfficiencyService.computeCollectionEfficiency(eq(cycleId))).thenReturn(collectionResponse(collectionEfficiency));
                    when(slaComplianceService.computeSLACompliance(any(), any())).thenReturn(slaResponse(slaCompliance));

                    DashboardResponse response = dashboardServiceImpl.getDashboard(LocalDate.now().minusDays(7), LocalDate.now(), cycleId);

                    assertNotNull(response);
                    assertNotNull(response.getKpis());
                    assertEquals(activeSubscribers, response.getKpis().getActiveSubscribers());
                    assertEquals(churnRate, response.getKpis().getChurnRate());
                    assertEquals(arpu, response.getKpis().getArpu());
                    assertEquals(collectionEfficiency, response.getKpis().getCollectionEfficiency());
                    assertEquals(slaCompliance, response.getKpis().getSlaCompliance());
                    assertEquals(faultCount, response.getKpis().getFaultCount());
                    assertNotNull(response.getCharts());
                    assertNotNull(response.getRegions());
                    assertNotNull(response.getSegments());
                }));
    }

    @TestFactory
    Stream<DynamicTest> reportServiceGeneratesAndPersistsReportsAcrossScenarios() {
        return IntStream.range(0, 25)
                .mapToObj(index -> DynamicTest.dynamicTest("report-scenario-" + index, () -> {
                    ReportGenerationRequest request = new ReportGenerationRequest();
                    request.setScope(ReportScope.values()[index % ReportScope.values().length]);
                    request.setScopeValue("region-" + index);
                    request.setPeriodStart(LocalDate.now().minusDays(10));
                    request.setPeriodEnd(LocalDate.now());
                    request.setGeneratedBy((long) index + 1);

                    when(churnService.computeChurn(any(), any(), any())).thenReturn(churnResponse(3.2 + index));
                    when(slaComplianceService.computeSLACompliance(any(), any())).thenReturn(slaResponse(92.0 + index));
                    when(subscriberGrowthService.computeGrowth(any(), any())).thenReturn(growthResponse(10L + index, 2L + index));
                    when(telecomReportRepository.save(any(TelecomReport.class))).thenAnswer(invocation -> {
                        TelecomReport report = invocation.getArgument(0);
                        report.setReportId(1000L + index);
                        return report;
                    });

                    TelecomReportResponse response = reportServiceImpl.generateReport(request);

                    assertNotNull(response);
                    assertEquals(request.getScope(), response.getScope());
                    assertEquals(request.getScopeValue(), response.getScopeValue());
                    assertEquals(request.getGeneratedBy(), response.getGeneratedBy());
                    assertTrue(response.getMetrics().contains("ChurnRate"));
                    assertTrue(response.getMetrics().contains("FaultResolutionRate"));
                    assertTrue(response.getMetrics().contains("ActiveSubscribers"));
                    verify(telecomReportRepository, atLeastOnce()).save(any(TelecomReport.class));
                }));
    }

    private SubscriberAccountDto activeAccount(Long accountId, AccountType type) {
        SubscriberAccountDto account = new SubscriberAccountDto();
        account.setAccountId(accountId);
        account.setSubscriberId(accountId + 100);
        account.setAccountType(type);
        account.setStatus(AccountStatus.ACTIVE);
        account.setRegionId(1L);
        return account;
    }

    private ChurnReportResponse churnResponse(double rate) {
        ChurnReportResponse response = new ChurnReportResponse();
        response.setChurnRate(rate);
        response.setGrossChurned(10L);
        response.setAtRiskCount(2);
        return response;
    }

    private SLAComplianceResponse slaResponse(double rate) {
        SLAComplianceResponse response = new SLAComplianceResponse();
        response.setOverallComplianceRate(rate);
        response.setTotalBreaches(1L);
        response.setAvgResolutionHours(3.5);
        return response;
    }

    private ARPUReportResponse arpuResponse(BigDecimal arpu) {
        ARPUReportResponse response = new ARPUReportResponse();
        response.setArpuOverall(arpu);
        response.setArpuPrepaid(arpu);
        response.setArpuPostpaid(arpu);
        response.setArpuEnterprise(arpu);
        response.setArpuByRegion(Map.of("REGION_1", arpu));
        return response;
    }

    private SubscriberGrowthResponse growthResponse(long grossAdds, long netAdds) {
        SubscriberGrowthResponse response = new SubscriberGrowthResponse();
        response.setGrossAdds(grossAdds);
        response.setNetAdds(netAdds);
        return response;
    }

    private CollectionEfficiencyResponse collectionResponse(double efficiency) {
        CollectionEfficiencyResponse response = new CollectionEfficiencyResponse();
        response.setCollectionEfficiencyPct(efficiency);
        response.setOverdueCount0to30(1);
        response.setOverdueCount31to60(2);
        response.setOverdueCount60plus(3);
        return response;
    }
}
