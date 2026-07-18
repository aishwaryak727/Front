package com.teleconnect.notification.scheduler;

import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs daily at 08:00.
 * Sends a PLAN expiry reminder when a ServiceSubscription expires in
 * {@code app.notification.plan-expiry-reminder-days} days (default 3).
 *
 * Reads from service_subscriptions table in the shared teleconnect_db.
 */
@Component
public class PlanExpiryScheduler {

    @Autowired
    private NotificationService notificationService;

    @Value("${app.notification.plan-expiry-reminder-days:3}")
    private int reminderDays;

    @Scheduled(cron = "${app.scheduler.plan-expiry.cron:0 0 8 * * *}")
    public void remindExpiringPlans() {
        // Integration stub — wire in ServiceSubscriptionRepository from module 2.3
        // when running in the same Spring context or shared DB config.

        /*
        LocalDate targetDate = LocalDate.now().plusDays(reminderDays);
        List<ServiceSubscription> expiring =
                serviceSubscriptionRepository.findByExpiryDate(targetDate);

        for (ServiceSubscription sub : expiring) {
            notificationService.createNotification(
                sub.getUserId(),
                "Your plan \"" + sub.getPlanName() + "\" expires in " + reminderDays +
                    " days. Renew now to avoid service interruption.",
                NotificationCategory.PLAN
            );
        }
        */
    }
}
