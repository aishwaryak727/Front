# Billing Service — Architecture Overview

> TeleConnect Billing and Invoice Management Module
>
> A Spring Boot microservice that manages the full post-paid billing lifecycle for a
> telecom platform: opening billing cycles, generating invoices, recording payments,
> applying/waiving late fees, handling billing disputes, producing PDF documents, and
> emitting operational reports.

This document is the entry point for a new developer. It explains the technology stack,
how to build and run the service, the layered architecture and *why* it is structured
that way, a concrete end-to-end request trace, the domain model, and the REST surface.

---

## 1. Tech stack & versions

Derived from `pom.xml` and `src/main/resources/application.properties`.

| Area | Choice | Notes |
|------|--------|-------|
| Language | **Java 21** | `<java.version>21</java.version>` |
| Framework | **Spring Boot 3.4.1** | Inherited from `spring-boot-starter-parent` |
| Build tool | **Maven** (with the bundled Maven Wrapper `mvnw`) | |
| Group / Artifact | `com.teleconnect` / `billing-service` | version `0.0.1-SNAPSHOT` |
| Base package | `com.teleconnect.billing_service` | |

### Key dependencies (starters & libraries)

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-web` | REST controllers, embedded Tomcat, Jackson JSON |
| `spring-boot-starter-data-jpa` | Spring Data JPA + Hibernate ORM for persistence |
| `spring-boot-starter-validation` | Bean Validation (`jakarta.validation`) on request DTOs |
| `spring-boot-starter-security` | HTTP security filter chain, password encoding |
| `spring-boot-devtools` (runtime, optional) | Hot reload during development |
| `mysql-connector-j` (runtime) | MySQL JDBC driver (`com.mysql.cj.jdbc.Driver`) |
| `org.projectlombok:lombok` (optional) | Boilerplate reduction; configured as an annotation processor. **Note:** the entities/DTOs in this codebase mostly hand-roll their getters, setters and `Builder` classes rather than relying on Lombok annotations. |
| `org.apache.pdfbox:pdfbox` **3.0.3** | Generates invoice and account-statement PDFs |
| `spring-boot-starter-test` (test) | JUnit 5, Mockito, AssertJ, Spring test support |
| `spring-security-test` (test) | Security-aware test utilities |

The Spring Boot Maven plugin builds the executable jar; Lombok is excluded from the
final artifact (it is compile-time only).

---

## 2. How to build & run

Use the Maven Wrapper checked into the repo (no local Maven install required).

```bash
# From the project root (the folder containing pom.xml and mvnw)

# Build + run the unit tests
./mvnw clean verify          # Linux / macOS
mvnw.cmd clean verify        # Windows

# Run the application
./mvnw spring-boot:run       # Linux / macOS
mvnw.cmd spring-boot:run     # Windows

# Or build a jar and run it
./mvnw clean package
java -jar target/billing-service-0.0.1-SNAPSHOT.jar
```

### Prerequisites

- **JDK 21** on the `PATH`.
- A reachable **MySQL** instance at `localhost:3306` with credentials `root` / `root`.
  The schema `teleconnect_billing` is created automatically
  (`createDatabaseIfNotExist=true`) and tables are created/updated by Hibernate
  (`ddl-auto=update`).

### Key `application.properties` settings explained

| Property | Value | What it does |
|----------|-------|--------------|
| `spring.application.name` | `billing-service` | Logical service name. |
| `server.port` | `8085` | The HTTP port the embedded Tomcat listens on. |
| `server.servlet.context-path` | `/teleConnect` | Prefix on every URL. The real base URL is `http://localhost:8085/teleConnect`. |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/teleconnect_billing?...` | MySQL connection; auto-creates the DB, disables SSL, forces UTC timezone, allows public-key retrieval. |
| `spring.datasource.username` / `.password` | `root` / `root` | DB credentials (dev defaults — externalize for any real deployment). |
| `spring.datasource.driver-class-name` | `com.mysql.cj.jdbc.Driver` | MySQL JDBC driver class. |
| `spring.jpa.hibernate.ddl-auto` | `update` | Hibernate creates/alters tables from the `@Entity` classes on startup. Convenient for dev; not recommended for production migrations. |
| `spring.jpa.show-sql` / `hibernate.format_sql` | `true` / `true` | Logs formatted SQL for debugging. |
| `spring.jpa.properties.hibernate.dialect` | `MySQLDialect` | Tells Hibernate the SQL dialect. |
| `spring.jackson.serialization.write-dates-as-timestamps` | `false` | Dates serialize as ISO strings, not epoch numbers. |
| `spring.jackson.date-format` | `yyyy-MM-dd` | Default JSON date format (some DTOs override with `@JsonFormat`). |

