# Service Interfaces (Business-Logic Contracts)

This document explains the five **service interfaces** of the billing microservice: `BillingCycleService`, `BillingDisputeService`, `InvoiceService`, `PaymentService`, and `ReportService`. In the layered architecture **Controller → Service → Repository → Entity/DB**, these interfaces are the *contracts* that controllers depend on: a REST controller is injected with one of these interface types and calls its methods to perform business operations, while the concrete `*ServiceImpl` classes (which `implements` these interfaces) hold the actual logic that talks to repositories and entities. Programming to an interface like this decouples the web layer from the implementation, makes the implementation swappable, and makes the services easy to mock in unit tests.

A note on the data types you will see repeatedly:
- **DTO (Data Transfer Object)** — plain objects in the `dto.request` / `dto.response` packages used to carry data across the boundary between the web layer and the service layer, instead of exposing JPA entities directly. `*Request` DTOs are inbound (what the client sends), `*Response` DTOs are outbound (what the service returns).
- **Enum types** (in the `enums` package) — fixed sets of named constants such as `BillingCycleStatus`, `DisputeStatus`, and `InvoiceStatus` used to represent state.
- **`List<T>`** — an ordered collection (`java.util.List`) returned when a method yields many results.
- **`Page<T>` / `Pageable`** (Spring Data) — `Pageable` is an inbound description of *which page* of results to fetch (page number, page size, sort); `Page<T>` is the returned slice of results plus paging metadata (total elements, total pages, etc.). These appear when an endpoint supports paginated browsing.
- **`Long`** — the database primary-key identifier type used throughout (account IDs, invoice IDs, etc.).
- **`LocalDate`** (`java.time.LocalDate`) — a date without a time-of-day or timezone, used for date-range filters.
- **`byte[]`** — a raw byte array, here used to return generated PDF binary content.

Because these are interfaces, they contain **no method bodies and no logic** — only method *signatures* (declarations). Every method listed is implicitly `public abstract`. The "step-by-step logic" of each operation lives in the corresponding `*ServiceImpl`; here we document the *contract* each method promises: its inputs, its output, and the business operation it represents.

---

## src/main/java/com/teleconnect/billing_service/service/BillingCycleService.java

Contract for managing **billing cycles** — the recurring periods over which an account is billed — including creating cycles, batch-generating invoices for a cycle, querying cycles, and transitioning their status.

```java
// L1
package com.teleconnect.billing_service.service;
```
The **package declaration**. It places this interface in the `com.teleconnect.billing_service.service` namespace, which (by Java/Maven convention) maps to the directory `src/main/java/com/teleconnect/billing_service/service`. All five service interfaces share this package, so they can refer to each other without imports, and Spring component-scanning of this base package will discover their implementations.

```java
// L3-L7
import com.teleconnect.billing_service.dto.request.BillingCycleRequest;
import com.teleconnect.billing_service.dto.request.CycleGenerationRequest;
import com.teleconnect.billing_service.dto.response.BatchGenerationResponse;
import com.teleconnect.billing_service.dto.response.BillingCycleResponse;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
```
These **imports** bring in the project types used in the method signatures below. `BillingCycleRequest` and `CycleGenerationRequest` are inbound request DTOs (the data a client sends to create a cycle or to generate invoices). `BatchGenerationResponse` and `BillingCycleResponse` are outbound response DTOs (what the service returns). `BillingCycleStatus` is the enum describing the lifecycle state of a cycle (e.g. open/closed). Importing a class lets us reference it by its short name rather than its fully-qualified name.

```java
// L8-L9
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```
These import **Spring Data**'s pagination abstractions. `Pageable` is the request side of pagination (page index, page size, sort order); `Page<T>` is the response side (one page of items plus total counts). They appear because one query method supports paged results. Their presence here shows the service contract — not just the repository — is pagination-aware.

```java
// L11
import java.util.List;
```
Imports the JDK `List` interface, used as the return type for the "find many" query methods.

