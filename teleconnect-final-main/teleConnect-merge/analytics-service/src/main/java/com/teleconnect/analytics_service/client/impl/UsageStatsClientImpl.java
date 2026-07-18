package com.teleconnect.analytics_service.client.impl;

import com.teleconnect.analytics_service.client.UsageStatsClient;
import com.teleconnect.analytics_service.dto.external.UsageSummaryDto;
import com.teleconnect.analytics_service.exception.ModuleClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
@ConditionalOnProperty(name = "mock.clients.enabled", havingValue = "false", matchIfMissing = true)
public class UsageStatsClientImpl implements UsageStatsClient {

    private static final String MODULE = "Usage & Consumption Tracking";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UsageStatsClientImpl(RestTemplate restTemplate,
                                 @Value("${teleconnect.modules.usage.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<UsageSummaryDto> getByCycle(Long cycleId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/usage/analytics/summaries")
                .queryParam("cycleId", cycleId)
                .toUriString();
        return safeGetList(url);
    }

    @Override
    public List<UsageSummaryDto> getByCycleAndRegion(Long cycleId, Long regionId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/usage/analytics/summaries")
                .queryParam("cycleId", cycleId)
                .queryParam("regionId", regionId)
                .toUriString();
        return safeGetList(url);
    }

    private List<UsageSummaryDto> safeGetList(String url) {
        try {
            UsageSummaryDto[] result = restTemplate.getForObject(url, UsageSummaryDto[].class);
            return result != null ? List.of(result) : List.of();
        } catch (RestClientException e) {
            throw new ModuleClientException(MODULE, "GET " + url + " failed: " + e.getMessage(), e);
        }
    }
}
