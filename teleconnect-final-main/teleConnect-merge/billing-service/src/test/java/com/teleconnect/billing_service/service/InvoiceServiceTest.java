package com.teleconnect.billing_service.service;

import com.teleconnect.billing_service.dto.request.InvoiceGenerationRequest;
import com.teleconnect.billing_service.dto.request.LateFeeRequest;
import com.teleconnect.billing_service.dto.request.LateFeeWaiverRequest;
import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.InvoiceResponse;
import com.teleconnect.billing_service.entity.BillingCycle;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.enums.PaymentMethod;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.BillingCycleRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.repository.PaymentRepository;
import com.teleconnect.billing_service.service.impl.InvoiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Invoice Service Tests")
class InvoiceServiceTest {

    // ── Mocks ────────────────────────────────────────────────────────────────
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private BillingCycleRepository billingCycleRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    // ── Common test data ─────────────────────────────────────────────────────
    private BillingCycle openCycle;
    private Invoice unpaidInvoice;
    private InvoiceGenerationRequest generationRequest;

    @BeforeEach
    void setUp() {
        // Open billing cycle
        openCycle = new BillingCycle();
        openCycle.setCycleId(1L);
        openCycle.setAccountId(1001L);
        openCycle.setCycleStart(LocalDate.of(2026, 5, 1));
        openCycle.setCycleEnd(LocalDate.of(2026, 5, 31));
        openCycle.setStatus(BillingCycleStatus.OPEN);

        // Invoice generation request
        generationRequest = new InvoiceGenerationRequest();
        generationRequest.setCycleId(1L);
        generationRequest.setAccountId(1001L);
        generationRequest.setPlanCharges(new BigDecimal("800.00"));
        generationRequest.setExcessCharges(new BigDecimal("75.00"));
        generationRequest.setAddOnCharges(new BigDecimal("25.00"));
        generationRequest.setTaxes(new BigDecimal("49.32"));

        // Unpaid invoice
        unpaidInvoice = new Invoice();
        unpaidInvoice.setInvoiceId(1L);
        unpaidInvoice.setAccountId(1001L);
        unpaidInvoice.setCycleId(1L);
        unpaidInvoice.setPlanCharges(new BigDecimal("800.00"));
        unpaidInvoice.setExcessCharges(new BigDecimal("75.00"));
        unpaidInvoice.setAddOnCharges(new BigDecimal("25.00"));
        unpaidInvoice.setTaxes(new BigDecimal("49.32"));
        unpaidInvoice.setTotalAmount(new BigDecimal("949.32"));
        unpaidInvoice.setPaidAmount(BigDecimal.ZERO);
        unpaidInvoice.setLateFee(BigDecimal.ZERO);
        unpaidInvoice.setDueDate(LocalDate.of(2026, 6, 15));
        unpaidInvoice.setStatus(InvoiceStatus.GENERATED);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. Invoice Generation — Total Calculation
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should generate invoice with correct total = planCharges + excessCharges + addOnCharges + taxes")
    void generateInvoice_correctTotalCalculation() {
        // Arrange
        when(billingCycleRepository.findById(1L)).thenReturn(Optional.of(openCycle));
        when(invoiceRepository.findByAccountIdAndCycleId(1001L, 1L)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice saved = inv.getArgument(0);
            saved.setInvoiceId(1L);
            return saved;
        });
        when(billingCycleRepository.save(any(BillingCycle.class))).thenReturn(openCycle);

        // Act
        InvoiceResponse response = invoiceService.generateInvoice(generationRequest);

        // Assert
        BigDecimal expectedTotal = new BigDecimal("800.00")
                .add(new BigDecimal("75.00"))
                .add(new BigDecimal("25.00"))
                .add(new BigDecimal("49.32")); // = 949.32

        assertEquals(0, expectedTotal.compareTo(response.getTotalAmount()),
                "Total amount must equal sum of all charge components");
        assertEquals(InvoiceStatus.GENERATED, response.getStatus());
        assertEquals(LocalDate.of(2026, 6, 15), response.getDueDate(),
                "Due date must be cycleEnd + 15 days");
    }

    @Test
    @DisplayName("Should generate invoice with zero excess charges when no excess usage")
    void generateInvoice_zeroExcessCharges() {
        // Arrange
        generationRequest.setExcessCharges(BigDecimal.ZERO);
        generationRequest.setAddOnCharges(BigDecimal.ZERO);

        when(billingCycleRepository.findById(1L)).thenReturn(Optional.of(openCycle));
        when(invoiceRepository.findByAccountIdAndCycleId(1001L, 1L)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice saved = inv.getArgument(0);
            saved.setInvoiceId(1L);
            return saved;
        });
        when(billingCycleRepository.save(any(BillingCycle.class))).thenReturn(openCycle);

        // Act
        InvoiceResponse response = invoiceService.generateInvoice(generationRequest);