### Security note

`SecurityConfig` registers a `SecurityFilterChain` that **disables CSRF**, sets the
session policy to **STATELESS**, and uses `.anyRequest().permitAll()` — so every
endpoint is currently open. It also defines a `BCryptPasswordEncoder` and an
in-memory `UserDetailsService` with two users (`billing`/`billing123` role `BILLING`,
and `admin`/`admin123` roles `ADMIN`+`BILLING`). The user store and encoder exist as
scaffolding for future authentication; no endpoint enforces a role yet.

---

## 3. Layered architecture

The service follows a classic **layered (n-tier) architecture**. A request flows
downward through the layers and a response flows back up. Each layer has one job and
talks only to the layer directly beneath it.

```
        HTTP request (JSON)
              │
              ▼
   ┌───────────────────────────────────────────┐
   │  Controller layer  (@RestController)        │  REST / web boundary
   │  e.g. InvoiceController, PaymentController   │  - maps URLs to methods
   └───────────────────────────────────────────┘  - validates request DTOs (@Valid)
              │  (Request DTO)                       - returns ResponseEntity<Response DTO>
              ▼
   ┌───────────────────────────────────────────┐
   │  Service interface  (e.g. InvoiceService)   │  Business-logic contract
   │  Service impl       (...ServiceImpl)        │  - rules, validation, state changes
   │                     @Transactional          │  - maps DTO <-> entity
   └───────────────────────────────────────────┘
              │  (Entity)
              ▼
   ┌───────────────────────────────────────────┐
   │  Repository layer  (Spring Data JPA)        │  Persistence abstraction
   │  e.g. InvoiceRepository extends JpaRepository│  - derived query methods, @Query
   └───────────────────────────────────────────┘
              │  (Entity / SQL)
              ▼
   ┌───────────────────────────────────────────┐
   │  Entity layer  (@Entity JPA mappings)       │  Object <-> table mapping
   │  Invoice, Payment, BillingCycle, Dispute    │
   └───────────────────────────────────────────┘
              │
              ▼
          MySQL database
```

### Responsibilities and why the separation exists

- **Controller layer** (`controller/`) — the web/REST boundary. Each controller is a
  `@RestController` that maps URLs (`@RequestMapping`, `@GetMapping`, `@PostMapping`,
  `@PutMapping`) to handler methods, binds path variables / query params / JSON
  bodies, triggers Bean Validation (`@Valid`), delegates to a service, and wraps the
  result in a `ResponseEntity` with the right HTTP status. Controllers contain **no
  business logic** — keeping them thin makes the API surface easy to read and the
  logic reusable and testable without HTTP.

