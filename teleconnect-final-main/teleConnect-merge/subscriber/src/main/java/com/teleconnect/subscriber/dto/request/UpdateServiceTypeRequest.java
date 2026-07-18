package com.teleconnect.subscriber.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateServiceTypeRequest {

    @NotBlank(message = "serviceType is required")
    private String serviceType;
}