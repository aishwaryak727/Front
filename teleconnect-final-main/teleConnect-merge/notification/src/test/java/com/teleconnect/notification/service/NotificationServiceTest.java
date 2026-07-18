package com.teleconnect.notification.service;

import com.teleconnect.notification.dto.request.NotificationRequest;
import com.teleconnect.notification.dto.response.NotificationResponse;
import com.teleconnect.notification.dto.response.NotificationSummaryResponse;
import com.teleconnect.notification.entity.Notification;
import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.entity.enums.NotificationStatus;
import com.teleconnect.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationServiceImpl service;

    @Test
    void createNotificationTest() {

        NotificationRequest request = new NotificationRequest();
        request.setUserId(201L);
        request.setMessage("Test Notification");
        request.setCategory(NotificationCategory.FAULT);

        Notification notification = new Notification();
        notification.setNotificationId(1L);
        notification.setUserId(201L);
        notification.setMessage("Test Notification");
        notification.setCategory(NotificationCategory.FAULT);
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setCreatedDate(LocalDateTime.now());

        when(repository.save(any(Notification.class)))
                .thenReturn(notification);

        NotificationResponse response =
                service.createNotification(request);

        assertNotNull(response);
        assertEquals(201L, response.getUserId());
    }

    @Test
    void getNotificationsTest() {

        Notification notification = new Notification();

        notification.setNotificationId(1L);
        notification.setUserId(201L);
        notification.setCategory(NotificationCategory.FAULT);
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setMessage("Test");

        when(repository.findByUserId(201L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result =
                service.getNotifications(201L);

        assertEquals(1, result.size());
    }

    @Test
    void getNotificationByIdTest() {

        Notification notification = new Notification();

        notification.setNotificationId(1L);
        notification.setUserId(201L);

        when(repository.findById(1L))
                .thenReturn(Optional.of(notification));

        NotificationResponse result =
                service.getNotificationById(1L);

        assertEquals(1L, result.getNotificationId());
    }

    @Test
    void getByStatusTest() {

        Notification notification = new Notification();

        notification.setNotificationId(1L);
        notification.setStatus(NotificationStatus.UNREAD);

        when(repository.findByUserIdAndStatus(
                201L,
                NotificationStatus.UNREAD))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result =
                service.getByStatus(
                        201L,
                        NotificationStatus.UNREAD);

        assertFalse(result.isEmpty());
    }

    @Test
    void getByCategoryTest() {

        Notification notification = new Notification();

        notification.setNotificationId(1L);
        notification.setCategory(NotificationCategory.FAULT);

        when(repository.findByUserIdAndCategory(
                201L,
                NotificationCategory.FAULT))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result =
                service.getByCategory(
                        201L,
                        NotificationCategory.FAULT);

        assertFalse(result.isEmpty());
    }

    @Test
    void getUnreadCountTest() {

        when(repository.countByUserIdAndStatus(
                201L,
                NotificationStatus.UNREAD))
                .thenReturn(5L);

        NotificationSummaryResponse response =
                service.getUnreadCount(201L);

        assertEquals(5L, response.getUnreadCount());
    }

    @Test
    void markAsReadTest() {

        Notification notification = new Notification();

        notification.setNotificationId(1L);
        notification.setStatus(NotificationStatus.UNREAD);

        when(repository.findById(1L))
                .thenReturn(Optional.of(notification));

        String result =
                service.markAsRead(1L);

        assertEquals(
                "Notification marked as read",
                result);
    }

    @Test
    void dismissNotificationTest() {

        Notification notification = new Notification();

        notification.setNotificationId(1L);

        when(repository.findById(1L))
                .thenReturn(Optional.of(notification));

        String result =
                service.dismissNotification(1L);

        assertEquals(
                "Notification dismissed successfully",
                result);
    }

    @Test
    void markAllAsReadTest() {

        Notification notification1 = new Notification();
        notification1.setStatus(NotificationStatus.UNREAD);

        Notification notification2 = new Notification();
        notification2.setStatus(NotificationStatus.UNREAD);

        when(repository.findByUserId(201L))
                .thenReturn(List.of(notification1, notification2));

        String result =
                service.markAllAsRead(201L);

        assertEquals(
                "All notifications marked as read",
                result);
    }
}