package com.teleconnect.plan.service;

import com.teleconnect.plan.dto.request.TelecomPlanRequest;
import com.teleconnect.plan.dto.response.TelecomPlanResponse;
import com.teleconnect.plan.entity.TelecomPlan;
import com.teleconnect.plan.repository.TelecomPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TelecomPlanServiceTest {

    @Mock
    private TelecomPlanRepository repository;

    @InjectMocks
    private TelecomPlanService service;

    private TelecomPlan mockPlan;
    private TelecomPlanRequest mockRequest;

    @BeforeEach
    void setUp() {
        // Create a mock TelecomPlan entity
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

        // Create a mock request DTO
        mockRequest = new TelecomPlanRequest();
        mockRequest.setName("Smart Postpaid 10GB");
        mockRequest.setType("Postpaid");
        mockRequest.setDataGb(new BigDecimal("10.00"));
        mockRequest.setVoiceMinutes(999);
        mockRequest.setSmsCount(200);
        mockRequest.setValidityDays(30);
        mockRequest.setPlanPrice(new BigDecimal("499.00"));
    }

    // ── CREATE PLAN TESTS ─────────────────────────────────────────────────────

    @Test
    void createPlan_withValidRequest_shouldSaveSuccessfully() {
        // Arrange
        when(repository.save(any(TelecomPlan.class))).thenReturn(mockPlan);

        // Act
        service.createPlan(mockRequest);

        // Assert
        verify(repository, times(1)).save(any(TelecomPlan.class));
    }

    @Test
    void createPlan_shouldSetStatusToActiveByDefault() {
        // Arrange
        when(repository.save(any(TelecomPlan.class))).thenReturn(mockPlan);

        // Act
        service.createPlan(mockRequest);

        // Assert - verify save was called with status A
        verify(repository).save(argThat(plan ->
            plan.getStatus() == TelecomPlan.PlanStatus.A
        ));
    }

    @Test
    void createPlan_withInvalidType_shouldThrowIllegalArgumentException() {
        // Arrange
        mockRequest.setType("InvalidType");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.createPlan(mockRequest);
        });
    }

    @Test
    void createPlan_withPrepaidType_shouldSaveSuccessfully() {
        // Arrange
        mockRequest.setType("Prepaid");
        when(repository.save(any(TelecomPlan.class))).thenReturn(mockPlan);

        // Act
        service.createPlan(mockRequest);

        // Assert
        verify(repository, times(1)).save(any(TelecomPlan.class));
    }

    // ── GET ALL PLANS TESTS ───────────────────────────────────────────────────

    @Test
    void getAllPlans_whenPlansExist_shouldReturnListOfPlans() {
        // Arrange
        when(repository.findAll()).thenReturn(Arrays.asList(mockPlan));

        // Act
        List<TelecomPlanResponse> result = service.getAllPlans();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Smart Postpaid 10GB", result.get(0).getName());
        assertEquals("Postpaid", result.get(0).getType());
        assertEquals("A", result.get(0).getStatus());
    }

    @Test
    void getAllPlans_whenNoPlansExist_shouldReturnEmptyList() {
        // Arrange
        when(repository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<TelecomPlanResponse> result = service.getAllPlans();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllPlans_shouldReturnAllFieldsCorrectly() {
        // Arrange
        when(repository.findAll()).thenReturn(Arrays.asList(mockPlan));

        // Act
        List<TelecomPlanResponse> result = service.getAllPlans();

        // Assert
        TelecomPlanResponse response = result.get(0);
        assertEquals(1, response.getPlanId());
        assertEquals("Smart Postpaid 10GB", response.getName());
        assertEquals(new BigDecimal("10.00"), response.getDataGb());
        assertEquals(999, response.getVoiceMinutes());
        assertEquals(200, response.getSmsCount());
        assertEquals(30, response.getValidityDays());
        assertEquals(new BigDecimal("499.00"), response.getPlanPrice());
    }

    // ── GET PLAN BY ID TESTS ──────────────────────────────────────────────────

    @Test
    void getPlanById_whenPlanExists_shouldReturnPlan() {
        // Arrange
        when(repository.findById(1)).thenReturn(Optional.of(mockPlan));

        // Act
        TelecomPlanResponse result = service.getPlanById(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getPlanId());
        assertEquals("Smart Postpaid 10GB", result.getName());
    }

    @Test
    void getPlanById_whenPlanDoesNotExist_shouldReturnNull() {
        // Arrange
        when(repository.findById(999)).thenReturn(Optional.empty());

        // Act
        TelecomPlanResponse result = service.getPlanById(999);

        // Assert
        assertNull(result);
    }

    @Test
    void getPlanById_shouldReturnCorrectStatus() {
        // Arrange
        when(repository.findById(1)).thenReturn(Optional.of(mockPlan));

        // Act
        TelecomPlanResponse result = service.getPlanById(1);

        // Assert
        assertEquals("A", result.getStatus());
    }

    // ── UPDATE PLAN TESTS ─────────────────────────────────────────────────────

    @Test
    void updatePlan_whenPlanExists_shouldReturnTrue() {
        // Arrange
        when(repository.findById(1)).thenReturn(Optional.of(mockPlan));
        when(repository.save(any(TelecomPlan.class))).thenReturn(mockPlan);
        mockRequest.setPlanPrice(new BigDecimal("549.00"));

        // Act
        boolean result = service.updatePlan(1, mockRequest);

        // Assert
        assertTrue(result);
    }

    @Test
    void updatePlan_whenPlanDoesNotExist_shouldReturnFalse() {
        // Arrange
        when(repository.findById(999)).thenReturn(Optional.empty());

        // Act
        boolean result = service.updatePlan(999, mockRequest);

        // Assert
        assertFalse(result);
    }

    @Test
    void updatePlan_shouldUpdateOnlyProvidedFields() {
        // Arrange
        when(repository.findById(1)).thenReturn(Optional.of(mockPlan));
        when(repository.save(any(TelecomPlan.class))).thenReturn(mockPlan);

        TelecomPlanRequest partialRequest = new TelecomPlanRequest();
        partialRequest.setPlanPrice(new BigDecimal("549.00"));

        // Act
        boolean result = service.updatePlan(1, partialRequest);

        // Assert
        assertTrue(result);
        verify(repository).save(argThat(plan ->
            plan.getPlanPrice().equals(new BigDecimal("549.00"))
        ));
    }

    @Test
    void updatePlan_shouldUpdateStatusToDiscontinued() {
        // Arrange
        when(repository.findById(1)).thenReturn(Optional.of(mockPlan));
        when(repository.save(any(TelecomPlan.class))).thenReturn(mockPlan);

        TelecomPlanRequest statusRequest = new TelecomPlanRequest();
        statusRequest.setStatus("D");

        // Act
        boolean result = service.updatePlan(1, statusRequest);

        // Assert
        assertTrue(result);
        verify(repository).save(argThat(plan ->
            plan.getStatus() == TelecomPlan.PlanStatus.D
        ));
    }

    @Test
    void updatePlan_shouldUpdateStatusToPromotional() {
        // Arrange
        when(repository.findById(1)).thenReturn(Optional.of(mockPlan));
        when(repository.save(any(TelecomPlan.class))).thenReturn(mockPlan);

        TelecomPlanRequest statusRequest = new TelecomPlanRequest();
        statusRequest.setStatus("P");

        // Act
        boolean result = service.updatePlan(1, statusRequest);

        // Assert
        assertTrue(result);
        verify(repository).save(argThat(plan ->
            plan.getStatus() == TelecomPlan.PlanStatus.P
        ));
    }

    @Test
    void updatePlan_withNullFields_shouldNotOverwriteExistingValues() {
        // Arrange
        when(repository.findById(1)).thenReturn(Optional.of(mockPlan));
        when(repository.save(any(TelecomPlan.class))).thenReturn(mockPlan);

        TelecomPlanRequest emptyRequest = new TelecomPlanRequest();
        // All fields are null

        // Act
        boolean result = service.updatePlan(1, emptyRequest);

        // Assert
        assertTrue(result);
        // Name should still be original value
        verify(repository).save(argThat(plan ->
            plan.getName().equals("Smart Postpaid 10GB")
        ));
    }
}