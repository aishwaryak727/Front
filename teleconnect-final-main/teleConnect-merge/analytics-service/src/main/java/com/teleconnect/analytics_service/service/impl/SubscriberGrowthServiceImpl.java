package com.teleconnect.analytics_service.service.impl;

import com.teleconnect.analytics_service.client.SubscriberStatsClient;
import com.teleconnect.analytics_service.dto.external.SimLineDto;
import com.teleconnect.analytics_service.dto.external.SubscriberAccountDto;
import com.teleconnect.analytics_service.dto.response.SubscriberGrowthResponse;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.enums.AccountType;
import com.teleconnect.analytics_service.service.SubscriberGrowthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Computes subscriber growth entirely from data fetched live from the
 * Subscriber Account & SIM Management module's analytics API.
 */
@Slf4j
@Service
public class SubscriberGrowthServiceImpl implements SubscriberGrowthService {

    private final SubscriberStatsClient subscriberStatsClient;

    public SubscriberGrowthServiceImpl(SubscriberStatsClient subscriberStatsClient) {
        this.subscriberStatsClient = subscriberStatsClient;
    }

    @Override
    public SubscriberGrowthResponse computeGrowth(LocalDate periodStart, LocalDate periodEnd) {
        log.info("Computing subscriber growth for period {} to {}", periodStart, periodEnd);
        List<SubscriberAccountDto> newAccounts = subscriberStatsClient
                .getByStatusAndRegistrationBetween(AccountStatus.ACTIVE, periodStart, periodEnd);

        List<SubscriberAccountDto> terminated = subscriberStatsClient
                .getTerminatedInPeriod(periodStart, periodEnd);

        List<SimLineDto> activatedLines = subscriberStatsClient.getActivatedInPeriod(periodStart, periodEnd);

        long grossAdds = newAccounts.size();
        long terminations = terminated.size();
        long netAdds = grossAdds - terminations;

        long prepaidAdds = newAccounts.stream()
                .filter(a -> a.getAccountType() == AccountType.PREPAID).count();
        long postpaidAdds = newAccounts.stream()
                .filter(a -> a.getAccountType() == AccountType.POSTPAID).count();
        long enterpriseAdds = newAccounts.stream()
                .filter(a -> a.getAccountType() == AccountType.ENTERPRISE).count();

        SubscriberGrowthResponse response = new SubscriberGrowthResponse();
        response.setPeriodStart(periodStart);
        response.setPeriodEnd(periodEnd);
        response.setGrossAdds(grossAdds);
        response.setTerminations(terminations);
        response.setNetAdds(netAdds);
        response.setActiveSIMActivations(activatedLines.size());
        response.setPrepaidAdds(prepaidAdds);
        response.setPostpaidAdds(postpaidAdds);
        response.setEnterpriseAdds(enterpriseAdds);

        log.debug("Subscriber growth result grossAdds={} terminations={} netAdds={} activeSimActivations={}",
                grossAdds, terminations, netAdds, activatedLines.size());
        return response;
    }
}
