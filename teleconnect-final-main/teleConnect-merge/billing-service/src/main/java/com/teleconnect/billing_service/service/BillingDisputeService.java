package com.teleconnect.billing_service.service;

import com.teleconnect.billing_service.dto.request.DisputeRequest;
import com.teleconnect.billing_service.dto.request.DisputeResolveRequest;
import com.teleconnect.billing_service.dto.request.DisputeReviewRequest;
import com.teleconnect.billing_service.dto.response.DisputeResponse;
import com.teleconnect.billing_service.enums.DisputeStatus;

import java.util.List;

public interface BillingDisputeService {

    DisputeResponse raiseDispute(DisputeRequest request);

    DisputeResponse getDisputeById(Long disputeId);

    List<DisputeResponse> getDisputesByInvoice(Long invoiceId);

    List<DisputeResponse> getDisputesBySubscriber(Long subscriberId);

    List<DisputeResponse> getDisputesByAccount(Long accountId, DisputeStatus status);

    List<DisputeResponse> getDisputesByStatus(DisputeStatus status);

    DisputeResponse updateDisputeStatus(Long disputeId, DisputeStatus status);

    DisputeResponse reviewDispute(Long disputeId, DisputeReviewRequest request);

    DisputeResponse resolveDispute(Long disputeId, DisputeResolveRequest request);
}