- **Service layer** (`service/` interface + `service/impl/` implementation) — the
  business logic. Interfaces define the contract; `*ServiceImpl` classes implement it.
  Methods that mutate data are annotated `@Transactional` so a unit of work either
  commits fully or rolls back on exception. The service enforces invariants (e.g. "an
  invoice cannot be paid twice", "late fees only apply to OVERDUE invoices") and maps
  between **DTOs** (the API shape) and **entities** (the persistence shape). Splitting
  the interface from the implementation allows mocking in tests and swapping the impl
  without touching callers.

- **Repository layer** (`repository/`) — persistence. Each interface extends
  `JpaRepository<Entity, IdType>`; Spring Data generates the implementation at runtime.
  Queries are expressed as **derived method names** (`findByAccountIdAndStatus`) or
  explicit JPQL via `@Query`. This isolates SQL/ORM details from business logic.

- **Entity layer** (`entity/`) — JPA `@Entity` classes mapped to tables with
  `@Table`, `@Id`, `@GeneratedValue`, `@Column`, and `@Enumerated`. Lifecycle hooks
  (`@PrePersist`, `@PreUpdate`) stamp `createdAt`/`updatedAt` and default money fields.
  Entities are the in-memory representation of database rows.

- **DTO layer** (`dto/request/`, `dto/response/`) — cross-cutting. **Request DTOs**
  define incoming payloads and carry validation annotations
  (`@NotNull`, `@DecimalMin`, etc.). **Response DTOs** define exactly what the API
  returns. Keeping DTOs separate from entities means the API contract can evolve
  independently of the database schema and internal fields (audit timestamps, etc.)
  are never accidentally exposed.

- **Enum layer** (`enums/`) — shared domain vocabulary for statuses and types,
  persisted as strings (`@Enumerated(EnumType.STRING)`) and accepted/returned directly
  in the API.

- **Exception-handling layer** (`exception/`) — custom exceptions
  (`ResourceNotFoundException`, `BillingException`) plus a single
  `@RestControllerAdvice` (`GlobalExceptionHandler`) that converts exceptions into
  consistent JSON error responses with the correct HTTP status. Centralizing this
  keeps controllers/services free of `try/catch`-to-HTTP plumbing.

- **Config layer** (`config/`) — Spring `@Configuration` classes: `SecurityConfig`
  (filter chain, password encoder, in-memory users) and `SchedulingConfig`
  (`@EnableScheduling`, which activates the `@Scheduled` overdue-marking job).

---

## 4. Request / response flow (ASCII)

```
Client
  │  POST /teleConnect/billing/invoices/{id}/pay   { amountPaid, paymentMethod, transactionRef }
  ▼
[ Spring DispatcherServlet ]
  │  routes by URL + HTTP verb
  ▼
[ InvoiceController.payInvoice() ]  ──@Valid──►  PaymentRequest validated
  │  request.setInvoiceId(id)
  ▼
[ InvoiceServiceImpl.processPayment() ]  @Transactional   <── business rules
  │   ├─ load Invoice  ──────────────►  InvoiceRepository.findById()  ──► SELECT
  │   ├─ check status / amount / dup ref
  │   ├─ build Payment entity  ──────►  PaymentRepository.save()      ──► INSERT
  │   └─ mark invoice PAID    ───────►  InvoiceRepository.save()      ──► UPDATE
  ▼
[ toResponse(...) → InvoiceResponse / MessageResponse ]
  │
  ▼
[ InvoiceController ]  wraps in ResponseEntity (200 OK)
  │
  ▼
Client  ◄── JSON { "message": "Payment recorded successfully" }

        ── on any thrown BillingException / ResourceNotFoundException ──►
        [ GlobalExceptionHandler ]  →  { timestamp, status, error, message }
        (transaction rolled back)
```

---

## 5. End-to-end trace: recording a payment

Concrete walk-through of `POST /teleConnect/billing/invoices/{invoiceId}/pay`.

**Request**
```
POST http://localhost:8085/teleConnect/billing/invoices/5001/pay
Content-Type: application/json

{ "amountPaid": 949.32, "paymentMethod": "UPI", "transactionRef": "TXN98765" }
```

1. **Controller** — `InvoiceController.payInvoice(Long invoiceId, @Valid PaymentRequest request)`
   (`controller/InvoiceController.java`). Spring binds `invoiceId` from the path and
   deserializes the JSON body into a **`PaymentRequest`** DTO. `@Valid` triggers Bean
   Validation: `amountPaid` must be non-null and `>= 0.01`, `paymentMethod` must be a
   valid `PaymentMethod` enum value. The controller calls
   `invoiceService.payInvoice(invoiceId, request)`.

2. **Service** — `InvoiceServiceImpl.payInvoice(...)`
   (`service/impl/InvoiceServiceImpl.java`) sets the path id onto the DTO
   (`request.setInvoiceId(invoiceId)`) and delegates to `processPayment(request)`,
   which is `@Transactional`. Inside `processPayment` the business rules run:
   - Load the **`Invoice`** entity via `invoiceRepository.findById(...)`; if absent,
     throw `ResourceNotFoundException`.
   - Reject if the invoice is already `PAID` or is `DISPUTED` (`BillingException`).
   - Reject if `amountPaid < totalAmount` (`BillingException`).
   - If a `transactionRef` is supplied, reject duplicates via
     `paymentRepository.findByTransactionRef(...)` (`BillingException`).

3. **DTO → Entity** — a **`Payment`** entity is assembled with `Payment.builder()`
   (invoiceId, amountPaid, paymentMethod, transactionRef, `status = SUCCESS`).

4. **Repository → DB** — `paymentRepository.save(payment)` issues an `INSERT` into
   `payments` (the `@PrePersist` hook stamps `paymentDate`). The invoice is then
   mutated (`status = PAID`, `paidAmount = amountPaid`) and persisted with
   `invoiceRepository.save(invoice)` → `UPDATE invoices` (the `@PreUpdate` hook bumps
   `updatedAt`).

5. **Entity → Response** — `processPayment` returns an `InvoiceResponse`
   (built by the private `toResponse`). For the `/pay` endpoint the controller ignores
   that value and instead returns `ResponseEntity.ok(new MessageResponse("Payment recorded successfully"))`.

6. **Serialization** — Jackson serializes the response DTO to JSON; Spring returns
   HTTP `200 OK`.

**Error propagation.** Any `BillingException` or `ResourceNotFoundException` thrown in
step 2 aborts the method; because `processPayment` is `@Transactional`, the transaction
is rolled back (no partial `Payment`/`Invoice` writes). The exception bubbles up to
`GlobalExceptionHandler` (`@RestControllerAdvice`), which maps it to a structured JSON
body `{ timestamp, status, error, message }`:
`ResourceNotFoundException → 404 NOT FOUND`, `BillingException → 400 BAD REQUEST`,
validation failures (`MethodArgumentNotValidException`) → `400` with a per-field
`errors` map, type-mismatched params (`MethodArgumentTypeMismatchException`) → `400`,
and anything else → `500 INTERNAL SERVER ERROR`.

> There is also a parallel `POST /teleConnect/billing/payments` endpoint handled by
> `PaymentController` → `PaymentServiceImpl.makePayment(...)`, which performs the same
> validation but returns a full `PaymentResponse` instead of a message.

---

## 6. Domain model

Four JPA entities. There are **no JPA relationship annotations** between them — they
are linked by plain ID columns (`accountId`, `cycleId`, `invoiceId`, `subscriberId`),
a deliberately loosely-coupled design. `accountId` / `subscriberId` reference customer
records owned by other services.

```
   BillingCycle (1) ───< cycleId ───  Invoice (1) ───< invoiceId ───  Payment
        │  one cycle can produce            │  one invoice can have
        │  one invoice per account          │  many payments
        │                                   └──< invoiceId ──  BillingDispute
        │                                            (a dispute targets one invoice)
        └─ both reference an external accountId / subscriberId
```

### Entities

- **`BillingCycle`** (`billing_cycles`) — a billing period for an account
  (`cycleStart`..`cycleEnd`), the unit that drives invoice generation. Tracks
  `generatedDate` and a `BillingCycleStatus`. When an invoice is generated for a cycle
  it moves `OPEN → GENERATED`; it can later be `CLOSED` (irreversible). Audited with
  `createdAt`/`updatedAt`.
  Enum: **`BillingCycleStatus { OPEN, GENERATED, CLOSED }`**.

- **`Invoice`** (`invoices`) — the bill produced for one account in one cycle. Holds a
  charge breakdown (`planCharges`, `excessCharges`, `addOnCharges`, `taxes`),
  `totalAmount`, `paidAmount`, `lateFee`, a `dueDate` (cycle end + 15 days), and an
  `InvoiceStatus`. All money is `BigDecimal(precision=10, scale=2)`. Audited with
  `createdAt`/`updatedAt`. Linked to its cycle by `cycleId` and to a customer by
  `accountId`.
  Enum: **`InvoiceStatus { GENERATED, SENT, PAID, OVERDUE, DISPUTED }`**.

- **`Payment`** (`payments`) — a single payment recorded against an invoice
  (`invoiceId`). Holds `amountPaid`, `paymentDate` (auto-stamped on persist),
  `paymentMethod`, a unique `transactionRef`, and a `PaymentStatus`.
  Enums: **`PaymentMethod { UPI, CARD, NETBANKING, WALLET, BANK_TRANSFER, CASH }`**
  and **`PaymentStatus { SUCCESS, FAILED, PENDING }`**.

- **`BillingDispute`** (`billing_disputes`) — a customer dispute against an invoice
  (`invoiceId`, `subscriberId`). Captures `disputeReason`, `description`,
  `disputedAmount`, optional `resolvedAmount`, lifecycle timestamps
  (`raisedDate`, `acknowledgedDate`, `resolvedDate`), `assignedTo`, `resolutionNotes`,
  and a `DisputeStatus`.
  Enum: **`DisputeStatus { OPEN, UNDER_REVIEW, RESOLVED, REJECTED }`**.

### Status lifecycles at a glance

```
Invoice:        GENERATED ──send──► SENT ──pay──► PAID
                    │                  │
                  (overdue cron / mark-overdue)
                    ▼                  ▼
                 OVERDUE ◄────────────┘     (also: DISPUTED when a dispute is raised)

BillingCycle:   OPEN ──generate invoice──► GENERATED ──close──► CLOSED

Dispute:        OPEN ──review──► UNDER_REVIEW ──resolve──► RESOLVED | REJECTED
```

A scheduled job (`InvoiceServiceImpl.markOverdueInvoices`, `@Scheduled(cron = "0 0 1 * * *")`,
enabled by `SchedulingConfig`) runs daily at 01:00 and flips `GENERATED`/`SENT`
invoices whose `dueDate` has passed to `OVERDUE`. The same logic is exposed manually as
`PUT /billing/invoices/mark-overdue`.

---

## 7. REST endpoints (grouped by controller)

All paths are prefixed with the context path **`/teleConnect`**
(e.g. the full URL for the first row is `http://localhost:8085/teleConnect/billing/invoices/generate`).

### `InvoiceController` — base `/billing/invoices`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/generate` | Generate an invoice for a cycle (201) |
| GET | `/account/{accountId}` | List invoices for an account (optional `status`, `fromDate`, `toDate` filters) |
| GET | `/status/{status}` | List invoices by status |
| PUT | `/mark-overdue` | Manually mark past-due invoices OVERDUE |
| GET | `/{invoiceId}` | Get one invoice |
| PUT | `/{invoiceId}/send` | Move a GENERATED invoice to SENT |
| POST | `/{invoiceId}/pay` | Record a payment against the invoice |
| POST | `/{invoiceId}/latefee` | Apply a late fee (OVERDUE invoices only) |
| POST | `/{invoiceId}/latefee/waive` | Waive an existing late fee |
| GET | `/{invoiceId}/download` | Download the invoice as a PDF |
| GET | `/account/{accountId}/statement` | Download a full account statement PDF |

### `PaymentController` — base `/billing/payments`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Record a payment (returns full PaymentResponse, 201) |
| GET | `/{paymentId}` | Get one payment |
| GET | `/invoice/{invoiceId}` | List payments for an invoice |

### `BillingCycleController` — base `/billing/cycles`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Create a billing cycle (201) |
| POST | `/generate` | Batch-generate invoices for eligible open cycles |
| GET | `/account/{accountId}` | List cycles for an account (paginated; optional `status`) |
| PUT | `/{cycleId}/close` | Close a cycle (irreversible) |
| PUT | `/{cycleId}/status` | Update a cycle's status (`status` query param) |
| GET | `/{cycleId}` | Get one cycle |

### `BillingDisputeController` — base `/billing/disputes`
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/` | Raise a dispute (201) |
| GET | `/account/{accountId}` | List disputes for an account (optional `status`) |
| GET | `/invoice/{invoiceId}` | List disputes for an invoice |
| GET | `/subscriber/{subscriberId}` | List disputes for a subscriber |
| GET | `/status/{status}` | List disputes by status |
| GET | `/{disputeId}` | Get one dispute |
| PUT | `/{disputeId}/review` | Move a dispute to UNDER_REVIEW |
| PUT | `/{disputeId}/resolve` | Resolve a dispute |
| PUT | `/{disputeId}/status` | Update a dispute's status (`status` query param) |

### `ReportController` — base `/billing/reports`
| Method | Path | Purpose |
|--------|------|---------|
| GET | `/overdue` | Overdue invoices by aging bucket (optional `region`, `agingBucket`) |
| GET | `/collection` | Collection efficiency for a period (`fromDate`, `toDate`, optional `region`) |
| GET | `/disputes/summary` | Dispute SLA-compliance summary (`fromDate`, `toDate`) |

> Note on route ordering: controllers deliberately declare static sub-paths
> (`/generate`, `/account/{id}`, `/status/{x}`) **before** the dynamic `/{id}` route so
> Spring does not try to bind a literal like `account` to a `Long` path variable.

---

## 8. Where to go next

For per-file detail, see the rest of the `docs/explanation/` folder — start with the
[index / README](README.md), which lists every document and a recommended reading
order.
