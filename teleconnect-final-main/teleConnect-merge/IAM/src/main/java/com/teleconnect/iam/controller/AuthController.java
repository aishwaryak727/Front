package com.teleconnect.iam.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.iam.dto.request.ChangePasswordRequest;
import com.teleconnect.iam.dto.request.LoginRequest;
import com.teleconnect.iam.dto.request.RegisterRequest;
import com.teleconnect.iam.dto.response.LoginResponseDTO;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.dto.response.RegisterResponseDTO;
import com.teleconnect.iam.service.UserService;
import com.teleconnect.iam.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/teleConnect/iam/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public AuthController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    

    // POST /auth/register
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequest req) {
        log.info("Register request for email={}", req.getEmail());
        RegisterResponseDTO result = userService.register(req);
        log.info("User registered email={}", req.getEmail());
        return ResponseEntity.status(201).body(result);
    }

    // POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequest req,
                                                  HttpServletRequest http) {
        log.info("Login attempt for email={} from ip={}", req.getEmail(), http.getRemoteAddr());
        LoginResponseDTO result = userService.login(req, http.getRemoteAddr());
        log.info("Login successful for email={}", req.getEmail());
        return ResponseEntity.ok(result);
    }

    // POST /auth/logout — JWT is stateless, so just record the audit entry
    @PostMapping("/logout")
    public ResponseEntity<MessageDTO> logout(Principal principal) {
        log.info("Logout request for user={}", principal.getName());
        if (principal == null) {
            log.warn("Logout called without authentication");
            throw new RuntimeException("Not authenticated");
        }

        Long userId = userService.findUserIdByEmail(principal.getName());
        if (userId == null) {
            log.error("User not found for email={}", principal.getName());
            throw new RuntimeException("User not found");
        }

        // Pass the real userId so audit_logs.user_id is never null
        auditLogService.log(userId, "USER_LOGOUT", "IAM", "N/A");
        log.info("User logged out userId={}", userId);

        return ResponseEntity.ok(new MessageDTO("Logged out successfully"));
    }

    // PUT /auth/changePassword — any logged-in user; email comes from the JWT
    @PutMapping("/changePassword")
    public ResponseEntity<MessageDTO> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                                     Principal principal) {
        log.info("Change password request for user={}", principal.getName());
        if (principal == null) {
            log.warn("Change password called without authentication");
            throw new RuntimeException("Not authenticated");
        }
        MessageDTO result = userService.changePassword(principal.getName(), req);
        log.info("Password changed for user={}", principal.getName());
        return ResponseEntity.ok(result);
    }
}
