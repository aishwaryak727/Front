package com.teleconnect.billing_service.service;

import com.teleconnect.billing_service.entity.BillingDispute;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.DisputeStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.repository.BillingDisputeRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private BillingDisputeRepository disputeRepository;

    @InjectMocks
    private ReportServiceImpl service;

    @Test
    void getOverdueReport_returnsReport() {
        Invoice overdueInvoice = new Invoice();
        overdueInvoice.setInvoiceId(1L);
        overdueInvoice.setStatus(InvoiceStatus.OVERDUE);
        overdueInvoice.setTotalAmount(BigDecimal.valueOf(100));
        overdueInvoice.setDueDate(LocalDate.now().minusDays(10));

        when(invoiceRepository.findByStatus(InvoiceStatus.OVERDUE)).thenReturn(List.of(overdueInvoice));

        var result = service.getOverdueReport("South", "0-30");

        assertThat(result.getRegion()).isEqualTo("South");
        assertThat(result.getTotalOverdueCount()).isEqualTo(1);
    }

    @Test
    void getCollectionReport_returnsSummary() {
        Invoice paidInvoice = new Invoice();
        paidInvoice.setInvoiceId(2L);
        paidInvoice.setStatus(InvoiceStatus.PAID);
        paidInvoice.setTotalAmount(BigDecimal.valueOf(200));
        paidInvoice.setPaidAmount(BigDecimal.valueOf(200));
        paidInvoice.setDueDate(LocalDate.of(2026, 5, 10));

        when(invoiceRepository.findByDueDateBetween(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .thenReturn(List.of(paidInvoice));

        var result = service.getCollectionReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "South");

        assertThat(result.getRegion()).isEqualTo("South");
        assertThat(result.getTotalBilled()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    void getDisputeSummary_returnsSummary() {
        BillingDispute dispute = new BillingDispute();
        dispute.setDisputeId(1L);
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setRaisedDate(LocalDate.of(2026, 5, 1));
        dispute.setAcknowledgedDate(LocalDateTime.of(2026, 5, 1, 12, 0));
        dispute.setResolvedDate(LocalDateTime.of(2026, 5, 3, 12, 0));

        when(disputeRepository.findByRaisedDateBetween(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .thenReturn(List.of(dispute));

        var result = service.getDisputeSummary(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        assertThat(result.getTotalDisputes()).isEqualTo(1);
        assertThat(result.getResolved()).isEqualTo(1);
    }
}
