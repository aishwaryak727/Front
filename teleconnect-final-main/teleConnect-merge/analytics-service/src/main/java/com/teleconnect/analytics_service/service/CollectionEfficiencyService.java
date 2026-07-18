package com.teleconnect.analytics_service.service;

import com.teleconnect.analytics_service.dto.response.CollectionEfficiencyResponse;

public interface CollectionEfficiencyService {
    CollectionEfficiencyResponse computeCollectionEfficiency(Long cycleId);
}
