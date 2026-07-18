package com.teleconnect.iam.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // omit userId from the JSON when it isn't set
public class RegisterResponseDTO {
    private Long userId;
    private String message;

    // Convenience constructor for responses without a userId (e.g. register)
    public RegisterResponseDTO(String message) {
        this.message = message;
    }
}
