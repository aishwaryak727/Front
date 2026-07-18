package com.teleconnect.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/subscriber")
    public Mono<ResponseEntity<Map<String, String>>> subscriberFallback() {
        return buildFallbackResponse("subscriber", "Subscriber service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping("/plan")
    public Mono<ResponseEntity<Map<String, String>>> planFallback() {
        return buildFallbackResponse("plan", "Plan service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping("/billing")
    public Mono<ResponseEntity<Map<String, String>>> billingFallback() {
        return buildFallbackResponse("billing", "Billing service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping("/usage")
    public Mono<ResponseEntity<Map<String, String>>> usageFallback() {
        return buildFallbackResponse("usage", "Usage service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping("/notification")
    public Mono<ResponseEntity<Map<String, String>>> notificationFallback() {
        return buildFallbackResponse("notification", "Notification service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping("/analytics")
    public Mono<ResponseEntity<Map<String, String>>> analyticsFallback() {
        return buildFallbackResponse("analytics", "Analytics service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping("/fault")
    public Mono<ResponseEntity<Map<String, String>>> faultFallback() {
        return buildFallbackResponse("fault", "Fault service is temporarily unavailable. Please try again later.");
    }

    private Mono<ResponseEntity<Map<String, String>>> buildFallbackResponse(String service, String message) {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "SERVICE_UNAVAILABLE",
                "service", service,
                "message", message
        )));
    }
}
