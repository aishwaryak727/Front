package com.teleconnect.iam.dto.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;       // Subscriber + Admin
    private String phone;      // Subscriber + Admin
    private Integer regionId;  // Admin only
    private String roleName;   // Admin only
    // No userId in body - it comes from the path param /users/{id}
}
