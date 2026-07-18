package com.teleconnect.analytics_service.client;

import com.teleconnect.analytics_service.dto.external.SimLineDto;
import com.teleconnect.analytics_service.dto.external.SubscriberAccountDto;
import com.teleconnect.analytics_service.enums.AccountStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * HTTP client for the Subscriber Account & SIM Management module's
 * analytics-facing API. Analytics-service never touches that module's
 * database directly — every method here is backed by a GET call to
 * the subscriber module's own service.
 */
public interface SubscriberStatsClient {

    /** Count of accounts currently in the given status. */
    long countByStatus(AccountStatus status);

    /** Count of accounts ACTIVE and registered on/before {@code date}, used as the churn period baseline. */
    long countActiveRegisteredBefore(LocalDate date);

    /** Count of ACTIVE accounts in a given region. */
    long countActiveByRegion(Long regionId);

    /** All active accounts (id, type, region) — used to map invoices to account type/region for ARPU breakdowns. */
    List<SubscriberAccountDto> getActiveAccounts();

    /** Accounts in the given status that registered within [start, end]. */
    List<SubscriberAccountDto> getByStatusAndRegistrationBetween(AccountStatus status, LocalDate start, LocalDate end);

    /** Accounts that transitioned to TERMINATED within [start, end]. */
    List<SubscriberAccountDto> getTerminatedInPeriod(LocalDate start, LocalDate end);

    /** SIM lines that ported out within [start, end]. */
    List<SimLineDto> getPortedOutInPeriod(LocalDate start, LocalDate end);

    /** SIM lines activated within [start, end]. */
    List<SimLineDto> getActivatedInPeriod(LocalDate start, LocalDate end);
}
