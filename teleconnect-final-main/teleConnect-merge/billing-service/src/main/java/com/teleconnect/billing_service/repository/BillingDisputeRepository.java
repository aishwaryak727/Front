package com.teleconnect.billing_service.repository;

import com.teleconnect.billing_service.entity.BillingDispute;
import com.teleconnect.billing_service.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BillingDisputeRepository extends JpaRepository<BillingDispute, Long> {

    List<BillingDispute> findByInvoiceId(Long invoiceId);

    List<BillingDispute> findBySubscriberId(Long subscriberId);

    List<BillingDispute> findByStatus(DisputeStatus status);

    List<BillingDispute> findByRaisedDateBetween(LocalDate fromDate, LocalDate toDate);
}
