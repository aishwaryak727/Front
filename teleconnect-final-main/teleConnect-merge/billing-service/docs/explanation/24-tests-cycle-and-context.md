# Tests: BillingCycleServiceTest & BillingServiceApplicationTests

This part documents the two test classes that protect the billing-cycle slice of the service. `BillingCycleServiceTest` is a pure **unit test** that exercises `BillingCycleServiceImpl` (the Service layer) in isolation — it replaces the Repository layer with Mockito mocks so no database, controller, or Spring context is involved, and verifies the cycle-creation / closing / status-update business rules. `BillingServiceApplicationTests` is the Spring Boot **smoke test** that boots the whole application context to prove every bean (controllers, services, repositories, entity mappings, configuration) wires together. In the Controller → Service → Repository → Entity/DB layering, the first file pins down the **Service** layer's logic with stubbed repositories, while the second confirms the *entire* stack of layers can be constructed at all.

---

## src/test/java/com/teleconnect/billing_service/service/BillingCycleServiceTest.java

Unit tests for `BillingCycleServiceImpl`. The real repositories (`BillingCycleRepository`, `InvoiceRepository`) are mocked, so each test drives one service method, stubs the repository calls it makes, and asserts on the returned `BillingCycleResponse` or the exception thrown.

```java
// L1
package com.teleconnect.billing_service.service;
```

The package declaration. The test lives in the same package (`...service`) as the interface it tests. Placing a test in the same package as the production code (just under `src/test/java` instead of `src/main/java`) is the standard Maven convention and means the test could access package-private members of that package if it needed to — here it only touches public API.

```java
// L3-L11
import com.teleconnect.billing_service.dto.request.BillingCycleRequest;
import com.teleconnect.billing_service.dto.response.BillingCycleResponse;
import com.teleconnect.billing_service.entity.BillingCycle;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.BillingCycleRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.service.impl.BillingCycleServiceImpl;
```

These imports pull in every production type the test touches:
- `BillingCycleRequest` — the inbound Data Transfer Object (DTO) carrying the fields a client sends to create a cycle (`accountId`, `cycleStart`, `cycleEnd`). A DTO is a plain data holder used to move data across a layer boundary without exposing the entity.
- `BillingCycleResponse` — the outbound DTO the service returns to the controller (the "view" of a cycle).
- `BillingCycle` — the JPA **entity** (the database-mapped object) the repository stores and returns.
- `BillingCycleStatus` — the enum of lifecycle states (`OPEN`, `GENERATED`, `CLOSED`, etc.) used both on the entity and in assertions.
- `BillingException` and `ResourceNotFoundException` — the two custom runtime exceptions whose throwing the tests assert on.
- `BillingCycleRepository` and `InvoiceRepository` — the Spring Data JPA repository interfaces that will be **mocked** (stubbed) instead of hitting a real DB.
- `BillingCycleServiceImpl` — the concrete Service-layer class **under test**.

```java
// L12-L18
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
```

