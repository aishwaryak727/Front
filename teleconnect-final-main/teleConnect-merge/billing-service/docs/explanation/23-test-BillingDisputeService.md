# Test: BillingDisputeServiceTest

This file is a **unit test class** for the dispute-handling business logic of the billing microservice. It exercises `BillingDisputeServiceImpl` (the Service layer) in complete isolation by replacing its two collaborators — `BillingDisputeRepository` and `InvoiceRepository` — with Mockito mocks, so no real database or Spring context is involved. In the Controller → Service → Repository → Entity/DB layering, this test sits *beside* the Service layer: it drives the service methods directly (skipping the controller) and stubs the repositories (skipping the real DB), verifying the dispute lifecycle rules — raising, reviewing, resolving/rejecting, and guarding invalid status transitions.

## src/test/java/com/teleconnect/billing_service/service/BillingDisputeServiceTest.java

The file's role: a pure unit-test suite that asserts the dispute service correctly enforces business rules (eligibility checks, amount limits, status transitions, required fields) and produces the right `DisputeResponse` and side effects, using mocked repositories.

---

### Package declaration

```java
// L1
package com.teleconnect.billing_service.service;
```

This places the test class in the `...service` package — the **same package** as the service interface it indirectly tests. Putting a unit test in the same package as its target is a common Java convention (it gives the test package-private visibility into classes in that package if ever needed) and it mirrors the `src/main/java/.../service` structure under `src/test/java`.

### Imports — DTOs, entities, enums, exceptions, repositories, and the service under test

```java
// L3-L15
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
```

These imports bring in every domain type the test touches:
- **`DisputeRequest`, `DisputeResolveRequest`, `DisputeReviewRequest`** — the input DTOs (Data Transfer Objects: plain carrier objects used to move request data into the service) for raising, resolving, and reviewing a dispute.
- **`DisputeResponse`** — the output DTO the service returns; the assertions read its getters.
- **`BillingDispute`, `Invoice`** — JPA entities (the persistent domain objects). The test builds in-memory instances to feed the mocks.
- **`DisputeStatus`, `InvoiceStatus`** — enums modeling the lifecycle states (e.g. `OPEN`, `UNDER_REVIEW`, `RESOLVED`, `REJECTED`; `SENT`, `PAID`, `DISPUTED`, `OVERDUE`).
- **`BillingException`, `ResourceNotFoundException`** — the custom exceptions the service throws for business-rule violations and missing records, respectively; the tests assert these are thrown.
- **`BillingDisputeRepository`, `InvoiceRepository`** — Spring Data JPA repository interfaces; here they are mocked rather than backed by a database.
- **`BillingDisputeServiceImpl`** — the concrete Service implementation under test (the System Under Test, or SUT).

### Imports — JUnit 5 and Mockito

```java
// L16-L22
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
```

These are the testing-framework imports:
- **`@BeforeEach`** (JUnit 5) — marks a method to run before *each* test, used here to rebuild fresh fixtures.
- **`@DisplayName`** (JUnit 5) — supplies a human-readable name for the class or a test method, shown in test reports.
- **`@Test`** (JUnit 5) — marks a method as a test case.
- **`@ExtendWith`** (JUnit 5) — registers an *extension* (a plugin) for the test lifecycle.
- **`@InjectMocks`** (Mockito) — tells Mockito to instantiate the SUT and inject the declared mocks into it.
- **`@Mock`** (Mockito) — declares a field to be replaced by a Mockito-generated stub/mock object.
- **`MockitoExtension`** (Mockito) — the JUnit 5 extension that wires Mockito annotations (`@Mock`, `@InjectMocks`) before each test and validates mock usage afterward.

### Imports — standard library and static helpers

```java
// L24-L30
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
```

