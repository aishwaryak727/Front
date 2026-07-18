package com.teleconnect.notification.dto.request;

import com.teleconnect.notification.entity.enums.NotificationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
@Data
public class NotificationRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "message is required")
    private String message;

    @NotNull(message = "category is required")
    private NotificationCategory category;
}