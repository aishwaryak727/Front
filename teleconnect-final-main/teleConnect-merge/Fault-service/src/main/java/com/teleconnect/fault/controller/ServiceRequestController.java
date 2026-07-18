package com.teleconnect.fault.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.fault.dto.request.ServiceRequestRequest;
import com.teleconnect.fault.dto.response.MessageResponse;
import com.teleconnect.fault.dto.response.ServiceRequestResponse;
import com.teleconnect.fault.service.ServiceRequestService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/fault")
public class ServiceRequestController {

    private final ServiceRequestService requestService;
    private final AuditClient auditClient;

    public ServiceRequestController(ServiceRequestService requestService, AuditClient auditClient) {
        this.requestService = requestService;
        this.auditClient = auditClient;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized ServiceRequestController");
    }

    // POST /teleConnect/fault/createRequests
    @PostMapping("/createRequests")
    @PreAuthorize("hasAuthority('SERVICE_REQUEST')")
    public ResponseEntity<MessageResponse> createRequest(
            @Valid @RequestBody ServiceRequestRequest req,
            HttpServletRequest httpReq) {
        log.info("Creating service request for account={}", req.getAccountId());
        MessageResponse result = requestService.createRequest(req);
        auditClient.record(AuditAction.CREATE_SERVICE_REQUEST, AuditModule.FAULT, httpReq);
        log.info("Service request created successfully");
        return ResponseEntity.status(201).body(result);
    }

    // GET /teleConnect/fault/getAllRequests
    @GetMapping("/getAllRequests")
    @PreAuthorize("hasAuthority('SERVICE_REQUEST')")
    public ResponseEntity<List<ServiceRequestResponse>> getAllRequests() {
        log.info("Fetching all service requests");
        List<ServiceRequestResponse> requests = requestService.getAllRequests();
        log.info("Retrieved {} service requests", requests.size());
        return ResponseEntity.ok(requests);
    }

    // GET /teleConnect/fault/getRequests/{requestId}
    @GetMapping("/getRequests/{requestId}")
    @PreAuthorize("hasAuthority('SERVICE_REQUEST')")
    public ResponseEntity<ServiceRequestResponse> getRequestById(
            @PathVariable Integer requestId) {
        log.info("Fetching service request requestId={}", requestId);
        ServiceRequestResponse request = requestService.getRequestById(requestId);
        log.debug("Service request retrieved requestId={}", requestId);
        return ResponseEntity.ok(request);
    }

    // PUT /teleConnect/fault/updateRequests/{requestId}
    @PutMapping("/updateRequests/{requestId}")
    @PreAuthorize("hasAuthority('SERVICE_REQUEST')")
    public ResponseEntity<MessageResponse> updateRequest(
            @PathVariable Integer requestId,
            @RequestBody ServiceRequestRequest req,
            HttpServletRequest httpReq) {
        log.info("Updating service request requestId={}", requestId);
        MessageResponse result = requestService.updateRequest(requestId, req);
        auditClient.record(AuditAction.UPDATE_SERVICE_REQUEST, AuditModule.FAULT, httpReq);
        log.info("Service request updated requestId={}", requestId);
        return ResponseEntity.ok(result);
    }

    // PUT /teleConnect/fault/cancelRequests/{requestId}
    @PutMapping("/cancelRequests/{requestId}")
    @PreAuthorize("hasAuthority('SERVICE_REQUEST')")
    public ResponseEntity<MessageResponse> cancelRequest(
            @PathVariable Integer requestId,
            HttpServletRequest httpReq) {
        log.info("Canceling service request requestId={}", requestId);
        MessageResponse result = requestService.cancelRequest(requestId);
        auditClient.record(AuditAction.CANCEL_SERVICE_REQUEST, AuditModule.FAULT, httpReq);
        log.info("Service request canceled requestId={}", requestId);
        return ResponseEntity.ok(result);
    }
}