- **`BigDecimal`** — exact decimal arithmetic for money (used for invoice totals and disputed amounts; never `double` for currency).
- **`LocalDate`** — calendar dates without time, used for due dates and raised dates.
- **`Optional<T>`** — a container that either holds a value or is empty; Spring Data's `findById` returns `Optional`, so the mocks return `Optional.of(...)` or `Optional.empty()`.
- **`static ...Assertions.*`** — JUnit 5 assertion methods (`assertEquals`, `assertNotNull`, `assertTrue`, `assertThrows`) imported statically so they can be called by bare name.
- **`static ...ArgumentMatchers.any`** — Mockito's `any(...)` matcher, used in stubbing to match any argument of a given type.
- **`static ...Mockito.*`** — Mockito's core static API (`when`, `verify`, etc.).

### Class declaration and class-level annotations

```java
// L32-L34
@ExtendWith(MockitoExtension.class)
@DisplayName("Billing Dispute Service Tests")
class BillingDisputeServiceTest {
```

`@ExtendWith(MockitoExtension.class)` activates Mockito for this class: before each test it creates the `@Mock` objects and injects them into the `@InjectMocks` field, and after each test it can flag unused stubs (strict stubbing). This replaces the older JUnit 4 `MockitoJUnitRunner`. `@DisplayName("Billing Dispute Service Tests")` gives the whole class a readable label in test output. The class is package-private (no `public`), which is fine for JUnit 5.

### Mock collaborators

```java
// L36-L37
    @Mock private BillingDisputeRepository disputeRepository;
    @Mock private InvoiceRepository invoiceRepository;
```

Two `@Mock` fields create fake implementations of the repositories. By default a Mockito mock returns "empty" values (e.g. `null`, empty `Optional`, empty list) until you stub specific calls with `when(...)`. These mocks let the test control exactly what the repositories "return" without a real database, and let it verify which repository methods were called.

### The system under test, with mocks injected

```java
// L39-L40
    @InjectMocks
    private BillingDisputeServiceImpl disputeService;
```

`@InjectMocks` makes Mockito construct a real `BillingDisputeServiceImpl` and inject the two `@Mock` repositories into it. The implementation uses field injection (`@Autowired` on its `disputeRepository` and `invoiceRepository` fields), so Mockito injects by matching field types. The result, `disputeService`, is the real service logic running against fake repositories.

### Reusable test fixtures (fields)

```java
// L42-L44
    private Invoice invoice;
    private BillingDispute openDispute;
    private DisputeRequest disputeRequest;
```

Three instance fields hold the shared fixtures rebuilt before every test:
- **`invoice`** — an `Invoice` entity used as the target of a dispute.
- **`openDispute`** — a `BillingDispute` entity in `OPEN` state, used by the review/resolve tests.
- **`disputeRequest`** — a `DisputeRequest` DTO representing the incoming "raise dispute" payload.

### `setUp()` — building fresh fixtures before each test

```java
// L46-L47
    @BeforeEach
    void setUp() {
```

`@BeforeEach` ensures this method runs before *every* `@Test`, giving each test an independent, pristine set of objects (no state leaks between tests).

```java
// L48-L56
        // Invoice in SENT status — eligible for dispute
        invoice = new Invoice();
        invoice.setInvoiceId(1L);
        invoice.setAccountId(1001L);
        invoice.setTotalAmount(new BigDecimal("949.32"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setLateFee(BigDecimal.ZERO);
        invoice.setDueDate(LocalDate.now().plusDays(5));
        invoice.setStatus(InvoiceStatus.SENT);
```

Constructs a fresh `Invoice` with id `1L`, owning account `1001L`, a total of `949.32`, nothing paid, no late fee, a due date five days in the future, and status `SENT`. `SENT` means the invoice has been issued but not yet paid — the state in which disputes are allowed. The future due date matters later: the service's `restoreInvoiceStatus` checks `dueDate.isBefore(today)` to decide between `OVERDUE` and `SENT`; with a future due date it would restore to `SENT`. `BigDecimal` literals are built from `String` (`"949.32"`) to avoid floating-point rounding errors.

