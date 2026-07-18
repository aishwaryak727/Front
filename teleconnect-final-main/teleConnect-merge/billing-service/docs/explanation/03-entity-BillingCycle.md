# Entity: `BillingCycle`

`BillingCycle.java` is a JPA (Jakarta Persistence API) **entity** — a plain Java class that the persistence framework (Hibernate, under Spring Data JPA) maps directly onto a relational database table named `billing_cycles`. Each instance of this class represents one row in that table: a single billing period for a customer account. In the standard Spring layering of **Controller → Service → Repository → Entity/DB**, this file sits at the bottom: the Controller receives HTTP requests, the Service holds business logic, the Repository (a Spring Data JPA interface) reads and writes these `BillingCycle` objects, and Hibernate translates them to and from SQL rows in the database.

---

## src/main/java/com/teleconnect/billing_service/entity/BillingCycle.java

This file defines the `BillingCycle` JPA entity, its column mappings, lifecycle callbacks for timestamps, constructors, getters/setters, and a hand-written Builder for fluent object construction.

```java
// L1
package com.teleconnect.billing_service.entity;
```

The **package declaration**. It states that this class lives in the namespace `com.teleconnect.billing_service.entity`. The package path mirrors the folder structure on disk, and grouping the class under `.entity` is a convention signalling that this is a persistence/domain entity (as opposed to a controller, service, or repository). Other classes must import `com.teleconnect.billing_service.entity.BillingCycle` to use it.

```java
// L3
import com.teleconnect.billing_service.enums.BillingCycleStatus;
```

Imports the `BillingCycleStatus` enum from the project's `enums` package. This enum is used as the type of the `status` field below. Its definition is `enum BillingCycleStatus { OPEN, GENERATED, CLOSED }` — i.e. a billing cycle is either **OPEN** (active/accumulating), **GENERATED** (an invoice/bill has been produced), or **CLOSED** (finalized). Using an enum rather than a free-form string constrains `status` to exactly these three legal values at compile time.

```java
// L4
import jakarta.persistence.*;
```

