# Controller: `BillingDisputeController`

This file is the REST entry point for everything related to **billing disputes** (a customer raising a complaint that an invoice/charge is wrong, having it reviewed, and having it resolved). It is the top of the standard Spring layering used throughout this service: an HTTP request hits a method here (**Controller**), which delegates all real work to `BillingDisputeService` (**Service**), which in turn talks to JPA repositories (**Repository**) that map to entities/tables (**Entity/DB**). The controller itself contains no business logic — it only maps URLs/HTTP verbs to service calls and wraps the results in HTTP responses.

## src/main/java/com/teleconnect/billing_service/controller/BillingDisputeController.java

Defines the HTTP API for raising, querying, reviewing, resolving, and status-updating billing disputes, delegating to `BillingDisputeService`.

```java
// L1
package com.teleconnect.billing_service.controller;
```

The `package` statement declares the fully-qualified namespace this class lives in. It places the class in the `controller` sub-package of the application, which is the conventional home for web-layer (REST) classes. The package path must mirror the folder structure on disk (`.../com/teleconnect/billing_service/controller/`).

```java
// L3-L9
import com.teleconnect.billing_service.dto.request.DisputeRequest;
import com.teleconnect.billing_service.dto.request.DisputeResolveRequest;
import com.teleconnect.billing_service.dto.request.DisputeReviewRequest;
import com.teleconnect.billing_service.dto.response.DisputeResponse;
import com.teleconnect.billing_service.dto.response.MessageResponse;
import com.teleconnect.billing_service.enums.DisputeStatus;
import com.teleconnect.billing_service.service.BillingDisputeService;
```

These imports bring in the application's own types used by this controller:
- **`DisputeRequest`, `DisputeResolveRequest`, `DisputeReviewRequest`** — *DTOs* (Data Transfer Objects). A DTO is a plain class used to carry data across a boundary (here, the JSON request body that the client sends). Each one defines the expected shape of the incoming JSON for a specific operation (raise / resolve / review).
- **`DisputeResponse`** — the DTO returned to the client describing a dispute (the read/output shape).
- **`MessageResponse`** — a small DTO that wraps a human-readable status string (e.g. `{"message": "..."}`), used for actions that don't return a full dispute object.
- **`DisputeStatus`** — an `enum` (a fixed set of named constants) representing the lifecycle states of a dispute (e.g. raised, under review, resolved). Using an enum means Spring will only accept those specific string values.
- **`BillingDisputeService`** — the service interface/class this controller delegates to. This is the dependency that does the actual work.

```java
// L10
import jakarta.validation.Valid;
```

Imports `@Valid` from **Jakarta Bean Validation**. When placed on a method parameter, `@Valid` tells Spring to run the validation constraints (such as `@NotNull`, `@Positive`, etc.) declared inside the target DTO class before the controller method runs. If validation fails, Spring rejects the request (typically with HTTP 400) before any of this code executes.

```java
// L11-L14
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
```

Spring framework imports:
- **`@Autowired`** — marks a field/constructor for *dependency injection*: Spring automatically supplies the required bean (object instance) at startup rather than the class creating it itself.
- **`HttpStatus`** — an enum of HTTP status codes (e.g. `CREATED` = 201, `OK` = 200).
- **`ResponseEntity`** — a generic wrapper representing the full HTTP response: status code, headers, and body. `ResponseEntity<T>` means the body is of type `T`.
- **`org.springframework.web.bind.annotation.*`** — a wildcard import that pulls in all the web MVC annotations used below: `@RestController`, `@RequestMapping`, `@PostMapping`, `@GetMapping`, `@PutMapping`, `@PathVariable`, `@RequestParam`, `@RequestBody`.

```java
// L16
import java.util.List;
```

Imports the standard `List<T>` collection interface, used as the return type for endpoints that return multiple disputes.

```java
// L18-L20
@RestController
@RequestMapping("/billing/disputes")
public class BillingDisputeController {
```

- **`@RestController`** — a Spring stereotype annotation that marks this class as a web controller whose methods return response *bodies* directly (it combines `@Controller` + `@ResponseBody`). Return values are serialized straight into the HTTP response body (here, as JSON) rather than being interpreted as view/template names.
- **`@RequestMapping("/billing/disputes")`** — sets the base URL path for every endpoint in this class. All method-level mappings below are appended to this prefix. (The Javadoc comments mention a `/teleConnect` prefix on top of this; that would come from a global servlet context-path / base-path configured elsewhere in the app, not from this annotation.)
- The class is `public` so Spring's component scanning can instantiate and manage it as a bean.

