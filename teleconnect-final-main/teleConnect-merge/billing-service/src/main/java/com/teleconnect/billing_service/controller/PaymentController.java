package com.teleconnect.billing_service.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.PaymentResponse;
import com.teleconnect.billing_service.service.PaymentService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/billing/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final AuditClient auditClient;

    public PaymentController(PaymentService paymentService, AuditClient auditClient) {
        this.paymentService = paymentService;
        this.auditClient = auditClient;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized PaymentController");
    }

    /**
     * POST /api/billing/payments
     * Record a payment against an invoice.
     * Validates invoice status, amount, and duplicate transaction reference.
     * Marks the invoice as PAID on success.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_INVOICE')")
    public ResponseEntity<PaymentResponse> makePayment(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpReq) {
        PaymentResponse response = paymentService.makePayment(request);
        auditClient.record(AuditAction.RECORD_PAYMENT, AuditModule.BILLING, httpReq);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/billing/payments/{paymentId}
     * Retrieve a single payment record by its ID.
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('PAY_BILL')")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    /**
     * GET /api/billing/payments/invoice/{invoiceId}
     * Retrieve all payment records for a given invoice.
     */
    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasAuthority('PAY_BILL')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByInvoice(
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(paymentService.getPaymentsByInvoice(invoiceId));
    }
}
