# Billing Service — Explanation Docs

This folder contains the `docs/explanation/` reference for the **TeleConnect Billing and
Invoice Management Module**, a Spring Boot 3 / Java 21 microservice. The documents walk
the codebase layer by layer — from the project build and runtime configuration, through
the domain enums and entities, the request/response DTOs, the persistence repositories,
the exception-handling layer, and finally the service implementations and REST
controllers, with the test suite covered at the end. Read them to understand how an HTTP
request travels through the controller → service → repository → entity → database stack
and back.

## Recommended reading order

1. Start with **[00-overview.md](00-overview.md)** for the big picture (architecture, tech stack, request flow, domain model).
2. Read **[01-project-and-config.md](01-project-and-config.md)** to understand how the app builds, boots, and is configured.
3. Learn the shared vocabulary in **[02-enums.md](02-enums.md)**, then the four entities (**[03](03-entity-BillingCycle.md)**–**[06](06-entity-BillingDispute.md)**).
4. Cover the API data shapes: request DTOs **[07](07-dto-request.md)** and response DTOs **[08](08-dto-response.md)**.
5. Move down to persistence **[09](09-repositories.md)** and error handling **[10](10-exceptions.md)**.
6. Study the business logic: service interfaces **[11](11-service-interfaces.md)** then the implementations **[12](12-impl-BillingCycleService.md)**–**[16](16-impl-ReportService.md)**.
7. See how it is all exposed in the controllers **[17](17-controller-BillingCycle.md)**–**[21](21-controller-Report.md)**.
8. Finish with the tests **[22](22-test-InvoiceService.md)**–**[24](24-tests-cycle-and-context.md)**.

## All documents

- [00-overview.md](00-overview.md) — Architecture overview, tech stack, request flow, and domain model (read this first).
- [01-project-and-config.md](01-project-and-config.md) — Project build, runtime configuration, the application entry point, and Spring config classes.
- [02-enums.md](02-enums.md) — Domain enums: status values and payment method/type definitions.
- [03-entity-BillingCycle.md](03-entity-BillingCycle.md) — Entity: `BillingCycle` (the billing period that drives invoice generation).
- [04-entity-Invoice.md](04-entity-Invoice.md) — Entity: `Invoice` (the bill, its charge breakdown and lifecycle).
- [05-entity-Payment.md](05-entity-Payment.md) — Entity: `Payment` (a payment recorded against an invoice).
- [06-entity-BillingDispute.md](06-entity-BillingDispute.md) — Entity: `BillingDispute` (a customer dispute against an invoice).
- [07-dto-request.md](07-dto-request.md) — Request DTOs: incoming API payloads and their validation rules.
- [08-dto-response.md](08-dto-response.md) — Response DTOs: outgoing API payloads.
- [09-repositories.md](09-repositories.md) — Repositories: the Spring Data JPA persistence layer and query methods.
- [10-exceptions.md](10-exceptions.md) — Exception-handling layer: custom exceptions and the global handler.
- [11-service-interfaces.md](11-service-interfaces.md) — Service interfaces: the business-logic contracts.
- [12-impl-BillingCycleService.md](12-impl-BillingCycleService.md) — Service impl: `BillingCycleServiceImpl`.
- [13-impl-InvoiceService.md](13-impl-InvoiceService.md) — Service impl: `InvoiceServiceImpl` (the largest file; invoices, payments, late fees, PDFs).
- [14-impl-PaymentService.md](14-impl-PaymentService.md) — Service impl: `PaymentServiceImpl`.
- [15-impl-BillingDisputeService.md](15-impl-BillingDisputeService.md) — Service impl: `BillingDisputeServiceImpl`.
- [16-impl-ReportService.md](16-impl-ReportService.md) — Service impl: `ReportServiceImpl`.
- [17-controller-BillingCycle.md](17-controller-BillingCycle.md) — Controller: `BillingCycleController`.
- [18-controller-BillingDispute.md](18-controller-BillingDispute.md) — Controller: `BillingDisputeController`.
- [19-controller-Invoice.md](19-controller-Invoice.md) — Controller: `InvoiceController`.
- [20-controller-Payment.md](20-controller-Payment.md) — Controller: `PaymentController`.
- [21-controller-Report.md](21-controller-Report.md) — Controller: `ReportController`.
- [22-test-InvoiceService.md](22-test-InvoiceService.md) — Test: `InvoiceServiceTest`.
- [23-test-BillingDisputeService.md](23-test-BillingDisputeService.md) — Test: `BillingDisputeServiceTest`.
- [24-tests-cycle-and-context.md](24-tests-cycle-and-context.md) — Tests: `BillingCycleServiceTest` and the `BillingServiceApplicationTests` context-load test.
