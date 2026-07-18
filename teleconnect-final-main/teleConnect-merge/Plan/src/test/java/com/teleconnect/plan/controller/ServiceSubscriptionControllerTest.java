package com.teleconnect.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.common.audit.AuditClient;
import com.teleconnect.plan.dto.request.ServiceSubscriptionRequest;
import com.teleconnect.plan.dto.response.MessageResponse;
import com.teleconnect.plan.dto.response.ServiceSubscriptionResponse;
import com.teleconnect.plan.service.ServiceSubscriptionService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ServiceSubscriptionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.teleconnect.plan.security.JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class ServiceSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServiceSubscriptionService service;

    @MockBean
    private AuditClient auditClient;

    @Test
    void createSubscription_returns201() throws Exception {
        ServiceSubscriptionRequest request = new ServiceSubscriptionRequest();
        request.setLineId(1);
        request.setPlanId(2);
        request.setActivationDate(LocalDate.now());
        request.setExpiryDate(LocalDate.now().plusDays(30));
        request.setRenewalType("AutoRenew");

        mockMvc.perform(post("/plan/createSubscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Subscription created successfully"));
    }

    @Test
    void getSubscriptionById_returnsNotFoundWhenMissing() throws Exception {
        when(service.getById(99)).thenReturn(null);

        mockMvc.perform(get("/plan/getSubscriptions/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Subscription with subscriptionId 99 not found"));
    }
}
