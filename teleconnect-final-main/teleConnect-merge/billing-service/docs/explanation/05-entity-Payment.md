# Entity: `Payment`

`Payment.java` is a **JPA entity** — a plain Java object whose fields are mapped, by Jakarta Persistence (JPA) annotations, onto rows and columns of a relational database table. This class represents a single payment made against an invoice, and is the Java-side mirror of the `payments` table. In the layered architecture of this service (Controller → Service → Repository → Entity/DB), the `Payment` entity sits at the very bottom: a `PaymentRepository` (Spring Data JPA) reads and writes `Payment` objects, the service layer applies business logic on them, and the controller exposes them over HTTP. Hibernate (the JPA provider Spring Boot uses by default) is responsible for translating between `Payment` instances and SQL.

## src/main/java/com/teleconnect/billing_service/entity/Payment.java

This file defines the `Payment` JPA entity: its persistent fields, the column mappings, a lifecycle callback that defaults the payment date, standard constructors/getters/setters, and a hand-written Builder for fluent object construction.

```java
// L1
package com.teleconnect.billing_service.entity;
```
The **package declaration** places this class in the `com.teleconnect.billing_service.entity` package. Package names mirror the folder structure on disk and provide a unique namespace so the class is referenced as `com.teleconnect.billing_service.entity.Payment`. Grouping all entities under an `.entity` package is a common convention that keeps the persistence model separate from controllers, services, and repositories.

```java
// L3-L4
import com.teleconnect.billing_service.enums.PaymentMethod;
import com.teleconnect.billing_service.enums.PaymentStatus;
```
These two **imports** pull in the project's own enum types so they can be used unqualified below. `PaymentMethod` is an `enum` listing the allowed ways to pay (`UPI`, `CARD`, `NETBANKING`, `WALLET`, `BANK_TRANSFER`, `CASH`). `PaymentStatus` is an `enum` listing the lifecycle states of a payment (`SUCCESS`, `FAILED`, `PENDING`). Using enums instead of free-form strings constrains the field to a fixed, type-safe set of values.

