package com.teleconnect.billing_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.billing_service.dto.response.CollectionReportResponse;
import com.teleconnect.billing_service.dto.response.DisputeSummaryResponse;
import com.teleconnect.billing_service.dto.response.OverdueReportResponse;
import com.teleconnect.billing_service.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.teleconnect.billing_service.security.JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    @Test
    void getOverdueReport_returnsOk() throws Exception {
        OverdueReportResponse response = new OverdueReportResponse();
        response.setRegion("South");

        when(reportService.getOverdueReport("South", "0-30")).thenReturn(response);

        mockMvc.perform(get("/billing/reports/overdue")
                        .param("region", "South")
                        .param("agingBucket", "0-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.region").value("South"));
    }

    @Test
    void getCollectionReport_returnsOk() throws Exception {
        CollectionReportResponse response = new CollectionReportResponse();
        response.setRegion("South");

        when(reportService.getCollectionReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "South"))
                .thenReturn(response);

        mockMvc.perform(get("/billing/reports/collection")
                        .param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-31")
                        .param("region", "South"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.region").value("South"));
    }

    @Test
    void getDisputeSummary_returnsOk() throws Exception {
        DisputeSummaryResponse response = new DisputeSummaryResponse();
        response.setTotalDisputes(1);

        when(reportService.getDisputeSummary(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .thenReturn(response);

        mockMvc.perform(get("/billing/reports/disputes/summary")
                        .param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDisputes").value(1));
    }
}
