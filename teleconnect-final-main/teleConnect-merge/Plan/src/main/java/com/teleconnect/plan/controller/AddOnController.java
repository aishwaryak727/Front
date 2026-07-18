package com.teleconnect.plan.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.plan.dto.request.AddOnRequest;
import com.teleconnect.plan.dto.response.AddOnResponse;
import com.teleconnect.plan.dto.response.MessageResponse;
import com.teleconnect.plan.service.AddOnService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/plan")
public class AddOnController {

    private final AddOnService service;
    private final AuditClient auditClient;

    public AddOnController(AddOnService service, AuditClient auditClient) {
        this.service = service;
        this.auditClient = auditClient;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized AddOnController");
    }

    @PostMapping("/createAddOns")
    @PreAuthorize("hasAuthority('MANAGE_PLANS')")
    public ResponseEntity<?> createAddOn(
            @RequestBody AddOnRequest req,
            HttpServletRequest httpReq) {
        log.info("Create add-on request name={} type={} price={}", req.getName(), req.getType(), req.getPrice());
        if (req.getName() == null || req.getName().isBlank()) {
            log.warn("Create add-on validation failed: name is required");
            return ResponseEntity.status(400)
                .body(new MessageResponse("name is required"));
        }
        if (req.getType() == null) {
            log.warn("Create add-on validation failed: type is required");
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "type must be DataTopup, ISDPack, RoamingPack, or SMSPack"));
        }
        if (req.getQuota() == null) {
            log.warn("Create add-on validation failed: quota is required");
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "quota must be a positive number"));
        }
        if (req.getPrice() == null) {
            log.warn("Create add-on validation failed: price is required");
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "price must be a positive number"));
        }
        try {
            service.createAddOn(req);
        } catch (IllegalArgumentException e) {
            log.error("Create add-on failed with illegal argument", e);
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "type must be DataTopup, ISDPack, RoamingPack, or SMSPack"));
        }
        auditClient.record(AuditAction.CREATE_ADDON, AuditModule.PLAN, httpReq);
        log.info("Add-on created successfully name={}", req.getName());
        return ResponseEntity.status(201)
            .body(new MessageResponse("Add-on created successfully"));
    }

    @GetMapping("/getAllAddOns")
    @PreAuthorize("hasAuthority('VIEW_PLAN')")
    public ResponseEntity<?> getAllAddOns() {
        log.info("Fetching all add-ons");
        List<AddOnResponse> addOns = service.getAllAddOns();
        if (addOns.isEmpty()) {
            log.info("No add-ons found in database");
            return ResponseEntity.status(404)
                .body(new MessageResponse("No add-ons found"));
        }
        log.info("Retrieved {} add-ons", addOns.size());
        return ResponseEntity.ok(addOns);
    }

    @GetMapping("/getAddOns/{addOnId}")
    @PreAuthorize("hasAuthority('VIEW_PLAN')")
    public ResponseEntity<?> getAddOnById(
            @PathVariable Integer addOnId) {
        log.info("Fetching add-on by id={}", addOnId);
        AddOnResponse addOn = service.getAddOnById(addOnId);
        if (addOn == null) {
            log.warn("Add-on not found addOnId={}", addOnId);
            return ResponseEntity.status(404)
                .body(new MessageResponse(
                    "Add-on with addOnId " + addOnId + " not found"));
        }
        log.debug("Add-on retrieved addOnId={} name={}", addOnId, addOn.getName());
        return ResponseEntity.ok(addOn);
    }
}