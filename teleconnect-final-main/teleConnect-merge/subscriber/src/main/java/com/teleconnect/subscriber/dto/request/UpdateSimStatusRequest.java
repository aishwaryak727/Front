package com.teleconnect.subscriber.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateSimStatusRequest {

    @NotBlank(message = "status is required")
    private String status;
}