# Request DTOs (Incoming API Payloads + Validation)

This group of classes are the **Request Data Transfer Objects (DTOs)** for the billing microservice. A DTO is a plain Java object whose only job is to carry data across a boundary — here, the boundary between the outside HTTP world and the application. When a client sends a JSON request body to one of the REST controllers, Spring's `@RequestBody` mechanism (using the Jackson JSON library) deserializes that JSON into one of these objects, and Bean Validation (Jakarta Validation, triggered by `@Valid` on the controller method parameter) checks the annotated constraints before any business logic runs. In the standard **Controller → Service → Repository → Entity/DB** layering, these DTOs sit at the very front: the **Controller** receives and validates them, then hands their data to the **Service** layer, which applies business rules and ultimately persists **Entity** objects through the **Repository**. Keeping request DTOs separate from entities means the public API shape can differ from the database shape and untrusted client input never directly touches persistence objects.

A note on a recurring pattern below: most of these classes are written as **classic POJOs** (Plain Old Java Objects) with hand-written constructors, getters, and setters rather than using Lombok. The Jackson deserializer and Spring's data binding use the no-argument constructor plus the setters (or the getters/field access) to populate an instance from JSON. The validation annotations decorate the fields and are evaluated by the validation provider, not by Jackson.

---

## src/main/java/com/teleconnect/billing_service/dto/request/BillingCycleRequest.java

Role: Binds and validates the JSON body used to create a billing cycle for an account, carrying the account id and the start/end dates of the cycle.

```java
// L1
package com.teleconnect.billing_service.dto.request;
```
The `package` statement declares the fully-qualified namespace this class lives in. Everything under `...dto.request` is, by convention, an inbound request DTO. The package path also mirrors the folder structure on disk, which is how the Java compiler and the classloader locate the class.

```java
// L3-L6
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
```
The imports pull in three notable types. `com.fasterxml.jackson.annotation.JsonFormat` comes from the Jackson library and lets you control exactly how a field is serialized to / deserialized from JSON (here, the date string format). `jakarta.validation.constraints.NotNull` is a Jakarta Bean Validation constraint annotation — when validation is run, a field annotated with it must not be `null`. `java.time.LocalDate` is the modern Java date type representing a calendar date (year-month-day) with no time-of-day or time zone.

```java
// L8
public class BillingCycleRequest {
```
Declares the public class. It is a plain class (not a Spring `@Component`, not a JPA `@Entity`) — it has no framework stereotype because a DTO is just a data holder that Spring instantiates per request during request binding.

```java
// L10-L11
    @NotNull(message = "Account ID is required")
    private Long accountId;
```
The `accountId` field is the database identifier of the billing account the cycle belongs to. Its type is `Long` (the boxed wrapper, so it can be `null` if the client omits it). `@NotNull(message = "...")` declares that during validation `accountId` must be present (non-null); if it is null, the validation failure carries the human-readable message "Account ID is required" (typically surfaced back to the client by a global exception handler).

```java
// L13-L15
    @NotNull(message = "Cycle start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate cycleStart;
```
`cycleStart` is the first day of the billing cycle, typed as `LocalDate`. `@NotNull` makes it mandatory. `@JsonFormat(pattern = "yyyy-MM-dd")` tells Jackson that the incoming JSON value will be (and the outgoing value should be) a string like `2026-06-15`, parsed with that exact pattern — this avoids ambiguity in how dates are read from the request body.

```java
// L17-L19
    @NotNull(message = "Cycle end date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate cycleEnd;
```
`cycleEnd` is the last day of the billing cycle, with the same mandatory-and-formatted treatment as `cycleStart`. *Aside: the class does not enforce that `cycleEnd` is after `cycleStart`; that ordering rule (if any) must be applied in the service layer, not here.*

