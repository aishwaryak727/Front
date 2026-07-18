package com.teleconnect.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.common.audit.AuditClient;
import com.teleconnect.plan.dto.request.AddOnRequest;
import com.teleconnect.plan.dto.response.AddOnResponse;
import com.teleconnect.plan.dto.response.MessageResponse;
import com.teleconnect.plan.service.AddOnService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AddOnController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.teleconnect.plan.security.JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AddOnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AddOnService service;

    @MockBean
    private AuditClient auditClient;

    @Test
    void createAddOn_returns201() throws Exception {
        AddOnRequest request = new AddOnRequest();
        request.setName("Data Pack");
        request.setType("DataTopup");
        request.setQuota(BigDecimal.valueOf(5));
        request.setValidityDays(30);
        request.setPrice(BigDecimal.valueOf(99.99));

        mockMvc.perform(post("/plan/createAddOns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Add-on created successfully"));
    }

    @Test
    void getAddOnById_returnsNotFoundWhenNull() throws Exception {
        when(service.getAddOnById(99)).thenReturn(null);

        mockMvc.perform(get("/plan/getAddOns/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Add-on with addOnId 99 not found"));
    }
}
