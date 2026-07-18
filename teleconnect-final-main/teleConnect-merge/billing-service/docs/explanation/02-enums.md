# Enums (Domain Status & Method Types)

This part documents the five Java `enum` types in the `com.teleconnect.billing_service.enums` package. An `enum` (enumeration) is a special Java class whose instances are a fixed, compile-time set of named constants; you cannot create new ones at runtime. These enums define the closed value sets used for *statuses* (billing-cycle, dispute, invoice, payment) and one *method type* (payment method). They are referenced throughout every layer of the Controller → Service → Repository → Entity/DB stack: controllers bind incoming `@RequestParam`/`@PathVariable` values to them, service implementations branch on them to enforce business rules, repositories use them as derived-query parameters, and entities persist them as columns via JPA's `@Enumerated(EnumType.STRING)` (so the database stores the literal constant name, e.g. the text `"OPEN"`). Centralizing these values in one place guarantees that every layer agrees on the exact same vocabulary and prevents typos that a free-form `String` status field would allow.

> **Plain-language primer (applies to all five files):** A Java `enum` declared as `public enum X { A, B, C }` compiles to a `final` class extending `java.lang.Enum`, with `A`, `B`, `C` as `public static final` singleton instances. The compiler also auto-generates two useful static methods: `X.values()` (returns an array of all constants in declaration order) and `X.valueOf("A")` (returns the constant whose name exactly matches the string, throwing `IllegalArgumentException` if none match). Spring MVC uses `valueOf`-style conversion to bind request strings to enum parameters, which is why an unknown status string in a request results in a conversion failure rather than a silently-accepted value. None of these five enums declare constructors, fields, or methods — they are *pure marker constant sets*.

---

## src/main/java/com/teleconnect/billing_service/enums/BillingCycleStatus.java

Defines the lifecycle states a billing cycle can be in. A billing cycle groups an account's charges for a period; its status tracks how far that period has progressed through generation and closure.

```java
// L1
package com.teleconnect.billing_service.enums;
```

The `package` declaration places this type in the `com.teleconnect.billing_service.enums` namespace. This must mirror the folder path (`.../com/teleconnect/billing_service/enums/`) so the Java compiler and the JVM can locate the class. Every other file that wants this enum must `import com.teleconnect.billing_service.enums.BillingCycleStatus;` (as the controllers, services, repositories, entities, and DTOs all do).

```java
// L2-L5
                                                                            (blank line)
public enum BillingCycleStatus {
    OPEN, GENERATED, CLOSED
}
```

`public` makes the type visible to all other packages (necessary, since it is used in the `controller`, `service`, `repository`, `entity`, and `dto` packages). `enum BillingCycleStatus` declares the enumeration. It has exactly three constants, in this declaration order:

- **`OPEN`** — the cycle is currently accumulating charges; no invoice has been produced yet. In `BillingCycleServiceImpl`, a newly created cycle is built with `.status(BillingCycleStatus.OPEN)`, and the duplicate-prevention check `findByAccountIdAndStatus(accountId, BillingCycleStatus.OPEN)` rejects creating a second OPEN cycle for the same account. Invoice generation selects `findByStatusAndCycleEndLessThanEqual(BillingCycleStatus.OPEN, ...)`, i.e. only OPEN cycles whose end date has passed.
- **`GENERATED`** — invoices have been produced from this cycle. The service sets `cycle.setStatus(BillingCycleStatus.GENERATED)` after generating invoices. Note that `updateCycleStatus` explicitly *forbids* a caller from setting GENERATED manually (`if (status == BillingCycleStatus.GENERATED)` throws), because GENERATED must only result from the generation process.
- **`CLOSED`** — the cycle is finalized and immutable. `updateCycleStatus` blocks re-closing an already `CLOSED` cycle and is the only path that sets `BillingCycleStatus.CLOSED`.

The closing brace `}` ends the enum body. The trailing blank line (L5/L6) is insignificant.