```java
// L22-L23
    @Autowired
    private BillingDisputeService disputeService;
```

Declares the single dependency of this controller: the `BillingDisputeService`. `@Autowired` on the field tells Spring to inject the managed `BillingDisputeService` bean here at startup (**field injection**). Every endpoint method delegates its real work to this `disputeService`. The field is `private` because nothing outside this class should access it directly.

*Aside: this uses field injection. Constructor injection (a `final` field set via the constructor) is generally preferred because it makes the dependency mandatory and the class easier to unit-test, but field injection is functionally equivalent at runtime.*

```java
// L25
    // ── Static-path endpoints first (must come before /{disputeId}) ────────────
```

A developer comment, not code. It documents an important Spring routing concern: endpoints with fixed literal path segments (like `/account/...`, `/invoice/...`, `/status/...`) are declared before the catch-all `/{disputeId}` path. This ordering in the source is for readability; what actually matters to Spring is that a literal segment such as `/status/{status}` is matched more specifically than `/{disputeId}`, so a request to `/billing/disputes/status/Resolved` resolves to `getDisputesByStatus` and not to `getDispute` with `disputeId="status"`.

```java
// L27-L33
    /**
     * POST /teleConnect/billing/disputes
     * Body: { "invoiceId": 7001, "disputeReason": "ExcessData",
     *         "disputedAmount": 173.60, "description": "..." }
     */
    @PostMapping
    public ResponseEntity<MessageResponse> raiseDispute(@Valid @RequestBody DisputeRequest request) {
```

The Javadoc documents the endpoint and an example JSON request body. Then:
- **`@PostMapping`** — maps HTTP `POST` requests to this method. With no path argument, it matches the class-level base path exactly: `POST /billing/disputes`. POST is the conventional verb for creating a new resource.
- **`raiseDispute`** returns `ResponseEntity<MessageResponse>` — an HTTP response whose body is a `MessageResponse`.
- **`@RequestBody DisputeRequest request`** — `@RequestBody` tells Spring to deserialize the incoming JSON request body into a `DisputeRequest` object. **`@Valid`** triggers Bean Validation on that object first; if the DTO's constraints are violated, Spring returns a 400 error and this method body never runs.

```java
// L34-L37
        disputeService.raiseDispute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Billing dispute raised successfully"));
    }
```

The method delegates creation to `disputeService.raiseDispute(request)` (the service performs the actual persistence and business rules; its return value, if any, is ignored here). It then builds the HTTP response: `ResponseEntity.status(HttpStatus.CREATED)` sets the status to **201 Created**, and `.body(...)` sets the response body to a new `MessageResponse` containing the success text. So a successful raise returns `201` with `{"message":"Billing dispute raised successfully"}` (or whatever field name `MessageResponse` uses).

```java
// L39-L45
    /**
     * GET /teleConnect/billing/disputes/account/{accountId}
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<DisputeResponse>> getDisputesByAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) DisputeStatus status) {
```

- **`@GetMapping("/account/{accountId}")`** — maps HTTP `GET` requests to `GET /billing/disputes/account/{accountId}`. The `{accountId}` is a *path variable* (a placeholder filled from the URL).
- The method returns `ResponseEntity<List<DisputeResponse>>` — a list of disputes in the body.
- **`@PathVariable Long accountId`** — binds the `{accountId}` URL segment to the `accountId` parameter, converting it to a `Long`. A non-numeric value would cause a type-conversion error.
- **`@RequestParam(required = false) DisputeStatus status`** — binds an optional query-string parameter named `status` (e.g. `?status=Resolved`) and converts it to the `DisputeStatus` enum. `required = false` means the caller may omit it, in which case `status` is `null`. This lets the same endpoint return all disputes for an account, or filter by status.

```java
// L46-L48
        List<DisputeResponse> data = disputeService.getDisputesByAccount(accountId, status);
        return ResponseEntity.ok(data);
    }
```

