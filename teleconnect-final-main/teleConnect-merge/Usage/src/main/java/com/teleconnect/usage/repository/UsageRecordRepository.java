package com.teleconnect.usage.repository;

import com.teleconnect.usage.entity.UsageRecord;
import com.teleconnect.usage.entity.enums.UsageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
        // Used by: GET /fetchRecords/{lineId}
        List<UsageRecord> findByLineId(Long lineId);

        // Used by: GET /fetchRecords/{lineId}/{billingCycleId}
        List<UsageRecord> findByLineIdAndBillingCycleId(Long lineId, Long billingCycleId);

        // Used by: analytics — sum of a specific type for a line across all cycles
        @Query("SELECT SUM(r.quantity) FROM UsageRecord r " +
                        "WHERE r.lineId = :lineId AND r.usageType = :type")
        Optional<Double> sumQuantityByLineIdAndUsageType(
                        @Param("lineId") Long lineId,
                        @Param("type") UsageType type);
}