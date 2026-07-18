package com.teleconnect.notification.entity;

import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.entity.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    private Long userId;

    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private LocalDateTime createdDate;
}