```java
// L5
import jakarta.persistence.*;
```
This wildcard **import** brings in every annotation and type from the Jakarta Persistence API (`jakarta.persistence`) — the standard ORM (Object-Relational Mapping) specification. It supplies `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `GenerationType`, `@Column`, `@Enumerated`, `EnumType`, and `@PrePersist`, all of which are used in this file. (The `jakarta.*` namespace is the modern replacement for the older `javax.persistence.*`, used by Spring Boot 3 / Jakarta EE 9+.)

```java
// L7-L8
import java.math.BigDecimal;
import java.time.LocalDateTime;
```
Two standard-library **imports**. `BigDecimal` is an arbitrary-precision decimal type used here for monetary amounts — it avoids the rounding errors of `float`/`double` and is the correct choice for currency. `LocalDateTime` represents a date-and-time without a timezone, used here to record when a payment occurred.

```java
// L10-L12
@Entity
@Table(name = "payments")
public class Payment {
```
- **`@Entity`** marks this class as a JPA entity: it tells the persistence provider (Hibernate) that instances of `Payment` should be persisted to and loaded from the database, and that the class participates in the persistence context. An `@Entity` class must have a no-argument constructor (provided at L45) and an identifier field (the `@Id` at L14).
- **`@Table(name = "payments")`** customizes the table mapping, explicitly binding this entity to a database table named `payments`. Without it, JPA would default the table name to the class name (`Payment`); naming it explicitly makes the schema predictable and conventionally pluralized.
- `public class Payment {` opens the class declaration. It is `public` so other layers (repository, service, controller, DTO mappers) can reference it.

```java
// L14-L16
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;
```
This is the **primary key** of the entity.
- **`@Id`** designates `paymentId` as the unique identifier of the entity — it maps to the table's primary-key column. Every JPA entity must have exactly one identity (or a composite id), and Hibernate uses it to track and distinguish managed instances.
- **`@GeneratedValue(strategy = GenerationType.IDENTITY)`** tells JPA the database itself generates the key value, using an auto-increment / IDENTITY column. With `IDENTITY`, the value is assigned by the database on `INSERT` and read back afterward; the application does not set `paymentId` for new rows. (A practical consequence of `IDENTITY` is that Hibernate cannot batch inserts as aggressively, because it must perform each insert to learn the generated id.)
- `private Long paymentId;` is the field. Using the wrapper `Long` (rather than primitive `long`) allows the value to be `null` before the row is persisted, which is the expected state for a not-yet-saved entity.

```java
// L18-L19
    @Column(nullable = false)
    private Long invoiceId;
```
- **`@Column(nullable = false)`** maps `invoiceId` to a column and adds a `NOT NULL` constraint, so the database rejects rows without an invoice id. `@Column` customizes the column mapping; here only nullability is set, so the column name defaults to `invoice_id` (Hibernate's default naming strategy converts camelCase to snake_case).
- `private Long invoiceId;` stores the id of the invoice this payment is applied to. *Note: this is a plain `Long` foreign-key value, not a JPA relationship (`@ManyToOne` to an `Invoice` entity). The link to `Invoice` is by raw id only, so JPA enforces no referential integrity here and there is no object navigation from `Payment` to `Invoice`.*

```java
// L21-L22
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;
```
- **`@Column(nullable = false, precision = 10, scale = 2)`** maps the monetary amount. `nullable = false` makes the column `NOT NULL`. `precision = 10` is the total number of significant digits the column can hold, and `scale = 2` is the number of digits after the decimal point — together they define a `DECIMAL(10,2)` column, i.e. up to 99,999,999.99. These attributes only meaningfully apply to numeric/decimal types like `BigDecimal`.
- `private BigDecimal amountPaid;` is the actual amount paid, kept as `BigDecimal` for exact decimal arithmetic on currency.

```java
// L24-L25
    @Column(nullable = false)
    private LocalDateTime paymentDate;
```
- **`@Column(nullable = false)`** makes the timestamp column `NOT NULL`.
- `private LocalDateTime paymentDate;` records when the payment was made. Note that although the column is `NOT NULL`, the application does not have to set it explicitly — the `@PrePersist` callback at L38 defaults it to "now" if it is left `null`, which is what keeps the `NOT NULL` constraint from being violated on new inserts.

```java
// L27-L29
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;
```
- **`@Enumerated(EnumType.STRING)`** controls how the Java enum is stored. `EnumType.STRING` persists the enum's **name** (e.g. the literal text `"UPI"`, `"CARD"`) into the column. The alternative, `EnumType.ORDINAL`, would store the enum's integer position — fragile because reordering or inserting enum constants silently corrupts existing data. `STRING` is the safer, self-documenting choice used here.
- **`@Column(nullable = false)`** makes the column `NOT NULL`.
- `private PaymentMethod paymentMethod;` holds the payment channel, constrained at the type level to the `PaymentMethod` enum's six allowed values.

```java
// L31-L32
    @Column(unique = true)
    private String transactionRef;
```
- **`@Column(unique = true)`** maps `transactionRef` to a column with a `UNIQUE` constraint, so no two payments can share the same transaction reference. Note `nullable` is not specified, so it defaults to `true` (the column is nullable). A `UNIQUE` constraint generally permits multiple `NULL` rows, so several payments may have no reference while any non-null reference must be unique.
- `private String transactionRef;` stores the external transaction/gateway reference string (e.g. a payment-processor receipt id).

```java
// L34-L36
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
```
The same pattern as `paymentMethod`: `@Enumerated(EnumType.STRING)` stores the `PaymentStatus` enum by name, and `@Column(nullable = false)` makes the column `NOT NULL`. `private PaymentStatus status;` records the payment's outcome — one of `SUCCESS`, `FAILED`, or `PENDING`. *Note: unlike `paymentDate`, there is no default for `status`; if a caller persists a `Payment` without setting it, the `NOT NULL` constraint will be violated at insert time. The service layer is responsible for always setting a status.*

```java
// L38-L43
    @PrePersist
    protected void onCreate() {
        if (this.paymentDate == null) {
            this.paymentDate = LocalDateTime.now();
        }
    }
```
- **`@PrePersist`** is a JPA lifecycle callback annotation. The annotated method is invoked automatically by the persistence provider **immediately before** the entity is first inserted into the database (before the `INSERT`). It runs once, only on initial persist (not on updates).
- `protected void onCreate()` is the callback method. It takes no parameters and returns `void`. `protected` visibility is sufficient — JPA invokes it reflectively, so it need not be `public`.
- `if (this.paymentDate == null)` checks whether the caller already supplied a payment date. If they did, it is left untouched.
- `this.paymentDate = LocalDateTime.now();` — only when no date was provided — defaults the field to the current date and time, guaranteeing the `NOT NULL` column at L24 is satisfied. This is the business rule: *a payment is timestamped at creation time unless an explicit date was given.*

```java
// L45
    public Payment() {}
```
The **no-argument (default) constructor**, with an empty body. JPA requires every entity to have a no-arg constructor so the provider can instantiate the object via reflection when materializing rows from query results before populating its fields. It is `public` here (a non-private no-arg constructor satisfies the spec).

```java
// L47-L56
    public Payment(Long paymentId, Long invoiceId, BigDecimal amountPaid, LocalDateTime paymentDate,
                   PaymentMethod paymentMethod, String transactionRef, PaymentStatus status) {
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.transactionRef = transactionRef;
        this.status = status;
    }
```
The **all-arguments constructor**. It accepts one parameter for every field — `paymentId`, `invoiceId`, `amountPaid`, `paymentDate`, `paymentMethod`, `transactionRef`, `status` — and assigns each to the corresponding instance field using `this.` to disambiguate the field from the parameter of the same name. It performs no validation or transformation; it is a straight field-by-field copy. This constructor is what the inner `Builder.build()` method (L98-L101) calls to produce a fully-populated `Payment`.

```java
// L58-L59
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
```
Standard **getter and setter** for `paymentId`. The getter returns the current value; the setter overwrites it. JPA and serialization frameworks (e.g. Jackson when converting to/from JSON, or Hibernate when accessing properties) rely on these accessor methods.

```java
// L61-L62
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
```
Getter/setter pair for `invoiceId` — read and write the linked invoice's id.

```java
// L64-L65
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
```
Getter/setter pair for `amountPaid` — read and write the monetary amount.

```java
// L67-L68
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
```
Getter/setter pair for `paymentDate`. Note a caller may set this explicitly; only if it is left `null` will the `@PrePersist` callback fill it in at insert time.

```java
// L70-L71
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
```
Getter/setter pair for the `paymentMethod` enum field.

```java
// L73-L74
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
```
Getter/setter pair for `transactionRef` — read and write the external transaction reference string.

```java
// L76-L77
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
```
Getter/setter pair for the `status` enum field. The service layer typically uses `setStatus(...)` to move a payment between `PENDING`, `SUCCESS`, and `FAILED`.

```java
// L79
    public static Builder builder() { return new Builder(); }
```
A **static factory method** that returns a fresh `Builder` instance, providing the entry point for the fluent builder API: e.g. `Payment.builder().invoiceId(1L)...build()`. This is a hand-written equivalent of Lombok's `@Builder`; because it is `static`, it is called on the class (`Payment.builder()`) rather than on an instance.

```java
// L81-L88
    public static class Builder {
        private Long paymentId;
        private Long invoiceId;
        private BigDecimal amountPaid;
        private LocalDateTime paymentDate;
        private PaymentMethod paymentMethod;
        private String transactionRef;
        private PaymentStatus status;
```
- `public static class Builder` is a **static nested class** implementing the Builder design pattern. Being `static`, it does not hold a reference to an enclosing `Payment` instance and can be created independently.
- It declares one private field mirroring each field of `Payment`. These fields accumulate the values supplied through the fluent setter-like methods below, until `build()` assembles them into a real `Payment`.

```java
// L90-L96
        public Builder paymentId(Long paymentId) { this.paymentId = paymentId; return this; }
        public Builder invoiceId(Long invoiceId) { this.invoiceId = invoiceId; return this; }
        public Builder amountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; return this; }
        public Builder paymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; return this; }
        public Builder paymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public Builder transactionRef(String transactionRef) { this.transactionRef = transactionRef; return this; }
        public Builder status(PaymentStatus status) { this.status = status; return this; }
```
Seven **fluent builder methods**, one per field. Each takes a single value, stores it in the builder's corresponding field, and **returns `this`** (the same `Builder`). Returning `this` is what enables method chaining — calls can be strung together in one expression. None perform validation; they simply record the value.

```java
// L98-L102
        public Payment build() {
            return new Payment(paymentId, invoiceId, amountPaid, paymentDate,
                    paymentMethod, transactionRef, status);
        }
    }
