package com.teleconnect.billing_service.service;

import com.teleconnect.billing_service.dto.request.InvoiceGenerationRequest;
import com.teleconnect.billing_service.dto.request.LateFeeRequest;
import com.teleconnect.billing_service.dto.request.LateFeeWaiverRequest;
import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.InvoiceResponse;
import com.teleconnect.billing_service.enums.InvoiceStatus;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceService {

    InvoiceResponse generateInvoice(InvoiceGenerationRequest request);

    InvoiceResponse getInvoiceById(Long invoiceId);

    List<InvoiceResponse> getInvoicesByAccount(Long accountId);

    List<InvoiceResponse> getInvoicesByAccount(Long accountId, InvoiceStatus status,
                                               LocalDate fromDate, LocalDate toDate);

    List<InvoiceResponse> getInvoicesByStatus(InvoiceStatus status);

    InvoiceResponse processPayment(PaymentRequest request);

    InvoiceResponse payInvoice(Long invoiceId, PaymentRequest request);

    InvoiceResponse applyLateFee(Long invoiceId, LateFeeRequest request);

    InvoiceResponse waiveLateFee(Long invoiceId, LateFeeWaiverRequest request);

    void markOverdueInvoices();

    InvoiceResponse sendInvoice(Long invoiceId);

    byte[] downloadInvoicePdf(Long invoiceId);

    byte[] downloadAccountStatementPdf(Long accountId);
}
