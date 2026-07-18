package com.teleconnect.billing_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.billing_service.dto.request.DisputeRequest;
import com.teleconnect.billing_service.dto.response.DisputeResponse;
import com.teleconnect.billing_service.service.BillingDisputeService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BillingDisputeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.teleconnect.billing_service.security.JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class BillingDisputeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BillingDisputeService disputeService;

    @MockBean
    private AuditClient auditClient;

    @Test
    void raiseDispute_returnsCreated() throws Exception {
        DisputeRequest request = new DisputeRequest();
        request.setInvoiceId(100L);
        request.setDisputeReason("ExcessData");
        request.setDisputedAmount(java.math.BigDecimal.valueOf(25.00));
        request.setDescription("Data charge error");

        mockMvc.perform(post("/billing/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Billing dispute raised successfully"));
    }

    @Test
    void getDisputesByInvoice_returnsOk() throws Exception {
        DisputeResponse response = new DisputeResponse();
        response.setDisputeId(50L);

        when(disputeService.getDisputesByInvoice(100L)).thenReturn(List.of(response));

        mockMvc.perform(get("/billing/disputes/invoice/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disputeId").value(50));
    }
}