```java
// L13
public interface BillingCycleService {
```
Declares a **public interface** named `BillingCycleService`. An interface defines a set of method signatures with no implementation; a class such as `BillingCycleServiceImpl` will `implement` it and supply the bodies. Controllers depend on this type (not the concrete class), and Spring injects the implementation at runtime.

```java
// L15
    BillingCycleResponse createBillingCycle(BillingCycleRequest request);
```
**`createBillingCycle`** — takes a `BillingCycleRequest` (the details of the cycle to create) and returns a `BillingCycleResponse` representing the newly created cycle. The contract: persist a new billing cycle and return its created form (typically including its generated ID and computed fields).

```java
// L17
    BatchGenerationResponse generateInvoicesBatch(CycleGenerationRequest request);
```
**`generateInvoicesBatch`** — takes a `CycleGenerationRequest` and returns a `BatchGenerationResponse`. This is a *bulk* operation: generate invoices for many accounts/subscribers belonging to a billing cycle in one pass, returning a summary of the batch (e.g. how many invoices succeeded/failed). The distinct response type (`BatchGenerationResponse` rather than a single invoice) signals this is an aggregate result.

```java
// L19
    BillingCycleResponse getBillingCycleById(Long cycleId);
```
**`getBillingCycleById`** — takes a cycle's primary key `cycleId` (`Long`) and returns a single `BillingCycleResponse`. The contract is a lookup-by-ID; the implementation is expected to throw a "not found" exception (rather than return `null`) when no cycle matches, consistent with the project's exception-based error handling.

```java
// L21
    List<BillingCycleResponse> getCyclesByAccount(Long accountId);
```
**`getCyclesByAccount`** (single-argument overload) — takes an `accountId` and returns a `List<BillingCycleResponse>`: all billing cycles belonging to that account, unpaginated.

```java
// L23
    Page<BillingCycleResponse> getCyclesByAccount(Long accountId, BillingCycleStatus status, Pageable pageable);
```
**`getCyclesByAccount`** (three-argument **overload** — same method name, different parameter list, resolved by Java at compile time). It takes an `accountId`, an optional `status` filter (`BillingCycleStatus`), and a `Pageable` describing the requested page, and returns a `Page<BillingCycleResponse>` — one page of cycles plus paging metadata. This is the paginated/filterable counterpart to the simple list version above; controllers typically call this one for browsable, server-side-paged listings.

```java
// L25
    List<BillingCycleResponse> getCyclesByStatus(BillingCycleStatus status);
```
**`getCyclesByStatus`** — takes a `BillingCycleStatus` and returns a `List<BillingCycleResponse>` of all cycles currently in that state (e.g. all `OPEN` cycles). Useful for operational/batch processing.

```java
// L27
    BillingCycleResponse updateCycleStatus(Long cycleId, BillingCycleStatus status);
```
**`updateCycleStatus`** — takes a `cycleId` and a target `status`, transitions the cycle to that status, and returns the updated `BillingCycleResponse`. This is a general state-transition operation; business rules about which transitions are legal live in the implementation.

```java
// L29-L30
    BillingCycleResponse closeBillingCycle(Long cycleId);
}
```
**`closeBillingCycle`** — takes a `cycleId` and returns the updated `BillingCycleResponse`. This is a *specialized* state transition (closing a cycle), separate from the generic `updateCycleStatus`, likely because closing carries extra business semantics (e.g. finalizing invoices, preventing further changes). The closing brace `}` ends the interface.

---

## src/main/java/com/teleconnect/billing_service/service/BillingDisputeService.java

Contract for the **billing dispute** workflow — raising a dispute against an invoice, querying disputes, and moving a dispute through its review and resolution lifecycle.

```java
// L1
package com.teleconnect.billing_service.service;
```
Package declaration placing this interface in the shared `...service` package (see the explanation under `BillingCycleService`).

