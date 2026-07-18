package com.teleconnect.iam.service;

import lombok.extern.slf4j.Slf4j;

import com.teleconnect.iam.dto.request.*;
import com.teleconnect.iam.dto.response.*;
import com.teleconnect.iam.entity.Permission;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.entity.User;
import com.teleconnect.iam.repository.RoleRepository;
import com.teleconnect.iam.repository.UserRepository;
import com.teleconnect.iam.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepo, RoleRepository roleRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, AuditLogService auditLogService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditLogService = auditLogService;
    }

    @Value("${app.default.staff.password}")
    private String defaultPassword;

    private List<String> getPermissions(Role role) {
        return role.getPermissions().stream()
            .map(Permission::getPermissionName)
            .collect(Collectors.toList());
    }

    // -- REGISTER ---------------------------------------------
    public RegisterResponseDTO register(RegisterRequest req) {
        log.info("Register request received for email={}", req.getEmail());
        if (userRepo.existsByEmail(req.getEmail())) {
            log.warn("Registration failed - email already in use: {}", req.getEmail());
            throw new RuntimeException("Email already in use");
        }

        Role role = roleRepo.findByRoleName("S")
            .orElseThrow(() -> new RuntimeException(
                "Role S (Subscriber) not found - ensure DataLoader has run"));

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setRole(role);
        user.setRegionId(req.getRegionId());
        user.setMustChangePassword(false);

        User saved = userRepo.save(user);
        auditLogService.log(saved.getUserId(), "USER_REGISTERED", "IAM", "N/A");
        log.info("Registration successful for userId={}", saved.getUserId());
        return new RegisterResponseDTO("Registration successful");
    }

    // -- LOGIN ------------------------------------------------
    public LoginResponseDTO login(LoginRequest req, String ip) {
        log.info("Login attempt for email={}", req.getEmail());
        User user = userRepo.findByEmail(req.getEmail())
            .orElseThrow(() -> {
                log.warn("Login failed - user not found: {}", req.getEmail());
                return new RuntimeException("User not found");
            });

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid credentials");

        if (user.getStatus() != User.Status.A)
            throw new RuntimeException("Account is not active");

        List<String> permissions = getPermissions(user.getRole());
        String token = jwtUtil.generateToken(user.getEmail(), permissions);
        auditLogService.log(user.getUserId(), "USER_LOGIN", "IAM", ip);
        log.info("Login successful for userId={}", user.getUserId());

        return new LoginResponseDTO(
            token,
            user.getRole().getRoleName(),
            user.getName(),
            user.getMustChangePassword(),
            permissions
        );
    }

    // -- toDTO helper -----------------------------------------
    private UserResponseDTO toDTO(User u) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(u.getUserId());
        dto.setName(u.getName());
        dto.setEmail(u.getEmail());
        dto.setPhone(u.getPhone());
        dto.setRoleName(u.getRole().getRoleName());
        dto.setRegionId(u.getRegionId());
        dto.setStatus(u.getStatus().name());
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }

    // -- CHANGE PASSWORD --------------------------------------
    public MessageDTO changePassword(String email, ChangePasswordRequest req) {
        log.info("Change password requested for email={}", email);
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("Change password failed - user not found: {}", email);
                return new RuntimeException("User not found");
            });

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            log.warn("Change password failed - incorrect current password for userId={}", user.getUserId());
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setMustChangePassword(false);
        userRepo.save(user);

        auditLogService.log(user.getUserId(), "PASSWORD_CHANGED", "IAM", "N/A");
        log.info("Password changed for userId={}", user.getUserId());
        return new MessageDTO("Password changed successfully");
    }

    // -- GET OWN PROFILE --------------------------------------
    public UserResponseDTO getOwnProfile(String email) {
        log.debug("Fetching own profile for email={}", email);
        UserResponseDTO dto = toDTO(userRepo.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("Own profile not found for email={}", email);
                return new RuntimeException("User not found");
            }));
        log.debug("Own profile retrieved for email={}", email);
        return dto;
    }

    // -- GET USER BY ID ---------------------------------------
    public UserResponseDTO getUserById(Long id) {
        log.debug("Fetching user by id={}", id);
        UserResponseDTO dto = toDTO(userRepo.findById(id)
            .orElseThrow(() -> {
                log.warn("User not found for id={}", id);
                return new RuntimeException("User not found");
            }));
        log.debug("User retrieved id={}", id);
        return dto;
    }

    // -- UPDATE USER ------------------------------------------
    public MessageDTO updateUser(Long id, UpdateUserRequest req, boolean isAdmin) {
        log.info("Update request for userId={} by {}", id, isAdmin ? "ADMIN" : "SELF");
        User user = userRepo.findById(id)
            .orElseThrow(() -> {
                log.warn("Update failed - user not found id={}", id);
                return new RuntimeException("User not found");
            });

        if (req.getName() != null) {
            user.setName(req.getName());
            log.debug("Updated name for userId={} to {}", id, req.getName());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
            log.debug("Updated phone for userId={}", id);
        }

        if (isAdmin) {
            if (req.getRegionId() != null) {
                user.setRegionId(req.getRegionId());
                log.debug("Admin updated region for userId={} to {}", id, req.getRegionId());
            }
            if (req.getRoleName() != null) {
                Role role = roleRepo.findByRoleName(req.getRoleName().toUpperCase())
                    .orElseThrow(() -> {
                        log.warn("Role not found: {}", req.getRoleName());
                        return new RuntimeException("Role not found");
                    });
                user.setRole(role);
                log.info("Admin changed role for userId={} to {}", id, req.getRoleName());
            }
        }

        userRepo.save(user);
        log.info("User updated successfully userId={}", id);
        return new MessageDTO("User updated successfully");
    }

    // -- GET ALL USERS ----------------------------------------
    public List<UserResponseDTO> getAllUsers() {
        log.info("Fetching all users");
        List<UserResponseDTO> res = userRepo.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        log.debug("Retrieved {} users", res.size());
        return res;
    }

    // -- UPDATE STATUS ----------------------------------------
    public MessageDTO updateStatus(Long id, UpdateStatusRequest req) {
        log.info("Update status requested for userId={} to {}", id, req.getStatus());
        User user = userRepo.findById(id)
            .orElseThrow(() -> {
                log.warn("Status update failed - user not found id={}", id);
                return new RuntimeException("User not found");
            });

        try {
            user.setStatus(User.Status.valueOf(req.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid status provided: {}", req.getStatus());
            throw new RuntimeException("Invalid status: " + req.getStatus());
        }

        userRepo.save(user);
        auditLogService.log(id, "USER_STATUS_CHANGED", "IAM", "ADMIN");
        log.info("User status updated userId={} to {}", id, req.getStatus().toUpperCase());
        return new MessageDTO("User status updated to " + req.getStatus().toUpperCase());
    }

    // -- RESET PASSWORD ---------------------------------------
    public MessageDTO resetPassword(Long id) {
        log.info("Password reset requested for userId={}", id);
        User user = userRepo.findById(id)
            .orElseThrow(() -> {
                log.warn("Password reset failed - user not found id={}", id);
                return new RuntimeException("User not found");
            });

        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setMustChangePassword(true);
        userRepo.save(user);

        auditLogService.log(id, "PASSWORD_RESET", "IAM", "ADMIN");
        log.warn("Password reset to default for userId={}", id);
        return new MessageDTO("Password reset to default successfully");
    }

    // -- SEARCH -----------------------------------------------
    public List<UserResponseDTO> searchUsers(String name, String email,
                                             String phone, String status, String roleName) {
        return userRepo.findAll().stream()
            .filter(u -> name == null || u.getName().toLowerCase().contains(name.toLowerCase()))
            .filter(u -> email == null || u.getEmail().toLowerCase().contains(email.toLowerCase()))
            .filter(u -> phone == null || (u.getPhone() != null && u.getPhone().equals(phone)))
            .filter(u -> status == null || u.getStatus().getCode().equalsIgnoreCase(status)
                                        || u.getStatus().getLabel().equalsIgnoreCase(status))
            .filter(u -> roleName == null || u.getRole().getRoleName().equalsIgnoreCase(roleName))
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    // -- CREATE STAFF -----------------------------------------
    public RegisterResponseDTO createStaff(CreateStaffRequest req) {
        log.info("Creating staff account for email={} role={}", req.getEmail(), req.getRoleName());
        if (userRepo.existsByEmail(req.getEmail())) {
            log.warn("Staff creation failed - email already exists: {}", req.getEmail());
            throw new RuntimeException("Email already in use");
        }

        Role role = roleRepo.findByRoleName(req.getRoleName().toUpperCase())
            .orElseThrow(() -> {
                log.error("Role not found for staff creation: {}", req.getRoleName());
                return new RuntimeException("Role not found: " + req.getRoleName());
            });

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setRole(role);
        user.setRegionId(req.getRegionId());
        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setMustChangePassword(true);

        User saved = userRepo.save(user);
        auditLogService.log(saved.getUserId(), "STAFF_ACCOUNT_CREATED", "IAM", "ADMIN");
        log.info("Staff account created successfully userId={}", saved.getUserId());
        return new RegisterResponseDTO("Staff account created successfully");
    }

    // -- helper used by controllers to avoid direct repository access ----------
    public Long findUserIdByEmail(String email) {
        return userRepo.findByEmail(email).map(User::getUserId).orElse(null);
    }

    public List<Role> getAllRoles() {
        return roleRepo.findAll();
    }

    public Role getRoleById(Integer id) {
        return roleRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found"));
    }
}
