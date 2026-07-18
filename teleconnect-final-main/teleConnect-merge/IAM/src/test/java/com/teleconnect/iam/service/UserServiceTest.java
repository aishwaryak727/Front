package com.teleconnect.iam.service;

import com.teleconnect.iam.dto.request.CreateStaffRequest;
import com.teleconnect.iam.dto.response.RegisterResponseDTO;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.entity.User;
import com.teleconnect.iam.repository.RoleRepository;
import com.teleconnect.iam.repository.UserRepository;
import com.teleconnect.iam.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private RoleRepository roleRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    @Test
    void createStaff_duplicateEmail_throwsException() {
        when(userRepo.existsByEmail("dup@test.com")).thenReturn(true);

        CreateStaffRequest request = new CreateStaffRequest();
        request.setName("Jane Staff");
        request.setEmail("dup@test.com");
        request.setPhone("9999999999");
        request.setRoleName("CS");

        assertThrows(RuntimeException.class, () -> userService.createStaff(request));
    }

    @Test
    void createStaff_success_savesUser() {
        when(userRepo.existsByEmail("new@test.com")).thenReturn(false);
        Role role = new Role();
        role.setRoleName("CS");
        when(roleRepo.findByRoleName("CS")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(10L);
            return user;
        });

        CreateStaffRequest request = new CreateStaffRequest();
        request.setName("Jane Staff");
        request.setEmail("new@test.com");
        request.setPhone("9999999999");
        request.setRoleName("CS");

        RegisterResponseDTO response = userService.createStaff(request);

        assertNotNull(response);
        assertEquals("Staff account created successfully", response.getMessage());
        verify(auditLogService).log(10L, "STAFF_ACCOUNT_CREATED", "IAM", "ADMIN");
    }
}
