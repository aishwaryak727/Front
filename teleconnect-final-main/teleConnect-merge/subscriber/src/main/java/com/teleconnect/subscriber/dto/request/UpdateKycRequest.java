package com.teleconnect.subscriber.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateKycRequest {

    @NotBlank(message = "kycStatus is required")
    private String kycStatus;
}