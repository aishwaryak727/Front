# Entity: `Invoice`

This file defines the `Invoice` JPA entity — the core billing document of the service. It is a plain Java class annotated so that the Java Persistence API (Jakarta Persistence, formerly JPA) maps each instance to one row in the `invoices` database table. In the layered architecture (Controller → Service → Repository → Entity/DB), this class lives at the very bottom: Controllers receive HTTP requests, Services hold business logic, Repositories (Spring Data JPA interfaces) read and write `Invoice` objects, and this entity is the in-memory representation that the persistence provider (Hibernate) converts to and from table rows.

## src/main/java/com/teleconnect/billing_service/entity/Invoice.java

The JPA entity class mapping the `invoices` table. It holds the financial fields of a single invoice (charges, taxes, totals, payment, late fee), lifecycle timestamps, a status enum, and offers both a manual builder and JPA lifecycle callbacks.

```java
// L1
package com.teleconnect.billing_service.entity;
```

The `package` declaration places this class in the `com.teleconnect.billing_service.entity` namespace. The package name corresponds to the folder path on disk and groups all JPA entities together. Other classes refer to this type as `com.teleconnect.billing_service.entity.Invoice` unless they `import` it.

```java
// L3-L8
import com.teleconnect.billing_service.enums.InvoiceStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
```

These `import` statements bring in the types used below:
- `InvoiceStatus` — the project's own enum (defined in the `enums` package) listing the legal lifecycle states of an invoice: `GENERATED, SENT, PAID, OVERDUE, DISPUTED`.
- `jakarta.persistence.*` — the wildcard import for the Jakarta Persistence API. This is where every JPA annotation used in this file comes from: `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@GenerationType`, `@Column`, `@Enumerated`, `@EnumType`, `@PrePersist`, and `@PreUpdate`. (Jakarta Persistence is the renamed successor to `javax.persistence`, used by Spring Boot 3+.)
- `java.math.BigDecimal` — an arbitrary-precision decimal type. It is used for all monetary fields because `double`/`float` introduce rounding errors that are unacceptable for currency.
- `java.time.LocalDate` — a date with no time component (year-month-day), used for the due date.
- `java.time.LocalDateTime` — a date plus time of day with no timezone, used for the created/updated audit timestamps.

```java
// L10-L12
@Entity
@Table(name = "invoices")
public class Invoice {
```

`@Entity` marks this class as a JPA entity, meaning the persistence provider (Hibernate, under Spring Data JPA) will manage instances of it and map them to a database table. Without this annotation the class would be an ordinary POJO and the repository layer could not persist it.

`@Table(name = "invoices")` explicitly names the backing table `invoices`. Without it, JPA would default the table name to the class name (`Invoice` / `invoice`); naming it explicitly makes the mapping unambiguous and lets the table follow a plural convention.

`public class Invoice` opens the class definition.

```java
// L14-L16
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;
```

`@Id` designates `invoiceId` as the table's primary key — the column that uniquely identifies each row. Every JPA entity must have exactly one identifier.

`@GeneratedValue(strategy = GenerationType.IDENTITY)` tells JPA that the database generates the key value automatically. `GenerationType.IDENTITY` means the column is an auto-increment / identity column, so the database assigns the next value on `INSERT` and the application does not set it. The field is a `Long` (a nullable wrapper, important because a not-yet-saved invoice has no id until the database assigns one).

```java
// L18-L19
    @Column(nullable = false)
    private Long accountId;
```

`@Column` customizes the column mapping for a field. `nullable = false` adds a `NOT NULL` constraint at the database level, so every invoice must reference an account. `accountId` (type `Long`) is a foreign-key-style reference to the customer account this invoice belongs to. Note this is a plain `Long` value, not a JPA relationship (`@ManyToOne`) — the entity stores the raw account id rather than an `Account` object, so there is no automatic join to an account entity.

```java
// L21-L22
    @Column(nullable = false)
    private Long cycleId;
```

`cycleId` (`Long`, `NOT NULL`) identifies the billing cycle this invoice was generated for. Like `accountId`, it is a raw id value, not a mapped relationship.

```java
// L24-L25
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal planCharges;
```

`planCharges` is the base charge for the customer's plan. The `@Column` attributes `precision = 10, scale = 2` define the SQL `DECIMAL(10,2)` shape: up to 10 total significant digits with 2 after the decimal point (i.e., values up to 99,999,999.99 with cent precision). `nullable = false` makes it required. Using `BigDecimal` keeps currency math exact.

```java
// L27-L28
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal excessCharges;
```

