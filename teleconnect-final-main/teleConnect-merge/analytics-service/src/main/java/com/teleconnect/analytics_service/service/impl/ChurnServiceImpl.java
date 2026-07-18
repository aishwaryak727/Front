package com.teleconnect.analytics_service.service.impl;

import com.teleconnect.analytics_service.client.BillingStatsClient;
import com.teleconnect.analytics_service.client.FaultStatsClient;
import com.teleconnect.analytics_service.client.SubscriberStatsClient;
import com.teleconnect.analytics_service.dto.external.SimLineDto;
import com.teleconnect.analytics_service.dto.external.SubscriberAccountDto;
import com.teleconnect.analytics_service.dto.response.ChurnReportResponse;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.service.ChurnService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes churn by combining the Subscriber module (terminated
 * accounts, ported-out lines, baseline subscriber count) with the
 * Fault and Billing modules (open tickets / disputes per account, for
 * at-risk detection) — all fetched live over HTTP.
 */
@Slf4j
@Service
public class ChurnServiceImpl implements ChurnService {

    private final SubscriberStatsClient subscriberStatsClient;
    private final FaultStatsClient faultStatsClient;
    private final BillingStatsClient billingStatsClient;

    @Value("${analytics.churn.threshold:5.0}")
    private double churnThreshold;

    public ChurnServiceImpl(SubscriberStatsClient subscriberStatsClient,
                             FaultStatsClient faultStatsClient,
                             BillingStatsClient billingStatsClient) {
        this.subscriberStatsClient = subscriberStatsClient;
        this.faultStatsClient = faultStatsClient;
        this.billingStatsClient = billingStatsClient;
    }

    @Override
    public ChurnReportResponse computeChurn(LocalDate periodStart, LocalDate periodEnd, String region) {
        log.info("Computing churn for periodStart={} periodEnd={} region={}", periodStart, periodEnd, region);
        long subscribersAtStart = subscriberStatsClient.countActiveRegisteredBefore(periodStart);

        List<SubscriberAccountDto> terminated = subscriberStatsClient.getTerminatedInPeriod(periodStart, periodEnd);
        List<SimLineDto> portedOut = subscriberStatsClient.getPortedOutInPeriod(periodStart, periodEnd);

        long terminatedCount = terminated.size();
        long portedOutCount = portedOut.size();
        long grossChurned = terminatedCount + portedOutCount;

        double churnRate = subscribersAtStart > 0
                ? (double) grossChurned / subscribersAtStart * 100
                : 0.0;

        boolean highChurnAlert = churnRate > churnThreshold;
        if (highChurnAlert) {
            log.warn("HIGH CHURN ALERT: Churn rate {}% exceeds threshold {}%", churnRate, churnThreshold);
        }
        log.debug("Churn computed subscribersAtStart={} terminatedCount={} portedOutCount={} churnRate={} highChurnAlert={}",
                subscribersAtStart, terminatedCount, portedOutCount, churnRate, highChurnAlert);

        List<Long> atRiskIds = identifyAtRiskAccounts(periodStart);

        ChurnReportResponse response = new ChurnReportResponse();
        response.setPeriodStart(periodStart);
        response.setPeriodEnd(periodEnd);
        response.setRegion(region);
        response.setSubscribersAtPeriodStart(subscribersAtStart);
        response.setTerminatedAccounts(terminatedCount);
        response.setPortedOutLines(portedOutCount);
        response.setGrossChurned(grossChurned);
        response.setChurnRate(Math.round(churnRate * 100.0) / 100.0);
        response.setHighChurnAlert(highChurnAlert);
        response.setAtRiskAccountIds(atRiskIds);
        response.setAtRiskCount(atRiskIds.size());

        return response;
    }

    private List<Long> identifyAtRiskAccounts(LocalDate asOf) {
        LocalDateTime twoMonthsAgo = asOf.minusMonths(2).atStartOfDay();
        LocalDate twoMonthsAgoDate = asOf.minusMonths(2);

        List<Long> atRisk = new ArrayList<>();
        List<SubscriberAccountDto> activeAccounts = subscriberStatsClient.getActiveAccounts();

        for (SubscriberAccountDto account : activeAccounts) {
            if (account.getStatus() != AccountStatus.ACTIVE) {
                continue;
            }
            long openTickets = faultStatsClient.countOpenByAccountSince(account.getAccountId(), twoMonthsAgo);
            long disputes = billingStatsClient.countDisputesBySubscriberSince(account.getSubscriberId(), twoMonthsAgoDate);

            if (openTickets >= 2 || disputes >= 1) {
                atRisk.add(account.getAccountId());
            }
        }

        log.debug("Identified {} at-risk accounts by {}", atRisk.size(), asOf);
        return atRisk;
    }
}
