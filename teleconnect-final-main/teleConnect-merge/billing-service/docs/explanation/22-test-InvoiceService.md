# Test: InvoiceServiceTest

`InvoiceServiceTest` is the largest unit-test class in the billing microservice. It exercises `InvoiceServiceImpl` — the Service-layer component that sits between the REST Controllers and the Spring Data JPA Repositories — completely in isolation. Because it is a *unit* test, the three repository collaborators (`InvoiceRepository`, `BillingCycleRepository`, `PaymentRepository`) are replaced by Mockito mocks, so no real database, Controller, or HTTP traffic is involved. The tests assert the service's business rules: total calculation, duplicate prevention, billing-cycle state transitions, payment validation, late-fee apply/waive, and read operations — i.e. the logic that, in production, would run between Controller -> Service -> Repository -> Entity/DB.

## src/test/java/com/teleconnect/billing_service/service/InvoiceServiceTest.java

This file is the JUnit 5 + Mockito unit-test suite for `InvoiceServiceImpl`. Each `@Test` arranges mocked repository behavior, calls a service method, and asserts the returned `InvoiceResponse` or the exception that should be thrown.

```java
// L1
package com.teleconnect.billing_service.service;
```

The `package` declaration places this test in the same package (`...service`) as the interface it indirectly tests. Test sources live under `src/test/java`, but Maven compiles them onto the same logical package namespace as `src/main/java`, so a test class in this package can reach package-private members of production classes in `com.teleconnect.billing_service.service` if needed. The class under test is `InvoiceServiceImpl`, which lives in the `...service.impl` sub-package and is imported explicitly below.

```java
// L3-L18
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
```

These imports bring in every domain type the tests manipulate:

