package com.teleconnect.billing_service.repository;

import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceRepositoryTest {

    @Mock
    private InvoiceRepository repository;

    @Test
    void findByStatus_returnsInvoices() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(1L);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setTotalAmount(BigDecimal.valueOf(100.0));

        when(repository.findByStatus(InvoiceStatus.PAID)).thenReturn(List.of(invoice));

        List<Invoice> result = repository.findByStatus(InvoiceStatus.PAID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(InvoiceStatus.PAID);
    }
}