```java
// L58-L63
        // Dispute request
        disputeRequest = new DisputeRequest();
        disputeRequest.setInvoiceId(1L);
        disputeRequest.setDisputeReason("ExcessData");
        disputeRequest.setDisputedAmount(new BigDecimal("173.60"));
        disputeRequest.setDescription("Charged for data I did not use");
```

Builds the incoming request DTO targeting invoice `1L`, with reason `"ExcessData"`, a disputed amount of `173.60` (well below the `949.32` total, so it passes the amount check), and a free-text description. Note that `subscriberId` is intentionally **not** set here, so it defaults to `null` — relevant to the "fall back to accountId" test.

```java
// L65-L74
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
```

Constructs a `BillingDispute` entity already in `OPEN` status (id `1L`, tied to invoice `1L` and subscriber `1001L`, reason `"ExcessData"`, amount `173.60`, raised today). This represents a dispute that already exists in the "database" so the review/resolve tests can have the mocked `disputeRepository.findById(1L)` return it. The closing brace ends `setUp()`.

---

## Section 1 — Dispute Creation

```java
// L76-L78
    // ════════════════════════════════════════════════════════════════════════
    // 1. Dispute Creation
    // ════════════════════════════════════════════════════════════════════════
```

A comment banner grouping the tests that exercise `raiseDispute(...)`.

### Test: raise dispute succeeds and marks invoice DISPUTED

```java
// L80-L82
    @Test
    @DisplayName("Should raise dispute successfully and mark invoice as DISPUTED")
    void raiseDispute_success_invoiceMarkedDisputed() {
```

A happy-path test for `raiseDispute`. The method name and `@DisplayName` describe the expected outcome: a dispute is created and the invoice flips to `DISPUTED`.

```java
// L83-L90
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(disputeRepository.save(any(BillingDispute.class))).thenAnswer(inv -> {
            BillingDispute d = inv.getArgument(0);
            d.setDisputeId(1L);
            return d;
        });
```

This is the **Arrange** (stubbing) phase using Mockito's `when(...).thenReturn(...)`/`.thenAnswer(...)` API:
- `invoiceRepository.findById(1L)` is stubbed to return the fixture `invoice` wrapped in `Optional.of(...)`, so the service finds the invoice.
- `invoiceRepository.save(any(Invoice.class))` is stubbed with `thenAnswer` to echo back its first argument (`inv.getArgument(0)`) — simulating a save that returns the saved entity unchanged. `any(Invoice.class)` matches any `Invoice` argument.
- `disputeRepository.save(any(BillingDispute.class))` is stubbed with `thenAnswer` to take the dispute being saved, assign it a generated id `1L` (mimicking the DB auto-assigning a primary key), and return it. This is important because the service maps the saved dispute into the response, and tests may assert on the id.

```java
// L92-L93
        // Act
        DisputeResponse response = disputeService.raiseDispute(disputeRequest);
```

The **Act** phase: call the real service method with the request DTO. Internally the service finds the invoice, runs the eligibility/amount checks, builds a `BillingDispute` with status `OPEN`, sets the invoice to `DISPUTED`, saves both via the mocks, and returns a `DisputeResponse`.

```java
// L95-L101
        // Assert
        assertNotNull(response);
        assertEquals(DisputeStatus.OPEN, response.getStatus());
        assertEquals(InvoiceStatus.DISPUTED, invoice.getStatus(),
                "Invoice status must be set to DISPUTED after raising dispute");
        verify(invoiceRepository).save(invoice);
    }
```

The **Assert** phase verifies four things: (1) a non-null response was returned; (2) the new dispute's status is `OPEN`; (3) the *side effect* on the invoice — its status was mutated to `DISPUTED` (the third argument is the failure message shown if the assertion fails); and (4) `verify(invoiceRepository).save(invoice)` confirms the service actually persisted the invoice exactly once with that same object reference. Together these confirm both the returned value and the persistence side effects.

### Test: subscriberId falls back to the invoice's accountId

