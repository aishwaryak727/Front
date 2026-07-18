package com.teleconnect.fault.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class FaultTicketRequest {

    @NotNull(message = "accountId is required")
    private Integer accountId;

    @NotNull(message = "lineId is required")
    private Integer lineId;

    @NotNull(message = "faultType is required")
    private String faultType;

    @NotBlank(message = "description is required")
    private String description;

    // priority is optional — defaults to M (Medium) if not sent
    private String priority;

    @NotNull(message = "raisedDate is required")
    private LocalDate raisedDate;

    // resolvedDate — only sent when resolving a ticket
    private LocalDate resolvedDate;

    // assignedToId — optional on create
    private Integer assignedToId;

    // status — only sent on update, not on create
    private String status;
}
