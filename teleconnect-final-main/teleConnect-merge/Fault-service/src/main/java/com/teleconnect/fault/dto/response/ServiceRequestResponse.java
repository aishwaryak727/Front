package com.teleconnect.fault.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ServiceRequestResponse {
    private Integer requestId;
    private Integer accountId;
    private Integer lineId;
    private String requestType;
    private Integer requestedBy;
    private LocalDate raisedDate;
    private String status;
}
