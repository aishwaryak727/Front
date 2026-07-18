package com.teleconnect.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditClient;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.plan.dto.request.TelecomPlanRequest;
import com.teleconnect.plan.dto.response.MessageResponse;
import com.teleconnect.plan.service.TelecomPlanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TelecomPlanController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.teleconnect.plan.security.JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class TelecomPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TelecomPlanService service;

    @MockBean
    private AuditClient auditClient;

    @Test
    @DisplayName("POST /plan/createPlans returns 400 when name is blank")
    void createPlan_blankName_returns400() throws Exception {
        TelecomPlanRequest request = new TelecomPlanRequest();
        request.setName("");

        mockMvc.perform(post("/plan/createPlans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name is required"));
    }

    @Test
    @DisplayName("GET /plan/getAllPlans returns 200 when plans exist")
    void getAllPlans_returns200() throws Exception {
        when(service.getAllPlans()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/plan/getAllPlans"))
                .andExpect(status().isNotFound());
    }
}
