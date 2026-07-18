package com.teleconnect.analytics_service.dto.external;

import com.teleconnect.analytics_service.enums.FaultPriority;
import com.teleconnect.analytics_service.enums.FaultStatus;

import java.time.LocalDateTime;

/**
 * Lightweight projection of FaultTicket, as returned by the Fault &
 * Service Request Management module's analytics-facing API.
 */
public class FaultTicketDto {

    private Long ticketId;
    private Long accountId;
    private Long lineId;
    private FaultPriority priority;
    private LocalDateTime raisedDate;
    private LocalDateTime resolvedDate;
    private FaultStatus status;

    public FaultTicketDto() {}

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }

    public FaultPriority getPriority() { return priority; }
    public void setPriority(FaultPriority priority) { this.priority = priority; }

    public LocalDateTime getRaisedDate() { return raisedDate; }
    public void setRaisedDate(LocalDateTime raisedDate) { this.raisedDate = raisedDate; }

    public LocalDateTime getResolvedDate() { return resolvedDate; }
    public void setResolvedDate(LocalDateTime resolvedDate) { this.resolvedDate = resolvedDate; }

    public FaultStatus getStatus() { return status; }
    public void setStatus(FaultStatus status) { this.status = status; }
}
