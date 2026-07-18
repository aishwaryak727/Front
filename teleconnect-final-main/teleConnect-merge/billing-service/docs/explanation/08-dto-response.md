# Response DTOs (Outgoing API Payloads)

This part documents the **response DTOs** of the billing microservice — the plain Java objects that shape the JSON sent *back* to API clients. In the standard layering (Controller -> Service -> Repository -> Entity/DB), these classes live at the edge: a Service or Controller maps internal JPA **entities** into one of these DTOs, and the Controller returns the DTO (usually wrapped in a `ResponseEntity<T>`) so Spring's Jackson serializer turns it into JSON. Keeping a separate DTO layer **decouples the persisted entity model from the wire format**, so the database schema can change without breaking the public contract, and sensitive or internal entity fields are never accidentally exposed.

A DTO ("Data Transfer Object") is just a container of fields with constructors and accessors — it holds no business logic and no persistence annotations. Every class in this group is a hand-written POJO ("Plain Old Java Object"): private fields, a no-argument constructor, an all-arguments constructor, getters and setters, and (for several of them) a hand-written **Builder**. None of these classes use Lombok or Jakarta Persistence annotations; the only framework annotation in the whole group is a single Jackson `@JsonFormat` in `PaymentResponse`.

---

## src/main/java/com/teleconnect/billing_service/dto/response/BatchGenerationResponse.java

Role: Summarizes the outcome of a bulk invoice-generation run (the result of triggering a batch job over many billing cycles), returned to whoever invoked the batch endpoint.

```java
// L1
package com.teleconnect.billing_service.dto.response;
```
The `package` statement declares the namespace this class belongs to. It places the class in the `dto.response` package, which is the convention this codebase uses for "objects returned by the API." The compiled `.class` file must live in a directory tree matching this package.

```java
// L3
import java.time.LocalDateTime;
```
Imports `LocalDateTime` from the Java time API. `LocalDateTime` is a date-plus-time value with no timezone (e.g. `2026-06-15T14:30:00`); it is used here for the timestamp of when the batch ran.

```java
// L5
public class BatchGenerationResponse {
```
Declares the public class. There is no annotation — this is a pure POJO. Being `public` lets controllers and services in other packages reference it.

```java
// L7-L12
    private int cyclesProcessed;
    private int invoicesGenerated;
    private int skipped;
    private int errors;
    private boolean dryRun;
    private LocalDateTime runDate;
```
The six instance fields, all `private` (encapsulation — access only via the accessors below). Their meanings:
- `cyclesProcessed` (`int`) — how many billing cycles the batch examined.
- `invoicesGenerated` (`int`) — how many invoices were actually created.
- `skipped` (`int`) — cycles intentionally not processed (e.g. already invoiced, ineligible).
- `errors` (`int`) — how many cycles failed during processing.
- `dryRun` (`boolean`) — `true` if this was a simulation that did not persist changes; `false` for a real run.
- `runDate` (`LocalDateTime`) — when the batch executed.

Note these are primitive `int`/`boolean`, not boxed `Integer`/`Boolean`, so they can never be `null`; an unset numeric counter serializes as `0` and `dryRun` as `false`.

