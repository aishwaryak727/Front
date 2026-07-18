package com.teleconnect.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotBlank(message = "status is required — READ / DISMISSED")
    private String status;
}