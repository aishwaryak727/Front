package com.teleconnect.plan.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddOnResponse {
    private Integer addOnId;
    private String name;
    private String type;
    private BigDecimal quota;
    private Integer validityDays;
    private BigDecimal price;
    private String status;
}