# Controller: BillingCycleController

`BillingCycleController` is the REST entry point for everything related to **billing cycles** in the TeleConnect billing microservice. In the standard layering of this service — **Controller → Service → Repository → Entity/DB** — this class is the topmost (Controller) layer: it receives HTTP requests, performs input binding/validation, delegates all real work to `BillingCycleService`, and wraps the service's results in HTTP responses (`ResponseEntity`). It never touches the database or business logic directly; it only translates between the HTTP world (URLs, JSON bodies, query params, status codes) and the service world (Java method calls and DTOs).

## src/main/java/com/teleconnect/billing_service/controller/BillingCycleController.java

This file defines the controller that exposes CRUD-style and lifecycle endpoints for billing cycles under the base path `/billing/cycles`, delegating to `BillingCycleService`.

```java
// L1
package com.teleconnect.billing_service.controller;
```

The `package` line declares the namespace this class belongs to. It places `BillingCycleController` in `com.teleconnect.billing_service.controller`, which both organizes the source tree and lets other classes import it by fully qualified name. Being in the `controller` sub-package is also a convention signaling "this is a web/REST layer class."

```java
// L3-L8
import com.teleconnect.billing_service.dto.request.BillingCycleRequest;
import com.teleconnect.billing_service.dto.request.CycleGenerationRequest;
import com.teleconnect.billing_service.dto.response.BillingCycleResponse;
import com.teleconnect.billing_service.dto.response.MessageResponse;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import com.teleconnect.billing_service.service.BillingCycleService;
```

These imports bring in the project's own types used by this controller:
- `BillingCycleRequest` — a **DTO** (Data Transfer Object: a plain data-carrying class used to shape the JSON request/response, decoupling the HTTP contract from the database entity) representing the JSON body for creating a billing cycle.
- `CycleGenerationRequest` — the DTO for the batch invoice-generation request body.
- `BillingCycleResponse` — the DTO returned to clients describing a billing cycle (the outbound shape).
- `MessageResponse` — a small DTO used to return a simple `{"message": "..."}`-style payload for operations that don't return a domain object.
- `BillingCycleStatus` — an **enum** (a fixed set of named constants) describing the possible states of a billing cycle (e.g., open/closed); used both as a query parameter and as part of update operations.
- `BillingCycleService` — the **service interface/class** this controller delegates to; it holds the actual business logic and talks to the repository layer.

```java
// L9
import jakarta.validation.Valid;
```

Imports `@Valid` from **Jakarta Bean Validation**. When applied to a method parameter, `@Valid` tells Spring to run the validation constraints declared on that object's fields (such as `@NotNull`, `@NotBlank`, `@Min`) before the controller method body executes. If validation fails, Spring throws a `MethodArgumentNotValidException` and the request is rejected (typically as HTTP 400) before any business logic runs.

```java
// L10
import java.util.List;
```

