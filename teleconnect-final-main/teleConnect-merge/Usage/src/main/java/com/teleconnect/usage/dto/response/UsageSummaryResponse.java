package com.teleconnect.usage.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UsageSummaryResponse {
    private Long summaryId;
    private Long lineId;
    private Long billingCycleId;
    private BigDecimal dataUsedMb;
    private BigDecimal voiceUsedMin;
    private Integer smsUsed;
    private BigDecimal dataRemainingMb;
    private BigDecimal voiceRemainingMin;
    private Integer smsRemaining;
    private LocalDateTime lastUpdated;
}