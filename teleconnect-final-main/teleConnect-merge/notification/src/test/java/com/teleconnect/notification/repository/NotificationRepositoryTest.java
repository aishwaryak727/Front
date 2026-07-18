package com.teleconnect.notification.repository;

import com.teleconnect.notification.entity.Notification;
import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.entity.enums.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRepositoryTest {

    @Mock
    private NotificationRepository repository;

    private Notification notification;

    @BeforeEach
    void setUp() {

        notification = new Notification();

        notification.setNotificationId(1L);
        notification.setUserId(201L);
        notification.setMessage(
                "Fault ticket FT1023 has been resolved");
        notification.setCategory(
                NotificationCategory.FAULT);
        notification.setStatus(
                NotificationStatus.UNREAD);
        notification.setCreatedDate(
                LocalDateTime.now());
    }

    @Test
    void findByUserIdTest() {

        when(repository.findByUserId(201L))
                .thenReturn(List.of(notification));

        List<Notification> result =
                repository.findByUserId(201L);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getUserId())
                .isEqualTo(201L);
    }

    @Test
    void findByUserIdAndStatusTest() {

        when(repository.findByUserIdAndStatus(
                201L,
                NotificationStatus.UNREAD))
                .thenReturn(List.of(notification));

        List<Notification> result =
                repository.findByUserIdAndStatus(
                        201L,
                        NotificationStatus.UNREAD);

        assertThat(result).hasSize(1);

        assertThat(result.get(0).getStatus())
                .isEqualTo(
                        NotificationStatus.UNREAD);
    }

    @Test
    void findByUserIdAndCategoryTest() {

        when(repository.findByUserIdAndCategory(
                201L,
                NotificationCategory.FAULT))
                .thenReturn(List.of(notification));

        List<Notification> result =
                repository.findByUserIdAndCategory(
                        201L,
                        NotificationCategory.FAULT);

        assertThat(result).hasSize(1);

        assertThat(result.get(0).getCategory())
                .isEqualTo(
                        NotificationCategory.FAULT);
    }

    @Test
    void countByUserIdAndStatusTest() {

        when(repository.countByUserIdAndStatus(
                201L,
                NotificationStatus.UNREAD))
                .thenReturn(5L);

        Long count =
                repository.countByUserIdAndStatus(
                        201L,
                        NotificationStatus.UNREAD);

        assertThat(count)
                .isEqualTo(5L);
    }
}