```java
// L103-L105
    @Test
    @DisplayName("Should use invoice accountId as subscriberId when not provided in request")
    void raiseDispute_noSubscriberId_fallsBackToAccountId() {
```

Verifies the rule that when the request omits a `subscriberId`, the service uses the invoice's `accountId` instead (service lines L53–L55).

```java
// L106-L116
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
```

The request's `subscriberId` is explicitly set to `null`. The invoice lookup and saves are stubbed as before. Here `any()` (untyped) is used for the invoice save. The dispute-save stub assigns id `1L` **and explicitly sets** the dispute's `subscriberId` to `invoice.getAccountId()` (`1001L`).

*Aside: this stub forces the fallback value rather than letting the service's own fallback logic produce it. The real service already computes `subscriberId = request.getSubscriberId() != null ? ... : invoice.getAccountId()` and passes it into the builder, so the dispute would carry `1001L` anyway. The extra `d.setSubscriberId(...)` in the stub means the assertion would pass even if the production fallback were broken — the test does not isolate the production logic as tightly as its name implies.*

```java
// L118-L123
        // Act
        DisputeResponse response = disputeService.raiseDispute(disputeRequest);

        // Assert — subscriberId should be the invoice's accountId
        assertEquals(1001L, response.getSubscriberId());
    }
```

Calls `raiseDispute` and asserts the response's `subscriberId` equals `1001L` (the invoice's account id). Note `assertEquals(1001L, ...)`: the literal `1001L` is a primitive `long`; the response getter returns a `Long`, which unboxes for the comparison — so this checks numeric equality with the expected account id.

### Test: cannot dispute a PAID invoice

```java
// L125-L136
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
```

The invoice's status is changed to `PAID`. Only `findById` is stubbed — no save stubs are needed because the service should bail out before saving. `assertThrows(BillingException.class, () -> ...)` runs the lambda and asserts it throws a `BillingException`, returning the caught exception so the test can inspect it. The final `assertEquals` checks the exact message, confirming the service hits the `PAID` branch (service L41–L43). Because the exception is thrown before any save, the unstubbed `save` calls are never reached (important under Mockito's strict stubbing).

### Test: cannot dispute an already-DISPUTED invoice

```java
// L138-L149
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
```

Sets the invoice status to `DISPUTED` and asserts that raising another dispute throws `BillingException` with the exact message `"A dispute is already open for this invoice"`. This pins down the second guard in the service (L44–L46), which prevents duplicate concurrent disputes on one invoice.

### Test: disputed amount cannot exceed invoice total

```java
// L151-L162
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
```

Sets the requested disputed amount to `2000.00`, which is greater than the invoice total of `949.32`. The service compares with `BigDecimal.compareTo(...) > 0` (service L47) and throws. The assertion uses `assertTrue(ex.getMessage().contains("Disputed amount cannot exceed"))` — a *substring* check rather than exact match, because the real message appends the dynamic total (`"... of 949.32"`), which would make an exact-equals assertion brittle.

### Test: invoice not found

```java
// L164-L174
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
```

Stubs `invoiceRepository.findById(99L)` to return `Optional.empty()` (no such invoice) and points the request at invoice `99L`. The service's `findById(...).orElseThrow(...)` (service L37–L39) then throws `ResourceNotFoundException`. The test only asserts the exception *type*, not its message. The order here (stub first, then change the request id) is fine because the stub key `99L` matches the new request id; the service uses `request.getInvoiceId()` (now `99L`) to look up.

---

## Section 2 — Review Dispute

```java
// L176-L178
    // ════════════════════════════════════════════════════════════════════════
    // 2. Review Dispute
    // ════════════════════════════════════════════════════════════════════════
```

Banner grouping tests for `reviewDispute(...)`, which moves an `OPEN` dispute to `UNDER_REVIEW`.

### Test: review moves OPEN → UNDER_REVIEW and records reviewer/timestamp