Delegates to `disputeService.getDisputesByAccount(accountId, status)`, passing both the account id and the (possibly `null`) status filter; the service decides how to interpret a `null` status. The resulting list is wrapped with `ResponseEntity.ok(...)`, which returns **HTTP 200 OK** with the list as the JSON body.

```java
// L50-L56
    /**
     * GET /teleConnect/billing/disputes/invoice/{invoiceId}
     */
    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<DisputeResponse>> getDisputesByInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(disputeService.getDisputesByInvoice(invoiceId));
    }
```

Maps `GET /billing/disputes/invoice/{invoiceId}`. `@PathVariable Long invoiceId` extracts the invoice id from the URL as a `Long`. The method directly returns `ResponseEntity.ok(...)` wrapping the list produced by `disputeService.getDisputesByInvoice(invoiceId)` — i.e. 200 OK with all disputes raised against that invoice. There is no separate local variable here; the service result is passed straight into `ok(...)`.

```java
// L58-L64
    /**
     * GET /teleConnect/billing/disputes/subscriber/{subscriberId}
     */
    @GetMapping("/subscriber/{subscriberId}")
    public ResponseEntity<List<DisputeResponse>> getDisputesBySubscriber(@PathVariable Long subscriberId) {
        return ResponseEntity.ok(disputeService.getDisputesBySubscriber(subscriberId));
    }
```

Maps `GET /billing/disputes/subscriber/{subscriberId}`. `@PathVariable Long subscriberId` binds the subscriber id from the path. It returns 200 OK wrapping `disputeService.getDisputesBySubscriber(subscriberId)` — all disputes belonging to that subscriber. Same delegation pattern as the invoice endpoint.

```java
// L66-L72
    /**
     * GET /teleConnect/billing/disputes/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DisputeResponse>> getDisputesByStatus(@PathVariable DisputeStatus status) {
        return ResponseEntity.ok(disputeService.getDisputesByStatus(status));
    }
```

Maps `GET /billing/disputes/status/{status}`. Here the path variable is bound directly to the `DisputeStatus` enum: **`@PathVariable DisputeStatus status`**. Spring converts the URL segment (e.g. `Resolved`) into the matching enum constant; a value not matching any constant causes a conversion error (typically 400). Returns 200 OK wrapping `disputeService.getDisputesByStatus(status)` — every dispute currently in that status.

```java
// L74
    // ── Dynamic /{disputeId} endpoints below ───────────────────────────────────
```

A separator comment marking the boundary between the literal-path endpoints above and the parameterized `/{disputeId}` endpoints below. Reinforces the routing-order note from line 25.

```java
// L76-L82
    /**
     * GET /teleConnect/billing/disputes/{disputeId}
     */
    @GetMapping("/{disputeId}")
    public ResponseEntity<DisputeResponse> getDispute(@PathVariable Long disputeId) {
        return ResponseEntity.ok(disputeService.getDisputeById(disputeId));
    }
```

Maps `GET /billing/disputes/{disputeId}` — fetch a single dispute by its id. `@PathVariable Long disputeId` binds the id. Returns `ResponseEntity<DisputeResponse>` (a single dispute, not a list) wrapped in 200 OK via `disputeService.getDisputeById(disputeId)`. If the service cannot find the dispute it will throw (e.g. a not-found exception handled elsewhere); this controller does not itself check for absence.

```java
// L84-L91
    /**
     * PUT /teleConnect/billing/disputes/{disputeId}/review
     * Body: { "assignedTo": "exec-201", "notes": "Reviewing UsageSummary for May cycle" }
     */
    @PutMapping("/{disputeId}/review")
    public ResponseEntity<MessageResponse> reviewDispute(
            @PathVariable Long disputeId,
            @Valid @RequestBody DisputeReviewRequest request) {
```

- **`@PutMapping("/{disputeId}/review")`** — maps HTTP `PUT` requests to `PUT /billing/disputes/{disputeId}/review`. PUT is the conventional verb for updating an existing resource. The path combines a dynamic id segment with a literal `/review` action segment.
- Parameters: `@PathVariable Long disputeId` identifies which dispute to act on; `@Valid @RequestBody DisputeReviewRequest request` deserializes and validates the JSON body (containing, per the Javadoc, `assignedTo` and `notes`).
- Returns `ResponseEntity<MessageResponse>`.

