# Service Implementation: `BillingCycleServiceImpl`

`BillingCycleServiceImpl` is the concrete business-logic class that implements the `BillingCycleService` interface. It sits in the **Service** layer of the classic `Controller -> Service -> Repository -> Entity/DB` flow: a REST controller calls these methods, and the methods in turn talk to Spring Data JPA repositories (`BillingCycleRepository`, `InvoiceRepository`) to read/write `BillingCycle` and `Invoice` rows. Its job is to validate input, enforce billing rules (e.g. only one OPEN cycle per account, date ordering, idempotent invoice generation), mutate entities, and translate them into outward-facing DTOs (`BillingCycleResponse`, `BatchGenerationResponse`) so the controller never touches entities directly.

## src/main/java/com/teleconnect/billing_service/service/impl/BillingCycleServiceImpl.java

The single file under documentation. It contains all create/read/update/close logic for billing cycles plus the batch invoice-generation routine, and two private helpers (`findById`, `toResponse`).

---

### Package declaration

```java
// L1
package com.teleconnect.billing_service.service.impl;
```

Declares the Java package this class lives in. The `.service.impl` sub-package is the conventional home for *implementations* of service interfaces, keeping the interface (`...service.BillingCycleService`) separate from its concrete code so other layers can depend on the abstraction.

### Imports — project DTOs and entities

```java
// L3-L10
import com.teleconnect.billing_service.dto.request.BillingCycleRequest;
import com.teleconnect.billing_service.dto.request.CycleGenerationRequest;
import com.teleconnect.billing_service.dto.response.BatchGenerationResponse;
import com.teleconnect.billing_service.dto.response.BillingCycleResponse;
import com.teleconnect.billing_service.entity.BillingCycle;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
```

These bring in the types this class manipulates. **DTOs** (Data Transfer Objects) are plain data carriers used at the API boundary: `BillingCycleRequest` (incoming create payload: `accountId`, `cycleStart`, `cycleEnd`), `CycleGenerationRequest` (incoming batch payload: `cycleDate`, `dryRun`), and the outgoing `BatchGenerationResponse` / `BillingCycleResponse`. **Entities** (`BillingCycle`, `Invoice`) are the JPA-mapped objects that correspond to database table rows. The **enums** are fixed value sets: `BillingCycleStatus` is `OPEN, GENERATED, CLOSED`; `InvoiceStatus` is `GENERATED, SENT, PAID, OVERDUE, DISPUTED`.

### Imports — exceptions and repositories

```java
// L11-L15
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.BillingCycleRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.service.BillingCycleService;
```

`BillingException` is a custom unchecked exception (`extends RuntimeException`) used for business-rule violations; `ResourceNotFoundException` signals a missing record (typically mapped to HTTP 404 by a global exception handler). The two repository interfaces are the **Repository** layer — Spring Data JPA generates their query implementations at runtime. `BillingCycleService` is the interface this class implements.

### Imports — Spring and Spring Data

