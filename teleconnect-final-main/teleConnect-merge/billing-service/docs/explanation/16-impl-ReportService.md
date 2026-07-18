# ReportServiceImpl — Collection & Overdue Reporting Business Logic

`ReportServiceImpl` is the concrete service-layer implementation that produces aggregate reports for the billing system: an **overdue report** (with aging buckets), a **collection report** (billed vs. collected money with an efficiency percentage), and a **dispute summary** (counts and SLA compliance for billing disputes). In the standard Spring layering — **Controller → Service → Repository → Entity/DB** — this class sits in the *Service* layer: a report controller calls the `ReportService` interface, this implementation reads raw `Invoice` and `BillingDispute` entities through two Spring Data JPA repositories, computes derived figures in memory using Java Streams and `BigDecimal` math, and returns plain response DTOs back up to the controller for serialization to JSON.

---

## src/main/java/com/teleconnect/billing_service/service/impl/ReportServiceImpl.java

This file implements the `ReportService` interface. It is read-only with respect to the database (it only queries), and all the reporting/aggregation logic lives here.

---

### Package declaration

```java
// L1
package com.teleconnect.billing_service.service.impl;
```

Declares the package this class belongs to. By convention, concrete service implementations live in the `service.impl` sub-package, while the interface (`ReportService`) lives one level up in `service`. The package name also maps to the directory path on disk.

---

### Imports — DTOs

```java
// L3-L6
import com.teleconnect.billing_service.dto.response.CollectionReportResponse;
import com.teleconnect.billing_service.dto.response.DisputeSummaryResponse;
import com.teleconnect.billing_service.dto.response.OverdueReportResponse;
import com.teleconnect.billing_service.dto.response.OverdueReportResponse.OverdueInvoiceItem;
```

These bring in the three response DTOs (Data Transfer Objects) that each public method returns. A DTO is a simple data-holder class used to shape the data sent back to the API client, decoupled from the persistence entities. Note line 6 imports the **nested static class** `OverdueInvoiceItem` defined inside `OverdueReportResponse`; importing it by its nested name lets the code refer to it simply as `OverdueInvoiceItem` rather than `OverdueReportResponse.OverdueInvoiceItem`.

---

### Imports — entities

```java
// L7-L8
import com.teleconnect.billing_service.entity.BillingDispute;
import com.teleconnect.billing_service.entity.Invoice;
```

The two JPA `@Entity` classes this service reads. `Invoice` maps to the `invoices` table; `BillingDispute` maps to the `billing_disputes` table. An entity is a Java class whose instances correspond to rows in a database table.

---

### Imports — enums

```java
// L9-L10
import com.teleconnect.billing_service.enums.DisputeStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
```

Type-safe enumerations used to filter records by their lifecycle state. `InvoiceStatus` has values `GENERATED, SENT, PAID, OVERDUE, DISPUTED`; `DisputeStatus` has values `OPEN, UNDER_REVIEW, RESOLVED, REJECTED`. These are stored in the database as strings (the entities annotate the status fields with `@Enumerated(EnumType.STRING)`).

---

### Imports — repositories and the interface

```java
// L11-L13
import com.teleconnect.billing_service.repository.BillingDisputeRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.service.ReportService;
```

`InvoiceRepository` and `BillingDisputeRepository` are Spring Data JPA repository interfaces (each extends `JpaRepository`) that give CRUD plus derived query methods over their entities. `ReportService` is the interface this class implements — the contract the controller depends on.

---

### Imports — Spring

```java
// L14-L15
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
```

`@Autowired` marks a field/constructor for **dependency injection** — Spring supplies the required bean at runtime instead of the code constructing it. `@Service` is a Spring stereotype annotation that marks this class as a service-layer Spring bean so the container detects it during component scanning and manages its lifecycle.

---

### Imports — JDK utilities

```java
// L17-L22
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
```

