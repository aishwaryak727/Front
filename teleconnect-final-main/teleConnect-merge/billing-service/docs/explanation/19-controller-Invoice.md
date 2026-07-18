# Controller: `InvoiceController`

`InvoiceController` is the REST entry point for everything to do with invoices in the billing microservice: generating invoices, querying them, marking them overdue, sending them, recording payments, applying and waiving late fees, and downloading PDF documents. It sits at the very top of the standard Spring layering — **Controller → Service → Repository → Entity/DB** — translating incoming HTTP requests into calls on the `InvoiceService` and wrapping the service's return values in `ResponseEntity` objects (HTTP status + body) that Spring serialises back to the client. The controller itself contains no business logic; it only maps URLs/HTTP verbs to service methods, binds request data (path variables, query parameters, JSON bodies), and shapes the HTTP response.

## src/main/java/com/teleconnect/billing_service/controller/InvoiceController.java

This file defines the `InvoiceController` class: a Spring MVC `@RestController` that exposes the `/billing/invoices` endpoint family and delegates all real work to `InvoiceService`.

```java
// L1
package com.teleconnect.billing_service.controller;
```

The `package` line declares the fully-qualified namespace this class lives in. In Java the package must mirror the directory layout (`com/teleconnect/billing_service/controller`), and it is what lets other classes refer to this one as `com.teleconnect.billing_service.controller.InvoiceController`. Placing it in a dedicated `controller` package is a convention that keeps the web/HTTP layer separated from `service`, `repository`, `entity`, `dto`, and `enums` packages.

```java
// L3-L10
import com.teleconnect.billing_service.dto.request.InvoiceGenerationRequest;
import com.teleconnect.billing_service.dto.request.LateFeeRequest;
import com.teleconnect.billing_service.dto.request.LateFeeWaiverRequest;
import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.InvoiceResponse;
import com.teleconnect.billing_service.dto.response.MessageResponse;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.service.InvoiceService;
```

These imports pull in the application's own types. A **DTO (Data Transfer Object)** is a plain object used to carry data across the HTTP boundary, separate from the JPA entities that map to database tables — this keeps the API contract decoupled from the persistence model.
- `InvoiceGenerationRequest`, `LateFeeRequest`, `LateFeeWaiverRequest`, `PaymentRequest` are **request DTOs**: their shapes define the JSON bodies clients must send to the relevant endpoints.
- `InvoiceResponse` and `MessageResponse` are **response DTOs**: `InvoiceResponse` is the JSON representation of an invoice returned to the client, and `MessageResponse` is a small wrapper (typically a single `message` string) used for simple acknowledgement responses.
- `InvoiceStatus` is an **enum** (an enumerated type with a fixed set of named constants, e.g. statuses like `PAID`, `OVERDUE`, etc.) used both as a query parameter and a path variable.
- `InvoiceService` is the service-layer interface/class that holds the actual business logic; the controller depends on it (see L27-L28).

```java
// L11
import jakarta.validation.Valid;
```

