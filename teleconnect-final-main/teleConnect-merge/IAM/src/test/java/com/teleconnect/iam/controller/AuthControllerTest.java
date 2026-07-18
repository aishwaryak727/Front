package com.teleconnect.iam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.iam.dto.request.ChangePasswordRequest;
import com.teleconnect.iam.dto.request.LoginRequest;
import com.teleconnect.iam.dto.request.RegisterRequest;
import com.teleconnect.iam.dto.response.LoginResponseDTO;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.dto.response.RegisterResponseDTO;
import com.teleconnect.iam.service.AuditLogService;
import com.teleconnect.iam.service.UserService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.teleconnect.iam.security.JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuditLogService auditLogService;

    @Test
    void register_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setPhone("1234567890");
        request.setRegionId(1);

        RegisterResponseDTO response = new RegisterResponseDTO();
        response.setUserId(1L);
        response.setMessage("Registered successfully");

        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/teleConnect/iam/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registered successfully"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void login_returns200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("bob@example.com");
        request.setPassword("password123");

        LoginResponseDTO response = new LoginResponseDTO(
                "token-123",
                "ADMIN",
                "Bob User",
                false,
                java.util.List.of("VIEW_ALL_USERS"));

        when(userService.login(any(LoginRequest.class), any(String.class)))
                .thenReturn(response);

        mockMvc.perform(post("/teleConnect/iam/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-123"));
    }

    @Test
    void logout_returns200() throws Exception {
        when(userService.findUserIdByEmail("alice@example.com")).thenReturn(42L);

        mockMvc.perform(post("/teleConnect/iam/api/auth/logout")
                        .principal((java.security.Principal) () -> "alice@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void changePassword_returns200() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass");

        when(userService.changePassword(eq("alice@example.com"), any(ChangePasswordRequest.class)))
                .thenReturn(new MessageDTO("Password updated successfully"));

        mockMvc.perform(put("/teleConnect/iam/api/auth/changePassword")
                        .principal((java.security.Principal) () -> "alice@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));
    }
}