- `BigDecimal` — arbitrary-precision decimal used for money, avoiding the rounding errors of `double`/`float`.
- `RoundingMode` — controls how `BigDecimal` rounds (here `HALF_UP`, i.e. standard "round half away from zero").
- `LocalDate` — a date without time-of-day or zone; used for due dates and report date ranges.
- `ChronoUnit` — provides `DAYS.between(...)` to compute the number of days between two dates.
- `List` — the collection type for query results and item lists.
- `Collectors` — terminal stream collectors, here `Collectors.toList()` to materialize a stream into a `List`.

---

### Class declaration and `@Service`

```java
// L24-L25
@Service
public class ReportServiceImpl implements ReportService {
```

`@Service` registers this class as a Spring-managed bean (a singleton by default), making it injectable wherever a `ReportService` is required. `implements ReportService` means it must provide concrete bodies for the three interface methods (`getOverdueReport`, `getCollectionReport`, `getDisputeSummary`); controllers program against the interface, and Spring injects this implementation.

---

### Field — `invoiceRepository`

```java
// L27-L28
    @Autowired
    private InvoiceRepository invoiceRepository;
```

A dependency on the invoice repository. `@Autowired` on the field tells Spring to inject the singleton `InvoiceRepository` proxy here at startup (this is **field injection**). Through it the service reads invoices by status, by due-date range, etc.

*Aside: field injection works but is generally discouraged versus constructor injection because it makes the dependency harder to set in unit tests without reflection and prevents the field from being `final`. This is a style note, not a bug.*

---

### Field — `disputeRepository`

```java
// L30-L31
    @Autowired
    private BillingDisputeRepository disputeRepository;
```

A dependency on the billing-dispute repository, injected the same way. Used by `getDisputeSummary` to fetch disputes raised within a date range.

---

### Method `getOverdueReport` — signature and `@Override`

```java
// L33-L34
    @Override
    public OverdueReportResponse getOverdueReport(String region, String agingBucket) {
```

`@Override` asserts at compile time that this method really overrides a method declared in `ReportService` (a safety check against signature typos). The method takes:
- `region` — a label describing the region the report is for. *Note: as we'll see, `region` is only echoed back into the response; it is NOT used to filter the data.*
- `agingBucket` — an optional string like `"0-30"`, `"31-60"`, `"61-90"`, or `"90+"` used to filter overdue invoices by how long they've been overdue.

It returns an `OverdueReportResponse` DTO.

---

### Fetch overdue invoices and capture "today"

```java
// L35-L36
        List<Invoice> overdueInvoices = invoiceRepository.findByStatus(InvoiceStatus.OVERDUE);
        LocalDate today = LocalDate.now();
```

Line 35 calls the derived query `findByStatus(OVERDUE)`, which Spring Data translates into `SELECT * FROM invoices WHERE status = 'OVERDUE'`, returning every invoice currently flagged overdue. Line 36 snapshots the current system date once so all aging calculations in this call use a single consistent "today" rather than re-reading the clock per invoice.

---

### Filter by aging bucket

```java
// L38-L40
        List<Invoice> filtered = overdueInvoices.stream()
                .filter(inv -> matchesAgingBucket(inv.getDueDate(), today, agingBucket))
                .collect(Collectors.toList());
```

Opens a stream over the overdue invoices and keeps only those whose due date falls in the requested aging bucket, delegating the bucket logic to the private helper `matchesAgingBucket` (described at the end). If `agingBucket` is null/blank, the helper returns `true` for everything (no filtering). `Collectors.toList()` collects the surviving invoices into a new `List` named `filtered`.

---

### Sum the overdue amount

```java
// L42-L44
        BigDecimal totalAmount = filtered.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
```

Streams the filtered invoices, maps each to its `totalAmount` (a `BigDecimal`) via the method reference `Invoice::getTotalAmount`, then `reduce` folds them into a single sum. The reduction starts from the identity `BigDecimal.ZERO` and combines values with `BigDecimal::add`. Because it seeds with `ZERO`, an empty list yields `0` rather than throwing, and the result is never null.

