package com.teleconnect.plan.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.plan.dto.request.TelecomPlanRequest;
import com.teleconnect.plan.dto.response.TelecomPlanResponse;
import com.teleconnect.plan.dto.response.MessageResponse;
import com.teleconnect.plan.service.TelecomPlanService;
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
public class TelecomPlanController {

    private final TelecomPlanService service;
    private final AuditClient auditClient;

    public TelecomPlanController(TelecomPlanService service, AuditClient auditClient) {
        this.service = service;
        this.auditClient = auditClient;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized TelecomPlanController");
    }

    @PostMapping("/createPlans")
    @PreAuthorize("hasAuthority('MANAGE_PLANS')")
    public ResponseEntity<?> createPlan(
            @RequestBody TelecomPlanRequest req,
            HttpServletRequest httpReq) {
        log.info("Create plan request name={} type={} price={}", req.getName(), req.getType(), req.getPlanPrice());
        if (req.getName() == null || req.getName().isBlank()) {
            log.warn("Create plan validation failed: name is required");
            return ResponseEntity.status(400)
                .body(new MessageResponse("name is required"));
        }
        if (req.getType() == null) {
            log.warn("Create plan validation failed: type is required");
            return ResponseEntity.status(400)
                .body(new MessageResponse("type must be Postpaid or Prepaid"));
        }
        if (req.getPlanPrice() == null) {
            log.warn("Create plan validation failed: planPrice is required");
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "planPrice must be a positive number"));
        }
        if (req.getValidityDays() == null) {
            log.warn("Create plan validation failed: validityDays is required");
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "validityDays must be a positive integer"));
        }
        try {
            service.createPlan(req);
        } catch (IllegalArgumentException e) {
            log.error("Create plan failed with illegal argument", e);
            return ResponseEntity.status(400)
                .body(new MessageResponse("type must be Postpaid or Prepaid"));
        }
        auditClient.record(AuditAction.CREATE_PLAN, AuditModule.PLAN, httpReq);
        log.info("Plan created successfully name={}", req.getName());
        return ResponseEntity.status(201)
            .body(new MessageResponse("Plan created successfully"));
    }

    @GetMapping("/getAllPlans")
    @PreAuthorize("hasAuthority('VIEW_PLAN')")
    public ResponseEntity<?> getAllPlans() {
        log.info("Fetching all plans");
        List<TelecomPlanResponse> plans = service.getAllPlans();
        if (plans.isEmpty()) {
            log.info("No plans found in database");
            return ResponseEntity.status(404)
                .body(new MessageResponse("No plans found"));
        }
        log.info("Retrieved {} plans", plans.size());
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/getPlans/{planId}")
    @PreAuthorize("hasAuthority('VIEW_PLAN')")
    public ResponseEntity<?> getPlanById(
            @PathVariable Integer planId) {
        log.info("Fetching plan by id={}", planId);
        TelecomPlanResponse plan = service.getPlanById(planId);
        if (plan == null) {
            log.warn("Plan not found planId={}", planId);
            return ResponseEntity.status(404)
                .body(new MessageResponse(
                    "Plan with planId " + planId + " not found"));
        }
        log.debug("Plan retrieved planId={} name={}", planId, plan.getName());
        return ResponseEntity.ok(plan);
    }

    @PutMapping("/updatePlans/{planId}")
    @PreAuthorize("hasAuthority('MANAGE_PLANS')")
    public ResponseEntity<?> updatePlan(
            @PathVariable Integer planId,
            @RequestBody TelecomPlanRequest req,
            HttpServletRequest httpReq) {
        log.info("Update plan request planId={} name={}", planId, req.getName());
        boolean updated = service.updatePlan(planId, req);
        if (!updated) {
            log.warn("Plan not found for update planId={}", planId);
            return ResponseEntity.status(404)
                .body(new MessageResponse(
                    "Plan with planId " + planId + " not found"));
        }
        auditClient.record(AuditAction.UPDATE_PLAN, AuditModule.PLAN, httpReq);
        log.info("Plan updated planId={}", planId);
        return ResponseEntity.ok(
            new MessageResponse("Plan updated successfully"));
    }
}