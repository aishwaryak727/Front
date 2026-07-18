package com.teleconnect.iam.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResponseDTO {
    private Long auditId;
    private Long userId;
    private String action;
    private String module;
    private String ipAddress;
    private LocalDateTime timestamp;
}