---

### Build the per-invoice item list

```java
// L46-L54
        List<OverdueInvoiceItem> items = filtered.stream()
                .map(inv -> new OverdueInvoiceItem(
                        inv.getInvoiceId(),
                        inv.getAccountId(),
                        inv.getTotalAmount(),
                        inv.getDueDate(),
                        ChronoUnit.DAYS.between(inv.getDueDate(), today)
                ))
                .collect(Collectors.toList());
```

Transforms each filtered `Invoice` entity into a lightweight `OverdueInvoiceItem` DTO. The `OverdueInvoiceItem` constructor takes, in order: `invoiceId` (`Long`), `accountId` (`Long`), `totalAmount` (`BigDecimal`), `dueDate` (`LocalDate`), and `daysOverdue` (`long`). The last value is computed with `ChronoUnit.DAYS.between(dueDate, today)`, i.e. how many whole days have elapsed since the due date (positive when the due date is in the past). The results are collected into the `items` list. This is the entity→DTO mapping step that keeps internal entity fields from leaking into the API.

---

### Assemble and return the overdue response

```java
// L56-L57
        return new OverdueReportResponse(agingBucket, region, filtered.size(), totalAmount, items);
    }
```

Constructs the response DTO. Matching the `OverdueReportResponse` constructor's parameter order, the arguments are: `agingBucket` (echoed back), `region` (echoed back unchanged), `totalOverdueCount` = `filtered.size()` (number of invoices that passed the bucket filter), `totalOverdueAmount` = `totalAmount`, and `content` = `items`. The closing brace ends the method.

*Aside: `region` is passed through to the response but never used to filter the invoices — `findByStatus` ignores region entirely. So the report's `region` label may not actually correspond to a region-scoped data set. Whether that's intended depends on the data model (the `Invoice` entity has no region field), but it's worth flagging as a possible gap.*

---

### Method `getCollectionReport` — signature

```java
// L59-L60
    @Override
    public CollectionReportResponse getCollectionReport(LocalDate fromDate, LocalDate toDate, String region) {
```

Overrides the interface method. Parameters: `fromDate` and `toDate` define the inclusive due-date window for the report; `region` is again a label that is echoed into the response but not used to filter. Returns a `CollectionReportResponse`.

---

### Fetch invoices in the date range

```java
// L61
        List<Invoice> invoicesInRange = invoiceRepository.findByDueDateBetween(fromDate, toDate);
```

Calls the derived query `findByDueDateBetween`, which becomes `SELECT * FROM invoices WHERE due_date BETWEEN :fromDate AND :toDate`. In Spring Data, `Between` is inclusive of both bounds. This collects all invoices whose due date sits within the requested window, regardless of status.

---

### Total billed

```java
// L63-L65
        BigDecimal totalBilled = invoicesInRange.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
```

Sums the `totalAmount` of every invoice in the range (paid, unpaid, overdue — all of them) to get the gross amount billed in that window. Same `ZERO`-seeded reduction pattern as before, so the result is never null and is `0` for an empty range.

---

### Filter the paid invoices

```java
// L67-L69
        List<Invoice> paidInvoices = invoicesInRange.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .collect(Collectors.toList());
```

Keeps only invoices whose status is exactly `PAID`. Enum constants are singletons, so comparing with `==` is correct and intentional here (reference equality is identity equality for enums). The result feeds both the collected-amount sum and the `invoicesPaid` count in the response.

---

### Total collected

```java
// L71-L73
        BigDecimal totalCollected = paidInvoices.stream()
                .map(inv -> inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
```

Sums `paidAmount` across the paid invoices. The ternary `getPaidAmount() != null ? getPaidAmount() : BigDecimal.ZERO` defensively substitutes `ZERO` when `paidAmount` is null, preventing a `NullPointerException` during the reduction. (In practice the `Invoice` entity defaults `paidAmount` to `BigDecimal.ZERO` and the column is non-null, so this guard is belt-and-suspenders.) The result is the actual cash collected on invoices billed in the window.

