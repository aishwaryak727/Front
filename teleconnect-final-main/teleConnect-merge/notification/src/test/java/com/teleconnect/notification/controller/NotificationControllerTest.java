package com.teleconnect.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.notification.dto.request.NotificationRequest;
import com.teleconnect.notification.dto.response.NotificationResponse;
import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.entity.enums.NotificationStatus;
import com.teleconnect.notification.exception.ResourceNotFoundException;
import com.teleconnect.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.teleconnect.common.audit.AuditClient;
import com.teleconnect.notification.security.JwtUtil;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService service;

        @MockBean
        private AuditClient auditClient;

        @MockBean
        private JwtUtil jwtUtil;

    @Test
    @DisplayName("POST /teleConnect/notification/createNotification returns 201")
    void createNotification_returns201() throws Exception {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(201L);
        request.setMessage("Test Notification");
        request.setCategory(NotificationCategory.FAULT);

        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(1L);
        response.setUserId(201L);
        response.setMessage("Test Notification");
        response.setCategory(NotificationCategory.FAULT);
        response.setStatus(NotificationStatus.UNREAD);
        response.setCreatedDate(LocalDateTime.now());

        when(service.createNotification(any(NotificationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/teleConnect/notification/createNotification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").value("Notification created successfully"));
    }

    @Test
    @DisplayName("GET /teleConnect/notification/fetchNotificationById/{id} unknown id -> 404")
    void getNotificationById_unknown_returns404() throws Exception {
        when(service.getNotificationById(999L))
                .thenThrow(new ResourceNotFoundException("Notification not found"));

        mockMvc.perform(get("/teleConnect/notification/fetchNotificationById/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found"));
    }
}