```java
// L21
    public BillingCycleRequest() {}
```
The explicit no-argument (default) constructor. Jackson and Spring's binding need a way to create an empty instance before populating its fields via setters, so providing this is what makes JSON deserialization work.

```java
// L23-L27
    public BillingCycleRequest(Long accountId, LocalDate cycleStart, LocalDate cycleEnd) {
        this.accountId = accountId;
        this.cycleStart = cycleStart;
        this.cycleEnd = cycleEnd;
    }
```
An all-arguments constructor that assigns each parameter to its matching field. This is a convenience for programmatically building an instance (for example in unit tests or when constructing a request in code) and is not required by Jackson.

```java
// L29-L36
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public LocalDate getCycleStart() { return cycleStart; }
    public void setCycleStart(LocalDate cycleStart) { this.cycleStart = cycleStart; }

    public LocalDate getCycleEnd() { return cycleEnd; }
    public void setCycleEnd(LocalDate cycleEnd) { this.cycleEnd = cycleEnd; }
}
```
Standard JavaBean getters and setters for all three fields. Jackson uses the setters (or getters) during deserialization to populate the object from JSON, and the controller/service code uses the getters to read the values out. The closing brace ends the class.

---

## src/main/java/com/teleconnect/billing_service/dto/request/CycleGenerationRequest.java

Role: Binds the JSON body for triggering a bulk billing-cycle generation run on a given date, with an optional "dry run" flag to preview the run without committing changes.

```java
// L1
package com.teleconnect.billing_service.dto.request;
```
Same request-DTO package as above.

```java
// L3-L5
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
```
Imports only `@NotNull` (Jakarta validation) and `LocalDate`. Note there is no `@JsonFormat` here, so the date field below relies on Jackson's default ISO-8601 date parsing (which also accepts `yyyy-MM-dd`).

```java
// L7
public class CycleGenerationRequest {
```
The plain DTO class declaration.

```java
// L9-L10
    @NotNull(message = "Cycle date is required")
    private LocalDate cycleDate;
```
`cycleDate` is the date the generation run should produce cycles for. It is mandatory (`@NotNull`).

```java
// L12
    private boolean dryRun = false;
```
`dryRun` is a primitive `boolean` initialized to `false`. When `true`, the caller is asking the service to simulate the generation (compute what would happen) without persisting results. Because it is a primitive with a default, omitting it from the JSON leaves it `false`; it carries no validation annotation since a primitive `boolean` can never be null.

```java
// L14
    public CycleGenerationRequest() {}
```
No-argument constructor for Jackson/Spring binding.

```java
// L16-L19
    public CycleGenerationRequest(LocalDate cycleDate, boolean dryRun) {
        this.cycleDate = cycleDate;
        this.dryRun = dryRun;
    }
```
All-arguments constructor assigning both fields, useful for tests or programmatic creation.

```java
// L21-L25
    public LocalDate getCycleDate() { return cycleDate; }
    public void setCycleDate(LocalDate cycleDate) { this.cycleDate = cycleDate; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }
}
```
Getters and setters. Note the boolean getter follows the JavaBean convention of `isDryRun()` (the `is` prefix) rather than `getDryRun()`; Jackson and Spring recognize both forms for boolean properties. The closing brace ends the class.

---

## src/main/java/com/teleconnect/billing_service/dto/request/DisputeRequest.java

Role: Binds and validates the JSON body a customer (or agent) submits to raise a billing dispute against an invoice, capturing the invoice, an optional subscriber, the reason, the disputed amount, and an optional description.

```java
// L1
package com.teleconnect.billing_service.dto.request;
```
Request-DTO package.

```java
// L3-L7
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
```
Three Jakarta validation constraints are imported here. `@DecimalMin` asserts a numeric value is greater than (or equal to) a configured minimum. `@NotBlank` asserts a `String` is non-null and contains at least one non-whitespace character (stronger than `@NotNull`, which only checks non-null). `@NotNull` asserts non-null. `java.math.BigDecimal` is the arbitrary-precision decimal type used for money — it avoids the rounding errors of `double`/`float`, which is essential in billing.