```java
// L3-L7
import com.teleconnect.billing_service.dto.request.DisputeRequest;
import com.teleconnect.billing_service.dto.request.DisputeResolveRequest;
import com.teleconnect.billing_service.dto.request.DisputeReviewRequest;
import com.teleconnect.billing_service.dto.response.DisputeResponse;
import com.teleconnect.billing_service.enums.DisputeStatus;
```
Imports the dispute-related DTOs and enum. `DisputeRequest` is the payload to raise a new dispute. `DisputeReviewRequest` and `DisputeResolveRequest` are the payloads for the two later workflow steps (review and resolution) — having separate DTOs lets each step require its own fields. `DisputeResponse` is the outbound view of a dispute. `DisputeStatus` enumerates the dispute's lifecycle states (e.g. raised, under review, resolved).

```java
// L9
import java.util.List;
```
Imports `List` for the "find many" query methods.

```java
// L11
public interface BillingDisputeService {
```
Declares the public `BillingDisputeService` interface — the contract implemented by `BillingDisputeServiceImpl` and depended on by the dispute controller.

```java
// L13
    DisputeResponse raiseDispute(DisputeRequest request);
```
**`raiseDispute`** — takes a `DisputeRequest` and returns a `DisputeResponse`. Creates a new dispute (the entry point of the workflow), typically against a specific invoice, and returns the created dispute with its generated ID and initial status.

```java
// L15
    DisputeResponse getDisputeById(Long disputeId);
```
**`getDisputeById`** — lookup of a single dispute by its primary key `disputeId`, returning a `DisputeResponse`. As with other by-ID lookups, a missing record is expected to surface as a "not found" exception in the implementation.

```java
// L17
    List<DisputeResponse> getDisputesByInvoice(Long invoiceId);
```
**`getDisputesByInvoice`** — returns all disputes (`List<DisputeResponse>`) raised against a given invoice (`invoiceId`).

```java
// L19
    List<DisputeResponse> getDisputesBySubscriber(Long subscriberId);
```
**`getDisputesBySubscriber`** — returns all disputes (`List<DisputeResponse>`) associated with a given subscriber (`subscriberId`). "Subscriber" here is the customer/end-user dimension, distinct from "account".

```java
// L21
    List<DisputeResponse> getDisputesByAccount(Long accountId, DisputeStatus status);
```
**`getDisputesByAccount`** — returns disputes for a given `accountId`, optionally filtered by `status` (`DisputeStatus`). Unlike the cycle service's account query, this one is not overloaded; the status filter is a required parameter of this single method (the implementation may treat a `null` status as "no filter").

```java
// L23
    List<DisputeResponse> getDisputesByStatus(DisputeStatus status);
```
**`getDisputesByStatus`** — returns all disputes (`List<DisputeResponse>`) currently in the given `DisputeStatus`, regardless of account/invoice — useful for queue/work-list views.

```java
// L25
    DisputeResponse updateDisputeStatus(Long disputeId, DisputeStatus status);
```
**`updateDisputeStatus`** — a generic state transition: set the dispute identified by `disputeId` to the supplied `status` and return the updated `DisputeResponse`.

```java
// L27
    DisputeResponse reviewDispute(Long disputeId, DisputeReviewRequest request);
```
**`reviewDispute`** — takes a `disputeId` plus a `DisputeReviewRequest` (data captured during the review step, e.g. reviewer notes/decision) and returns the updated `DisputeResponse`. This is a dedicated, semantically-rich transition for the *review* stage of the workflow rather than the generic status setter.

```java
// L29-L30
    DisputeResponse resolveDispute(Long disputeId, DisputeResolveRequest request);
}
```
**`resolveDispute`** — takes a `disputeId` plus a `DisputeResolveRequest` (resolution details, e.g. accepted/rejected, adjustment amount) and returns the updated `DisputeResponse`. This is the terminal step of the dispute workflow. The closing `}` ends the interface.

