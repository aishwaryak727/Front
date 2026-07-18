package com.teleconnect.plan.service;

import lombok.extern.slf4j.Slf4j;

import com.teleconnect.plan.dto.request.ServiceSubscriptionRequest;
import com.teleconnect.plan.dto.response.ServiceSubscriptionResponse;
import com.teleconnect.plan.entity.ServiceSubscription;
import com.teleconnect.plan.entity.TelecomPlan;
import com.teleconnect.plan.repository.ServiceSubscriptionRepository;
import com.teleconnect.plan.repository.TelecomPlanRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ServiceSubscriptionService {

    private final ServiceSubscriptionRepository repository;
    private final TelecomPlanRepository planRepository;

    public ServiceSubscriptionService(ServiceSubscriptionRepository repository, TelecomPlanRepository planRepository) {
        this.repository = repository;
        this.planRepository = planRepository;
    }

    private ServiceSubscriptionResponse toDTO(ServiceSubscription s) {
        ServiceSubscriptionResponse dto = new ServiceSubscriptionResponse();
        dto.setSubscriptionId(s.getSubscriptionId());
        dto.setLineId(s.getLineId());
        dto.setPlanId(s.getPlanId());
        dto.setAddOnId(s.getAddOnId());
        dto.setActivationDate(s.getActivationDate());
        dto.setExpiryDate(s.getExpiryDate());
        dto.setRenewalType(s.getRenewalType().name());
        dto.setStatus(s.getStatus().name());
        return dto;
    }

    public String validate(ServiceSubscriptionRequest req) {
        log.debug("Validating subscription request planId={} lineId={}", req.getPlanId(), req.getLineId());
        if (req.getLineId() == null)
            return "lineId is required";
        if (req.getPlanId() == null)
            return "planId is required";
        if (req.getActivationDate() == null)
            return "activationDate is required";
        if (req.getExpiryDate() == null)
            return "expiryDate is required";
        if (req.getExpiryDate() != null
                && req.getActivationDate() != null
                && !req.getExpiryDate().isAfter(req.getActivationDate()))
            return "expiryDate must be after activationDate";
        if (req.getRenewalType() == null)
            return "renewalType must be AutoRenew or Manual";
        TelecomPlan plan = planRepository
            .findById(req.getPlanId()).orElse(null);
        if (plan == null) {
            log.warn("Plan not found during subscription validation planId={}", req.getPlanId());
            return "Plan with planId " + req.getPlanId() + " not found";
        }
        return null;
    }

    public void createSubscription(ServiceSubscriptionRequest req) {
        log.info("Create subscription request received lineId={} planId={}", req.getLineId(), req.getPlanId());
        ServiceSubscription sub = new ServiceSubscription();
        sub.setLineId(req.getLineId());
        sub.setPlanId(req.getPlanId());
        sub.setAddOnId(req.getAddOnId());
        sub.setActivationDate(req.getActivationDate());
        sub.setExpiryDate(req.getExpiryDate());
        sub.setRenewalType(
            ServiceSubscription.RenewalType.valueOf(req.getRenewalType()));
        sub.setStatus(ServiceSubscription.Status.A);
        repository.save(sub);
        log.info("Subscription created successfully lineId={} subscriptionId={}", req.getLineId(), sub.getSubscriptionId());
    }

    public List<ServiceSubscriptionResponse> getAllSubscriptions() {
        log.debug("Fetching all subscriptions");
        List<ServiceSubscriptionResponse> res = repository.findAll()
            .stream().map(this::toDTO)
            .collect(Collectors.toList());
        log.debug("Retrieved {} subscriptions", res.size());
        return res;
    }

    public ServiceSubscriptionResponse getById(Integer subscriptionId) {
        log.debug("Fetching subscription by id={}", subscriptionId);
        ServiceSubscription sub = repository
            .findById(subscriptionId).orElse(null);
        if (sub == null) {
            log.warn("Subscription not found id={}", subscriptionId);
            return null;
        }
        return toDTO(sub);
    }

    public boolean updateSubscription(Integer subscriptionId,
            ServiceSubscriptionRequest req) {
        log.info("Update subscription requested id={}", subscriptionId);
        ServiceSubscription existing = repository
            .findById(subscriptionId).orElse(null);
        if (existing == null) return false;
        if (req.getAddOnId() != null)
            existing.setAddOnId(req.getAddOnId());
        if (req.getRenewalType() != null)
            existing.setRenewalType(ServiceSubscription.RenewalType
                .valueOf(req.getRenewalType()));
        if (req.getStatus() != null)
        existing.setStatus(ServiceSubscription.Status
            .valueOf(req.getStatus()));
        repository.save(existing);
        log.info("Subscription updated successfully id={}", subscriptionId);
        return true;
    }
}