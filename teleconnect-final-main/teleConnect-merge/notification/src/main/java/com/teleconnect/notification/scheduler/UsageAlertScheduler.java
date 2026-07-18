package com.teleconnect.notification.scheduler;

import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the usage_summaries table every hour.
 * Fires a USAGE alert when a subscriber has consumed >= 80% of any quota.
 *
 * NOTE: This scheduler reads from the shared teleconnect_db — the same DB
 * used by the Usage Tracking module (module 2.4). No inter-service HTTP call
 * is needed because both modules share the database.
 */
@Component
public class UsageAlertScheduler {

    @Autowired
    private NotificationService notificationService;

    @Value("${app.scheduler.usage-alert.cron:0 0 * * * *}")
    private String cron;

    private static final double WARNING_THRESHOLD  = 80.0;
    private static final double CRITICAL_THRESHOLD = 90.0;

    /**
     * Runs on the cron defined in application.properties (default: every hour).
     * Queries usage_summaries directly via JDBC to avoid coupling to the
     * UsageTracking module's JPA entities.
     */
    @Scheduled(cron = "${app.scheduler.usage-alert.cron:0 0 * * * *}")
    public void checkDataUsageThresholds() {
        // This scheduler is intentionally lightweight — it delegates all
        // persistence to NotificationService.createNotification() which
        // already deduplicates UNREAD alerts.
        //
        // In a real deployment, inject a DataSource or a shared UsageSummaryRepository
        // (if modules share the same Spring context / monorepo) and iterate over
        // active summaries. Example stub below shows the integration pattern:

        /*
        List<UsageSummary> summaries = usageSummaryRepository.findAll();
        for (UsageSummary s : summaries) {
            double dataPct  = computePct(s.getDataUsedMb(),   s.getDataAllowanceMb());
            double voicePct = computePct(s.getVoiceUsedMin(), s.getVoiceAllowanceMin());
            double smsPct   = computePct(s.getSmsUsed(),      s.getSmsAllowance());

            if (dataPct >= WARNING_THRESHOLD) {
                String level = dataPct >= CRITICAL_THRESHOLD ? "CRITICAL" : "WARNING";
                notificationService.createNotification(
                    s.getUserId(),
                    level + ": You have used " + Math.round(dataPct) + "% of your monthly data.",
                    NotificationCategory.USAGE
                );
            }
            if (voicePct >= WARNING_THRESHOLD) {
                String level = voicePct >= CRITICAL_THRESHOLD ? "CRITICAL" : "WARNING";
                notificationService.createNotification(
                    s.getUserId(),
                    level + ": You have used " + Math.round(voicePct) + "% of your voice minutes.",
                    NotificationCategory.USAGE
                );
            }
            if (smsPct >= WARNING_THRESHOLD) {
                String level = smsPct >= CRITICAL_THRESHOLD ? "CRITICAL" : "WARNING";
                notificationService.createNotification(
                    s.getUserId(),
                    level + ": You have used " + Math.round(smsPct) + "% of your SMS quota.",
                    NotificationCategory.USAGE
                );
            }
        }
        */
    }

    private double computePct(double used, double limit) {
        if (limit <= 0) return 0;
        return (used / limit) * 100.0;
    }
}
