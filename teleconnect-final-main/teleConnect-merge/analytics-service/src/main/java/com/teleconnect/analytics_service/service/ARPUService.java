package com.teleconnect.analytics_service.service;

import com.teleconnect.analytics_service.dto.response.ARPUReportResponse;

public interface ARPUService {
    ARPUReportResponse computeARPU(Long cycleId, String scope, String scopeValue);
}
