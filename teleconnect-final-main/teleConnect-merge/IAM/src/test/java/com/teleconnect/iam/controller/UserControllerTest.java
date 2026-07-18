package com.teleconnect.iam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.iam.dto.request.UpdateUserRequest;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.dto.response.UserResponseDTO;
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

import java.security.Principal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.teleconnect.iam.security.JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void getMe_returnsUserProfile() throws Exception {
        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(10L);
        user.setEmail("me@example.com");
        user.setName("Me User");

        when(userService.getOwnProfile("me@example.com")).thenReturn(user);

        mockMvc.perform(get("/teleConnect/iam/api/users/me")
                        .principal((Principal) () -> "me@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.email").value("me@example.com"));
    }

    @Test
    void getUserById_returnsUserProfile() throws Exception {
        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(5L);
        user.setEmail("user5@example.com");
        user.setName("User Five");

        when(userService.getUserById(5L)).thenReturn(user);

        mockMvc.perform(get("/teleConnect/iam/api/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.email").value("user5@example.com"));
    }
}
