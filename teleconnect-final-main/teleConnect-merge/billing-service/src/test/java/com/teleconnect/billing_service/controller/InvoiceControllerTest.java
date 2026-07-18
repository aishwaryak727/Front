package com.teleconnect.billing_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.billing_service.dto.request.InvoiceGenerationRequest;
import com.teleconnect.billing_service.dto.response.InvoiceResponse;
import com.teleconnect.billing_service.service.InvoiceService;
import com.teleconnect.common.audit.AuditClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InvoiceController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.teleconnect.billing_service.security.JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InvoiceService invoiceService;

    @MockBean
    private AuditClient auditClient;

    @Test
    @DisplayName("POST /billing/invoices/generate returns 201")
    void generateInvoice_returns201() throws Exception {
        InvoiceGenerationRequest request = new InvoiceGenerationRequest();
        request.setAccountId(5L);
        request.setCycleId(10L);
        request.setPlanCharges(BigDecimal.valueOf(200.0));
        request.setExcessCharges(BigDecimal.valueOf(25.0));
        request.setAddOnCharges(BigDecimal.valueOf(15.0));
        request.setTaxes(BigDecimal.valueOf(10.0));

        InvoiceResponse response = new InvoiceResponse();
        response.setInvoiceId(1L);
        response.setAccountId(5L);
        response.setTotalAmount(BigDecimal.valueOf(250.0));

        when(invoiceService.generateInvoice(any(InvoiceGenerationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/billing/invoices/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceId").value(1));
    }

    @Test
    @DisplayName("GET /billing/invoices/{invoiceId} returns 200")
    void getInvoice_returns200() throws Exception {
        InvoiceResponse response = new InvoiceResponse();
        response.setInvoiceId(1L);
        response.setAccountId(5L);

        when(invoiceService.getInvoiceById(1L)).thenReturn(response);

        mockMvc.perform(get("/billing/invoices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(1));
    }
}
