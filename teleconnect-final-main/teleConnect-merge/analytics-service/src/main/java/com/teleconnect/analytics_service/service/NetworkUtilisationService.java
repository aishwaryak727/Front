package com.teleconnect.analytics_service.service;

import com.teleconnect.analytics_service.dto.response.NetworkUtilisationResponse;

public interface NetworkUtilisationService {
    NetworkUtilisationResponse computeUtilisation(Long cycleId, String region);
}
