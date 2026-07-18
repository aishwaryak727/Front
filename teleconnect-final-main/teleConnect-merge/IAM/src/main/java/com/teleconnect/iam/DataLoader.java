package com.teleconnect.iam;

import com.teleconnect.iam.entity.Permission;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.entity.User;
import com.teleconnect.iam.repository.PermissionRepository;
import com.teleconnect.iam.repository.RoleRepository;
import com.teleconnect.iam.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component // must run before SubscriberSeeder (it needs the "S" role to exist)
public class DataLoader implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(RoleRepository roleRepo, PermissionRepository permRepo,
                      UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.roleRepo = roleRepo;
        this.permRepo = permRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // 1. Create all 18 permissions if they do not exist
        List<String> permNames = List.of(
            "VIEW_PLAN", "PAY_BILL", "RAISE_SERVICE_REQUEST",
            "VIEW_SUBSCRIBER", "CREATE_FAULT_TICKET", "UPDATE_FAULT_TICKET",
            "VIEW_INVOICE", "EDIT_INVOICE", "RAISE_DISPUTE",
            "VIEW_NETWORK_FAULTS", "CLOSE_FAULT_TICKET",
            "VIEW_AUDIT_LOGS", "VIEW_REPORTS", "VIEW_KYC",
            "CREATE_USER", "DELETE_USER", "MANAGE_PLANS",
            "VIEW_ALL_USERS","USAGE_RECORDS","USAGE_ANALYTICS",
            "KYC_EXPIRE","CREATE_SUB","GET_SUB","BILLING_CYCLE",
            "BILLING_REPORT","BILLING_DISPUTE","EDIT_DISPUTE","CREATE_NOTIFICATION",
            "VIEW_NOTIFICATIONS","MARK_NOTIFICATIONS","SERVICE_REQUEST","GET_UPDATE_TICKET","RESOLVE_TICKET",
            "VIEW_OWN_PLAN",
            "VIEW_REPORT_ARPU","VIEW_REPORT_CHURN","VIEW_REPORT_NETWORK_UTILISATION",
            "VIEW_REPORT_SLA_COMPLIANCE","VIEW_REPORT_COLLECTION_EFFICIENCY",
            "VIEW_REPORT_SUBSCRIBER_GROWTH","GENERATE_REPORT"
        );

        Map<String, Permission> permMap = new HashMap<>();
        for (String name : permNames) {
            try {
                Permission p = permRepo.findByPermissionName(name).orElseGet(() -> {
                    Permission np = new Permission();
                    np.setPermissionName(name);
                    return permRepo.save(np);
                });
                permMap.put(name, p);
                log.info("[TeleConnect IAM] Created permission: {}", name);
            } catch (Exception e) {
                log.error("[TeleConnect IAM] ERROR creating permission '{}'", name, e);
            }
        }
        log.info("[TeleConnect IAM] Total permissions created: {} out of {}", permMap.size(), permNames.size());

        // Helper
        java.util.function.Function<String[], List<Permission>> perms =
            names -> Arrays.stream(names).map(permMap::get).toList();

        // 2. Create all 6 roles with their permissions if they do not exist
        createRole("S",   perms.apply(new String[]{"VIEW_PLAN", "PAY_BILL", "RAISE_SERVICE_REQUEST","USAGE_RECORDS","GET_SUB","VIEW_INVOICE","BILLING_DISPUTE","MARK_NOTIFICATIONS","VIEW_NOTIFICATIONS","VIEW_OWN_PLAN"}));
        createRole("CS",  perms.apply(new String[]{"VIEW_SUBSCRIBER", "CREATE_FAULT_TICKET", "UPDATE_FAULT_TICKET","USAGE_RECORDS","USAGE_ANALYTICS","VIEW_KYC","VIEW_PLAN","CREATE_SUB","GET_SUB","VIEW_NOTIFICATIONS","SERVICE_REQUEST","GET_UPDATE_TICKET"}));
        createRole("B",   perms.apply(new String[]{"VIEW_INVOICE", "EDIT_INVOICE", "RAISE_DISPUTE","USAGE_RECORDS","USAGE_ANALYTICS","VIEW_SUBSCRIBER","VIEW_PLAN","GET_SUB","BILLING_CYCLE","PAY_BILL","BILLING_REPORT","BILLING_DISPUTE","EDIT_DISPUTE","VIEW_NOTIFICATIONS",
                                                           "VIEW_REPORT_ARPU","VIEW_REPORT_COLLECTION_EFFICIENCY","GENERATE_REPORT"}));
        createRole("N",   perms.apply(new String[]{"VIEW_NETWORK_FAULTS", "CLOSE_FAULT_TICKET","USAGE_ANALYTICS","VIEW_PLAN","VIEW_NOTIFICATIONS","GET_UPDATE_TICKET","RESOLVE_TICKET",
                                                           "VIEW_REPORT_NETWORK_UTILISATION","VIEW_REPORT_SLA_COMPLIANCE","GENERATE_REPORT"}));
        createRole("C",   perms.apply(new String[]{"VIEW_AUDIT_LOGS", "VIEW_REPORTS","USAGE_RECORDS","USAGE_ANALYTICS","VIEW_PLAN","GET_SUB","VIEW_NOTIFICATIONS",
                                                           "VIEW_REPORT_CHURN","VIEW_REPORT_SLA_COMPLIANCE","VIEW_REPORT_SUBSCRIBER_GROWTH"}));
        createRole("A",   perms.apply(new String[]{"CREATE_USER", "DELETE_USER", "MANAGE_PLANS", "VIEW_ALL_USERS", "USAGE_RECORDS","USAGE_ANALYTICS","VIEW_SUBSCRIBER","VIEW_KYC","KYC_EXPIRE","VIEW_PLAN",
                                                           "CREATE_SUB","GET_SUB","BILLING_CYCLE","BILLING_REPORT","VIEW_INVOICE", "EDIT_INVOICE",
                                                           "PAY_BILL","BILLING_DISPUTE","EDIT_DISPUTE","CREATE_NOTIFICATION","VIEW_NOTIFICATIONS","MARK_NOTIFICATIONS","SERVICE_REQUEST","GET_UPDATE_TICKET","RESOLVE_TICKET",
                                                           "VIEW_REPORT_ARPU","VIEW_REPORT_CHURN","VIEW_REPORT_NETWORK_UTILISATION","VIEW_REPORT_SLA_COMPLIANCE",
                                                           "VIEW_REPORT_COLLECTION_EFFICIENCY","VIEW_REPORT_SUBSCRIBER_GROWTH","GENERATE_REPORT","VIEW_AUDIT_LOGS"}));

        log.info("[TeleConnect IAM] Roles and permissions seeded successfully.");

        // 3. Create bootstrap users if they don't exist
        createBootstrapUsers();
    }

    private void createBootstrapUsers() {
        try {
            // Create bootstrap admin user
            if (!userRepo.existsByEmail("admin@teleconnect.com")) {
                Role adminRole = roleRepo.findByRoleName("A")
                    .orElseThrow(() -> new RuntimeException("Admin role (A) not found"));

                User adminUser = new User();
                adminUser.setName("Administrator");
                adminUser.setEmail("admin@teleconnect.com");
                adminUser.setPhone("0000000000");
                adminUser.setPassword(passwordEncoder.encode("Admin@123"));
                adminUser.setRole(adminRole);
                adminUser.setRegionId(1);
                adminUser.setStatus(User.Status.A);
                adminUser.setMustChangePassword(true);
                userRepo.save(adminUser);
                log.info("[TeleConnect IAM] Created bootstrap admin user: admin@teleconnect.com (password: Admin@123)");
            } else {
                log.info("[TeleConnect IAM] Bootstrap admin user already exists.");
            }

            // Create bootstrap CS user
            if (!userRepo.existsByEmail("cs@teleconnect.com")) {
                Role csRole = roleRepo.findByRoleName("CS")
                    .orElseThrow(() -> new RuntimeException("CS role not found"));

                User csUser = new User();
                csUser.setName("Customer Service Agent");
                csUser.setEmail("cs@teleconnect.com");
                csUser.setPhone("0000000001");
                csUser.setPassword(passwordEncoder.encode("CS@123456"));
                csUser.setRole(csRole);
                csUser.setRegionId(1);
                csUser.setStatus(User.Status.A);
                csUser.setMustChangePassword(true);
                userRepo.save(csUser);
                log.info("[TeleConnect IAM] Created bootstrap CS user: cs@teleconnect.com (password: CS@123456)");
            } else {
                log.info("[TeleConnect IAM] Bootstrap CS user already exists.");
            }

        } catch (Exception e) {
            log.error("[TeleConnect IAM] ERROR creating bootstrap users", e);
        }
    }

    private void createRole(String name, List<Permission> permissions) {
        try {
            var existingRole = roleRepo.findByRoleName(name);
            if (existingRole.isPresent()) {
                Role role = existingRole.get();
                // Update permissions for existing role
                role.setPermissions(permissions);
                roleRepo.save(role);
                log.info("[TeleConnect IAM] Updated role: {} with {} permissions", name, permissions.size());
            } else {
                Role role = new Role();
                role.setRoleName(name);
                role.setPermissions(permissions);
                roleRepo.save(role);
                log.info("[TeleConnect IAM] Created role: {} with {} permissions", name, permissions.size());
            }
        } catch (Exception e) {
            log.error("[TeleConnect IAM] ERROR creating/updating role '{}'", name, e);
        }
    }
}
