package com.teleconnect.fault.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ServiceRequestRequest {

    @NotNull(message = "accountId is required")
    private Integer accountId;

    @NotNull(message = "lineId is required")
    private Integer lineId;

    @NotNull(message = "requestType is required")
    private String requestType;

    @NotNull(message = "requestedBy is required")
    private Integer requestedBy;

    @NotNull(message = "raisedDate is required")
    private LocalDate raisedDate;

    // status not included — defaults to O (Open) on creation; only sent on update
    private String status;
}
