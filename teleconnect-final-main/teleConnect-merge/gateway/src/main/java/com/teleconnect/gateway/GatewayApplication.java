package com.teleconnect.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

/**
 * TeleConnect API Gateway.
 *
 * Single entry point (port 9090) that:
 * 1. Routes incoming requests to appropriate downstream microservice based on URL path prefix
 * 2. Handles centralized JWT authentication
 * 3. Validates tokens and injects user identity headers for downstream services
 *
 * Routing by path:
 *   /teleConnect/iam/**      -> IAM service        (8081)
 *   /teleConnect/api/**      -> Subscriber service (8086)
 *   /teleConnect/plan/**     -> Plan service       (8083)
 *   /teleConnect/usage/**    -> Usage service      (8084)
 *   /teleConnect/billing/**  -> Billing service    (8085)
 *   /teleConnect/notifications/** -> Notification service (8087)
 *   /teleConnect/fault/**    -> Fault service      (8090)
 *
 * Authentication:
 * - Public endpoints (/teleConnect/iam/api/auth/login, /auth/register) require no token
 * - Protected endpoints require Bearer token in Authorization header
 * - Gateway validates JWT and extracts user identity + permissions
 * - Forwards requests with X-Authenticated-User and X-Authenticated-Permissions headers
 * - Downstream services rely on these headers for authorization
 *
 * Routes are declared in application.yml.
 */
@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public GlobalFilter gatewayLoggingFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            log.info("[Gateway] incoming {} {} auth={}",
                    exchange.getRequest().getMethod(),
                    path,
                    auth != null ? "present" : "missing");
            return chain.filter(exchange)
                    .doOnSuccess(aVoid -> log.info("[Gateway] finished {} {}", exchange.getRequest().getMethod(), path));
        };
    }
}