`excessCharges` (`BigDecimal`, `DECIMAL(10,2)`, `NOT NULL`) holds charges for usage beyond the plan's included allowance (overage).

```java
// L30-L31
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal addOnCharges;
```

`addOnCharges` (`BigDecimal`, `DECIMAL(10,2)`, `NOT NULL`) holds charges for optional add-on products or services purchased on top of the plan.

```java
// L33-L34
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxes;
```

`taxes` (`BigDecimal`, `DECIMAL(10,2)`, `NOT NULL`) is the tax amount applied to the invoice.

```java
// L36-L37
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
```

`totalAmount` (`BigDecimal`, `DECIMAL(10,2)`, `NOT NULL`) is the grand total the customer owes. The entity stores this as a persisted value; the arithmetic that derives it from the charge components and taxes lives in the service layer, not here.

```java
// L39-L40
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;
```

`paidAmount` (`BigDecimal`, `DECIMAL(10,2)`, `NOT NULL`) tracks how much has been paid against the invoice so far. It is initialized inline to `BigDecimal.ZERO` so a freshly constructed invoice starts at zero paid rather than `null`. The difference `totalAmount - paidAmount` (plus any late fee) represents the outstanding balance.

```java
// L42-L43
    @Column(precision = 10, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;
```

`lateFee` (`BigDecimal`, `DECIMAL(10,2)`) is the penalty fee charged when an invoice is paid late. Note `nullable` is **not** set here, so it defaults to `nullable = true` — the column allows `NULL` at the database level, unlike the other money columns. The field is nonetheless initialized to `BigDecimal.ZERO` in code so new objects begin with no fee.

```java
// L45-L46
    @Column(nullable = false)
    private LocalDate dueDate;
```

`dueDate` (`LocalDate`, `NOT NULL`) is the calendar date by which payment is due. Stored as a SQL `DATE` with no time component. It is the reference point for deciding whether an invoice is overdue.

```java
// L48-L50
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;
```

`status` holds the invoice's lifecycle state as an `InvoiceStatus` enum value (`GENERATED`, `SENT`, `PAID`, `OVERDUE`, or `DISPUTED`).

`@Enumerated(EnumType.STRING)` controls how the enum is stored: `EnumType.STRING` persists the enum's **name** as text (e.g., the literal `"PAID"`). This is far safer than the alternative `EnumType.ORDINAL`, which would store the position index (0, 1, 2…) and would silently corrupt data if the enum constants were ever reordered. `@Column(nullable = false)` makes the status column required.

```java
// L52-L53
    @Column(updatable = false)
    private LocalDateTime createdAt;
```

`createdAt` (`LocalDateTime`) records when the invoice row was first inserted. `@Column(updatable = false)` means JPA will include this column in the `INSERT` statement but **never** in any subsequent `UPDATE`, so the creation timestamp can never be overwritten once set.

```java
// L55
    private LocalDateTime updatedAt;
```