```java
// L16-L20
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

`@Autowired` enables Spring **dependency injection** (Spring supplies bean instances for the annotated fields). `Page<T>` is Spring Data's container for one *page* of results plus paging metadata (total elements, page number, etc.); `Pageable` carries the requested page number, size, and sort. `@Service` marks the class as a Spring-managed service bean. `@Transactional` (Spring's, not Jakarta's) wraps a method in a database transaction.

### Imports — JDK types

```java
// L22-L26
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
```

`BigDecimal` is the exact, arbitrary-precision decimal type used for money (avoids floating-point rounding errors). `LocalDate` is a date without time/zone (used for cycle/due dates); `LocalDateTime` is date+time without zone (timestamps). `List` is the collection interface for returning multiple cycles, and `Collectors` provides the `toList()` collector used in stream pipelines.

### Class declaration and `@Service`

```java
// L28-L29
@Service
public class BillingCycleServiceImpl implements BillingCycleService {
```

`@Service` registers this class in the Spring application context as a singleton bean (a specialization of `@Component` that signals "business service layer"). Because it `implements BillingCycleService`, controllers can depend on the interface and Spring injects this implementation. Every method below is an override of a method declared on that interface.

### Injected dependency: `billingCycleRepository`

```java
// L31-L32
    @Autowired
    private BillingCycleRepository billingCycleRepository;
```

Field-level `@Autowired` tells Spring to inject the `BillingCycleRepository` bean here. This repository extends `JpaRepository<BillingCycle, Long>`, giving CRUD methods (`save`, `findById`, …) plus the derived finders used below (`findByAccountId`, `findByStatus`, `findByAccountIdAndStatus`, `findByStatusAndCycleEndLessThanEqual`, and their `Pageable` overloads). It is the gateway to the `billing_cycles` table.

*Aside: field injection is convenient but is generally discouraged in favor of constructor injection, which makes dependencies final, easier to unit-test (no reflection needed), and impossible to leave un-set. This is a style note, not a bug.*

### Injected dependency: `invoiceRepository`

```java
// L34-L35
    @Autowired
    private InvoiceRepository invoiceRepository;
```

Injects the `InvoiceRepository` (extends `JpaRepository<Invoice, Long>`). Only two of its capabilities are used here: `findByAccountIdAndCycleId(...)` (to check whether an invoice already exists for a cycle) and the inherited `save(...)`. It maps to the invoices table.

---

### Method `createBillingCycle` — annotations and signature

```java
// L37-L39
    @Override
    @Transactional
    public BillingCycleResponse createBillingCycle(BillingCycleRequest request) {
```

`@Override` asserts this implements the interface method (compile-time safety). `@Transactional` opens a database transaction around the whole method: all writes commit together when the method returns normally, and roll back if a `RuntimeException` (such as `BillingException`) escapes. The method takes a validated `BillingCycleRequest` and returns a `BillingCycleResponse` DTO.

### `createBillingCycle` — date-order rule

```java
// L40-L42
        if (request.getCycleEnd().isBefore(request.getCycleStart())) {
            throw new BillingException("Cycle end date must be after cycle start date");
        }
```

Business rule #1: the cycle's end date must not precede its start date. `LocalDate.isBefore` returns `true` only when `cycleEnd` is strictly earlier than `cycleStart`; in that case a `BillingException` is thrown. Because the exception is a `RuntimeException` and the method is transactional, no row is written.

*Aside: the check is strictly "before", so `cycleEnd == cycleStart` (a single-day cycle) is allowed even though the message says "must be after". This is a minor message/behavior mismatch, not a functional bug.*

### `createBillingCycle` — single-open-cycle rule

```java
// L44-L48
        billingCycleRepository.findByAccountIdAndStatus(request.getAccountId(), BillingCycleStatus.OPEN)
                .ifPresent(c -> {
                    throw new BillingException(
                            "An open billing cycle already exists for account: " + request.getAccountId());
                });
```

Business rule #2: an account may have at most one `OPEN` cycle at a time. `findByAccountIdAndStatus` returns an `Optional<BillingCycle>` — a container that is either empty or holds one value, used to avoid `null`. `Optional.ifPresent(...)` runs the lambda only if a matching OPEN cycle was found; the lambda throws `BillingException`, aborting creation. If the Optional is empty, nothing happens and execution continues.

### `createBillingCycle` — build the entity

```java
// L50-L55
        BillingCycle cycle = BillingCycle.builder()
                .accountId(request.getAccountId())
                .cycleStart(request.getCycleStart())
                .cycleEnd(request.getCycleEnd())
                .status(BillingCycleStatus.OPEN)
                .build();
```

Constructs a new `BillingCycle` using its hand-written **builder** (a fluent step-by-step object constructor; here it is plain Java, not Lombok `@Builder`). Only four fields are set — `accountId`, `cycleStart`, `cycleEnd`, and `status = OPEN`. `cycleId` is left null (the DB generates it via `@GeneratedValue(IDENTITY)`), `generatedDate` stays null (no invoice yet), and `createdAt`/`updatedAt` are populated automatically by the entity's `@PrePersist` callback on save.

### `createBillingCycle` — persist and map

```java
// L57-L58
        return toResponse(billingCycleRepository.save(cycle));
    }
```

`save(cycle)` is JPA: it issues an `INSERT` (entity is new) and returns the managed instance now carrying the generated `cycleId` and the timestamps set by `@PrePersist`. That saved entity is passed to the private `toResponse` helper (L166) and the resulting DTO is returned to the caller (the controller), which serializes it to JSON.

---

### Method `generateInvoicesBatch` — annotations and signature

```java
// L60-L62
    @Override
    @Transactional
    public BatchGenerationResponse generateInvoicesBatch(CycleGenerationRequest request) {
```

The batch routine that scans for due, still-open cycles and generates their invoices. It is `@Transactional`, so the entire batch runs in one transaction. The `CycleGenerationRequest` supplies `cycleDate` (the cutoff) and `dryRun` (a flag that, when true, computes counts without writing anything). Returns a `BatchGenerationResponse` summary.

### `generateInvoicesBatch` — fetch eligible cycles

```java
// L63-L64
        List<BillingCycle> eligible = billingCycleRepository
                .findByStatusAndCycleEndLessThanEqual(BillingCycleStatus.OPEN, request.getCycleDate());
```

Loads every cycle that is still `OPEN` *and* whose `cycleEnd` is on or before `request.getCycleDate()` (the `LessThanEqual` keyword maps to SQL `<=`). These are the cycles that have completed their period and are due for invoicing. The result is an in-memory `List<BillingCycle>`.

### `generateInvoicesBatch` — counters

```java
// L66-L69
        int processed = 0;
        int generated = 0;
        int skipped = 0;
        int errors = 0;
```

Four running tallies for the summary: `processed` = cycles examined, `generated` = invoices created (or that would be in a dry run), `skipped` = cycles that already had an invoice, `errors` = cycles that threw during processing.

### `generateInvoicesBatch` — loop start and process count

```java
// L71-L72
        for (BillingCycle cycle : eligible) {
            processed++;
```

Iterates each eligible cycle. `processed` is incremented first so every cycle that enters the loop is counted regardless of its later outcome (generated, skipped, or error).

### `generateInvoicesBatch` — duplicate-invoice (idempotency) check

```java
// L73-L80
            try {
                boolean invoiceExists = invoiceRepository
                        .findByAccountIdAndCycleId(cycle.getAccountId(), cycle.getCycleId())
                        .isPresent();
                if (invoiceExists) {
                    skipped++;
                    continue;
                }
```

The per-cycle work is wrapped in `try` so one failure does not abort the whole batch. It asks the invoice repository whether an invoice already exists for this `(accountId, cycleId)` pair; `Optional.isPresent()` returns `true` if one was found. If so, the cycle is counted as `skipped` and `continue` jumps to the next iteration — this makes the batch **idempotent** (re-running it won't double-bill an account).

### `generateInvoicesBatch` — guard on dry-run and build the invoice

```java
// L82-L96
                if (!request.isDryRun()) {
                    // Charge components are zero-initialised here: plan and usage data live in
                    // the Plan/Usage modules (out of this service's Phase 1 scope) and are
                    // populated via the explicit generate-invoice endpoint or later integration.
                    Invoice invoice = Invoice.builder()
                            .accountId(cycle.getAccountId())
                            .cycleId(cycle.getCycleId())
                            .planCharges(BigDecimal.ZERO)
                            .excessCharges(BigDecimal.ZERO)
                            .addOnCharges(BigDecimal.ZERO)
                            .taxes(BigDecimal.ZERO)
                            .totalAmount(BigDecimal.ZERO)
                            .dueDate(cycle.getCycleEnd().plusDays(15))
                            .status(InvoiceStatus.GENERATED)
                            .build();
```

All writing is skipped when `dryRun` is true — `!request.isDryRun()` ensures the body runs only for a real run. The source comment documents a deliberate design decision: every monetary field is set to `BigDecimal.ZERO` because plan/usage figures are owned by other modules and filled in later. `dueDate` is computed as the cycle's end date plus 15 days (`LocalDate.plusDays(15)`), and the invoice opens in `GENERATED` status. `cycleId` here is the invoice's link back to its billing cycle.

### `generateInvoicesBatch` — save invoice and advance the cycle

```java
// L97-L102
                    invoiceRepository.save(invoice);

                    cycle.setStatus(BillingCycleStatus.GENERATED);
                    cycle.setGeneratedDate(LocalDate.now());
                    billingCycleRepository.save(cycle);
                }
```

`invoiceRepository.save(invoice)` inserts the new invoice row. The cycle is then transitioned from `OPEN` to `GENERATED` and stamped with today's date (`LocalDate.now()`), and `billingCycleRepository.save(cycle)` persists that change (an `UPDATE`, since the entity already has an id; this also fires the entity's `@PreUpdate` to refresh `updatedAt`). The closing brace ends the `if (!dryRun)` block.

### `generateInvoicesBatch` — success and error tally

```java
// L103-L107
                generated++;
            } catch (Exception ex) {
                errors++;
            }
        }
```

`generated++` runs after the `if` block, so it counts the cycle as generated *even in a dry run* (i.e. "would have generated"). If any exception was thrown anywhere in the `try` (e.g. a DB error on save), it is caught and `errors` is incremented instead — the loop then proceeds to the next cycle. The final brace closes the `for` loop.

*Aside: the `catch (Exception ex)` swallows the exception entirely — there is no logging and `ex` is unused, so failures are counted but their cause is not recorded anywhere. Also note that because the method is `@Transactional`, a caught DB exception may have already marked the surrounding transaction rollback-only; subsequent saves in the same transaction could then fail at commit. For a per-cycle isolation guarantee you would typically generate each invoice in its own transaction (e.g. a separate `@Transactional(REQUIRES_NEW)` method). Worth flagging, though it depends on the JPA provider's behavior.*

### `generateInvoicesBatch` — build and return the summary

```java
// L109-L111
        return new BatchGenerationResponse(
                processed, generated, skipped, errors, request.isDryRun(), LocalDateTime.now());
    }
```

Constructs the response DTO from the four counters plus the `dryRun` flag and a `runDate` timestamp of `LocalDateTime.now()`. (The DTO's constructor maps these positionally to `cyclesProcessed`, `invoicesGenerated`, `skipped`, `errors`, `dryRun`, `runDate`.) This is returned to the controller as the batch outcome.

---

### Method `getBillingCycleById`

```java
// L113-L116
    @Override
    public BillingCycleResponse getBillingCycleById(Long cycleId) {
        return toResponse(findById(cycleId));
    }
```

A read-only lookup (no `@Transactional` — a single fetch needs none). It delegates to the private `findById` helper (L160), which returns the entity or throws `ResourceNotFoundException` if absent, then maps it to a DTO via `toResponse`.

### Method `getCyclesByAccount` (list, no paging)

```java
// L118-L122
    @Override
    public List<BillingCycleResponse> getCyclesByAccount(Long accountId) {
        return billingCycleRepository.findByAccountId(accountId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }
```

Returns all cycles for an account as a plain `List`. `findByAccountId` gives a `List<BillingCycle>`; `.stream()` opens a Java Stream pipeline, `.map(this::toResponse)` converts each entity to a DTO via a method reference, and `.collect(Collectors.toList())` gathers the DTOs into a `List<BillingCycleResponse>`. If the account has no cycles, an empty list is returned (not an error).

### Method `getCyclesByAccount` (paged + optional status filter)

```java
// L124-L130
    @Override
    public Page<BillingCycleResponse> getCyclesByAccount(Long accountId, BillingCycleStatus status, Pageable pageable) {
        Page<BillingCycle> page = (status == null)
                ? billingCycleRepository.findByAccountId(accountId, pageable)
                : billingCycleRepository.findByAccountIdAndStatus(accountId, status, pageable);
        return page.map(this::toResponse);
    }
```

An overload that returns a `Page` so the API can paginate. The ternary picks the query: when `status` is `null`, fetch the account's cycles unfiltered (`findByAccountId(accountId, pageable)`); otherwise filter by status too (`findByAccountIdAndStatus(...)`). The `pageable` argument carries page number/size/sort. `Page.map(this::toResponse)` converts each entity in the page to a DTO while preserving the paging metadata, yielding a `Page<BillingCycleResponse>`.

### Method `getCyclesByStatus`

```java
// L132-L136
    @Override
    public List<BillingCycleResponse> getCyclesByStatus(BillingCycleStatus status) {
        return billingCycleRepository.findByStatus(status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }
```

Returns every cycle in a given status (e.g. all `OPEN` cycles) as a DTO list, using the same stream-map-collect pattern as L120.

---

### Method `updateCycleStatus`

```java
// L138-L147
    @Override
    @Transactional
    public BillingCycleResponse updateCycleStatus(Long cycleId, BillingCycleStatus status) {
        BillingCycle cycle = findById(cycleId);
        cycle.setStatus(status);
        if (status == BillingCycleStatus.GENERATED) {
            cycle.setGeneratedDate(LocalDate.now());
        }
        return toResponse(billingCycleRepository.save(cycle));
    }
```

A transactional status setter. It loads the cycle (`findById`, throwing 404 if missing), assigns the new `status`, and — if the new status is `GENERATED` — also stamps `generatedDate` with today's date so that field stays consistent with the GENERATED state. It then saves (an `UPDATE`, firing `@PreUpdate` to refresh `updatedAt`) and returns the mapped DTO. No transition-validity checks are made here, so any status can be set to any other (including, e.g., re-opening a CLOSED cycle); only `closeBillingCycle` guards a specific transition.

### Method `closeBillingCycle`

```java
// L149-L158
    @Override
    @Transactional
    public BillingCycleResponse closeBillingCycle(Long cycleId) {
        BillingCycle cycle = findById(cycleId);
        if (cycle.getStatus() == BillingCycleStatus.CLOSED) {
            throw new BillingException("Billing cycle is already closed");
        }
        cycle.setStatus(BillingCycleStatus.CLOSED);
        return toResponse(billingCycleRepository.save(cycle));
    }
```

Closes a cycle. After loading it, the guard rejects a double-close: if the cycle is already `CLOSED`, it throws `BillingException` (the transaction rolls back, nothing changes). Otherwise it sets status to `CLOSED`, saves, and returns the DTO. Note this allows closing from any non-closed status (OPEN or GENERATED), and unlike L143 it does not touch `generatedDate`.

---

### Private helper `findById`

```java
// L160-L164
    private BillingCycle findById(Long cycleId) {
        return billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Billing cycle not found with ID: " + cycleId));
    }
```

A shared lookup used by several public methods. `JpaRepository.findById` returns `Optional<BillingCycle>`; `Optional.orElseThrow(...)` returns the entity if present, otherwise lazily builds and throws a `ResourceNotFoundException` (typically surfaced as HTTP 404). Centralizing this keeps the "fetch or 404" logic in one place rather than repeating it in every method.

### Private helper `toResponse`

```java
// L166-L177
    private BillingCycleResponse toResponse(BillingCycle cycle) {
        return BillingCycleResponse.builder()
                .cycleId(cycle.getCycleId())
                .accountId(cycle.getAccountId())
                .cycleStart(cycle.getCycleStart())
                .cycleEnd(cycle.getCycleEnd())
                .generatedDate(cycle.getGeneratedDate())
                .status(cycle.getStatus())
                .createdAt(cycle.getCreatedAt())
                .updatedAt(cycle.getUpdatedAt())
                .build();
    }
```

The **entity-to-DTO mapper**. It copies all eight fields from a `BillingCycle` entity into a `BillingCycleResponse` via that DTO's hand-written builder. This boundary mapping is important: it keeps JPA entities (with their persistence concerns and lazy-loading) out of the HTTP/JSON layer and lets the API contract evolve independently from the database schema. Every public method that returns a single cycle funnels through here.

### Closing brace

```java
// L178
}
```

Closes the class body.

---

## How this connects

- **Called by (upstream):** A REST controller (the **Controller** layer) injects `BillingCycleService` and invokes these methods, passing in validated request DTOs (`BillingCycleRequest`, `CycleGenerationRequest`) and returning the response DTOs (`BillingCycleResponse`, `BatchGenerationResponse`, or a `Page` of them) straight to the HTTP client as JSON.
- **Calls into (downstream):** It depends on two Spring Data JPA repositories — `BillingCycleRepository` (the `billing_cycles` table) and `InvoiceRepository` (the invoices table). Those repositories translate the derived finder method names and inherited CRUD calls into SQL against the **DB**.
- **Entities/enums:** It builds and mutates the `BillingCycle` entity (whose `@PrePersist`/`@PreUpdate` hooks manage `createdAt`/`updatedAt`) and the `Invoice` entity, using the `BillingCycleStatus` and `InvoiceStatus` enums as the legal state values.
- **Error handling:** `BillingException` (rule violations) and `ResourceNotFoundException` (missing records) are thrown here and are expected to be translated into appropriate HTTP responses by a global exception handler (e.g. a `@RestControllerAdvice`) elsewhere in the application.
