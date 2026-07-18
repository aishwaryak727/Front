# InvoiceServiceImpl — Core Invoice Business Logic

`InvoiceServiceImpl` is the concrete implementation of the `InvoiceService` interface and is the heart of the billing microservice. It contains all invoice business logic: generating invoices from billing cycles, processing payments, applying and waiving late fees, transitioning invoice statuses (including a scheduled batch job that marks overdue invoices), and rendering PDF documents (single invoice and full account statement). In the layered architecture, this class sits in the **Service** layer: a `@RestController` (Controller layer) calls these methods, which in turn use Spring Data JPA **Repository** interfaces to load and persist **Entity** objects mapped to the database, and finally maps entities to **DTO** response objects returned to the caller.

## src/main/java/com/teleconnect/billing_service/service/impl/InvoiceServiceImpl.java

This file is the single largest unit of business logic in the service: it implements every invoice-related use case and is the only place that orchestrates invoices, billing cycles, and payments together.

```java
// L1
package com.teleconnect.billing_service.service.impl;
```

The `package` declaration places this class in the `...service.impl` package. By convention the service *interface* (`InvoiceService`) lives in the `...service` package and its implementation(s) live in `...service.impl`. This separation lets controllers depend on the abstraction while Spring injects the concrete implementation at runtime.

```java
// L3-L19
import com.teleconnect.billing_service.dto.request.InvoiceGenerationRequest;
import com.teleconnect.billing_service.dto.request.LateFeeRequest;
import com.teleconnect.billing_service.dto.request.LateFeeWaiverRequest;
import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.InvoiceResponse;
import com.teleconnect.billing_service.entity.BillingCycle;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.entity.Payment;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.enums.PaymentStatus;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.BillingCycleRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.repository.PaymentRepository;
import com.teleconnect.billing_service.service.InvoiceService;
```

These imports bring in the application's own types. The **request DTOs** (`InvoiceGenerationRequest`, `LateFeeRequest`, `LateFeeWaiverRequest`, `PaymentRequest`) are plain data carriers that hold the inbound data the controller received from clients; using DTOs instead of entities keeps the API contract separate from the database model. `InvoiceResponse` is the **response DTO** returned to callers. The **entities** (`BillingCycle`, `Invoice`, `Payment`) are JPA-mapped classes that correspond to database tables. The **enums** (`BillingCycleStatus`, `InvoiceStatus`, `PaymentStatus`) model the finite set of states each record can be in. The two **exceptions** are custom runtime exceptions: `BillingException` for business-rule violations and `ResourceNotFoundException` for missing records (these are typically mapped to HTTP 400/409 and 404 respectively by a global exception handler). The three **repositories** are Spring Data JPA interfaces that provide CRUD and custom finder methods. `InvoiceService` is the interface this class implements.

```java
// L20-L25
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
```

These imports come from **Apache PDFBox**, an open-source library for creating and manipulating PDF files. `PDDocument` is the in-memory PDF document, `PDPage` is a single page, `PDPageContentStream` is the drawing surface used to write text and lines onto a page, `PDRectangle` provides standard page sizes (e.g. `A4`), and `PDType1Font` / `Standard14Fonts` give access to the 14 built-in PDF fonts (here Helvetica and Helvetica-Bold). These are used by the two PDF-generation methods.

```java
// L26-L29
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

These are Spring framework imports. `@Autowired` requests dependency injection of a bean. `@Scheduled` marks a method to be run automatically on a timer/cron. `@Service` marks the class as a service-layer Spring-managed component. `@Transactional` declares that a method runs inside a database transaction.

```java
// L31-L38
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
```

Standard JDK imports. `ByteArrayOutputStream` collects the generated PDF bytes in memory. `IOException` is thrown by PDFBox I/O operations. `BigDecimal` is the precise decimal type used for all monetary amounts (never `double`, to avoid rounding errors), and `RoundingMode` controls how amounts are rounded (here always `HALF_UP`, standard commercial rounding). `LocalDate` and `LocalDateTime` are date/time types. `List` and `Collectors` support working with collections and the Stream API.

```java
// L40-L41
@Service
public class InvoiceServiceImpl implements InvoiceService {
```

`@Service` is a Spring stereotype annotation. It tells Spring's component scanning to instantiate this class as a singleton bean in the application context, making it available for injection wherever an `InvoiceService` is needed. `implements InvoiceService` means this class fulfills the service contract; controllers depend on the `InvoiceService` interface, and Spring wires in this implementation.

```java
// L43-L50
    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private BillingCycleRepository billingCycleRepository;

