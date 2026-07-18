# Service Implementation: `PaymentServiceImpl`

`PaymentServiceImpl` is the concrete business-logic class that records payments against invoices in the billing microservice. It implements the `PaymentService` interface and sits in the **Service** layer of the classic Spring layering: a REST **Controller** receives an HTTP request and delegates to this service; the service applies business rules and talks to the **Repository** layer (`PaymentRepository`, `InvoiceRepository`); the repositories load and persist **Entity** objects (`Payment`, `Invoice`) mapped to **database** tables via JPA. This file is where the actual "should this payment be allowed, and what state changes result" decisions live, before anything is committed to the database.

## src/main/java/com/teleconnect/billing_service/service/impl/PaymentServiceImpl.java

This file is the Spring-managed implementation of the payment use cases: making a payment, fetching a single payment, and listing all payments for an invoice. It coordinates two repositories inside a transaction and maps entities to response DTOs.

```java
// L1
package com.teleconnect.billing_service.service.impl;
```

The `package` statement declares the fully-qualified namespace of this class. `...service.impl` is the conventional location for service *implementations* (the interface lives one package up in `...service`). The package must match the folder path on disk; it also controls which other classes can see package-private members.

```java
// L3-L13
import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.PaymentResponse;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.entity.Payment;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.enums.PaymentStatus;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.repository.PaymentRepository;
import com.teleconnect.billing_service.service.PaymentService;
```

These imports pull in the project's own types this class depends on:
- `PaymentRequest` — the inbound DTO (Data Transfer Object: a plain object used to carry data across a layer boundary) that the controller hands in. It holds `invoiceId`, `amountPaid`, `paymentMethod`, `transactionRef`.
- `PaymentResponse` — the outbound DTO returned to the controller/client.
- `Invoice`, `Payment` — JPA entities (database-mapped objects).
- `InvoiceStatus`, `PaymentStatus` — enums describing the lifecycle states of an invoice (`GENERATED, SENT, PAID, OVERDUE, DISPUTED`) and a payment (`SUCCESS, FAILED, PENDING`).
- `BillingException` — a custom exception used for **business-rule violations** (e.g., already paid, underpayment).
- `ResourceNotFoundException` — a custom exception used when a requested entity does not exist (typically mapped to HTTP 404 by a global exception handler).
- `InvoiceRepository`, `PaymentRepository` — Spring Data JPA repositories that provide CRUD and custom finder methods.
- `PaymentService` — the interface this class implements (the contract the controller programs against).