Imports the standard `java.util.List` interface, used as the return type for the paginated "cycles by account" endpoint (the controller returns just the page's content as a `List`).

```java
// L11
import org.springframework.beans.factory.annotation.Autowired;
```

Imports Spring's `@Autowired` annotation, which marks a field/constructor/setter for **dependency injection** — i.e., Spring's container automatically supplies a managed bean instance instead of the code calling `new`.

```java
// L12-L14
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
```

These three come from **Spring Data** and support pagination:
- `Pageable` — an interface describing a page request (which page number, how many items per page, and optional sorting).
- `PageRequest` — a concrete implementation of `Pageable`; `PageRequest.of(page, size)` builds a pageable for a given zero-based page index and page size.
- `Page<T>` — the result wrapper returned by paginated queries; it holds the items for the current page plus metadata (total elements, total pages, etc.). Here only its `.getContent()` (the items list) is used.

```java
// L15-L16
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
```

- `HttpStatus` — an enum of HTTP status codes (e.g., `CREATED` = 201, `OK` = 200), used to set the response status explicitly.
- `ResponseEntity<T>` — a Spring type representing the **full HTTP response**: status code, headers, and a typed body of type `T`. Returning a `ResponseEntity` lets the controller control the status code and body together, rather than relying on defaults.

```java
// L17
import org.springframework.web.bind.annotation.*;
```

A wildcard import pulling in the whole Spring Web MVC annotation package: `@RestController`, `@RequestMapping`, `@PostMapping`, `@GetMapping`, `@PutMapping`, `@RequestBody`, `@PathVariable`, `@RequestParam`, etc. These are the annotations that map HTTP requests to methods and bind request data to parameters.

```java
// L19-L21
@RestController
@RequestMapping("/billing/cycles")
public class BillingCycleController {
```

- `@RestController` — a Spring stereotype that combines `@Controller` (marks the class as a web request handler and a Spring-managed bean) with `@ResponseBody` (means every handler method's return value is serialized directly into the HTTP response body, typically as JSON, rather than being interpreted as a view/template name). So every method here returns data, not a web page.
- `@RequestMapping("/billing/cycles")` — sets the **base URL path** for every endpoint in this class. All routes below are relative to `/billing/cycles`. (The Javadoc comments mention a `/teleConnect` prefix; that prefix is not present here, so it presumably comes from a global servlet context-path or server configuration elsewhere — *note: it is not defined in this file*.)
- `public class BillingCycleController` — the class declaration itself.

```java
// L23-L24
    @Autowired
    private BillingCycleService billingCycleService;
```

This declares the controller's single dependency: the `BillingCycleService`. `@Autowired` on the field is **field injection** — at startup, Spring finds the `BillingCycleService` bean and assigns it here automatically, so the controller can call business methods on it. Every endpoint below delegates to this one field. (*Aside: field injection works but constructor injection is generally preferred because it makes the dependency final, easier to test, and impossible to leave unset; this is a stylistic, not functional, concern.*)

```java
// L26-L29
    /**
     * POST /teleConnect/billing/cycles
     * Creates a new billing cycle for an account.
     */
```

A Javadoc comment documenting the next method: it handles `POST` to the base path and creates a new billing cycle for an account.

```java
// L30-L35
    @PostMapping
    public ResponseEntity<BillingCycleResponse> createBillingCycle(
            @Valid @RequestBody BillingCycleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingCycleService.createBillingCycle(request));
    }
```

This is the **create** endpoint.
- `@PostMapping` (with no path) maps HTTP `POST /billing/cycles` to this method. `POST` is the conventional verb for creating a resource.
- Return type `ResponseEntity<BillingCycleResponse>` — the method returns a full HTTP response whose body is a `BillingCycleResponse` DTO.
- Parameter `@Valid @RequestBody BillingCycleRequest request`:
  - `@RequestBody` tells Spring to deserialize the incoming JSON request body into a `BillingCycleRequest` object.
  - `@Valid` triggers Bean Validation on that object before the method body runs; if the request fails its declared constraints, the request is rejected with a validation error (HTTP 400) and the body never executes.
- Body logic: it calls `billingCycleService.createBillingCycle(request)`, which performs the actual creation (validation of business rules, persistence via the repository, mapping to a response DTO) and returns a `BillingCycleResponse`. That response is wrapped via `ResponseEntity.status(HttpStatus.CREATED).body(...)`, so the client receives **HTTP 201 Created** with the new cycle's representation as the body. No explicit exceptions are thrown here; any thrown by the service propagate to Spring's global exception handling.

```java
// L37-L41
    /**
     * POST /teleConnect/billing/cycles/generate
     * Triggers batch invoice generation for all eligible open cycles.
     * NOTE: Must be declared before /{cycleId} to avoid path conflict.
     */
```

Javadoc for the next method. It documents that this endpoint triggers **batch invoice generation** and includes an important routing note: this method must be declared *before* the `/{cycleId}` path-variable routes so that the literal segment `generate` is not mistakenly matched as a dynamic `{cycleId}` value. (In practice Spring prefers more specific/literal mappings over path variables, but the author is being explicit about ordering intent.)

```java
// L42-L47
    @PostMapping("/generate")
    public ResponseEntity<MessageResponse> generateInvoices(
            @Valid @RequestBody CycleGenerationRequest request) {
        billingCycleService.generateInvoicesBatch(request);
        return ResponseEntity.ok(new MessageResponse("Invoice generation completed successfully"));
    }
```

This is the **batch invoice generation** endpoint.
- `@PostMapping("/generate")` maps `POST /billing/cycles/generate` here.
- Return type `ResponseEntity<MessageResponse>` — returns a simple message payload, since there is no single domain object to return for a batch operation.
- Parameter `@Valid @RequestBody CycleGenerationRequest request` — same pattern as before: the JSON body is bound to `CycleGenerationRequest` and validated before execution.
- Body logic:
  1. `billingCycleService.generateInvoicesBatch(request)` performs the actual batch work (presumably iterating eligible open cycles and generating invoices). Its return value, if any, is discarded — this endpoint cares only about completion, not a payload.
  2. It then returns `ResponseEntity.ok(...)` — **HTTP 200 OK** — with body `new MessageResponse("Invoice generation completed successfully")`. `ResponseEntity.ok(...)` is a shorthand for status 200 with the given body.
- If `generateInvoicesBatch` throws (e.g., a business error), the success message is never returned and the exception propagates to global error handling.

```java
// L49-L53
    /**
     * GET /teleConnect/billing/cycles/account/{accountId}
     * Returns a paginated list of billing cycles for a subscriber account.
     * NOTE: Must be declared before /{cycleId} to avoid "account" being parsed as Long.
     */
```

Javadoc for the "list cycles by account" endpoint, with a routing note: declaring `/account/{accountId}` before `/{cycleId}` prevents the literal word `account` from being interpreted as a numeric `{cycleId}` (which would fail to parse as a `Long`).

```java
// L54-L59
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<BillingCycleResponse>> getCyclesByAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) BillingCycleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
```

This is the **list-by-account (paginated)** endpoint's signature.
- `@GetMapping("/account/{accountId}")` maps `GET /billing/cycles/account/{accountId}`. The `{accountId}` segment is a **path variable** — a dynamic part of the URL.
- Return type `ResponseEntity<List<BillingCycleResponse>>` — returns a list of cycle DTOs (the current page's items only).
- Parameters:
  - `@PathVariable Long accountId` — `@PathVariable` binds the `{accountId}` URL segment to this parameter and converts it to a `Long`. This is the subscriber account whose cycles are requested.
  - `@RequestParam(required = false) BillingCycleStatus status` — `@RequestParam` binds an HTTP query-string parameter (`?status=...`). `required = false` makes it optional; if absent, `status` is `null`. Spring converts the string value into the `BillingCycleStatus` enum, so callers can filter cycles by status.
  - `@RequestParam(defaultValue = "0") int page` — an optional query param for the zero-based page index; defaults to `0` (first page) if not supplied.
  - `@RequestParam(defaultValue = "5") int size` — an optional query param for page size; defaults to `5` items per page.

```java
// L60-L63
        Pageable pageable = PageRequest.of(page, size);
        Page<BillingCycleResponse> data = billingCycleService.getCyclesByAccount(accountId, status, pageable);
        return ResponseEntity.ok(data.getContent());
    }
```

The body of `getCyclesByAccount`:
1. `Pageable pageable = PageRequest.of(page, size);` builds a pagination request from the supplied `page` and `size` parameters. (*Aside: there is no sorting specified, so results come back in the repository/database's default order.*)
2. `Page<BillingCycleResponse> data = billingCycleService.getCyclesByAccount(accountId, status, pageable);` delegates to the service, which queries the repository for cycles belonging to `accountId`, optionally filtered by `status`, paged according to `pageable`, and maps each result into a `BillingCycleResponse`. The result is a full `Page` (items + paging metadata).
3. `return ResponseEntity.ok(data.getContent());` returns **HTTP 200 OK** with only the page's **content** (the `List<BillingCycleResponse>`). *Note: pagination metadata such as total element count and total page count is computed but not returned to the client — only the raw list of items for the requested page is sent. If clients need to know the total number of pages/elements, this endpoint does not currently expose it.*

```java
// L65-L69
    /**
     * PUT /teleConnect/billing/cycles/{cycleId}/close
     * Manually closes a billing cycle. Irreversible.
     * NOTE: Must be declared before /{cycleId} GET to avoid conflict.
     */
```

Javadoc for the "close cycle" endpoint. It documents that this is a `PUT` operation that manually and irreversibly closes a billing cycle, and again notes ordering relative to the bare `/{cycleId}` route.

```java
// L70-L74
    @PutMapping("/{cycleId}/close")
    public ResponseEntity<MessageResponse> closeCycle(@PathVariable Long cycleId) {
        billingCycleService.closeBillingCycle(cycleId);
        return ResponseEntity.ok(new MessageResponse("Billing cycle closed successfully"));
    }
```

This is the **close cycle** endpoint.
- `@PutMapping("/{cycleId}/close")` maps `PUT /billing/cycles/{cycleId}/close`. `PUT` is used here for a state-changing update on an existing resource.
- Return type `ResponseEntity<MessageResponse>` — returns a simple confirmation message rather than the cycle object.
- Parameter `@PathVariable Long cycleId` — binds and converts the `{cycleId}` URL segment to a `Long`, identifying which cycle to close.
- Body logic:
  1. `billingCycleService.closeBillingCycle(cycleId)` performs the actual close (presumably loading the cycle, enforcing business rules such as "must be open," updating its status, and persisting). Its return value (if any) is ignored.
  2. Returns `ResponseEntity.ok(new MessageResponse("Billing cycle closed successfully"))` — **HTTP 200 OK** with a success message.
- If the cycle does not exist or cannot be closed, the service is expected to throw, and that exception propagates to global error handling (no try/catch here).

```java
// L76-L79
    /**
     * PUT /teleConnect/billing/cycles/{cycleId}/status
     * Updates the status of a billing cycle.
     */
```

Javadoc for the "update status" endpoint: a `PUT` that changes a cycle's status to a caller-supplied value.

```java
// L80-L85
    @PutMapping("/{cycleId}/status")
    public ResponseEntity<BillingCycleResponse> updateStatus(
            @PathVariable Long cycleId,
            @RequestParam BillingCycleStatus status) {
        return ResponseEntity.ok(billingCycleService.updateCycleStatus(cycleId, status));
    }
```

This is the **update status** endpoint.
- `@PutMapping("/{cycleId}/status")` maps `PUT /billing/cycles/{cycleId}/status`.
- Return type `ResponseEntity<BillingCycleResponse>` — returns the updated cycle as a DTO so the client sees its new state.
- Parameters:
  - `@PathVariable Long cycleId` — which cycle to update (from the URL).
  - `@RequestParam BillingCycleStatus status` — the **required** new status, supplied as a query parameter (e.g., `?status=CLOSED`). Because no `required = false` is given, the parameter is mandatory; omitting it results in an HTTP 400. Spring converts the string to the `BillingCycleStatus` enum.
- Body logic: it calls `billingCycleService.updateCycleStatus(cycleId, status)`, which applies the new status (after any business-rule checks) and returns the updated `BillingCycleResponse`. That response is returned with **HTTP 200 OK** via `ResponseEntity.ok(...)`.
- *Aside: unlike `closeCycle`, which uses a dedicated method, this is a generic status setter; depending on the service implementation, it may or may not allow transitioning a cycle into any status, including closing it. The two paths could overlap in effect.*

```java
// L87-L91
    /**
     * GET /teleConnect/billing/cycles/{cycleId}
     * Returns full details of one billing cycle by its ID.
     * NOTE: Declared last — only matches numeric IDs.
     */
```

Javadoc for the "get one cycle by ID" endpoint. The note explains it is declared **last** so that the more specific literal paths above (`/generate`, `/account/...`, `/{cycleId}/close`, `/{cycleId}/status`) are matched first, and this catch-all path-variable route only handles a plain numeric ID.

```java
// L92-L96
    @GetMapping("/{cycleId}")
    public ResponseEntity<BillingCycleResponse> getBillingCycle(@PathVariable Long cycleId) {
        return ResponseEntity.ok(billingCycleService.getBillingCycleById(cycleId));
    }
}
```

This is the **get-by-ID** endpoint and the end of the class.
- `@GetMapping("/{cycleId}")` maps `GET /billing/cycles/{cycleId}`.
- Return type `ResponseEntity<BillingCycleResponse>` — returns one cycle's full details as a DTO.
- Parameter `@PathVariable Long cycleId` — the numeric cycle identifier from the URL, converted to `Long`.
- Body logic: it calls `billingCycleService.getBillingCycleById(cycleId)`, which fetches the cycle (typically throwing a "not found" exception if no such cycle exists) and maps it to a `BillingCycleResponse`. The result is returned with **HTTP 200 OK** via `ResponseEntity.ok(...)`.
- The final `}` closes the class.

## How this connects

`BillingCycleController` is the **Controller** layer. Every endpoint here delegates to the injected `BillingCycleService` (the **Service** layer), which is where business rules, transactions, and entity-to-DTO mapping live. The service in turn uses a Spring Data **Repository** to read/write the billing-cycle **Entity** mapped to the database. The DTOs (`BillingCycleRequest`, `CycleGenerationRequest`, `BillingCycleResponse`, `MessageResponse`) define the HTTP contract so that the internal entity is never exposed directly, and `BillingCycleStatus` is the shared enum that flows from query parameters down through the service into the entity. Validation (`@Valid` + Bean Validation constraints on the request DTOs) and HTTP-shaping (`ResponseEntity`, `HttpStatus`) are handled here at the boundary, keeping the lower layers focused on logic and persistence.
