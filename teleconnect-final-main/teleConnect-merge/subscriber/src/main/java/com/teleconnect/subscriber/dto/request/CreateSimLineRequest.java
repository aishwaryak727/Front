package com.teleconnect.subscriber.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSimLineRequest {

    @NotBlank(message = "msisdn is required")
    private String msisdn;

    @NotBlank(message = "iccid is required")
    private String iccid;

    private String serviceType = "VoiceData";
}