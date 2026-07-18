package com.teleconnect.notification.repository;

import com.teleconnect.notification.entity.Notification;
import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.entity.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdAndStatus(
            Long userId,
            NotificationStatus status);

    List<Notification> findByUserIdAndCategory(
            Long userId,
            NotificationCategory category);

    Long countByUserIdAndStatus(
            Long userId,
            NotificationStatus status);
}