The JUnit 5 (Jupiter) and Mockito infrastructure:
- `@BeforeEach` — JUnit annotation marking a method that runs **before every** `@Test` (fresh setup per test, so tests don't leak state into each other).
- `@DisplayName` — JUnit annotation giving a class or test a human-readable name shown in test reports/IDE instead of the method name.
- `@Test` — JUnit annotation marking a method as a runnable test case.
- `@ExtendWith` — JUnit 5 mechanism for plugging an "extension" (a behaviour add-on) into the test lifecycle.
- `@InjectMocks` / `@Mock` — Mockito annotations (explained at their use sites below).
- `MockitoExtension` — the JUnit 5 extension that activates Mockito's annotation processing (creating mocks and injecting them) and strict stubbing checks.

```java
// L20-L25
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
```

- `LocalDate` — the date type used for cycle start/end dates (no time-of-day component).
- `Optional<T>` — a container that holds either one value or nothing; Spring Data repository finder methods return `Optional` to represent "found" vs "not found" without using `null`. The mocks here stub those to return `Optional.of(...)` or `Optional.empty()`.
- The three `static` imports bring assertion/Mockito methods into scope so they can be called bare (without a class prefix): JUnit's assertions (`assertNotNull`, `assertEquals`, `assertThrows`, `assertTrue`), Mockito's `any(...)` argument matcher, and the rest of Mockito's static API (`when`, `verify`, `never`).

```java
// L27-L29
@ExtendWith(MockitoExtension.class)
@DisplayName("Billing Cycle Service Tests")
class BillingCycleServiceTest {
```

`@ExtendWith(MockitoExtension.class)` registers Mockito with this test class. That extension does two key jobs: it instantiates every `@Mock` field as a stub object, and it constructs every `@InjectMocks` field and pushes the mocks into it. It also enforces *strict stubbing* — if a test sets up a `when(...)` stub that is never used, the test fails, which keeps stubs honest. `@DisplayName` labels the whole class. The class is package-private (`class`, no `public`), which is the normal style for JUnit 5 test classes.

```java
// L31-L35
    @Mock private BillingCycleRepository billingCycleRepository;
    @Mock private InvoiceRepository invoiceRepository;

    @InjectMocks
    private BillingCycleServiceImpl billingCycleService;
```

- `@Mock` creates a fake implementation of the field's type whose methods do nothing (returning defaults like empty `Optional`/`null`) until a test stubs them with `when(...)`. `billingCycleRepository` and `invoiceRepository` are the two repository dependencies of the service.
- `@InjectMocks` constructs a **real** `BillingCycleServiceImpl` and injects the two mocks above into its dependency fields. Looking at the implementation, the service uses field injection (`@Autowired private BillingCycleRepository billingCycleRepository;` etc.), so Mockito sets those fields by type matching. The result: `billingCycleService` is the genuine production class, but every call it makes to a repository goes to a mock the test controls.

*Aside: `invoiceRepository` is mocked and injected because the production class declares it as a dependency, but none of the tests in this file stub or use it (the `generateInvoicesBatch` method that uses it is not exercised here). Because `MockitoExtension` uses lenient handling for unused mocks that are never stubbed, this is harmless; it would only matter if an unused `when(...)` stub were created on it.*

```java
// L37-L38
    private BillingCycleRequest validRequest;
    private BillingCycle savedCycle;
```

Two shared test fixtures (reset before each test in `setUp`): `validRequest` is a well-formed input DTO; `savedCycle` is a stand-in for what a repository would return after persistence (it carries a generated `cycleId` and a status).

```java
// L40-L53
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
```

`setUp()` runs before every test (per `@BeforeEach`) and builds clean fixtures so tests are independent:
- `validRequest` is a `BillingCycleRequest` for account `1001L` covering May 2026 (start `2026-05-01`, end `2026-05-31`). This is the "happy path" input; individual tests mutate it (e.g. swap the dates) when they need an invalid case.
- `savedCycle` is a `BillingCycle` entity representing a persisted, OPEN cycle with primary key `1L` and the same account/dates. Tests return this from mocked finders/saves to simulate the DB round-trip.
- `1001L` and `1L` use the `L` suffix to make them `long` literals, matching the `Long` field types.

```java
// L55-L57
    // ════════════════════════════════════════════════════════════════════════
    // 1. Create Billing Cycle
    // ════════════════════════════════════════════════════════════════════════
```

A comment banner grouping the next block of tests by scenario ("Create Billing Cycle"). Comments have no runtime effect; they organize the file into the four behavioural areas tested.

```java
// L59-L75
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
```

The happy-path creation test, structured in the classic **Arrange / Act / Assert** form.
- **Arrange:** `when(...).thenReturn(...)` defines mock behaviour. The first stub says "when the service asks the repo for an OPEN cycle on account 1001, return `Optional.empty()`" — i.e. there is no existing open cycle, so the duplicate check passes. The second stub says "when the service calls `save` with *any* `BillingCycle` (the `any(BillingCycle.class)` matcher), return `savedCycle`." `any(...)` is needed because the service builds a brand-new `BillingCycle` internally, so the test can't supply the exact instance.
- **Act:** calls the real `createBillingCycle(validRequest)`. Internally the service validates the date range, runs the duplicate check (which returns empty here), builds an OPEN cycle, saves it, and maps the saved entity to a `BillingCycleResponse`.
- **Assert:** `assertNotNull` confirms a response came back; `assertEquals(OPEN, ...)` confirms the returned status is OPEN (it comes from `savedCycle`); `assertEquals(1001L, ...)` confirms the account id round-tripped. `verify(billingCycleRepository).save(any(BillingCycle.class))` confirms `save` was actually invoked exactly once — proving the cycle was persisted rather than silently dropped.

```java
// L77-L79
    // ════════════════════════════════════════════════════════════════════════
    // 2. Duplicate Billing Cycle Prevention
    // ════════════════════════════════════════════════════════════════════════
```

Banner comment introducing the duplicate-prevention and date-validation scenarios.

```java
// L81-L95
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
```

The duplicate-prevention rule: a single account may not have two OPEN cycles at once.
- **Arrange:** the finder is stubbed to return `Optional.of(savedCycle)`, i.e. an OPEN cycle already exists for account 1001.
- **Act & Assert:** `assertThrows(BillingException.class, () -> ...)` runs the lambda and passes only if it throws a `BillingException`; the thrown exception is captured in `ex`. The service's `findByAccountIdAndStatus(...).ifPresent(...)` branch fires and throws.
- `assertTrue(ex.getMessage().contains("open billing cycle already exists"))` checks the message text. The production message is `"An open billing cycle already exists for account: 1001"`; the assertion only checks the lowercase substring `"open billing cycle already exists"`, which is present, so it passes.
- `verify(billingCycleRepository, never()).save(any())` is the crucial negative check: `never()` asserts `save` was called **zero** times, proving the service short-circuited before any write. `any()` here matches any argument of any type.

```java
// L97-L111
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
```

The complement of the previous test: it documents that the duplicate check only blocks on *OPEN* cycles. Because the service only queries for `status = OPEN`, a previously CLOSED cycle is invisible to that finder, so stubbing it to return `Optional.empty()` correctly models "the prior cycle is closed." The save is stubbed to succeed, the new cycle is created, and the test asserts a non-null response with OPEN status. Functionally this is identical to the L59 happy-path test; it exists to *name and prove the business rule* that closing a cycle frees the account to start a new one.

```java
// L113-L124
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
```

The date-range validation rule. The fixture is mutated so `cycleStart = 2026-05-31` and `cycleEnd = 2026-05-01` (end before start). `createBillingCycle` checks `request.getCycleEnd().isBefore(request.getCycleStart())` first, so it throws `BillingException` before any repository call. `assertThrows` captures the exception and `assertEquals` does an **exact** match on the message `"Cycle end date must be after cycle start date"`. Note no repository stubs are set up here — under strict stubbing that's fine because the method never reaches the repository.

*Aside: the validation uses `isBefore`, so equal start and end dates (`cycleEnd == cycleStart`) would NOT be rejected. The `@DisplayName` wording "end date must be after start date" is slightly stronger than the code, which actually only rejects end strictly before start. This is a minor naming nuance, not necessarily a bug.*

```java
// L126-L128
    // ════════════════════════════════════════════════════════════════════════
    // 3. Close Billing Cycle
    // ════════════════════════════════════════════════════════════════════════
```

Banner comment for the close-cycle scenarios.

```java
// L130-L142
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
```

Happy path for closing a cycle.
- `findById(1L)` is stubbed to return the OPEN `savedCycle` (so the service finds it).
- `save(...)` uses `.thenAnswer(inv -> inv.getArgument(0))` instead of `.thenReturn`. `thenAnswer` lets the mock compute its return value from the call; here it echoes back **argument 0** — the very entity passed to `save`. This matters because the service mutates the cycle's status to CLOSED *before* saving, so returning the saved argument reflects that mutation. (A plain `thenReturn(savedCycle)` would have worked too since it's the same object, but the echo pattern is robust to that.)
- The service sets status to CLOSED and returns the mapped response; the test asserts the response status is CLOSED.

