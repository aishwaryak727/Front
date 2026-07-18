package com.teleconnect.subscriber.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditClient;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.subscriber.dto.request.CreateAccountRequest;
import com.teleconnect.subscriber.dto.request.UpdateAccountStatusRequest;
import com.teleconnect.subscriber.dto.request.UpdateKycRequest;
import com.teleconnect.subscriber.dto.response.AccountListResponseDTO;
import com.teleconnect.subscriber.dto.response.AccountResponseDTO;
import com.teleconnect.subscriber.dto.response.MessageDTO;
import com.teleconnect.subscriber.service.SubscriberAccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/teleConnect/api/subscribers")
public class SubscriberAccountController {

    private final SubscriberAccountService accountService;
    private final AuditClient auditClient;

    public SubscriberAccountController(SubscriberAccountService accountService, AuditClient auditClient) {
        this.accountService = accountService;
        this.auditClient = auditClient;
    }

    @PostConstruct
    public void init() {
        log.info("Initialized SubscriberAccountController");
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CREATE_USER','VIEW_SUBSCRIBER')")
    public ResponseEntity<MessageDTO> createAccount(
            @Valid @RequestBody CreateAccountRequest req,
            HttpServletRequest httpReq) {
        log.info("Creating subscriber account subscriberId={} accountType={}", req.getSubscriberId(), req.getAccountType());
        var result = accountService.createAccount(req);
        auditClient.record(AuditAction.CREATE_SUBSCRIBER_ACCOUNT, AuditModule.SUBSCRIBER, httpReq);
        log.info("Subscriber account created subscriberId={} accountType={}", req.getSubscriberId(), req.getAccountType());
        return ResponseEntity.status(201)
            .body(result);
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyAuthority('VIEW_SUBSCRIBER')")
    public ResponseEntity<AccountResponseDTO> getAccount(
            @PathVariable Integer accountId) {
        log.info("Fetching account by id={}", accountId);
        AccountResponseDTO account = accountService.getAccountById(accountId);
        log.debug("Account retrieved accountId={}", accountId);
        return ResponseEntity.ok(account);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('VIEW_SUBSCRIBER', 'VIEW_OWN_PLAN')")
    public ResponseEntity<AccountListResponseDTO> getAllAccounts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long subscriberId) {
        log.info("Fetching all accounts status={} subscriberId={}", status, subscriberId);
        AccountListResponseDTO accounts = accountService.getAllAccounts(status, subscriberId);
        log.info("Retrieved accounts");
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/kyc/expired")
    @PreAuthorize("hasAnyAuthority('KYC_EXPIRE')")
    public ResponseEntity<List<AccountResponseDTO>> getExpiredKyc() {
        log.info("Fetching expired KYC accounts");
        List<AccountResponseDTO> accounts = accountService.getExpiredKycAccounts();
        log.info("Retrieved {} expired KYC accounts", accounts.size());
        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{accountId}/kyc")
    @PreAuthorize("hasAnyAuthority('VIEW_KYC')")
    public ResponseEntity<MessageDTO> updateKyc(
            @PathVariable Integer accountId,
            @Valid @RequestBody UpdateKycRequest req,
            HttpServletRequest httpReq) {
        log.info("Update KYC for accountId={} status={}", accountId, req.getKycStatus());
        var result = accountService.updateKyc(accountId, req);
        auditClient.record(AuditAction.UPDATE_KYC_STATUS, AuditModule.SUBSCRIBER, httpReq);
        log.info("KYC updated for accountId={}", accountId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{accountId}/status")
    @PreAuthorize("hasAnyAuthority('VIEW_SUBSCRIBER')")
    public ResponseEntity<MessageDTO> updateStatus(
            @PathVariable Integer accountId,
            @Valid @RequestBody UpdateAccountStatusRequest req,
            HttpServletRequest httpReq) {
        log.info("Update account status accountId={} status={}", accountId, req.getStatus());
        var result = accountService.updateStatus(accountId, req);
        auditClient.record(AuditAction.UPDATE_ACCOUNT_STATUS, AuditModule.SUBSCRIBER, httpReq);
        log.info("Account status updated accountId={}", accountId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{accountId}")
    @PreAuthorize("hasAuthority('DELETE_USER')")
    public ResponseEntity<MessageDTO> deleteAccount(
            @PathVariable Integer accountId,
            HttpServletRequest httpReq) {
        log.info("Delete account accountId={}", accountId);
        var result = accountService.deleteAccount(accountId);
        auditClient.record(AuditAction.DELETE_SUBSCRIBER_ACCOUNT, AuditModule.SUBSCRIBER, httpReq);
        log.info("Account deleted accountId={}", accountId);
        return ResponseEntity.ok(result);
    }
}