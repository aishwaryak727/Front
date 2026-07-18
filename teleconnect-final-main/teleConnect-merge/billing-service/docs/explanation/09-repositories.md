# Repositories (Spring Data JPA Persistence Layer)

This part covers the four Spring Data JPA repository interfaces of the billing service: `BillingCycleRepository`, `BillingDisputeRepository`, `InvoiceRepository`, and `PaymentRepository`. In the layered architecture (**Controller → Service → Repository → Entity/DB**), repositories are the lowest application-layer tier: the **service** classes inject these interfaces and call their methods to read and write rows, while the repositories translate those calls into SQL against the database and hydrate the results back into **entity** objects (`BillingCycle`, `BillingDispute`, `Invoice`, `Payment`). Crucially, you write *no* implementation code here — you declare an interface, and Spring Data JPA generates the concrete implementation at runtime.

### Framework concepts used throughout (defined once here)

- **`interface ... extends JpaRepository<T, ID>`** — `JpaRepository` is a Spring Data interface that, for an entity type `T` whose primary-key type is `ID`, provides a complete CRUD toolkit out of the box: `save`, `saveAll`, `findById`, `findAll`, `findAllById`, `count`, `existsById`, `delete`, `deleteById`, `deleteAll`, plus JPA-specific helpers like `flush`, `saveAndFlush`, and `getReferenceById`. Because all four interfaces extend it, none of these standard methods are restated in the source files — they are inherited.
- **`@Repository`** — a Spring stereotype annotation. It marks the interface as a persistence-layer component so it is eligible for component scanning, and (more importantly for repositories) it activates **persistence exception translation**: low-level vendor/JDBC exceptions are translated into Spring's unified `DataAccessException` hierarchy. On a `JpaRepository`, Spring Data already registers the bean and applies this translation, so `@Repository` here is largely explicit documentation/safety rather than strictly required, but it is harmless and conventional.
- **Derived query methods** — Spring Data parses the *method name* into a query. A name like `findByAccountIdAndStatus` is split into a property path (`accountId`, `status`) joined by keywords (`And`, `Between`, `LessThanEqual`, `Before`, etc.). Each property name must match a field on the entity (or a navigable path). The method parameters supply the values in order. At application startup Spring validates these names against the entity metamodel; a typo that does not map to a real field fails fast with an error.
- **`@Query("...JPQL...")`** — lets you write an explicit query instead of relying on the method name. The string is **JPQL** (Java Persistence Query Language): it operates on *entity names and field names*, not on table/column names. Spring binds method parameters to the named placeholders (`:fromDate`) in the query.
- **`Optional<T>`** — a container that holds either one value or nothing, used for finder methods that return at most one row. It forces the caller to handle the "not found" case explicitly instead of risking a `NullPointerException`.
- **`List<T>`** — returned by finders that may match zero, one, or many rows; an empty list (never `null`) signals no matches.
- **`Page<T>` / `Pageable`** — the pagination contract. A `Pageable` argument carries the requested page number, page size, and sort order; the returned `Page<T>` carries that slice of results *plus* metadata (total element count, total pages, etc.). Spring Data automatically issues a second `COUNT` query to populate the totals.

---

## src/main/java/com/teleconnect/billing_service/repository/BillingCycleRepository.java

Persistence interface for the `BillingCycle` entity (the recurring billing period for an account). It adds account- and status-based lookups, a due-cycle finder, and paginated variants on top of the inherited CRUD operations.

```java
// L1
package com.teleconnect.billing_service.repository;
```

Declares the package. Placing all four repositories in `...billing_service.repository` keeps them on the component-scan path of the Spring Boot application (whose main class lives in the root `com.teleconnect.billing_service` package), so they are discovered and instantiated automatically.

```java
// L3-L4
import com.teleconnect.billing_service.entity.BillingCycle;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
```