`jakarta.validation.Valid` is the Jakarta Bean Validation annotation `@Valid`. When placed on a `@RequestBody` parameter, it tells Spring to run the validation constraints declared inside that DTO (such as `@NotNull`, `@NotBlank`, `@Positive`, `@DecimalMin`, etc. on the DTO's fields) before the controller method body runs. If validation fails, Spring throws a `MethodArgumentNotValidException` and the request never reaches the service — typically surfaced as an HTTP 400 Bad Request.

```java
// L12
import org.springframework.beans.factory.annotation.Autowired;
```

`@Autowired` is Spring's dependency-injection annotation. It asks Spring's IoC (Inversion of Control) container to find a managed bean of the required type and inject it automatically, so the developer never calls `new` on it. Here it is used for field injection of `InvoiceService` (L27).

```java
// L13
import org.springframework.format.annotation.DateTimeFormat;
```

`@DateTimeFormat` controls how Spring parses incoming string values into date/time types. It is applied to the `fromDate`/`toDate` query parameters (L49-L50) to tell Spring the strings arrive in ISO date format (e.g. `2026-06-15`) and should be bound to `LocalDate`.

```java
// L14-L17
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
```

These are Spring's HTTP abstractions:
- `HttpHeaders` is a container for HTTP response headers (used when serving PDFs, L132/L145).
- `HttpStatus` is an enum of HTTP status codes (e.g. `CREATED` = 201, `OK` = 200), used on L38.
- `MediaType` represents content types like `application/pdf` (L133/L146).
- `ResponseEntity<T>` is a generic wrapper representing the **entire** HTTP response — status code, headers, and a body of type `T`. Every handler method here returns a `ResponseEntity`, giving precise control over what the client receives.

```java
// L18
import org.springframework.web.bind.annotation.*;
```

A wildcard import of Spring Web's binding annotations. This is what brings in `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PathVariable`, `@RequestParam`, and `@RequestBody` — all used throughout this file.

```java
// L20-L21
import java.time.LocalDate;
import java.util.List;
```

Standard JDK imports. `LocalDate` is a date without time-of-day or timezone (used for the date-range filter parameters). `List` is the interface for ordered collections, used as the return type for endpoints that return multiple invoices.

```java
// L23-L25
@RestController
@RequestMapping("/billing/invoices")
public class InvoiceController {
```

`@RestController` is a Spring stereotype annotation that combines `@Controller` (marks the class as a web request handler bean) with `@ResponseBody` (tells Spring to serialise each method's return value directly into the HTTP response body — typically as JSON via Jackson — rather than treating it as a view name). So every method here returns data, not an HTML page.

`@RequestMapping("/billing/invoices")` declares the **base path** for the whole controller. Every method-level mapping below is appended to this prefix; e.g. `@PostMapping("/generate")` resolves to `POST /billing/invoices/generate`. *(Note: the Javadoc comments on the methods reference a `/teleConnect/billing/invoices/...` prefix. The extra `/teleConnect` segment is not present in this class's mappings — it would have to come from a global servlet context-path or server config defined elsewhere, e.g. `server.servlet.context-path` in `application.properties`. If no such prefix is configured, the real paths are `/billing/invoices/...`. The comments may be stale or assume an external gateway prefix.)*

```java
// L27-L28
    @Autowired
    private InvoiceService invoiceService;
```

This is the controller's single dependency: the service that performs all business logic. `@Autowired` on the field tells Spring to inject the `InvoiceService` bean at startup. This is **field injection**. *(Aside: field injection is convenient but generally discouraged in favour of constructor injection, which makes the dependency final, easier to unit-test, and impossible to leave uninitialised. This is a style observation, not a bug.)* Every handler below calls a method on this `invoiceService` instance — that is the controller's hand-off into the service layer.

```java
// L30
    // ── Static-path endpoints first (must come before /{invoiceId}) ────────────
```

A developer comment documenting an important ordering concern. Spring matches the most specific path, but to avoid ambiguity the author groups literal/static segments (like `/generate`, `/status/{status}`) ahead of the catch-all dynamic `/{invoiceId}` mapping. This is a readability/maintenance convention; with Spring's path-matching it is mostly defensive, but it prevents a literal like `generate` from accidentally being interpreted as an `invoiceId`.

```java
// L32-L40
    /**
     * POST /teleConnect/billing/invoices/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<InvoiceResponse> generateInvoice(
            @Valid @RequestBody InvoiceGenerationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.generateInvoice(request));
    }
```

The `generateInvoice` handler. `@PostMapping("/generate")` maps HTTP `POST /billing/invoices/generate` to this method (POST is used because it creates a new resource). The parameter `@RequestBody InvoiceGenerationRequest request` tells Spring to deserialise the incoming JSON body into an `InvoiceGenerationRequest` object; `@Valid` triggers bean-validation on that object first (a validation failure stops execution before the body runs). 

Logic: the method calls `invoiceService.generateInvoice(request)`, which performs the actual invoice creation in the service layer and returns an `InvoiceResponse`. That response is wrapped via `ResponseEntity.status(HttpStatus.CREATED).body(...)`, producing an HTTP **201 Created** with the new invoice as the JSON body. The return type is `ResponseEntity<InvoiceResponse>`. No branching or loops; any business exceptions (e.g. account not found) are thrown by the service and handled by a global exception handler elsewhere.

```java
// L42-L53
    /**
     * GET /teleConnect/billing/invoices/account/{accountId}
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        List<InvoiceResponse> data = invoiceService.getInvoicesByAccount(accountId, status, fromDate, toDate);
        return ResponseEntity.ok(data);
    }
```

The `getInvoicesByAccount` handler — a filtered listing of invoices for one account. `@GetMapping("/account/{accountId}")` maps `GET /billing/invoices/account/{accountId}`.

Parameters:
- `@PathVariable Long accountId` binds the `{accountId}` segment of the URL to the method argument. `@PathVariable` extracts a value embedded in the path; Spring converts the string to `Long`.
- `@RequestParam(required = false) InvoiceStatus status` binds an optional `?status=...` query parameter. `@RequestParam` reads from the query string; `required = false` means the request is valid even if the parameter is absent (then `status` is `null`). Spring converts the string to the `InvoiceStatus` enum constant.
- `@RequestParam(required = false) @DateTimeFormat(...ISO.DATE) LocalDate fromDate` and the matching `toDate` are optional date-range filters. `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` instructs Spring to parse ISO-8601 calendar dates (e.g. `2026-01-31`) into `LocalDate`.

Logic: it forwards all four (possibly null) arguments to `invoiceService.getInvoicesByAccount(...)`, stores the resulting `List<InvoiceResponse>` in `data`, and returns it with `ResponseEntity.ok(data)` — HTTP **200 OK** with the list as the JSON array body. The service is responsible for interpreting the null filters (e.g. ignoring a filter when its argument is null).

```java
// L55-L61
    /**
     * GET /teleConnect/billing/invoices/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByStatus(@PathVariable InvoiceStatus status) {
        return ResponseEntity.ok(invoiceService.getInvoicesByStatus(status));
    }
```

The `getInvoicesByStatus` handler. `@GetMapping("/status/{status}")` maps `GET /billing/invoices/status/{status}`. Here `@PathVariable InvoiceStatus status` takes the `{status}` path segment and converts it to the `InvoiceStatus` enum (an invalid value that doesn't match an enum constant would cause a conversion failure → typically HTTP 400). It calls `invoiceService.getInvoicesByStatus(status)` and returns the resulting list with HTTP **200 OK**. Return type `ResponseEntity<List<InvoiceResponse>>`.

```java
// L63-L70
    /**
     * PUT /teleConnect/billing/invoices/mark-overdue
     */
    @PutMapping("/mark-overdue")
    public ResponseEntity<MessageResponse> markOverdue() {
        invoiceService.markOverdueInvoices();
        return ResponseEntity.ok(new MessageResponse("Overdue invoices updated successfully"));
    }
```

The `markOverdue` handler — a maintenance/batch operation that flips eligible invoices to an overdue status. `@PutMapping("/mark-overdue")` maps `PUT /billing/invoices/mark-overdue` (PUT signals a state-changing update). It takes no parameters and no body. It calls the void service method `invoiceService.markOverdueInvoices()` (which scans for invoices past their due date and updates them) and then returns HTTP **200 OK** with a new `MessageResponse("Overdue invoices updated successfully")` — a simple JSON acknowledgement. There is no branching; the response message is fixed regardless of how many invoices were updated.

```java
// L72
    // ── Dynamic /{invoiceId} endpoints below ───────────────────────────────────
```

A comment marking the boundary: everything below is keyed by a specific invoice id in the path. Pairs with the L30 comment to document the static-before-dynamic ordering convention.

```java
// L74-L80
    /**
     * GET /teleConnect/billing/invoices/{invoiceId}
     */
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(invoiceId));
    }
```

The `getInvoice` handler — fetch a single invoice by id. `@GetMapping("/{invoiceId}")` maps `GET /billing/invoices/{invoiceId}`, with `@PathVariable Long invoiceId` binding the path segment to a `Long`. It returns `invoiceService.getInvoiceById(invoiceId)` wrapped in HTTP **200 OK**. If no invoice exists for that id, the service is expected to throw a not-found exception (mapped to HTTP 404 by a global handler) rather than the controller returning an empty body.

```java
// L82-L88
    /**
     * PUT /teleConnect/billing/invoices/{invoiceId}/send
     */
    @PutMapping("/{invoiceId}/send")
    public ResponseEntity<InvoiceResponse> sendInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.sendInvoice(invoiceId));
    }
```

The `sendInvoice` handler — marks/dispatches an invoice as sent (e.g. transitions its status and possibly triggers delivery). `@PutMapping("/{invoiceId}/send")` maps `PUT /billing/invoices/{invoiceId}/send`. It binds the id via `@PathVariable Long invoiceId`, calls `invoiceService.sendInvoice(invoiceId)`, and returns the updated `InvoiceResponse` with HTTP **200 OK**.

```java
// L90-L100
    /**
     * POST /teleConnect/billing/invoices/{invoiceId}/pay
     * Body: { "amountPaid": 949.32, "paymentMethod": "UPI", "transactionRef": "TXN98765" }
     */
    @PostMapping("/{invoiceId}/pay")
    public ResponseEntity<MessageResponse> payInvoice(
            @PathVariable Long invoiceId,
            @Valid @RequestBody PaymentRequest request) {
        invoiceService.payInvoice(invoiceId, request);
        return ResponseEntity.ok(new MessageResponse("Payment recorded successfully"));
    }
```

The `payInvoice` handler — records a payment against an invoice. `@PostMapping("/{invoiceId}/pay")` maps `POST /billing/invoices/{invoiceId}/pay`. The Javadoc shows the expected JSON body shape (`amountPaid`, `paymentMethod`, `transactionRef`). Parameters: `@PathVariable Long invoiceId` for the target invoice, and `@Valid @RequestBody PaymentRequest request` to deserialise + validate the payment payload. It calls the void method `invoiceService.payInvoice(invoiceId, request)` (where the service applies payment business rules — partial vs full payment, status updates, persistence) and returns HTTP **200 OK** with `MessageResponse("Payment recorded successfully")`.

```java
// L102-L112
    /**
     * POST /teleConnect/billing/invoices/{invoiceId}/latefee
     * Body: { "feeAmount": 100.00, "reason": "Overdue past grace period" }
     */
    @PostMapping("/{invoiceId}/latefee")
    public ResponseEntity<MessageResponse> applyLateFee(
            @PathVariable Long invoiceId,
            @Valid @RequestBody LateFeeRequest request) {
        invoiceService.applyLateFee(invoiceId, request);
        return ResponseEntity.ok(new MessageResponse("Late fee applied successfully"));
    }
```

The `applyLateFee` handler — adds a late fee to an invoice. `@PostMapping("/{invoiceId}/latefee")` maps `POST /billing/invoices/{invoiceId}/latefee`. The expected body (per Javadoc) carries `feeAmount` and `reason`. It binds `@PathVariable Long invoiceId` and `@Valid @RequestBody LateFeeRequest request`, delegates to the void `invoiceService.applyLateFee(invoiceId, request)`, and returns HTTP **200 OK** with `MessageResponse("Late fee applied successfully")`.

```java
// L114-L124
    /**
     * POST /teleConnect/billing/invoices/{invoiceId}/latefee/waive
     * Body: { "waiverReason": "Goodwill gesture", "authorisedBy": "user-501" }
     */
    @PostMapping("/{invoiceId}/latefee/waive")
    public ResponseEntity<MessageResponse> waiveLateFee(
            @PathVariable Long invoiceId,
            @Valid @RequestBody LateFeeWaiverRequest request) {
        invoiceService.waiveLateFee(invoiceId, request);
        return ResponseEntity.ok(new MessageResponse("Late fee waived successfully"));
    }
```

The `waiveLateFee` handler — reverses/waives a previously applied late fee. `@PostMapping("/{invoiceId}/latefee/waive")` maps `POST /billing/invoices/{invoiceId}/latefee/waive`. The expected body (per Javadoc) carries `waiverReason` and `authorisedBy` (capturing who approved the waiver, for audit). It binds `@PathVariable Long invoiceId` and `@Valid @RequestBody LateFeeWaiverRequest request`, calls the void `invoiceService.waiveLateFee(invoiceId, request)`, and returns HTTP **200 OK** with `MessageResponse("Late fee waived successfully")`.

```java
// L126-L136
    /**
     * GET /teleConnect/billing/invoices/{invoiceId}/download
     */
    @GetMapping("/{invoiceId}/download")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long invoiceId) {
        byte[] pdfBytes = invoiceService.downloadInvoicePdf(invoiceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Invoice_" + invoiceId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
```

The `downloadInvoice` handler — streams a generated PDF of one invoice. `@GetMapping("/{invoiceId}/download")` maps `GET /billing/invoices/{invoiceId}/download`. Note the return type is `ResponseEntity<byte[]>` (raw binary), not a DTO — because the response is a file, not JSON.

Step by step:
1. `invoiceService.downloadInvoicePdf(invoiceId)` returns the PDF document as a `byte[]`.
2. A new `HttpHeaders` object is created.
3. `headers.setContentType(MediaType.APPLICATION_PDF)` sets the `Content-Type: application/pdf` header so the client treats the bytes as a PDF.
4. `headers.setContentDispositionFormData("attachment", "Invoice_" + invoiceId + ".pdf")` sets the `Content-Disposition` header to `attachment` with a filename like `Invoice_42.pdf`, prompting the browser to download (rather than inline-display) the file. The id is concatenated into the filename so each download is uniquely named.
5. `ResponseEntity.ok().headers(headers).body(pdfBytes)` returns HTTP **200 OK** with those headers and the PDF bytes as the body.

```java
// L138-L149
    /**
     * GET /teleConnect/billing/invoices/account/{accountId}/statement
     * Downloads a full account statement PDF with all billing cycles and charges.
     */
    @GetMapping("/account/{accountId}/statement")
    public ResponseEntity<byte[]> downloadAccountStatement(@PathVariable Long accountId) {
        byte[] pdfBytes = invoiceService.downloadAccountStatementPdf(accountId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Statement_Account_" + accountId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
```

The `downloadAccountStatement` handler — like `downloadInvoice`, but for a whole account's statement (all billing cycles and charges). `@GetMapping("/account/{accountId}/statement")` maps `GET /billing/invoices/account/{accountId}/statement`, binding `@PathVariable Long accountId`. It calls `invoiceService.downloadAccountStatementPdf(accountId)` to get the statement as a `byte[]`, builds `HttpHeaders` with `application/pdf` content type, sets a `Content-Disposition` attachment filename like `Statement_Account_7.pdf`, and returns HTTP **200 OK** with the PDF bytes. The logic mirrors `downloadInvoice` exactly, only the service method and filename prefix differ.

```java
// L150
}
```

Closing brace of the `InvoiceController` class.

## How this connects

- **Upstream (clients):** HTTP clients (browsers, gateways, other services) hit the `/billing/invoices/...` URLs. Spring MVC routes each request to the matching handler method based on its `@GetMapping`/`@PostMapping`/`@PutMapping` and binds path variables, query params, and JSON bodies into the method arguments, validating `@RequestBody` DTOs via `@Valid`.
- **The DTO layer:** Request DTOs (`InvoiceGenerationRequest`, `PaymentRequest`, `LateFeeRequest`, `LateFeeWaiverRequest`) define the inbound JSON contracts; response DTOs (`InvoiceResponse`, `MessageResponse`) define the outbound JSON. The controller never exposes JPA entities directly — it speaks only in DTOs.
- **Downstream (service):** Every handler delegates to the injected `InvoiceService` (see `src/main/java/com/teleconnect/billing_service/service/InvoiceService.java`). The service holds the business rules (invoice generation, payment processing, late-fee logic, overdue detection, PDF generation) and in turn calls repositories that read/write the JPA entities to the database — completing the Controller → Service → Repository → Entity/DB chain.
- **Error handling:** This controller throws no exceptions itself; validation failures and any business exceptions raised by the service (not found, illegal state, etc.) are expected to be translated into HTTP error responses by a centralised exception handler (e.g. a `@RestControllerAdvice` class elsewhere in the project).