    @Autowired
    private PaymentRepository paymentRepository;
```

These are the three injected collaborators. `@Autowired` on a field tells Spring to perform **field injection**: at startup Spring finds the matching repository bean and assigns it to this field. `invoiceRepository` handles all `Invoice` persistence and queries; `billingCycleRepository` handles `BillingCycle` records; `paymentRepository` handles `Payment` records. Each is a Spring Data JPA repository, meaning Spring auto-generates the implementation (CRUD plus any declared finder methods) at runtime.

*Aside: field injection works but constructor injection is generally preferred (it makes dependencies final, easier to test, and explicit). This is a design note, not a bug.*

### generateInvoice — create a new invoice for a billing cycle

```java
// L52-L54
    @Override
    @Transactional
    public InvoiceResponse generateInvoice(InvoiceGenerationRequest request) {
```

`@Override` confirms this method implements a method declared in `InvoiceService`. `@Transactional` wraps the whole method in a single database transaction: every write inside it commits together at the end, or all roll back if a runtime exception is thrown. This matters here because the method writes both a billing cycle and an invoice — they must succeed or fail atomically. The method takes an `InvoiceGenerationRequest` (the data needed to build an invoice) and returns an `InvoiceResponse` DTO.

```java
// L55-L57
        BillingCycle cycle = billingCycleRepository.findById(request.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Billing cycle not found: " + request.getCycleId()));
```

It loads the billing cycle by the ID supplied in the request. `findById` returns an `Optional<BillingCycle>` — a container that either holds the value or is empty. `.orElseThrow(...)` unwraps it if present, or throws a `ResourceNotFoundException` (lazily constructed via the lambda) if the cycle does not exist. This enforces that you cannot invoice against a non-existent cycle.

```java
// L59-L61
        if (cycle.getStatus() == BillingCycleStatus.CLOSED) {
            throw new BillingException("Cannot generate invoice for a closed billing cycle");
        }
```

**Business rule:** a `CLOSED` billing cycle can no longer be invoiced. If the cycle's status is `CLOSED`, a `BillingException` is thrown to block the operation.

```java
// L63-L68
        invoiceRepository.findByAccountIdAndCycleId(request.getAccountId(), request.getCycleId())
                .ifPresent(inv -> {
                    throw new BillingException(
                            "Invoice already exists for account " + request.getAccountId()
                            + " and cycle " + request.getCycleId());
                });
```

**Business rule (idempotency / duplicate guard):** there can be at most one invoice per (account, cycle) pair. The repository finder returns an `Optional<Invoice>`; `.ifPresent(...)` runs the lambda only if an invoice already exists, and that lambda throws a `BillingException` to prevent a duplicate. If no invoice exists, nothing happens and execution continues.

```java
// L70-L74
        BigDecimal total = request.getPlanCharges()
                .add(request.getExcessCharges())
                .add(request.getAddOnCharges())
                .add(request.getTaxes())
                .setScale(2, RoundingMode.HALF_UP);
```

It computes the invoice total by summing the four charge components from the request (plan charges, excess/overage charges, add-on charges, and taxes). `BigDecimal.add` is immutable — each call returns a new value. `setScale(2, RoundingMode.HALF_UP)` rounds the result to exactly two decimal places using standard half-up rounding, giving a currency-correct total.

*Aside: this code assumes the four charge fields are non-null. If any were null, this would throw a `NullPointerException`. Validation of those fields is presumably handled upstream on the request DTO (e.g. `@NotNull`).*

```java
// L76-L86
        Invoice invoice = Invoice.builder()
                .accountId(request.getAccountId())
                .cycleId(request.getCycleId())
                .planCharges(request.getPlanCharges())
                .excessCharges(request.getExcessCharges())
                .addOnCharges(request.getAddOnCharges())
                .taxes(request.getTaxes())
                .totalAmount(total)
                .dueDate(cycle.getCycleEnd().plusDays(15))
                .status(InvoiceStatus.GENERATED)
                .build();
```

It constructs a new `Invoice` entity using the **builder pattern** (provided by Lombok's `@Builder` on the `Invoice` class). `Invoice.builder()` returns a builder, each `.field(value)` sets a property fluently, and `.build()` produces the immutable-ish constructed object. The charge fields are copied straight from the request. **Business rule:** the due date is set to 15 days after the cycle's end date (`cycle.getCycleEnd().plusDays(15)`), and the new invoice starts in the `GENERATED` status. The `invoiceId` is not set here because it is database-generated.

```java
// L88-L90
        cycle.setStatus(BillingCycleStatus.GENERATED);
        cycle.setGeneratedDate(LocalDate.now());
        billingCycleRepository.save(cycle);
```

After creating the invoice, the billing cycle is advanced: its status becomes `GENERATED` and its generated-date is stamped with today's date (`LocalDate.now()`). `billingCycleRepository.save(cycle)` persists the updated cycle. Because the cycle was loaded inside this transaction it is already a managed JPA entity, so saving makes the state change explicit and durable.

```java
// L92-L93
        return toResponse(invoiceRepository.save(invoice));
    }
```

The invoice is saved via `invoiceRepository.save(invoice)`, which inserts the row and returns the persisted entity now populated with its database-generated `invoiceId`. That entity is passed to the private `toResponse(...)` helper (see L496) to convert it into an `InvoiceResponse` DTO, which is returned to the caller.

### getInvoiceById — fetch a single invoice

```java
// L95-L98
    @Override
    public InvoiceResponse getInvoiceById(Long invoiceId) {
        return toResponse(findById(invoiceId));
    }
```

A simple read. It delegates to the private `findById` helper (L490) which loads the invoice or throws `ResourceNotFoundException` if missing, then maps it to a response DTO. There is no `@Transactional` here because it is a single read with no writes.

### getInvoicesByAccount — list all invoices for an account

```java
// L100-L104
    @Override
    public List<InvoiceResponse> getInvoicesByAccount(Long accountId) {
        return invoiceRepository.findByAccountId(accountId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }
```

Returns every invoice belonging to an account. `findByAccountId` returns a `List<Invoice>`. `.stream()` turns the list into a stream, `.map(this::toResponse)` converts each entity to a DTO (using a method reference to `toResponse`), and `.collect(Collectors.toList())` gathers the results into a `List<InvoiceResponse>`.

### getInvoicesByAccount (filtered overload) — list with optional status / date filters

```java
// L106-L108
    @Override
    public List<InvoiceResponse> getInvoicesByAccount(Long accountId, InvoiceStatus status,
                                                      LocalDate fromDate, LocalDate toDate) {
```

This is an **overloaded** version of `getInvoicesByAccount` (same name, different parameters). It supports optional filtering by invoice `status` and by a due-date range (`fromDate`..`toDate`). Any of these filter arguments may be `null`, meaning "do not filter on this dimension".

```java
// L109-L119
        List<Invoice> invoices;
        if (status != null && fromDate != null && toDate != null) {
            invoices = invoiceRepository.findByAccountIdAndStatusAndDueDateBetween(
                    accountId, status, fromDate, toDate);
        } else if (status != null) {
            invoices = invoiceRepository.findByAccountIdAndStatus(accountId, status);
        } else if (fromDate != null && toDate != null) {
            invoices = invoiceRepository.findByAccountIdAndDueDateBetween(accountId, fromDate, toDate);
        } else {
            invoices = invoiceRepository.findByAccountId(accountId);
        }
```

A four-way branch selects the appropriate repository finder based on which filters are present:
- **Both status and a full date range:** `findByAccountIdAndStatusAndDueDateBetween`.
- **Only status:** `findByAccountIdAndStatus`.
- **Only a full date range (both dates):** `findByAccountIdAndDueDateBetween`.
- **Neither (or only one date supplied):** falls through to `findByAccountId`, returning all invoices for the account.

*Aside: if exactly one of `fromDate`/`toDate` is provided (but not both), the date filter is silently ignored — only the status branch or the unfiltered branch can apply. This is intentional given the `&&` conditions but worth noting as a subtle behavior.*

```java
// L120-L121
        return invoices.stream().map(this::toResponse).collect(Collectors.toList());
    }
```

Whichever list was selected is mapped to DTOs and returned, identical to the simpler overload.

### getInvoicesByStatus — list invoices in a given status

```java
// L123-L127
    @Override
    public List<InvoiceResponse> getInvoicesByStatus(InvoiceStatus status) {
        return invoiceRepository.findByStatus(status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }
```

Returns all invoices across all accounts that are currently in the given `status` (e.g. all `OVERDUE` invoices), mapped to response DTOs.

### processPayment — record a payment and mark an invoice paid

```java
// L129-L132
    @Override
    @Transactional
    public InvoiceResponse processPayment(PaymentRequest request) {
        Invoice invoice = findById(request.getInvoiceId());
```

`@Transactional` again ensures the payment row and the invoice update commit atomically. It first loads the target invoice via `findById` (throwing `ResourceNotFoundException` if it does not exist).

```java
// L134-L139
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BillingException("Invoice is already paid");
        }
        if (invoice.getStatus() == InvoiceStatus.DISPUTED) {
            throw new BillingException("Cannot process payment for a disputed invoice. Resolve the dispute first.");
        }
```

**Business rules (state guards):** a `PAID` invoice cannot be paid again, and a `DISPUTED` invoice cannot be paid until the dispute is resolved. Each violation throws a `BillingException`.

```java
// L140-L144
        if (request.getAmountPaid().compareTo(invoice.getTotalAmount()) < 0) {
            throw new BillingException(
                    "Payment amount " + request.getAmountPaid()
                    + " is less than the invoice total " + invoice.getTotalAmount());
        }
```

**Business rule (full payment required):** the amount paid must be at least the invoice total. `BigDecimal.compareTo` returns a negative number when the left value is smaller; so if `amountPaid < totalAmount` the payment is rejected. This means partial payments are not allowed; the customer must pay the full amount (overpayment is permitted, since only `< 0` is rejected).

```java
// L146-L152
        if (request.getTransactionRef() != null && !request.getTransactionRef().isBlank()) {
            paymentRepository.findByTransactionRef(request.getTransactionRef())
                    .ifPresent(existing -> {
                        throw new BillingException(
                                "Duplicate transaction reference: " + request.getTransactionRef());
                    });
        }
```

**Business rule (idempotent transaction reference):** if the request carries a non-blank transaction reference, the code checks whether a payment with that same reference already exists. `findByTransactionRef` returns an `Optional<Payment>`; `.ifPresent(...)` throws a `BillingException` when a match is found, preventing the same external transaction from being recorded twice. A null or blank reference skips this check.

```java
// L154-L161
        Payment payment = Payment.builder()
                .invoiceId(request.getInvoiceId())
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .transactionRef(request.getTransactionRef())
                .status(PaymentStatus.SUCCESS)
                .build();
        paymentRepository.save(payment);
```

A `Payment` entity is built (again via Lombok builder) linking it to the invoice (`invoiceId`), capturing the amount, method, and transaction reference, and marking its status as `SUCCESS`. It is persisted with `paymentRepository.save(payment)`, creating the payment record.

*Aside: the payment status is hard-coded to `SUCCESS` — there is no gateway call or failure path here, so the service assumes the payment has already succeeded externally.*

```java
// L163-L166
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAmount(request.getAmountPaid());
        return toResponse(invoiceRepository.save(invoice));
    }
```

The invoice is then transitioned to `PAID` and its `paidAmount` is set to the amount paid. Saving the invoice persists these changes, and the updated invoice is returned as a DTO. Because both the payment insert and the invoice update happen in one transaction, they cannot be left half-applied.

*Aside: `paidAmount` records the full amount tendered, which may exceed `totalAmount` when overpaid; there is no change/refund handling.*

### payInvoice — convenience wrapper that injects the path ID

```java
// L168-L173
    @Override
    @Transactional
    public InvoiceResponse payInvoice(Long invoiceId, PaymentRequest request) {
        request.setInvoiceId(invoiceId);
        return processPayment(request);
    }
```

This is a thin convenience method, typically called from a REST endpoint like `POST /invoices/{invoiceId}/pay` where the invoice ID comes from the URL path rather than the request body. It copies the path `invoiceId` into the request DTO and then delegates to `processPayment`, reusing all of that method's validation and persistence logic.

### applyLateFee — add a late fee to an overdue invoice

```java
// L175-L182
    @Override
    @Transactional
    public InvoiceResponse applyLateFee(Long invoiceId, LateFeeRequest request) {
        Invoice invoice = findById(invoiceId);

        if (invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new BillingException("Late fee can only be applied to OVERDUE invoices");
        }
```

Loads the invoice and enforces the **business rule** that late fees apply only to invoices currently in the `OVERDUE` status; any other status throws a `BillingException`.

```java
// L184-L188
        BigDecimal feeAmount = request.getFeeAmount().setScale(2, RoundingMode.HALF_UP);
        invoice.setLateFee(invoice.getLateFee().add(feeAmount));
        invoice.setTotalAmount(invoice.getTotalAmount().add(feeAmount));

        return toResponse(invoiceRepository.save(invoice));
    }
```

The requested fee is rounded to two decimals. The fee is **accumulated** onto any existing late fee (`getLateFee().add(feeAmount)`), so calling this repeatedly stacks fees. The same amount is added to the invoice total so the customer now owes more. The invoice is saved and returned.

*Aside: this assumes `invoice.getLateFee()` is non-null (it relies on the late fee defaulting to zero, presumably set in the entity). If it were null, `.add` would throw a `NullPointerException`.*

### waiveLateFee — remove a late fee

```java
// L191-L198
    @Override
    @Transactional
    public InvoiceResponse waiveLateFee(Long invoiceId, LateFeeWaiverRequest request) {
        Invoice invoice = findById(invoiceId);

        if (invoice.getLateFee() == null || invoice.getLateFee().compareTo(BigDecimal.ZERO) == 0) {
            throw new BillingException("No late fee to waive for invoice: " + invoiceId);
        }
```

Loads the invoice and checks that there is actually a late fee to waive. The guard throws a `BillingException` if the late fee is null **or** equal to zero (`compareTo(BigDecimal.ZERO) == 0` is the correct way to compare `BigDecimal` for numeric equality, unlike `.equals` which also compares scale).

*Aside: the `request` parameter (`LateFeeWaiverRequest`, which presumably carries a waiver reason/approver) is accepted but never used in this method body — the reason is not persisted here.*

```java
// L200-L204
        invoice.setTotalAmount(invoice.getTotalAmount().subtract(invoice.getLateFee()));
        invoice.setLateFee(BigDecimal.ZERO);

        return toResponse(invoiceRepository.save(invoice));
    }
```

The entire current late fee is subtracted from the invoice total, then the late fee is reset to zero. The invoice is saved and returned. The order matters: the subtraction must use the old late fee before it is zeroed.

### markOverdueInvoices — scheduled batch job

```java
// L206-L209
    @Override
    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void markOverdueInvoices() {
```

`@Scheduled(cron = "0 0 1 * * *")` makes Spring invoke this method automatically every day at **01:00:00**. The cron fields are `second minute hour day-of-month month day-of-week`, so `0 0 1 * * *` means "at second 0, minute 0, hour 1, every day". `@Transactional` ensures all status updates in this batch commit together. The method returns `void` (it is a background job, not a request handler). For the schedule to fire, the application must have scheduling enabled (typically `@EnableScheduling` on a configuration class).

```java
// L210-L213
        List<Invoice> generatedOverdue = invoiceRepository
                .findByStatusAndDueDateBefore(InvoiceStatus.GENERATED, LocalDate.now());
        List<Invoice> sentOverdue = invoiceRepository
                .findByStatusAndDueDateBefore(InvoiceStatus.SENT, LocalDate.now());
```

It queries for two sets of candidates: invoices that are still `GENERATED` and invoices that have been `SENT`, where in both cases the due date is **before today** (i.e. past due). `findByStatusAndDueDateBefore` is a Spring Data derived query translating to `WHERE status = ? AND due_date < ?`.

```java
// L215-L219
        generatedOverdue.forEach(inv -> inv.setStatus(InvoiceStatus.OVERDUE));
        sentOverdue.forEach(inv -> inv.setStatus(InvoiceStatus.OVERDUE));

        invoiceRepository.saveAll(generatedOverdue);
        invoiceRepository.saveAll(sentOverdue);
    }
```

Both lists have every invoice's status set to `OVERDUE` (via `forEach`), then `saveAll` persists each batch. **Business rule:** only `GENERATED` and `SENT` invoices become `OVERDUE`; already `PAID`, `DISPUTED`, or `OVERDUE` invoices are untouched because they were never selected.

### sendInvoice — transition GENERATED → SENT

```java
// L222-L233
    @Override
    @Transactional
    public InvoiceResponse sendInvoice(Long invoiceId) {
        Invoice invoice = findById(invoiceId);

        if (invoice.getStatus() != InvoiceStatus.GENERATED) {
            throw new BillingException("Only GENERATED invoices can be sent. Current status: " + invoice.getStatus());
        }

        invoice.setStatus(InvoiceStatus.SENT);
        return toResponse(invoiceRepository.save(invoice));
    }
```

Marks an invoice as sent to the customer. **Business rule:** only invoices in the `GENERATED` status may be sent; otherwise a `BillingException` is thrown (the message includes the current status for clarity). On success the status becomes `SENT` and the invoice is saved and returned. Note this only flips the status flag — it does not itself email or deliver anything.

### downloadInvoicePdf — render a single-invoice PDF

```java
// L235-L237
    @Override
    public byte[] downloadInvoicePdf(Long invoiceId) {
        Invoice invoice = findById(invoiceId);
```

Returns the invoice rendered as a PDF, as a raw `byte[]` (the controller typically streams these bytes back with a `application/pdf` content type). It first loads the invoice (404 if missing). No `@Transactional` is needed since this is read-only.

```java
// L238-L240
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
```

A new in-memory PDF `document` is opened inside a **try-with-resources** block, which guarantees the document is closed (freeing native resources) when the block exits, even on exception. A single A4-sized `page` is created and added to the document.

```java
// L242-L244
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
```

A nested try-with-resources opens a `PDPageContentStream` — the object you draw text and graphics through. Two fonts are prepared: Helvetica-Bold for labels/titles and plain Helvetica for values.

```java
// L246-L248
                float margin = 50;
                float yStart = page.getMediaBox().getHeight() - margin;
                float leading = 18;
```

Layout constants. PDF coordinates have the origin at the **bottom-left**, so `yStart` is computed as the page height minus the top margin to start drawing near the top of the page. `leading` is the vertical gap between successive lines of text. `margin` is the left/top inset.

```java
// L250-L254
                cs.beginText();
                cs.setFont(bold, 18);
                cs.newLineAtOffset(margin, yStart);
                cs.showText("TeleConnect - Invoice");
                cs.endText();
```

Draws the title. `beginText()`/`endText()` bracket a text-drawing operation, `setFont(bold, 18)` selects the font and size, `newLineAtOffset(margin, yStart)` moves the text cursor to the start position, and `showText(...)` writes the string.

```java
// L256-L265
                float y = yStart - 30;
                writeRow(cs, bold, regular, margin, y, "Invoice ID", invoice.getInvoiceId().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Account ID", invoice.getAccountId().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Cycle ID", invoice.getCycleId().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Due Date", invoice.getDueDate().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Status", invoice.getStatus().toString());
```

`y` is moved 30 points below the title to begin the body. The `writeRow` helper (L476) prints a bold label and a regular-font value on the same line. Each `y -= leading` moves the cursor down one line before the next row. This block prints the invoice header fields: invoice ID, account ID, cycle ID, due date, and status. `.toString()` converts numbers/dates/enums to displayable strings.

```java
// L267-L272
                y -= 25;
                cs.beginText();
                cs.setFont(bold, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Charge Breakdown");
                cs.endText();
```

Adds extra vertical space and draws a bold "Charge Breakdown" section heading.

```java
// L274-L281
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Plan Charges", invoice.getPlanCharges().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Excess Charges", invoice.getExcessCharges().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Add-On Charges", invoice.getAddOnCharges().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Taxes", invoice.getTaxes().toString());
```

Prints each component charge as its own labeled row: plan, excess, add-on, and taxes.

```java
// L283-L286
                if (invoice.getLateFee() != null && invoice.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                    y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Late Fee", invoice.getLateFee().toString());
                }
```

The late-fee row is printed **only if** a positive late fee exists (non-null and greater than zero). This keeps the PDF clean for invoices without a late fee.

```java
// L288-L291
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Total Amount", invoice.getTotalAmount().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Amount Paid", invoice.getPaidAmount().toString());
```

Prints the grand total and the amount paid.

*Aside: `invoice.getPaidAmount().toString()` assumes `paidAmount` is non-null. If an unpaid invoice has a null `paidAmount`, this would throw a `NullPointerException`; it presumably defaults to zero in the entity.*

```java
// L293-L305
                y -= 25;
                cs.beginText();
                cs.setFont(regular, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText("Payment instructions: Please pay via UPI/NEFT/CARD before the due date to avoid late fees.");
                cs.endText();

                y -= leading;
                cs.beginText();
                cs.setFont(regular, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText("Generated: " + LocalDateTime.now());
                cs.endText();
            }
```

Two small (9-point) footer lines: a payment-instructions note and a "Generated: <timestamp>" line stamped with the current date-time. The closing brace ends the content-stream try-with-resources, flushing and closing the stream.

```java
// L308-L314
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BillingException("Failed to generate PDF for invoice: " + invoiceId);
        }
    }
```

The completed document is serialized into a `ByteArrayOutputStream` (an in-memory byte buffer) via `document.save(baos)`, and those bytes are returned. The outer try-with-resources then closes the document. If any PDFBox I/O fails, the `IOException` is caught and rethrown as a `BillingException` with a friendly message (so callers see a domain exception rather than a raw I/O error).

### downloadAccountStatementPdf — render a multi-page account statement

```java
// L316-L321
    @Override
    public byte[] downloadAccountStatementPdf(Long accountId) {
        List<Invoice> invoices = invoiceRepository.findByAccountId(accountId);
        if (invoices.isEmpty()) {
            throw new ResourceNotFoundException("No invoices found for account: " + accountId);
        }
```

Builds a complete account statement PDF (a cover/summary page plus one detail page per invoice) and returns it as bytes. It loads all of the account's invoices; if the account has none, it throws `ResourceNotFoundException` (404).

```java
// L323-L325
        try (PDDocument document = new PDDocument()) {
            PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
```

Opens the PDF document (try-with-resources) and prepares the bold and regular fonts that will be reused across every page.

```java
// L327-L329
            // ── Cover / Summary Page ────────────────────────────────────────────
            PDPage coverPage = new PDPage(PDRectangle.A4);
            document.addPage(coverPage);
```

Creates the first A4 page — the cover/summary page — and adds it to the document.

```java
// L331-L334
            BigDecimal grandTotal   = invoices.stream().map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal grandPaid    = invoices.stream().map(Invoice::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal grandLateFee = invoices.stream().map(Invoice::getLateFee).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal grandBalance = grandTotal.subtract(grandPaid);
```

Computes statement-wide aggregates using streams. Each line maps every invoice to one money field, then `reduce(BigDecimal.ZERO, BigDecimal::add)` sums them starting from zero. `grandTotal` is total billed, `grandPaid` is total paid, `grandLateFee` is the sum of late fees, and `grandBalance` is the outstanding balance (`grandTotal − grandPaid`).

*Aside: these reductions assume every invoice's `totalAmount`, `paidAmount`, and `lateFee` are non-null (a null would cause a `NullPointerException` inside `add`); they rely on entity defaults of zero.*

```java
// L336-L346
            try (PDPageContentStream cs = new PDPageContentStream(document, coverPage)) {
                float margin = 50;
                float y = coverPage.getMediaBox().getHeight() - margin;
                float leading = 20;

                // Title
                cs.beginText();
                cs.setFont(bold, 20);
                cs.newLineAtOffset(margin, y);
                cs.showText("TeleConnect - Account Statement");
                cs.endText();
```

Opens a content stream for the cover page, sets up the same layout constants (margin, top-anchored `y`, and a 20-point `leading`), and draws the large title.

```java
// L348-L354
                y -= 10;
                drawLine(cs, margin, y, coverPage.getMediaBox().getWidth() - margin, y);
                y -= 25;

                writeRow(cs, bold, regular, margin, y, "Account ID",       accountId.toString());           y -= leading;
                writeRow(cs, bold, regular, margin, y, "Generated On",     LocalDateTime.now().toString()); y -= leading;
                writeRow(cs, bold, regular, margin, y, "Total Invoices",   String.valueOf(invoices.size())); y -= leading;
```

Draws a horizontal rule (via the `drawLine` helper, L470) spanning from the left margin to the right margin, then prints three header rows: account ID, the generation timestamp, and the invoice count (`invoices.size()`). The `y -= leading` statements are placed on the same source line as each row for compactness.

```java
// L356-L365
                y -= 10;
                drawLine(cs, margin, y, coverPage.getMediaBox().getWidth() - margin, y);
                y -= 20;

                cs.beginText();
                cs.setFont(bold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("Summary");
                cs.endText();
                y -= leading;
```

Another horizontal rule, then a bold "Summary" section heading.

```java
// L367-L370
                writeRow(cs, bold, regular, margin, y, "Grand Total Billed",  grandTotal.setScale(2, RoundingMode.HALF_UP).toString());   y -= leading;
                writeRow(cs, bold, regular, margin, y, "Grand Total Paid",    grandPaid.setScale(2, RoundingMode.HALF_UP).toString());    y -= leading;
                writeRow(cs, bold, regular, margin, y, "Total Late Fees",     grandLateFee.setScale(2, RoundingMode.HALF_UP).toString()); y -= leading;
                writeRow(cs, bold, regular, margin, y, "Outstanding Balance", grandBalance.setScale(2, RoundingMode.HALF_UP).toString()); y -= leading;
```

Prints the four aggregate values, each rounded to two decimals for display: total billed, total paid, total late fees, and outstanding balance.

```java
// L372-L378
                y -= 20;
                cs.beginText();
                cs.setFont(bold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("Invoice List");
                cs.endText();
                y -= leading;
```

A bold "Invoice List" heading introduces the per-invoice table that follows.

```java
// L380-L389
                // Header row
                cs.beginText(); cs.setFont(bold, 10); cs.newLineAtOffset(margin,      y); cs.showText("Inv ID");      cs.endText();
                cs.beginText(); cs.setFont(bold, 10); cs.newLineAtOffset(margin + 60,  y); cs.showText("Cycle ID");    cs.endText();
                cs.beginText(); cs.setFont(bold, 10); cs.newLineAtOffset(margin + 120, y); cs.showText("Due Date");    cs.endText();
                cs.beginText(); cs.setFont(bold, 10); cs.newLineAtOffset(margin + 210, y); cs.showText("Total");       cs.endText();
                cs.beginText(); cs.setFont(bold, 10); cs.newLineAtOffset(margin + 290, y); cs.showText("Paid");        cs.endText();
                cs.beginText(); cs.setFont(bold, 10); cs.newLineAtOffset(margin + 360, y); cs.showText("Status");      cs.endText();
                y -= 5;
                drawLine(cs, margin, y, coverPage.getMediaBox().getWidth() - margin, y);
                y -= 15;
```

Draws the table header. Each column header is its own text block positioned at a fixed horizontal offset from the margin (`+60`, `+120`, `+210`, `+290`, `+360`), simulating columns. A horizontal rule is then drawn beneath the header row.

```java
// L391-L400
                for (Invoice inv : invoices) {
                    if (y < 60) break; // avoid overflow on cover page
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin,      y); cs.showText(inv.getInvoiceId().toString());                          cs.endText();
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin + 60,  y); cs.showText(inv.getCycleId().toString());                            cs.endText();
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin + 120, y); cs.showText(inv.getDueDate().toString());                             cs.endText();
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin + 210, y); cs.showText(inv.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toString()); cs.endText();
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin + 290, y); cs.showText(inv.getPaidAmount().setScale(2, RoundingMode.HALF_UP).toString());  cs.endText();
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin + 360, y); cs.showText(inv.getStatus().toString());                              cs.endText();
                    y -= 15;
                }
            }
```

Loops over every invoice and prints one table row per invoice (ID, cycle, due date, total, paid, status), aligned to the same column offsets as the header. The guard `if (y < 60) break;` stops drawing once the cursor nears the bottom of the page, preventing text from overflowing off the cover page. The closing brace ends the cover-page content stream.

*Aside: because of the `break`, if the account has more invoices than fit on the cover page, the summary table is truncated. The full detail still appears later (one page per invoice), so the truncation only affects the summary listing.*

```java
// L403-L411
            // ── One Detailed Page Per Invoice ───────────────────────────────────
            for (Invoice invoice : invoices) {
                PDPage detailPage = new PDPage(PDRectangle.A4);
                document.addPage(detailPage);

                try (PDPageContentStream cs = new PDPageContentStream(document, detailPage)) {
                    float margin = 50;
                    float y = detailPage.getMediaBox().getHeight() - margin;
                    float leading = 20;
```

Now a second loop creates **one detail page per invoice**. For each invoice it adds a new A4 page, opens its own content stream, and sets up the layout constants again.

```java
// L413-L420
                    cs.beginText();
                    cs.setFont(bold, 16);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Invoice #" + invoice.getInvoiceId() + " — Detail");
                    cs.endText();
                    y -= 10;
                    drawLine(cs, margin, y, detailPage.getMediaBox().getWidth() - margin, y);
                    y -= 25;
```

Draws a per-invoice title ("Invoice #<id> — Detail") followed by a horizontal rule.

```java
// L422-L426
                    writeRow(cs, bold, regular, margin, y, "Invoice ID",   invoice.getInvoiceId().toString()); y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Account ID",   invoice.getAccountId().toString()); y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Cycle ID",     invoice.getCycleId().toString());   y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Due Date",     invoice.getDueDate().toString());   y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Status",       invoice.getStatus().toString());    y -= leading;
```

Prints the invoice header fields (ID, account, cycle, due date, status) as labeled rows.

```java
// L428-L433
                    y -= 10;
                    drawLine(cs, margin, y, detailPage.getMediaBox().getWidth() - margin, y);
                    y -= 20;

