package com.teleconnect.subscriber.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotNull(message = "subscriberId is required")
    private Long subscriberId;

    @NotNull(message = "accountType is required")
    private String accountType;

    private String kycStatus = "Pending";
}