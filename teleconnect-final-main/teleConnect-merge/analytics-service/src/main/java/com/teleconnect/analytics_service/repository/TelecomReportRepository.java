package com.teleconnect.analytics_service.repository;

import com.teleconnect.analytics_service.entity.TelecomReport;
import com.teleconnect.analytics_service.enums.ReportScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TelecomReportRepository extends JpaRepository<TelecomReport, Long> {

    List<TelecomReport> findByScopeAndScopeValue(ReportScope scope, String scopeValue);

    @Query("SELECT r FROM TelecomReport r WHERE " +
           "(:scope IS NULL OR r.scope = :scope) AND " +
           "(:from IS NULL OR r.periodStart >= :from) AND " +
           "(:to IS NULL OR r.periodEnd <= :to)")
    Page<TelecomReport> findByFilters(
            @Param("scope") ReportScope scope,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    List<TelecomReport> findByScopeAndPeriodStartBetween(ReportScope scope, LocalDate from, LocalDate to);
}