*Aside: the constants carry no associated data or methods — the ordering above is also the `ordinal()` order (OPEN=0, GENERATED=1, CLOSED=2), but because the entity persists with `EnumType.STRING` the database stores the names, not the ordinals, so reordering would not corrupt existing data.*

---

## src/main/java/com/teleconnect/billing_service/enums/DisputeStatus.java

Defines the lifecycle states of a billing dispute (a customer's challenge against an invoice). Drives the dispute workflow and the guard rules in `BillingDisputeServiceImpl`.

```java
// L1-L2
package com.teleconnect.billing_service.enums;
                                                                            (blank line)
```

Same package declaration as above, followed by a blank separator line.

```java
// L3-L5
public enum DisputeStatus {
    OPEN, UNDER_REVIEW, RESOLVED, REJECTED
}
```

`public enum DisputeStatus` declares four constants representing the dispute's progression:

- **`OPEN`** — a dispute has just been raised. `BillingDisputeServiceImpl` builds new disputes with `.status(DisputeStatus.OPEN)` and simultaneously flips the related invoice to `InvoiceStatus.DISPUTED`.
- **`UNDER_REVIEW`** — an agent is actively investigating. (Note the underscore: `valueOf("UNDER_REVIEW")` must include it; a request sending `"UNDER REVIEW"` or `"under_review"` would fail binding because the match is case- and character-exact.)
- **`RESOLVED`** — the dispute was decided in a way that completes it (terminal state).
- **`REJECTED`** — the dispute was denied (terminal state).

`RESOLVED` and `REJECTED` are treated as **terminal** by the service: `updateDisputeStatus` throws if the current status is already `RESOLVED || REJECTED` (you cannot reopen a closed dispute), and applies resolution side-effects when the *target* status is `RESOLVED || REJECTED`. The closing brace `}` ends the type.

---

## src/main/java/com/teleconnect/billing_service/enums/InvoiceStatus.java

Defines the lifecycle states of an invoice. This is the most widely consulted status enum: it appears in invoice queries, dispute creation guards, billing-cycle generation, and reporting sums.

```java
// L1-L2
package com.teleconnect.billing_service.enums;
                                                                            (blank line)
```

Package declaration and blank separator, identical pattern to the others.

```java
// L3-L5
public enum InvoiceStatus {
    GENERATED, SENT, PAID, OVERDUE, DISPUTED
}
```

`public enum InvoiceStatus` declares five constants:

- **`GENERATED`** — the invoice has been created but not yet delivered. Set during billing-cycle generation via `.status(InvoiceStatus.GENERATED)` in `BillingCycleServiceImpl`.
- **`SENT`** — the invoice has been delivered to the customer.
- **`PAID`** — payment has settled the invoice. Used as a guard in `BillingDisputeServiceImpl` (`if (invoice.getStatus() == InvoiceStatus.PAID)` blocks disputing a paid invoice) and in `InvoiceRepository.sumPaidAmountByStatusAndDueDateBetween(InvoiceStatus, ...)` for revenue reporting.
- **`OVERDUE`** — payment is past its due date. Repositories such as `findByStatusAndDueDateBefore(InvoiceStatus, LocalDate)` exist to support overdue queries.
- **`DISPUTED`** — the invoice is the subject of an open dispute. `BillingDisputeServiceImpl` sets `invoice.setStatus(InvoiceStatus.DISPUTED)` when a dispute is raised and blocks raising a *second* dispute via `if (invoice.getStatus() == InvoiceStatus.DISPUTED)`.

The closing brace ends the enum. As with the others, the entity `Invoice` persists this with `@Enumerated(EnumType.STRING)`, so the column holds the names `GENERATED`/`SENT`/`PAID`/`OVERDUE`/`DISPUTED`.

---

## src/main/java/com/teleconnect/billing_service/enums/PaymentMethod.java

Defines the set of accepted payment instruments. Unlike the other four, this is a *method type* (the "how money moved"), not a lifecycle status. It is used both inbound (in `PaymentRequest`) and persisted (on the `Payment` entity).

```java
// L1-L2
package com.teleconnect.billing_service.enums;
                                                                            (blank line)
```

Standard package declaration plus blank line.

```java
// L3-L10
public enum PaymentMethod {
    UPI,
    CARD,
    NETBANKING,
    WALLET,
    BANK_TRANSFER,
    CASH
}
```

`public enum PaymentMethod` declares six constants, here written one-per-line purely for readability (formatting has no semantic effect — the commas separate constants exactly as the single-line style in the other files does):

- **`UPI`** — Unified Payments Interface (India's real-time bank-to-bank rails).
- **`CARD`** — credit/debit card.
- **`NETBANKING`** — direct online bank-portal transfer.
- **`WALLET`** — digital/stored-value wallet.
- **`BANK_TRANSFER`** — direct bank account transfer (e.g. NEFT/IMPS/wire). Contains an underscore, so request binding must use the exact `"BANK_TRANSFER"` string.
- **`CASH`** — physical cash payment.

This enum is the type of `PaymentRequest.paymentMethod` (the inbound DTO field a client submits when recording a payment) and of `Payment.paymentMethod` (the persisted column). The closing brace ends the type. There is no `OTHER`/`UNKNOWN` catch-all, so any payment instrument outside these six cannot be represented without changing this enum.

---

## src/main/java/com/teleconnect/billing_service/enums/PaymentStatus.java

Defines the outcome states of a payment attempt. Stored on the `Payment` entity to record whether the money actually moved.

```java
// L1-L2
package com.teleconnect.billing_service.enums;
                                                                            (blank line)
```

Package declaration and blank separator.

```java
// L3-L7
public enum PaymentStatus {
    SUCCESS,
    FAILED,
    PENDING
}
```

`public enum PaymentStatus` declares three constants describing the result of a payment:

- **`SUCCESS`** — the payment completed and funds were captured.
- **`FAILED`** — the payment attempt was declined or errored.
- **`PENDING`** — the payment is initiated but not yet confirmed (e.g. awaiting gateway settlement).

This enum types `Payment.status` and is persisted via `@Enumerated(EnumType.STRING)`. The closing brace ends the type. Like the others, it is a flat list with no transition logic encoded in the enum itself — any rules about moving from `PENDING` to `SUCCESS`/`FAILED` live in the service layer, not here.

---

## How this connects

These five enums sit at the very bottom of the dependency graph — they import nothing and depend on no other application class, while almost every other layer depends on them:

- **Entity/DB layer:** `BillingCycle.status`, `BillingDispute.status`, `Invoice.status`, and `Payment.paymentMethod` / `Payment.status` are typed by these enums and annotated `@Enumerated(EnumType.STRING)` + `@Column(nullable = false)`, so each value is stored as its literal name in a non-null column. `EnumType.STRING` (rather than the default `ORDINAL`) is what makes the persisted data human-readable and resilient to reordering the constants.
- **Repository layer:** Spring Data JPA derived queries accept these enums as parameters — e.g. `BillingCycleRepository.findByStatus(BillingCycleStatus)`, `InvoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus, LocalDate)`, `BillingDisputeRepository.findByStatus(DisputeStatus)` — and JPA translates the constant to its string form in the generated SQL `WHERE` clause.
- **Service layer:** The `*ServiceImpl` classes branch on these constants to enforce business rules (e.g. one OPEN cycle per account; cannot dispute a `PAID` or already-`DISPUTED` invoice; cannot update a `RESOLVED`/`REJECTED` dispute; cannot manually set a cycle to `GENERATED`).
- **DTO layer:** Response DTOs (`BillingCycleResponse`, `DisputeResponse`, `InvoiceResponse`) expose the status enums back to clients, and `PaymentRequest` accepts a `PaymentMethod` inbound.
- **Controller layer:** REST endpoints bind these enums directly from the HTTP request as `@RequestParam`/`@PathVariable` (e.g. `getInvoicesByStatus(@PathVariable InvoiceStatus status)`); Spring MVC converts the request string to the matching constant, and an unrecognized value fails conversion before any business logic runs — the type system thereby acts as the first line of input validation for these fixed value sets.
