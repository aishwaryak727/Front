package com.teleconnect.iam.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDTO {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String roleName;
    private Integer regionId;
    private String status;
    private LocalDateTime createdAt;
}
