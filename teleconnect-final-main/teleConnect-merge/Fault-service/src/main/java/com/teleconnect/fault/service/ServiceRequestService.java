package com.teleconnect.fault.service;

import lombok.extern.slf4j.Slf4j;

import com.teleconnect.fault.dto.request.ServiceRequestRequest;
import com.teleconnect.fault.dto.response.*;
import com.teleconnect.fault.entity.ServiceRequest;
import com.teleconnect.fault.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepo;

    ServiceRequestService(ServiceRequestRepository requestRepo) {
        this.requestRepo = requestRepo;
    }

    // Convert Entity to Response DTO
    private ServiceRequestResponse toDTO(ServiceRequest r) {
        ServiceRequestResponse dto = new ServiceRequestResponse();
        dto.setRequestId(r.getRequestId());
        dto.setAccountId(r.getAccountId());
        dto.setLineId(r.getLineId());
        dto.setRequestType(r.getRequestType().name());
        dto.setRequestedBy(r.getRequestedBy());
        dto.setRaisedDate(r.getRaisedDate());
        dto.setStatus(r.getStatus().name());
        return dto;
    }

    // POST — create new service request
    public MessageResponse createRequest(ServiceRequestRequest req) {
        log.info("Create service request received accountId={} lineId={} type={}", req.getAccountId(), req.getLineId(), req.getRequestType());
        try {
            ServiceRequest.RequestType.valueOf(req.getRequestType());
        } catch (IllegalArgumentException e) {
            log.error("Invalid requestType provided: {}", req.getRequestType());
            throw new RuntimeException(
                "requestType must be PlanChange, SIMReplacement, PortingRequest, or AccountUpdate");
        }
        ServiceRequest sr = new ServiceRequest();
        sr.setAccountId(req.getAccountId());
        sr.setLineId(req.getLineId());
        sr.setRequestType(ServiceRequest.RequestType.valueOf(req.getRequestType()));
        sr.setRequestedBy(req.getRequestedBy());
        sr.setRaisedDate(req.getRaisedDate());
        sr.setStatus(ServiceRequest.RequestStatus.O);
        requestRepo.save(sr);
        log.info("Service request created accountId={} lineId={}", req.getAccountId(), req.getLineId());
        return new MessageResponse("Service request created successfully");
    }

    // GET all
    public List<ServiceRequestResponse> getAllRequests() {
        log.debug("Fetching all service requests");
        List<ServiceRequestResponse> res = requestRepo.findAll().stream()
            .map(this::toDTO).collect(Collectors.toList());
        log.debug("Retrieved {} service requests", res.size());
        return res;
    }

    // GET by ID
    public ServiceRequestResponse getRequestById(Integer requestId) {
        log.debug("Fetching service request id={}", requestId);
        ServiceRequest sr = requestRepo.findById(requestId)
            .orElseThrow(() -> {
                log.warn("Service request not found id={}", requestId);
                return new RuntimeException(
                "Service request with requestId " + requestId + " not found");
            });
        return toDTO(sr);
    }

    // PUT — update status
    public MessageResponse updateRequest(Integer requestId, ServiceRequestRequest req) {
        log.info("Update service request id={}", requestId);
        ServiceRequest sr = requestRepo.findById(requestId)
                .orElseThrow(() -> {
                    log.warn("Service request not found id={}", requestId);
                    return new RuntimeException(
                        "Service request with requestId " + requestId + " not found");
                });
        if (req.getStatus() != null) {
            try {
                sr.setStatus(ServiceRequest.RequestStatus.valueOf(req.getStatus()));
            } catch (IllegalArgumentException e) {
                log.error("Invalid service request status provided: {}", req.getStatus());
                throw new RuntimeException("status must be O, P, C, or X");
            }
        }
        requestRepo.save(sr);
        log.info("Service request updated id={}", requestId);
        return new MessageResponse("Service request updated successfully");
    }

    // PUT — cancel (only when Open)
    public MessageResponse cancelRequest(Integer requestId) {
        log.info("Cancel service request id={}", requestId);
        ServiceRequest sr = requestRepo.findById(requestId)
                .orElseThrow(() -> {
                    log.warn("Service request not found id={}", requestId);
                    return new RuntimeException(
                        "Service request with requestId " + requestId + " not found");
                });
        if (sr.getStatus() != ServiceRequest.RequestStatus.O) {
            log.warn("Cannot cancel non-open request id={}", requestId);
            throw new RuntimeException("Only Open requests can be cancelled");
        }
        sr.setStatus(ServiceRequest.RequestStatus.X);
        requestRepo.save(sr);
        log.info("Service request cancelled id={}", requestId);
        return new MessageResponse("Service request cancelled successfully");
    }
}
