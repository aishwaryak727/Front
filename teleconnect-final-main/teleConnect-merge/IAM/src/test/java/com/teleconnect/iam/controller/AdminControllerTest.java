package com.teleconnect.iam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.iam.dto.request.CreateStaffRequest;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.service.AuditLogService;
import com.teleconnect.iam.service.UserService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.teleconnect.iam.security.JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuditLogService auditLogService;

    @Test
    @DisplayName("GET /teleConnect/iam/api/roles/{id}/permissions returns 400 when role is absent")
    void getRolePermissions_unknownRole_returns400() throws Exception {
        when(userService.getRoleById(99)).thenThrow(new RuntimeException("Role not found"));

        mockMvc.perform(get("/teleConnect/iam/api/roles/99/permissions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Role not found"));
    }

    @Test
    @DisplayName("POST /teleConnect/iam/api/admin/users/createStaff returns 400 when duplicate email")
    void createStaff_duplicateEmail_returns400() throws Exception {
        when(userService.createStaff(any(CreateStaffRequest.class)))
                .thenThrow(new RuntimeException("Email already in use"));

        CreateStaffRequest request = new CreateStaffRequest();
        request.setName("Jane Staff");
        request.setEmail("dup@test.com");
        request.setPhone("9999999999");
        request.setRoleName("CS");

        mockMvc.perform(post("/teleConnect/iam/api/admin/users/createStaff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }
}