                    cs.beginText(); cs.setFont(bold, 13); cs.newLineAtOffset(margin, y); cs.showText("Charge Breakdown"); cs.endText();
                    y -= leading;
```

A horizontal rule and a bold "Charge Breakdown" heading.

```java
// L435-L438
                    writeRow(cs, bold, regular, margin, y, "Plan Charges",   invoice.getPlanCharges().setScale(2, RoundingMode.HALF_UP).toString());   y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Excess Charges", invoice.getExcessCharges().setScale(2, RoundingMode.HALF_UP).toString()); y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Add-On Charges", invoice.getAddOnCharges().setScale(2, RoundingMode.HALF_UP).toString());  y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Taxes",          invoice.getTaxes().setScale(2, RoundingMode.HALF_UP).toString());          y -= leading;
```

Prints each component charge, rounded to two decimals: plan, excess, add-on, and taxes.

```java
// L440-L442
                    if (invoice.getLateFee() != null && invoice.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                        writeRow(cs, bold, regular, margin, y, "Late Fee", invoice.getLateFee().setScale(2, RoundingMode.HALF_UP).toString()); y -= leading;
                    }
```

Prints a late-fee row only when a positive late fee exists, same conditional logic as the single-invoice PDF.

```java
// L444-L452
                    y -= 5;
                    drawLine(cs, margin, y, detailPage.getMediaBox().getWidth() - margin, y);
                    y -= 20;

                    writeRow(cs, bold, regular, margin, y, "Total Amount",   invoice.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toString()); y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Amount Paid",    invoice.getPaidAmount().setScale(2, RoundingMode.HALF_UP).toString());  y -= leading;

                    BigDecimal balance = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
                    writeRow(cs, bold, regular, margin, y, "Balance Due",    balance.setScale(2, RoundingMode.HALF_UP).toString()); y -= leading;