        // Assert — total = planCharges(800) + taxes(49.32) = 849.32
        BigDecimal expected = new BigDecimal("800.00").add(new BigDecimal("49.32"));
        assertEquals(0, expected.compareTo(response.getTotalAmount()));
    }

    @Test
    @DisplayName("Should set billing cycle status to GENERATED after invoice creation")
    void generateInvoice_updatesCycleStatusToGenerated() {
        // Arrange
        when(billingCycleRepository.findById(1L)).thenReturn(Optional.of(openCycle));
        when(invoiceRepository.findByAccountIdAndCycleId(1001L, 1L)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice saved = inv.getArgument(0);
            saved.setInvoiceId(1L);
            return saved;
        });
        when(billingCycleRepository.save(any(BillingCycle.class))).thenReturn(openCycle);

        // Act
        invoiceService.generateInvoice(generationRequest);

        // Assert — cycle status must be changed to GENERATED before saving
        assertEquals(BillingCycleStatus.GENERATED, openCycle.getStatus());
        verify(billingCycleRepository).save(openCycle);
    }

    @Test
    @DisplayName("Should throw BillingException when cycle is CLOSED")
    void generateInvoice_closedCycle_throwsBillingException() {
        // Arrange
        openCycle.setStatus(BillingCycleStatus.CLOSED);
        when(billingCycleRepository.findById(1L)).thenReturn(Optional.of(openCycle));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> invoiceService.generateInvoice(generationRequest));
        assertEquals("Cannot generate invoice for a closed billing cycle", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when cycle does not exist")
    void generateInvoice_cycleNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(billingCycleRepository.findById(99L)).thenReturn(Optional.empty());
        generationRequest.setCycleId(99L);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.generateInvoice(generationRequest));
    }

    @Test
    @DisplayName("Should throw BillingException when invoice already exists for the same account and cycle")
    void generateInvoice_duplicateInvoice_throwsBillingException() {
        // Arrange
        when(billingCycleRepository.findById(1L)).thenReturn(Optional.of(openCycle));
        when(invoiceRepository.findByAccountIdAndCycleId(1001L, 1L))
                .thenReturn(Optional.of(unpaidInvoice));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> invoiceService.generateInvoice(generationRequest));
        assertTrue(ex.getMessage().contains("Invoice already exists"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. Excess Charge Computation
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should correctly include excess charges in total amount")
    void generateInvoice_excessChargesIncludedInTotal() {
        // Arrange — high excess charges scenario
        generationRequest.setPlanCharges(new BigDecimal("500.00"));
        generationRequest.setExcessCharges(new BigDecimal("300.00")); // heavy excess data usage
        generationRequest.setAddOnCharges(new BigDecimal("50.00"));
        generationRequest.setTaxes(new BigDecimal("76.50"));

        when(billingCycleRepository.findById(1L)).thenReturn(Optional.of(openCycle));
        when(invoiceRepository.findByAccountIdAndCycleId(1001L, 1L)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice saved = inv.getArgument(0);
            saved.setInvoiceId(1L);
            return saved;
        });
        when(billingCycleRepository.save(any(BillingCycle.class))).thenReturn(openCycle);

        // Act
        InvoiceResponse response = invoiceService.generateInvoice(generationRequest);

        // Assert — 500 + 300 + 50 + 76.50 = 926.50
        assertEquals(0, new BigDecimal("926.50").compareTo(response.getTotalAmount()));
        assertEquals(0, new BigDecimal("300.00").compareTo(response.getExcessCharges()));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. Payment Tests
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should mark invoice as PAID after successful payment")
    void payInvoice_success_marksInvoiceAsPaid() {
        // Arrange
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmountPaid(new BigDecimal("949.32"));
        paymentRequest.setPaymentMethod(PaymentMethod.UPI);
        paymentRequest.setTransactionRef("TXN001");

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(unpaidInvoice));
        when(paymentRepository.findByTransactionRef("TXN001")).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        InvoiceResponse response = invoiceService.payInvoice(1L, paymentRequest);

        // Assert
        assertEquals(InvoiceStatus.PAID, response.getStatus());
        assertEquals(0, new BigDecimal("949.32").compareTo(response.getPaidAmount()));
    }

    @Test
    @DisplayName("Should throw BillingException when paying an already paid invoice")
    void payInvoice_alreadyPaid_throwsBillingException() {
        // Arrange
        unpaidInvoice.setStatus(InvoiceStatus.PAID);
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmountPaid(new BigDecimal("949.32"));
        paymentRequest.setPaymentMethod(PaymentMethod.UPI);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(unpaidInvoice));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> invoiceService.payInvoice(1L, paymentRequest));
        assertEquals("Invoice is already paid", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw BillingException when payment amount is less than invoice total")
    void payInvoice_insufficientAmount_throwsBillingException() {
        // Arrange
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmountPaid(new BigDecimal("500.00")); // less than 949.32
        paymentRequest.setPaymentMethod(PaymentMethod.CARD);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(unpaidInvoice));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> invoiceService.payInvoice(1L, paymentRequest));
        assertTrue(ex.getMessage().contains("less than the invoice total"));
    }

    @Test
    @DisplayName("Should throw BillingException for duplicate transaction reference")
    void payInvoice_duplicateTransactionRef_throwsBillingException() {
        // Arrange
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmountPaid(new BigDecimal("949.32"));
        paymentRequest.setPaymentMethod(PaymentMethod.UPI);
        paymentRequest.setTransactionRef("TXN_DUPLICATE");

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(unpaidInvoice));
        when(paymentRepository.findByTransactionRef("TXN_DUPLICATE"))
                .thenReturn(Optional.of(new com.teleconnect.billing_service.entity.Payment()));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> invoiceService.payInvoice(1L, paymentRequest));
        assertTrue(ex.getMessage().contains("Duplicate transaction reference"));
    }

    @Test
    @DisplayName("Should throw BillingException when paying a DISPUTED invoice")
    void payInvoice_disputedInvoice_throwsBillingException() {
        // Arrange
        unpaidInvoice.setStatus(InvoiceStatus.DISPUTED);
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmountPaid(new BigDecimal("949.32"));
        paymentRequest.setPaymentMethod(PaymentMethod.UPI);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(unpaidInvoice));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> invoiceService.payInvoice(1L, paymentRequest));
        assertTrue(ex.getMessage().contains("disputed invoice"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. Late Fee Tests
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should apply late fee only to OVERDUE invoices and add it to total")
    void applyLateFee_overdueInvoice_success() {
        // Arrange
        unpaidInvoice.setStatus(InvoiceStatus.OVERDUE);
        unpaidInvoice.setLateFee(BigDecimal.ZERO);

        LateFeeRequest lateFeeRequest = new LateFeeRequest();
        lateFeeRequest.setFeeAmount(new BigDecimal("100.00"));
        lateFeeRequest.setReason("Overdue past grace period");

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(unpaidInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        InvoiceResponse response = invoiceService.applyLateFee(1L, lateFeeRequest);

        // Assert — total = 949.32 + 100.00 = 1049.32, lateFee = 100.00
        assertEquals(0, new BigDecimal("1049.32").compareTo(response.getTotalAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getLateFee()));
    }

    @Test
    @DisplayName("Should throw BillingException when applying late fee to non-OVERDUE invoice")
    void applyLateFee_nonOverdueInvoice_throwsBillingException() {
        // Arrange — invoice is GENERATED, not OVERDUE
        LateFeeRequest lateFeeRequest = new LateFeeRequest();
        lateFeeRequest.setFeeAmount(new BigDecimal("100.00"));

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(unpaidInvoice));

        // Act & Assert
        BillingException ex = assertThrows(BillingException.class,
                () -> invoiceService.applyLateFee(1L, lateFeeRequest));
        assertEquals("Late fee can only be applied to OVERDUE invoices", ex.getMessage());
    }

    @Test
    @DisplayName("Should waive late fee and subtract it from total amount")
    void waiveLateFee_success() {
        // Arrange
        unpaidInvoice.setLateFee(new BigDecimal("100.00"));
        unpaidInvoice.setTotalAmount(new BigDecimal("1049.32")); // with late fee added

        LateFeeWaiverRequest waiverRequest = new LateFeeWaiverRequest();
        waiverRequest.setWaiverReason("Goodwill gesture");
        waiverRequest.setAuthorisedBy("admin-501");

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(unpaidInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        InvoiceResponse response = invoiceService.waiveLateFee(1L, waiverRequest);

        // Assert — total back to 949.32, lateFee = 0
        assertEquals(0, new BigDecimal("949.32").compareTo(response.getTotalAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getLateFee()));
    }

    @Test
    @DisplayName("Should throw BillingException when waiving late fee that is zero")
    void waiveLateFee_noLateFee_throwsBillingException() {
        // Arrange — invoice has no late fee
        LateFeeWaiverRequest waiverRequest = new LateFeeWaiverRequest();
        waiverRequest.setWaiverReason("Test");

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(unpaidInvoice));

        // Act & Assert
        assertThrows(BillingException.class,
                () -> invoiceService.waiveLateFee(1L, waiverRequest));
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. Get Invoices
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should return invoice by ID")
    void getInvoiceById_success() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(unpaidInvoice));
        InvoiceResponse response = invoiceService.getInvoiceById(1L);
        assertNotNull(response);
        assertEquals(1L, response.getInvoiceId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when invoice not found")
    void getInvoiceById_notFound_throwsException() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.getInvoiceById(99L));
    }

    @Test
    @DisplayName("Should return all invoices for an account")
    void getInvoicesByAccount_success() {
        when(invoiceRepository.findByAccountId(1001L))
                .thenReturn(List.of(unpaidInvoice));

        List<InvoiceResponse> result = invoiceService.getInvoicesByAccount(1001L);

        assertEquals(1, result.size());
        assertEquals(1001L, result.get(0).getAccountId());
    }
}