A wildcard import of the **Jakarta Persistence** package. This single line brings in every JPA annotation and type used in this file: `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `GenerationType`, `@Column`, `@Enumerated`, `EnumType`, `@PrePersist`, and `@PreUpdate`. `jakarta.persistence` is the modern (Jakarta EE 9+) namespace for JPA, used by Spring Boot 3.x; in older Spring Boot 2.x this would have been `javax.persistence`.

```java
// L6-L7
import java.time.LocalDate;
import java.time.LocalDateTime;
```

Imports two classes from the modern Java date/time API (`java.time`). `LocalDate` represents a calendar date with no time-of-day and no timezone (e.g. `2026-06-15`); it is used for the cycle's start/end and generation dates. `LocalDateTime` represents a date plus a time-of-day, still without a timezone (e.g. `2026-06-15T14:30:00`); it is used for the audit timestamps `createdAt` and `updatedAt`. JPA/Hibernate maps `LocalDate` to a SQL `DATE` column and `LocalDateTime` to a SQL `TIMESTAMP`/`DATETIME` column.

```java
// L9
@Entity
```

`@Entity` is the core JPA annotation that marks this class as a **persistent entity** — a class whose instances can be stored in and retrieved from the database. It tells Hibernate to manage this class: to track it in the persistence context, generate SQL for it, and treat it as a mapped table. Every JPA entity must have a no-argument constructor and a primary key (defined below).

```java
// L10
@Table(name = "billing_cycles")
```

`@Table` overrides the default table-name derivation. Without it, JPA would default the table name from the class name (`BillingCycle`). Here `name = "billing_cycles"` explicitly binds this entity to a database table literally called `billing_cycles`. This makes the mapping deterministic regardless of any naming-strategy configuration.

```java
// L11
public class BillingCycle {
```

Declares the public class `BillingCycle`. Because it is annotated `@Entity`, the JPA provider will instantiate it (via the no-arg constructor) when reading rows back from the database.

```java
// L13-L15
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cycleId;
```

This is the **primary key** field.
- `@Id` designates `cycleId` as the entity's primary key — the column that uniquely identifies each row.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` tells JPA that the database itself generates the key value. `GenerationType.IDENTITY` relies on an auto-increment / identity column (e.g. MySQL `AUTO_INCREMENT`, SQL Server `IDENTITY`, PostgreSQL `SERIAL`); the database assigns the next ID on `INSERT`, and Hibernate reads it back. The field type is `Long` (the object wrapper, nullable) rather than primitive `long`, so that a not-yet-persisted entity can have a `null` ID before it is saved.

```java
// L17-L18
    @Column(nullable = false)
    private Long accountId;
```

`accountId` is a `Long` holding the identifier of the customer account this billing cycle belongs to. `@Column` customizes the column mapping; `nullable = false` adds a `NOT NULL` constraint to the generated DDL, so the database will reject a row with a missing account ID. Note this is a **plain foreign-key-style ID**, not a JPA `@ManyToOne` association — there is no object reference to an `Account` entity here, just the raw key value. (`@ManyToOne` would model the relationship as an object graph; this design keeps it as a loose numeric reference, which is simpler but means JPA won't enforce or navigate the relationship.)

```java
// L20-L21
    @Column(nullable = false)
    private LocalDate cycleStart;
```

`cycleStart` is the first calendar date of the billing period, mapped to a non-null `DATE` column. `nullable = false` again enforces `NOT NULL` at the database level — every cycle must have a defined start date.

```java
// L23-L24
    @Column(nullable = false)
    private LocalDate cycleEnd;
```

`cycleEnd` is the last calendar date of the billing period, also a non-null `DATE` column. Together with `cycleStart` it defines the time window over which charges accumulate. *Note: there is no validation in this class ensuring `cycleEnd` is after `cycleStart`; any such business rule would have to be enforced in the service layer.*

```java
// L26
    private LocalDate generatedDate;
```

`generatedDate` is a `LocalDate` recording when the bill for this cycle was generated. It has **no `@Column` annotation**, so it takes JPA defaults: a column named `generated_date` (per the default naming strategy) that is **nullable**. Nullability is intentional here — a cycle that is still `OPEN` has not been generated yet, so this date is legitimately empty until generation occurs.

```java
// L28-L30
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycleStatus status;
```

`status` holds the lifecycle state of the cycle, typed as the `BillingCycleStatus` enum (`OPEN`, `GENERATED`, `CLOSED`).
- `@Enumerated(EnumType.STRING)` controls how the enum is persisted. `EnumType.STRING` stores the **enum constant's name as text** (e.g. the literal string `"OPEN"`). The alternative, `EnumType.ORDINAL`, would store the enum's positional index (0, 1, 2) — which is fragile because reordering or inserting enum constants silently corrupts existing data. Choosing `STRING` is the robust, human-readable option.
- `@Column(nullable = false)` makes the column `NOT NULL`, so every cycle must have a status.

```java
// L32-L33
    @Column(updatable = false)
    private LocalDateTime createdAt;
```

`createdAt` is the audit timestamp of when the row was first inserted. `@Column(updatable = false)` means that after the initial `INSERT`, Hibernate will **never include this column in `UPDATE` statements** — so the creation time can never be accidentally overwritten on subsequent saves. (It is left nullable because the value is populated automatically by the lifecycle callback below rather than by the constructors.)

```java
// L35
    private LocalDateTime updatedAt;
```

`updatedAt` is the audit timestamp of the most recent modification. It has no `@Column` annotation, so it maps to a nullable `updated_at` column that *is* updatable (the default), allowing it to be rewritten on every change.

```java
// L37-L41
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
```

This is a **JPA lifecycle callback**. `@PrePersist` registers `onCreate()` to run automatically **immediately before the entity is first INSERTed** into the database (i.e. when `repository.save(...)` persists a brand-new entity). Step by step:
1. `this.createdAt = LocalDateTime.now();` — captures the current date-time as the creation timestamp.
2. `this.updatedAt = this.createdAt;` — initializes the "last updated" timestamp to the same value, so a freshly created row has matching created/updated times.

The method is `protected` and returns `void`; JPA only requires that a callback be a no-arg method, and `protected` is sufficient for the provider to invoke while keeping it out of the public API. Putting this logic here means callers never have to set timestamps manually — persistence does it.

```java
// L43-L46
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
```

A second lifecycle callback. `@PreUpdate` registers `onUpdate()` to run automatically **immediately before an existing row is UPDATEd**. It sets `updatedAt` to the current date-time, so the timestamp reflects the moment of the latest change. It deliberately does **not** touch `createdAt` (which is also protected from updates by `updatable = false`), preserving the original creation time. Together, `@PrePersist` and `@PreUpdate` implement a simple created/modified auditing pattern entirely within the entity.

```java
// L48
    public BillingCycle() {}
```

The **no-argument (default) constructor**. JPA requires every entity to have a no-arg constructor (at least `protected` visibility) because the provider instantiates the object reflectively when materializing query results before populating its fields. Here it is empty and `public`.

```java
// L50-L58
    public BillingCycle(Long cycleId, Long accountId, LocalDate cycleStart, LocalDate cycleEnd,
                        LocalDate generatedDate, BillingCycleStatus status) {
        this.cycleId = cycleId;
        this.accountId = accountId;
        this.cycleStart = cycleStart;
        this.cycleEnd = cycleEnd;
        this.generatedDate = generatedDate;
        this.status = status;
    }
```

An **all-but-audit-fields constructor**. It takes six parameters and assigns each to the corresponding field. Notably it sets the six business fields (`cycleId`, `accountId`, `cycleStart`, `cycleEnd`, `generatedDate`, `status`) but **not** `createdAt`/`updatedAt` — those are intentionally left to the lifecycle callbacks. This constructor is what the `Builder.build()` method (below) calls to assemble a fully-populated object.

```java
// L60-L82
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

Standard **JavaBean getters and setters** for all eight fields (the six business fields plus the two audit timestamps). Each getter returns the field value; each setter assigns a new value. These accessors matter for several reasons:
- **JPA/Hibernate** can use property access to read and write field values when mapping rows.
- **Serialization frameworks** (e.g. Jackson, when these entities or DTOs are turned into JSON in the controller layer) use getters to read properties and setters to bind incoming JSON.
- **The service layer** uses the setters to mutate state — for example, calling `setStatus(BillingCycleStatus.GENERATED)` and `setGeneratedDate(...)` when a bill is produced, which on the next `save` will trigger `@PreUpdate` and refresh `updatedAt`.

This class is written **without Lombok**; the getters, setters, constructors, and builder are all hand-written rather than generated by annotations like `@Data` or `@Builder`. That is purely a stylistic/explicitness choice and has no functional effect.

```java
// L84
    public static Builder builder() { return new Builder(); }
```

A **static factory method** providing the entry point to the fluent builder. Calling `BillingCycle.builder()` returns a fresh `Builder` instance, enabling the readable construction style `BillingCycle.builder().accountId(1L).status(OPEN).build()`. This is a manual implementation of the same pattern that Lombok's `@Builder` would auto-generate.

```java
// L86-L92
    public static class Builder {
        private Long cycleId;
        private Long accountId;
        private LocalDate cycleStart;
        private LocalDate cycleEnd;
        private LocalDate generatedDate;
        private BillingCycleStatus status;
```

A **static nested `Builder` class**. Because it is `static`, it does not hold a reference to an enclosing `BillingCycle` instance and can be created independently. It declares one private field for each of the six business fields it will collect before constructing the final object. These mirror the parameters of the L50 constructor (the audit timestamps are deliberately not builder-settable, since they are auto-managed).

```java
// L94-L99
        public Builder cycleId(Long cycleId) { this.cycleId = cycleId; return this; }
        public Builder accountId(Long accountId) { this.accountId = accountId; return this; }
        public Builder cycleStart(LocalDate cycleStart) { this.cycleStart = cycleStart; return this; }
        public Builder cycleEnd(LocalDate cycleEnd) { this.cycleEnd = cycleEnd; return this; }
        public Builder generatedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; return this; }
        public Builder status(BillingCycleStatus status) { this.status = status; return this; }
```

The **fluent setter methods**, one per field. Each stores its argument into the builder's field and then `return this;` — returning the same `Builder` instance so calls can be chained (`.cycleStart(d1).cycleEnd(d2)...`). This chaining is the defining characteristic of the builder pattern: it lets callers set only the fields they care about, in any order, with self-documenting method names.

```java
// L101-L104
        public BillingCycle build() {
            return new BillingCycle(cycleId, accountId, cycleStart, cycleEnd, generatedDate, status);
        }
    }
}
```

`build()` is the **terminal operation** of the builder. It invokes the six-argument constructor (L50) with all the values accumulated in the builder's fields and returns the finished, immutable-at-construction `BillingCycle`. Any field never set on the builder is passed as its default (`null`), so for example a cycle built without `generatedDate` will have a null generation date — consistent with a newly-opened cycle. The final two braces close the `Builder` class and the `BillingCycle` class respectively.

---

## How this connects

`BillingCycle` is the persistence anchor for billing-period data and is consumed by the layers above it:

- **Repository layer:** A Spring Data JPA repository (typically `BillingCycleRepository extends JpaRepository<BillingCycle, Long>`) provides CRUD and query methods that operate on this entity, with `Long` being the `cycleId` primary-key type. Hibernate turns `save`/`find`/`delete` calls into SQL against the `billing_cycles` table.
- **Service layer:** Business logic (e.g. opening a new cycle, generating a bill, closing a cycle) creates and mutates `BillingCycle` instances — often via `BillingCycle.builder()` for new objects and the setters (e.g. `setStatus`, `setGeneratedDate`) for transitions — then delegates persistence to the repository. The `@PrePersist`/`@PreUpdate` callbacks transparently maintain `createdAt`/`updatedAt` on each save.
- **Controller layer:** REST controllers expose these operations over HTTP. The entity (or a DTO derived from it) is serialized to/from JSON, with the getters/setters supporting that mapping.
- **Enum dependency:** The `status` field couples this entity to `BillingCycleStatus` (`OPEN`/`GENERATED`/`CLOSED`), persisted as text via `@Enumerated(EnumType.STRING)`, which encodes the lifecycle each cycle moves through.
