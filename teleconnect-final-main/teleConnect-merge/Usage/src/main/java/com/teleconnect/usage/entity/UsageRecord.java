package com.teleconnect.usage.entity;

import com.teleconnect.usage.entity.enums.UsageType;
import com.teleconnect.usage.entity.enums.UsageUnit;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_records")
@Data
public class UsageRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long usageId;
    @Column(nullable = false)
    private Long lineId; // FK → SIMLine.lineId
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UsageType usageType; // DATA / VOICE / SMS
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity; // Amount consumed
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UsageUnit unit; // MB / MINUTES / COUNT (auto-set by service)
    @Column(nullable = false)
    private LocalDateTime usageDate;
    @Column(nullable = false)
    private Long billingCycleId; // FK → BillingCycle.cycleId
}