```java
// L92-L94
        disputeService.reviewDispute(disputeId, request);
        return ResponseEntity.ok(new MessageResponse("Dispute moved to Under Review"));
    }
```

Delegates the state transition to `disputeService.reviewDispute(disputeId, request)` (the service assigns the reviewer, records notes, and moves the dispute into the "under review" state). Returns 200 OK with a `MessageResponse` confirming "Dispute moved to Under Review". The endpoint reports a status message rather than the full updated dispute.

```java
// L96-L103
    /**
     * PUT /teleConnect/billing/disputes/{disputeId}/resolve
     * Body: { "resolution": "Resolved", "creditAmount": 173.60, "resolutionNotes": "..." }
     */
    @PutMapping("/{disputeId}/resolve")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable Long disputeId,
            @Valid @RequestBody DisputeResolveRequest request) {
```

Maps `PUT /billing/disputes/{disputeId}/resolve` — the action that closes out a dispute. `@PathVariable Long disputeId` selects the dispute; `@Valid @RequestBody DisputeResolveRequest request` deserializes and validates the resolution payload (resolution outcome, credit amount, resolution notes per the Javadoc). Unlike `reviewDispute`, this one returns `ResponseEntity<DisputeResponse>` — the full updated dispute, so the caller can see the final resolved state and applied credit.

```java
// L104-L105
        return ResponseEntity.ok(disputeService.resolveDispute(disputeId, request));
    }
```

Delegates to `disputeService.resolveDispute(disputeId, request)`, which performs the resolution logic (and presumably applies any credit) and returns the updated `DisputeResponse`. That response is wrapped in 200 OK and returned to the client.

```java
// L107-L113
    /**
     * PUT /teleConnect/billing/disputes/{disputeId}/status
     */
    @PutMapping("/{disputeId}/status")
    public ResponseEntity<DisputeResponse> updateStatus(
            @PathVariable Long disputeId,
            @RequestParam DisputeStatus status) {
```

Maps `PUT /billing/disputes/{disputeId}/status` — a direct status override for a dispute. `@PathVariable Long disputeId` identifies the dispute. `@RequestParam DisputeStatus status` reads the new status from the query string (e.g. `?status=UnderReview`) and converts it to the enum; note there is **no** `required = false` here, so the `status` query parameter is **mandatory** — omitting it yields a 400 error. Returns `ResponseEntity<DisputeResponse>` (the updated dispute).

```java
// L114-L116
        return ResponseEntity.ok(disputeService.updateDisputeStatus(disputeId, status));
    }
}
```

Delegates to `disputeService.updateDisputeStatus(disputeId, status)` (which presumably validates the transition and persists the change) and returns the updated `DisputeResponse` wrapped in 200 OK. The final `}` closes the class.

*Aside: this `/status` endpoint passes a raw enum status straight to the service with no payload/notes, so any business rules about which transitions are legal (e.g. you cannot move a resolved dispute back to raised) must be enforced inside the service, not here.*

## How this connects

- **Inbound:** HTTP clients (the front-end, API gateway, or other services) call these URLs. Spring's web layer deserializes JSON bodies into the `request`/`response` DTOs (`DisputeRequest`, `DisputeReviewRequest`, `DisputeResolveRequest`) and binds path/query parameters, running `@Valid` Bean Validation on bodies first.
- **Delegation:** every method forwards to `BillingDisputeService` (the injected `disputeService`). This controller holds **no** business logic, persistence, or transaction handling — it is purely a thin HTTP adapter.
- **Downstream (via the service):** `BillingDisputeService` applies business rules and transactions, then uses Spring Data JPA repositories to read/write the `BillingDispute` entity (and related entities such as the disputed invoice). Results flow back up as `DisputeResponse` / `List<DisputeResponse>` / `MessageResponse` DTOs.
- **Supporting types:** the `DisputeStatus` enum constrains the allowed status values across both path variables and query parameters; the `MessageResponse` / `DisputeResponse` DTOs define the JSON shapes returned to clients.
- To fully understand the behavior, the adjacent files to read next are `BillingDisputeService` (and its implementation), the three request DTOs, the `DisputeResponse`/`MessageResponse` DTOs, and the `BillingDispute` entity with its repository.
