package com.teleconnect.analytics_service.client;

import com.teleconnect.analytics_service.dto.external.InvoiceDto;
import com.teleconnect.analytics_service.enums.InvoiceStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * HTTP client for the Billing & Invoice Management module's
 * analytics-facing API.
 */
public interface BillingStatsClient {

    /** Invoices for a billing cycle, restricted to the given statuses (e.g. PAID/OVERDUE/SENT for "billable"). */
    List<InvoiceDto> getInvoicesByCycle(Long cycleId, List<InvoiceStatus> statuses);

    /** Count of disputes raised by a subscriber on/after {@code since} — used for at-risk account detection. */
    long countDisputesBySubscriberSince(Long subscriberId, LocalDate since);
}
