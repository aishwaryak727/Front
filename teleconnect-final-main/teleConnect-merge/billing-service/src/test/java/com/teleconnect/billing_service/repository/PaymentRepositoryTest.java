package com.teleconnect.billing_service.repository;

import com.teleconnect.billing_service.entity.Payment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRepositoryTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    void findByInvoiceId_returnsPayments() {
        Payment payment = new Payment();
        payment.setPaymentId(1L);
        payment.setInvoiceId(10L);

        when(paymentRepository.findByInvoiceId(10L)).thenReturn(List.of(payment));

        List<Payment> result = paymentRepository.findByInvoiceId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvoiceId()).isEqualTo(10L);
    }
}
