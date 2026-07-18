package com.teleconnect.billing_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.PaymentResponse;
import com.teleconnect.billing_service.enums.PaymentMethod;
import com.teleconnect.billing_service.service.PaymentService;
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

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.teleconnect.billing_service.security.JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private AuditClient auditClient;

    @Test
    void makePayment_returnsCreated() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setInvoiceId(1L);
        request.setAmountPaid(BigDecimal.valueOf(100.00));
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setTransactionRef("TXN-001");

        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(10L);
        response.setInvoiceId(1L);

        when(paymentService.makePayment(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/billing/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(10));
    }

    @Test
    void getPaymentById_returnsOk() throws Exception {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(20L);

        when(paymentService.getPaymentById(20L)).thenReturn(response);

        mockMvc.perform(get("/billing/payments/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(20));
    }
}
