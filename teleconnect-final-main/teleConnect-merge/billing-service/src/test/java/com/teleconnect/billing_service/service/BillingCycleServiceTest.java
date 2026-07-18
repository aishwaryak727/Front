package com.teleconnect.billing_service.service;

import com.teleconnect.billing_service.dto.request.BillingCycleRequest;
import com.teleconnect.billing_service.dto.response.BillingCycleResponse;
import com.teleconnect.billing_service.entity.BillingCycle;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.BillingCycleRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.service.impl.BillingCycleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing Cycle Service Tests")
class BillingCycleServiceTest {

    @Mock private BillingCycleRepository billingCycleRepository;
    @Mock private InvoiceRepository invoiceRepository;

    @InjectMocks
    private BillingCycleServiceImpl billingCycleService;

    private BillingCycleRequest validRequest;
    private BillingCycle savedCycle;

    @BeforeEach
    void setUp() {
        validRequest = new BillingCycleRequest();
        validRequest.setAccountId(1001L);
        validRequest.setCycleStart(LocalDate.of(2026, 5, 1));
        validRequest.setCycleEnd(LocalDate.of(2026, 5, 31));

        savedCycle = new BillingCycle();
        savedCycle.setCycleId(1L);
        savedCycle.setAccountId(1001L);
        savedCycle.setCycleStart(LocalDate.of(2026, 5, 1));
        savedCycle.setCycleEnd(LocalDate.of(2026, 5, 31));
        savedCycle.setStatus(BillingCycleStatus.OPEN);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. Create Billing Cycle
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should create billing cycle successfully with OPEN status")
    void createBillingCycle_success() {
        // Arrange
        when(billingCycleRepository.findByAccountIdAndStatus(1001L, BillingCycleStatus.OPEN))
                .thenReturn(Optional.empty());
        when(billingCycleRepository.save(any(BillingCycle.class))).thenReturn(savedCycle);

        // Act
        BillingCycleResponse response = billingCycleService.createBillingCycle(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(BillingCycleStatus.OPEN, response.getStatus());
        assertEquals(1001L, response.getAccountId());
        verify(billingCycleRepository).save(any(BillingCycle.class));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. Duplicate Billing Cycle Prevention
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should throw BillingException when an OPEN cycle already exists for the account")
    void createBillingCycle_duplicateOpenCycle_throwsBillingException() {
        // Arrange — an OPEN cycle already exists
        when(billingCycleRepository.findByAccountIdAndStatus(1001L, BillingCycleStatus.OPEN))
                .thenReturn(Optional.of(savedCycle));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> billingCycleService.createBillingCycle(validRequest));

        assertTrue(ex.getMessage().contains("open billing cycle already exists"));
        // Make sure save was NEVER called
        verify(billingCycleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should allow creating a new cycle when previous cycle is CLOSED")
    void createBillingCycle_previousCycleClosed_success() {
        // Arrange — no OPEN cycle exists (previous is CLOSED)
        when(billingCycleRepository.findByAccountIdAndStatus(1001L, BillingCycleStatus.OPEN))
                .thenReturn(Optional.empty());
        when(billingCycleRepository.save(any(BillingCycle.class))).thenReturn(savedCycle);

        // Act
        BillingCycleResponse response = billingCycleService.createBillingCycle(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(BillingCycleStatus.OPEN, response.getStatus());
    }

    @Test
    @DisplayName("Should throw BillingException when cycleEnd is before cycleStart")
    void createBillingCycle_endBeforeStart_throwsBillingException() {
        // Arrange — invalid date range
        validRequest.setCycleStart(LocalDate.of(2026, 5, 31));
        validRequest.setCycleEnd(LocalDate.of(2026, 5, 1)); // end before start

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> billingCycleService.createBillingCycle(validRequest));
        assertEquals("Cycle end date must be after cycle start date", ex.getMessage());
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. Close Billing Cycle
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should close an OPEN billing cycle successfully")
    void closeBillingCycle_success() {
        // Arrange
        when(billingCycleRepository.findById(1L)).thenReturn(Optional.of(savedCycle));
        when(billingCycleRepository.save(any(BillingCycle.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        BillingCycleResponse response = billingCycleService.closeBillingCycle(1L);

        // Assert
        assertEquals(BillingCycleStatus.CLOSED, response.getStatus());
    }

    @Test
    @DisplayName("Should throw BillingException when closing an already CLOSED cycle")
    void closeBillingCycle_alreadyClosed_throwsBillingException() {
        // Arrange
        savedCycle.setStatus(BillingCycleStatus.CLOSED);
        when(billingCycleRepository.findById(1L)).thenReturn(Optional.of(savedCycle));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> billingCycleService.closeBillingCycle(1L));
        assertEquals("Billing cycle is already closed", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when cycle ID does not exist")
    void closeBillingCycle_notFound_throwsResourceNotFoundException() {
        // Arrange
        when(billingCycleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> billingCycleService.closeBillingCycle(999L));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. Invalid Status Transition
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should update cycle status to GENERATED and set generatedDate")
    void updateCycleStatus_toGenerated_setsGeneratedDate() {
        // Arrange
        when(billingCycleRepository.findById(1L)).thenReturn(Optional.of(savedCycle));
        when(billingCycleRepository.save(any(BillingCycle.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        BillingCycleResponse response = billingCycleService.updateCycleStatus(1L, BillingCycleStatus.GENERATED);

        // Assert
        assertEquals(BillingCycleStatus.GENERATED, response.getStatus());
        assertNotNull(response.getGeneratedDate());
    }

    @Test
    @DisplayName("Should get billing cycle by ID successfully")
    void getBillingCycleById_success() {
        when(billingCycleRepository.findById(1L)).thenReturn(Optional.of(savedCycle));
        BillingCycleResponse response = billingCycleService.getBillingCycleById(1L);
        assertNotNull(response);
        assertEquals(1L, response.getCycleId());
    }
}
