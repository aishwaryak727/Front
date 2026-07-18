
package com.teleconnect.plan.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ServiceSubscriptionRequest {
    private Integer lineId;
    private Integer planId;
    private Integer addOnId;
    private LocalDate activationDate;
    private LocalDate expiryDate;
    private String renewalType;
    private String status;
}
