package com.teleconnect.subscriber.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.subscriber.dto.request.*;
import com.teleconnect.subscriber.dto.response.*;
import com.teleconnect.subscriber.service.SimLineService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/teleConnect/api/subscribers")
public class SimLineController {

    private final SimLineService simLineService;
    private final AuditClient auditClient;

    public SimLineController(SimLineService simLineService, AuditClient auditClient) {
        this.simLineService = simLineService;
        this.auditClient = auditClient;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized SimLineController");
    }

    @PostMapping("/{accountId}/simLines")
    @PreAuthorize("hasAnyAuthority('CREATE_USER','VIEW_SUBSCRIBER')")
    public ResponseEntity<MessageDTO> createSimLine(
            @PathVariable Integer accountId,
            @Valid @RequestBody CreateSimLineRequest req,
            HttpServletRequest httpReq) {
        log.info("Create SIM line for accountId={} msisdn={}", accountId, req.getMsisdn());
        var result = simLineService.createSimLine(accountId, req);
        auditClient.record(AuditAction.CREATE_SIM_LINE, AuditModule.SUBSCRIBER, httpReq);
        log.info("SIM line created for accountId={}", accountId);
        return ResponseEntity.status(201)
            .body(result);
    }

    @GetMapping("/{accountId}/simLines")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER','VIEW_OWN_PLAN')")
    public ResponseEntity<List<SimLineResponseDTO>> getSimLines(
            @PathVariable Integer accountId,
            @RequestParam(required = false) String status) {
        log.info("Fetching SIM lines for accountId={} status={}", accountId, status);
        List<SimLineResponseDTO> lines = simLineService.getSimLinesByAccount(accountId, status);
        log.info("Retrieved {} SIM lines for accountId={}", lines.size(), accountId);
        return ResponseEntity.ok(lines);
    }

    @GetMapping("/{accountId}/simLines/{lineId}")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER','VIEW_OWN_PLAN')")
    public ResponseEntity<SimLineResponseDTO> getSimLine(
            @PathVariable Integer accountId,
            @PathVariable Integer lineId) {
        log.info("Get SIM line accountId={} lineId={}", accountId, lineId);
        SimLineResponseDTO line = simLineService.getSimLineById(accountId, lineId);
        log.debug("SIM line retrieved accountId={} lineId={}", accountId, lineId);
        return ResponseEntity.ok(line);
    }

    @GetMapping("/sim-lines/lookup")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER')")
    public ResponseEntity<SimLineResponseDTO> lookupByMsisdn(
            @RequestParam String msisdn) {
        log.info("Looking up SIM line by msisdn={}", msisdn);
        SimLineResponseDTO line = simLineService.lookupByMsisdn(msisdn);
        log.debug("SIM line lookup found msisdn={}", msisdn);
        return ResponseEntity.ok(line);
    }

    @PutMapping("/{accountId}/simLines/{lineId}/status")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER')")
    public ResponseEntity<MessageDTO> updateSimStatus(
            @PathVariable Integer accountId,
            @PathVariable Integer lineId,
            @Valid @RequestBody UpdateSimStatusRequest req,
            HttpServletRequest httpReq) {
        log.info("Update SIM status accountId={} lineId={} status={}", accountId, lineId, req.getStatus());
        var result = simLineService.updateSimStatus(accountId, lineId, req);
        auditClient.record(AuditAction.UPDATE_SIM_STATUS, AuditModule.SUBSCRIBER, httpReq);
        log.info("SIM status updated accountId={} lineId={}", accountId, lineId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{accountId}/simLines/{lineId}/replace")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER')")
    public ResponseEntity<SimLineResponseDTO> replaceSim(
            @PathVariable Integer accountId,
            @PathVariable Integer lineId,
            @Valid @RequestBody ReplaceSimRequest req,
            HttpServletRequest httpReq) {
        log.info("Replace SIM accountId={} lineId={}", accountId, lineId);
        var result = simLineService.replaceSim(accountId, lineId, req);
        auditClient.record(AuditAction.REPLACE_SIM, AuditModule.SUBSCRIBER, httpReq);
        log.info("SIM replaced accountId={} lineId={}", accountId, lineId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{accountId}/simLines/{lineId}/service-type")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER')")
    public ResponseEntity<MessageDTO> updateServiceType(
            @PathVariable Integer accountId,
            @PathVariable Integer lineId,
            @Valid @RequestBody UpdateServiceTypeRequest req,
            HttpServletRequest httpReq) {
        log.info("Update service type accountId={} lineId={} type={}", accountId, lineId, req.getServiceType());
        var result = simLineService.updateServiceType(accountId, lineId, req);
        auditClient.record(AuditAction.UPDATE_SIM_SERVICE_TYPE, AuditModule.SUBSCRIBER, httpReq);
        log.info("Service type updated accountId={} lineId={}", accountId, lineId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{accountId}/simLines/{lineId}")
    @PreAuthorize("hasAuthority('DELETE_USER')")
    public ResponseEntity<MessageDTO> deleteSimLine(
            @PathVariable Integer accountId,
            @PathVariable Integer lineId,
            HttpServletRequest httpReq) {
        log.info("Delete SIM line accountId={} lineId={}", accountId, lineId);
        var result = simLineService.deleteSimLine(accountId, lineId);
        auditClient.record(AuditAction.DELETE_SIM_LINE, AuditModule.SUBSCRIBER, httpReq);
        log.info("SIM line deleted accountId={} lineId={}", accountId, lineId);
        return ResponseEntity.ok(result);
    }
}