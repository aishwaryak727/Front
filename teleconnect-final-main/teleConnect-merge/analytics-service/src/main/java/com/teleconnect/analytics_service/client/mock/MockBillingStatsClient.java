package com.teleconnect.analytics_service.client.mock;

import com.teleconnect.analytics_service.client.BillingStatsClient;
import com.teleconnect.analytics_service.dto.external.InvoiceDto;
import com.teleconnect.analytics_service.enums.InvoiceStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of BillingStatsClient for testing without actual Billing Service.
 * Enable with: mock.clients.enabled=true
 */
@Component
@ConditionalOnProperty(name = "mock.clients.enabled", havingValue = "true")
public class MockBillingStatsClient implements BillingStatsClient {

    @Override
    public List<InvoiceDto> getInvoicesByCycle(Long cycleId, List<InvoiceStatus> statuses) {
        List<InvoiceDto> invoices = new ArrayList<>();

        // Test data for Cycle 1
        if (cycleId == 1) {
            // PAID invoices
            if (statuses.contains(InvoiceStatus.PAID)) {
                invoices.addAll(List.of(
                    createInvoice(3001L, 1001L, 1L, 1500.00, 1500.00, "2024-06-30", InvoiceStatus.PAID),
                    createInvoice(3002L, 1002L, 1L, 2500.00, 2500.00, "2024-06-30", InvoiceStatus.PAID),
                    createInvoice(3003L, 1003L, 1L, 5000.00, 5000.00, "2024-06-30", InvoiceStatus.PAID),
                    createInvoice(3004L, 1005L, 1L, 1800.00, 1800.00, "2024-06-30", InvoiceStatus.PAID),
                    createInvoice(3005L, 1007L, 1L, 2200.00, 2200.00, "2024-06-30", InvoiceStatus.PAID)
                ));
            }
            // OVERDUE invoices
            if (statuses.contains(InvoiceStatus.OVERDUE)) {
                invoices.addAll(List.of(
                    createInvoice(3006L, 1004L, 1L, 1200.00, 0.00, "2024-05-15", InvoiceStatus.OVERDUE),
                    createInvoice(3007L, 1006L, 1L, 1600.00, 0.00, "2024-05-20", InvoiceStatus.OVERDUE),
                    createInvoice(3008L, 1008L, 1L, 4500.00, 2000.00, "2024-06-10", InvoiceStatus.OVERDUE)
                ));
            }
            // SENT invoices
            if (statuses.contains(InvoiceStatus.SENT)) {
                invoices.addAll(List.of(
                    createInvoice(3009L, 1001L, 1L, 1500.00, 0.00, "2024-07-15", InvoiceStatus.SENT),
                    createInvoice(3010L, 1002L, 1L, 2500.00, 0.00, "2024-07-15", InvoiceStatus.SENT)
                ));
            }
        }
        // Test data for Cycle 2
        else if (cycleId == 2) {
            if (statuses.contains(InvoiceStatus.SENT)) {
                invoices.addAll(List.of(
                    createInvoice(3009L, 1001L, 2L, 1500.00, 0.00, "2024-07-31", InvoiceStatus.SENT),
                    createInvoice(3010L, 1002L, 2L, 2500.00, 0.00, "2024-07-31", InvoiceStatus.SENT),
                    createInvoice(3011L, 1003L, 2L, 5000.00, 0.00, "2024-07-31", InvoiceStatus.SENT),
                    createInvoice(3012L, 1004L, 2L, 1200.00, 0.00, "2024-07-31", InvoiceStatus.SENT),
                    createInvoice(3013L, 1005L, 2L, 1800.00, 0.00, "2024-07-31", InvoiceStatus.SENT),
                    createInvoice(3014L, 1006L, 2L, 1600.00, 0.00, "2024-07-31", InvoiceStatus.SENT),
                    createInvoice(3015L, 1007L, 2L, 2200.00, 0.00, "2024-07-31", InvoiceStatus.SENT),
                    createInvoice(3016L, 1008L, 2L, 4500.00, 0.00, "2024-07-31", InvoiceStatus.SENT)
                ));
            }
        }

        return invoices;
    }

    @Override
    public long countDisputesBySubscriberSince(Long subscriberId, LocalDate since) {
        // Subscriber 5004 and 5006 have disputes
        if (subscriberId == 5004L || subscriberId == 5006L) {
            return 1L;
        }
        return 0L;
    }

    private InvoiceDto createInvoice(Long invoiceId, Long accountId, Long cycleId, 
                                     double totalAmount, double paidAmount, 
                                     String dueDate, InvoiceStatus status) {
        InvoiceDto invoice = new InvoiceDto();
        invoice.setInvoiceId(invoiceId);
        invoice.setAccountId(accountId);
        invoice.setCycleId(cycleId);
        invoice.setTotalAmount(BigDecimal.valueOf(totalAmount));
        invoice.setPaidAmount(BigDecimal.valueOf(paidAmount));
        invoice.setDueDate(LocalDate.parse(dueDate));
        invoice.setStatus(status);
        return invoice;
    }
}
