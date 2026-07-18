
package com.teleconnect.plan.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TelecomPlanRequest {
    private String name;
    private String type;
    private BigDecimal dataGb;
    private Integer voiceMinutes;
    private Integer smsCount;
    private Integer validityDays;
    private BigDecimal planPrice;
    private String status;
}