```java
// L9
public class DisputeRequest {
```
The plain DTO class.

```java
// L11-L12
    @NotNull(message = "Invoice ID is required")
    private Long invoiceId;
```
`invoiceId` (a `Long`) identifies the invoice being disputed and is mandatory.

```java
// L14
    private Long subscriberId;
```
`subscriberId` (a `Long`) is the optional id of the subscriber raising the dispute. It has no validation annotation, so it may be omitted/`null`.

```java
// L16-L17
    @NotBlank(message = "Dispute reason is required")
    private String disputeReason;
```
`disputeReason` is a required text field; `@NotBlank` ensures the client sends a meaningful, non-empty, non-whitespace reason rather than an empty string.

```java
// L19-L21
    @NotNull(message = "Disputed amount is required")
    @DecimalMin(value = "0.01", message = "Disputed amount must be greater than 0")
    private BigDecimal disputedAmount;
```
`disputedAmount` is the monetary amount in dispute, typed as `BigDecimal`. It must be present (`@NotNull`) and, via `@DecimalMin(value = "0.01")`, must be at least `0.01` — i.e. strictly positive in practical money terms. The minimum is given as a string so the exact decimal value is preserved without floating-point conversion.

```java
// L23
    private String description;
```
`description` is an optional free-text elaboration on the dispute; unvalidated, so it may be `null`.

```java
// L25
    public DisputeRequest() {}
```
No-argument constructor for binding. *Aside: unlike most siblings, this class provides no all-arguments constructor — only the default one plus getters/setters.*

```java
// L27-L40
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public Long getSubscriberId() { return subscriberId; }
    public void setSubscriberId(Long subscriberId) { this.subscriberId = subscriberId; }

    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }

    public BigDecimal getDisputedAmount() { return disputedAmount; }
    public void setDisputedAmount(BigDecimal disputedAmount) { this.disputedAmount = disputedAmount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
```
Getters and setters for all five fields, used by Jackson for binding and by the controller/service for reading the values. The closing brace ends the class.

---

## src/main/java/com/teleconnect/billing_service/dto/request/DisputeResolveRequest.java

Role: Binds and validates the JSON body an agent submits to finalize/resolve an open dispute, capturing the resolution outcome, an optional credit amount to issue, and optional notes.

```java
// L1
package com.teleconnect.billing_service.dto.request;
```
Request-DTO package.

```java
// L3-L5
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
```
Imports `@NotBlank` (required non-blank string) and `BigDecimal` (for the credit amount money value).

```java
// L7
public class DisputeResolveRequest {
```
The plain DTO class.

```java
// L9-L10
    @NotBlank(message = "Resolution is required")
    private String resolution;
```
`resolution` is the required outcome of the dispute (for example a status or decision label). `@NotBlank` guarantees a non-empty value.

```java
// L12
    private BigDecimal creditAmount;
```
`creditAmount` is the optional amount of money to credit back to the customer as part of the resolution. It is unvalidated and may be `null` (i.e. a resolution that grants no credit). *Aside: there is no lower-bound constraint here, so a zero or negative credit would pass validation; any such rule would have to live in the service.*

```java
// L14
    private String resolutionNotes;
```
`resolutionNotes` is optional free-text explaining the resolution.

```java
// L16
    public DisputeResolveRequest() {}
```
No-argument constructor for binding.

```java
// L18-L22
    public DisputeResolveRequest(String resolution, BigDecimal creditAmount, String resolutionNotes) {
        this.resolution = resolution;
        this.creditAmount = creditAmount;
        this.resolutionNotes = resolutionNotes;
    }
```
All-arguments constructor assigning all three fields.

```java
// L24-L31
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public BigDecimal getCreditAmount() { return creditAmount; }
    public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}
```
Getters and setters for all three fields. The closing brace ends the class.