```java
// L14
    public BatchGenerationResponse() {}
```
The no-argument (default) constructor with an empty body. A no-arg constructor is required so Jackson (Spring's JSON library) can instantiate the object during deserialization and so frameworks can construct it reflectively; it also lets callers build the object via setters.

```java
// L16-L24
    public BatchGenerationResponse(int cyclesProcessed, int invoicesGenerated, int skipped,
                                   int errors, boolean dryRun, LocalDateTime runDate) {
        this.cyclesProcessed = cyclesProcessed;
        this.invoicesGenerated = invoicesGenerated;
        this.skipped = skipped;
        this.errors = errors;
        this.dryRun = dryRun;
        this.runDate = runDate;
    }
```
The all-arguments constructor. It takes one parameter per field and assigns each to the matching instance field (`this.x = x` disambiguates the field from the same-named parameter). This is the convenient way for a service to construct a fully populated response in one statement.

```java
// L26-L42
    public int getCyclesProcessed() { return cyclesProcessed; }
    public void setCyclesProcessed(int cyclesProcessed) { this.cyclesProcessed = cyclesProcessed; }

    public int getInvoicesGenerated() { return invoicesGenerated; }
    public void setInvoicesGenerated(int invoicesGenerated) { this.invoicesGenerated = invoicesGenerated; }

    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }

    public int getErrors() { return errors; }
    public void setErrors(int errors) { this.errors = errors; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public LocalDateTime getRunDate() { return runDate; }
    public void setRunDate(LocalDateTime runDate) { this.runDate = runDate; }
}
```
The standard getter/setter pair for each field. These follow the JavaBeans naming convention, which is how Jackson decides the JSON property names: a getter `getCyclesProcessed()` produces the JSON key `cyclesProcessed`. Note the `boolean` accessor is `isDryRun()` (not `getDryRun()`) — the JavaBeans convention for boolean properties — which still serializes to the JSON key `dryRun`. The setters allow population field-by-field after a no-arg construction. The final `}` closes the class.

---

## src/main/java/com/teleconnect/billing_service/dto/response/BillingCycleResponse.java

Role: The wire representation of a billing cycle (the recurring period for which a customer account is billed), returned when a client reads or lists billing cycles.

```java
// L1-L6
package com.teleconnect.billing_service.dto.response;

import com.teleconnect.billing_service.enums.BillingCycleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
```
Package declaration, then imports. `BillingCycleStatus` is a project **enum** (a fixed set of named constants, e.g. lifecycle states like `OPEN`/`GENERATED`/`CLOSED`) describing the cycle's state; exposing it directly means the JSON shows the enum's name as a string. `LocalDate` is a date with no time component (e.g. `2026-06-15`), used for the cycle's start/end/generated dates; `LocalDateTime` adds time, used for audit timestamps.

```java
// L8
public class BillingCycleResponse {
```
Plain public POJO class declaration, no annotations.

```java
// L10-L17
    private Long cycleId;
    private Long accountId;
    private LocalDate cycleStart;
    private LocalDate cycleEnd;
    private LocalDate generatedDate;
    private BillingCycleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
```
The eight fields:
- `cycleId` (`Long`) — the billing cycle's primary-key identifier. `Long` (boxed) rather than `long` so it can be `null` when unknown.
- `accountId` (`Long`) — the owning customer account's id (a flattened reference, not a nested object).
- `cycleStart` / `cycleEnd` (`LocalDate`) — the inclusive date range the cycle covers.
- `generatedDate` (`LocalDate`) — the date the cycle's invoice(s) were generated.
- `status` (`BillingCycleStatus`) — the cycle's current lifecycle status.
- `createdAt` / `updatedAt` (`LocalDateTime`) — audit timestamps for when the record was created and last modified.

```java
// L19
    public BillingCycleResponse() {}
```
No-arg constructor (needed for Jackson / setter-based construction), empty body.

```java
// L21-L32
    public BillingCycleResponse(Long cycleId, Long accountId, LocalDate cycleStart, LocalDate cycleEnd,
                                LocalDate generatedDate, BillingCycleStatus status,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.cycleId = cycleId;
        this.accountId = accountId;
        this.cycleStart = cycleStart;
        this.cycleEnd = cycleEnd;
        this.generatedDate = generatedDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
```
All-args constructor; assigns each parameter to its matching field. The parameter order here is the same order the inner `Builder.build()` (below) uses to call it.

```java
// L34-L56
    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public LocalDate getCycleStart() { return cycleStart; }
    public void setCycleStart(LocalDate cycleStart) { this.cycleStart = cycleStart; }

    public LocalDate getCycleEnd() { return cycleEnd; }
    public void setCycleEnd(LocalDate cycleEnd) { this.cycleEnd = cycleEnd; }

    public LocalDate getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }

    public BillingCycleStatus getStatus() { return status; }
    public void setStatus(BillingCycleStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
```
Standard getter/setter pairs for all eight fields, driving the JSON property names on serialization.

```java
// L58
    public static Builder builder() { return new Builder(); }
```
A `static` factory method that returns a new `Builder` instance. This is the entry point of the hand-rolled **Builder pattern** — callers write `BillingCycleResponse.builder()....build()`. The Builder pattern allows fluent, readable construction where only the desired fields are set, instead of remembering the positional order of the 8-argument constructor. (This mirrors what Lombok's `@Builder` would generate automatically, but here it is written by hand.)

```java
// L60-L68
    public static class Builder {
        private Long cycleId;
        private Long accountId;
        private LocalDate cycleStart;
        private LocalDate cycleEnd;
        private LocalDate generatedDate;
        private BillingCycleStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
```
A `static` nested class `Builder`. Being `static` means it does not hold a reference to an enclosing `BillingCycleResponse` instance — it stands on its own. It mirrors the outer class's fields, accumulating values before the final object is built.

```java
// L70-L77
        public Builder cycleId(Long cycleId) { this.cycleId = cycleId; return this; }
        public Builder accountId(Long accountId) { this.accountId = accountId; return this; }
        public Builder cycleStart(LocalDate cycleStart) { this.cycleStart = cycleStart; return this; }
        public Builder cycleEnd(LocalDate cycleEnd) { this.cycleEnd = cycleEnd; return this; }
        public Builder generatedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; return this; }
        public Builder status(BillingCycleStatus status) { this.status = status; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
```
One fluent setter per field. Each stores the value and `return this;` so calls can be chained (`.cycleId(1L).accountId(5L)...`). This chaining is the whole point of the fluent Builder.

```java
// L79-L83
        public BillingCycleResponse build() {
            return new BillingCycleResponse(cycleId, accountId, cycleStart, cycleEnd, generatedDate,
                    status, createdAt, updatedAt);
        }
    }
}
```
`build()` is the terminal operation: it invokes the outer all-args constructor with the accumulated field values and returns a finished, immutable-in-practice `BillingCycleResponse`. Any field never set on the builder remains its default (`null` for the boxed/object types). The two closing braces end the `Builder` class and the outer class.

---

## src/main/java/com/teleconnect/billing_service/dto/response/CollectionReportResponse.java

Role: An aggregate financial report covering a date range (and optionally a region) — totals billed/collected/outstanding plus efficiency metrics — returned by a reporting endpoint.

```java
// L1-L4
package com.teleconnect.billing_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
```
Package, then imports. `BigDecimal` is the arbitrary-precision decimal type — the correct choice for money because it avoids the rounding errors of `double`/`float`. `LocalDate` is used for the report's date bounds.

```java
// L6
public class CollectionReportResponse {
```
Plain public POJO.

```java
// L8-L16
    private LocalDate fromDate;
    private LocalDate toDate;
    private String region;
    private BigDecimal totalBilled;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    private double collectionEfficiency;
    private int invoicesPaid;
    private int invoicesOverdue;
```
The nine fields:
- `fromDate` / `toDate` (`LocalDate`) — the reporting window.
- `region` (`String`) — the region the report is scoped to (may be `null`/all).
- `totalBilled` / `totalCollected` / `totalOutstanding` (`BigDecimal`) — the headline money totals.
- `collectionEfficiency` (`double`) — a ratio/percentage of how much of what was billed got collected. Using `double` here is acceptable because it is a derived statistic, not a stored monetary amount.
- `invoicesPaid` / `invoicesOverdue` (`int`) — counts of invoices in each state within the window.

```java
// L18
    public CollectionReportResponse() {}
```
Empty no-arg constructor.

```java
// L20-L33
    public CollectionReportResponse(LocalDate fromDate, LocalDate toDate, String region,
                                    BigDecimal totalBilled, BigDecimal totalCollected,
                                    BigDecimal totalOutstanding, double collectionEfficiency,
                                    int invoicesPaid, int invoicesOverdue) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.region = region;
        this.totalBilled = totalBilled;
        this.totalCollected = totalCollected;
        this.totalOutstanding = totalOutstanding;
        this.collectionEfficiency = collectionEfficiency;
        this.invoicesPaid = invoicesPaid;
        this.invoicesOverdue = invoicesOverdue;
    }
```
All-args constructor assigning each parameter to its field.

```java
// L35-L60
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public BigDecimal getTotalBilled() { return totalBilled; }
    public void setTotalBilled(BigDecimal totalBilled) { this.totalBilled = totalBilled; }

    public BigDecimal getTotalCollected() { return totalCollected; }
    public void setTotalCollected(BigDecimal totalCollected) { this.totalCollected = totalCollected; }

    public BigDecimal getTotalOutstanding() { return totalOutstanding; }
    public void setTotalOutstanding(BigDecimal totalOutstanding) { this.totalOutstanding = totalOutstanding; }

    public double getCollectionEfficiency() { return collectionEfficiency; }
    public void setCollectionEfficiency(double collectionEfficiency) { this.collectionEfficiency = collectionEfficiency; }

    public int getInvoicesPaid() { return invoicesPaid; }
    public void setInvoicesPaid(int invoicesPaid) { this.invoicesPaid = invoicesPaid; }

    public int getInvoicesOverdue() { return invoicesOverdue; }
    public void setInvoicesOverdue(int invoicesOverdue) { this.invoicesOverdue = invoicesOverdue; }
}
```
Getter/setter pair per field; these define the JSON keys. This class has **no Builder** — it is constructed via the all-args constructor or via setters. The final `}` closes the class.

---

## src/main/java/com/teleconnect/billing_service/dto/response/DataResponse.java

Role: A generic envelope wrapping any payload together with an HTTP-style status code — a reusable "result wrapper" the API can return for arbitrary data types.

```java
// L1
package com.teleconnect.billing_service.dto.response;
```
Package declaration. No imports are needed because the class uses only core types (`int`) and a generic type parameter.

```java
// L3
public class DataResponse<T> {
```
A **generic** class. The `<T>` is a *type parameter*: it lets the same class wrap any payload type, with the concrete type chosen at the use site — e.g. `DataResponse<InvoiceResponse>` or `DataResponse<List<PaymentResponse>>`. This avoids writing a separate envelope class per payload type and keeps compile-time type safety.

```java
// L5-L6
    private int statusCode;
    private T data;
```
Two fields:
- `statusCode` (`int`) — an HTTP-style status code carried inside the body (distinct from the actual HTTP response status).
- `data` (`T`) — the wrapped payload, whose type is whatever `T` resolves to at the call site.

```java
// L8
    public DataResponse() {}
```
No-arg constructor for Jackson/setter construction.

```java
// L10-L13
    public DataResponse(int statusCode, T data) {
        this.statusCode = statusCode;
        this.data = data;
    }
```
All-args constructor taking the status code and a `T` payload, assigning both.

```java
// L15-L19
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
```
Getter/setter pairs. `getData()` returns `T`, and `setData(T data)` accepts `T`, preserving the generic typing through the accessors. On serialization Jackson emits `{"statusCode": ..., "data": ...}` where `data` is the JSON form of whatever `T` was. The `}` closes the class.

---

## src/main/java/com/teleconnect/billing_service/dto/response/DisputeResponse.java

Role: The wire representation of a billing dispute (a customer's challenge of an invoice), returned when reading/listing disputes; the richest of these DTOs, with full lifecycle fields and a Builder.

```java
// L1-L7
package com.teleconnect.billing_service.dto.response;

import com.teleconnect.billing_service.enums.DisputeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
```
Package, then imports. `DisputeStatus` is the project enum for a dispute's state (e.g. open/under-review/resolved/rejected). `BigDecimal` is used for the disputed and resolved monetary amounts; `LocalDate` for the date a dispute was raised; `LocalDateTime` for the precise acknowledged/resolved timestamps.

```java
// L9
public class DisputeResponse {
```
Plain public POJO.

```java
// L11-L23
    private Long disputeId;
    private Long invoiceId;
    private Long subscriberId;
    private String disputeReason;
    private String description;
    private BigDecimal disputedAmount;
    private BigDecimal resolvedAmount;
    private LocalDate raisedDate;
    private LocalDateTime acknowledgedDate;
    private LocalDateTime resolvedDate;
    private String assignedTo;
    private String resolutionNotes;
    private DisputeStatus status;
```
Thirteen fields:
- `disputeId` (`Long`) — the dispute's primary key.
- `invoiceId` (`Long`) — id of the disputed invoice (flattened reference).
- `subscriberId` (`Long`) — id of the subscriber who raised it.
- `disputeReason` (`String`) — short reason/category for the dispute.
- `description` (`String`) — free-text detail.
- `disputedAmount` (`BigDecimal`) — the amount the subscriber is contesting.
- `resolvedAmount` (`BigDecimal`) — the amount actually granted/adjusted upon resolution (likely `null` until resolved).
- `raisedDate` (`LocalDate`) — the date the dispute was opened.
- `acknowledgedDate` (`LocalDateTime`) — when staff acknowledged it (relevant to SLA tracking).
- `resolvedDate` (`LocalDateTime`) — when it was resolved.
- `assignedTo` (`String`) — the agent/owner handling it.
- `resolutionNotes` (`String`) — notes recorded at resolution.
- `status` (`DisputeStatus`) — current lifecycle status.

```java
// L25
    public DisputeResponse() {}
```
No-arg constructor.

```java
// L27-L44
    public DisputeResponse(Long disputeId, Long invoiceId, Long subscriberId, String disputeReason,
                           String description, BigDecimal disputedAmount, BigDecimal resolvedAmount,
                           LocalDate raisedDate, LocalDateTime acknowledgedDate, LocalDateTime resolvedDate,
                           String assignedTo, String resolutionNotes, DisputeStatus status) {
        this.disputeId = disputeId;
        this.invoiceId = invoiceId;
        this.subscriberId = subscriberId;
        this.disputeReason = disputeReason;
        this.description = description;
        this.disputedAmount = disputedAmount;
        this.resolvedAmount = resolvedAmount;
        this.raisedDate = raisedDate;
        this.acknowledgedDate = acknowledgedDate;
        this.resolvedDate = resolvedDate;
        this.assignedTo = assignedTo;
        this.resolutionNotes = resolutionNotes;
        this.status = status;
    }
```
All-args constructor (13 parameters) assigning each to its field; the parameter order matches the order `Builder.build()` uses.

```java
// L46-L83
    public Long getDisputeId() { return disputeId; }
    public void setDisputeId(Long disputeId) { this.disputeId = disputeId; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public Long getSubscriberId() { return subscriberId; }
    public void setSubscriberId(Long subscriberId) { this.subscriberId = subscriberId; }

    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getDisputedAmount() { return disputedAmount; }
    public void setDisputedAmount(BigDecimal disputedAmount) { this.disputedAmount = disputedAmount; }

    public BigDecimal getResolvedAmount() { return resolvedAmount; }
    public void setResolvedAmount(BigDecimal resolvedAmount) { this.resolvedAmount = resolvedAmount; }

    public LocalDate getRaisedDate() { return raisedDate; }
    public void setRaisedDate(LocalDate raisedDate) { this.raisedDate = raisedDate; }

    public LocalDateTime getAcknowledgedDate() { return acknowledgedDate; }
    public void setAcknowledgedDate(LocalDateTime acknowledgedDate) { this.acknowledgedDate = acknowledgedDate; }

    public LocalDateTime getResolvedDate() { return resolvedDate; }
    public void setResolvedDate(LocalDateTime resolvedDate) { this.resolvedDate = resolvedDate; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public DisputeStatus getStatus() { return status; }
    public void setStatus(DisputeStatus status) { this.status = status; }
```
Getter/setter pairs for all thirteen fields, defining the JSON keys.

```java
// L85
    public static Builder builder() { return new Builder(); }
```
Static factory entry point for the Builder pattern, identical in shape to `BillingCycleResponse.builder()`.

```java
// L87-L100
    public static class Builder {
        private Long disputeId;
        private Long invoiceId;
        private Long subscriberId;
        private String disputeReason;
        private String description;
        private BigDecimal disputedAmount;
        private BigDecimal resolvedAmount;
        private LocalDate raisedDate;
        private LocalDateTime acknowledgedDate;
        private LocalDateTime resolvedDate;
        private String assignedTo;
        private String resolutionNotes;
        private DisputeStatus status;
```
Static nested `Builder` mirroring all thirteen outer fields.

```java
// L102-L114
        public Builder disputeId(Long disputeId) { this.disputeId = disputeId; return this; }
        public Builder invoiceId(Long invoiceId) { this.invoiceId = invoiceId; return this; }
        public Builder subscriberId(Long subscriberId) { this.subscriberId = subscriberId; return this; }
        public Builder disputeReason(String disputeReason) { this.disputeReason = disputeReason; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder disputedAmount(BigDecimal disputedAmount) { this.disputedAmount = disputedAmount; return this; }
        public Builder resolvedAmount(BigDecimal resolvedAmount) { this.resolvedAmount = resolvedAmount; return this; }
        public Builder raisedDate(LocalDate raisedDate) { this.raisedDate = raisedDate; return this; }
        public Builder acknowledgedDate(LocalDateTime acknowledgedDate) { this.acknowledgedDate = acknowledgedDate; return this; }
        public Builder resolvedDate(LocalDateTime resolvedDate) { this.resolvedDate = resolvedDate; return this; }
        public Builder assignedTo(String assignedTo) { this.assignedTo = assignedTo; return this; }
        public Builder resolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; return this; }
        public Builder status(DisputeStatus status) { this.status = status; return this; }
```
Fluent per-field setters, each returning `this` for chaining.

```java
// L116-L121
        public DisputeResponse build() {
            return new DisputeResponse(disputeId, invoiceId, subscriberId, disputeReason, description,
                    disputedAmount, resolvedAmount, raisedDate, acknowledgedDate, resolvedDate,
                    assignedTo, resolutionNotes, status);
        }
    }
}
```
`build()` calls the outer 13-arg constructor with all accumulated values and returns the finished `DisputeResponse`. Closing braces end the `Builder` and the outer class.

---

## src/main/java/com/teleconnect/billing_service/dto/response/DisputeSummaryResponse.java

Role: An aggregate disputes report over a date range — counts by outcome plus SLA (Service-Level Agreement) compliance metrics — returned by a dispute-analytics endpoint.

```java
// L1-L3
package com.teleconnect.billing_service.dto.response;

import java.time.LocalDate;
```
Package, then a single import of `LocalDate` for the report's date bounds. All other fields are primitives, so no further imports are needed.

```java
// L5
public class DisputeSummaryResponse {
```
Plain public POJO.

```java
// L7-L17
    private LocalDate fromDate;
    private LocalDate toDate;
    private int totalDisputes;
    private int acknowledgedWithin24h;
    private int resolvedWithin5Days;
    private int slaBreaches;
    private double slaComplianceRate;
    private int resolved;
    private int rejected;
    private int open;
    private int underReview;
```
Eleven fields:
- `fromDate` / `toDate` (`LocalDate`) — the reporting window.
- `totalDisputes` (`int`) — total disputes in the window.
- `acknowledgedWithin24h` (`int`) — disputes acknowledged within the 24-hour SLA.
- `resolvedWithin5Days` (`int`) — disputes resolved within the 5-day SLA.
- `slaBreaches` (`int`) — count of SLA violations.
- `slaComplianceRate` (`double`) — overall SLA compliance ratio/percentage (a derived statistic, hence `double`).
- `resolved` / `rejected` / `open` / `underReview` (`int`) — counts of disputes by their `DisputeStatus` outcome. These names correspond to dispute statuses but are plain counts here, not enum-typed.

```java
// L19
    public DisputeSummaryResponse() {}
```
No-arg constructor.

```java
// L21-L36
    public DisputeSummaryResponse(LocalDate fromDate, LocalDate toDate, int totalDisputes,
                                  int acknowledgedWithin24h, int resolvedWithin5Days, int slaBreaches,
                                  double slaComplianceRate, int resolved, int rejected,
                                  int open, int underReview) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.totalDisputes = totalDisputes;
        this.acknowledgedWithin24h = acknowledgedWithin24h;
        this.resolvedWithin5Days = resolvedWithin5Days;
        this.slaBreaches = slaBreaches;
        this.slaComplianceRate = slaComplianceRate;
        this.resolved = resolved;
        this.rejected = rejected;
        this.open = open;
        this.underReview = underReview;
    }
```
All-args constructor (11 parameters) assigning each to its field.

```java
// L38-L69
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public int getTotalDisputes() { return totalDisputes; }
    public void setTotalDisputes(int totalDisputes) { this.totalDisputes = totalDisputes; }

    public int getAcknowledgedWithin24h() { return acknowledgedWithin24h; }
    public void setAcknowledgedWithin24h(int acknowledgedWithin24h) { this.acknowledgedWithin24h = acknowledgedWithin24h; }

    public int getResolvedWithin5Days() { return resolvedWithin5Days; }
    public void setResolvedWithin5Days(int resolvedWithin5Days) { this.resolvedWithin5Days = resolvedWithin5Days; }

    public int getSlaBreaches() { return slaBreaches; }
    public void setSlaBreaches(int slaBreaches) { this.slaBreaches = slaBreaches; }

    public double getSlaComplianceRate() { return slaComplianceRate; }
    public void setSlaComplianceRate(double slaComplianceRate) { this.slaComplianceRate = slaComplianceRate; }

    public int getResolved() { return resolved; }
    public void setResolved(int resolved) { this.resolved = resolved; }

    public int getRejected() { return rejected; }
    public void setRejected(int rejected) { this.rejected = rejected; }

    public int getOpen() { return open; }
    public void setOpen(int open) { this.open = open; }

    public int getUnderReview() { return underReview; }
    public void setUnderReview(int underReview) { this.underReview = underReview; }
}
```
Getter/setter pairs for all eleven fields, defining the JSON keys. No Builder here — constructed via the all-args constructor or setters. The `}` closes the class.

---

## src/main/java/com/teleconnect/billing_service/dto/response/InvoiceResponse.java

Role: The wire representation of an invoice — its charge breakdown, totals, due date, and status — returned whenever a client reads/lists invoices or after one is generated; includes a Builder.

```java
// L1-L6
package com.teleconnect.billing_service.dto.response;

import com.teleconnect.billing_service.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
```
Package, then imports. `InvoiceStatus` is the project enum for an invoice's lifecycle (e.g. issued/paid/overdue). `BigDecimal` is used for every monetary field (precise money). `LocalDate` is used for the due date.

```java
// L8
public class InvoiceResponse {
```
Plain public POJO.

```java
// L10-L21
    private Long invoiceId;
    private Long accountId;
    private Long cycleId;
    private BigDecimal planCharges;
    private BigDecimal excessCharges;
    private BigDecimal addOnCharges;
    private BigDecimal taxes;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal lateFee;
    private LocalDate dueDate;
    private InvoiceStatus status;
```
Twelve fields:
- `invoiceId` (`Long`) — invoice primary key.
- `accountId` (`Long`) — owning account id.
- `cycleId` (`Long`) — the billing cycle this invoice belongs to.
- `planCharges` (`BigDecimal`) — base subscription/plan charges.
- `excessCharges` (`BigDecimal`) — overage charges (usage beyond plan).
- `addOnCharges` (`BigDecimal`) — charges for add-on services.
- `taxes` (`BigDecimal`) — tax component.
- `totalAmount` (`BigDecimal`) — the invoice grand total.
- `paidAmount` (`BigDecimal`) — how much has been paid so far.
- `lateFee` (`BigDecimal`) — any late-payment fee applied.
- `dueDate` (`LocalDate`) — when payment is due.
- `status` (`InvoiceStatus`) — current invoice lifecycle status.

```java
// L23
    public InvoiceResponse() {}
```
No-arg constructor.

```java
// L25-L41
    public InvoiceResponse(Long invoiceId, Long accountId, Long cycleId, BigDecimal planCharges,
                           BigDecimal excessCharges, BigDecimal addOnCharges, BigDecimal taxes,
                           BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal lateFee,
                           LocalDate dueDate, InvoiceStatus status) {
        this.invoiceId = invoiceId;
        this.accountId = accountId;
        this.cycleId = cycleId;
        this.planCharges = planCharges;
        this.excessCharges = excessCharges;
        this.addOnCharges = addOnCharges;
        this.taxes = taxes;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.lateFee = lateFee;
        this.dueDate = dueDate;
        this.status = status;
    }
```
All-args constructor (12 parameters), assigning each to its field; order matches `Builder.build()`.

```java
// L43-L77
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public BigDecimal getPlanCharges() { return planCharges; }
    public void setPlanCharges(BigDecimal planCharges) { this.planCharges = planCharges; }

    public BigDecimal getExcessCharges() { return excessCharges; }
    public void setExcessCharges(BigDecimal excessCharges) { this.excessCharges = excessCharges; }

    public BigDecimal getAddOnCharges() { return addOnCharges; }
    public void setAddOnCharges(BigDecimal addOnCharges) { this.addOnCharges = addOnCharges; }

    public BigDecimal getTaxes() { return taxes; }
    public void setTaxes(BigDecimal taxes) { this.taxes = taxes; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getLateFee() { return lateFee; }
    public void setLateFee(BigDecimal lateFee) { this.lateFee = lateFee; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
```
Getter/setter pairs for all twelve fields, defining the JSON keys.

```java
// L79
    public static Builder builder() { return new Builder(); }
```
Static factory for the Builder pattern.

```java
// L81-L93
    public static class Builder {
        private Long invoiceId;
        private Long accountId;
        private Long cycleId;
        private BigDecimal planCharges;
        private BigDecimal excessCharges;
        private BigDecimal addOnCharges;
        private BigDecimal taxes;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal lateFee;
        private LocalDate dueDate;
        private InvoiceStatus status;
```
Static nested `Builder` mirroring the twelve outer fields.

```java
// L95-L106
        public Builder invoiceId(Long invoiceId) { this.invoiceId = invoiceId; return this; }
        public Builder accountId(Long accountId) { this.accountId = accountId; return this; }
        public Builder cycleId(Long cycleId) { this.cycleId = cycleId; return this; }
        public Builder planCharges(BigDecimal planCharges) { this.planCharges = planCharges; return this; }
        public Builder excessCharges(BigDecimal excessCharges) { this.excessCharges = excessCharges; return this; }
        public Builder addOnCharges(BigDecimal addOnCharges) { this.addOnCharges = addOnCharges; return this; }
        public Builder taxes(BigDecimal taxes) { this.taxes = taxes; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder paidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; return this; }
        public Builder lateFee(BigDecimal lateFee) { this.lateFee = lateFee; return this; }
        public Builder dueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
        public Builder status(InvoiceStatus status) { this.status = status; return this; }
```
Fluent per-field setters returning `this` for chaining.

```java
// L108-L113
        public InvoiceResponse build() {
            return new InvoiceResponse(invoiceId, accountId, cycleId, planCharges, excessCharges,
                    addOnCharges, taxes, totalAmount, paidAmount, lateFee, dueDate, status);
        }
    }
}
```
`build()` invokes the 12-arg constructor with the accumulated values and returns the finished `InvoiceResponse`. Closing braces end the `Builder` and outer class.

---

## src/main/java/com/teleconnect/billing_service/dto/response/MessageResponse.java

Role: A tiny envelope carrying a single human-readable message string — used for simple acknowledgements or status messages in API responses.

```java
// L1
package com.teleconnect.billing_service.dto.response;
```
Package declaration. No imports — uses only `String`.

```java
// L3
public class MessageResponse {
```
Plain public POJO.

```java
// L5
    private String message;
```
The single field: `message` (`String`), the text to return (e.g. `"Invoice generated successfully"`).

```java
// L7
    public MessageResponse() {}
```
No-arg constructor.

```java
// L9-L11
    public MessageResponse(String message) {
        this.message = message;
    }
```
All-args (single-arg) constructor assigning the message. This is the typical way the API builds a quick `new MessageResponse("...")`.

```java
// L13-L14
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
```
Getter/setter for `message`, producing the JSON `{"message": "..."}`. The `}` closes the class.

---

## src/main/java/com/teleconnect/billing_service/dto/response/OverdueReportResponse.java

Role: A report of overdue invoices for an aging bucket / region — header totals plus a list of per-invoice line items — returned by an overdue/aging report endpoint. It also defines its own nested line-item type.

```java
// L1-L5
package com.teleconnect.billing_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
```
Package, then imports. `BigDecimal` for monetary totals/amounts; `LocalDate` for due dates; `List` (the `java.util` collection interface) to hold the collection of line items.

```java
// L7
public class OverdueReportResponse {
```
Plain public POJO (the report header/container).

```java
// L9-L13
    private String agingBucket;
    private String region;
    private int totalOverdueCount;
    private BigDecimal totalOverdueAmount;
    private List<OverdueInvoiceItem> content;
```
Five fields:
- `agingBucket` (`String`) — the aging band this report covers (e.g. `"0-30"`, `"31-60"` days).
- `region` (`String`) — the region scope.
- `totalOverdueCount` (`int`) — total number of overdue invoices.
- `totalOverdueAmount` (`BigDecimal`) — total overdue money.
- `content` (`List<OverdueInvoiceItem>`) — the detailed rows; the element type is the nested class defined below. The name `content` mirrors Spring Data's `Page.content` convention for a list of results.

```java
// L15
    public OverdueReportResponse() {}
```
No-arg constructor.

```java
// L17-L24
    public OverdueReportResponse(String agingBucket, String region, int totalOverdueCount,
                                 BigDecimal totalOverdueAmount, List<OverdueInvoiceItem> content) {
        this.agingBucket = agingBucket;
        this.region = region;
        this.totalOverdueCount = totalOverdueCount;
        this.totalOverdueAmount = totalOverdueAmount;
        this.content = content;
    }
```
All-args constructor assigning each parameter, including the whole list reference.

```java
// L26-L39
    public String getAgingBucket() { return agingBucket; }
    public void setAgingBucket(String agingBucket) { this.agingBucket = agingBucket; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public int getTotalOverdueCount() { return totalOverdueCount; }
    public void setTotalOverdueCount(int totalOverdueCount) { this.totalOverdueCount = totalOverdueCount; }

    public BigDecimal getTotalOverdueAmount() { return totalOverdueAmount; }
    public void setTotalOverdueAmount(BigDecimal totalOverdueAmount) { this.totalOverdueAmount = totalOverdueAmount; }

    public List<OverdueInvoiceItem> getContent() { return content; }
    public void setContent(List<OverdueInvoiceItem> content) { this.content = content; }
```
Getter/setter pairs for the five header fields. `getContent()` returns the list, which Jackson serializes as a JSON array of item objects. No Builder on the outer class.

```java
// L41-L46
    public static class OverdueInvoiceItem {
        private Long invoiceId;
        private Long accountId;
        private BigDecimal totalAmount;
        private LocalDate dueDate;
        private long daysOverdue;
```
A `static` nested class `OverdueInvoiceItem` representing one overdue-invoice row inside the report. Being `static`, it is independent of any outer instance and can be referenced as `OverdueReportResponse.OverdueInvoiceItem`. Its fields:
- `invoiceId` (`Long`) — the overdue invoice's id.
- `accountId` (`Long`) — the owning account's id.
- `totalAmount` (`BigDecimal`) — the invoice total.
- `dueDate` (`LocalDate`) — when it was due.
- `daysOverdue` (`long`) — how many days past due. Note this is the primitive `long` (not boxed `Long`), so it is always a number and never `null`; a day count fits comfortably in `long`.

```java
// L48
        public OverdueInvoiceItem() {}
```
No-arg constructor for the nested item.

```java
// L50-L57
        public OverdueInvoiceItem(Long invoiceId, Long accountId, BigDecimal totalAmount,
                                  LocalDate dueDate, long daysOverdue) {
            this.invoiceId = invoiceId;
            this.accountId = accountId;
            this.totalAmount = totalAmount;
            this.dueDate = dueDate;
            this.daysOverdue = daysOverdue;
        }
```
All-args constructor for the item, assigning each of its five fields.

```java
// L59-L72
        public Long getInvoiceId() { return invoiceId; }
        public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

        public long getDaysOverdue() { return daysOverdue; }
        public void setDaysOverdue(long daysOverdue) { this.daysOverdue = daysOverdue; }
    }
}
```
Getter/setter pairs for the item's five fields, defining the JSON keys for each element in the `content` array. The two closing braces end the nested `OverdueInvoiceItem` class and the outer `OverdueReportResponse` class.

---

## src/main/java/com/teleconnect/billing_service/dto/response/PaymentResponse.java

Role: The wire representation of a payment recorded against an invoice, returned after a payment is made or when payments are read; the only DTO here with a Jackson formatting annotation, plus a Builder.

```java
// L1-L8
package com.teleconnect.billing_service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.teleconnect.billing_service.enums.PaymentMethod;
import com.teleconnect.billing_service.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
```
Package, then imports. `JsonFormat` comes from Jackson (the JSON library Spring uses) and lets you control how a field is serialized/deserialized. `PaymentMethod` and `PaymentStatus` are project enums for how the payment was made (e.g. card/bank/UPI) and its state (e.g. pending/success/failed). `BigDecimal` is the precise money type for the amount; `LocalDateTime` for the payment timestamp.

```java
// L10
public class PaymentResponse {
```
Plain public POJO.

```java
// L12-L14
    private Long paymentId;
    private Long invoiceId;
    private BigDecimal amountPaid;
```
Three of the fields:
- `paymentId` (`Long`) — the payment's primary key.
- `invoiceId` (`Long`) — id of the invoice this payment applies to.
- `amountPaid` (`BigDecimal`) — the amount of this payment.

```java
// L16-L17
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentDate;
```
The `paymentDate` field (`LocalDateTime`), annotated with `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`. `@JsonFormat` tells Jackson exactly how to render this date/time in JSON: instead of the default ISO-8601 form (`2026-06-15T14:30:00`), it will be emitted as a space-separated string like `2026-06-15 14:30:00`, and parsed back using the same pattern on input. This is the single framework annotation in the entire response-DTO group; the other date fields across these DTOs use Jackson's default formatting.

```java
// L19-L21
    private PaymentMethod paymentMethod;
    private String transactionRef;
    private PaymentStatus status;
```
The remaining three fields:
- `paymentMethod` (`PaymentMethod` enum) — how the payment was made.
- `transactionRef` (`String`) — the external/gateway transaction reference.
- `status` (`PaymentStatus` enum) — the payment's current state.

```java
// L23
    public PaymentResponse() {}
```
No-arg constructor.

```java
// L25-L35
    public PaymentResponse(Long paymentId, Long invoiceId, BigDecimal amountPaid,
                           LocalDateTime paymentDate, PaymentMethod paymentMethod,
                           String transactionRef, PaymentStatus status) {
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.transactionRef = transactionRef;
        this.status = status;
    }
```
All-args constructor (7 parameters) assigning each field; order matches `Builder.build()`.

```java
// L37-L56
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
```
Getter/setter pairs for all seven fields. The `@JsonFormat` on the field applies regardless of the accessor, so `getPaymentDate()`'s value serializes with the custom pattern.

```java
// L58
    public static Builder builder() { return new Builder(); }
```
Static factory for the Builder pattern.

```java
// L60-L67
    public static class Builder {
        private Long paymentId;
        private Long invoiceId;
        private BigDecimal amountPaid;
        private LocalDateTime paymentDate;
        private PaymentMethod paymentMethod;
        private String transactionRef;
        private PaymentStatus status;
```
Static nested `Builder` mirroring the seven outer fields. *(Note: the builder's `paymentDate` field is not itself annotated with `@JsonFormat`, but that is irrelevant — the builder is never serialized; only the constructed `PaymentResponse`, whose field carries the annotation, is.)*

```java
// L69-L75
        public Builder paymentId(Long paymentId) { this.paymentId = paymentId; return this; }
        public Builder invoiceId(Long invoiceId) { this.invoiceId = invoiceId; return this; }
        public Builder amountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; return this; }
        public Builder paymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; return this; }
        public Builder paymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public Builder transactionRef(String transactionRef) { this.transactionRef = transactionRef; return this; }
        public Builder status(PaymentStatus status) { this.status = status; return this; }
```
Fluent per-field setters, each returning `this` for chaining.

```java
// L77-L82
        public PaymentResponse build() {
            return new PaymentResponse(paymentId, invoiceId, amountPaid, paymentDate,
                    paymentMethod, transactionRef, status);
        }
    }
}
```
`build()` calls the 7-arg constructor with the accumulated values and returns the finished `PaymentResponse`. Closing braces end the `Builder` and outer class.

---

## How this connects

These response DTOs sit at the **outermost edge** of the Controller -> Service -> Repository -> Entity/DB stack, on the *return* path:

- The **Repository** layer (Spring Data JPA) loads JPA **entities** from the database.
- The **Service** layer applies business logic and **maps** those entities (and computed aggregates) into the DTOs in this package — e.g. an `Invoice` entity becomes an `InvoiceResponse`, often via the fluent `...builder()....build()` shown above. Aggregate/reporting endpoints build `CollectionReportResponse`, `DisputeSummaryResponse`, `OverdueReportResponse`, or `BatchGenerationResponse` from query results.
- The **Controller** layer returns these DTOs to clients, typically wrapped in a `ResponseEntity<T>` (Spring's HTTP-response holder that bundles status code, headers, and body) or inside the generic `DataResponse<T>` envelope. Spring's Jackson `HttpMessageConverter` then serializes the DTO to JSON using the JavaBeans getters, the enum names, and the one `@JsonFormat` pattern on `PaymentResponse.paymentDate`.

Because these classes are decoupled from the entities, the persistence model (table columns, relationships, lazy loading) can evolve without changing the public API contract, and internal fields are never leaked to clients. The companion **request DTOs** (in the sibling `dto/request` package) handle the inbound direction; these response DTOs handle everything flowing back out. None of these classes carry persistence, validation, or Lombok annotations — they are deliberately minimal data carriers.
