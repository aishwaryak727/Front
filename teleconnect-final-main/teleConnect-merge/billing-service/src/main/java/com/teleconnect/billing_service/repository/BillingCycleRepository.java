package com.teleconnect.billing_service.repository;

import com.teleconnect.billing_service.entity.BillingCycle;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {

    List<BillingCycle> findByAccountId(Long accountId);

    List<BillingCycle> findByStatus(BillingCycleStatus status);

    Optional<BillingCycle> findByAccountIdAndStatus(Long accountId, BillingCycleStatus status);

    List<BillingCycle> findByStatusAndCycleEndLessThanEqual(BillingCycleStatus status, LocalDate cycleEnd);

    Page<BillingCycle> findByAccountId(Long accountId, Pageable pageable);

    Page<BillingCycle> findByAccountIdAndStatus(Long accountId, BillingCycleStatus status, Pageable pageable);
}
