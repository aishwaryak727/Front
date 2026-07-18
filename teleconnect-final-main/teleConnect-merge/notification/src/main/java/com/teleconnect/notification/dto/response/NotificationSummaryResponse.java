package com.teleconnect.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationSummaryResponse {

    private Long userId;
    private Long unreadCount;
}