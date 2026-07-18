package com.teleconnect.usage.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UsageRecordRequest {
    @NotNull(message = "lineId is required")
    private Long lineId;
    @NotNull(message = "billingCycleId is required")
    private Long billingCycleId;
    @NotBlank(message = "usageType is required — DATA / VOICE / SMS")
    private String usageType; // Validated & converted to enum in service
    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.01", message = "quantity must be > 0")
    private BigDecimal quantity;
    @NotNull(message = "usageDate is required")
    private LocalDateTime usageDate;
    @NotNull(message = "dataLimitMb is required")
    private Double dataLimitMb; // e.g. 5120.0 (5 GB plan)

    @NotNull(message = "voiceLimitMin is required")
    private Double voiceLimitMin; // e.g. 300.0

    @NotNull(message = "smsLimit is required")
    private Integer smsLimit; // e.g. 100
    // unit is NOT accepted from client — server derives it from usageType
}