`updatedAt` (`LocalDateTime`) records the time of the last modification. It has no `@Column` annotation, so it maps with all default settings (column name `updated_at` per Hibernate's naming strategy, nullable, updatable). It is refreshed on every update by the `@PreUpdate` callback below.

```java
// L57-L63
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.paidAmount == null) this.paidAmount = BigDecimal.ZERO;
        if (this.lateFee == null) this.lateFee = BigDecimal.ZERO;
    }
```

`@PrePersist` marks `onCreate()` as a JPA lifecycle callback that the persistence provider invokes automatically **just before** the entity is first inserted into the database (i.e., before the `INSERT`). It is a `protected void` method taking no parameters.

Step by step:
- `this.createdAt = LocalDateTime.now();` stamps the current date-time as the creation time.
- `this.updatedAt = this.createdAt;` sets the initial "updated" time equal to the creation time, so a brand-new row has matching timestamps.
- `if (this.paidAmount == null) this.paidAmount = BigDecimal.ZERO;` — a defensive guard: if some code path managed to leave `paidAmount` null (for example by explicitly setting it via a setter), this forces it to zero before persisting, satisfying the `NOT NULL` constraint.
- `if (this.lateFee == null) this.lateFee = BigDecimal.ZERO;` — the same guard for `lateFee`, ensuring it is never null even though its column technically allows null.

This callback is the reason creation/timestamp logic does not need to be duplicated in the service layer.

```java
// L65-L68
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
```

`@PreUpdate` marks `onUpdate()` as a lifecycle callback invoked automatically **just before** an existing row is updated (before the `UPDATE` statement). It refreshes `updatedAt` to the current time, giving an accurate "last modified" timestamp on every change. `createdAt` is untouched here (and is `updatable = false` anyway), so the original creation time is preserved.

```java
// L70
    public Invoice() {}
```

The no-argument (default) constructor. JPA **requires** every entity to have a public or protected no-arg constructor so the provider can instantiate the object via reflection when loading rows from the database. Its body is empty.

```java
// L72-L88
    public Invoice(Long invoiceId, Long accountId, Long cycleId, BigDecimal planCharges,
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

The all-arguments constructor. It accepts a value for every business field (twelve parameters — every field except the auto-managed `createdAt` and `updatedAt`) and assigns each to the corresponding instance field via `this.`. This is the constructor the inner `Builder.build()` method calls. Note it does **not** apply the null-to-zero defaulting that `onCreate()` does, so if `paidAmount` or `lateFee` are passed as `null` they remain `null` until `@PrePersist` runs at insert time.

```java
// L90-L130
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
```

These are the standard JavaBean **getters and setters** — one pair for each of the fourteen fields (the twelve business fields plus `createdAt` and `updatedAt`). Each getter returns the field's current value; each setter stores a new value via `this.`. They follow the conventional `getXxx`/`setXxx` naming that JPA, Spring, and JSON serializers (Jackson) rely on for property access. Because they are hand-written here, this entity does not depend on Lombok's `@Getter`/`@Setter`/`@Data` to generate them. Note that exposing setters for `createdAt`/`updatedAt` lets callers override the audit timestamps, though in normal flow the lifecycle callbacks manage them.

```java
// L132
    public static Builder builder() { return new Builder(); }
```

A static factory method that returns a fresh `Builder` instance. This is the entry point for the fluent builder pattern, letting callers write `Invoice.builder()....build()`. It is a hand-coded equivalent of what Lombok's `@Builder` would generate.

```java
// L134-L146
    public static class Builder {
        private Long invoiceId;
        private Long accountId;
        private Long cycleId;
        private BigDecimal planCharges;
        private BigDecimal excessCharges;
        private BigDecimal addOnCharges;
        private BigDecimal taxes;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount = BigDecimal.ZERO;
        private BigDecimal lateFee = BigDecimal.ZERO;
        private LocalDate dueDate;
        private InvoiceStatus status;
```

`public static class Builder` is a nested static class implementing the builder pattern — a readable way to construct an `Invoice` by setting only the fields you care about, in any order, instead of remembering the position of twelve constructor arguments. It declares a private field mirroring each business field of the outer class. Like the entity, `paidAmount` and `lateFee` default to `BigDecimal.ZERO`, so a built invoice that never sets them starts at zero rather than null.

```java
// L148-L159
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

These are the **fluent setter methods**, one per field. Each stores its argument into the builder's matching field and then `return this;` — returning the builder itself so calls can be chained, e.g. `Invoice.builder().accountId(1L).totalAmount(new BigDecimal("99.99")).status(InvoiceStatus.GENERATED)`. Each returns `Builder`, the type of the enclosing nested class.

```java
// L161-L165
        public Invoice build() {
            return new Invoice(invoiceId, accountId, cycleId, planCharges, excessCharges,
                    addOnCharges, taxes, totalAmount, paidAmount, lateFee, dueDate, status);
        }
    }
```

`build()` is the terminal method of the builder. It calls the all-args constructor (L72) with the values accumulated in the builder's fields, in the constructor's exact parameter order, and returns the finished `Invoice`. It returns the outer type `Invoice`. The closing brace ends the nested `Builder` class.

```java
// L166-L167
    }
}
```

The final braces close the `Invoice` class.

## How this connects

`Invoice` sits at the bottom of the Controller → Service → Repository → Entity/DB stack:

- The **Repository** layer (a Spring Data JPA interface, typically `JpaRepository<Invoice, Long>`) performs CRUD and query operations that return and accept `Invoice` instances; Hibernate maps them to and from rows in the `invoices` table.
- The **Service** layer owns the business logic — computing `totalAmount` from `planCharges`, `excessCharges`, `addOnCharges`, and `taxes`, applying `lateFee`, advancing `status` through the `InvoiceStatus` lifecycle, and recording payments into `paidAmount`. It builds `Invoice` objects (often via `Invoice.builder()`) and hands them to the repository.
- The **Controller** layer exposes these operations over HTTP and serializes `Invoice` data (commonly via DTOs) to clients.
- The `@PrePersist`/`@PreUpdate` callbacks automatically maintain the `createdAt`/`updatedAt` audit fields, so no other layer must manage timestamps.
- The companion enum `src/main/java/com/teleconnect/billing_service/enums/InvoiceStatus.java` defines the allowed values of the `status` field.
