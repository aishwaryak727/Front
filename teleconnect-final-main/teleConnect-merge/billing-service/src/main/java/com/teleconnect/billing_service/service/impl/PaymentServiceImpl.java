package com.teleconnect.billing_service.service.impl;

import lombok.extern.slf4j.Slf4j;

import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.PaymentResponse;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.entity.Payment;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.enums.PaymentStatus;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.repository.PaymentRepository;
import com.teleconnect.billing_service.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional
    public PaymentResponse makePayment(PaymentRequest request) {
        log.info("Processing payment for invoiceId={} amount={} method={}", request.getInvoiceId(), request.getAmountPaid(), request.getPaymentMethod());
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with ID: " + request.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            log.warn("Attempted payment on already paid invoice invoiceId={}", request.getInvoiceId());
            throw new BillingException("Invoice " + request.getInvoiceId() + " is already paid");
        }
        if (invoice.getStatus() == InvoiceStatus.DISPUTED) {
            log.warn("Attempted payment on disputed invoice invoiceId={}", request.getInvoiceId());
            throw new BillingException(
                    "Cannot process payment for a disputed invoice. Resolve the dispute first.");
        }
        if (request.getAmountPaid().compareTo(invoice.getTotalAmount()) < 0) {
            log.warn("Payment amount less than invoice total invoiceId={} paid={} total={}",
                    request.getInvoiceId(), request.getAmountPaid(), invoice.getTotalAmount());
            throw new BillingException(
                    "Payment amount " + request.getAmountPaid()
                    + " is less than the invoice total " + invoice.getTotalAmount());
        }

        if (request.getTransactionRef() != null && !request.getTransactionRef().isBlank()) {
            paymentRepository.findByTransactionRef(request.getTransactionRef())
                    .ifPresent(existing -> {
                        log.warn("Duplicate transaction reference detected transactionRef={}", request.getTransactionRef());
                        throw new BillingException(
                                "Duplicate transaction reference: " + request.getTransactionRef());
                    });
        }

        Payment payment = Payment.builder()
                .invoiceId(request.getInvoiceId())
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .transactionRef(request.getTransactionRef())
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment recorded paymentId={} invoiceId={}", saved.getPaymentId(), saved.getInvoiceId());

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAmount(request.getAmountPaid());
        invoiceRepository.save(invoice);
        log.debug("Invoice status updated to PAID invoiceId={}", invoice.getInvoiceId());

        return toResponse(saved);
    }

    @Override
    public PaymentResponse getPaymentById(Long paymentId) {
        log.debug("Fetching payment by id={}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with ID: " + paymentId));
        log.debug("Payment retrieved paymentId={}", paymentId);
        return toResponse(payment);
    }

    @Override
    public List<PaymentResponse> getPaymentsByInvoice(Long invoiceId) {
        log.debug("Fetching payments for invoiceId={}", invoiceId);
        if (!invoiceRepository.existsById(invoiceId)) {
            log.warn("Invoice not found when fetching payments invoiceId={}", invoiceId);
            throw new ResourceNotFoundException("Invoice not found with ID: " + invoiceId);
        }
        List<PaymentResponse> payments = paymentRepository.findByInvoiceId(invoiceId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        log.debug("Found {} payments for invoiceId={}", payments.size(), invoiceId);
        return payments;
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .invoiceId(payment.getInvoiceId())
                .amountPaid(payment.getAmountPaid())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .transactionRef(payment.getTransactionRef())
                .status(payment.getStatus())
                .build();
    }
}
