package com.teleconnect.iam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;

    @Column
    private Long userId;

    @Column(nullable = false, length = 100)
    private String action; // USER_LOGIN, USER_REGISTERED, STAFF_ACCOUNT_CREATED, USER_LOGOUT, etc.

    @Column(nullable = false, length = 100)
    private String module; // Always IAM

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
