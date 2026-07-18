package com.teleconnect.billing_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.billing_service.dto.request.BillingCycleRequest;
import com.teleconnect.billing_service.dto.request.CycleGenerationRequest;
import com.teleconnect.billing_service.dto.response.BatchGenerationResponse;
import com.teleconnect.billing_service.dto.response.BillingCycleResponse;
import com.teleconnect.billing_service.dto.response.MessageResponse;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import com.teleconnect.billing_service.service.BillingCycleService;
import com.teleconnect.common.audit.AuditClient;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BillingCycleController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.teleconnect.billing_service.security.JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class BillingCycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BillingCycleService billingCycleService;

    @MockBean
    private AuditClient auditClient;

    @Test
    void createBillingCycle_returnsCreated() throws Exception {
        BillingCycleRequest request = new BillingCycleRequest();
        request.setAccountId(1L);
        request.setCycleStart(LocalDate.now());
        request.setCycleEnd(LocalDate.now().plusDays(30));

        BillingCycleResponse response = new BillingCycleResponse();
        response.setCycleId(100L);

        when(billingCycleService.createBillingCycle(any(BillingCycleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/billing/cycles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cycleId").value(100));
    }

    @Test
    void generateInvoices_returnsOk() throws Exception {
        CycleGenerationRequest request = new CycleGenerationRequest();
        request.setCycleDate(LocalDate.now());
        request.setDryRun(true);

        when(billingCycleService.generateInvoicesBatch(any(CycleGenerationRequest.class)))
                .thenReturn(new BatchGenerationResponse());

        mockMvc.perform(post("/billing/cycles/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invoice generation completed successfully"));
    }

    @Test
    void getBillingCycle_returnsOk() throws Exception {
        BillingCycleResponse response = new BillingCycleResponse();
        response.setCycleId(123L);

        when(billingCycleService.getBillingCycleById(123L)).thenReturn(response);

        mockMvc.perform(get("/billing/cycles/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycleId").value(123));
    }
}
