# PaymentController

`PaymentController` is the REST entry point for everything related to payments in the billing microservice. It exposes HTTP endpoints under `/billing/payments` for recording a payment against an invoice and for reading payment records back, then delegates all real work to the `PaymentService`. It sits at the top of the standard layering: **Controller (this file) -> Service (`PaymentService`) -> Repository (Spring Data JPA) -> Entity/DB**, translating HTTP requests/responses into service method calls and DTOs.

## src/main/java/com/teleconnect/billing_service/controller/PaymentController.java

This file is the web-facing controller for payments. Its only job is HTTP plumbing: map URLs and HTTP verbs to methods, validate/deserialize the request body, call the service, and wrap the result in an HTTP response with the right status code.

```java
// L1
package com.teleconnect.billing_service.controller;
```

This is the **package declaration**. It tells the Java compiler that this class lives in the logical namespace `com.teleconnect.billing_service.controller`, which must match the folder path on disk (`.../controller/`). Grouping all controllers in a `controller` package is the conventional way to keep the web layer separate from service, repository, and entity layers. Spring Boot's component scanning (rooted at the main application class's package) will discover this class because it lives under that root package.

```java
// L3-L5
import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.PaymentResponse;
import com.teleconnect.billing_service.service.PaymentService;
```

These three imports bring in the types this controller talks in:
- `PaymentRequest` — the **inbound DTO** (Data Transfer Object), a plain Java object that models the JSON body a client sends when creating a payment (invoice id, amount, method, transaction reference). Using a dedicated request DTO instead of the JPA entity keeps the API contract decoupled from the database schema.
- `PaymentResponse` — the **outbound DTO**, the object serialized back to JSON in responses (payment id, invoice id, amount, date, method, transaction ref, status).
- `PaymentService` — the **service interface** this controller depends on. The controller holds only the interface, not a concrete implementation, so business logic stays in the service layer and can be swapped/mocked.

```java
// L6
import jakarta.validation.Valid;
```

Imports the `@Valid` annotation from **Jakarta Bean Validation** (the `jakarta.validation` namespace is the successor to `javax.validation` used in Spring Boot 3+). `@Valid` triggers automatic validation of the annotated object's constraints. Here it will validate the constraints declared on `PaymentRequest` (`@NotNull`, `@DecimalMin`).

```java
// L7-L9
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
```

Spring imports for dependency injection and HTTP handling:
- `@Autowired` — Spring's annotation that asks the framework to inject (supply) a dependency automatically.
- `HttpStatus` — an enum of HTTP status codes (e.g. `CREATED` = 201, `OK` = 200).
- `ResponseEntity<T>` — a wrapper representing a full HTTP response: status code, headers, and a typed body of type `T`. Returning a `ResponseEntity` (rather than just the body) lets the method control the status code explicitly.

```java
// L10
import org.springframework.web.bind.annotation.*;
```

A wildcard import of Spring Web MVC's binding annotations. This single line brings in everything used below: `@RestController`, `@RequestMapping`, `@PostMapping`, `@GetMapping`, `@RequestBody`, `@PathVariable`, etc. Each is explained where it is used.

```java
// L12
import java.util.List;
```

Imports the standard `List<T>` collection interface, used as the return type when fetching multiple payments for an invoice.

```java
// L14-L16
@RestController
@RequestMapping("/billing/payments")
public class PaymentController {
```

- `@RestController` — a Spring stereotype annotation that marks this class as a web controller whose methods return data directly as the HTTP response body (it combines `@Controller` + `@ResponseBody`). Every handler method's return value is automatically serialized to JSON (via Jackson) and written to the response, rather than being treated as a view/template name. It also makes the class a Spring-managed bean discovered by component scanning.
- `@RequestMapping("/billing/payments")` — declares the **base URL path** for every endpoint in this class. All handler paths below are appended to this prefix, so the full routes are `/billing/payments`, `/billing/payments/{paymentId}`, and `/billing/payments/invoice/{invoiceId}`.

*Aside: The Javadoc comments below claim the paths are under `/api/billing/payments`, but the actual mapping has no `/api` prefix. Unless a global `server.servlet.context-path=/api` (or similar) is configured elsewhere, the real paths start at `/billing/payments`. The comments may be stale.*

```java
// L18-L19
    @Autowired
    private PaymentService paymentService;
```

This declares the controller's single dependency: a reference to a `PaymentService`. `@Autowired` is **field injection** — at startup Spring finds the bean that implements `PaymentService` and assigns it into this field, so the controller never constructs the service itself. The field type is the interface, keeping the controller loosely coupled to whatever concrete implementation Spring wires in.

*Aside: Field injection works but is generally discouraged compared to constructor injection (which makes the dependency final, easier to test, and impossible to forget). It is functionally fine here.*

```java
// L21-L26
    /**
     * POST /api/billing/payments
     * Record a payment against an invoice.
     * Validates invoice status, amount, and duplicate transaction reference.
     * Marks the invoice as PAID on success.
     */
```

A Javadoc comment documenting the first endpoint. It states the intended behavior: record a payment, validate the invoice's status, the amount, and that the transaction reference is not a duplicate, and mark the invoice `PAID` on success. Note that all of that business logic lives in `PaymentService.makePayment(...)`, not in this controller — the comment describes what the service does, which the controller merely invokes. (As noted above, the documented `/api/...` prefix does not match the actual mapping.)