Imports the domain types this interface references: the `BillingCycle` entity (the `T` of the repository, and the element type of every return value) and the `BillingCycleStatus` enum (used both as a query parameter type and as the type of the entity's `status` field).

```java
// L5-L8
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
```

Imports the Spring Data infrastructure: `Page` and `Pageable` for pagination (see concepts above), `JpaRepository` for the inherited CRUD base, and the `@Repository` stereotype annotation.

```java
// L10-L12
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
```

Imports `LocalDate` (a date without time, matching the entity's `cycleEnd`/`cycleStart` fields and used as a query bound), and the `List`/`Optional` return-type containers described above.

```java
// L14-L15
@Repository
public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {
```

`@Repository` registers this as a persistence component with exception translation. The interface extends `JpaRepository<BillingCycle, Long>`: it manages `BillingCycle` entities whose primary key (`cycleId`, annotated `@Id`/`@GeneratedValue` in the entity) is a `Long`. All standard CRUD methods are inherited from this declaration alone.

```java
// L17
    List<BillingCycle> findByAccountId(Long accountId);
```

Derived query returning every billing cycle belonging to one account. Spring generates `... WHERE account_id = ?` (against the entity's `accountId` field) and returns a `List` (empty if the account has no cycles). Used by services to list an account's billing history.

```java
// L19
    List<BillingCycle> findByStatus(BillingCycleStatus status);
```

Returns all cycles in a given lifecycle state (e.g. `OPEN`, `CLOSED`, `BILLED` — whatever the `BillingCycleStatus` enum defines). Because `status` is mapped `@Enumerated(EnumType.STRING)` on the entity, the comparison is done against the enum's *name* stored as text in the column.

```java
// L21
    Optional<BillingCycle> findByAccountIdAndStatus(Long accountId, BillingCycleStatus status);
```

Returns *at most one* cycle for a given account in a given status, wrapped in `Optional`. The `And` keyword combines both conditions: `... WHERE account_id = ? AND status = ?`. This signature encodes a business assumption that an account has at most one cycle in a particular status at a time (e.g. a single currently-`OPEN` cycle). *Aside: if the data ever violates that assumption and two rows match, Spring Data throws `IncorrectResultSizeDataAccessException` at call time — the `Optional` return type does not by itself guarantee uniqueness; it relies on the data being consistent.*

```java
// L23
    List<BillingCycle> findByStatusAndCycleEndLessThanEqual(BillingCycleStatus status, LocalDate cycleEnd);
```

Finds cycles in a given status whose `cycleEnd` date is on or before a supplied date (`LessThanEqual` → `<=`). Generated SQL is roughly `... WHERE status = ? AND cycle_end <= ?`. This is the classic "which cycles are due to be processed/closed as of this date" query — a service would pass `status = OPEN` and `cycleEnd = today` to find cycles ready for invoicing.

```java
// L25
    Page<BillingCycle> findByAccountId(Long accountId, Pageable pageable);
```

A paginated overload of the L17 finder. Same `WHERE account_id = ?` filter, but the extra `Pageable` argument applies `LIMIT`/`OFFSET`/`ORDER BY`, and the return is a `Page` carrying the slice plus total counts. Java method overloading lets this coexist with the `List`-returning version of the same name. Used by controllers exposing paged endpoints of an account's cycles.

```java
// L27
    Page<BillingCycle> findByAccountIdAndStatus(Long accountId, BillingCycleStatus status, Pageable pageable);
}
```

Paginated, status-filtered variant: `... WHERE account_id = ? AND status = ?` with paging applied. *Aside: note this returns a `Page` (potentially many rows), whereas the non-paginated `findByAccountIdAndStatus` at L21 returns `Optional` (at most one). The two overloads make subtly different assumptions about cardinality — fine because the names/signatures differ, but worth knowing when choosing which to call.* The closing brace ends the interface.

---

## src/main/java/com/teleconnect/billing_service/repository/BillingDisputeRepository.java

Persistence interface for the `BillingDispute` entity (a customer's challenge to charges on an invoice). It provides lookups by invoice, by subscriber, by dispute status, and by the date a dispute was raised.

```java
// L1-L9
package com.teleconnect.billing_service.repository;

import com.teleconnect.billing_service.entity.BillingDispute;
import com.teleconnect.billing_service.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
```

Package declaration plus imports. It brings in the `BillingDispute` entity and the `DisputeStatus` enum (the entity's `status` field type, e.g. `OPEN`/`RESOLVED`/`REJECTED`), the `JpaRepository` base and `@Repository` stereotype, and `LocalDate`/`List` for the finders below. Note there is no `Optional` or `Page` import here — every method returns a `List`.

```java
// L11-L12
@Repository
public interface BillingDisputeRepository extends JpaRepository<BillingDispute, Long> {
```

Marks the persistence component and extends `JpaRepository<BillingDispute, Long>`: manages `BillingDispute` entities keyed by their `Long disputeId`. CRUD operations are inherited.

```java
// L14
    List<BillingDispute> findByInvoiceId(Long invoiceId);
```

Returns all disputes filed against a single invoice (`... WHERE invoice_id = ?`). Because one invoice can attract multiple disputes, the return is a `List`. The `invoiceId` field is a plain `Long` foreign-key-style column on the entity (not a JPA relationship), so this matches by raw value.

```java
// L16
    List<BillingDispute> findBySubscriberId(Long subscriberId);
```

Returns every dispute raised by a given subscriber (`... WHERE subscriber_id = ?`), across all of that subscriber's invoices. Used for "show me this customer's dispute history" views.

```java
// L18
    List<BillingDispute> findByStatus(DisputeStatus status);
```

Returns all disputes currently in a given workflow state (`... WHERE status = ?`, compared as the enum's string name). Typical use: a back-office service listing all `OPEN` disputes awaiting action.

```java
// L20
    List<BillingDispute> findByRaisedDateBetween(LocalDate fromDate, LocalDate toDate);
}
```

Returns disputes whose `raisedDate` falls within an inclusive date range. The `Between` keyword generates `... WHERE raised_date BETWEEN ? AND ?` (SQL `BETWEEN` is inclusive of both endpoints), binding `fromDate` to the lower bound and `toDate` to the upper. Used for reporting disputes raised in a period. The closing brace ends the interface.

---

## src/main/java/com/teleconnect/billing_service/repository/InvoiceRepository.java

Persistence interface for the `Invoice` entity (the billed document for an account in a cycle). This is the richest repository: besides many derived finders by account, status, cycle, and due-date range, it adds two custom JPQL aggregate queries that sum monetary amounts for reporting.

```java
// L1-L12
package com.teleconnect.billing_service.repository;

import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
```

Package plus imports. New compared to the previous files: `org.springframework.data.jpa.repository.Query` enables the `@Query` annotation for explicit JPQL (used at L36/L39), and `java.math.BigDecimal` is the exact-precision decimal type used for money — it is the return type of the two aggregate queries and matches the entity's `totalAmount`/`paidAmount` columns (`precision = 10, scale = 2`). `BigDecimal` (not `double`) is chosen because floating-point types cannot represent currency exactly. Also imports `InvoiceStatus` (the entity's `status` enum), and the usual `List`/`Optional`/`LocalDate`.

```java
// L14-L15
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
```

Persistence component managing `Invoice` entities keyed by `Long invoiceId`, inheriting CRUD.

```java
// L17
    List<Invoice> findByAccountId(Long accountId);
```

All invoices belonging to an account (`... WHERE account_id = ?`). Foundation for an account's invoice list.

```java
// L19
    List<Invoice> findByStatus(InvoiceStatus status);
```

All invoices in a given status (e.g. `UNPAID`, `PAID`, `OVERDUE` — whatever `InvoiceStatus` defines), compared as the stored enum string.

```java
// L21
    Optional<Invoice> findByAccountIdAndCycleId(Long accountId, Long cycleId);
```

Returns at most one invoice for a specific account-and-cycle combination (`... WHERE account_id = ? AND cycle_id = ?`), wrapped in `Optional`. This encodes the rule that one account has a single invoice per billing cycle, so it is the natural lookup when (re)generating or fetching that cycle's invoice. *Aside: as with the earlier `Optional` finder, uniqueness is assumed from the data model, not enforced by this method; duplicate rows would trigger `IncorrectResultSizeDataAccessException`.*

```java
// L23
    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);
```

Finds invoices in a given status whose `dueDate` is strictly before a supplied date (`Before` → `<`, exclusive). Generated SQL: `... WHERE status = ? AND due_date < ?`. The canonical use is overdue detection — e.g. `status = UNPAID` and `date = today` yields invoices that are past due and not yet paid (a batch job could then mark them `OVERDUE` or apply late fees).

```java
// L25
    List<Invoice> findByAccountIdAndStatus(Long accountId, InvoiceStatus status);
```

All invoices for one account filtered by status (`... WHERE account_id = ? AND status = ?`). Returns a `List` because an account can have many invoices in the same status.

```java
// L27
    List<Invoice> findByAccountIdAndDueDateBetween(Long accountId, LocalDate fromDate, LocalDate toDate);
```

Invoices for one account whose `dueDate` falls inclusively within a range: `... WHERE account_id = ? AND due_date BETWEEN ? AND ?`. Parameters bind in declaration order (`accountId`, then the range's `fromDate`/`toDate`).

```java
// L29-L30
    List<Invoice> findByAccountIdAndStatusAndDueDateBetween(Long accountId, InvoiceStatus status,
                                                            LocalDate fromDate, LocalDate toDate);
```

The most specific account finder: filters by account, status, *and* a due-date range simultaneously — `... WHERE account_id = ? AND status = ? AND due_date BETWEEN ? AND ?`. The four parameters map left-to-right to the four conditions. Used for narrow statements like "this account's unpaid invoices due this quarter."

```java
// L32
    List<Invoice> findByDueDateBetween(LocalDate fromDate, LocalDate toDate);
```

System-wide (no account filter) invoices due within a date range: `... WHERE due_date BETWEEN ? AND ?`. Useful for cross-account billing-run or cash-flow reporting.

```java
// L34
    List<Invoice> findByStatusAndDueDateBetween(InvoiceStatus status, LocalDate fromDate, LocalDate toDate);
```

System-wide, status-filtered range query: `... WHERE status = ? AND due_date BETWEEN ? AND ?`. E.g. all `PAID` invoices due in a period, regardless of account.

```java
// L36-L37
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.dueDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumTotalAmountByDueDateBetween(LocalDate fromDate, LocalDate toDate);
```

First custom JPQL query, used for revenue/billed-amount reporting. `@Query` supplies an explicit JPQL string (note it references the *entity* `Invoice` and field `i.totalAmount`/`i.dueDate`, not table/column names). The query selects the **sum of `totalAmount`** over every invoice whose `dueDate` is inclusively within the range. `SUM(...)` would return SQL `NULL` if zero rows match; `COALESCE(SUM(...), 0)` converts that `NULL` to `0`, guaranteeing the method returns a real `BigDecimal` (e.g. `BigDecimal.ZERO`) rather than `null` — saving callers from null checks. The parameters `:fromDate` and `:toDate` are bound from the method arguments.

*Aside: the method arguments are not annotated with `@Param("fromDate")`/`@Param("toDate")`. This works only because the project is compiled with parameter-name retention (the Spring Boot default `-parameters` compiler flag); Spring Data then matches the method parameter names to the JPQL placeholder names. If that flag were ever turned off, these two methods would fail to bind their parameters at startup — the explicit `@Param` annotation would make it robust regardless.*

```java
// L39-L40
    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM Invoice i WHERE i.status = :status AND i.dueDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumPaidAmountByStatusAndDueDateBetween(InvoiceStatus status, LocalDate fromDate, LocalDate toDate);
}
```

Second custom JPQL query, for collected-revenue reporting. It sums the **`paidAmount`** of invoices that both have a given `status` and fall inclusively within the due-date range, again using `COALESCE(..., 0)` to return `0` instead of `null` when nothing matches. Three parameters bind by name (`:status`, `:fromDate`, `:toDate`), with the same parameter-name-retention dependency noted above. A service would combine this with `sumTotalAmountByDueDateBetween` to compute figures like "billed vs. collected in a period." The closing brace ends the interface.

---

## src/main/java/com/teleconnect/billing_service/repository/PaymentRepository.java

Persistence interface for the `Payment` entity (a payment applied to an invoice). It is the smallest repository, adding just two lookups — by invoice and by external transaction reference — on top of inherited CRUD.

```java
// L1-L8
package com.teleconnect.billing_service.repository;

import com.teleconnect.billing_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
```

Package and imports. It pulls in the `Payment` entity, the `JpaRepository` base and `@Repository` stereotype, and the `List`/`Optional` return containers. Notably there is **no enum import** here — `Payment`'s status/method enums (`PaymentStatus`, `PaymentMethod`) exist on the entity but are not used by any query in this repository, so they are not imported.

```java
// L10-L11
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
```

Persistence component managing `Payment` entities keyed by `Long paymentId`, inheriting all CRUD operations.

```java
// L13
    List<Payment> findByInvoiceId(Long invoiceId);
```

Returns all payments recorded against a single invoice (`... WHERE invoice_id = ?`). A `List` because an invoice may be settled by several partial payments over time; a service can sum these to determine how much of an invoice has been paid.

```java
// L15
    Optional<Payment> findByTransactionRef(String transactionRef);
}
```

Looks up a single payment by its external `transactionRef` (the reference returned by a payment gateway/processor), returning `Optional` since at most one payment should carry a given reference. This is backed by the entity's `@Column(unique = true)` constraint on `transactionRef`, so the at-most-one guarantee is enforced at the database level — making this the idempotency/reconciliation lookup (e.g. "have we already booked the payment for gateway transaction X?"). The closing brace ends the interface.

---

## How this connects

These four interfaces sit directly **below the service layer and above the database**. Service classes (e.g. an invoice/billing/dispute/payment service) declare a field of each repository type and receive a Spring-generated proxy via constructor injection; they call the inherited CRUD methods (`save`, `findById`, `findAll`, `deleteById`, …) plus the custom finders documented above to fulfil business operations, often wrapping those calls in `@Transactional` boundaries defined at the service level.

Each repository is bound to exactly one **entity** — `BillingCycle`, `BillingDispute`, `Invoice`, `Payment` — and the property names inside every derived-query method name (`accountId`, `status`, `cycleEnd`, `raisedDate`, `dueDate`, `cycleId`, `invoiceId`, `subscriberId`, `transactionRef`, `totalAmount`, `paidAmount`) correspond one-to-one with the fields declared on those entities. Spring Data validates this mapping at startup, so the entity definitions and these interfaces must evolve together. The query results flow back up as entity objects, which services typically map into DTOs before the **controller** returns them to the client — completing the Controller → Service → Repository → Entity/DB round trip.
