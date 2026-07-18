package com.teleconnect.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank private String currentPassword;
    @NotBlank private String newPassword;
    // No userId - email is extracted from the JWT token by the server
}
