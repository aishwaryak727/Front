package com.teleconnect.iam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String role;
    private String name;
    private Boolean mustChangePassword;
    private List<String> permissions;
}
