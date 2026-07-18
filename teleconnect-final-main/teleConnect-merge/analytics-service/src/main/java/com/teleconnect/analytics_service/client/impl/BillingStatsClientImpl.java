package com.teleconnect.analytics_service.client.impl;

import com.teleconnect.analytics_service.client.BillingStatsClient;
import com.teleconnect.analytics_service.dto.external.InvoiceDto;
import com.teleconnect.analytics_service.enums.InvoiceStatus;
import com.teleconnect.analytics_service.exception.ModuleClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "mock.clients.enabled", havingValue = "false", matchIfMissing = true)
public class BillingStatsClientImpl implements BillingStatsClient {

    private static final String MODULE = "Billing & Invoice Management";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public BillingStatsClientImpl(RestTemplate restTemplate,
                                   @Value("${teleconnect.modules.billing.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<InvoiceDto> getInvoicesByCycle(Long cycleId, List<InvoiceStatus> statuses) {
        String statusParam = statuses.stream().map(Enum::name).collect(Collectors.joining(","));
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/billing/analytics/invoices")
                .queryParam("cycleId", cycleId)
                .queryParam("statuses", statusParam)
                .toUriString();
        try {
            InvoiceDto[] result = restTemplate.getForObject(url, InvoiceDto[].class);
            return result != null ? List.of(result) : List.of();
        } catch (RestClientException e) {
            throw new ModuleClientException(MODULE, "GET " + url + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public long countDisputesBySubscriberSince(Long subscriberId, LocalDate since) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/teleConnect/api/billing/analytics/disputes/count")
                .queryParam("subscriberId", subscriberId)
                .queryParam("since", since)
                .toUriString();
        try {
            Long result = restTemplate.getForObject(url, Long.class);
            return result != null ? result : 0L;
        } catch (RestClientException e) {
            throw new ModuleClientException(MODULE, "GET " + url + " failed: " + e.getMessage(), e);
        }
    }
}