```java
// L14-L16
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

Framework imports:
- `@Autowired` — Spring's dependency-injection annotation; it tells the Spring container to supply a bean (a managed object) automatically.
- `@Service` — a Spring stereotype annotation that marks this class as a service-layer bean so it is detected during component scanning and registered in the application context.
- `@Transactional` — Spring's declarative transaction annotation; a method (or class) annotated with it runs inside a database transaction that commits on normal return and rolls back on unchecked exceptions.

```java
// L18-L19
import java.util.List;
import java.util.stream.Collectors;
```

Standard JDK imports. `List` is the collection type returned by the "list payments" method. `Collectors` provides the `toList()` collector used to gather a stream's elements back into a `List`.

```java
// L21-L22
@Service
public class PaymentServiceImpl implements PaymentService {
```

`@Service` registers this class as a singleton Spring bean. `implements PaymentService` means this class must provide concrete bodies for every method declared in the `PaymentService` interface (`makePayment`, `getPaymentById`, `getPaymentsByInvoice`). Because it implements the interface, controllers can depend on the *interface* type and Spring will inject this implementation, keeping the layers loosely coupled.

```java
// L24-L25
    @Autowired
    private PaymentRepository paymentRepository;
```

A field of type `PaymentRepository` injected by Spring (**field injection**). `PaymentRepository` extends `JpaRepository<Payment, Long>`, so it gives this service ready-made operations like `save`, `findById`, plus the custom finders `findByInvoiceId(Long)` and `findByTransactionRef(String)`. This is the bridge from the service into the persistence layer for `Payment` rows.

```java
// L27-L28
    @Autowired
    private InvoiceRepository invoiceRepository;
```

A second injected repository, this one for `Invoice` entities. The payment logic needs it to look up the target invoice, validate its state, and update it after a successful payment.

*Aside: both repositories use field injection (`@Autowired` directly on the field). It works, but constructor injection is generally preferred because it makes dependencies explicit, allows `final` fields, and makes the class easier to unit-test without a Spring context. This is a style observation, not a bug.*

```java
// L30-L32
    @Override
    @Transactional
    public PaymentResponse makePayment(PaymentRequest request) {
```

- `@Override` declares that this method implements/overrides a method from the `PaymentService` interface; the compiler verifies the signature matches.
- `@Transactional` wraps the whole method in a single database transaction. This is important here because the method writes **two** rows (a new `Payment` and an updated `Invoice`); if the second write fails, the transaction rolls back so you never end up with a saved payment but an unmarked invoice. Any `RuntimeException` thrown inside (including `BillingException` and `ResourceNotFoundException`) triggers a rollback by default.
- Parameters/return: it takes the inbound `PaymentRequest` and returns a `PaymentResponse` describing the persisted payment.

```java
// L33-L35
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with ID: " + request.getInvoiceId()));
```

Look up the invoice the payment is for. `findById(...)` returns an `Optional<Invoice>` — a container that either holds the found invoice or is empty. `orElseThrow(...)` unwraps it: if present, you get the `Invoice`; if empty, the supplied lambda builds and throws a `ResourceNotFoundException` carrying the missing ID in its message. So a payment for a non-existent invoice fails fast before any business checks run.

```java
// L37-L39
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BillingException("Invoice " + request.getInvoiceId() + " is already paid");
        }
```

First business rule: you cannot pay an invoice that is already in the `PAID` state. The enum comparison `==` is correct here because Java enum constants are singletons. If already paid, throw a `BillingException` (a business-rule failure, distinct from "not found").

```java
// L40-L43
        if (invoice.getStatus() == InvoiceStatus.DISPUTED) {
            throw new BillingException(
                    "Cannot process payment for a disputed invoice. Resolve the dispute first.");
        }
```

Second business rule: an invoice in the `DISPUTED` state cannot be paid until the dispute is resolved. Again signalled with `BillingException`.

```java
// L44-L48
        if (request.getAmountPaid().compareTo(invoice.getTotalAmount()) < 0) {
            throw new BillingException(
                    "Payment amount " + request.getAmountPaid()
                    + " is less than the invoice total " + invoice.getTotalAmount());
        }
```

Third business rule: the payment must cover the full invoice total. Both amounts are `BigDecimal` (a precise decimal type used for money to avoid floating-point rounding errors). You must compare `BigDecimal`s with `compareTo`, not `<` or `equals` — `compareTo` returns a negative number when `amountPaid` is less than `totalAmount`, so `< 0` means "underpayment", which is rejected. Equal or greater amounts are allowed (so overpayment is permitted by this code).

*Aside: overpayment is accepted but later (L69) only `amountPaid` is recorded on the invoice's `paidAmount`; there is no change/refund handling. That appears intentional for this simple model, just worth noting.*

```java
// L50-L56
        if (request.getTransactionRef() != null && !request.getTransactionRef().isBlank()) {
            paymentRepository.findByTransactionRef(request.getTransactionRef())
                    .ifPresent(existing -> {
                        throw new BillingException(
                                "Duplicate transaction reference: " + request.getTransactionRef());
                    });
        }
```

Fourth business rule: prevent duplicate transaction references. The outer `if` runs the check only when a `transactionRef` was actually supplied — `!= null` guards against null and `!isBlank()` skips empty/whitespace-only strings (so blank refs are simply not deduplicated). When a real ref is present, `findByTransactionRef` returns an `Optional<Payment>`; `ifPresent(...)` runs its lambda only if a matching payment already exists, throwing `BillingException` to reject the duplicate. This complements the `@Column(unique = true)` constraint on `Payment.transactionRef` by producing a clean business error instead of a raw database constraint violation.

```java
// L58-L64
        Payment payment = Payment.builder()
                .invoiceId(request.getInvoiceId())
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .transactionRef(request.getTransactionRef())
                .status(PaymentStatus.SUCCESS)
                .build();
```

All validations passed, so construct the new `Payment` entity using its hand-written **builder** (a fluent object-construction pattern; here it is plain Java in `Payment.Builder`, not Lombok). Each `.xxx(...)` call sets one field and returns the builder; `.build()` produces the finished `Payment`. The values come straight from the request, and `status` is hard-coded to `PaymentStatus.SUCCESS` — this service treats a payment that passes all checks as immediately successful (there is no asynchronous/pending gateway step here). Note `paymentId` and `paymentDate` are not set: the ID is generated by the database (`@GeneratedValue(strategy = IDENTITY)`), and `paymentDate` defaults to `LocalDateTime.now()` via the entity's `@PrePersist` hook just before insert.

```java
// L66
        Payment saved = paymentRepository.save(payment);
```

Persist the new payment. `save(...)` inserts the row (since the entity has no ID yet) and returns the managed, fully-populated entity — including the database-generated `paymentId` and the `paymentDate` filled in by `@PrePersist`. Capturing the return value in `saved` is what lets the response include those generated values.

```java
// L68-L70
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAmount(request.getAmountPaid());
        invoiceRepository.save(invoice);
```

Apply the side effects of a successful payment to the invoice: mark it `PAID` and record how much was paid. `invoiceRepository.save(invoice)` writes the change. Because both `save` calls and these mutations occur inside the `@Transactional` method, they commit together as one unit; a failure here would roll back the payment insert as well.

*Aside: `invoice` was loaded inside the same transaction and is therefore a managed JPA entity, so Hibernate's dirty-checking would flush these field changes even without the explicit `save`. The explicit `save` is harmless and arguably clearer, just technically redundant.*

```java
// L72
        return toResponse(saved);
```

Map the persisted `Payment` entity to a `PaymentResponse` DTO and return it to the caller (the controller). Returning a DTO rather than the entity keeps persistence details out of the API contract.

```java
// L73
    }
