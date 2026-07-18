package com.teleconnect.analytics_service.client;

import com.teleconnect.analytics_service.dto.external.FaultTicketDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HTTP client for the Fault & Service Request Management module's
 * analytics-facing API.
 */
public interface FaultStatsClient {

    /** Tickets resolved/closed with a raisedDate within [start, end]. */
    List<FaultTicketDto> getClosedInPeriod(LocalDateTime start, LocalDateTime end);

    /** Tickets still OPEN/IN_PROGRESS that were raised before {@code threshold} (i.e. breaching SLA). */
    List<FaultTicketDto> getOpenBreachingBefore(LocalDateTime threshold);

    /** Count of OPEN/IN_PROGRESS tickets for an account raised on/after {@code since}. */
    long countOpenByAccountSince(Long accountId, LocalDateTime since);

    /** Count of tickets currently in ESCALATED status. */
    long countEscalated();

    /** Marks a ticket as ESCALATED in the owning module (used by the SLA breach-escalation job). */
    void escalateTicket(Long ticketId);
}
