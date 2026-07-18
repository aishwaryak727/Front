package com.teleconnect.analytics_service.service;

import com.teleconnect.analytics_service.dto.response.SubscriberGrowthResponse;

import java.time.LocalDate;

public interface SubscriberGrowthService {
    SubscriberGrowthResponse computeGrowth(LocalDate periodStart, LocalDate periodEnd);
}
