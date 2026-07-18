package com.teleconnect.usage.repository;

import com.teleconnect.usage.entity.UsageSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsageSummaryRepository extends JpaRepository<UsageSummary, Long> {
    // Used by: GET /fetchSummary/{lineId}/{billingCycleId} & PUT /updateSummary
    Optional<UsageSummary> findByLineIdAndBillingCycleId(Long lineId, Long billingCycleId);

    // Used by: GET /analytics/{lineId} — trend across all cycles
    List<UsageSummary> findByLineId(Long lineId);
}