```java
// L144-L155
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
```

The idempotency guard: you cannot close an already-CLOSED cycle. The fixture's status is flipped to CLOSED, and `findById(1L)` returns it. `closeBillingCycle` finds the cycle, sees `status == CLOSED`, and throws `BillingException("Billing cycle is already closed")` before saving. The test asserts both the exception type and the exact message. No `save` stub is defined, consistent with the guard returning early.

```java
// L157-L166
    @Test
    @DisplayName("Should throw ResourceNotFoundException when cycle ID does not exist")
    void closeBillingCycle_notFound_throwsResourceNotFoundException() {
        // Arrange
        when(billingCycleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> billingCycleService.closeBillingCycle(999L));
    }
```

The not-found path. `findById(999L)` returns `Optional.empty()`, modelling a missing row. The service's private `findById` helper does `.orElseThrow(() -> new ResourceNotFoundException(...))`, so closing a non-existent cycle throws `ResourceNotFoundException`. The test only checks the exception **type** (no message assertion). This distinction matters because the application's exception handler typically maps `ResourceNotFoundException` to HTTP 404 and `BillingException` to 400/409 — so testing the type, not just "some exception," guards the HTTP contract.

```java
// L168-L170
    // ════════════════════════════════════════════════════════════════════════
    // 4. Invalid Status Transition
    // ════════════════════════════════════════════════════════════════════════
```

