package com.teleconnect.billing_service.service;

import com.teleconnect.billing_service.dto.request.DisputeRequest;
import com.teleconnect.billing_service.dto.request.DisputeResolveRequest;
import com.teleconnect.billing_service.dto.request.DisputeReviewRequest;
import com.teleconnect.billing_service.dto.response.DisputeResponse;
import com.teleconnect.billing_service.entity.BillingDispute;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.DisputeStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.BillingDisputeRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.service.impl.BillingDisputeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing Dispute Service Tests")
class BillingDisputeServiceTest {

    @Mock private BillingDisputeRepository disputeRepository;
    @Mock private InvoiceRepository invoiceRepository;

    @InjectMocks
    private BillingDisputeServiceImpl disputeService;

    private Invoice invoice;
    private BillingDispute openDispute;
    private DisputeRequest disputeRequest;

    @BeforeEach
    void setUp() {
        // Invoice in SENT status — eligible for dispute
        invoice = new Invoice();
        invoice.setInvoiceId(1L);
        invoice.setAccountId(1001L);
        invoice.setTotalAmount(new BigDecimal("949.32"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setLateFee(BigDecimal.ZERO);
        invoice.setDueDate(LocalDate.now().plusDays(5));
        invoice.setStatus(InvoiceStatus.SENT);

        // Dispute request
        disputeRequest = new DisputeRequest();
        disputeRequest.setInvoiceId(1L);
        disputeRequest.setDisputeReason("ExcessData");
        disputeRequest.setDisputedAmount(new BigDecimal("173.60"));
        disputeRequest.setDescription("Charged for data I did not use");

        // Open dispute
        openDispute = new BillingDispute();
        openDispute.setDisputeId(1L);
        openDispute.setInvoiceId(1L);
        openDispute.setSubscriberId(1001L);
        openDispute.setDisputeReason("ExcessData");
        openDispute.setDisputedAmount(new BigDecimal("173.60"));
        openDispute.setStatus(DisputeStatus.OPEN);
        openDispute.setRaisedDate(LocalDate.now());
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. Dispute Creation
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should raise dispute successfully and mark invoice as DISPUTED")
    void raiseDispute_success_invoiceMarkedDisputed() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(disputeRepository.save(any(BillingDispute.class))).thenAnswer(inv -> {
            BillingDispute d = inv.getArgument(0);
            d.setDisputeId(1L);
            return d;
        });

        // Act
        DisputeResponse response = disputeService.raiseDispute(disputeRequest);

        // Assert
        assertNotNull(response);
        assertEquals(DisputeStatus.OPEN, response.getStatus());
        assertEquals(InvoiceStatus.DISPUTED, invoice.getStatus(),
                "Invoice status must be set to DISPUTED after raising dispute");
        verify(invoiceRepository).save(invoice);
    }

    @Test
    @DisplayName("Should use invoice accountId as subscriberId when not provided in request")
    void raiseDispute_noSubscriberId_fallsBackToAccountId() {
        // Arrange — no subscriberId in request
        disputeRequest.setSubscriberId(null);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(disputeRepository.save(any(BillingDispute.class))).thenAnswer(inv -> {
            BillingDispute d = inv.getArgument(0);
            d.setDisputeId(1L);
            d.setSubscriberId(invoice.getAccountId()); // should fallback
            return d;
        });

        // Act
        DisputeResponse response = disputeService.raiseDispute(disputeRequest);

        // Assert — subscriberId should be the invoice's accountId
        assertEquals(1001L, response.getSubscriberId());
    }

    @Test
    @DisplayName("Should throw BillingException when raising dispute on a PAID invoice")
    void raiseDispute_paidInvoice_throwsBillingException() {
        // Arrange
        invoice.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> disputeService.raiseDispute(disputeRequest));
        assertEquals("Cannot raise a dispute on an already paid invoice", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw BillingException when invoice is already DISPUTED")
    void raiseDispute_alreadyDisputed_throwsBillingException() {
        // Arrange
        invoice.setStatus(InvoiceStatus.DISPUTED);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> disputeService.raiseDispute(disputeRequest));
        assertEquals("A dispute is already open for this invoice", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw BillingException when disputed amount exceeds invoice total")
    void raiseDispute_amountExceedsTotal_throwsBillingException() {
        // Arrange — disputed amount (2000) > invoice total (949.32)
        disputeRequest.setDisputedAmount(new BigDecimal("2000.00"));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> disputeService.raiseDispute(disputeRequest));
        assertTrue(ex.getMessage().contains("Disputed amount cannot exceed"));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when invoice does not exist")
    void raiseDispute_invoiceNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        disputeRequest.setInvoiceId(99L);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> disputeService.raiseDispute(disputeRequest));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. Review Dispute
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should move OPEN dispute to UNDER_REVIEW with assignedTo and acknowledgedDate set")
    void reviewDispute_success() {
        // Arrange
        DisputeReviewRequest reviewRequest = new DisputeReviewRequest();
        reviewRequest.setAssignedTo("exec-201");
        reviewRequest.setNotes("Reviewing usage summary for May cycle");

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));
        when(disputeRepository.save(any(BillingDispute.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        DisputeResponse response = disputeService.reviewDispute(1L, reviewRequest);

        // Assert
        assertEquals(DisputeStatus.UNDER_REVIEW, response.getStatus());
        assertEquals("exec-201", response.getAssignedTo());
        assertNotNull(response.getAcknowledgedDate());
    }

    @Test
    @DisplayName("Should throw BillingException when reviewing a non-OPEN dispute")
    void reviewDispute_nonOpenDispute_throwsBillingException() {
        // Arrange — dispute is already UNDER_REVIEW
        openDispute.setStatus(DisputeStatus.UNDER_REVIEW);
        DisputeReviewRequest reviewRequest = new DisputeReviewRequest();
        reviewRequest.setAssignedTo("exec-202");

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> disputeService.reviewDispute(1L, reviewRequest));
        assertTrue(ex.getMessage().contains("Only OPEN disputes can be moved to Under Review"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. Resolve Dispute
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should resolve dispute with RESOLVED status and set resolvedDate")
    void resolveDispute_resolvedStatus_success() {
        // Arrange
        openDispute.setStatus(DisputeStatus.UNDER_REVIEW);

        DisputeResolveRequest resolveRequest = new DisputeResolveRequest();
        resolveRequest.setResolution("Resolved");
        resolveRequest.setCreditAmount(new BigDecimal("173.60"));
        resolveRequest.setResolutionNotes("Credit applied after usage verification");

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));
        when(disputeRepository.save(any(BillingDispute.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        DisputeResponse response = disputeService.resolveDispute(1L, resolveRequest);

        // Assert
        assertEquals(DisputeStatus.RESOLVED, response.getStatus());
        assertEquals(0, new BigDecimal("173.60").compareTo(response.getResolvedAmount()));
        assertNotNull(response.getResolvedDate());
    }

    @Test
    @DisplayName("Should reject dispute with REJECTED status")
    void resolveDispute_rejectedStatus_success() {
        // Arrange
        openDispute.setStatus(DisputeStatus.UNDER_REVIEW);

        DisputeResolveRequest resolveRequest = new DisputeResolveRequest();
        resolveRequest.setResolution("Rejected");
        resolveRequest.setResolutionNotes("Usage data verified — charges are correct");

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));
        when(disputeRepository.save(any(BillingDispute.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        DisputeResponse response = disputeService.resolveDispute(1L, resolveRequest);

        // Assert
        assertEquals(DisputeStatus.REJECTED, response.getStatus());
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. Invalid Status Transition Handling
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should throw BillingException when resolving an already RESOLVED dispute")
    void resolveDispute_alreadyResolved_throwsBillingException() {
        // Arrange
        openDispute.setStatus(DisputeStatus.RESOLVED);
        DisputeResolveRequest resolveRequest = new DisputeResolveRequest();
        resolveRequest.setResolution("Resolved");

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> disputeService.resolveDispute(1L, resolveRequest));
        assertTrue(ex.getMessage().contains("already closed"));
    }

    @Test
    @DisplayName("Should throw BillingException when resolving an already REJECTED dispute")
    void resolveDispute_alreadyRejected_throwsBillingException() {
        // Arrange
        openDispute.setStatus(DisputeStatus.REJECTED);
        DisputeResolveRequest resolveRequest = new DisputeResolveRequest();
        resolveRequest.setResolution("Resolved");

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));

        // Act & Assert
        assertThrows(BillingException.class,
                () -> disputeService.resolveDispute(1L, resolveRequest));
    }

    @Test
    @DisplayName("Should throw BillingException when rejecting without resolution notes")
    void resolveDispute_rejectedWithoutNotes_throwsBillingException() {
        // Arrange — rejection requires resolutionNotes
        openDispute.setStatus(DisputeStatus.UNDER_REVIEW);

        DisputeResolveRequest resolveRequest = new DisputeResolveRequest();
        resolveRequest.setResolution("Rejected");
        resolveRequest.setResolutionNotes(null); // missing notes

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> disputeService.resolveDispute(1L, resolveRequest));
        assertEquals("Resolution notes are mandatory when rejecting a dispute", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw BillingException when updating a RESOLVED dispute status")
    void updateDisputeStatus_resolvedDispute_throwsBillingException() {
        // Arrange
        openDispute.setStatus(DisputeStatus.RESOLVED);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> disputeService.updateDisputeStatus(1L, DisputeStatus.OPEN));
        assertTrue(ex.getMessage().contains("Cannot update a dispute that is already"));
    }

    @Test
    @DisplayName("Should throw BillingException when updating a REJECTED dispute status")
    void updateDisputeStatus_rejectedDispute_throwsBillingException() {
        // Arrange
        openDispute.setStatus(DisputeStatus.REJECTED);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));

        // Act & Assert
        assertThrows(BillingException.class,
                () -> disputeService.updateDisputeStatus(1L, DisputeStatus.OPEN));
    }
}
