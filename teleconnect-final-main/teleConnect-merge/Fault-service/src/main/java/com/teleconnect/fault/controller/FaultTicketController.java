package com.teleconnect.fault.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import com.teleconnect.fault.dto.request.FaultTicketRequest;
import com.teleconnect.fault.dto.response.FaultTicketResponse;
import com.teleconnect.fault.dto.response.MessageResponse;
import com.teleconnect.fault.service.FaultTicketService;
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
public class FaultTicketController {

    private final FaultTicketService ticketService;
    private final AuditClient auditClient;

    public FaultTicketController(FaultTicketService ticketService, AuditClient auditClient) {
        this.ticketService = ticketService;
        this.auditClient = auditClient;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized FaultTicketController");
    }

    // POST /teleConnect/fault/createTickets
    @PostMapping("/createTickets")
    @PreAuthorize("hasAuthority('SERVICE_REQUEST')")
    public ResponseEntity<MessageResponse> createTicket(
            @Valid @RequestBody FaultTicketRequest req,
            HttpServletRequest httpReq) {
        log.info("Creating fault ticket for lineId={} accountId={}", req.getLineId(), req.getAccountId());
        MessageResponse result = ticketService.createTicket(req);
        auditClient.record(AuditAction.CREATE_FAULT_TICKET, AuditModule.FAULT, httpReq);
        log.info("Fault ticket created successfully");
        return ResponseEntity.status(201).body(result);
    }

    // GET /teleConnect/fault/getAllTickets
    @GetMapping("/getAllTickets")
    @PreAuthorize("hasAuthority('GET_UPDATE_TICKET')")
    public ResponseEntity<List<FaultTicketResponse>> getAllTickets() {
        log.info("Fetching all fault tickets");
        List<FaultTicketResponse> tickets = ticketService.getAllTickets();
        log.info("Retrieved {} fault tickets", tickets.size());
        return ResponseEntity.ok(tickets);
    }

    // GET /teleConnect/fault/getTickets/{ticketId}
    @GetMapping("/getTickets/{ticketId}")
    @PreAuthorize("hasAuthority('GET_UPDATE_TICKET')")
    public ResponseEntity<FaultTicketResponse> getTicketById(
            @PathVariable Integer ticketId) {
        log.info("Fetching fault ticket ticketId={}", ticketId);
        FaultTicketResponse ticket = ticketService.getTicketById(ticketId);
        log.debug("Fault ticket retrieved ticketId={}", ticketId);
        return ResponseEntity.ok(ticket);
    }

    // PUT /teleConnect/fault/assignTickets/{ticketId}
    @PutMapping("/assignTickets/{ticketId}")
    @PreAuthorize("hasAuthority('RESOLVE_TICKET')")
    public ResponseEntity<MessageResponse> assignTicket(
            @PathVariable Integer ticketId,
            @RequestBody FaultTicketRequest req,
            HttpServletRequest httpReq) {
        log.info("Assigning fault ticket ticketId={}", ticketId);
        MessageResponse result = ticketService.assignTicket(ticketId, req);
        auditClient.record(AuditAction.ASSIGN_FAULT_TICKET, AuditModule.FAULT, httpReq);
        log.info("Fault ticket assigned ticketId={}", ticketId);
        return ResponseEntity.ok(result);
    }

    // PUT /teleConnect/fault/updateTickets/{ticketId}
    @PutMapping("/updateTickets/{ticketId}")
    @PreAuthorize("hasAuthority('GET_UPDATE_TICKET')")
    public ResponseEntity<MessageResponse> updateTicket(
            @PathVariable Integer ticketId,
            @RequestBody FaultTicketRequest req,
            HttpServletRequest httpReq) {
        log.info("Updating fault ticket ticketId={}", ticketId);
        MessageResponse result = ticketService.updateTicket(ticketId, req);
        auditClient.record(AuditAction.UPDATE_FAULT_TICKET, AuditModule.FAULT, httpReq);
        log.info("Fault ticket updated ticketId={}", ticketId);
        return ResponseEntity.ok(result);
    }

    // PUT /teleConnect/fault/resolveTickets/{ticketId}
    @PutMapping("/resolveTickets/{ticketId}")
    @PreAuthorize("hasAuthority('RESOLVE_TICKET')")
    public ResponseEntity<MessageResponse> resolveTicket(
            @PathVariable Integer ticketId,
            @RequestBody FaultTicketRequest req,
            HttpServletRequest httpReq) {
        log.info("Resolving fault ticket ticketId={}", ticketId);
        MessageResponse result = ticketService.resolveTicket(ticketId, req);
        auditClient.record(AuditAction.RESOLVE_FAULT_TICKET, AuditModule.FAULT, httpReq);
        log.info("Fault ticket resolved ticketId={}", ticketId);
        return ResponseEntity.ok(result);
    }
}