---

### Total outstanding

```java
// L75
        BigDecimal totalOutstanding = totalBilled.subtract(totalCollected);
```

Outstanding (unrecovered) money = billed minus collected, using exact `BigDecimal` subtraction. *Note: this is computed across all in-range invoices versus only the paid ones' `paidAmount`; partial payments on non-`PAID` invoices are not counted as collected, so they remain entirely in "outstanding."*

---

### Collection efficiency percentage

```java
// L77-L81
        double efficiency = totalBilled.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : totalCollected.divide(totalBilled, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();
```

Computes the collection efficiency as a percentage. The guard `totalBilled.compareTo(BigDecimal.ZERO) == 0` checks whether billed is exactly zero (using `compareTo` because `BigDecimal.equals` is scale-sensitive — e.g. `0` vs `0.00` — whereas `compareTo` compares numeric value). If nothing was billed, efficiency is `0.0`, which also avoids a divide-by-zero. Otherwise it divides collected/billed to 4 decimal places with `HALF_UP` rounding, multiplies by 100 to make it a percentage, fixes the scale at 2 decimals, and converts the `BigDecimal` to a primitive `double` for the response field. Example: collected 75, billed 100 → `0.7500 × 100 = 75.00` → `75.0`.

---

### Count overdue invoices in range

```java
// L83-L85
        long invoicesOverdue = invoicesInRange.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.OVERDUE)
                .count();
```

Counts how many invoices in the window currently have status `OVERDUE`. `count()` returns a `long`. This is a snapshot count of present overdue status, independent of the paid/collected figures above.

---

### Assemble and return the collection response

```java
// L87-L91
        return new CollectionReportResponse(
                fromDate, toDate, region,
                totalBilled, totalCollected, totalOutstanding,
                efficiency, paidInvoices.size(), (int) invoicesOverdue);
    }
```

Builds the `CollectionReportResponse` in constructor order: `fromDate`, `toDate`, `region` (echoed), `totalBilled`, `totalCollected`, `totalOutstanding`, `collectionEfficiency` = `efficiency`, `invoicesPaid` = `paidInvoices.size()`, and `invoicesOverdue` = `(int) invoicesOverdue`. The cast `(int)` narrows the `long` count to `int` because the DTO field is `int`; this is safe unless the count exceeds `Integer.MAX_VALUE` (~2.1 billion), which is unrealistic for invoice volumes. The closing brace ends the method.

---

### Method `getDisputeSummary` — signature

```java
// L93-L94
    @Override
    public DisputeSummaryResponse getDisputeSummary(LocalDate fromDate, LocalDate toDate) {
```

Overrides the interface method. Parameters `fromDate`/`toDate` bound the dispute *raised-date* window (inclusive). Returns a `DisputeSummaryResponse` with totals, SLA metrics, and per-status counts. No `region` parameter here.

---

### Fetch disputes and count total

```java
// L95-L97
        List<BillingDispute> disputes = disputeRepository.findByRaisedDateBetween(fromDate, toDate);

        int total = disputes.size();
```

Line 95 runs the derived query `findByRaisedDateBetween` → `SELECT * FROM billing_disputes WHERE raised_date BETWEEN :fromDate AND :toDate` (inclusive). Line 97 records the total number of disputes raised in the window; this is the denominator for the SLA compliance rate.

---

### Acknowledged within 24 hours

```java
// L99-L103
        long acknowledgedWithin24h = disputes.stream()
                .filter(d -> d.getAcknowledgedDate() != null)
                .filter(d -> !d.getAcknowledgedDate().isAfter(
                        d.getRaisedDate().atStartOfDay().plusHours(24)))
                .count();
```

