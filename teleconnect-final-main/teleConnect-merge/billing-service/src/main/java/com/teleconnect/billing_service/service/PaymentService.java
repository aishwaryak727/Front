package com.teleconnect.billing_service.service;

import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse makePayment(PaymentRequest request);

    PaymentResponse getPaymentById(Long paymentId);

    List<PaymentResponse> getPaymentsByInvoice(Long invoiceId);
}
