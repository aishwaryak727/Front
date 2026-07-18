package com.teleconnect.iam.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.iam.dto.request.UpdateUserRequest;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.dto.response.UserResponseDTO;
import com.teleconnect.iam.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/teleConnect/iam/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    

    // GET /users/me — any logged-in user
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMe(Principal principal) {
        log.info("Getting own profile for user={}", principal.getName());
        UserResponseDTO dto = userService.getOwnProfile(principal.getName());
        log.debug("Own profile retrieved successfully");
        return ResponseEntity.ok(dto);
    }

    // GET /users/{id} — Subscriber(own via VIEW logic), CS Agent, Network Ops, Admin
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VIEW_SUBSCRIBER','VIEW_ALL_USERS','VIEW_NETWORK_FAULTS')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        log.info("Getting user by id={}", id);
        UserResponseDTO dto = userService.getUserById(id);
        log.debug("User retrieved id={}", id);
        return ResponseEntity.ok(dto);
    }

    // PUT /users/{id} — Subscriber(own name+phone), Admin(all fields)
    @PutMapping("/{id}")
    public ResponseEntity<MessageDTO> updateUser(@PathVariable Long id,
                                                 @RequestBody UpdateUserRequest req,
                                                 Authentication auth) {
        log.info("Updating user id={}", id);
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("VIEW_ALL_USERS"));
        log.debug("Update user isAdmin={}", isAdmin);
        MessageDTO msg = userService.updateUser(id, req, isAdmin);
        log.info("User updated id={}", id);
        return ResponseEntity.ok(msg);
    }

    // GET /users — Admin only (VIEW_ALL_USERS)
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        log.info("Fetching all users");
        List<UserResponseDTO> users = userService.getAllUsers();
        log.info("Retrieved {} users", users.size());
        return ResponseEntity.ok(users);
    }

    // GET /users/search — Admin + CS Agent
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER')")
    public ResponseEntity<List<UserResponseDTO>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {
        log.info("Searching users with filters name={} email={} phone={} status={} role={}", name, email, phone, status, role);
        List<UserResponseDTO> results = userService.searchUsers(name, email, phone, status, role);
        log.info("Search returned {} results", results.size());
        return ResponseEntity.ok(results);
    }
}