```java
// L180-L189
    @Test
    @DisplayName("Should move OPEN dispute to UNDER_REVIEW with assignedTo and acknowledgedDate set")
    void reviewDispute_success() {
        // Arrange
        DisputeReviewRequest reviewRequest = new DisputeReviewRequest();
        reviewRequest.setAssignedTo("exec-201");
        reviewRequest.setNotes("Reviewing usage summary for May cycle");

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(openDispute));
        when(disputeRepository.save(any(BillingDispute.class))).thenAnswer(inv -> inv.getArgument(0));
```

Builds a `DisputeReviewRequest` assigning the dispute to agent `"exec-201"` with review notes. The dispute lookup is stubbed to return the `OPEN` fixture, and the save echoes its argument back. Since the fixture is `OPEN`, the service's guard (it must be `OPEN`) passes.

```java
// L191-L198
        // Act
        DisputeResponse response = disputeService.reviewDispute(1L, reviewRequest);

        // Assert
        assertEquals(DisputeStatus.UNDER_REVIEW, response.getStatus());
        assertEquals("exec-201", response.getAssignedTo());
        assertNotNull(response.getAcknowledgedDate());
    }
```

Calls `reviewDispute(1L, reviewRequest)` and asserts: the status became `UNDER_REVIEW`; the assignee was recorded as `"exec-201"`; and an `acknowledgedDate` timestamp was set (non-null). These map directly to the service's behavior at L136–L138 (set status, assignee, and `acknowledgedDate = LocalDateTime.now()`).

### Test: cannot review a non-OPEN dispute

```java
// L200-L214
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
```

Mutates the fixture to `UNDER_REVIEW` so it is *not* `OPEN`, then attempts a review. The service's guard `if (dispute.getStatus() != DisputeStatus.OPEN)` (service L131–L134) throws `BillingException`. The substring assertion checks the message starts with `"Only OPEN disputes can be moved to Under Review"` (the real message appends the current status). No save stub is provided because the method throws before saving.

---

## Section 3 — Resolve Dispute

```java
// L216-L218
    // ════════════════════════════════════════════════════════════════════════
    // 3. Resolve Dispute
    // ════════════════════════════════════════════════════════════════════════
```

Banner grouping tests for `resolveDispute(...)`, which closes a dispute as either `RESOLVED` or `REJECTED`.

### Test: resolve with RESOLVED status sets resolved amount and date

```java
// L220-L234
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
```

The fixture is put into `UNDER_REVIEW` (a legitimate state to resolve from). A `DisputeResolveRequest` requests resolution `"Resolved"` with a `creditAmount` of `173.60` and notes. Four stubs are set up because resolving also restores the invoice status: the dispute lookup and save, **plus** the invoice lookup and save used by the service's `restoreInvoiceStatus(...)` helper (service L171, L176–L184). The invoice has a future due date, so it would restore to `SENT`.

```java
// L236-L243
        // Act
        DisputeResponse response = disputeService.resolveDispute(1L, resolveRequest);

        // Assert
        assertEquals(DisputeStatus.RESOLVED, response.getStatus());
        assertEquals(0, new BigDecimal("173.60").compareTo(response.getResolvedAmount()));
        assertNotNull(response.getResolvedDate());
    }
```

Calls `resolveDispute` and asserts: status is `RESOLVED`; the `resolvedAmount` equals `173.60`; and a `resolvedDate` was stamped. Note the amount check uses `new BigDecimal("173.60").compareTo(response.getResolvedAmount())` and expects `0`. This is the correct way to compare `BigDecimal` values for *numeric* equality — `compareTo` returns `0` when values are equal regardless of scale (e.g. `173.60` vs `173.6`), whereas `equals` would consider scale and could fail. This is because `"Resolved"` matched (case-insensitively) so the service set `RESOLVED`, applied the credit amount (service L167–L169), and stamped `resolvedDate` (L164).

### Test: resolve with "Rejected" resolution yields REJECTED status

```java
// L245-L265
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
```

