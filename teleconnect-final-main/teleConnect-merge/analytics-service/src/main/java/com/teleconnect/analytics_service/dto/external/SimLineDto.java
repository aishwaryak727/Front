package com.teleconnect.analytics_service.dto.external;

import com.teleconnect.analytics_service.enums.SIMStatus;

import java.time.LocalDate;

/**
 * Lightweight projection of SIMLine, as returned by the Subscriber
 * Account & SIM Management module's analytics-facing API.
 */
public class SimLineDto {

    private Long lineId;
    private Long accountId;
    private LocalDate activationDate;
    private SIMStatus status;

    public SimLineDto() {}

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public LocalDate getActivationDate() { return activationDate; }
    public void setActivationDate(LocalDate activationDate) { this.activationDate = activationDate; }

    public SIMStatus getStatus() { return status; }
    public void setStatus(SIMStatus status) { this.status = status; }
}