```

After a divider line, prints the totals section: total amount, amount paid, and a computed **balance due** (`totalAmount − paidAmount`), each rounded to two decimals. Unlike the single-invoice PDF, the per-invoice detail page additionally shows the remaining balance.

```java
// L454-L459
                    y -= 20;
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin, y);
                    cs.showText("Generated: " + LocalDateTime.now());
                    cs.endText();
                }
            }
```

Adds a small "Generated: <timestamp>" footer to each detail page, then the content stream and the per-invoice loop iteration close.

```java
// L461-L468
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new BillingException("Failed to generate account statement PDF for account: " + accountId);
        }
    }
```

Once the cover page and all detail pages are drawn, the whole document is saved to an in-memory byte buffer and the bytes are returned. Any PDFBox `IOException` is translated into a `BillingException` with an account-specific message.

### drawLine — private PDF helper

```java
// L470-L474
    private void drawLine(PDPageContentStream cs, float x1, float y, float x2, float y2) throws IOException {
        cs.moveTo(x1, y);
        cs.lineTo(x2, y2);
        cs.stroke();
    }
```

A small private helper that draws a straight line on the content stream. `moveTo` sets the start point `(x1, y)`, `lineTo` defines the end point `(x2, y2)`, and `stroke()` actually paints the line. It declares `throws IOException` because the PDFBox drawing calls can fail. Every call site passes the same value for `y` and `y2`, so in practice it always draws a horizontal rule.

### writeRow — private label/value row helper

```java
// L476-L488
    private void writeRow(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
                          float x, float y, String label, String value) throws IOException {
        cs.beginText();
        cs.setFont(bold, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(label + ": ");
        cs.endText();
        cs.beginText();
        cs.setFont(regular, 11);
        cs.newLineAtOffset(x + 130, y);
        cs.showText(value);
        cs.endText();
    }
```

The workhorse used throughout both PDF methods. It draws a two-part row on a single baseline `y`: the `label` (with a trailing ": ") in 11-point **bold** starting at horizontal position `x`, and the `value` in 11-point **regular** starting at `x + 130` (a fixed 130-point column offset so all values line up). Each part is its own `beginText`/`endText` block because the font and horizontal offset differ. It declares `throws IOException` for the same reason as `drawLine`.

### findById — private entity lookup

```java
// L490-L494
    private Invoice findById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with ID: " + invoiceId));
    }