*Aside: the three workflow methods (`raiseDispute` → `reviewDispute` → `resolveDispute`) plus the generic `updateDisputeStatus` together imply a state machine; the legality of transitions and any validation is enforced in the implementation, not in this interface.*

---

## src/main/java/com/teleconnect/billing_service/service/InvoiceService.java

The largest contract: it covers the full **invoice** lifecycle — generation, querying, payment application, late-fee handling, overdue marking, sending, and PDF export — making it the central business interface of the service.

```java
// L1
package com.teleconnect.billing_service.service;
```
Package declaration; same shared `...service` package.

```java
// L3-L8
import com.teleconnect.billing_service.dto.request.InvoiceGenerationRequest;
import com.teleconnect.billing_service.dto.request.LateFeeRequest;
import com.teleconnect.billing_service.dto.request.LateFeeWaiverRequest;
import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.InvoiceResponse;
import com.teleconnect.billing_service.enums.InvoiceStatus;
```
Imports the invoice DTOs and enum. `InvoiceGenerationRequest` is the payload for creating an invoice. `LateFeeRequest` / `LateFeeWaiverRequest` are payloads for applying and waiving late fees respectively (separate DTOs because the data differs — e.g. a fee amount versus a waiver reason). `PaymentRequest` is reused here (it is the same DTO used by `PaymentService`) because invoices can be paid directly. `InvoiceResponse` is the outbound invoice view. `InvoiceStatus` enumerates invoice states (e.g. issued, paid, overdue).

```java
// L10-L11
import java.time.LocalDate;
import java.util.List;
```
`LocalDate` (a date with no time/zone) is imported for the date-range filter parameters; `List` is imported for the multi-result query methods.

```java
// L13
public interface InvoiceService {
```
Declares the public `InvoiceService` interface, implemented by `InvoiceServiceImpl` and depended on by the invoice controller.

```java
// L15
    InvoiceResponse generateInvoice(InvoiceGenerationRequest request);
```
**`generateInvoice`** — takes an `InvoiceGenerationRequest` and returns an `InvoiceResponse`. Creates (generates) a single invoice from the request (charges, period, subscriber/account, etc.) and returns the persisted invoice.

```java
// L17
    InvoiceResponse getInvoiceById(Long invoiceId);
```
**`getInvoiceById`** — single-invoice lookup by primary key `invoiceId`, returning an `InvoiceResponse`; missing records are expected to raise a "not found" exception in the implementation.

```java
// L19
    List<InvoiceResponse> getInvoicesByAccount(Long accountId);
```
**`getInvoicesByAccount`** (single-argument overload) — returns all invoices (`List<InvoiceResponse>`) for the given `accountId`, unfiltered.

```java
// L21-L22
    List<InvoiceResponse> getInvoicesByAccount(Long accountId, InvoiceStatus status,
                                               LocalDate fromDate, LocalDate toDate);
```
**`getInvoicesByAccount`** (four-argument **overload**) — returns invoices for an `accountId` filtered by `status` and by an issue/due date range (`fromDate` … `toDate`). The implementation may treat any of `status`/`fromDate`/`toDate` as optional (a `null` meaning "no constraint on that dimension"). Note this overload returns a plain `List`, not a `Page` — so unlike `BillingCycleService`, invoice filtering here is not server-side paginated at the contract level.

```java
// L24
    List<InvoiceResponse> getInvoicesByStatus(InvoiceStatus status);
```
**`getInvoicesByStatus`** — returns all invoices (`List<InvoiceResponse>`) currently in the given `InvoiceStatus` across accounts; useful for operational sweeps (e.g. list all overdue invoices).

```java
// L26
    InvoiceResponse processPayment(PaymentRequest request);
```
**`processPayment`** — takes a `PaymentRequest` and returns the affected `InvoiceResponse`. This records a payment where the target invoice is identified *inside* the request payload (no separate ID parameter). It returns the invoice (reflecting the new paid/partial state), not a payment object.