```
- `public Payment build()` is the **terminal builder method**. It calls the all-args constructor (L47-L56), passing the seven accumulated builder fields in order, and returns the constructed `Payment`. After this point, the assembled object can be saved via the repository.
- The final closing brace at L102 ends the inner `Builder` class.

```java
// L103
}
```
The closing brace of the `Payment` class.

## How this connects

`Payment` is the persistence model at the foot of the Controller → Service → Repository → Entity/DB stack:

- **Database / Hibernate:** The `@Entity` / `@Table(name = "payments")` mapping, the `@Id` + `@GeneratedValue` key, the `@Column` constraints, and the `@Enumerated` enum mappings together define the `payments` table schema and how rows convert to/from `Payment` objects. The `@PrePersist` hook runs inside Hibernate's insert lifecycle.
- **Repository layer:** A Spring Data JPA repository (e.g. `PaymentRepository extends JpaRepository<Payment, Long>`) consumes this entity directly — `save`, `findById`, and derived/`@Query` methods all operate on `Payment` instances keyed by its `Long` id.
- **Service layer:** Business logic constructs `Payment` objects (often via `Payment.builder()...build()`), sets `status`, and delegates persistence to the repository.
- **Controller / DTO layer:** Controllers map between request/response DTOs and this entity, exposing payment data over HTTP.
- **Enums:** `PaymentMethod` and `PaymentStatus` (in `com.teleconnect.billing_service.enums`) constrain the `paymentMethod` and `status` fields to fixed value sets, stored as strings in the database.
- **Related entity:** The link to an invoice is carried only as a raw `Long invoiceId` (not a JPA `@ManyToOne` relationship), so any join to an `Invoice` entity must be done manually by id.
