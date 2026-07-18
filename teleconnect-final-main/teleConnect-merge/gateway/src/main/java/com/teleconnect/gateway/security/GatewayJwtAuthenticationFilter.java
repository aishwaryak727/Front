package com.teleconnect.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Global Gateway Filter for JWT Authentication.
 * 
 * This filter:
 * 1. Allows public endpoints without authentication
 * 2. Validates JWT token from Authorization header for protected endpoints
 * 3. Extracts user and permissions from token
 * 4. Injects X-Authenticated-User and X-Authenticated-Permissions headers
 * 5. Forwards authenticated request to downstream microservices
 */
@Slf4j
@Component
public class GatewayJwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Endpoints that do NOT require authentication
    private static final Set<String> PUBLIC_ENDPOINTS = new HashSet<>(Arrays.asList(
            "/teleConnect/iam/api/auth/login",
            "/teleConnect/iam/api/auth/register",
            "/health",
            "/actuator"
    ));

    public GatewayJwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "UNKNOWN";

        // Log incoming request
        log.debug("[Gateway Auth] {} {}", method, path);

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            log.debug("[Gateway Auth] public endpoint, skipping authentication: {}", path);
            return chain.filter(exchange);
        }

        // Extract Bearer token from Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[Gateway Auth] missing or invalid Authorization header for protected endpoint: {} {}", method, path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        // Validate token
        if (!jwtUtil.validateToken(token)) {
            log.warn("[Gateway Auth] invalid or expired token for: {} {}", method, path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Extract user and permissions from token
        String user = jwtUtil.extractEmail(token);
        List<String> permissions = jwtUtil.extractPermissions(token);

        if (user == null || user.isBlank()) {
            log.warn("[Gateway Auth] unable to extract user from token for: {} {}", method, path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        log.info("[Gateway Auth] authenticated user: {}, permissions: {}", user, permissions);

        // Create new request with authentication headers
        String permissionsHeader = String.join(",", permissions);
        exchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-Authenticated-User", user)
                        .header("X-Authenticated-Permissions", permissionsHeader)
                        .build())
                .build();

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Execute this filter early in the chain (before other global filters)
        return -100;
    }

    /**
     * Check if the request path is a public endpoint that doesn't require authentication.
     */
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }
}
