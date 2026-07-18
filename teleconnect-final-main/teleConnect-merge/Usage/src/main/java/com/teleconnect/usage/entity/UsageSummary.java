package com.teleconnect.usage.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_summaries", uniqueConstraints = @UniqueConstraint(columnNames = { "line_id", "billing_cycle_id" }))
@Data
public class UsageSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long summaryId;
    @Column(name = "line_id", nullable = false)
    private Long lineId;
    @Column(name = "billing_cycle_id", nullable = false)
    private Long billingCycleId;
    @Column(precision = 10, scale = 2)
    private BigDecimal dataUsedMb = BigDecimal.ZERO;
    @Column(precision = 10, scale = 2)
    private BigDecimal voiceUsedMin = BigDecimal.ZERO;
    @Column
    private Integer smsUsed = 0;
    @Column(precision = 10, scale = 2)
    private BigDecimal dataRemainingMb = BigDecimal.ZERO;
    @Column(precision = 10, scale = 2)
    private BigDecimal voiceRemainingMin = BigDecimal.ZERO;
    @Column
    private Integer smsRemaining = 0;
    @Column(nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();
}