package com.teleconnect.analytics_service.service.impl;

import com.teleconnect.analytics_service.client.SubscriberStatsClient;
import com.teleconnect.analytics_service.client.UsageStatsClient;
import com.teleconnect.analytics_service.dto.external.UsageSummaryDto;
import com.teleconnect.analytics_service.dto.response.NetworkUtilisationResponse;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.service.NetworkUtilisationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Computes network utilisation from usage summaries fetched live from
 * the Usage module, combined with active-subscriber counts fetched
 * live from the Subscriber module.
 */
@Slf4j
@Service
public class NetworkUtilisationServiceImpl implements NetworkUtilisationService {

    private final UsageStatsClient usageStatsClient;
    private final SubscriberStatsClient subscriberStatsClient;

    public NetworkUtilisationServiceImpl(UsageStatsClient usageStatsClient,
                                          SubscriberStatsClient subscriberStatsClient) {
        this.usageStatsClient = usageStatsClient;
        this.subscriberStatsClient = subscriberStatsClient;
    }

    @Override
    public NetworkUtilisationResponse computeUtilisation(Long cycleId, String region) {
        log.info("Computing network utilisation for cycleId={} region={}", cycleId, region);
        List<UsageSummaryDto> summaries;
        long subscriberCount;

        if (region != null && !region.isBlank()) {
            try {
                Long regionId = Long.parseLong(region);
                summaries = usageStatsClient.getByCycleAndRegion(cycleId, regionId);
                subscriberCount = subscriberStatsClient.countActiveByRegion(regionId);
            } catch (NumberFormatException e) {
                log.warn("Region value '{}' is not numeric, falling back to overall cycle metrics", region);
                summaries = usageStatsClient.getByCycle(cycleId);
                subscriberCount = subscriberStatsClient.countByStatus(AccountStatus.ACTIVE);
            }
        } else {
            summaries = usageStatsClient.getByCycle(cycleId);
            subscriberCount = subscriberStatsClient.countByStatus(AccountStatus.ACTIVE);
        }

        long totalData = summaries.stream().mapToLong(UsageSummaryDto::getDataUsedMb).sum();
        long totalVoice = summaries.stream().mapToLong(UsageSummaryDto::getVoiceUsedMin).sum();
        long totalSms = summaries.stream().mapToLong(UsageSummaryDto::getSmsUsed).sum();

        NetworkUtilisationResponse response = new NetworkUtilisationResponse();
        response.setCycleId(cycleId);
        response.setRegion(region);
        response.setTotalDataUsedMb(totalData);
        response.setTotalVoiceUsedMin(totalVoice);
        response.setTotalSmsUsed(totalSms);
        response.setSubscriberCount(subscriberCount);
        response.setAvgDataPerSubscriberMb(subscriberCount > 0 ? (double) totalData / subscriberCount : 0);
        response.setAvgVoicePerSubscriberMin(subscriberCount > 0 ? (double) totalVoice / subscriberCount : 0);

        log.debug("Network utilisation computed totalData={} totalVoice={} totalSms={} subscriberCount={}",
                totalData, totalVoice, totalSms, subscriberCount);
        return response;
    }
}