```

Closes `makePayment`. Normal return triggers the transaction commit.

```java
// L75-L81
    @Override
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with ID: " + paymentId));
        return toResponse(payment);
    }
```

A read-only lookup of a single payment by its primary key. Parameter `paymentId` (`Long`) is the ID; the return type is `PaymentResponse`. `findById` yields an `Optional<Payment>`; `orElseThrow` either unwraps the payment or throws `ResourceNotFoundException` with the missing ID. The found entity is converted to a DTO via `toResponse` and returned. There is **no** `@Transactional` here; for a single read it is not strictly needed, though some teams annotate reads with `@Transactional(readOnly = true)` for clarity/optimization.

```java
// L83-L87
    @Override
    public List<PaymentResponse> getPaymentsByInvoice(Long invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("Invoice not found with ID: " + invoiceId);
        }
```

Lists all payments belonging to one invoice. It first validates that the invoice exists at all using `existsById(...)` (a lightweight existence check that does not load the whole entity). If the invoice is missing, it throws `ResourceNotFoundException` rather than silently returning an empty list — this distinguishes "no such invoice" from "invoice exists but has no payments yet."

```java
// L88-L92
        return paymentRepository.findByInvoiceId(invoiceId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
```

Fetch every `Payment` for the invoice via the derived query method `findByInvoiceId`, which returns a `List<Payment>`. The list is turned into a `Stream`, each `Payment` is mapped to a `PaymentResponse` through the method reference `this::toResponse` (equivalent to `p -> toResponse(p)`), and the results are collected back into a `List<PaymentResponse>` that is returned. An invoice with no payments yields an empty list (not an error).

```java
// L94-L104
    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .invoiceId(payment.getInvoiceId())
                .amountPaid(payment.getAmountPaid())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .transactionRef(payment.getTransactionRef())
                .status(payment.getStatus())
                .build();
    }
```

A `private` helper that converts a `Payment` entity into a `PaymentResponse` DTO using the response's builder. It is `private` because it is an internal mapping detail not part of the service contract. Each getter on the entity feeds the matching builder field, so the response mirrors the persisted payment one-to-one (including the generated `paymentId` and `paymentDate`). Centralizing the mapping here means all three public methods produce identically shaped responses.

```java
// L105
}
```

Closes the class.

## How this connects

- **Called by (upstream):** a payment REST controller (Controller layer) injects the `PaymentService` interface and calls `makePayment`, `getPaymentById`, or `getPaymentsByInvoice`, passing in a validated `PaymentRequest` (the `@NotNull`/`@DecimalMin` constraints on `PaymentRequest` are enforced at the controller boundary via `@Valid`, before this code runs). The `BillingException` and `ResourceNotFoundException` thrown here are typically translated into HTTP status codes by a `@RestControllerAdvice` global exception handler.
- **Calls into (downstream):** `PaymentRepository` and `InvoiceRepository` (Repository layer, Spring Data JPA) to read and persist `Payment` and `Invoice` entities, which map to the `payments` and invoice database tables.
- **Data shapes:** input arrives as `PaymentRequest`; persisted state lives in the `Payment` and `Invoice` entities; output leaves as `PaymentResponse`. The `toResponse` helper is the single conversion point from entity to response DTO.
- **Transaction boundary:** `makePayment` is the only transactional method and is the only one that writes; it atomically inserts the payment and updates the invoice, ensuring the two stay consistent.
