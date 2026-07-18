package com.teleconnect.usage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.common.audit.AuditClient;
import com.teleconnect.usage.dto.response.UsageSummaryResponse;
import com.teleconnect.usage.security.JwtUtil;
import com.teleconnect.usage.service.UsageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsageController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsageService usageService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuditClient auditClient;

    @Test
    @DisplayName("GET /teleConnect/usage/fetchSummary/{line}/{cycle} returns 200")
    void fetchSummary_returns200() throws Exception {
        UsageSummaryResponse response = new UsageSummaryResponse();
        response.setLineId(42L);
        response.setBillingCycleId(99L);
        response.setDataUsedMb(BigDecimal.valueOf(50));

        when(usageService.fetchSummary(42L, 99L)).thenReturn(response);

        mockMvc.perform(get("/teleConnect/usage/fetchSummary/42/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineId").value(42));
    }
}
