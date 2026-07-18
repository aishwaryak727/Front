package com.teleconnect.billing_service.service;

import com.teleconnect.billing_service.dto.request.BillingCycleRequest;
import com.teleconnect.billing_service.dto.request.CycleGenerationRequest;
import com.teleconnect.billing_service.dto.response.BatchGenerationResponse;
import com.teleconnect.billing_service.dto.response.BillingCycleResponse;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BillingCycleService {

    BillingCycleResponse createBillingCycle(BillingCycleRequest request);

    BatchGenerationResponse generateInvoicesBatch(CycleGenerationRequest request);

    BillingCycleResponse getBillingCycleById(Long cycleId);

    List<BillingCycleResponse> getCyclesByAccount(Long accountId);

    Page<BillingCycleResponse> getCyclesByAccount(Long accountId, BillingCycleStatus status, Pageable pageable);

    List<BillingCycleResponse> getCyclesByStatus(BillingCycleStatus status);

    BillingCycleResponse updateCycleStatus(Long cycleId, BillingCycleStatus status);

    BillingCycleResponse closeBillingCycle(Long cycleId);
}
