package com.teleconnect.billing_service.service;

import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.entity.Payment;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.enums.PaymentMethod;
import com.teleconnect.billing_service.enums.PaymentStatus;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.repository.PaymentRepository;
import com.teleconnect.billing_service.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private PaymentServiceImpl service;

    @Test
    void makePayment_savesPaymentAndMarksInvoicePaid() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(1L);
        invoice.setStatus(InvoiceStatus.GENERATED);
        invoice.setTotalAmount(BigDecimal.valueOf(100.00));

        Payment payment = Payment.builder()
                .paymentId(1L)
                .invoiceId(1L)
                .amountPaid(BigDecimal.valueOf(100.00))
                .paymentMethod(PaymentMethod.CARD)
                .transactionRef("TRX-1")
                .status(PaymentStatus.SUCCESS)
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByTransactionRef("TRX-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        PaymentRequest request = new PaymentRequest();
        request.setInvoiceId(1L);
        request.setAmountPaid(BigDecimal.valueOf(100.00));
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setTransactionRef("TRX-1");

        var result = service.makePayment(request);

        assertThat(result.getPaymentId()).isEqualTo(1L);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void getPaymentsByInvoice_returnsList() {
        when(invoiceRepository.existsById(1L)).thenReturn(true);
        when(paymentRepository.findByInvoiceId(1L)).thenReturn(List.of(Payment.builder().paymentId(2L).build()));

        var result = service.getPaymentsByInvoice(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPaymentId()).isEqualTo(2L);
    }
}
