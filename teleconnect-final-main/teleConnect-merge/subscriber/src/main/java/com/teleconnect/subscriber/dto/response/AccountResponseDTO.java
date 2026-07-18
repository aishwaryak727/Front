package com.teleconnect.subscriber.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AccountResponseDTO {
    private Integer accountId;
    private Long    subscriberId;
    private String  accountType;
    private LocalDate registrationDate;
    private String  kycStatus;
    private String  status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}