```java
// L27-L29
    @PostMapping
    public ResponseEntity<PaymentResponse> makePayment(
            @Valid @RequestBody PaymentRequest request) {
```

- `@PostMapping` — maps HTTP `POST` requests on the class's base path (`/billing/payments`) to this method. `POST` is the conventional verb for creating a resource (here, recording a new payment).
- The method `makePayment` returns `ResponseEntity<PaymentResponse>` — an HTTP response carrying a `PaymentResponse` body.
- `@RequestBody PaymentRequest request` — `@RequestBody` tells Spring to deserialize the incoming JSON request body into a `PaymentRequest` object (using Jackson) and pass it as the `request` parameter.
- `@Valid` — before the method body runs, Spring validates `request` against the Bean Validation constraints on `PaymentRequest`: `amountPaid` must be non-null (`@NotNull`) and at least `0.01` (`@DecimalMin`), and `paymentMethod` must be non-null (`@NotNull`). If any constraint fails, Spring throws a `MethodArgumentNotValidException` (resulting in an HTTP 400) and the method body never executes. (Note: `invoiceId` and `transactionRef` carry no validation annotations on the DTO, so they are not checked here.)

```java
// L30-L32
        PaymentResponse response = paymentService.makePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
```

- `paymentService.makePayment(request)` — delegates the actual work to the service layer. The service performs the business rules described in the Javadoc (validate invoice status/amount, reject duplicate transaction references, persist the payment, mark the invoice `PAID`) and returns a populated `PaymentResponse`. Any business-rule failures surface as exceptions thrown by the service (typically handled by a global exception handler elsewhere), not here.
- `ResponseEntity.status(HttpStatus.CREATED).body(response)` — builds the HTTP response with status **201 Created** (the correct status for a successful resource creation) and the returned DTO as the JSON body. The closing brace ends the method.

```java
// L34-L37
    /**
     * GET /api/billing/payments/{paymentId}
     * Retrieve a single payment record by its ID.
     */
    @GetMapping("/{paymentId}")
```

Javadoc plus the mapping for the second endpoint. `@GetMapping("/{paymentId}")` maps HTTP `GET` requests to the base path plus `/{paymentId}` — i.e. `/billing/payments/{paymentId}`. The `{paymentId}` segment is a **path variable**: a placeholder whose value is taken from the URL (e.g. `/billing/payments/42` -> `paymentId = 42`). `GET` is the read verb and is the right choice for fetching a single resource.

```java
// L39-L42
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }
```

- `getPaymentById` returns `ResponseEntity<PaymentResponse>`.
- `@PathVariable Long paymentId` — binds the `{paymentId}` URL segment to the `Long paymentId` parameter; Spring converts the textual URL value to a `Long`. (If the segment isn't a valid number, Spring raises a type-mismatch error -> HTTP 400.)
- `paymentService.getPaymentById(paymentId)` — asks the service to look up the payment by id and return its `PaymentResponse`. If no payment exists, the service is responsible for throwing a not-found exception (typically mapped to HTTP 404 by a global handler); this controller does no null/existence checking itself.
- `ResponseEntity.ok(...)` — wraps the result with HTTP status **200 OK** and the DTO as the body. The closing brace ends the method.

```java
// L44-L48
    /**
     * GET /api/billing/payments/invoice/{invoiceId}
     * Retrieve all payment records for a given invoice.
     */
    @GetMapping("/invoice/{invoiceId}")
```

Javadoc plus the mapping for the third endpoint. `@GetMapping("/invoice/{invoiceId}")` maps `GET /billing/payments/invoice/{invoiceId}` to this handler. The literal `invoice` segment plus the `{invoiceId}` path variable distinguish this "list payments for one invoice" route from the "get one payment by its own id" route above.

```java
// L49-L52
    public ResponseEntity<List<PaymentResponse>> getPaymentsByInvoice(
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(paymentService.getPaymentsByInvoice(invoiceId));
    }
}
```

- `getPaymentsByInvoice` returns `ResponseEntity<List<PaymentResponse>>` — an HTTP response whose body is a JSON array of payment DTOs.
- `@PathVariable Long invoiceId` — binds the `{invoiceId}` URL segment to the `Long invoiceId` parameter.
- `paymentService.getPaymentsByInvoice(invoiceId)` — delegates to the service, which returns the list of all payments associated with that invoice (possibly empty). The controller does not filter or sort; it returns whatever the service produces.
- `ResponseEntity.ok(...)` — wraps that list with HTTP status **200 OK**. The final `}` closes the `PaymentController` class.

## How this connects

`PaymentController` is the thinnest possible web layer: it contains no business logic, only HTTP mapping, request validation, delegation, and status-code selection. Its inbound contract is the `PaymentRequest` DTO (`src/main/java/com/teleconnect/billing_service/dto/request/PaymentRequest.java`), whose `@NotNull`/`@DecimalMin` constraints are enforced by `@Valid`; its outbound contract is `PaymentResponse` (`src/main/java/com/teleconnect/billing_service/dto/response/PaymentResponse.java`). All three methods forward to the `PaymentService` interface (`src/main/java/com/teleconnect/billing_service/service/PaymentService.java`), whose implementation carries out the real rules (invoice-status checks, duplicate-transaction-reference detection, persistence via the JPA repositories, and marking the invoice `PAID`). From there the flow continues down the standard chain — **Service -> Repository -> Entity/DB** — while exceptions thrown by the service are expected to be translated into appropriate HTTP error responses by a global exception handler rather than by this controller.
