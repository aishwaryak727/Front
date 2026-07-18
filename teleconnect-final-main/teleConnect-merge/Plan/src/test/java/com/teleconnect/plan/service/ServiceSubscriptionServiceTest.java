package com.teleconnect.plan.service;

import com.teleconnect.plan.dto.request.ServiceSubscriptionRequest;
import com.teleconnect.plan.dto.response.ServiceSubscriptionResponse;
import com.teleconnect.plan.entity.ServiceSubscription;
import com.teleconnect.plan.entity.TelecomPlan;
import com.teleconnect.plan.repository.ServiceSubscriptionRepository;
import com.teleconnect.plan.repository.TelecomPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceSubscriptionServiceTest {

    @Mock
    private ServiceSubscriptionRepository repository;

    @Mock
    private TelecomPlanRepository planRepository;

    @InjectMocks
    private ServiceSubscriptionService service;

    private ServiceSubscription mockSub;
    private ServiceSubscriptionRequest mockRequest;
    private TelecomPlan mockPlan;

    @BeforeEach
    void setUp() {
        mockPlan = new TelecomPlan();
        mockPlan.setPlanId(1);
        mockPlan.setName("Smart Postpaid 10GB");
        mockPlan.setType(TelecomPlan.PlanType.Postpaid);
        mockPlan.setDataGb(new BigDecimal("10.00"));
        mockPlan.setVoiceMinutes(999);
        mockPlan.setSmsCount(200);
        mockPlan.setValidityDays(30);
        mockPlan.setPlanPrice(new BigDecimal("499.00"));
        mockPlan.setStatus(TelecomPlan.PlanStatus.A);

        mockSub = new ServiceSubscription();
        mockSub.setSubscriptionId(1);
        mockSub.setLineId(1);
        mockSub.setPlanId(1);
        mockSub.setAddOnId(1);
        mockSub.setActivationDate(LocalDate.of(2026, 6, 1));
        mockSub.setExpiryDate(LocalDate.of(2026, 6, 30));
        mockSub.setRenewalType(ServiceSubscription.RenewalType.AutoRenew);
        mockSub.setStatus(ServiceSubscription.Status.A);

        mockRequest = new ServiceSubscriptionRequest();
        mockRequest.setLineId(1);
        mockRequest.setPlanId(1);
        mockRequest.setAddOnId(1);
        mockRequest.setActivationDate(LocalDate.of(2026, 6, 1));
        mockRequest.setExpiryDate(LocalDate.of(2026, 6, 30));
        mockRequest.setRenewalType("AutoRenew");
    }

    // ── VALIDATE TESTS ────────────────────────────────────────────────────────

    @Test
    void validate_withValidRequest_shouldReturnNull() {
        when(planRepository.findById(1)).thenReturn(Optional.of(mockPlan));
        String result = service.validate(mockRequest);
        assertNull(result);
    }

    @Test
    void validate_withMissingLineId_shouldReturnError() {
        mockRequest.setLineId(null);
        String result = service.validate(mockRequest);
        assertEquals("lineId is required", result);
    }

    @Test
    void validate_withMissingPlanId_shouldReturnError() {
        mockRequest.setPlanId(null);
        String result = service.validate(mockRequest);
        assertEquals("planId is required", result);
    }

    @Test
    void validate_withMissingActivationDate_shouldReturnError() {
        mockRequest.setActivationDate(null);
        String result = service.validate(mockRequest);
        assertEquals("activationDate is required", result);
    }

    @Test
    void validate_withMissingExpiryDate_shouldReturnError() {
        mockRequest.setExpiryDate(null);
        String result = service.validate(mockRequest);
        assertEquals("expiryDate is required", result);
    }

    @Test
    void validate_withExpiryBeforeActivation_shouldReturnError() {
        mockRequest.setActivationDate(LocalDate.of(2026, 6, 30));
        mockRequest.setExpiryDate(LocalDate.of(2026, 6, 1));
        String result = service.validate(mockRequest);
        assertEquals("expiryDate must be after activationDate", result);
    }

    @Test
    void validate_withSameActivationAndExpiryDate_shouldReturnError() {
        mockRequest.setActivationDate(LocalDate.of(2026, 6, 1));
        mockRequest.setExpiryDate(LocalDate.of(2026, 6, 1));
        String result = service.validate(mockRequest);
        assertEquals("expiryDate must be after activationDate", result);
    }

    @Test
    void validate_withMissingRenewalType_shouldReturnError() {
        lenient().when(planRepository.findById(1)).thenReturn(Optional.of(mockPlan));
        mockRequest.setRenewalType(null);
        String result = service.validate(mockRequest);
        assertEquals("renewalType must be AutoRenew or Manual", result);
    }

    @Test
    void validate_withNonExistingPlanId_shouldReturnError() {
        when(planRepository.findById(999)).thenReturn(Optional.empty());
        mockRequest.setPlanId(999);
        String result = service.validate(mockRequest);
        assertEquals("Plan with planId 999 not found", result);
    }

    // ── CREATE SUBSCRIPTION TESTS ─────────────────────────────────────────────

    @Test
    void createSubscription_withValidRequest_shouldSaveSuccessfully() {
        when(repository.save(any(ServiceSubscription.class)))
            .thenReturn(mockSub);
        service.createSubscription(mockRequest);
        verify(repository, times(1)).save(any(ServiceSubscription.class));
    }

    @Test
    void createSubscription_shouldSetStatusToActiveByDefault() {
        when(repository.save(any(ServiceSubscription.class)))
            .thenReturn(mockSub);
        service.createSubscription(mockRequest);
        verify(repository).save(argThat(sub ->
            sub.getStatus() == ServiceSubscription.Status.A
        ));
    }

    @Test
    void createSubscription_withoutAddOn_shouldSaveWithNullAddOnId() {
        mockRequest.setAddOnId(null);
        when(repository.save(any(ServiceSubscription.class)))
            .thenReturn(mockSub);
        service.createSubscription(mockRequest);
        verify(repository).save(argThat(sub ->
            sub.getAddOnId() == null
        ));
    }

    @Test
    void createSubscription_withManualRenewal_shouldSaveCorrectly() {
        mockRequest.setRenewalType("Manual");
        when(repository.save(any(ServiceSubscription.class)))
            .thenReturn(mockSub);
        service.createSubscription(mockRequest);
        verify(repository).save(argThat(sub ->
            sub.getRenewalType() == ServiceSubscription.RenewalType.Manual
        ));
    }

    // ── GET ALL SUBSCRIPTIONS TESTS ───────────────────────────────────────────

    @Test
    void getAllSubscriptions_whenSubscriptionsExist_shouldReturnList() {
        when(repository.findAll()).thenReturn(Arrays.asList(mockSub));
        List<ServiceSubscriptionResponse> result =
            service.getAllSubscriptions();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getSubscriptionId());
    }

    @Test
    void getAllSubscriptions_whenNoSubscriptions_shouldReturnEmptyList() {
        when(repository.findAll()).thenReturn(Collections.emptyList());
        List<ServiceSubscriptionResponse> result =
            service.getAllSubscriptions();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllSubscriptions_shouldReturnAllFieldsCorrectly() {
        when(repository.findAll()).thenReturn(Arrays.asList(mockSub));
        List<ServiceSubscriptionResponse> result =
            service.getAllSubscriptions();
        ServiceSubscriptionResponse res = result.get(0);
        assertEquals(1, res.getSubscriptionId());
        assertEquals(1, res.getLineId());
        assertEquals(1, res.getPlanId());
        assertEquals(1, res.getAddOnId());
        assertEquals("AutoRenew", res.getRenewalType());
        assertEquals("A", res.getStatus());
    }

    // ── GET BY ID TESTS ───────────────────────────────────────────────────────

    @Test
    void getById_whenSubscriptionExists_shouldReturnSubscription() {
        when(repository.findById(1)).thenReturn(Optional.of(mockSub));
        ServiceSubscriptionResponse result = service.getById(1);
        assertNotNull(result);
        assertEquals(1, result.getSubscriptionId());
    }

    @Test
    void getById_whenSubscriptionDoesNotExist_shouldReturnNull() {
        when(repository.findById(999)).thenReturn(Optional.empty());
        ServiceSubscriptionResponse result = service.getById(999);
        assertNull(result);
    }

    // ── UPDATE SUBSCRIPTION TESTS ─────────────────────────────────────────────

    @Test
    void updateSubscription_whenExists_shouldReturnTrue() {
        when(repository.findById(1)).thenReturn(Optional.of(mockSub));
        when(repository.save(any(ServiceSubscription.class)))
            .thenReturn(mockSub);
        ServiceSubscriptionRequest updateReq =
            new ServiceSubscriptionRequest();
        updateReq.setRenewalType("Manual");
        boolean result = service.updateSubscription(1, updateReq);
        assertTrue(result);
    }

    @Test
    void updateSubscription_whenDoesNotExist_shouldReturnFalse() {
        when(repository.findById(999)).thenReturn(Optional.empty());
        boolean result = service.updateSubscription(999, mockRequest);
        assertFalse(result);
    }

    @Test
    void updateSubscription_shouldUpdateStatusToSuspended() {
        when(repository.findById(1)).thenReturn(Optional.of(mockSub));
        when(repository.save(any(ServiceSubscription.class)))
            .thenReturn(mockSub);
        ServiceSubscriptionRequest updateReq =
            new ServiceSubscriptionRequest();
        updateReq.setStatus("S");
        service.updateSubscription(1, updateReq);
        verify(repository).save(argThat(sub ->
            sub.getStatus() == ServiceSubscription.Status.S
        ));
    }

    @Test
    void updateSubscription_shouldUpdateStatusToExpired() {
        when(repository.findById(1)).thenReturn(Optional.of(mockSub));
        when(repository.save(any(ServiceSubscription.class)))
            .thenReturn(mockSub);
        ServiceSubscriptionRequest updateReq =
            new ServiceSubscriptionRequest();
        updateReq.setStatus("E");
        service.updateSubscription(1, updateReq);
        verify(repository).save(argThat(sub ->
            sub.getStatus() == ServiceSubscription.Status.E
        ));
    }

    @Test
    void updateSubscription_shouldUpdateAddOnId() {
        when(repository.findById(1)).thenReturn(Optional.of(mockSub));
        when(repository.save(any(ServiceSubscription.class)))
            .thenReturn(mockSub);
        ServiceSubscriptionRequest updateReq =
            new ServiceSubscriptionRequest();
        updateReq.setAddOnId(2);
        service.updateSubscription(1, updateReq);
        verify(repository).save(argThat(sub ->
            sub.getAddOnId() == 2
        ));
    }
}