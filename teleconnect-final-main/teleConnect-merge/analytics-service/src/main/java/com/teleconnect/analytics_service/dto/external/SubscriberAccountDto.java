package com.teleconnect.analytics_service.dto.external;

import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.enums.AccountType;

import java.time.LocalDate;

/**
 * Lightweight projection of SubscriberAccount, as returned by the
 * Subscriber Account & SIM Management module's analytics-facing API.
 * Owned and maintained by that module; analytics-service treats this
 * as a read-only external contract.
 */
public class SubscriberAccountDto {

    private Long accountId;
    private Long subscriberId;
    private AccountType accountType;
    private LocalDate registrationDate;
    private Long regionId;
    private AccountStatus status;

    public SubscriberAccountDto() {}

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getSubscriberId() { return subscriberId; }
    public void setSubscriberId(Long subscriberId) { this.subscriberId = subscriberId; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public LocalDate getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDate registrationDate) { this.registrationDate = registrationDate; }

    public Long getRegionId() { return regionId; }
    public void setRegionId(Long regionId) { this.regionId = regionId; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
}