Same setup but with resolution `"Rejected"` and **with** resolution notes provided (rejection requires them). Because `"Resolved".equalsIgnoreCase("Rejected")` is `false`, the service chooses `REJECTED` (service L156–L157). The notes are present, so the mandatory-notes guard does not trip. The four stubs again cover the dispute and the invoice-restore path. The single assertion confirms the status is `REJECTED`. No credit amount is set on rejection (the service only applies the credit when resolved).

---

## Section 4 — Invalid Status Transition Handling

```java
// L267-L269
    // ════════════════════════════════════════════════════════════════════════
    // 4. Invalid Status Transition Handling
    // ════════════════════════════════════════════════════════════════════════
```

Banner grouping tests that confirm closed disputes cannot be re-resolved or re-updated.

### Test: cannot resolve an already-RESOLVED dispute

```java
// L271-L285
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
```

Marks the dispute `RESOLVED`, then tries to resolve it again. The service's closed-state guard `if (status == RESOLVED || status == REJECTED)` (service L152–L154) throws `BillingException` with a message containing `"already closed"`. Only `findById` is stubbed because the method short-circuits before any save. The substring assertion tolerates the appended current status in the message.

### Test: cannot resolve an already-REJECTED dispute

```java
// L287-L300
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
```

The mirror of the previous test for the `REJECTED` terminal state: resolving a rejected dispute must also throw `BillingException` (same guard at service L152). Here only the exception *type* is asserted, not the message.

### Test: rejecting without resolution notes is forbidden

```java
// L302-L318
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
```

The dispute is `UNDER_REVIEW` (so it passes the closed-state guard) and the request is `"Rejected"` with `resolutionNotes` explicitly `null`. The service's rule (service L159–L161) — `if (!isResolved && (notes == null || notes.isBlank()))` — requires notes when rejecting, so it throws `BillingException` with the exact message asserted here. This documents that a rejection must always carry a justification. Only `findById` is stubbed since the method throws before saving.

### Test: cannot update the status of a RESOLVED dispute

```java
// L320-L331
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
```

Targets the separate `updateDisputeStatus(disputeId, status)` method. The dispute is `RESOLVED`; attempting to update it (here back to `OPEN`) must throw, because the service forbids editing terminal-state disputes (service L112–L114). The substring assertion checks the message begins with `"Cannot update a dispute that is already"` (the real message appends the current status). Only `findById` is stubbed.

### Test: cannot update the status of a REJECTED dispute

```java
// L333-L344
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
```

The mirror for the `REJECTED` terminal state: updating a rejected dispute's status must also throw `BillingException` (same guard at service L112). Only the exception type is asserted. The final `}` closes the test class.

---

## How this connects

- **Service layer (what is tested):** Every test drives a method of `BillingDisputeServiceImpl` (`raiseDispute`, `reviewDispute`, `resolveDispute`, `updateDisputeStatus`), which lives at `src/main/java/com/teleconnect/billing_service/service/impl/BillingDisputeServiceImpl.java`. The tests pin down its business rules: dispute eligibility (`SENT`/not `PAID`/not `DISPUTED`), the disputed-amount ceiling, the `subscriberId` fallback, the `OPEN → UNDER_REVIEW → RESOLVED/REJECTED` lifecycle, mandatory rejection notes, and the prohibition on editing closed disputes.
- **Repository layer (mocked away):** `BillingDisputeRepository` and `InvoiceRepository` are `@Mock`ed, so this suite never touches a real database. It only specifies how those repositories *should* behave (`findById`, `save`) and, in the happy path, verifies the service calls `invoiceRepository.save(invoice)`.
- **Entity/DTO/enum layer:** The tests construct `Invoice` and `BillingDispute` entities and the `DisputeRequest`/`DisputeReviewRequest`/`DisputeResolveRequest` DTOs as fixtures, and read the `DisputeResponse` DTO plus the `DisputeStatus`/`InvoiceStatus` enums in assertions.
- **Controller layer (not involved):** The web/controller layer that would normally call this service is bypassed entirely — these are isolated unit tests, complementing (not replacing) any controller or integration tests elsewhere in the project.