```

A private helper that centralizes invoice loading. It calls the repository's `findById` (returns `Optional<Invoice>`) and either returns the entity or throws `ResourceNotFoundException` when absent. Nearly every public method calls this, so the 404-style "not found" behavior is defined in exactly one place.

### toResponse — private entity → DTO mapper

```java
// L496-L511
    private InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .accountId(invoice.getAccountId())
                .cycleId(invoice.getCycleId())
                .planCharges(invoice.getPlanCharges())
                .excessCharges(invoice.getExcessCharges())
                .addOnCharges(invoice.getAddOnCharges())
                .taxes(invoice.getTaxes())
                .totalAmount(invoice.getTotalAmount())
                .paidAmount(invoice.getPaidAmount())
                .lateFee(invoice.getLateFee())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .build();
    }
```

The mapping helper that converts an `Invoice` **entity** into an `InvoiceResponse` **DTO** using the response builder (Lombok `@Builder`). It copies every field one-to-one. This keeps the JPA entity out of the API/HTTP layer: callers receive only the response DTO's shape, which can evolve independently of the database model. Every read and write method funnels its result through this method.

```java
// L512
}
```

Closes the class.

## How this connects

- **Called by the Controller layer:** a REST controller (e.g. `InvoiceController`) receives HTTP requests, binds the JSON body to the request DTOs (`InvoiceGenerationRequest`, `PaymentRequest`, `LateFeeRequest`, `LateFeeWaiverRequest`) — typically validated with `@Valid` — and invokes these service methods. The returned `InvoiceResponse` (or `byte[]` for PDFs) is wrapped in a `ResponseEntity` and sent back to the client.
- **Calls into the Repository layer:** `InvoiceRepository`, `BillingCycleRepository`, and `PaymentRepository` are Spring Data JPA interfaces that translate method calls (`findById`, `findByAccountIdAndStatus`, `save`, `saveAll`, etc.) into SQL against the database. The `@Transactional` boundaries here ensure multi-table writes (invoice + cycle, or payment + invoice) commit atomically.
- **Operates on the Entity layer:** `Invoice`, `BillingCycle`, and `Payment` are JPA entities mapped to tables; their `@Builder`-style construction and getters/setters (Lombok) are used throughout.
- **Enums and exceptions:** invoice lifecycle is governed by `InvoiceStatus` (GENERATED → SENT → OVERDUE → PAID, plus DISPUTED), with `BillingException` signaling business-rule violations (mapped to 4xx) and `ResourceNotFoundException` signaling missing records (mapped to 404), usually by a global `@ControllerAdvice` exception handler.
- **Scheduling:** `markOverdueInvoices` runs as a daily batch via `@Scheduled`, independent of any HTTP request, relying on scheduling being enabled application-wide.
