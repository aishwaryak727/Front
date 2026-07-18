package com.teleconnect.analytics_service.client.mock;

import com.teleconnect.analytics_service.client.UsageStatsClient;
import com.teleconnect.analytics_service.dto.external.UsageSummaryDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of UsageStatsClient for testing without actual Usage Service.
 * Enable with: mock.clients.enabled=true
 */
@Component
@ConditionalOnProperty(name = "mock.clients.enabled", havingValue = "true")
public class MockUsageStatsClient implements UsageStatsClient {

    @Override
    public List<UsageSummaryDto> getByCycle(Long cycleId) {
        List<UsageSummaryDto> summaries = new ArrayList<>();

        if (cycleId == 1L) {
            summaries.add(createUsage(5001L, 2001L, 1L, 1200L, 300L, 100L));
            summaries.add(createUsage(5002L, 2002L, 1L, 2500L, 600L, 250L));
            summaries.add(createUsage(5003L, 2003L, 1L, 5000L, 1200L, 500L));
            summaries.add(createUsage(5004L, 2004L, 1L, 800L, 250L, 75L));
            summaries.add(createUsage(5005L, 2005L, 1L, 1500L, 400L, 120L));
            summaries.add(createUsage(5006L, 2006L, 1L, 900L, 200L, 80L));
            summaries.add(createUsage(5007L, 2007L, 1L, 2000L, 500L, 180L));
            summaries.add(createUsage(5008L, 2008L, 1L, 4500L, 1000L, 450L));
        } else if (cycleId == 2L) {
            summaries.add(createUsage(5009L, 2001L, 2L, 1100L, 280L, 95L));
            summaries.add(createUsage(5010L, 2002L, 2L, 2600L, 620L, 260L));
            summaries.add(createUsage(5011L, 2003L, 2L, 5100L, 1250L, 520L));
            summaries.add(createUsage(5012L, 2004L, 2L, 750L, 220L, 70L));
            summaries.add(createUsage(5013L, 2005L, 2L, 1600L, 420L, 130L));
            summaries.add(createUsage(5014L, 2006L, 2L, 850L, 180L, 75L));
            summaries.add(createUsage(5015L, 2007L, 2L, 2100L, 520L, 190L));
            summaries.add(createUsage(5016L, 2008L, 2L, 4600L, 1050L, 460L));
        }

        return summaries;
    }

    @Override
    public List<UsageSummaryDto> getByCycleAndRegion(Long cycleId, Long regionId) {
        // For region filtering, return half the data
        List<UsageSummaryDto> all = getByCycle(cycleId);
        if (regionId == 1L) {
            return all.stream().filter(u -> u.getLineId() % 2 == 1).toList();
        } else if (regionId == 2L) {
            return all.stream().filter(u -> u.getLineId() % 2 == 0).toList();
        }
        return all;
    }

    private UsageSummaryDto createUsage(Long summaryId, Long lineId, Long cycleId, 
                                        Long dataMb, Long voiceMin, Long sms) {
        UsageSummaryDto usage = new UsageSummaryDto();
        usage.setSummaryId(summaryId);
        usage.setLineId(lineId);
        usage.setBillingCycleId(cycleId);
        usage.setDataUsedMb(dataMb);
        usage.setVoiceUsedMin(voiceMin);
        usage.setSmsUsed(sms);
        return usage;
    }
}
