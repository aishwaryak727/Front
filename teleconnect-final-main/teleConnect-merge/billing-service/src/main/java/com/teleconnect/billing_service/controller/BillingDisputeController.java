package com.teleconnect.billing_service.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.billing_service.dto.request.DisputeRequest;
import com.teleconnect.billing_service.dto.request.DisputeResolveRequest;
import com.teleconnect.billing_service.dto.request.DisputeReviewRequest;
import com.teleconnect.billing_service.dto.response.DisputeResponse;
import com.teleconnect.billing_service.dto.response.MessageResponse;
import com.teleconnect.billing_service.enums.DisputeStatus;
import com.teleconnect.billing_service.service.BillingDisputeService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/billing/disputes")
public class BillingDisputeController {

    private final BillingDisputeService disputeService;
    private final AuditClient auditClient;

    public BillingDisputeController(BillingDisputeService disputeService, AuditClient auditClient) {
        this.disputeService = disputeService;
        this.auditClient = auditClient;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized BillingDisputeController");
    }

    // ── Static-path endpoints first (must come before /{disputeId}) ────────────

    /**
     * POST /teleConnect/billing/disputes
     * Body: { "invoiceId": 7001, "disputeReason": "ExcessData",
     *         "disputedAmount": 173.60, "description": "..." }
     */
    @PostMapping
    @PreAuthorize("hasAuthority('BILLING_DISPUTE')")
    public ResponseEntity<MessageResponse> raiseDispute(@Valid @RequestBody DisputeRequest request,
            HttpServletRequest httpReq) {
        disputeService.raiseDispute(request);
        auditClient.record(AuditAction.RAISE_DISPUTE, AuditModule.BILLING, httpReq);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Billing dispute raised successfully"));
    }

    /**
     * GET /teleConnect/billing/disputes/account/{accountId}
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAuthority('BILLING_DISPUTE')")
    public ResponseEntity<List<DisputeResponse>> getDisputesByAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) DisputeStatus status) {
        List<DisputeResponse> data = disputeService.getDisputesByAccount(accountId, status);
        return ResponseEntity.ok(data);
    }

    /**
     * GET /teleConnect/billing/disputes/invoice/{invoiceId}
     */
    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasAuthority('EDIT_DISPUTE')")
    public ResponseEntity<List<DisputeResponse>> getDisputesByInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(disputeService.getDisputesByInvoice(invoiceId));
    }

    /**
     * GET /teleConnect/billing/disputes/subscriber/{subscriberId}
     */
    @GetMapping("/subscriber/{subscriberId}")
    @PreAuthorize("hasAuthority('EDIT_DISPUTE')")
    public ResponseEntity<List<DisputeResponse>> getDisputesBySubscriber(@PathVariable Long subscriberId) {
        return ResponseEntity.ok(disputeService.getDisputesBySubscriber(subscriberId));
    }

    /**
     * GET /teleConnect/billing/disputes/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('EDIT_DISPUTE')")
    public ResponseEntity<List<DisputeResponse>> getDisputesByStatus(@PathVariable DisputeStatus status) {
        return ResponseEntity.ok(disputeService.getDisputesByStatus(status));
    }

    // ── Dynamic /{disputeId} endpoints below ───────────────────────────────────

    /**
     * GET /teleConnect/billing/disputes/{disputeId}
     */
    @GetMapping("/{disputeId}")
    @PreAuthorize("hasAuthority('BILLING_DISPUTE')")
    public ResponseEntity<DisputeResponse> getDispute(@PathVariable Long disputeId) {
        return ResponseEntity.ok(disputeService.getDisputeById(disputeId));
    }

    /**
     * PUT /teleConnect/billing/disputes/{disputeId}/review
     * Body: { "assignedTo": "exec-201", "notes": "Reviewing UsageSummary for May cycle" }
     */
    @PutMapping("/{disputeId}/review")
    @PreAuthorize("hasAuthority('EDIT_DISPUTE')")
    public ResponseEntity<MessageResponse> reviewDispute(
            @PathVariable Long disputeId,
            @Valid @RequestBody DisputeReviewRequest request,
            HttpServletRequest httpReq) {
        disputeService.reviewDispute(disputeId, request);
        auditClient.record(AuditAction.REVIEW_DISPUTE, AuditModule.BILLING, httpReq);
        return ResponseEntity.ok(new MessageResponse("Dispute moved to Under Review"));
    }

    /**
     * PUT /teleConnect/billing/disputes/{disputeId}/resolve
     * Body: { "resolution": "Resolved", "creditAmount": 173.60, "resolutionNotes": "..." }
     */
    @PutMapping("/{disputeId}/resolve")
    @PreAuthorize("hasAuthority('EDIT_DISPUTE')")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable Long disputeId,
            @Valid @RequestBody DisputeResolveRequest request,
            HttpServletRequest httpReq) {
        DisputeResponse result = disputeService.resolveDispute(disputeId, request);
        auditClient.record(AuditAction.RESOLVE_DISPUTE, AuditModule.BILLING, httpReq);
        return ResponseEntity.ok(result);
    }

    /**
     * PUT /teleConnect/billing/disputes/{disputeId}/status
     */
    @PutMapping("/{disputeId}/status")
    @PreAuthorize("hasAuthority('EDIT_DISPUTE')")
    public ResponseEntity<DisputeResponse> updateStatus(
            @PathVariable Long disputeId,
            @RequestParam DisputeStatus status,
            HttpServletRequest httpReq) {
        DisputeResponse result = disputeService.updateDisputeStatus(disputeId, status);
        auditClient.record(AuditAction.UPDATE_DISPUTE_STATUS, AuditModule.BILLING, httpReq);
        return ResponseEntity.ok(result);
    }
}
