package com.teleconnect.billing_service.service.impl;

import lombok.extern.slf4j.Slf4j;

import com.teleconnect.billing_service.dto.request.BillingCycleRequest;
import com.teleconnect.billing_service.dto.request.CycleGenerationRequest;
import com.teleconnect.billing_service.dto.response.BatchGenerationResponse;
import com.teleconnect.billing_service.dto.response.BillingCycleResponse;
import com.teleconnect.billing_service.entity.BillingCycle;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.BillingCycleRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.service.BillingCycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BillingCycleServiceImpl implements BillingCycleService {

    private final BillingCycleRepository billingCycleRepository;
    private final InvoiceRepository invoiceRepository;

    public BillingCycleServiceImpl(BillingCycleRepository billingCycleRepository, InvoiceRepository invoiceRepository) {
        this.billingCycleRepository = billingCycleRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional
    public BillingCycleResponse createBillingCycle(BillingCycleRequest request) {
        log.info("Creating billing cycle for accountId={} start={} end={}",
                request.getAccountId(), request.getCycleStart(), request.getCycleEnd());
        if (request.getCycleEnd().isBefore(request.getCycleStart())) {
            log.warn("Invalid billing cycle dates for accountId={} start={} end={}",
                    request.getAccountId(), request.getCycleStart(), request.getCycleEnd());
            throw new BillingException("Cycle end date must be after cycle start date");
        }

        billingCycleRepository.findByAccountIdAndStatus(request.getAccountId(), BillingCycleStatus.OPEN)
                .ifPresent(c -> {
                    throw new BillingException(
                            "An open billing cycle already exists for account: " + request.getAccountId());
                });

        BillingCycle cycle = BillingCycle.builder()
                .accountId(request.getAccountId())
                .cycleStart(request.getCycleStart())
                .cycleEnd(request.getCycleEnd())
                .status(BillingCycleStatus.OPEN)
                .build();

        return toResponse(billingCycleRepository.save(cycle));
    }

    @Override
    @Transactional
    public BatchGenerationResponse generateInvoicesBatch(CycleGenerationRequest request) {
        log.info("Generating invoice batch for cycleDate={} dryRun={}", request.getCycleDate(), request.isDryRun());
        List<BillingCycle> eligible = billingCycleRepository
                .findByStatusAndCycleEndLessThanEqual(BillingCycleStatus.OPEN, request.getCycleDate());

        int processed = 0;
        int generated = 0;
        int skipped = 0;
        int errors = 0;

        for (BillingCycle cycle : eligible) {
            processed++;
            try {
                boolean invoiceExists = invoiceRepository
                        .findByAccountIdAndCycleId(cycle.getAccountId(), cycle.getCycleId())
                        .isPresent();
                if (invoiceExists) {
                    skipped++;
                    log.debug("Skipping cycle with existing invoice cycleId={}", cycle.getCycleId());
                    continue;
                }

                if (!request.isDryRun()) {
                    // Charge components are zero-initialised here: plan and usage data live in
                    // the Plan/Usage modules (out of this service's Phase 1 scope) and are
                    // populated via the explicit generate-invoice endpoint or later integration.
                    Invoice invoice = Invoice.builder()
                            .accountId(cycle.getAccountId())
                            .cycleId(cycle.getCycleId())
                            .planCharges(BigDecimal.ZERO)
                            .excessCharges(BigDecimal.ZERO)
                            .addOnCharges(BigDecimal.ZERO)
                            .taxes(BigDecimal.ZERO)
                            .totalAmount(BigDecimal.ZERO)
                            .dueDate(cycle.getCycleEnd().plusDays(15))
                            .status(InvoiceStatus.GENERATED)
                            .build();
                    invoiceRepository.save(invoice);

                    cycle.setStatus(BillingCycleStatus.GENERATED);
                    cycle.setGeneratedDate(LocalDate.now());
                    billingCycleRepository.save(cycle);
                }
                generated++;
            } catch (Exception ex) {
                errors++;
                log.error("Error generating invoices batch for cycleId={}", cycle.getCycleId(), ex);
            }
        }

        return new BatchGenerationResponse(
                processed, generated, skipped, errors, request.isDryRun(), LocalDateTime.now());
    }

    @Override
    public BillingCycleResponse getBillingCycleById(Long cycleId) {
        log.debug("Fetching billing cycle by id={}", cycleId);
        BillingCycleResponse response = toResponse(findById(cycleId));
        log.debug("Billing cycle retrieved cycleId={}", cycleId);
        return response;
    }

    @Override
    public List<BillingCycleResponse> getCyclesByAccount(Long accountId) {
        log.debug("Fetching billing cycles for accountId={}", accountId);
        List<BillingCycleResponse> response = billingCycleRepository.findByAccountId(accountId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        log.debug("Found {} cycles for accountId={}", response.size(), accountId);
        return response;
    }

    @Override
    public Page<BillingCycleResponse> getCyclesByAccount(Long accountId, BillingCycleStatus status, Pageable pageable) {
        log.debug("Fetching paging billing cycles for accountId={} status={} page={} size={}",
                accountId, status, pageable.getPageNumber(), pageable.getPageSize());
        Page<BillingCycle> page = (status == null)
                ? billingCycleRepository.findByAccountId(accountId, pageable)
                : billingCycleRepository.findByAccountIdAndStatus(accountId, status, pageable);
        return page.map(this::toResponse);
    }

    @Override
    public List<BillingCycleResponse> getCyclesByStatus(BillingCycleStatus status) {
        log.debug("Fetching billing cycles by status={}", status);
        List<BillingCycleResponse> response = billingCycleRepository.findByStatus(status)
                .stream().map(this::toResponse).collect(Collectors.toList());
        log.debug("Found {} cycles with status={}", response.size(), status);
        return response;
    }

    @Override
    @Transactional
    public BillingCycleResponse updateCycleStatus(Long cycleId, BillingCycleStatus status) {
        log.info("Updating billing cycle status cycleId={} newStatus={}", cycleId, status);
        BillingCycle cycle = findById(cycleId);
        cycle.setStatus(status);
        if (status == BillingCycleStatus.GENERATED) {
            cycle.setGeneratedDate(LocalDate.now());
        }
        BillingCycle updated = billingCycleRepository.save(cycle);
        log.info("Billing cycle status updated cycleId={} status={}", updated.getCycleId(), updated.getStatus());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public BillingCycleResponse closeBillingCycle(Long cycleId) {
        log.info("Closing billing cycle cycleId={}", cycleId);
        BillingCycle cycle = findById(cycleId);
        if (cycle.getStatus() == BillingCycleStatus.CLOSED) {
            log.warn("Billing cycle already closed cycleId={}", cycleId);
            throw new BillingException("Billing cycle is already closed");
        }
        cycle.setStatus(BillingCycleStatus.CLOSED);
        BillingCycle updated = billingCycleRepository.save(cycle);
        log.info("Billing cycle closed cycleId={}", updated.getCycleId());
        return toResponse(updated);
    }

    private BillingCycle findById(Long cycleId) {
        log.debug("Looking up billing cycle cycleId={}", cycleId);
        return billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Billing cycle not found with ID: " + cycleId));
    }

    private BillingCycleResponse toResponse(BillingCycle cycle) {
        return BillingCycleResponse.builder()
                .cycleId(cycle.getCycleId())
                .accountId(cycle.getAccountId())
                .cycleStart(cycle.getCycleStart())
                .cycleEnd(cycle.getCycleEnd())
                .generatedDate(cycle.getGeneratedDate())
                .status(cycle.getStatus())
                .createdAt(cycle.getCreatedAt())
                .updatedAt(cycle.getUpdatedAt())
                .build();
    }
}
