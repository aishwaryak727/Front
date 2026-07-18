# Entity: `BillingDispute`

`BillingDispute` is a JPA (Java Persistence API) **entity** — a plain Java class whose instances map one-to-one to rows of the `billing_disputes` database table. In the layered architecture of this service (Controller -> Service -> Repository -> Entity/DB), it sits at the very bottom: Controllers receive HTTP requests, Services apply business logic, Repositories (Spring Data JPA) issue the actual SQL, and entities like this one are the in-memory representation of the persisted rows that flow back and forth. This particular entity models a customer's *dispute* against a billing invoice — who raised it, how much money is contested, its lifecycle status, and how/when it was resolved.

## src/main/java/com/teleconnect/billing_service/entity/BillingDispute.java

This file declares the `BillingDispute` JPA entity, its persistent fields, two constructors, standard getters/setters, and a hand-written nested `Builder` for fluent object construction.

```java
// L1
package com.teleconnect.billing_service.entity;
```

The `package` statement declares the namespace this class lives in. The fully qualified name of the class becomes `com.teleconnect.billing_service.entity.BillingDispute`, and the file must physically sit in the matching directory `.../entity/`. Grouping all entities under the `entity` package is a common Spring convention that keeps the persistence model separate from controllers, services, and repositories.

```java
// L3-L8
import com.teleconnect.billing_service.enums.DisputeStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
```

