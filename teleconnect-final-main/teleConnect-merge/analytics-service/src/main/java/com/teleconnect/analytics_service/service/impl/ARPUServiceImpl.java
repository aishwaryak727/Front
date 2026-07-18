package com.teleconnect.analytics_service.service.impl;

import com.teleconnect.analytics_service.client.BillingStatsClient;
import com.teleconnect.analytics_service.client.SubscriberStatsClient;
import com.teleconnect.analytics_service.dto.external.InvoiceDto;
import com.teleconnect.analytics_service.dto.external.SubscriberAccountDto;
import com.teleconnect.analytics_service.dto.response.ARPUReportResponse;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.enums.AccountType;
import com.teleconnect.analytics_service.enums.InvoiceStatus;
import com.teleconnect.analytics_service.exception.AnalyticsException;
import com.teleconnect.analytics_service.service.ARPUService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes ARPU by combining a billable-invoice list from the Billing
 * module with an active-account list from the Subscriber module — both
 * fetched live over HTTP via their respective clients. No local
 * database access to either module's tables.
 */
@Slf4j
@Service
public class ARPUServiceImpl implements ARPUService {

    private final BillingStatsClient billingStatsClient;
    private final SubscriberStatsClient subscriberStatsClient;

    public ARPUServiceImpl(BillingStatsClient billingStatsClient,
                            SubscriberStatsClient subscriberStatsClient) {
        this.billingStatsClient = billingStatsClient;
        this.subscriberStatsClient = subscriberStatsClient;
    }

    @Override
    public ARPUReportResponse computeARPU(Long cycleId, String scope, String scopeValue) {
        log.info("Computing ARPU for cycleId={} scope={} scopeValue={}", cycleId, scope, scopeValue);
        if (cycleId == null) {
            log.error("ARPU computation failed: cycleId is null");
            throw new AnalyticsException("cycleId is required for ARPU computation");
        }

        List<InvoiceStatus> billableStatuses = List.of(
                InvoiceStatus.PAID, InvoiceStatus.OVERDUE, InvoiceStatus.SENT);

        List<InvoiceDto> invoices = billingStatsClient.getInvoicesByCycle(cycleId, billableStatuses);
        List<SubscriberAccountDto> activeAccounts = subscriberStatsClient.getActiveAccounts();

        BigDecimal totalRevenue = invoices.stream()
                .map(InvoiceDto::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeSubscribers = activeAccounts.size();

        BigDecimal arpuOverall = activeSubscribers > 0
                ? totalRevenue.divide(BigDecimal.valueOf(activeSubscribers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal arpuPrepaid = computeARPUForType(invoices, activeAccounts, AccountType.PREPAID);
        BigDecimal arpuPostpaid = computeARPUForType(invoices, activeAccounts, AccountType.POSTPAID);
        BigDecimal arpuEnterprise = computeARPUForType(invoices, activeAccounts, AccountType.ENTERPRISE);

        Map<String, BigDecimal> arpuByRegion = computeARPUByRegion(invoices, activeAccounts);

        ARPUReportResponse response = new ARPUReportResponse();
        response.setCycleId(cycleId);
        response.setScope(scope);
        response.setScopeValue(scopeValue);
        response.setTotalRevenue(totalRevenue);
        response.setActiveSubscribers(activeSubscribers);
        response.setArpuOverall(arpuOverall);
        response.setArpuPrepaid(arpuPrepaid);
        response.setArpuPostpaid(arpuPostpaid);
        response.setArpuEnterprise(arpuEnterprise);
        response.setArpuByRegion(arpuByRegion);

        log.debug("ARPU computed totalRevenue={} activeSubscribers={} overall={} prepaid={} postpaid={} enterprise={}",
                totalRevenue, activeSubscribers, arpuOverall, arpuPrepaid, arpuPostpaid, arpuEnterprise);
        return response;
    }

    private BigDecimal computeARPUForType(List<InvoiceDto> invoices, List<SubscriberAccountDto> accounts, AccountType type) {
        List<Long> accountIds = accounts.stream()
                .filter(a -> a.getAccountType() == type && a.getStatus() == AccountStatus.ACTIVE)
                .map(SubscriberAccountDto::getAccountId)
                .toList();

        BigDecimal revenue = invoices.stream()
                .filter(i -> accountIds.contains(i.getAccountId()))
                .map(InvoiceDto::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.debug("Computed ARPU for type={} revenue={} accountCount={}", type, revenue, accountIds.size());

        long count = accountIds.size();
        return count > 0
                ? revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private Map<String, BigDecimal> computeARPUByRegion(List<InvoiceDto> invoices, List<SubscriberAccountDto> accounts) {
        Map<Long, List<Long>> regionAccounts = new HashMap<>();
        accounts.stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE && a.getRegionId() != null)
                .forEach(a -> regionAccounts
                        .computeIfAbsent(a.getRegionId(), k -> new java.util.ArrayList<>())
                        .add(a.getAccountId()));

        Map<String, BigDecimal> result = new HashMap<>();
        for (Map.Entry<Long, List<Long>> entry : regionAccounts.entrySet()) {
            BigDecimal revenue = invoices.stream()
                    .filter(i -> entry.getValue().contains(i.getAccountId()))
                    .map(InvoiceDto::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long count = entry.getValue().size();
            BigDecimal arpu = count > 0
                    ? revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            result.put("REGION_" + entry.getKey(), arpu);
            log.debug("Computed ARPU for region={} revenue={} accountCount={} arpu={}", entry.getKey(), revenue, count, arpu);
        }
        return result;
    }
}
