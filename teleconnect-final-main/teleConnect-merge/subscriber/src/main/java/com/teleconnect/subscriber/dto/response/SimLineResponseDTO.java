package com.teleconnect.subscriber.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SimLineResponseDTO {
    private Integer  lineId;
    private Integer  accountId;
    private String   msisdn;
    private String   iccid;
    private LocalDate activationDate;
    private String   serviceType;
    private String   status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}