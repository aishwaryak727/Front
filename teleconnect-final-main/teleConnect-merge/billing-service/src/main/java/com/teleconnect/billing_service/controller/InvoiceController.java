package com.teleconnect.billing_service.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.billing_service.dto.request.InvoiceGenerationRequest;
import com.teleconnect.billing_service.dto.request.LateFeeRequest;
import com.teleconnect.billing_service.dto.request.LateFeeWaiverRequest;
import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.InvoiceResponse;
import com.teleconnect.billing_service.dto.response.MessageResponse;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.service.InvoiceService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/billing/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final AuditClient auditClient;

    public InvoiceController(InvoiceService invoiceService, AuditClient auditClient) {
        this.invoiceService = invoiceService;
        this.auditClient = auditClient;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized InvoiceController");
    }

    // ── Static-path endpoints first (must come before /{invoiceId}) ────────────

    /**
     * POST /teleConnect/billing/invoices/generate
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('EDIT_INVOICE')")
    public ResponseEntity<InvoiceResponse> generateInvoice(
            @Valid @RequestBody InvoiceGenerationRequest request,
            HttpServletRequest httpReq) {
        InvoiceResponse result = invoiceService.generateInvoice(request);
        auditClient.record(AuditAction.GENERATE_INVOICE, AuditModule.BILLING, httpReq);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * GET /teleConnect/billing/invoices/account/{accountId}
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAuthority('VIEW_INVOICE')")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        List<InvoiceResponse> data = invoiceService.getInvoicesByAccount(accountId, status, fromDate, toDate);
        return ResponseEntity.ok(data);
    }

    /**
     * GET /teleConnect/billing/invoices/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('EDIT_INVOICE')")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByStatus(@PathVariable InvoiceStatus status) {
        return ResponseEntity.ok(invoiceService.getInvoicesByStatus(status));
    }

    /**
     * PUT /teleConnect/billing/invoices/mark-overdue
     */
    @PutMapping("/mark-overdue")
    @PreAuthorize("hasAuthority('EDIT_INVOICE')")
    public ResponseEntity<MessageResponse> markOverdue(HttpServletRequest httpReq) {
        invoiceService.markOverdueInvoices();
        auditClient.record(AuditAction.MARK_INVOICES_OVERDUE, AuditModule.BILLING, httpReq);
        return ResponseEntity.ok(new MessageResponse("Overdue invoices updated successfully"));
    }

    // ── Dynamic /{invoiceId} endpoints below ───────────────────────────────────

    /**
     * GET /teleConnect/billing/invoices/{invoiceId}
     */
    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAuthority('VIEW_INVOICE')")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(invoiceId));
    }

    /**
     * PUT /teleConnect/billing/invoices/{invoiceId}/send
     */
    @PutMapping("/{invoiceId}/send")
    @PreAuthorize("hasAuthority('EDIT_INVOICE')")
    public ResponseEntity<InvoiceResponse> sendInvoice(@PathVariable Long invoiceId,
            HttpServletRequest httpReq) {
        InvoiceResponse result = invoiceService.sendInvoice(invoiceId);
        auditClient.record(AuditAction.SEND_INVOICE, AuditModule.BILLING, httpReq);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /teleConnect/billing/invoices/{invoiceId}/pay
     * Body: { "amountPaid": 949.32, "paymentMethod": "UPI", "transactionRef": "TXN98765" }
     */
    @PostMapping("/{invoiceId}/pay")
    @PreAuthorize("hasAuthority('PAY_BILL')")
    public ResponseEntity<MessageResponse> payInvoice(
            @PathVariable Long invoiceId,
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpReq) {
        invoiceService.payInvoice(invoiceId, request);
        auditClient.record(AuditAction.RECORD_PAYMENT, AuditModule.BILLING, httpReq);
        return ResponseEntity.ok(new MessageResponse("Payment recorded successfully"));
    }

    /**
     * POST /teleConnect/billing/invoices/{invoiceId}/latefee
     * Body: { "feeAmount": 100.00, "reason": "Overdue past grace period" }
     */
    @PostMapping("/{invoiceId}/latefee")
    @PreAuthorize("hasAuthority('EDIT_INVOICE')")
    public ResponseEntity<MessageResponse> applyLateFee(
            @PathVariable Long invoiceId,
            @Valid @RequestBody LateFeeRequest request,
            HttpServletRequest httpReq) {
        invoiceService.applyLateFee(invoiceId, request);
        auditClient.record(AuditAction.APPLY_LATE_FEE, AuditModule.BILLING, httpReq);
        return ResponseEntity.ok(new MessageResponse("Late fee applied successfully"));
    }

    /**
     * POST /teleConnect/billing/invoices/{invoiceId}/latefee/waive
     * Body: { "waiverReason": "Goodwill gesture", "authorisedBy": "user-501" }
     */
    @PostMapping("/{invoiceId}/latefee/waive")
    @PreAuthorize("hasAuthority('EDIT_INVOICE')")
    public ResponseEntity<MessageResponse> waiveLateFee(
            @PathVariable Long invoiceId,
            @Valid @RequestBody LateFeeWaiverRequest request,
            HttpServletRequest httpReq) {
        invoiceService.waiveLateFee(invoiceId, request);
        auditClient.record(AuditAction.WAIVE_LATE_FEE, AuditModule.BILLING, httpReq);
        return ResponseEntity.ok(new MessageResponse("Late fee waived successfully"));
    }

    /**
     * GET /teleConnect/billing/invoices/{invoiceId}/download
     */
    @GetMapping("/{invoiceId}/download")
    @PreAuthorize("hasAuthority('VIEW_INVOICE')")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long invoiceId) {
        byte[] pdfBytes = invoiceService.downloadInvoicePdf(invoiceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Invoice_" + invoiceId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    /**
     * GET /teleConnect/billing/invoices/account/{accountId}/statement
     * Downloads a full account statement PDF with all billing cycles and charges.
     */
    @GetMapping("/account/{accountId}/statement")
    @PreAuthorize("hasAuthority('VIEW_INVOICE')")
    public ResponseEntity<byte[]> downloadAccountStatement(@PathVariable Long accountId) {
        byte[] pdfBytes = invoiceService.downloadAccountStatementPdf(accountId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Statement_Account_" + accountId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
