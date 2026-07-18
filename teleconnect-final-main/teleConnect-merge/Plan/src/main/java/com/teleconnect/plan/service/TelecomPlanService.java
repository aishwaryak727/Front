package com.teleconnect.plan.service;

import lombok.extern.slf4j.Slf4j;

import com.teleconnect.plan.dto.request.TelecomPlanRequest;
import com.teleconnect.plan.dto.response.TelecomPlanResponse;
import com.teleconnect.plan.entity.TelecomPlan;
import com.teleconnect.plan.repository.TelecomPlanRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TelecomPlanService {

    private final TelecomPlanRepository repository;

    public TelecomPlanService(TelecomPlanRepository repository) {
        this.repository = repository;
    }

    private TelecomPlanResponse toDTO(TelecomPlan p) {
        TelecomPlanResponse dto = new TelecomPlanResponse();
        dto.setPlanId(p.getPlanId());
        dto.setName(p.getName());
        dto.setType(p.getType().name());
        dto.setDataGb(p.getDataGb());
        dto.setVoiceMinutes(p.getVoiceMinutes());
        dto.setSmsCount(p.getSmsCount());
        dto.setValidityDays(p.getValidityDays());
        dto.setPlanPrice(p.getPlanPrice());
        dto.setStatus(p.getStatus().name());
        return dto;
    }

    public void createPlan(TelecomPlanRequest req) {
        log.info("Create plan request received name={} type={}", req.getName(), req.getType());
        TelecomPlan plan = new TelecomPlan();
        plan.setName(req.getName());
        plan.setType(TelecomPlan.PlanType.valueOf(req.getType()));
        plan.setDataGb(req.getDataGb());
        plan.setVoiceMinutes(req.getVoiceMinutes());
        plan.setSmsCount(req.getSmsCount());
        plan.setValidityDays(req.getValidityDays());
        plan.setPlanPrice(req.getPlanPrice());
        plan.setStatus(TelecomPlan.PlanStatus.A);
        repository.save(plan);
        log.info("Plan created successfully name={}", req.getName());
    }

    public List<TelecomPlanResponse> getAllPlans() {
        log.debug("Fetching all telecom plans");
        List<TelecomPlanResponse> res = repository.findAll()
            .stream().map(this::toDTO)
            .collect(Collectors.toList());
        log.debug("Retrieved {} plans", res.size());
        return res;
    }

    public TelecomPlanResponse getPlanById(Integer planId) {
        TelecomPlan plan = repository.findById(planId).orElse(null);
        if (plan == null) return null;
        return toDTO(plan);
    }

    public boolean updatePlan(Integer planId, TelecomPlanRequest req) {
        log.info("Update plan requested planId={}", planId);
        TelecomPlan existing = repository.findById(planId).orElse(null);
        if (existing == null) return false;
        if (req.getName() != null)
            existing.setName(req.getName());
        if (req.getDataGb() != null)
            existing.setDataGb(req.getDataGb());
        if (req.getVoiceMinutes() != null)
            existing.setVoiceMinutes(req.getVoiceMinutes());
        if (req.getSmsCount() != null)
            existing.setSmsCount(req.getSmsCount());
        if (req.getValidityDays() != null)
            existing.setValidityDays(req.getValidityDays());
        if (req.getPlanPrice() != null)
            existing.setPlanPrice(req.getPlanPrice());
        if (req.getStatus() != null)
        existing.setStatus(TelecomPlan.PlanStatus.valueOf(req.getStatus()));
        repository.save(existing);
        log.info("Plan updated successfully planId={}", planId);
        return true;
    }
}