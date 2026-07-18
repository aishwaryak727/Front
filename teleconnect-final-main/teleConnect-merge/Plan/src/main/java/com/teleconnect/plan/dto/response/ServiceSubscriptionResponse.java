package com.teleconnect.plan.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ServiceSubscriptionResponse {
    private Integer subscriptionId;
    private Integer lineId;
    private Integer planId;
    private Integer addOnId;
    private LocalDate activationDate;
    private LocalDate expiryDate;
    private String renewalType;
    private String status;
}