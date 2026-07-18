package com.teleconnect.analytics_service.client.mock;

import com.teleconnect.analytics_service.client.SubscriberStatsClient;
import com.teleconnect.analytics_service.dto.external.SimLineDto;
import com.teleconnect.analytics_service.dto.external.SubscriberAccountDto;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.enums.AccountType;
import com.teleconnect.analytics_service.enums.SIMStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of SubscriberStatsClient for testing without actual Subscriber Service.
 * Enable with: mock.clients.enabled=true
 */
@Component
@ConditionalOnProperty(name = "mock.clients.enabled", havingValue = "true")
public class MockSubscriberStatsClient implements SubscriberStatsClient {

    @Override
    public long countByStatus(AccountStatus status) {
        if (status == AccountStatus.ACTIVE) return 8L;
        if (status == AccountStatus.TERMINATED) return 3L;
        return 0L;
    }

    @Override
    public long countActiveRegisteredBefore(LocalDate date) {
        return 8L; // 8 active accounts
    }

    @Override
    public long countActiveByRegion(Long regionId) {
        if (regionId == 1L) return 4L; // Region 1: 4 active
        if (regionId == 2L) return 4L; // Region 2: 4 active
        return 0L;
    }

    @Override
    public List<SubscriberAccountDto> getActiveAccounts() {
        List<SubscriberAccountDto> accounts = new ArrayList<>();
        accounts.add(createAccount(1001L, 5001L, AccountType.PREPAID, "2024-01-15", 1L, AccountStatus.ACTIVE));
        accounts.add(createAccount(1002L, 5002L, AccountType.POSTPAID, "2024-02-20", 1L, AccountStatus.ACTIVE));
        accounts.add(createAccount(1003L, 5003L, AccountType.ENTERPRISE, "2024-03-10", 2L, AccountStatus.ACTIVE));
        accounts.add(createAccount(1004L, 5004L, AccountType.PREPAID, "2024-01-05", 2L, AccountStatus.ACTIVE));
        accounts.add(createAccount(1005L, 5005L, AccountType.POSTPAID, "2024-04-01", 1L, AccountStatus.ACTIVE));
        accounts.add(createAccount(1006L, 5006L, AccountType.PREPAID, "2024-05-15", 2L, AccountStatus.ACTIVE));
        accounts.add(createAccount(1007L, 5007L, AccountType.POSTPAID, "2024-02-10", 1L, AccountStatus.ACTIVE));
        accounts.add(createAccount(1008L, 5008L, AccountType.ENTERPRISE, "2024-03-20", 2L, AccountStatus.ACTIVE));
        return accounts;
    }

    @Override
    public List<SubscriberAccountDto> getByStatusAndRegistrationBetween(AccountStatus status, LocalDate start, LocalDate end) {
        // If looking for ACTIVE between 2024-01-01 and 2024-06-30, return 8 new accounts
        if (status == AccountStatus.ACTIVE && start.isBefore(LocalDate.parse("2024-07-01"))) {
            return getActiveAccounts();
        }
        return List.of();
    }

    @Override
    public List<SubscriberAccountDto> getTerminatedInPeriod(LocalDate start, LocalDate end) {
        // Return 3 terminated accounts (for churn calculation)
        List<SubscriberAccountDto> terminated = new ArrayList<>();
        terminated.add(createAccount(1009L, 5009L, AccountType.PREPAID, "2023-06-01", 1L, AccountStatus.TERMINATED));
        terminated.add(createAccount(1010L, 5010L, AccountType.POSTPAID, "2023-07-15", 2L, AccountStatus.TERMINATED));
        terminated.add(createAccount(1011L, 5011L, AccountType.PREPAID, "2023-08-20", 1L, AccountStatus.TERMINATED));
        return terminated;
    }

    @Override
    public List<SimLineDto> getPortedOutInPeriod(LocalDate start, LocalDate end) {
        List<SimLineDto> portedOut = new ArrayList<>();
        portedOut.add(createSimLine(2009L, 1009L, "2023-06-01", SIMStatus.PORTED_OUT));
        portedOut.add(createSimLine(2010L, 1010L, "2023-07-15", SIMStatus.PORTED_OUT));
        return portedOut;
    }

    @Override
    public List<SimLineDto> getActivatedInPeriod(LocalDate start, LocalDate end) {
        // Return 8 activated lines
        List<SimLineDto> activated = new ArrayList<>();
        activated.add(createSimLine(2001L, 1001L, "2024-01-15", SIMStatus.ACTIVE));
        activated.add(createSimLine(2002L, 1002L, "2024-02-20", SIMStatus.ACTIVE));
        activated.add(createSimLine(2003L, 1003L, "2024-03-10", SIMStatus.ACTIVE));
        activated.add(createSimLine(2004L, 1004L, "2024-01-05", SIMStatus.ACTIVE));
        activated.add(createSimLine(2005L, 1005L, "2024-04-01", SIMStatus.ACTIVE));
        activated.add(createSimLine(2006L, 1006L, "2024-05-15", SIMStatus.ACTIVE));
        activated.add(createSimLine(2007L, 1007L, "2024-02-10", SIMStatus.ACTIVE));
        activated.add(createSimLine(2008L, 1008L, "2024-03-20", SIMStatus.ACTIVE));
        return activated;
    }

    private SubscriberAccountDto createAccount(Long accountId, Long subscriberId, AccountType type, 
                                               String regDate, Long regionId, AccountStatus status) {
        SubscriberAccountDto account = new SubscriberAccountDto();
        account.setAccountId(accountId);
        account.setSubscriberId(subscriberId);
        account.setAccountType(type);
        account.setRegistrationDate(LocalDate.parse(regDate));
        account.setRegionId(regionId);
        account.setStatus(status);
        return account;
    }

    private SimLineDto createSimLine(Long lineId, Long accountId, String activationDate, SIMStatus status) {
        SimLineDto line = new SimLineDto();
        line.setLineId(lineId);
        line.setAccountId(accountId);
        line.setActivationDate(LocalDate.parse(activationDate));
        line.setStatus(status);
        return line;
    }
}
