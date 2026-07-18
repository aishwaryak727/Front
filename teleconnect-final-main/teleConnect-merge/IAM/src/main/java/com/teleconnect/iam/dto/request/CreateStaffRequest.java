package com.teleconnect.iam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateStaffRequest {
    @NotBlank private String name;
    @Email @NotBlank private String email;
    @NotBlank private String phone;
    @NotBlank private String roleName; // CS (CS Agent) / B (Billing) / N (Network Ops) / C (Compliance)
    private Integer regionId;
    // No password - server sets app.default.staff.password automatically
}
