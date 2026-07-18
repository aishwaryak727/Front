package com.teleconnect.billing_service.service.impl;

import lombok.extern.slf4j.Slf4j;

import com.teleconnect.billing_service.dto.request.DisputeRequest;
import com.teleconnect.billing_service.dto.request.DisputeResolveRequest;
import com.teleconnect.billing_service.dto.request.DisputeReviewRequest;
import com.teleconnect.billing_service.dto.response.DisputeResponse;
import com.teleconnect.billing_service.entity.BillingDispute;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.DisputeStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.BillingDisputeRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.service.BillingDisputeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BillingDisputeServiceImpl implements BillingDisputeService {

    private final BillingDisputeRepository disputeRepository;
    private final InvoiceRepository invoiceRepository;

    public BillingDisputeServiceImpl(BillingDisputeRepository disputeRepository, InvoiceRepository invoiceRepository) {
        this.disputeRepository = disputeRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional
    public DisputeResponse raiseDispute(DisputeRequest request) {
        log.info("Raising dispute for invoiceId={} subscriberId={} amount={}", request.getInvoiceId(), request.getSubscriberId(), request.getDisputedAmount());
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found: " + request.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            log.warn("Dispute attempt on paid invoice invoiceId={}", request.getInvoiceId());
            throw new BillingException("Cannot raise a dispute on an already paid invoice");
        }
        if (invoice.getStatus() == InvoiceStatus.DISPUTED) {
            log.warn("Dispute attempt on already disputed invoice invoiceId={}", request.getInvoiceId());
            throw new BillingException("A dispute is already open for this invoice");
        }
        if (request.getDisputedAmount().compareTo(invoice.getTotalAmount()) > 0) {
            log.warn("Disputed amount exceeds invoice total invoiceId={} disputedAmount={} totalAmount={}",
                    request.getInvoiceId(), request.getDisputedAmount(), invoice.getTotalAmount());
            throw new BillingException(
                    "Disputed amount cannot exceed the invoice total of " + invoice.getTotalAmount());
        }

        // subscriberId is optional — fall back to invoice's accountId
        Long subscriberId = (request.getSubscriberId() != null)
                ? request.getSubscriberId()
                : invoice.getAccountId();

        BillingDispute dispute = BillingDispute.builder()
                .invoiceId(request.getInvoiceId())
                .subscriberId(subscriberId)
                .disputeReason(request.getDisputeReason())
                .description(request.getDescription())
                .disputedAmount(request.getDisputedAmount())
                .raisedDate(LocalDate.now())
                .status(DisputeStatus.OPEN)
                .build();

        invoice.setStatus(InvoiceStatus.DISPUTED);
        invoiceRepository.save(invoice);

        BillingDispute savedDispute = disputeRepository.save(dispute);
        log.info("Billing dispute created disputeId={} invoiceId={}", savedDispute.getDisputeId(), savedDispute.getInvoiceId());
        return toResponse(savedDispute);
    }

    @Override
    public DisputeResponse getDisputeById(Long disputeId) {
        log.debug("Fetching dispute by id={}", disputeId);
        DisputeResponse response = toResponse(findById(disputeId));
        log.debug("Dispute retrieved disputeId={}", disputeId);
        return response;
    }

    @Override
    public List<DisputeResponse> getDisputesByInvoice(Long invoiceId) {
        log.debug("Fetching disputes for invoiceId={}", invoiceId);
        List<DisputeResponse> disputes = disputeRepository.findByInvoiceId(invoiceId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        log.debug("Found {} disputes for invoiceId={}", disputes.size(), invoiceId);
        return disputes;
    }

    @Override
    public List<DisputeResponse> getDisputesBySubscriber(Long subscriberId) {
        log.debug("Fetching disputes for subscriberId={}", subscriberId);
        List<DisputeResponse> disputes = disputeRepository.findBySubscriberId(subscriberId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        log.debug("Found {} disputes for subscriberId={}", disputes.size(), subscriberId);
        return disputes;
    }

    @Override
    public List<DisputeResponse> getDisputesByAccount(Long accountId, DisputeStatus status) {
        log.debug("Fetching disputes for accountId={} status={}", accountId, status);
        List<BillingDispute> disputes = disputeRepository.findBySubscriberId(accountId);
        if (status != null) {
            disputes = disputes.stream()
                    .filter(d -> d.getStatus() == status)
                    .collect(Collectors.toList());
        }
        List<DisputeResponse> responses = disputes.stream().map(this::toResponse).collect(Collectors.toList());
        log.debug("Found {} disputes for accountId={} status={}", responses.size(), accountId, status);
        return responses;
    }

    @Override
    public List<DisputeResponse> getDisputesByStatus(DisputeStatus status) {
        log.debug("Fetching disputes by status={}", status);
        List<DisputeResponse> disputes = disputeRepository.findByStatus(status)
                .stream().map(this::toResponse).collect(Collectors.toList());
        log.debug("Found {} disputes with status={}", disputes.size(), status);
        return disputes;
    }

    @Override
    @Transactional
    public DisputeResponse updateDisputeStatus(Long disputeId, DisputeStatus status) {
        log.info("Updating dispute status disputeId={} status={}", disputeId, status);
        BillingDispute dispute = findById(disputeId);

        if (dispute.getStatus() == DisputeStatus.RESOLVED || dispute.getStatus() == DisputeStatus.REJECTED) {
            throw new BillingException("Cannot update a dispute that is already " + dispute.getStatus());
        }

        dispute.setStatus(status);

        if (status == DisputeStatus.RESOLVED || status == DisputeStatus.REJECTED) {
            dispute.setResolvedDate(LocalDateTime.now());
            restoreInvoiceStatus(dispute.getInvoiceId());
        }

        BillingDispute updated = disputeRepository.save(dispute);
        log.info("Dispute resolved disputeId={} newStatus={}", updated.getDisputeId(), updated.getStatus());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public DisputeResponse reviewDispute(Long disputeId, DisputeReviewRequest request) {
        log.info("Reviewing dispute disputeId={} assignedTo={}", disputeId, request.getAssignedTo());
        BillingDispute dispute = findById(disputeId);

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new BillingException(
                    "Only OPEN disputes can be moved to Under Review. Current status: " + dispute.getStatus());
        }

        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        dispute.setAssignedTo(request.getAssignedTo());
        dispute.setAcknowledgedDate(LocalDateTime.now());

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            dispute.setResolutionNotes(request.getNotes());
        }

        return toResponse(disputeRepository.save(dispute));
    }

    @Override
    @Transactional
    public DisputeResponse resolveDispute(Long disputeId, DisputeResolveRequest request) {
        log.info("Resolving dispute disputeId={} resolution={}", disputeId, request.getResolution());
        BillingDispute dispute = findById(disputeId);

        if (dispute.getStatus() == DisputeStatus.RESOLVED || dispute.getStatus() == DisputeStatus.REJECTED) {
            throw new BillingException("Dispute is already closed with status: " + dispute.getStatus());
        }

        boolean isResolved = "Resolved".equalsIgnoreCase(request.getResolution());
        DisputeStatus newStatus = isResolved ? DisputeStatus.RESOLVED : DisputeStatus.REJECTED;

        if (!isResolved && (request.getResolutionNotes() == null || request.getResolutionNotes().isBlank())) {
            throw new BillingException("Resolution notes are mandatory when rejecting a dispute");
        }

        dispute.setStatus(newStatus);
        dispute.setResolvedDate(LocalDateTime.now());
        dispute.setResolutionNotes(request.getResolutionNotes());

        if (isResolved && request.getCreditAmount() != null) {
            dispute.setResolvedAmount(request.getCreditAmount());
        }

        restoreInvoiceStatus(dispute.getInvoiceId());

        return toResponse(disputeRepository.save(dispute));
    }

    private void restoreInvoiceStatus(Long invoiceId) {
        log.debug("Restoring invoice status for invoiceId={}", invoiceId);
        invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
            InvoiceStatus restored = invoice.getDueDate().isBefore(LocalDate.now())
                    ? InvoiceStatus.OVERDUE
                    : InvoiceStatus.SENT;
            invoice.setStatus(restored);
            invoiceRepository.save(invoice);
            log.info("Invoice status restored invoiceId={} status={}", invoiceId, restored);
        });
    }

    private BillingDispute findById(Long disputeId) {
        log.debug("Looking up dispute disputeId={}", disputeId);
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dispute not found with ID: " + disputeId));
    }

    private DisputeResponse toResponse(BillingDispute dispute) {
        return DisputeResponse.builder()
                .disputeId(dispute.getDisputeId())
                .invoiceId(dispute.getInvoiceId())
                .subscriberId(dispute.getSubscriberId())
                .disputeReason(dispute.getDisputeReason())
                .description(dispute.getDescription())
                .disputedAmount(dispute.getDisputedAmount())
                .resolvedAmount(dispute.getResolvedAmount())
                .raisedDate(dispute.getRaisedDate())
                .acknowledgedDate(dispute.getAcknowledgedDate())
                .resolvedDate(dispute.getResolvedDate())
                .assignedTo(dispute.getAssignedTo())
                .resolutionNotes(dispute.getResolutionNotes())
                .status(dispute.getStatus())
                .build();
    }
}
