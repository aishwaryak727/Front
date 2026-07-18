package com.teleconnect.analytics_service.client.mock;

import com.teleconnect.analytics_service.client.FaultStatsClient;
import com.teleconnect.analytics_service.dto.external.FaultTicketDto;
import com.teleconnect.analytics_service.enums.FaultPriority;
import com.teleconnect.analytics_service.enums.FaultStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of FaultStatsClient for testing without actual Fault Service.
 * Enable with: mock.clients.enabled=true
 */
@Component
@ConditionalOnProperty(name = "mock.clients.enabled", havingValue = "true")
public class MockFaultStatsClient implements FaultStatsClient {

    @Override
    public List<FaultTicketDto> getClosedInPeriod(LocalDateTime start, LocalDateTime end) {
        List<FaultTicketDto> tickets = new ArrayList<>();

        // RESOLVED tickets
        tickets.add(createTicket(6001L, 1001L, 2001L, FaultPriority.HIGH, 
            "2024-06-01 10:00:00", "2024-06-01 14:00:00", FaultStatus.RESOLVED));
        tickets.add(createTicket(6002L, 1002L, 2002L, FaultPriority.CRITICAL, 
            "2024-06-05 09:00:00", "2024-06-05 11:00:00", FaultStatus.RESOLVED));
        tickets.add(createTicket(6003L, 1003L, 2003L, FaultPriority.MEDIUM, 
            "2024-06-10 08:00:00", "2024-06-10 20:00:00", FaultStatus.RESOLVED));
        tickets.add(createTicket(6004L, 1005L, 2005L, FaultPriority.LOW, 
            "2024-06-15 15:00:00", "2024-06-18 09:00:00", FaultStatus.RESOLVED));
        tickets.add(createTicket(6005L, 1007L, 2007L, FaultPriority.HIGH, 
            "2024-06-20 12:00:00", "2024-06-20 18:00:00", FaultStatus.RESOLVED));

        // CLOSED tickets
        tickets.add(createTicket(6006L, 1006L, 2006L, FaultPriority.MEDIUM, 
            "2024-06-08 11:00:00", "2024-06-09 10:00:00", FaultStatus.CLOSED));
        tickets.add(createTicket(6007L, 1008L, 2008L, FaultPriority.HIGH, 
            "2024-06-12 07:00:00", "2024-06-12 16:00:00", FaultStatus.CLOSED));

        // July tickets
        tickets.add(createTicket(6011L, 1001L, 2001L, FaultPriority.MEDIUM, 
            "2024-07-01 09:00:00", "2024-07-01 14:00:00", FaultStatus.RESOLVED));
        tickets.add(createTicket(6012L, 1002L, 2002L, FaultPriority.HIGH, 
            "2024-07-05 10:00:00", "2024-07-05 15:00:00", FaultStatus.CLOSED));
        tickets.add(createTicket(6013L, 1003L, 2003L, FaultPriority.CRITICAL, 
            "2024-07-10 08:00:00", "2024-07-10 10:00:00", FaultStatus.RESOLVED));

        return tickets;
    }

    @Override
    public List<FaultTicketDto> getOpenBreachingBefore(LocalDateTime threshold) {
        // Return OPEN tickets that are breaching SLA
        List<FaultTicketDto> tickets = new ArrayList<>();
        tickets.add(createTicket(6008L, 1004L, 2004L, FaultPriority.HIGH, 
            "2024-06-25 14:00:00", null, FaultStatus.OPEN));
        tickets.add(createTicket(6009L, 1004L, 2004L, FaultPriority.MEDIUM, 
            "2024-06-26 10:00:00", null, FaultStatus.OPEN));
        tickets.add(createTicket(6010L, 1006L, 2006L, FaultPriority.LOW, 
            "2024-06-27 13:00:00", null, FaultStatus.OPEN));
        return tickets;
    }

    @Override
    public long countOpenByAccountSince(Long accountId, LocalDateTime since) {
        // Account 1004 has 2 open tickets, others have 0 or 1
        if (accountId == 1004L) return 2L;
        if (accountId == 1006L) return 1L;
        return 0L;
    }

    @Override
    public long countEscalated() {
        return 0L; // No escalated tickets in mock data
    }

    @Override
    public void escalateTicket(Long ticketId) {
        // Mock does nothing on escalate
    }

    private FaultTicketDto createTicket(Long ticketId, Long accountId, Long lineId, 
                                        FaultPriority priority, String raisedDate, 
                                        String resolvedDate, FaultStatus status) {
        FaultTicketDto ticket = new FaultTicketDto();
        ticket.setTicketId(ticketId);
        ticket.setAccountId(accountId);
        ticket.setLineId(lineId);
        ticket.setPriority(priority);
        ticket.setRaisedDate(parseDateTime(raisedDate));
        ticket.setResolvedDate(resolvedDate != null ? parseDateTime(resolvedDate) : null);
        ticket.setStatus(status);
        return ticket;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}
