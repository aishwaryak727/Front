package com.teleconnect.analytics_service.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Shared RestTemplate used by every module client (SubscriberStatsClient,
 * BillingStatsClient, UsageStatsClient, FaultStatsClient) to call the
 * analytics-facing API exposed by each peer module. Uses the JDK's
 * built-in java.net.http.HttpClient as the request factory (no extra
 * dependency needed) since it supports PATCH, unlike the default
 * SimpleClientHttpRequestFactory. Timeouts are kept short and explicit
 * so a slow/unavailable module cannot stall analytics requests
 * indefinitely — callers are still responsible for catching
 * RestClientException around individual calls (see ModuleClientException).
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkHttpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));

        return builder
                .requestFactory(() -> factory)
                .build();
    }
}
