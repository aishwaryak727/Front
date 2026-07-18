package com.teleconnect.usage.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UsageRecordResponse {
    private Long usageId;
    private Long lineId;
    private String usageType;
    private BigDecimal quantity;
    private String unit;
    private LocalDateTime usageDate;
    private Long billingCycleId;
}