package com.teleconnect.iam.dto.request;

import lombok.Data;

@Data
public class UpdateStatusRequest {
    private String status; // A (Active) / S (Suspended) / I (Inactive)
}