These are the imports that bring in the types used below.
- `DisputeStatus` is a project-local enum (see "How this connects") with the four constants `OPEN, UNDER_REVIEW, RESOLVED, REJECTED`; it is used for the `status` field to represent the dispute's lifecycle stage.
- `jakarta.persistence.*` is a wildcard import of the **Jakarta Persistence** API (the modern successor to `javax.persistence` used by Spring Boot 3+). It brings in every persistence annotation used here: `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `GenerationType`, `@Column`, `@Enumerated`, and `EnumType`.
- `java.math.BigDecimal` is the arbitrary-precision decimal type used for the monetary amounts. `BigDecimal` (rather than `double`/`float`) is the correct choice for money because it avoids binary floating-point rounding errors.
- `java.time.LocalDate` represents a date with no time-of-day (year-month-day), used for the day a dispute was raised. `java.time.LocalDateTime` represents a date plus a time-of-day with no timezone, used for the more precise acknowledgement/resolution timestamps.

```java
// L10-L12
@Entity
@Table(name = "billing_disputes")
public class BillingDispute {
```

- `@Entity` marks this class as a JPA entity. This tells the JPA provider (Hibernate, the default in Spring Boot) to manage instances of this class as persistent objects: each instance corresponds to a table row, and the provider will generate the SQL to read/insert/update/delete them. An entity must have a no-argument constructor and a primary key.
- `@Table(name = "billing_disputes")` overrides the default table name. Without it, Hibernate would derive a table name from the class name (`BillingDispute`); here it is pinned explicitly to `billing_disputes`. The `name` attribute is the exact SQL table name this entity is stored in.
- `public class BillingDispute {` begins the class body. It does not extend a base class or implement any interface — it is a self-contained POJO.

```java
// L14-L16
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long disputeId;
```

This is the **primary key** field.
- `@Id` designates `disputeId` as the entity's unique identifier — the column JPA uses to distinguish one row from another.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` tells JPA the database itself generates the key value. `GenerationType.IDENTITY` means an auto-increment / identity column: the database assigns the next value on `INSERT`, and Hibernate reads it back. As a consequence, `disputeId` is `null` for a brand-new (unsaved) object and only gets populated after persistence.
- The field type is `Long` (the boxed wrapper, not primitive `long`) so it can legitimately hold `null` before the row is inserted. The column name defaults to `dispute_id` because Hibernate's default naming strategy converts the camelCase Java name to snake_case SQL.

```java
// L18-L19
    @Column(nullable = false)
    private Long invoiceId;
```

`invoiceId` is the foreign-key-style reference to the invoice this dispute is about. `@Column` customizes the mapping of this field to its database column; here `nullable = false` adds a `NOT NULL` constraint, so every dispute must reference an invoice. *Note: this is a plain `Long` value, not a JPA association (`@ManyToOne`), so the entity stores only the raw invoice id and does not navigate to an `Invoice` object directly.* The column name defaults to `invoice_id`.

```java
// L21-L22
    @Column(nullable = false)
    private Long subscriberId;
```

`subscriberId` is the id of the subscriber (customer) who raised the dispute. `@Column(nullable = false)` enforces `NOT NULL`. Like `invoiceId`, this is a raw id rather than a mapped relationship; column name defaults to `subscriber_id`.

```java
// L24-L25
    @Column(nullable = false, length = 1000)
    private String disputeReason;
```

`disputeReason` is a required short text explaining why the dispute was raised. `nullable = false` makes it mandatory; `length = 1000` sets the column's character capacity (e.g. `VARCHAR(1000)`), bounding how long the reason can be. Column name defaults to `dispute_reason`.

```java
// L27-L28
    @Column(length = 2000)
    private String description;
```

`description` is an optional, longer free-text elaboration on the dispute. There is no `nullable = false`, so the column is nullable by default (the dispute can be saved without a description). `length = 2000` allows up to 2000 characters. Column name defaults to `description`.

```java
// L30-L31
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal disputedAmount;
```

`disputedAmount` is the monetary amount the customer is contesting. It is required (`nullable = false`). For a `BigDecimal`, `precision = 10` is the total number of significant digits and `scale = 2` is the number of digits after the decimal point — i.e. a `DECIMAL(10,2)` column that holds values up to 99,999,999.99 with two decimal places (cents). Column name defaults to `disputed_amount`.

```java
// L33-L34
    @Column(precision = 10, scale = 2)
    private BigDecimal resolvedAmount;
```

`resolvedAmount` is the amount actually credited/agreed once the dispute is resolved. It is nullable (no `nullable = false`), which makes sense because it is unknown while the dispute is still open. Same `DECIMAL(10,2)` precision/scale as `disputedAmount`. Column name defaults to `resolved_amount`.

```java
// L36-L37
    @Column(nullable = false)
    private LocalDate raisedDate;
```

`raisedDate` is the calendar date the dispute was filed. Required (`nullable = false`). The type `LocalDate` maps to a SQL `DATE` column (no time component). Column name defaults to `raised_date`.

```java
// L39-L40
    @Column
    private LocalDateTime acknowledgedDate;
```

`acknowledgedDate` records when the dispute was acknowledged by the billing team. A bare `@Column` (no attributes) is equivalent to relying on all defaults — nullable, default name (`acknowledged_date`). It is nullable because acknowledgement happens after the dispute is created. `LocalDateTime` maps to a `TIMESTAMP`-style column capturing both date and time.

```java
// L42-L43
    @Column
    private LocalDateTime resolvedDate;
```

`resolvedDate` records when the dispute reached a final state. Like `acknowledgedDate`, it is a nullable `LocalDateTime` timestamp (`resolved_date`), populated only once the dispute is resolved or rejected.

```java
// L45-L46
    @Column(length = 500)
    private String assignedTo;
```

`assignedTo` holds the identifier/name of the agent or team handling the dispute. Optional (nullable) with a `length = 500` cap. Column name defaults to `assigned_to`.

```java
// L48-L49
    @Column(length = 2000)
    private String resolutionNotes;
```

`resolutionNotes` is optional free text describing how the dispute was settled, up to 2000 characters. Column name defaults to `resolution_notes`.

```java
// L51-L53
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status;
```

`status` is the dispute's lifecycle stage, typed as the `DisputeStatus` enum.
- `@Enumerated(EnumType.STRING)` tells JPA how to persist an enum. `EnumType.STRING` stores the enum *constant's name* as text (e.g. the literal `"OPEN"`, `"RESOLVED"`). This is the safer choice over `EnumType.ORDINAL` (which would store the integer position `0,1,2,3`), because adding or reordering enum constants later would not corrupt existing rows.
- `@Column(nullable = false)` makes the status mandatory — every dispute must always carry a valid status. Column name defaults to `status`.

```java
// L55
    public BillingDispute() {}
```

This is the **no-argument constructor**. JPA requires every entity to have an accessible no-arg constructor so the provider (Hibernate) can instantiate a blank object via reflection and then populate its fields when materializing a row from the database. The empty body means it creates an object with all fields at their defaults (`null`).

```java
// L57-L74
    public BillingDispute(Long disputeId, Long invoiceId, Long subscriberId, String disputeReason,
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

This is the **all-arguments constructor**. It takes one parameter for every field (in declaration order) and assigns each to the corresponding instance field using the `this.` prefix to disambiguate the field from the same-named parameter. It performs no validation or transformation — it is a straight field-by-field copy. It is used internally by the nested `Builder.build()` method (L146-L150) to assemble a fully populated instance in one call, and could also be called directly by any code that has all 13 values.

```java
// L76-L83
    public Long getDisputeId() { return disputeId; }
    public void setDisputeId(Long disputeId) { this.disputeId = disputeId; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public Long getSubscriberId() { return subscriberId; }
    public void setSubscriberId(Long subscriberId) { this.subscriberId = subscriberId; }
```

These are standard **JavaBean accessor/mutator** (getter/setter) pairs for the id-bearing fields. Each getter returns the field's current value; each setter overwrites it. JPA and serialization libraries (e.g. Jackson, which turns the object into JSON for HTTP responses) rely on these public getters/setters to read and write field values. There is no extra logic in any of them.

```java
// L85-L95
    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getDisputedAmount() { return disputedAmount; }
    public void setDisputedAmount(BigDecimal disputedAmount) { this.disputedAmount = disputedAmount; }

    public BigDecimal getResolvedAmount() { return resolvedAmount; }
    public void setResolvedAmount(BigDecimal resolvedAmount) { this.resolvedAmount = resolvedAmount; }
```

The same trivial getter/setter pattern for the textual reason/description fields and the two monetary `BigDecimal` fields. No conversion, rounding, or validation occurs in these accessors — callers are responsible for supplying correctly scaled `BigDecimal` values.

```java
// L97-L107
    public LocalDate getRaisedDate() { return raisedDate; }
    public void setRaisedDate(LocalDate raisedDate) { this.raisedDate = raisedDate; }

    public LocalDateTime getAcknowledgedDate() { return acknowledgedDate; }
    public void setAcknowledgedDate(LocalDateTime acknowledgedDate) { this.acknowledgedDate = acknowledgedDate; }

    public LocalDateTime getResolvedDate() { return resolvedDate; }
    public void setResolvedDate(LocalDateTime resolvedDate) { this.resolvedDate = resolvedDate; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
```

Plain getter/setter pairs for the date/time fields (`raisedDate`, `acknowledgedDate`, `resolvedDate`) and `assignedTo`. They simply read and write the underlying fields with no side effects.

```java
// L109-L113
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public DisputeStatus getStatus() { return status; }
    public void setStatus(DisputeStatus status) { this.status = status; }
```

The final getter/setter pairs, covering `resolutionNotes` and the `status` enum. `getStatus()` returns the current `DisputeStatus`, and `setStatus(...)` replaces it — this is the typical hook a service layer uses to transition a dispute (e.g. from `OPEN` to `UNDER_REVIEW` to `RESOLVED`).

```java
// L115
    public static Builder builder() { return new Builder(); }
```

This is a **static factory method** that returns a fresh `Builder` instance. It is the entry point to the fluent builder pattern: calling code writes `BillingDispute.builder()....build()` instead of invoking the long 13-argument constructor positionally. Being `static`, it is called on the class itself, not on an instance. *Note: this is a hand-written equivalent of what Lombok's `@Builder` annotation would generate; here it is implemented manually rather than via Lombok.*

```java
// L117-L130
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

This declares a **static nested class** `Builder` and its private mirror fields — one for every field of the enclosing `BillingDispute`. Because it is `static`, a `Builder` exists independently of any `BillingDispute` instance; it acts as a temporary, mutable scratch object that accumulates values before the immutable-ish final object is constructed. Each field starts at `null` and is filled in only if the caller invokes the matching setter method.

```java
// L132-L144
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

These are the **fluent setter methods**, one per field. Each one assigns its argument to the builder's corresponding field and then `return this;` — returning the same `Builder` object so calls can be chained, e.g. `builder().invoiceId(5L).disputedAmount(amount).status(DisputeStatus.OPEN)`. None of them perform validation; any field left unset simply remains `null`. This is the classic builder idiom for constructing an object with many optional parameters readably.

```java
// L146-L151
        public BillingDispute build() {
            return new BillingDispute(disputeId, invoiceId, subscriberId, disputeReason, description,
                    disputedAmount, resolvedAmount, raisedDate, acknowledgedDate, resolvedDate,
                    assignedTo, resolutionNotes, status);
        }
    }
```

`build()` is the terminal method of the builder. It takes no parameters and returns a new `BillingDispute` by passing all 13 accumulated builder fields, in declaration order, into the all-args constructor (L57). It does no checking of required fields, so it is the caller's responsibility (or the database's `NOT NULL` constraints at insert time) to ensure mandatory values like `invoiceId`, `subscriberId`, `disputeReason`, `disputedAmount`, `raisedDate`, and `status` are present. The closing brace ends the `Builder` nested class.

```java
// L152
}
```

The final brace closes the `BillingDispute` class.

## How this connects

- **DisputeStatus enum** (`src/main/java/com/teleconnect/billing_service/enums/DisputeStatus.java`): defines the four states a dispute can be in — `OPEN`, `UNDER_REVIEW`, `RESOLVED`, `REJECTED`. Persisted as text via `@Enumerated(EnumType.STRING)` on the `status` field.
- **Repository layer**: a Spring Data JPA repository (typically `BillingDisputeRepository extends JpaRepository<BillingDispute, Long>`) uses this entity as its domain type and `Long` as its id type to provide CRUD/query operations that translate to SQL against the `billing_disputes` table.
- **Service layer**: services orchestrate business logic — creating a dispute (often via `BillingDispute.builder()`), transitioning its `status`, recording `acknowledgedDate`/`resolvedDate`/`resolvedAmount`/`resolutionNotes`, then handing the entity to the repository to persist.
- **Controller layer**: REST controllers convert incoming HTTP/JSON into (or from) this entity (or an associated DTO) using its getters/setters, and return it as part of HTTP responses.
- **Database**: each instance maps to a row of `billing_disputes`, with the primary key `dispute_id` auto-generated by the database identity column.
