package com.teleconnect.subscriber.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplaceSimRequest {

    @NotBlank(message = "newIccid is required")
    private String newIccid;
}