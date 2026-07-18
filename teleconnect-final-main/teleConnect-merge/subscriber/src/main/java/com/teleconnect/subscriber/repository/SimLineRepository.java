package com.teleconnect.subscriber.repository;

import com.teleconnect.subscriber.entity.SimLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SimLineRepository
        extends JpaRepository<SimLine, Integer> {

    List<SimLine> findByAccountId(Integer accountId);

    List<SimLine> findByAccountIdAndStatus(
            Integer accountId, SimLine.SimStatus status);

    Optional<SimLine> findByMsisdn(String msisdn);

    boolean existsByMsisdn(String msisdn);

    boolean existsByIccid(String iccid);

    List<SimLine> findByStatus(SimLine.SimStatus status);
}