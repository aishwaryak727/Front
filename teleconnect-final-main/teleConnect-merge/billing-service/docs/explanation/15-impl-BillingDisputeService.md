# Service impl: BillingDisputeServiceImpl

This file is the concrete business-logic implementation for billing disputes. It implements the `BillingDisputeService` interface and sits in the **Service** layer of the classic Spring layering: a REST **Controller** receives an HTTP request, calls a method on this **Service**, which in turn talks to Spring Data JPA **Repositories** (`BillingDisputeRepository`, `InvoiceRepository`) that persist/load **Entities** (`BillingDispute`, `Invoice`) to/from the database. It encapsulates all the rules for raising a dispute, moving it through review, and resolving/rejecting it, and it keeps the related `Invoice` status in sync as the dispute moves through its lifecycle.

## src/main/java/com/teleconnect/billing_service/service/impl/BillingDisputeServiceImpl.java

The `@Service`-annotated class that holds the full lifecycle logic for billing disputes (raise, query, review, resolve/reject) and synchronizes the parent invoice's status.

```java
// L1
package com.teleconnect.billing_service.service.impl;
```
Declares the **package** this class lives in. The package name maps to the folder path (`.../service/impl/`) and forms part of the class's fully-qualified name (`com.teleconnect.billing_service.service.impl.BillingDisputeServiceImpl`). Placing implementation classes under an `impl` sub-package is a common convention that separates the public interface (`service` package) from its concrete implementation.

```java
// L3-L10
import com.teleconnect.billing_service.dto.request.DisputeRequest;
import com.teleconnect.billing_service.dto.request.DisputeResolveRequest;
import com.teleconnect.billing_service.dto.request.DisputeReviewRequest;
import com.teleconnect.billing_service.dto.response.DisputeResponse;
import com.teleconnect.billing_service.entity.BillingDispute;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.DisputeStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
```
These imports bring in the application's own domain types. **DTOs** (Data Transfer Objects) are plain carrier classes used to move data across the API boundary so the database entities are never exposed directly: `DisputeRequest` (payload for raising a dispute), `DisputeResolveRequest` and `DisputeReviewRequest` (payloads for resolving/reviewing), and `DisputeResponse` (the object returned to callers). `BillingDispute` and `Invoice` are the JPA **entities** (rows in the `billing_disputes` and invoice tables). `DisputeStatus` and `InvoiceStatus` are **enums** describing the lifecycle states; `DisputeStatus` has values `OPEN, UNDER_REVIEW, RESOLVED, REJECTED`.

```java
// L11-L15
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.BillingDisputeRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.service.BillingDisputeService;
```
`BillingException` is a custom exception used to signal a broken business rule (e.g., disputing a paid invoice); `ResourceNotFoundException` signals a lookup that returned nothing. These are typically mapped to HTTP 400/404 by a global exception handler elsewhere. `BillingDisputeRepository` and `InvoiceRepository` are the Spring Data JPA repository interfaces this class depends on. `BillingDisputeService` is the interface this class implements (it defines the public method contract).

```java
// L16-L18
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```
Framework imports. `@Autowired` lets Spring inject collaborating beans automatically. `@Service` marks this class as a Spring-managed service component. `@Transactional` declares database-transaction boundaries around methods.

```java
// L20-L23
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
```
Standard-library imports. `LocalDate` is a date with no time-of-day (used for `raisedDate` and `dueDate`). `LocalDateTime` is a date plus time (used for `acknowledgedDate`/`resolvedDate` timestamps). `List` is the collection type returned by the query methods. `Collectors` provides `toList()` used to materialize Java Streams into a `List`.

```java
// L25-L26
@Service
public class BillingDisputeServiceImpl implements BillingDisputeService {
```
`@Service` is a Spring **stereotype** annotation: it registers this class in the application context as a singleton bean and marks it as part of the service (business-logic) layer, so Spring can discover it via component scanning and inject it wherever the `BillingDisputeService` interface is needed. The class `implements BillingDisputeService`, meaning it must provide concrete bodies for every method declared in that interface (the methods marked `@Override` below). Programming against the interface lets controllers depend on the abstraction rather than this concrete class.

```java
// L28-L29
    @Autowired
    private BillingDisputeRepository disputeRepository;
```
A field injected by Spring. `@Autowired` tells Spring to find the bean of type `BillingDisputeRepository` and assign it here at startup (this is **field injection**, as opposed to constructor injection). `disputeRepository` is the gateway to the `billing_disputes` table; because the repository extends `JpaRepository<BillingDispute, Long>`, it provides CRUD methods (`save`, `findById`, etc.) plus the derived finders `findByInvoiceId`, `findBySubscriberId`, and `findByStatus`.

