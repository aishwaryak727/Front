package com.teleconnect.subscriber.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.common.audit.AuditClient;
import com.teleconnect.subscriber.dto.request.CreateSimLineRequest;
import com.teleconnect.subscriber.dto.request.ReplaceSimRequest;
import com.teleconnect.subscriber.dto.response.MessageDTO;
import com.teleconnect.subscriber.dto.response.SimLineResponseDTO;
import com.teleconnect.subscriber.service.SimLineService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SimLineController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = com.teleconnect.subscriber.security.JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class SimLineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SimLineService simLineService;

    @MockBean
    private AuditClient auditClient;

    @Test
    void createSimLine_returns201() throws Exception {
        CreateSimLineRequest request = new CreateSimLineRequest();
        request.setMsisdn("1234567890");
        request.setIccid("ICCID-1234");
        request.setServiceType("VoiceData");

        when(simLineService.createSimLine(eq(1), any(CreateSimLineRequest.class)))
                .thenReturn(new MessageDTO("SIM line activated successfully"));

        mockMvc.perform(post("/teleConnect/api/subscribers/1/simLines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("SIM line activated successfully"));
    }

    @Test
    void getSimLine_returns200() throws Exception {
        SimLineResponseDTO response = new SimLineResponseDTO();
        response.setLineId(5);
        response.setAccountId(1);
        response.setMsisdn("1234567890");

        when(simLineService.getSimLineById(1, 5)).thenReturn(response);

        mockMvc.perform(get("/teleConnect/api/subscribers/1/simLines/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineId").value(5));
    }

    @Test
    void lookupByMsisdn_returns200() throws Exception {
        SimLineResponseDTO response = new SimLineResponseDTO();
        response.setLineId(7);
        response.setMsisdn("1234567890");

        when(simLineService.lookupByMsisdn("1234567890")).thenReturn(response);

        mockMvc.perform(get("/teleConnect/api/subscribers/sim-lines/lookup")
                        .param("msisdn", "1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msisdn").value("1234567890"));
    }
}
