package com.teleconnect.plan.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TelecomPlanResponse {
    private Integer planId;
    private String name;
    private String type;
    private BigDecimal dataGb;
    private Integer voiceMinutes;
    private Integer smsCount;
    private Integer validityDays;
    private BigDecimal planPrice;
    private String status;
}