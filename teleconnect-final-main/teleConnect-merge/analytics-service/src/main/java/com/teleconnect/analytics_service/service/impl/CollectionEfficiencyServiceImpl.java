package com.teleconnect.analytics_service.service.impl;

import com.teleconnect.analytics_service.client.BillingStatsClient;
import com.teleconnect.analytics_service.dto.external.InvoiceDto;
import com.teleconnect.analytics_service.dto.response.CollectionEfficiencyResponse;
import com.teleconnect.analytics_service.enums.InvoiceStatus;
import com.teleconnect.analytics_service.service.CollectionEfficiencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Computes collection efficiency entirely from invoices fetched live
 * from the Billing module's analytics API.
 */
@Slf4j
@Service
public class CollectionEfficiencyServiceImpl implements CollectionEfficiencyService {

    private final BillingStatsClient billingStatsClient;

    public CollectionEfficiencyServiceImpl(BillingStatsClient billingStatsClient) {
        this.billingStatsClient = billingStatsClient;
    }

    @Override
    public CollectionEfficiencyResponse computeCollectionEfficiency(Long cycleId) {
        log.info("Computing collection efficiency for cycleId={}", cycleId);
        List<InvoiceStatus> billableStatuses = List.of(
                InvoiceStatus.PAID, InvoiceStatus.OVERDUE, InvoiceStatus.SENT);
        List<InvoiceDto> invoices = billingStatsClient.getInvoicesByCycle(cycleId, billableStatuses);

        BigDecimal totalInvoiced = invoices.stream()
                .map(InvoiceDto::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCollected = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .map(InvoiceDto::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double efficiency = totalInvoiced.compareTo(BigDecimal.ZERO) > 0
                ? totalCollected.divide(totalInvoiced, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        LocalDate today = LocalDate.now();

        long count0to30 = 0, count31to60 = 0, count60plus = 0;
        BigDecimal amt0to30 = BigDecimal.ZERO, amt31to60 = BigDecimal.ZERO, amt60plus = BigDecimal.ZERO;

        List<InvoiceDto> overdueInvoices = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.OVERDUE)
                .toList();

        for (InvoiceDto inv : overdueInvoices) {
            long daysOverdue = ChronoUnit.DAYS.between(inv.getDueDate(), today);
            if (daysOverdue <= 30) {
                count0to30++;
                amt0to30 = amt0to30.add(inv.getTotalAmount());
            } else if (daysOverdue <= 60) {
                count31to60++;
                amt31to60 = amt31to60.add(inv.getTotalAmount());
            } else {
                count60plus++;
                amt60plus = amt60plus.add(inv.getTotalAmount());
            }
        }

        CollectionEfficiencyResponse response = new CollectionEfficiencyResponse();
        response.setCycleId(cycleId);
        response.setTotalInvoiced(totalInvoiced);
        response.setTotalCollected(totalCollected);
        response.setCollectionEfficiencyPct(Math.round(efficiency * 100.0) / 100.0);
        response.setOverdueCount0to30(count0to30);
        response.setOverdueCount31to60(count31to60);
        response.setOverdueCount60plus(count60plus);
        response.setOverdueAmount0to30(amt0to30);
        response.setOverdueAmount31to60(amt31to60);
        response.setOverdueAmount60plus(amt60plus);

        log.debug("Collection efficiency result totalInvoiced={} totalCollected={} efficiencyPct={} counts=[{},{},{}]",
                totalInvoiced, totalCollected, response.getCollectionEfficiencyPct(), count0to30, count31to60, count60plus);
        return response;
    }
}