---

## src/main/java/com/teleconnect/billing_service/dto/request/DisputeReviewRequest.java

Role: Binds and validates the JSON body used to move a dispute into review by assigning it to a person/team, with optional notes.

```java
// L1
package com.teleconnect.billing_service.dto.request;
```
Request-DTO package.

```java
// L3
import jakarta.validation.constraints.NotBlank;
```
Imports just `@NotBlank`. There are no numeric or date fields, so no `BigDecimal`/`LocalDate`/`@JsonFormat` are needed.

```java
// L5
public class DisputeReviewRequest {
```
The plain DTO class.

```java
// L7-L8
    @NotBlank(message = "Assigned to is required")
    private String assignedTo;
```
`assignedTo` identifies the agent or queue the dispute is being assigned to for review; it is required and must be non-blank.

```java
// L10
    private String notes;
```
`notes` is an optional free-text comment about the assignment/review.

```java
// L12
    public DisputeReviewRequest() {}
```
No-argument constructor for binding.

```java
// L14-L17
    public DisputeReviewRequest(String assignedTo, String notes) {
        this.assignedTo = assignedTo;
        this.notes = notes;
    }
```
All-arguments constructor assigning both fields.

```java
// L19-L23
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
```
Getters and setters for the two fields. The closing brace ends the class.

---

## src/main/java/com/teleconnect/billing_service/dto/request/InvoiceGenerationRequest.java

Role: Binds and validates the JSON body used to generate an invoice for a specific account and billing cycle, carrying the four monetary line-item categories (plan, excess/overage, add-on, and tax charges).

```java
// L1
package com.teleconnect.billing_service.dto.request;
```
Request-DTO package.

```java
// L3-L6
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
```
Imports `@DecimalMin` and `@NotNull` (validation) plus `BigDecimal` for the money fields.

```java
// L8
public class InvoiceGenerationRequest {
```
The plain DTO class.

```java
// L10-L14
    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Cycle ID is required")
    private Long cycleId;
```
`accountId` and `cycleId` (both `Long`) identify which account and which billing cycle the invoice is for. Both are mandatory (`@NotNull`).

```java
// L16-L18
    @NotNull(message = "Plan charges are required")
    @DecimalMin(value = "0.0", message = "Plan charges must be 0 or more")
    private BigDecimal planCharges;
```
`planCharges` is the base subscription/plan portion of the invoice. It is mandatory and, via `@DecimalMin(value = "0.0")`, must be zero or greater (charges of exactly `0.0` are allowed, unlike the dispute/payment amounts which require `0.01`).

```java
// L20-L22
    @NotNull(message = "Excess charges are required")
    @DecimalMin(value = "0.0", message = "Excess charges must be 0 or more")
    private BigDecimal excessCharges;
```
`excessCharges` is the overage portion (usage beyond the plan allowance). Same constraints: required and `>= 0.0`.

```java
// L24-L26
    @NotNull(message = "Add-on charges are required")
    @DecimalMin(value = "0.0", message = "Add-on charges must be 0 or more")
    private BigDecimal addOnCharges;
```
`addOnCharges` is the total for optional add-on services. Required and `>= 0.0`.

```java
// L28-L30
    @NotNull(message = "Taxes are required")
    @DecimalMin(value = "0.0", message = "Taxes must be 0 or more")
    private BigDecimal taxes;
```
`taxes` is the tax portion of the invoice. Required and `>= 0.0`. *Aside: the request does not include a grand total field — the service is expected to compute the invoice total by summing these four components.*

```java
// L32
    public InvoiceGenerationRequest() {}
```
No-argument constructor for binding.

