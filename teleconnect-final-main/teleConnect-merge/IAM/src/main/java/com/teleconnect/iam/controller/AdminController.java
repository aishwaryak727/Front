package com.teleconnect.iam.controller;

import lombok.extern.slf4j.Slf4j;

import com.teleconnect.iam.dto.request.AuditLogFilterDTO;
import com.teleconnect.iam.dto.request.AuditRecordRequest;
import com.teleconnect.iam.dto.request.CreateStaffRequest;
import com.teleconnect.iam.dto.request.UpdateStatusRequest;
import com.teleconnect.iam.dto.response.AuditLogResponseDTO;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.dto.response.RegisterResponseDTO;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.service.UserService;
import com.teleconnect.iam.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/teleConnect/iam/api")
public class AdminController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public AdminController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }


    // POST /admin/users/createStaff — Admin only (CREATE_USER permission)
    @PostMapping("/admin/users/createStaff")
    @PreAuthorize("hasAuthority('CREATE_USER')")
    public ResponseEntity<RegisterResponseDTO> createStaff(@Valid @RequestBody CreateStaffRequest req) {
        log.info("Creating staff user email={} role={}", req.getEmail(), req.getRoleName());
        RegisterResponseDTO result = userService.createStaff(req);
        log.info("Staff user created email={}", req.getEmail());
        return ResponseEntity.status(201).body(result);
    }

    // PUT /users/{id}/status — Admin only (DELETE_USER)
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('DELETE_USER')")
    public ResponseEntity<MessageDTO> updateStatus(@PathVariable Long id,
                                                   @RequestBody UpdateStatusRequest req) {
        log.info("Updating user status id={} status={}", id, req.getStatus());
        MessageDTO result = userService.updateStatus(id, req);
        log.info("User status updated id={} status={}", id, req.getStatus());
        return ResponseEntity.ok(result);
    }

    // PUT /admin/users/{id}/resetPassword — Admin only (CREATE_USER)
    @PutMapping("/admin/users/{id}/resetPassword")
    @PreAuthorize("hasAuthority('CREATE_USER')")
    public ResponseEntity<MessageDTO> resetPassword(@PathVariable Long id) {
        log.info("Resetting password for user id={}", id);
        MessageDTO result = userService.resetPassword(id);
        log.info("Password reset for user id={}", id);
        return ResponseEntity.ok(result);
    }

    // GET /roles — Admin only (VIEW_ALL_USERS)
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    public ResponseEntity<List<Role>> getRoles() {
        log.info("Fetching all roles");
        List<Role> roles = userService.getAllRoles();
        log.info("Retrieved {} roles", roles.size());
        return ResponseEntity.ok(roles);
    }

    // GET /roles/{roleId}/permissions — Admin only (VIEW_ALL_USERS)
    @GetMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    public ResponseEntity<Role> getRolePermissions(@PathVariable Integer roleId) {
        log.info("Fetching permissions for roleId={}", roleId);
        Role role = userService.getRoleById(roleId);
        log.debug("Role retrieved roleId={} name={}", roleId, role.getRoleName());
        return ResponseEntity.ok(role);
    }

    // GET /auditLogs — Compliance + Admin (VIEW_AUDIT_LOGS)
    @GetMapping("/auditLogs")
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
    public ResponseEntity<Page<AuditLogResponseDTO>> getAllLogs(@ModelAttribute AuditLogFilterDTO filter) {
        log.info("Fetching all audit logs with filter");
        Page<AuditLogResponseDTO> logs = auditLogService.getAllLogs(filter);
        log.info("Retrieved {} audit log entries", logs.getTotalElements());
        return ResponseEntity.ok(logs);
    }

    // GET /auditLogs/user/{userId} — Compliance + Admin (VIEW_AUDIT_LOGS)
    @GetMapping("/auditLogs/user/{userId}")
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
    public ResponseEntity<Page<AuditLogResponseDTO>> getLogsByUser(@PathVariable Long userId,
                                                                   @ModelAttribute AuditLogFilterDTO filter) {
        log.info("Fetching audit logs for userId={}", userId);
        Page<AuditLogResponseDTO> logs = auditLogService.getLogsByUser(userId, filter);
        log.info("Retrieved {} audit entries for userId={}", logs.getTotalElements(), userId);
        return ResponseEntity.ok(logs);
    }

    // POST /auditLogs — central audit sink for ALL modules.
    // Any authenticated caller may record an action; the userId is resolved here
    // from the JWT (email -> userId) so callers never need to know the numeric id.
    @PostMapping("/auditLogs")
    public ResponseEntity<Void> recordAudit(@RequestBody AuditRecordRequest req,
                                            HttpServletRequest httpReq) {
        log.info("Recording audit log action={} module={}", req.getAction(), req.getModule());
        Long userId = resolveCurrentUserId();
        String ip = (req.getIpAddress() != null && !req.getIpAddress().isBlank())
                ? req.getIpAddress()
                : httpReq.getRemoteAddr();
        auditLogService.log(userId, req.getAction(), req.getModule(), ip);
        log.debug("Audit log recorded userId={} action={} ip={}", userId, req.getAction(), ip);
        return ResponseEntity.status(201).build();
    }

    // Resolve the numeric userId of the currently authenticated principal (email).
    private Long resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userService.findUserIdByEmail(auth.getName());
    }
}