- **DTO request types** (`InvoiceGenerationRequest`, `LateFeeRequest`, `LateFeeWaiverRequest`, `PaymentRequest`) — plain data carriers a Controller would normally populate from an incoming JSON body and pass to the service. Here the tests build them by hand.
- **`InvoiceResponse`** — the DTO the service returns (so the test never inspects entities directly; it asserts on the response projection produced by the service's `toResponse(...)` mapper).
- **Entities** `BillingCycle` and `Invoice` — the JPA-mapped persistence objects. The tests instantiate them as ordinary POJOs (no database backing them).
- **Enums** `BillingCycleStatus` (OPEN/GENERATED/CLOSED…), `InvoiceStatus` (GENERATED/SENT/PAID/OVERDUE/DISPUTED…), and `PaymentMethod` (UPI/CARD/…) — the controlled vocabularies the business rules switch on.
- **Exceptions** `BillingException` (a business-rule violation, e.g. paying a closed invoice) and `ResourceNotFoundException` (a lookup miss) — the two outcomes the negative tests assert.
- **Repository interfaces** `BillingCycleRepository`, `InvoiceRepository`, `PaymentRepository` — the Spring Data JPA collaborators. They are imported only so they can be declared as `@Mock` fields; their real implementations are never used.
- **`InvoiceServiceImpl`** — the concrete class under test, instantiated by Mockito via `@InjectMocks`.

```java
// L19-L25
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
```

The JUnit 5 (Jupiter) and Mockito infrastructure:

- **`@BeforeEach`** — marks a method that JUnit runs *before every single `@Test`*, giving each test a freshly initialized fixture (here `setUp()`).
- **`@DisplayName`** — supplies a human-readable label for a class or test, shown in test reports instead of the raw method name.
- **`@Test`** — marks a method as a test case JUnit should execute.
- **`@ExtendWith`** — registers a JUnit 5 *extension* (a plug-in that hooks into the test lifecycle).
- **`@InjectMocks` / `@Mock`** — Mockito annotations explained at their declarations below.
- **`MockitoExtension`** — the Mockito JUnit-5 extension that, before each test, creates the `@Mock` objects and injects them into the `@InjectMocks` target, then (in strict mode) verifies there are no unused stubbings.

```java
// L27-L30
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
```

Standard JDK types used throughout. **`BigDecimal`** is used for all monetary amounts (exact decimal arithmetic, never `double`, to avoid rounding errors in money). **`LocalDate`** is a date without time, used for cycle start/end and due dates. **`List`** is used for the "all invoices for an account" assertion. **`Optional<T>`** is a container that either holds a value or is empty; Spring Data `findBy...` methods return `Optional`, and the tests stub them to return `Optional.of(...)` (found) or `Optional.empty()` (not found).

```java
// L32-L34
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
```

Three `static` imports so the assertion/stubbing helpers can be called unqualified:

- **`Assertions.*`** — JUnit assertions: `assertEquals`, `assertThrows`, `assertTrue`, `assertNotNull`.
- **`ArgumentMatchers.any`** — a Mockito matcher meaning "any argument of this type"; used in stubs like `save(any(Invoice.class))`.
- **`Mockito.*`** — the core stubbing/verification API: `when(...).thenReturn(...)`, `.thenAnswer(...)`, and `verify(...)`.

```java
// L36-L38
@ExtendWith(MockitoExtension.class)
@DisplayName("Invoice Service Tests")
class InvoiceServiceTest {
```

`@ExtendWith(MockitoExtension.class)` wires Mockito into the JUnit 5 lifecycle: before each test it initializes all `@Mock` fields and injects them into the `@InjectMocks` field, removing the need to call `MockitoAnnotations.openMocks(this)` manually. `@DisplayName("Invoice Service Tests")` gives the whole class a readable report name. The class is package-private (no `public`), which is the idiomatic visibility for JUnit 5 test classes.

```java
// L40-L43
    // ── Mocks ────────────────────────────────────────────────────────────────
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private BillingCycleRepository billingCycleRepository;
    @Mock private PaymentRepository paymentRepository;
```

`@Mock` tells Mockito to create a fake implementation of each repository interface. A mock returns benign defaults (e.g. `null`, empty `Optional`, empty `List`) for any method until the test stubs a specific behavior with `when(...)`. These three mocks are the service's collaborators:

- `invoiceRepository` — invoice CRUD and finder queries.
- `billingCycleRepository` — billing-cycle lookups and saves.
- `paymentRepository` — payment persistence and duplicate-transaction lookups.

Because they are mocks, no SQL ever runs; the tests fully control what each repository "returns."

```java
// L45-L46
    @InjectMocks
    private InvoiceServiceImpl invoiceService;
```

`@InjectMocks` makes Mockito instantiate the *real* `InvoiceServiceImpl` and inject the three `@Mock` fields into it. `InvoiceServiceImpl` uses field injection (`@Autowired` on its private repository fields), so Mockito sets those fields by type-matching. The result is a genuine service object whose only fakes are its repositories — exactly what a unit test wants. `invoiceService` is the System Under Test (SUT).

```java
// L48-L51
    // ── Common test data ─────────────────────────────────────────────────────
    private BillingCycle openCycle;
    private Invoice unpaidInvoice;
    private InvoiceGenerationRequest generationRequest;
```

Three shared fixture fields, re-created before each test in `setUp()`:

- `openCycle` — a billing cycle in `OPEN` status, the happy-path precondition for generating an invoice.
- `unpaidInvoice` — a `GENERATED`, unpaid invoice totalling 949.32, reused across payment/late-fee/read tests.
- `generationRequest` — a populated invoice-generation request matching `openCycle`.

```java
// L53-L54
    @BeforeEach
    void setUp() {
```

`setUp()` runs before each `@Test`. Re-building the fixtures every time guarantees test isolation: one test mutating `openCycle.setStatus(CLOSED)` or `unpaidInvoice.setStatus(PAID)` cannot leak into the next test.

```java
// L55-L61
        // Open billing cycle
        openCycle = new BillingCycle();
        openCycle.setCycleId(1L);
        openCycle.setAccountId(1001L);
        openCycle.setCycleStart(LocalDate.of(2026, 5, 1));
        openCycle.setCycleEnd(LocalDate.of(2026, 5, 31));
        openCycle.setStatus(BillingCycleStatus.OPEN);
```

Builds an `OPEN` billing cycle with id `1`, account `1001`, and a May 2026 window. The `cycleEnd` of 2026-05-31 is significant: the service computes an invoice's due date as `cycleEnd.plusDays(15)` (see `InvoiceServiceImpl` L84), which lands on 2026-06-15 — the value the generation tests assert. `1L`/`1001L` are `long` literals (the `L` suffix) matching the entities' `Long` id fields.

```java
// L63-L70
        // Invoice generation request
        generationRequest = new InvoiceGenerationRequest();
        generationRequest.setCycleId(1L);
        generationRequest.setAccountId(1001L);
        generationRequest.setPlanCharges(new BigDecimal("800.00"));
        generationRequest.setExcessCharges(new BigDecimal("75.00"));
        generationRequest.setAddOnCharges(new BigDecimal("25.00"));
        generationRequest.setTaxes(new BigDecimal("49.32"));
```

Populates the generation request so it points at `openCycle` (same `cycleId`/`accountId`) and carries the four charge components. Note `BigDecimal` is constructed from `String` literals (`"800.00"`), not `double`, so the scale and value are exact. The four components sum to **800.00 + 75.00 + 25.00 + 49.32 = 949.32**, the expected total.

```java
// L72-L86
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
```

Builds a fully populated, consistent `Invoice`: total 949.32, nothing paid (`paidAmount = 0`), no late fee (`lateFee = 0`), due 2026-06-15, status `GENERATED`. `BigDecimal.ZERO` is the constant for zero. This object is the standard subject for the payment, late-fee, and read tests; individual tests mutate just the one field they need (e.g. status -> `PAID`, `OVERDUE`, or `DISPUTED`). The closing `}` at L86 ends `setUp()`.

---

### Section 1 — Invoice Generation: Total Calculation

```java
// L92-L94
    @Test
    @DisplayName("Should generate invoice with correct total = planCharges + excessCharges + addOnCharges + taxes")
    void generateInvoice_correctTotalCalculation() {
```

A happy-path test verifying that `generateInvoice` sums the four charge components into `totalAmount`. The `@DisplayName` documents the business rule. The method name follows the `methodUnderTest_scenario` convention.

```java
// L95-L103
        // Arrange
        when(billingCycleRepository.findById(1L)).thenReturn(Optional.of(openCycle));
        when(invoiceRepository.findByAccountIdAndCycleId(1001L, 1L)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice saved = inv.getArgument(0);
            saved.setInvoiceId(1L);
            return saved;
        });
        when(billingCycleRepository.save(any(BillingCycle.class))).thenReturn(openCycle);
```

This is the canonical stubbing block reused by most generation tests. Reading line by line:

- `findById(1L)` returns the open cycle, so the service finds a valid, non-closed cycle (clears the `ResourceNotFoundException` and `CLOSED` checks at `InvoiceServiceImpl` L55–L61).
- `findByAccountIdAndCycleId(1001L, 1L)` returns `Optional.empty()`, meaning *no* invoice exists yet for that account+cycle — so the duplicate check at L63–L68 passes.
- `invoiceRepository.save(any(Invoice.class))` uses `.thenAnswer(...)` rather than `.thenReturn(...)` so the mock can act on the actual entity the service built: it grabs argument 0 (the `Invoice` the service constructed), stamps an `invoiceId` of `1L` (simulating the DB assigning a generated key), and returns that same object. This lets the test assert on the real, service-computed `totalAmount`, `status`, and `dueDate`.
- `billingCycleRepository.save(any(BillingCycle.class))` returns `openCycle` (the service ignores the return value here but the call must succeed).

```java
// L105-L106
        // Act
        InvoiceResponse response = invoiceService.generateInvoice(generationRequest);
```

Invokes the SUT. Internally `generateInvoice` looks up the cycle, validates it, checks for duplicates, computes the total, builds the `Invoice` (due date = cycleEnd + 15 days, status `GENERATED`), flips the cycle to `GENERATED`, saves both, and returns the mapped `InvoiceResponse`.

```java
// L108-L119
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
```

Computes the expected total (949.32) and asserts three things:

- **Total** — uses `assertEquals(0, expectedTotal.compareTo(response.getTotalAmount()))`. Comparing via `compareTo == 0` (rather than `assertEquals(expectedTotal, actual)`) checks *numeric* equality while ignoring scale; this matters because the service applies `.setScale(2, HALF_UP)` to the total, and `949.32` vs `949.320` are `.equals`-unequal but `.compareTo`-equal. This is the correct idiom for `BigDecimal` money comparisons.
- **Status** — the returned invoice is `GENERATED`.
- **Due date** — 2026-06-15, confirming the `cycleEnd (2026-05-31) + 15 days` rule.

The closing `}` ends the test.

```java
// L121-L143
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
```

A boundary variant: it overrides the fixture so `excessCharges` and `addOnCharges` are zero (a subscriber with no overage and no add-ons). The same four stubs are set up, the service generates the invoice, and the test asserts the total collapses to `800.00 + 49.32 = 849.32`. This confirms the summation formula handles zero components correctly (zero contributes nothing) rather than, say, treating null/zero specially.

```java
// L145-L164
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
```

This test verifies the *side effect* on the billing cycle (the state machine OPEN -> GENERATED). After generation, it asserts `openCycle.getStatus()` is now `GENERATED` — possible because the mock handed the service the *same* `openCycle` instance, which the service mutated in place at `InvoiceServiceImpl` L88. `verify(billingCycleRepository).save(openCycle)` is an *interaction* assertion: it fails unless the service called `save` exactly once with that exact cycle object, proving the transition is persisted, not just done in memory. The return value of `generateInvoice` is discarded since only the side effect matters here.

```java
// L166-L177
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
```

A negative test for the business rule "you cannot bill a closed cycle." It flips the cycle to `CLOSED` and stubs only `findById` (the later `findByAccountIdAndCycleId`/`save` stubs are deliberately omitted, because the service should throw before reaching them). `assertThrows` runs the lambda and verifies a `BillingException` is thrown, returning the caught exception; the test then asserts the exact message matches `InvoiceServiceImpl` L60. Because Mockito strict-stubbing is in effect, omitting the unused stubs is required — adding them would trigger an `UnnecessaryStubbingException`.

```java
// L179-L189
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
```

Verifies the lookup-miss path. The request's `cycleId` is changed to `99`, and `findById(99L)` is stubbed to return `Optional.empty()`. Inside the service, the `Optional.orElseThrow(...)` at `InvoiceServiceImpl` L55–L57 fires, producing a `ResourceNotFoundException`. The test only checks the exception *type* (not the message). The stub must use `99L` to match the changed `cycleId`; if it used `1L`, the mock would return its default empty `Optional` anyway, but the explicit stub documents intent.

```java
// L191-L203
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
```

Verifies duplicate prevention: one account may have only one invoice per cycle. The cycle is found and `OPEN`, but `findByAccountIdAndCycleId` now returns `Optional.of(unpaidInvoice)` — an existing invoice. The service's `.ifPresent(inv -> { throw ... })` at `InvoiceServiceImpl` L63–L68 fires a `BillingException`. The test uses `assertTrue(ex.getMessage().contains("Invoice already exists"))` (a substring check) because the full message includes the dynamic account and cycle ids, so a substring match is more robust than an exact-equals.

---

### Section 2 — Excess Charge Computation

```java
// L209-L233
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
```

A heavy-overage scenario (e.g. a customer who blew past their data allowance). The request is reconfigured with a large `excessCharges` of 300.00 and recomputed components summing to `500 + 300 + 50 + 76.50 = 926.50`. After generating, the test asserts two things via `compareTo`: the total is 926.50 (so excess is genuinely folded into the total, not dropped) and the response's `excessCharges` field still echoes 300.00 (so the component is preserved on the invoice for the line-item breakdown). The stubbing block is identical to the happy-path generation tests.

---

### Section 3 — Payment Tests

```java
// L239-L259
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
```

The happy-path payment. A `PaymentRequest` is built paying the exact total (949.32) by UPI with a unique transaction reference `TXN001`. The stubs: the invoice is found (`GENERATED`, unpaid), no payment exists with that reference (`Optional.empty()` -> duplicate check passes), and both `paymentRepository.save` and `invoiceRepository.save` echo back their argument. `payInvoice(1L, paymentRequest)` (a thin wrapper that sets `invoiceId` on the request and delegates to `processPayment`, per `InvoiceServiceImpl` L170–L173) runs the validations, persists a `SUCCESS` `Payment`, sets the invoice to `PAID` with `paidAmount = 949.32`, and returns the mapped response. The test asserts status `PAID` and `paidAmount` 949.32.

```java
// L261-L276
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
```

A double-payment guard. The fixture invoice is flipped to `PAID`. Only `findById` is stubbed, since the service throws on the very first status check (`InvoiceServiceImpl` L134–L136) before touching the payment repository. The test asserts a `BillingException` with the exact message "Invoice is already paid." This prevents charging a customer twice for one invoice.

```java
// L278-L292
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
```

Enforces the "no partial payments" rule. The invoice (total 949.32, still `GENERATED`) is found, but the request pays only 500.00. The service's `amountPaid.compareTo(totalAmount) < 0` check (`InvoiceServiceImpl` L140–L144) throws a `BillingException`. The test uses a substring match because the real message embeds the actual amounts. Stubbing only `findById` again reflects that the throw happens before any payment lookup/save.

```java
// L294-L311
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
```

Idempotency / double-spend protection on the payment gateway reference. The request pays the full amount but reuses transaction reference `TXN_DUPLICATE`. The invoice is found and *valid* this time, so the service proceeds past the status/amount checks to the duplicate-reference lookup at `InvoiceServiceImpl` L146–L152; `findByTransactionRef` returns an existing `Payment` (constructed inline with its fully-qualified class name since `Payment` is not imported in the test), so `.ifPresent(...)` throws. The test confirms a `BillingException` mentioning "Duplicate transaction reference." This is why this test, unlike the earlier negative payment tests, must also stub `findByTransactionRef`.

```java
// L313-L328
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
```

Verifies that a `DISPUTED` invoice cannot be paid until the dispute is resolved. The fixture is set to `DISPUTED`, `findById` is stubbed, and the service throws at the dispute check (`InvoiceServiceImpl` L137–L139) with message "Cannot process payment for a disputed invoice. Resolve the dispute first." The test asserts the message contains the substring "disputed invoice."

---

### Section 4 — Late Fee Tests

```java
// L334-L354
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
```

Happy-path late-fee application. The invoice is forced to `OVERDUE` with a starting `lateFee` of 0, and a `LateFeeRequest` of 100.00 with a reason is built. The invoice is found and `save` echoes it back. The service (`InvoiceServiceImpl` L177–L188) confirms the `OVERDUE` status, rounds the fee to 2 decimals, *adds* it to both `lateFee` (0 -> 100.00) and `totalAmount` (949.32 -> 1049.32), and saves. The test asserts the new total 1049.32 and `lateFee` 100.00. Adding to the *existing* `lateFee` rather than replacing it means repeated calls accumulate fees.

```java
// L356-L369
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
```

The negative counterpart: the fixture stays `GENERATED` (not overdue). The service's status guard (`InvoiceServiceImpl` L180–L182) throws a `BillingException` whose exact message is asserted. Only `findById` is stubbed because the throw precedes any `save`. This enforces that fees attach only to genuinely overdue invoices, not invoices still within their grace period.

```java
// L371-L391
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
```

The reverse operation: waiving a fee that was previously applied. The fixture is set up as if a 100.00 fee had already been added (`lateFee = 100.00`, `totalAmount = 1049.32`). A `LateFeeWaiverRequest` carries an audit trail (reason "Goodwill gesture", authorised by "admin-501"). The service (`InvoiceServiceImpl` L193–L203) confirms there *is* a fee, subtracts the current `lateFee` from `totalAmount` (1049.32 -> 949.32), zeroes `lateFee`, and saves. The test asserts the total reverts to 949.32 and the fee is back to zero — symmetric with the apply test.

```java
// L393-L405
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
```

The negative waiver case. The fixture invoice's `lateFee` is still `BigDecimal.ZERO` (from `setUp()`), so there is nothing to waive. The service's guard at `InvoiceServiceImpl` L196–L198 (`lateFee == null || lateFee.compareTo(ZERO) == 0`) throws a `BillingException`. The test checks only the exception type, not the message. This stops bogus waivers from incorrectly reducing a total below its true value.

---

### Section 5 — Get Invoices

```java
// L411-L418
    @Test
    @DisplayName("Should return invoice by ID")
    void getInvoiceById_success() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(unpaidInvoice));
        InvoiceResponse response = invoiceService.getInvoiceById(1L);
        assertNotNull(response);
        assertEquals(1L, response.getInvoiceId());
    }
```

A read happy-path. `findById(1L)` returns the fixture invoice; `getInvoiceById` (`InvoiceServiceImpl` L96–L98, via the private `findById` helper and `toResponse`) maps it to a response. The test asserts the response is non-null and carries `invoiceId` 1. `assertEquals(1L, response.getInvoiceId())` compares a `long` literal against the `Long` getter, which auto-unboxes for the comparison.

```java
// L420-L426
    @Test
    @DisplayName("Should throw ResourceNotFoundException when invoice not found")
    void getInvoiceById_notFound_throwsException() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.getInvoiceById(99L));
    }
```

The read miss path. `findById(99L)` returns empty, so the service's private `findById` helper throws `ResourceNotFoundException` (`InvoiceServiceImpl` L490–L494). The test asserts that exception type. This is the behavior a Controller would translate into an HTTP 404.

```java
// L428-L438
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
```

Tests the account-level list query (the single-argument `getInvoicesByAccount`, `InvoiceServiceImpl` L101–L104). `findByAccountId(1001L)` returns a one-element list (`List.of(...)` builds an immutable list), the service maps each entity through `toResponse` via a stream, and the test asserts the result has one element whose `accountId` is 1001. The final `}` at L439 closes the class.

---

## How this connects

`InvoiceServiceTest` validates the Service layer (`InvoiceServiceImpl`) — the place where the application's billing rules actually live — without any database or web layer attached:

- **Upward (toward Controllers):** every method tested here (`generateInvoice`, `payInvoice`, `applyLateFee`, `waiveLateFee`, `getInvoiceById`, `getInvoicesByAccount`) is the same method an Invoice REST Controller calls. The DTOs the tests build by hand (`InvoiceGenerationRequest`, `PaymentRequest`, `LateFeeRequest`, `LateFeeWaiverRequest`) are exactly what a Controller would deserialize from JSON, and the `InvoiceResponse` asserted here is what the Controller would serialize back. The exception types (`BillingException`, `ResourceNotFoundException`) are what a global exception handler maps to HTTP 400/404.
- **Downward (toward Repositories/DB):** the `@Mock` repositories stand in for the real Spring Data JPA `InvoiceRepository`, `BillingCycleRepository`, and `PaymentRepository`. By stubbing their `findBy...`/`save` methods, the tests assert the service's logic and its *interactions* with persistence (e.g. `verify(billingCycleRepository).save(openCycle)`) while leaving actual SQL, the `Invoice`/`BillingCycle`/`Payment` entity mappings, and the database to separate integration or repository tests.
- **State machines verified:** OPEN -> GENERATED (billing cycle), GENERATED/OVERDUE/DISPUTED/PAID transitions (invoice), and the money invariants (total = sum of components; late fee add/subtract symmetry) — all the rules a developer must trust when reading the production service.
