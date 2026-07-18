package com.teleconnect.analytics_service.service;

import com.teleconnect.analytics_service.dto.response.ChurnReportResponse;

import java.time.LocalDate;

public interface ChurnService {
    ChurnReportResponse computeChurn(LocalDate periodStart, LocalDate periodEnd, String region);
}
