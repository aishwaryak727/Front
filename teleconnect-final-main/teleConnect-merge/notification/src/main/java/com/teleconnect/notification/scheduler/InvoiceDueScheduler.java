package com.teleconnect.notification.scheduler;

import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs daily at 09:00.
 * Sends a BILLING payment-due reminder when an Invoice due date is
 * {@code app.notification.invoice-due-reminder-days} days away (default 2).
 *
 * Also fires an OVERDUE alert for invoices whose status = OVERDUE.
 * Reads from the invoices table in the shared teleconnect_db.
 */
@Component
public class InvoiceDueScheduler {

    @Autowired
    private NotificationService notificationService;

    @Value("${app.notification.invoice-due-reminder-days:2}")
    private int reminderDays;

    @Scheduled(cron = "${app.scheduler.invoice-due.cron:0 0 9 * * *}")
    public void remindUpcomingInvoices() {
        // Integration stub — wire in InvoiceRepository from module 2.5.

        /*
        LocalDate targetDate = LocalDate.now().plusDays(reminderDays);
        List<Invoice> upcoming = invoiceRepository
                .findByDueDateAndStatus(targetDate, Invoice.Status.SENT);

        for (Invoice inv : upcoming) {
            notificationService.createNotification(
                inv.getUserId(),
                "Your invoice of ₹" + inv.getTotalAmount() +
                    " is due on " + inv.getDueDate() + ". Please pay to avoid late fees.",
                NotificationCategory.BILLING
            );
        }

        // Overdue alerts
        List<Invoice> overdue = invoiceRepository.findByStatus(Invoice.Status.OVERDUE);
        for (Invoice inv : overdue) {
            notificationService.createNotification(
                inv.getUserId(),
                "OVERDUE: Your invoice of ₹" + inv.getTotalAmount() +
                    " was due on " + inv.getDueDate() + ". Late fees may apply.",
                NotificationCategory.BILLING
            );
        }
        */
    }
}
