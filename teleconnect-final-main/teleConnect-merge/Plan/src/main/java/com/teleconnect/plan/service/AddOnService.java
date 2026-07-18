package com.teleconnect.plan.service;

import lombok.extern.slf4j.Slf4j;

import com.teleconnect.plan.dto.request.AddOnRequest;
import com.teleconnect.plan.dto.response.AddOnResponse;
import com.teleconnect.plan.entity.AddOn;
import com.teleconnect.plan.repository.AddOnRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AddOnService {

    private final AddOnRepository repository;

    public AddOnService(AddOnRepository repository) {
        this.repository = repository;
    }

    private AddOnResponse toDTO(AddOn a) {
        AddOnResponse dto = new AddOnResponse();
        dto.setAddOnId(a.getAddOnId());
        dto.setName(a.getName());
        dto.setType(a.getType().name());
        dto.setQuota(a.getQuota());
        dto.setValidityDays(a.getValidityDays());
        dto.setPrice(a.getPrice());
        dto.setStatus(a.getStatus().name());
        return dto;
    }

    public void createAddOn(AddOnRequest req) {
        log.info("Create add-on request received name={} type={}", req.getName(), req.getType());
        AddOn addOn = new AddOn();
        addOn.setName(req.getName());
        addOn.setType(AddOn.AddOnType.valueOf(req.getType()));
        addOn.setQuota(req.getQuota());
        addOn.setValidityDays(req.getValidityDays());
        addOn.setPrice(req.getPrice());
        addOn.setStatus(AddOn.AddOnStatus.A);
        repository.save(addOn);
        log.info("Add-on created successfully name={}", req.getName());
    }

    public List<AddOnResponse> getAllAddOns() {
        log.debug("Fetching all add-ons");
        return repository.findAll()
            .stream().map(this::toDTO)
            .collect(Collectors.toList());
    }

    public AddOnResponse getAddOnById(Integer addOnId) {
        log.debug("Fetching add-on by id={}", addOnId);
        AddOn addOn = repository.findById(addOnId).orElse(null);
        if (addOn == null) {
            log.warn("Add-on not found id={}", addOnId);
            return null;
        }
        return toDTO(addOn);
    }
}
