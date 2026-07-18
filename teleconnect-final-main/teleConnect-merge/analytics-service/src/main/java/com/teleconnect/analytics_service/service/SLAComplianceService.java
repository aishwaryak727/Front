package com.teleconnect.analytics_service.service;

import com.teleconnect.analytics_service.dto.response.SLAComplianceResponse;

import java.time.LocalDate;

public interface SLAComplianceService {
    SLAComplianceResponse computeSLACompliance(LocalDate periodStart, LocalDate periodEnd);
    void escalateBreachingTickets();
}
