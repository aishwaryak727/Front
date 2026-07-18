package com.teleconnect.analytics_service.client.impl;

import com.teleconnect.analytics_service.client.SubscriberStatsClient;
import com.teleconnect.analytics_service.dto.external.SimLineDto;
import com.teleconnect.analytics_service.dto.external.SubscriberAccountDto;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.exception.ModuleClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(name = "mock.clients.enabled", havingValue = "false", matchIfMissing = true)
public class SubscriberStatsClientImpl implements SubscriberStatsClient {

    private static final String MODULE = "Subscriber & SIM Management";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SubscriberStatsClientImpl(RestTemplate restTemplate,
                                      @Value("${teleconnect.modules.subscriber.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public long countByStatus(AccountStatus status) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/subscribers/analytics/count")
                .queryParam("status", status)
                .toUriString();
        return safeGet(url, Long.class, 0L);
    }

    @Override
    public long countActiveRegisteredBefore(LocalDate date) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/subscribers/analytics/count")
                .queryParam("status", AccountStatus.ACTIVE)
                .queryParam("registeredBefore", date)
                .toUriString();
        return safeGet(url, Long.class, 0L);
    }

    @Override
    public long countActiveByRegion(Long regionId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/subscribers/analytics/count")
                .queryParam("status", AccountStatus.ACTIVE)
                .queryParam("regionId", regionId)
                .toUriString();
        return safeGet(url, Long.class, 0L);
    }

    @Override
    public List<SubscriberAccountDto> getActiveAccounts() {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/subscribers/analytics/accounts")
                .queryParam("status", AccountStatus.ACTIVE)
                .toUriString();
        return safeGetList(url, SubscriberAccountDto[].class);
    }

    @Override
    public List<SubscriberAccountDto> getByStatusAndRegistrationBetween(AccountStatus status, LocalDate start, LocalDate end) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/subscribers/analytics/accounts")
                .queryParam("status", status)
                .queryParam("registeredFrom", start)
                .queryParam("registeredTo", end)
                .toUriString();
        return safeGetList(url, SubscriberAccountDto[].class);
    }

    @Override
    public List<SubscriberAccountDto> getTerminatedInPeriod(LocalDate start, LocalDate end) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/subscribers/analytics/accounts")
                .queryParam("status", AccountStatus.TERMINATED)
                .queryParam("statusChangedFrom", start)
                .queryParam("statusChangedTo", end)
                .toUriString();
        return safeGetList(url, SubscriberAccountDto[].class);
    }

    @Override
    public List<SimLineDto> getPortedOutInPeriod(LocalDate start, LocalDate end) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/subscribers/analytics/simlines/ported-out")
                .queryParam("from", start)
                .queryParam("to", end)
                .toUriString();
        return safeGetList(url, SimLineDto[].class);
    }

    @Override
    public List<SimLineDto> getActivatedInPeriod(LocalDate start, LocalDate end) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/subscribers/analytics/simlines/activated")
                .queryParam("from", start)
                .queryParam("to", end)
                .toUriString();
        return safeGetList(url, SimLineDto[].class);
    }

    // ─── helpers ──────────────────────────────────────────────────────────

    private <T> T safeGet(String url, Class<T> type, T fallback) {
        try {
            T result = restTemplate.getForObject(url, type);
            return result != null ? result : fallback;
        } catch (RestClientException e) {
            throw new ModuleClientException(MODULE, "GET " + url + " failed: " + e.getMessage(), e);
        }
    }

    private <T> List<T> safeGetList(String url, Class<T[]> arrayType) {
        try {
            T[] result = restTemplate.getForObject(url, arrayType);
            return result != null ? List.of(result) : List.of();
        } catch (RestClientException e) {
            throw new ModuleClientException(MODULE, "GET " + url + " failed: " + e.getMessage(), e);
        }
    }
}
