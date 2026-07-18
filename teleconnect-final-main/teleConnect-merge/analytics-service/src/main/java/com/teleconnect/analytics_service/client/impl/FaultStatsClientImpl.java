package com.teleconnect.analytics_service.client.impl;

import com.teleconnect.analytics_service.client.FaultStatsClient;
import com.teleconnect.analytics_service.dto.external.FaultTicketDto;
import com.teleconnect.analytics_service.exception.ModuleClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "mock.clients.enabled", havingValue = "false", matchIfMissing = true)
public class FaultStatsClientImpl implements FaultStatsClient {

    private static final String MODULE = "Fault & Service Request Management";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public FaultStatsClientImpl(RestTemplate restTemplate,
                                 @Value("${teleconnect.modules.fault.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<FaultTicketDto> getClosedInPeriod(LocalDateTime start, LocalDateTime end) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/faults/analytics/tickets")
                .queryParam("status", "RESOLVED,CLOSED")
                .queryParam("raisedFrom", start)
                .queryParam("raisedTo", end)
                .toUriString();
        return safeGetList(url);
    }

    @Override
    public List<FaultTicketDto> getOpenBreachingBefore(LocalDateTime threshold) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/faults/analytics/tickets")
                .queryParam("status", "OPEN,IN_PROGRESS")
                .queryParam("raisedBefore", threshold)
                .toUriString();
        return safeGetList(url);
    }

    @Override
    public long countOpenByAccountSince(Long accountId, LocalDateTime since) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/faults/analytics/tickets/count")
                .queryParam("accountId", accountId)
                .queryParam("status", "OPEN,IN_PROGRESS")
                .queryParam("raisedFrom", since)
                .toUriString();
        try {
            Long result = restTemplate.getForObject(url, Long.class);
            return result != null ? result : 0L;
        } catch (RestClientException e) {
            throw new ModuleClientException(MODULE, "GET " + url + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public long countEscalated() {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/faults/analytics/tickets/count")
                .queryParam("status", "ESCALATED")
                .toUriString();
        try {
            Long result = restTemplate.getForObject(url, Long.class);
            return result != null ? result : 0L;
        } catch (RestClientException e) {
            throw new ModuleClientException(MODULE, "GET " + url + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void escalateTicket(Long ticketId) {
        String url = baseUrl + "/teleConnect/api/faults/tickets/" + ticketId + "/escalate";
        try {
            restTemplate.patchForObject(url, null, Void.class);
        } catch (RestClientException e) {
            throw new ModuleClientException(MODULE, "PATCH " + url + " failed: " + e.getMessage(), e);
        }
    }

    private List<FaultTicketDto> safeGetList(String url) {
        try {
            FaultTicketDto[] result = restTemplate.getForObject(url, FaultTicketDto[].class);
            return result != null ? List.of(result) : List.of();
        } catch (RestClientException e) {
            throw new ModuleClientException(MODULE, "GET " + url + " failed: " + e.getMessage(), e);
        }
    }
}