```java
// L34-L42
    public InvoiceGenerationRequest(Long accountId, Long cycleId, BigDecimal planCharges,
                                    BigDecimal excessCharges, BigDecimal addOnCharges, BigDecimal taxes) {
        this.accountId = accountId;
        this.cycleId = cycleId;
        this.planCharges = planCharges;
        this.excessCharges = excessCharges;
        this.addOnCharges = addOnCharges;
        this.taxes = taxes;
    }
```
All-arguments constructor assigning each of the six fields, useful for tests and programmatic construction.

```java
// L44-L60
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public BigDecimal getPlanCharges() { return planCharges; }
    public void setPlanCharges(BigDecimal planCharges) { this.planCharges = planCharges; }

    public BigDecimal getExcessCharges() { return excessCharges; }
    public void setExcessCharges(BigDecimal excessCharges) { this.excessCharges = excessCharges; }

    public BigDecimal getAddOnCharges() { return addOnCharges; }
    public void setAddOnCharges(BigDecimal addOnCharges) { this.addOnCharges = addOnCharges; }

    public BigDecimal getTaxes() { return taxes; }
    public void setTaxes(BigDecimal taxes) { this.taxes = taxes; }
}
```
Getters and setters for all six fields, used by Jackson for binding and by the controller/service to read the values. The closing brace ends the class.

---

## src/main/java/com/teleconnect/billing_service/dto/request/LateFeeRequest.java

Role: Binds and validates the JSON body used to apply a late fee to an account/invoice, carrying the fee amount and an optional reason.

```java
// L1
package com.teleconnect.billing_service.dto.request;
```
Request-DTO package.

```java
// L3-L6
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
```
Imports `@DecimalMin`, `@NotNull`, and `BigDecimal`.

```java
// L8
public class LateFeeRequest {
```
The plain DTO class.

```java
// L10-L12
    @NotNull(message = "Fee amount is required")
    @DecimalMin(value = "0.01", message = "Fee amount must be greater than 0")
    private BigDecimal feeAmount;
```
`feeAmount` is the monetary value of the late fee. It is required (`@NotNull`) and must be at least `0.01` (`@DecimalMin`), i.e. a strictly positive charge.

```java
// L14
    private String reason;
```
`reason` is an optional free-text justification for the fee; unvalidated.

```java
// L16
    public LateFeeRequest() {}
```
No-argument constructor for binding.

```java
// L18-L21
    public LateFeeRequest(BigDecimal feeAmount, String reason) {
        this.feeAmount = feeAmount;
        this.reason = reason;
    }
```
All-arguments constructor assigning both fields.

```java
// L23-L27
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
```
Getters and setters for the two fields. The closing brace ends the class.

---

## src/main/java/com/teleconnect/billing_service/dto/request/LateFeeWaiverRequest.java

Role: Binds and validates the JSON body used to waive a previously applied late fee, requiring both a reason and the name of the authorizing party.

```java
// L1
package com.teleconnect.billing_service.dto.request;
```
Request-DTO package.

```java
// L3
import jakarta.validation.constraints.NotBlank;
```
Imports only `@NotBlank`, since both fields are required strings.

```java
// L5
public class LateFeeWaiverRequest {
```
The plain DTO class.

```java
// L7-L8
    @NotBlank(message = "Waiver reason is required")
    private String waiverReason;
```
`waiverReason` is the required explanation for why the late fee is being waived; must be non-blank.

```java
// L10-L11
    @NotBlank(message = "Authorised by is required")
    private String authorisedBy;
```
`authorisedBy` records who authorized the waiver (an accountability/audit field). Required and non-blank. (Note the British spelling "Authorised".)

```java
// L13
    public LateFeeWaiverRequest() {}
```
No-argument constructor for binding.

```java
// L15-L18
    public LateFeeWaiverRequest(String waiverReason, String authorisedBy) {
        this.waiverReason = waiverReason;
        this.authorisedBy = authorisedBy;
    }
```
All-arguments constructor assigning both fields.

