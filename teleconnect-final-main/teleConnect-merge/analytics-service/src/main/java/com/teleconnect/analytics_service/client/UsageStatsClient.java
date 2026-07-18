package com.teleconnect.analytics_service.client;

import com.teleconnect.analytics_service.dto.external.UsageSummaryDto;

import java.util.List;

/**
 * HTTP client for the Usage & Consumption Tracking module's
 * analytics-facing API.
 */
public interface UsageStatsClient {

    /** All usage summaries for a billing cycle. */
    List<UsageSummaryDto> getByCycle(Long cycleId);

    /** Usage summaries for a billing cycle, restricted to lines belonging to accounts in the given region. */
    List<UsageSummaryDto> getByCycleAndRegion(Long cycleId, Long regionId);
}
