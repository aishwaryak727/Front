package com.teleconnect.subscriber.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.subscriber.dto.request.CreateAccountRequest;
import com.teleconnect.subscriber.dto.response.MessageDTO;
import com.teleconnect.common.audit.AuditClient;
import com.teleconnect.subscriber.security.JwtUtil;
import com.teleconnect.subscriber.service.SubscriberAccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubscriberAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriberAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriberAccountService accountService;

    @MockBean
    private AuditClient auditClient;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("POST /teleConnect/api/subscribers returns 201 on success")
    void createAccount_returns201() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setSubscriberId(101L);
        request.setAccountType("Prepaid");
        request.setKycStatus("Pending");

        when(accountService.createAccount(any(CreateAccountRequest.class)))
                .thenReturn(new MessageDTO("Account created successfully"));

        mockMvc.perform(post("/teleConnect/api/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Account created successfully"));
    }
}
