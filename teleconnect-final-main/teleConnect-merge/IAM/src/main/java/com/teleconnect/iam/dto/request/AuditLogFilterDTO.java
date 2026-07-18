package com.teleconnect.iam.dto.request;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class AuditLogFilterDTO {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from; // ?from=2024-01-01T00:00:00

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;   // ?to=2024-12-31T23:59:59

    private String action;      // ?action=USER_LOGIN
    private String module;      // ?module=IAM

    private Integer page = 0;   // ?page=0
    private Integer size = 20;  // ?size=20
}
