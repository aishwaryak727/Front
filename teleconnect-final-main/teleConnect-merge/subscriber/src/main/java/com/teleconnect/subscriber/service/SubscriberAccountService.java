package com.teleconnect.subscriber.service;

import lombok.extern.slf4j.Slf4j;

import com.teleconnect.subscriber.dto.request.*;
import com.teleconnect.subscriber.dto.response.*;
import com.teleconnect.subscriber.entity.SubscriberAccount;
import com.teleconnect.subscriber.entity.SimLine;
import com.teleconnect.subscriber.repository.SubscriberAccountRepository;
import com.teleconnect.subscriber.repository.SimLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubscriberAccountService {

    private final SubscriberAccountRepository accountRepo;
    private final SimLineRepository simLineRepo;

    public SubscriberAccountService(SubscriberAccountRepository accountRepo, SimLineRepository simLineRepo) {
        this.accountRepo = accountRepo;
        this.simLineRepo = simLineRepo;
    }

    private AccountResponseDTO toDTO(SubscriberAccount a) {
        AccountResponseDTO dto = new AccountResponseDTO();
        dto.setAccountId(a.getAccountId());
        dto.setSubscriberId(a.getSubscriberId());
        dto.setAccountType(a.getAccountType().name());
        dto.setRegistrationDate(a.getRegistrationDate());
        dto.setKycStatus(a.getKycStatus().name());
        dto.setStatus(a.getStatus().name());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }

    public MessageDTO createAccount(CreateAccountRequest req) {
        log.info("Create account request for subscriberId={} type={}", req.getSubscriberId(), req.getAccountType());
        SubscriberAccount account = new SubscriberAccount();
        account.setSubscriberId(req.getSubscriberId());
        account.setAccountType(
            SubscriberAccount.AccountType.valueOf(req.getAccountType()));
        account.setKycStatus(
            SubscriberAccount.KycStatus.valueOf(req.getKycStatus()));
        accountRepo.save(account);
        log.info("Account created successfully for subscriberId={} accountId={}", req.getSubscriberId(), account.getAccountId());
        return new MessageDTO("Account created successfully");
    }

    public AccountResponseDTO getAccountById(Integer accountId) {
        log.debug("Fetching account by id={}", accountId);
        SubscriberAccount account = accountRepo.findById(accountId)
            .orElseThrow(() -> new RuntimeException(
                "Account not found: " + accountId));
        return toDTO(account);
    }

    public AccountListResponseDTO getAllAccounts(String status, Long subscriberId) {
        log.debug("Fetching accounts list status={} subscriberId={}", status, subscriberId);
        List<SubscriberAccount> all = accountRepo.findAll();
        if (status != null) {
            SubscriberAccount.AccountStatus s =
                SubscriberAccount.AccountStatus.valueOf(status);
            all = all.stream()
                .filter(a -> a.getStatus() == s)
                .collect(Collectors.toList());
        }
        if (subscriberId != null) {
            all = all.stream()
                .filter(a -> a.getSubscriberId().equals(subscriberId))
                .collect(Collectors.toList());
        }
        List<AccountResponseDTO> dtos = all.stream()
            .map(this::toDTO).collect(Collectors.toList());
        return new AccountListResponseDTO(dtos, dtos.size());
    }

    public List<AccountResponseDTO> getExpiredKycAccounts() {
        log.debug("Fetching expired KYC accounts");
        return accountRepo
            .findByKycStatus(SubscriberAccount.KycStatus.Expired)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public MessageDTO updateKyc(Integer accountId, UpdateKycRequest req) {
        log.info("Update KYC requested accountId={} status={}", accountId, req.getKycStatus());
        SubscriberAccount account = accountRepo.findById(accountId)
            .orElseThrow(() -> new RuntimeException(
                "Account not found: " + accountId));
        account.setKycStatus(
            SubscriberAccount.KycStatus.valueOf(req.getKycStatus()));
        accountRepo.save(account);
        log.info("KYC status updated accountId={} status={}", accountId, req.getKycStatus());
        return new MessageDTO("KYC status updated to " + req.getKycStatus());
    }

    public MessageDTO updateStatus(Integer accountId,
                                   UpdateAccountStatusRequest req) {
        log.info("Update account status requested accountId={} status={}", accountId, req.getStatus());
        SubscriberAccount account = accountRepo.findById(accountId)
            .orElseThrow(() -> new RuntimeException(
                "Account not found: " + accountId));
        account.setStatus(
            SubscriberAccount.AccountStatus.valueOf(req.getStatus()));
        accountRepo.save(account);
        log.info("Account status updated accountId={} status={}", accountId, req.getStatus());
        return new MessageDTO("Account status updated to " + req.getStatus());
    }

    public MessageDTO deleteAccount(Integer accountId) {
        log.info("Delete account requested accountId={}", accountId);
        SubscriberAccount account = accountRepo.findById(accountId)
            .orElseThrow(() -> new RuntimeException(
                "Account not found: " + accountId));
        List<SimLine> simLines = simLineRepo.findByAccountId(accountId);
        long count = simLines.stream()
            .filter(sl -> sl.getStatus() == SimLine.SimStatus.Active)
            .peek(sl -> sl.setStatus(SimLine.SimStatus.Deactivated))
            .peek(simLineRepo::save)
            .count();
        account.setStatus(SubscriberAccount.AccountStatus.Terminated);
        accountRepo.save(account);
        log.info("Account {} terminated; SIM lines deactivated: {}", accountId, count);
        return new MessageDTO("Account " + accountId +
            " terminated. SIM lines deactivated: " + count);
    }
}