```java
// L28
    InvoiceResponse payInvoice(Long invoiceId, PaymentRequest request);
```
**`payInvoice`** — takes an explicit `invoiceId` plus a `PaymentRequest` and returns the updated `InvoiceResponse`. This is the same operation as `processPayment` but with the invoice identified by a path/URL parameter rather than inside the body.

*Aside: `processPayment` and `payInvoice` are two paths to "apply a payment to an invoice", differing only in how the invoice is identified. This is a likely intentional convenience (one body-driven, one ID-driven), but the apparent duplication is worth flagging — confirm in the implementation that both delegate to the same underlying logic and that they cannot diverge.*

```java
// L30
    InvoiceResponse applyLateFee(Long invoiceId, LateFeeRequest request);
```
**`applyLateFee`** — takes an `invoiceId` and a `LateFeeRequest` and returns the updated `InvoiceResponse` with the late fee added to its balance.

```java
// L32
    InvoiceResponse waiveLateFee(Long invoiceId, LateFeeWaiverRequest request);
```
**`waiveLateFee`** — takes an `invoiceId` and a `LateFeeWaiverRequest` (e.g. the reason/justification) and returns the updated `InvoiceResponse` with the previously-applied late fee removed/waived. The inverse of `applyLateFee`.

```java
// L34
    void markOverdueInvoices();
```
**`markOverdueInvoices`** — takes no parameters and returns `void`. A bulk maintenance operation that scans for invoices whose due date has passed and transitions them to overdue status. Returning `void` (rather than a result) signals this is a fire-and-forget batch job, likely invoked on a schedule or by an admin endpoint rather than per-invoice.

```java
// L36
    InvoiceResponse sendInvoice(Long invoiceId);
```
**`sendInvoice`** — takes an `invoiceId`, performs the "send/deliver" action (e.g. emailing or marking the invoice as sent), and returns the updated `InvoiceResponse`.

```java
// L38
    byte[] downloadInvoicePdf(Long invoiceId);
```
**`downloadInvoicePdf`** — takes an `invoiceId` and returns a `byte[]`: the rendered PDF of that invoice as raw bytes, which a controller streams to the client as a file download.