```java
// L20-L24
    public String getWaiverReason() { return waiverReason; }
    public void setWaiverReason(String waiverReason) { this.waiverReason = waiverReason; }

    public String getAuthorisedBy() { return authorisedBy; }
    public void setAuthorisedBy(String authorisedBy) { this.authorisedBy = authorisedBy; }
}
```
Getters and setters for the two fields. The closing brace ends the class.

---

## src/main/java/com/teleconnect/billing_service/dto/request/PaymentRequest.java

Role: Binds and validates the JSON body for recording a payment against an invoice, carrying the (optional) invoice id, the amount paid, the payment method, and an optional external transaction reference.

```java
// L1
package com.teleconnect.billing_service.dto.request;
```
Request-DTO package.

```java
// L3-L7
import com.teleconnect.billing_service.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
```
This DTO additionally imports the project's own `PaymentMethod` enum from the `enums` package. An `enum` is a fixed, type-safe set of named constants; using it here means Jackson will only accept JSON values that match one of the enum's names (otherwise deserialization fails). For reference, `PaymentMethod` defines: `UPI`, `CARD`, `NETBANKING`, `WALLET`, `BANK_TRANSFER`, and `CASH`. The remaining imports are `@DecimalMin`, `@NotNull`, and `BigDecimal`.

```java
// L9
public class PaymentRequest {
```
The plain DTO class.

```java
// L11
    private Long invoiceId;
```
`invoiceId` (a `Long`) is the invoice the payment is being applied to. *Aside: it carries no `@NotNull`, so it is optional at the validation layer even though logically a payment usually targets an invoice — the invoice may instead be supplied via the URL path in the controller, with the service deciding how to associate the payment.*

```java
// L13-L15
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    private BigDecimal amountPaid;
```
`amountPaid` is the monetary amount of the payment. It is mandatory (`@NotNull`) and must be at least `0.01` (`@DecimalMin`), i.e. strictly positive.

```java
// L17-L18
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
```
`paymentMethod` is the channel used to pay, typed as the `PaymentMethod` enum. It is required (`@NotNull`). Because it is an enum, the value is also implicitly constrained to one of the six allowed constants by Jackson during deserialization.

```java
// L20
    private String transactionRef;
```
`transactionRef` is an optional external reference for the transaction (for example a gateway/UPI transaction id); unvalidated.

```java
// L22
    public PaymentRequest() {}
```
No-argument constructor for binding.

```java
// L24-L29
    public PaymentRequest(Long invoiceId, BigDecimal amountPaid, PaymentMethod paymentMethod, String transactionRef) {
        this.invoiceId = invoiceId;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.transactionRef = transactionRef;
    }
```
All-arguments constructor assigning all four fields.

```java
// L31-L41
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
}
```
Getters and setters for all four fields, used by Jackson for binding and by the controller/service to read the values. The closing brace ends the class.

---

## How this connects

These request DTOs are the **entry point** of the request flow. A REST **Controller** method declares one of them as a `@RequestBody @Valid` parameter; Spring + Jackson deserialize the incoming JSON into the DTO using its no-arg constructor and setters, then Bean Validation evaluates the `@NotNull`, `@NotBlank`, and `@DecimalMin` constraints. If any constraint fails, a `MethodArgumentNotValidException` is raised before the controller body runs and is normally translated into a `400 Bad Request` by a global exception handler (typically a `@RestControllerAdvice` class), with the `message` text from each annotation surfaced to the client.

If validation passes, the controller reads the values via the getters and passes them to the **Service** layer, which applies cross-field and business rules these DTOs deliberately do not enforce (date ordering, sufficient balances, dispute state transitions, summing invoice line items, etc.). The service then constructs and persists **Entity** objects via the **Repository** layer. Keeping these DTOs distinct from the JPA entities means client input is validated and shaped at the boundary and never binds directly to database rows. The `PaymentRequest` is the only DTO here that references a domain `enum` (`PaymentMethod`), tying the request vocabulary to the same constant set used deeper in the application.