Banner comment for the status-update / read scenarios (labelled "Invalid Status Transition," though the tests below actually cover a successful status update and a read).

```java
// L172-L185
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
```

Verifies the side-effect of transitioning to GENERATED. `findById` returns the cycle and `save` echoes its argument (same pattern as the close test, so mutations are reflected). The service's `updateCycleStatus` sets the new status and, *specifically when the target is GENERATED*, also sets `generatedDate = LocalDate.now()`. The test asserts the status is GENERATED **and** that `generatedDate` is now non-null — pinning the conditional `if (status == GENERATED)` branch in the implementation. For any other target status that branch would be skipped and `generatedDate` would remain null.

```java
// L187-L194
    @Test
    @DisplayName("Should get billing cycle by ID successfully")
    void getBillingCycleById_success() {
        when(billingCycleRepository.findById(1L)).thenReturn(Optional.of(savedCycle));
        BillingCycleResponse response = billingCycleService.getBillingCycleById(1L);
        assertNotNull(response);
        assertEquals(1L, response.getCycleId());
    }
```

The simple read path. `findById(1L)` returns `savedCycle`; the service maps it to a response via the private `toResponse` helper. The test asserts the response is present and its `cycleId` is `1L`, confirming the entity-to-DTO mapping preserves the identifier. This test is written inline (no explicit Arrange/Act/Assert comments) since the flow is trivial.

```java
// L195
}
```

Closing brace of the test class.

---

## src/test/java/com/teleconnect/billing_service/BillingServiceApplicationTests.java

The Spring Boot context-load smoke test: it boots the full application context once to confirm all beans and configuration wire up without error.

```java
// L1
package com.teleconnect.billing_service;
```

Package declaration. This test sits in the **root** application package `com.teleconnect.billing_service` — the same package as the `@SpringBootApplication` main class. That placement is deliberate: Spring Boot's component scanning starts from the main class's package, so a test here can locate and load the application's configuration via the default `@SpringBootConfiguration` discovery.

```java
// L3-L4
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
```

- `@Test` — the JUnit 5 marker for a runnable test method.
- `@SpringBootTest` — the Spring Boot test annotation that, when applied to a test class, bootstraps a real `ApplicationContext` (the dependency-injection container) for the test, scanning for the primary `@SpringBootApplication` configuration and creating all the application's beans.

```java
// L6-L7
@SpringBootTest
class BillingServiceApplicationTests {
```

`@SpringBootTest` tells the framework to start the complete application context before running the tests in this class — instantiating controllers, services, repositories, JPA entity mappings, and any auto-configuration (data source, etc.). If anything is misconfigured (a missing bean, a bad `@Autowired`, an invalid entity mapping, a broken property), the context fails to start and the test fails. The class is package-private, the JUnit 5 convention.

```java
// L9-L11
	@Test
	void contextLoads() {
	}
```

The single test method, `contextLoads`, has an **empty body** on purpose. The "test" is really the act of starting the context that `@SpringBootTest` performs before this method runs. If startup succeeds, the empty body completes trivially and the test passes; if startup throws, JUnit reports the failure. This is the canonical Spring Boot smoke test — cheap insurance that the whole wiring is internally consistent even though it asserts nothing explicitly.

```java
// L13
}
```

Closing brace of the test class.

---

## How this connects

`BillingCycleServiceTest` sits squarely at the **Service** layer of the Controller → Service → Repository → Entity/DB stack. It instantiates the real `BillingCycleServiceImpl` (Service) but swaps the **Repository** layer (`BillingCycleRepository`, `InvoiceRepository`) for Mockito mocks, so it tests business rules — duplicate-OPEN-cycle prevention, the end-before-start date check, the already-closed guard, the not-found path, and the GENERATED status side-effect — entirely in memory, with no DB and no controller. The `BillingCycleResponse`/`BillingCycleRequest` DTOs and the `BillingCycle` entity flow through it exactly as they would in production. By contrast, `BillingServiceApplicationTests` is a cross-cutting integration check: `@SpringBootTest` stands up the *entire* context, so it indirectly validates that the Controllers, the real Services, the real Spring Data repositories, the JPA entity mappings, and the exception handlers all bind together — the complement to the focused, mocked unit tests in the first file.