```java
// L40-L41
    byte[] downloadAccountStatementPdf(Long accountId);
}
```
**`downloadAccountStatementPdf`** — takes an `accountId` and returns a `byte[]`: a rendered PDF *account statement* (a consolidated document spanning the account's invoices) rather than a single invoice. The closing `}` ends the interface.

---

## src/main/java/com/teleconnect/billing_service/service/PaymentService.java

A small, focused contract for **payment records** as first-class entities — making a payment and looking payments up — complementing the invoice-centric payment methods in `InvoiceService`.

```java
// L1
package com.teleconnect.billing_service.service;
```
Package declaration; shared `...service` package.

```java
// L3-L4
import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.PaymentResponse;
```
Imports the inbound `PaymentRequest` (reused from `InvoiceService`) and the outbound `PaymentResponse`. The key distinction from `InvoiceService` is that this service returns `PaymentResponse` objects (a payment-centric view) rather than `InvoiceResponse`.

```java
// L6
import java.util.List;
```
Imports `List` for the multi-result query method.

```java
// L8
public interface PaymentService {
```
Declares the public `PaymentService` interface, implemented by `PaymentServiceImpl` and depended on by the payment controller.

```java
// L10
    PaymentResponse makePayment(PaymentRequest request);
```
**`makePayment`** — takes a `PaymentRequest` and returns a `PaymentResponse`. Records a payment and returns the created payment record (its ID, amount, status, etc.). This is the payment-record counterpart to `InvoiceService.processPayment`, which returns the *invoice* instead.

```java
// L12
    PaymentResponse getPaymentById(Long paymentId);
```
**`getPaymentById`** — single payment lookup by its primary key `paymentId`, returning a `PaymentResponse`; a missing record is expected to raise a "not found" exception in the implementation.

```java
// L14-L15
    List<PaymentResponse> getPaymentsByInvoice(Long invoiceId);
}
```
**`getPaymentsByInvoice`** — returns all payments (`List<PaymentResponse>`) recorded against a given invoice (`invoiceId`), reflecting that one invoice can have many payments (partials). The closing `}` ends the interface.

---

## src/main/java/com/teleconnect/billing_service/service/ReportService.java

Contract for **reporting/analytics** — read-only aggregated views (overdue receivables, collections, dispute summaries) used by reporting endpoints, returning purpose-built summary DTOs rather than raw entities.

```java
// L1
package com.teleconnect.billing_service.service;
```
Package declaration; shared `...service` package.

```java
// L3-L5
import com.teleconnect.billing_service.dto.response.CollectionReportResponse;
import com.teleconnect.billing_service.dto.response.DisputeSummaryResponse;
import com.teleconnect.billing_service.dto.response.OverdueReportResponse;
```
Imports the three report response DTOs. Each method returns one of these aggregate/summary types. Note there are **no request DTOs** imported — reports take their parameters as simple scalars (strings and dates) rather than as a wrapped request object.

```java
// L7
import java.time.LocalDate;
```
Imports `LocalDate` for the date-range parameters that bound the reporting periods.

```java
// L9
public interface ReportService {
```
Declares the public `ReportService` interface, implemented by `ReportServiceImpl` and depended on by the report controller. All methods here are read-only queries (no create/update/delete).

```java
// L11
    OverdueReportResponse getOverdueReport(String region, String agingBucket);
```
**`getOverdueReport`** — takes a `region` (`String`) and an `agingBucket` (`String`, e.g. "0-30", "31-60" days overdue) and returns an `OverdueReportResponse` summarizing outstanding/overdue amounts for that region and aging band. Both parameters are likely optional filters (the implementation may treat `null` as "all regions"/"all buckets").

```java
// L13
    CollectionReportResponse getCollectionReport(LocalDate fromDate, LocalDate toDate, String region);
```
**`getCollectionReport`** — takes a date range (`fromDate` … `toDate`) and an optional `region`, returning a `CollectionReportResponse` that summarizes payments collected within that window (and region). This is the "money received" counterpart to the overdue ("money owed") report.

```java
// L15-L16
    DisputeSummaryResponse getDisputeSummary(LocalDate fromDate, LocalDate toDate);
}
```
**`getDisputeSummary`** — takes only a date range (`fromDate` … `toDate`) and returns a `DisputeSummaryResponse` aggregating dispute activity over that period (e.g. counts by status, amounts disputed/resolved). The closing `}` ends the interface.

---

## How this connects

- **Above (Controllers):** Each REST controller declares a dependency on one of these interface types (`BillingCycleService`, `BillingDisputeService`, `InvoiceService`, `PaymentService`, or `ReportService`), typically via constructor injection. When an HTTP request arrives, the controller deserializes the body into a `*Request` DTO, calls the matching service method, and wraps the returned `*Response` DTO (or `byte[]` PDF, or `Page`) in an HTTP response. Because controllers depend on the *interface*, they are insulated from how the work is actually done.
- **The implementations (`*ServiceImpl`):** The concrete classes that `implements` these interfaces (e.g. `InvoiceServiceImpl`) contain all the real logic — transaction boundaries, validation, status-transition rules, mapping between entities and DTOs, PDF rendering, and so on. They are the classes referenced when this document says "the implementation is expected to…".
- **Below (Repositories → Entities → DB):** The implementations call Spring Data JPA repositories to load and save the `BillingCycle`, `Invoice`, `Payment`, and `BillingDispute` entities documented in files `03`–`06`, then convert those entities into the response DTOs (file `08`, response DTOs) returned through these interfaces. The `*Request` DTOs (file `07`) and enums (file `02`) define the inbound shapes and state vocabulary these contracts consume, and the custom exceptions (file `10`) are how the implementations signal the "not found" and rule-violation cases referenced above.
