package com.teleconnect.analytics_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.analytics_service.dto.request.ReportGenerationRequest;
import com.teleconnect.analytics_service.dto.response.*;
import com.teleconnect.analytics_service.enums.ReportScope;
import com.teleconnect.analytics_service.exception.GlobalExceptionHandler;
import com.teleconnect.analytics_service.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AnalyticsController.class, DashboardController.class, ReportController.class, ExportController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ControllerLayerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ARPUService arpuService;

    @MockBean
    private ChurnService churnService;

    @MockBean
    private NetworkUtilisationService networkUtilisationService;

    @MockBean
    private SLAComplianceService slaComplianceService;

    @MockBean
    private CollectionEfficiencyService collectionEfficiencyService;

    @MockBean
    private SubscriberGrowthService subscriberGrowthService;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private ExportService exportService;

    @MockBean
    private ReportService reportService;

    @Test
    void analyticsControllerReturnsApiResponseForArpuEndpoint() throws Exception {
        ARPUReportResponse response = new ARPUReportResponse();
        response.setArpuOverall(BigDecimal.TEN);
        response.setActiveSubscribers(10L);
        when(arpuService.computeARPU(eq(1L), anyString(), anyString())).thenReturn(response);

        mockMvc.perform(get("/api/reports/arpu").param("cycleId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.arpuOverall").value(10));
    }

    @Test
    void dashboardControllerReturnsApiResponseForKpisEndpoint() throws Exception {
        DashboardResponse.KPIMetrics kpis = new DashboardResponse.KPIMetrics();
        kpis.setActiveSubscribers(42);
        kpis.setChurnRate(1.5);
        when(dashboardService.getKPIMetrics(any(), any(), anyLong())).thenReturn(kpis);

        mockMvc.perform(get("/api/dashboard/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeSubscribers").value(42));
    }

    @Test
    void reportControllerCreatesReportAndReturnsCreatedStatus() throws Exception {
        ReportGenerationRequest request = new ReportGenerationRequest();
        request.setScope(ReportScope.PERIOD);
        request.setScopeValue("north");
        request.setPeriodStart(LocalDate.now().minusDays(3));
        request.setPeriodEnd(LocalDate.now());
        request.setGeneratedBy(99L);

        TelecomReportResponse reportResponse = new TelecomReportResponse();
        reportResponse.setScope(ReportScope.PERIOD);
        reportResponse.setScopeValue("north");
        reportResponse.setGeneratedBy(99L);
        when(reportService.generateReport(any(ReportGenerationRequest.class))).thenReturn(reportResponse);

        mockMvc.perform(post("/api/reports/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scopeValue").value("north"));
    }

    @Test
    void exportControllerReturnsAttachmentHeadersForPdfExport() throws Exception {
        when(dashboardService.getDashboard(any(), any(), anyLong())).thenReturn(new DashboardResponse());
        when(exportService.exportDashboardAsPdf(any(DashboardResponse.class))).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/reports/dashboard/export").param("cycleId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    void globalExceptionHandlerReturnsValidationMessage() throws Exception {
        ReportGenerationRequest request = new ReportGenerationRequest();
        request.setScope(null);
        request.setScopeValue(null);
        request.setPeriodStart(LocalDate.now());
        request.setPeriodEnd(LocalDate.now().minusDays(1));

        mockMvc.perform(post("/api/reports/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