Counts disputes acknowledged within the 24-hour SLA. The first `filter` keeps only disputes that have an `acknowledgedDate` (it's a nullable `LocalDateTime`). The second filter keeps a dispute when its acknowledged timestamp is **not after** the deadline. The deadline is built from the dispute's `raisedDate` (`LocalDate`) by `atStartOfDay()` (midnight at the start of the raised day, giving a `LocalDateTime`) plus 24 hours. Using `!isAfter(deadline)` means exactly-at-the-deadline still counts as compliant (inclusive boundary). `count()` returns the matching count as a `long`.

*Note: the deadline is measured from the **start** (midnight) of the raised date, not from the actual moment the dispute was raised. The raised date carries no time-of-day, so this is the only choice the data permits — but it means the effective window can be up to nearly 48 hours of wall-clock time if a dispute is "raised" late in the day. Worth knowing when interpreting the metric.*

---

### Resolved within 5 days

```java
// L105-L110
        long resolvedWithin5Days = disputes.stream()
                .filter(d -> d.getResolvedDate() != null
                        && d.getStatus() == DisputeStatus.RESOLVED)
                .filter(d -> !d.getResolvedDate().toLocalDate().isAfter(
                        d.getRaisedDate().plusDays(5)))
                .count();
```

Counts disputes resolved within 5 days. The first filter requires both a non-null `resolvedDate` and a status of exactly `RESOLVED` (so rejected/closed-otherwise disputes are excluded even if they have a resolved timestamp). The second filter compares dates at day granularity: it takes the resolved timestamp's date part (`toLocalDate()`) and keeps the dispute if that date is **not after** `raisedDate + 5 days`. Again `!isAfter` makes the 5th day inclusive. Result is a `long` count.

---

### SLA breaches

```java
// L112-L116
        long slaBreaches = disputes.stream()
                .filter(d -> d.getAcknowledgedDate() == null
                        || d.getAcknowledgedDate().isAfter(
                                d.getRaisedDate().atStartOfDay().plusHours(24)))
                .count();
```

Counts SLA breaches on the **acknowledgement** SLA. A dispute breaches if either it was never acknowledged (`acknowledgedDate == null`) **or** it was acknowledged after the 24-hour deadline (same deadline expression as in `acknowledgedWithin24h`). This is the logical complement of `acknowledgedWithin24h`, so for a given dataset `acknowledgedWithin24h + slaBreaches == total`.

---

### SLA compliance rate

```java
// L118-L123
        double slaComplianceRate = total == 0 ? 0.0
                : BigDecimal.valueOf(acknowledgedWithin24h)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();
```

Computes the percentage of disputes acknowledged within 24 hours. Guards against divide-by-zero: if `total == 0`, the rate is `0.0`. Otherwise it converts the two `long` counts to `BigDecimal` (`BigDecimal.valueOf(...)`), divides acknowledged/total to 4 decimals (`HALF_UP`), multiplies by 100, sets the scale to 2 decimals, and converts to a primitive `double`. This intentionally uses `BigDecimal` rather than `long`/`double` division to control rounding precisely.

---

### Per-status counts

```java
// L125-L128
        long resolved = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.RESOLVED).count();
        long rejected = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.REJECTED).count();
        long open = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.OPEN).count();
        long underReview = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.UNDER_REVIEW).count();
```

Four independent stream passes over the same `disputes` list, each counting disputes in one `DisputeStatus` state: `RESOLVED`, `REJECTED`, `OPEN`, and `UNDER_REVIEW`. Together these cover every enum value, so `resolved + rejected + open + underReview == total`. Each `count()` returns a `long`.

*Aside: this performs four separate iterations over the list. For large result sets a single grouping pass (e.g. `Collectors.groupingBy(BillingDispute::getStatus, Collectors.counting())`) would be more efficient, but the current approach is clearer and fine for typical dispute volumes.*

---

### Assemble and return the dispute summary

```java
// L130-L134
        return new DisputeSummaryResponse(
                fromDate, toDate, total,
                (int) acknowledgedWithin24h, (int) resolvedWithin5Days, (int) slaBreaches,
                slaComplianceRate, (int) resolved, (int) rejected, (int) open, (int) underReview);
    }
```

Builds the response in constructor order: `fromDate`, `toDate`, `totalDisputes` = `total`, then the `long` counts each narrowed to `int` (`acknowledgedWithin24h`, `resolvedWithin5Days`, `slaBreaches`), `slaComplianceRate` (a `double`), and the four per-status counts (`resolved`, `rejected`, `open`, `underReview`) also cast to `int`. The `(int)` casts match the DTO's `int` fields and are safe for realistic counts. The closing brace ends the method.

---

### Private helper `matchesAgingBucket`

```java
// L136-L137
    private boolean matchesAgingBucket(LocalDate dueDate, LocalDate today, String agingBucket) {
        if (agingBucket == null || agingBucket.isBlank()) return true;
```

A `private` helper used only by `getOverdueReport` to decide whether a single invoice belongs to the requested aging bucket. Parameters: the invoice's `dueDate`, the shared `today` snapshot, and the requested `agingBucket` string; returns a `boolean`. Line 137 short-circuits: if no bucket was requested (`null` or blank — `String.isBlank()` is true for empty or whitespace-only strings), it returns `true` so the invoice passes through unfiltered.

---

### Compute days overdue and bucket-match via switch

```java
// L138-L146
        long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
        return switch (agingBucket) {
            case "0-30" -> daysOverdue >= 0 && daysOverdue <= 30;
            case "31-60" -> daysOverdue >= 31 && daysOverdue <= 60;
            case "61-90" -> daysOverdue >= 61 && daysOverdue <= 90;
            case "90+" -> daysOverdue > 90;
            default -> true;
        };
    }
```

Line 138 computes whole days between the invoice's due date and today (`ChronoUnit.DAYS.between`), positive when the due date is in the past. Lines 139-145 use a **switch expression** (Java 14+) that returns a `boolean` directly from each arrow-form branch:
- `"0-30"` → overdue between 0 and 30 days inclusive (note this also admits invoices due today or — if data allowed — not yet overdue, since `>= 0`).
- `"31-60"` → 31 to 60 days.
- `"61-90"` → 61 to 90 days.
- `"90+"` → strictly more than 90 days.
- `default` → any unrecognized bucket string returns `true`, i.e. unknown buckets do not filter anything out.

The arrow form means there is no fall-through and no `break` needed; exactly one branch's value is returned. The closing brace ends the method, and the final `}` (L147) closes the class.

*Aside: the boundaries are mutually exclusive and contiguous (0-30, 31-60, 61-90, 90+), so a given `daysOverdue ≥ 0` matches exactly one of the named buckets — there's no overlap or gap. Negative `daysOverdue` (a due date in the future) would match none of the named buckets and be filtered out, though `OVERDUE`-status invoices realistically have past due dates.*

---

## How this connects

- **Upstream (Controller layer):** A report controller (the one mapping `ReportService` to REST endpoints) injects the `ReportService` interface and calls `getOverdueReport`, `getCollectionReport`, or `getDisputeSummary`. Spring wires this `ReportServiceImpl` in as the implementation. The returned DTOs (`OverdueReportResponse`, `CollectionReportResponse`, `DisputeSummaryResponse`) are serialized to JSON for the client.
- **Downstream (Repository/Entity/DB layer):** This service reads data exclusively through `InvoiceRepository.findByStatus` / `findByDueDateBetween` and `BillingDisputeRepository.findByRaisedDateBetween` — Spring Data JPA derived queries that hit the `invoices` and `billing_disputes` tables and return `Invoice` and `BillingDispute` entities.
- **Aggregation responsibility:** All filtering, summing, counting, percentage, and SLA logic is done in memory here using Java Streams and `BigDecimal`, keeping the repositories thin and the controllers free of business rules. Notably, the `region` parameter is passed through to responses but does not drive any query, and there is no `@Transactional` annotation on these read-only methods (acceptable for pure reads, though each repository call may open its own short transaction).