```java
// L31-L32
    @Autowired
    private InvoiceRepository invoiceRepository;
```
A second injected dependency: the repository for `Invoice` entities. The dispute service needs it to validate the invoice being disputed and to flip the invoice's status to/from `DISPUTED` as the dispute lifecycle progresses.

```java
// L34-L36
    @Override
    @Transactional
    public DisputeResponse raiseDispute(DisputeRequest request) {
```
`raiseDispute` creates a brand-new dispute. `@Override` confirms it implements the interface method (the compiler errors if the signature doesn't match). `@Transactional` wraps the whole method body in a single database transaction: if any statement throws a runtime exception, **all** changes made in this method (saving the invoice and the dispute) are rolled back together, keeping the data consistent. It takes a `DisputeRequest` DTO and returns a `DisputeResponse` DTO.

```java
// L37-L39
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found: " + request.getInvoiceId()));
```
Loads the invoice the dispute targets. `findById` returns an `Optional<Invoice>` — a container that either holds the invoice or is empty (the JPA way to avoid `null`). `orElseThrow(...)` unwraps the value if present, or executes the supplied lambda to throw `ResourceNotFoundException` if the invoice id doesn't exist. The id comes from the request DTO (`request.getInvoiceId()`), which the controller's `@Valid` validation guarantees is non-null.

```java
// L41-L43
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BillingException("Cannot raise a dispute on an already paid invoice");
        }
```
**Business rule:** you cannot dispute an invoice that has already been paid. If the invoice's status equals `PAID`, the method throws `BillingException`, aborting the operation (and rolling back the transaction).

```java
// L44-L46
        if (invoice.getStatus() == InvoiceStatus.DISPUTED) {
            throw new BillingException("A dispute is already open for this invoice");
        }
```
**Business rule:** at most one open dispute per invoice. If the invoice is already in `DISPUTED` state, raising another is rejected with a `BillingException`.

```java
// L47-L50
        if (request.getDisputedAmount().compareTo(invoice.getTotalAmount()) > 0) {
            throw new BillingException(
                    "Disputed amount cannot exceed the invoice total of " + invoice.getTotalAmount());
        }
```
**Business rule:** the disputed amount cannot be more than the invoice total. Both amounts are `BigDecimal` (exact decimal arithmetic for money), so they're compared with `compareTo`, which returns a positive number when the left side is greater. A positive result means `disputedAmount > totalAmount`, which triggers the `BillingException`. *Note: the `@DecimalMin("0.01")` validation on `disputedAmount` (in `DisputeRequest`) already guarantees the amount is greater than zero, so only the upper bound needs checking here.*

```java
// L52-L55
        // subscriberId is optional — fall back to invoice's accountId
        Long subscriberId = (request.getSubscriberId() != null)
                ? request.getSubscriberId()
                : invoice.getAccountId();
```
Resolves which subscriber the dispute belongs to. The request's `subscriberId` is optional (it has no `@NotNull`). The ternary expression uses the request's value if it was supplied, otherwise falls back to the invoice's `accountId`. This guarantees the entity's non-nullable `subscriberId` column always gets a value.

```java
// L57-L65
        BillingDispute dispute = BillingDispute.builder()
                .invoiceId(request.getInvoiceId())
                .subscriberId(subscriberId)
                .disputeReason(request.getDisputeReason())
                .description(request.getDescription())
                .disputedAmount(request.getDisputedAmount())
                .raisedDate(LocalDate.now())
                .status(DisputeStatus.OPEN)
                .build();
```
Constructs the new dispute entity using a hand-written **builder** (a fluent object-construction pattern; here it is implemented manually in `BillingDispute`, not generated by Lombok). Each chained method sets one field and returns the builder, and `build()` produces the finished `BillingDispute`. The fields are copied from the request, the just-resolved `subscriberId` is applied, `raisedDate` is stamped with today's date (`LocalDate.now()`), and the initial `status` is set to `OPEN`. Fields not set here (`resolvedAmount`, `acknowledgedDate`, `resolvedDate`, `assignedTo`, `resolutionNotes`, and `disputeId`) remain null; `disputeId` will be auto-generated by the database on save.

```java
// L67-L68
        invoice.setStatus(InvoiceStatus.DISPUTED);
        invoiceRepository.save(invoice);
```
Flips the parent invoice to `DISPUTED` and persists that change. This is the side effect that enforces the "one open dispute per invoice" rule on subsequent calls (L44). `save` performs an UPDATE since the invoice already exists.

```java
// L70-L71
        return toResponse(disputeRepository.save(dispute));
    }
```
Persists the new dispute (an INSERT, which populates its generated `disputeId`), then converts the saved entity into a `DisputeResponse` DTO via the private `toResponse` helper, and returns it to the caller (the controller). Because the method is `@Transactional`, both the invoice update and the dispute insert commit together when the method returns normally.

```java
// L73-L76
    @Override
    public DisputeResponse getDisputeById(Long disputeId) {
        return toResponse(findById(disputeId));
    }
```
A read-only lookup of a single dispute. It delegates to the private `findById` helper (which throws `ResourceNotFoundException` if missing) and maps the entity to a `DisputeResponse`. There is no `@Transactional` here because it's a simple read.

```java
// L78-L82
    @Override
    public List<DisputeResponse> getDisputesByInvoice(Long invoiceId) {
        return disputeRepository.findByInvoiceId(invoiceId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }
```
Returns all disputes attached to one invoice. `findByInvoiceId` is a **derived query method**: Spring Data JPA generates the SQL automatically from the method name (`WHERE invoice_id = ?`). The resulting `List<BillingDispute>` is turned into a Java Stream, each entity is mapped through `toResponse` (the `this::toResponse` is a method reference equivalent to `d -> toResponse(d)`), and the stream is collected back into a `List<DisputeResponse>`.

```java
// L84-L88
    @Override
    public List<DisputeResponse> getDisputesBySubscriber(Long subscriberId) {
        return disputeRepository.findBySubscriberId(subscriberId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }
```
Same pattern as above, but filters by `subscriberId` via the derived `findBySubscriberId` query, then maps each entity to a response DTO.

```java
// L90-L99
    @Override
    public List<DisputeResponse> getDisputesByAccount(Long accountId, DisputeStatus status) {
        List<BillingDispute> disputes = disputeRepository.findBySubscriberId(accountId);
        if (status != null) {
            disputes = disputes.stream()
                    .filter(d -> d.getStatus() == status)
                    .collect(Collectors.toList());
        }
        return disputes.stream().map(this::toResponse).collect(Collectors.toList());
    }
```
Returns disputes for an "account", optionally narrowed to a specific status. It first fetches by `findBySubscriberId(accountId)` — note it reuses the **subscriberId** finder for the `accountId` argument, treating account id and subscriber id as the same key here. If `status` is non-null, it filters the list in memory (`filter(d -> d.getStatus() == status)`) keeping only matching disputes; if `status` is null, no filtering happens and all are kept. Finally it maps the (possibly filtered) list to response DTOs. *Note: the status filtering is done in Java rather than the database; a derived `findBySubscriberIdAndStatus` query would push that work to SQL, but functionally the result is the same.*

```java
// L101-L105
    @Override
    public List<DisputeResponse> getDisputesByStatus(DisputeStatus status) {
        return disputeRepository.findByStatus(status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }
```
Returns every dispute in a given status (e.g., all `OPEN` disputes) using the derived `findByStatus` query, mapped to response DTOs. Because `status` is an enum stored as a string (`@Enumerated(EnumType.STRING)` on the entity), the generated SQL compares against the enum's name.

```java
// L107-L110
    @Override
    @Transactional
    public DisputeResponse updateDisputeStatus(Long disputeId, DisputeStatus status) {
        BillingDispute dispute = findById(disputeId);
```
A generic status-update method (transactional, since it both reads and writes). It first loads the dispute via `findById` (throwing `ResourceNotFoundException` if absent).

```java
// L112-L114
        if (dispute.getStatus() == DisputeStatus.RESOLVED || dispute.getStatus() == DisputeStatus.REJECTED) {
            throw new BillingException("Cannot update a dispute that is already " + dispute.getStatus());
        }
```
**Business rule:** terminal states are immutable. If the dispute is already `RESOLVED` or `REJECTED` (its closed/final states), any further status change is rejected with a `BillingException`.

```java
// L116
        dispute.setStatus(status);
```
Applies the requested new status to the entity in memory.

```java
// L118-L121
        if (status == DisputeStatus.RESOLVED || status == DisputeStatus.REJECTED) {
            dispute.setResolvedDate(LocalDateTime.now());
            restoreInvoiceStatus(dispute.getInvoiceId());
        }
```
If the new status is a terminal one (`RESOLVED`/`REJECTED`), the method stamps `resolvedDate` with the current timestamp and calls `restoreInvoiceStatus` to take the parent invoice back out of `DISPUTED` (see L176). This keeps the invoice and dispute states consistent when a dispute closes through this generic path.

```java
// L123-L124
        return toResponse(disputeRepository.save(dispute));
    }
```
Persists the modified dispute (UPDATE) and returns the mapped response DTO.

```java
// L126-L129
    @Override
    @Transactional
    public DisputeResponse reviewDispute(Long disputeId, DisputeReviewRequest request) {
        BillingDispute dispute = findById(disputeId);
```
`reviewDispute` moves a dispute into the "under review" stage and records who is handling it. Transactional read-then-write; it begins by loading the dispute (or throwing not-found).

```java
// L131-L134
        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new BillingException(
                    "Only OPEN disputes can be moved to Under Review. Current status: " + dispute.getStatus());
        }
```
**Business rule:** the review transition is only valid from `OPEN`. Any other current status (already under review, resolved, or rejected) causes a `BillingException`, enforcing a one-directional state machine.

```java
// L136-L138
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        dispute.setAssignedTo(request.getAssignedTo());
        dispute.setAcknowledgedDate(LocalDateTime.now());
```
Transitions the dispute to `UNDER_REVIEW`, records the handler from the request (`assignedTo`), and stamps `acknowledgedDate` with the current timestamp (marking when the dispute was picked up).

```java
// L140-L142
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            dispute.setResolutionNotes(request.getNotes());
        }
```
Optionally records review notes. The notes are only stored if the request supplied a non-null, non-blank string (`isBlank()` treats whitespace-only as empty). If notes are missing or blank, the existing `resolutionNotes` value is left untouched.

```java
// L144-L145
        return toResponse(disputeRepository.save(dispute));
    }
```
Persists the updated dispute and returns it as a `DisputeResponse`.

```java
// L147-L150
    @Override
    @Transactional
    public DisputeResponse resolveDispute(Long disputeId, DisputeResolveRequest request) {
        BillingDispute dispute = findById(disputeId);
```
`resolveDispute` closes a dispute — either accepting it (`RESOLVED`) or rejecting it (`REJECTED`). Transactional; starts by loading the dispute (or throwing not-found).

```java
// L152-L154
        if (dispute.getStatus() == DisputeStatus.RESOLVED || dispute.getStatus() == DisputeStatus.REJECTED) {
            throw new BillingException("Dispute is already closed with status: " + dispute.getStatus());
        }
```
**Business rule:** a dispute cannot be resolved twice. If it's already in a terminal state, the call fails with a `BillingException`.

```java
// L156-L157
        boolean isResolved = "Resolved".equalsIgnoreCase(request.getResolution());
        DisputeStatus newStatus = isResolved ? DisputeStatus.RESOLVED : DisputeStatus.REJECTED;
```
Interprets the request's free-text `resolution` field. `"Resolved".equalsIgnoreCase(...)` returns true if the caller sent "Resolved" in any letter case (and is null-safe because the literal is the receiver). Any other value — including null or "Rejected" — is treated as a rejection. The chosen outcome is captured both as the boolean `isResolved` and the target `newStatus`.

```java
// L159-L161
        if (!isResolved && (request.getResolutionNotes() == null || request.getResolutionNotes().isBlank())) {
            throw new BillingException("Resolution notes are mandatory when rejecting a dispute");
        }
```
**Business rule:** rejecting a dispute requires an explanation. When the outcome is a rejection (`!isResolved`) and the resolution notes are null or blank, a `BillingException` is thrown. For an approval (`isResolved`), notes are not required by this check.

```java
// L163-L165
        dispute.setStatus(newStatus);
        dispute.setResolvedDate(LocalDateTime.now());
        dispute.setResolutionNotes(request.getResolutionNotes());
```
Applies the closing changes: sets the final status, stamps the resolution timestamp, and stores the resolution notes from the request. *Note: unlike the review step (L140), this assigns notes unconditionally, so resolving with null notes would overwrite any previously stored review notes with null.*

```java
// L167-L169
        if (isResolved && request.getCreditAmount() != null) {
            dispute.setResolvedAmount(request.getCreditAmount());
        }
```
Records a credit amount only when the dispute was accepted (`isResolved`) and a `creditAmount` was actually provided. This populates `resolvedAmount`, representing how much credit the customer is granted as a result of the dispute. On rejection, or when no credit is supplied, `resolvedAmount` stays null.

```java
// L171-L174
        restoreInvoiceStatus(dispute.getInvoiceId());

        return toResponse(disputeRepository.save(dispute));
    }
```
Because the dispute is now closed, the parent invoice is taken out of `DISPUTED` via `restoreInvoiceStatus`, then the dispute is persisted and returned as a DTO. The invoice update and dispute update commit together under the method's transaction.

```java
// L176-L184
    private void restoreInvoiceStatus(Long invoiceId) {
        invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
            InvoiceStatus restored = invoice.getDueDate().isBefore(LocalDate.now())
                    ? InvoiceStatus.OVERDUE
                    : InvoiceStatus.SENT;
            invoice.setStatus(restored);
            invoiceRepository.save(invoice);
        });
    }
```
A private helper that returns an invoice to a sensible non-disputed status after its dispute closes. It looks up the invoice and uses `ifPresent` so the body runs **only if** the invoice exists (if it's missing, nothing happens — no exception). Inside, it decides the restored status by comparing the invoice's `dueDate` to today: if the due date is before today, the invoice has lapsed and becomes `OVERDUE`; otherwise it becomes `SENT` (issued but not yet overdue). It then saves that change. *Note: the original pre-dispute status isn't remembered, so a previously `PAID`/`DRAFT` invoice would be reset to `SENT`/`OVERDUE`; in practice only non-paid invoices can be disputed (L41), so this is a reasonable approximation.*

```java
// L186-L190
    private BillingDispute findById(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dispute not found with ID: " + disputeId));
    }
```
A private convenience method used by every operation that needs a single dispute. It wraps the repository's `Optional`-returning `findById` and converts an empty result into a `ResourceNotFoundException`, so callers always get a real entity or a clean 404-style error.

```java
// L192-L208
    private DisputeResponse toResponse(BillingDispute dispute) {
        return DisputeResponse.builder()
                .disputeId(dispute.getDisputeId())
                .invoiceId(dispute.getInvoiceId())
                .subscriberId(dispute.getSubscriberId())
                .disputeReason(dispute.getDisputeReason())
                .description(dispute.getDescription())
                .disputedAmount(dispute.getDisputedAmount())
                .resolvedAmount(dispute.getResolvedAmount())
                .raisedDate(dispute.getRaisedDate())
                .acknowledgedDate(dispute.getAcknowledgedDate())
                .resolvedDate(dispute.getResolvedDate())
                .assignedTo(dispute.getAssignedTo())
                .resolutionNotes(dispute.getResolutionNotes())
                .status(dispute.getStatus())
                .build();
    }
```
The entity-to-DTO mapper. It builds a `DisputeResponse` by copying each field from the `BillingDispute` entity one-to-one using `DisputeResponse.builder()`. This is the boundary that decouples the persisted entity from the API representation — controllers only ever see `DisputeResponse`, never the JPA entity, which avoids leaking persistence details and lets the API contract evolve independently of the schema.

```java
// L209
}
```
Closes the class.

## How this connects

- **Called by (Controller layer):** A `BillingDisputeController` (REST controller) injects `BillingDisputeService` and forwards validated HTTP requests here, passing in `@Valid`-checked DTOs (`DisputeRequest`, `DisputeReviewRequest`, `DisputeResolveRequest`) and returning the `DisputeResponse` objects this class produces.
- **Calls into (Repository layer):** It uses `BillingDisputeRepository` (CRUD + derived finders `findByInvoiceId`, `findBySubscriberId`, `findByStatus`) and `InvoiceRepository` to load and persist `BillingDispute` and `Invoice` entities — i.e., the gateway to the database.
- **Entities / DB:** It reads and mutates the `BillingDispute` entity (`billing_disputes` table) and the `Invoice` entity, keeping the two in sync (invoice flips to `DISPUTED` on raise, and back to `SENT`/`OVERDUE` on close via `restoreInvoiceStatus`).
- **Cross-cutting:** `@Transactional` on the mutating methods (`raiseDispute`, `updateDisputeStatus`, `reviewDispute`, `resolveDispute`) ensures the paired invoice+dispute writes commit or roll back atomically. Business-rule violations raise `BillingException` and missing lookups raise `ResourceNotFoundException`, both of which a global exception handler (`@ControllerAdvice`) typically translates into appropriate HTTP error responses.
