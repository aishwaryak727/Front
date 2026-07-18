# ReportController — Reporting REST Endpoints

`ReportController` is the web (presentation) layer for the billing service's *reporting* features. It exposes three read-only HTTP `GET` endpoints — overdue-invoice aging, collection-efficiency metrics, and dispute-SLA summaries — and does nothing more than translate incoming HTTP query parameters into method calls on `ReportService`, then wrap the service's result in an HTTP response. It sits at the top of the standard layering: **Controller → Service → Repository → Entity/DB**. This controller delegates all business logic and data access downward to `ReportService` (interface), which in turn uses repositories and entities; the controller itself holds no state and performs no querying.

## src/main/java/com/teleconnect/billing_service/controller/ReportController.java

This file defines the `ReportController` REST controller. Its sole job is to map three HTTP `GET` routes under `/billing/reports` to corresponding `ReportService` calls and return the results as JSON inside `ResponseEntity` wrappers.

```java
// L1
package com.teleconnect.billing_service.controller;
```

This is the **package declaration**. It places the class in the `com.teleconnect.billing_service.controller` package — the conventional home for web/MVC controllers in this project. The package path must match the directory structure on disk (`.../controller/ReportController.java`), and it determines the fully qualified class name (`com.teleconnect.billing_service.controller.ReportController`) used by Spring's component scanning to discover and register this bean.

```java
// L3-L6
import com.teleconnect.billing_service.dto.response.CollectionReportResponse;
import com.teleconnect.billing_service.dto.response.DisputeSummaryResponse;
import com.teleconnect.billing_service.dto.response.OverdueReportResponse;
import com.teleconnect.billing_service.service.ReportService;
```

These imports bring in the project's own types. The three `*Response` classes are **DTOs (Data Transfer Objects)** from the `dto.response` package — plain data-carrying objects that define the JSON shape returned to the client for each report (collection report, dispute summary, overdue report). A DTO is deliberately separate from the database entities so the API contract is decoupled from the persistence model. `ReportService` is the **service-layer interface** this controller depends on; the controller talks only to this abstraction, never directly to repositories, which keeps business logic out of the web layer.

```java
// L7
import org.springframework.beans.factory.annotation.Autowired;
```

Imports Spring's `@Autowired` annotation, which marks a field, constructor, or setter for **dependency injection** — telling the Spring container to automatically supply a matching bean (here, a `ReportService` implementation) at runtime so the controller does not have to construct it itself.

```java
// L8
import org.springframework.format.annotation.DateTimeFormat;
```

Imports `@DateTimeFormat`, a Spring annotation used to tell Spring MVC how to **parse a request string into a date/time type** (here `java.time.LocalDate`). Without it, Spring may not know how to convert a query string like `2026-05-01` into a `LocalDate`.

```java
// L9
import org.springframework.http.ResponseEntity;
```

Imports `ResponseEntity<T>` — a generic Spring type representing the **entire HTTP response**: status code, headers, and a typed body of type `T`. Returning a `ResponseEntity` (rather than the bare DTO) gives explicit control over the HTTP status returned to the client.

```java
// L10-L13
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
```

These are the Spring Web MVC annotations used to declare the HTTP routing. `@RestController` marks the class as a REST controller; `@RequestMapping` sets a base URL path; `@GetMapping` maps a method to an HTTP `GET` route; `@RequestParam` binds a URL query-string parameter to a method argument. Each is defined in detail at its first use below.

```java
// L15
import java.time.LocalDate;
```

Imports `java.time.LocalDate` — the standard Java type for a calendar date (year-month-day) with no time-of-day or time-zone component. The date-range report endpoints use it for their `fromDate`/`toDate` parameters.

```java
// L17-L19
@RestController
@RequestMapping("/billing/reports")
public class ReportController {
```

`@RestController` is a Spring stereotype annotation that combines `@Controller` (marking the class as a web request handler that Spring should detect and register as a bean during component scanning) with `@ResponseBody` (meaning every handler method's return value is **serialized directly into the HTTP response body**, typically as JSON, rather than being interpreted as a view/template name). So all three methods here return JSON, not HTML pages.

`@RequestMapping("/billing/reports")` sets the **base URL path** shared by every endpoint in this class. Each method's `@GetMapping` path is appended to this base. So the effective paths are `/billing/reports/overdue`, `/billing/reports/collection`, and `/billing/reports/disputes/summary`.

*Aside: the Javadoc comments on each method describe the URL as `/teleConnect/billing/reports/...` with a `/teleConnect` prefix, but no such prefix appears in the `@RequestMapping` value. That prefix would have to come from elsewhere — for example a servlet context path or `server.servlet.context-path` configured in application properties — and is not visible in this file. Treat the doc comments' `/teleConnect` prefix as documentation of the deployed external path, not something this annotation establishes.*

`public class ReportController` declares the class itself.

```java
// L21-L22
    @Autowired
    private ReportService reportService;
```

This is the controller's single field and its only dependency. `private ReportService reportService;` declares a field of the service-interface type. `@Autowired` performs **field-based dependency injection**: when Spring instantiates this controller, it looks up a bean that implements `ReportService` and assigns it to this field, so the controller can delegate work to it. Because the field is typed to the interface (`ReportService`) rather than a concrete implementation class, the controller is loosely coupled to whatever implementation Spring wires in.

*Aside: field injection via `@Autowired` works but is generally discouraged in favor of constructor injection (which makes the dependency final, easier to unit-test without a Spring context, and impossible to leave null). This is a style note, not a bug.*

```java
// L24-L28
    /**
     * GET /teleConnect/billing/reports/overdue
     * Returns overdue invoices grouped by aging buckets: 0-30, 31-60, 61-90, 90+ days past DueDate.
     * Query: region=South, agingBucket=0-30
     */
```

A Javadoc comment documenting the first endpoint. It states the route, that the report returns overdue invoices grouped into aging buckets (0–30, 31–60, 61–90, and 90+ days past the due date), and gives example query parameters. The actual bucketing logic lives in the service layer, not here — this is descriptive documentation only.

```java
// L29-L34
    @GetMapping("/overdue")
    public ResponseEntity<OverdueReportResponse> getOverdueReport(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String agingBucket) {
        return ResponseEntity.ok(reportService.getOverdueReport(region, agingBucket));
    }
```

`@GetMapping("/overdue")` maps HTTP `GET` requests for `/billing/reports/overdue` (base path + `/overdue`) to this method. `@GetMapping` is a shortcut for `@RequestMapping(method = GET)`.

The method `getOverdueReport` returns `ResponseEntity<OverdueReportResponse>` — an HTTP response whose body is an `OverdueReportResponse` DTO.

Its two parameters use `@RequestParam`, which binds an HTTP **query-string parameter** to the argument. `@RequestParam(required = false) String region` binds the `?region=...` query parameter; `required = false` means the parameter is **optional** — if the client omits it, the argument is `null` rather than causing a 400 Bad Request. Likewise `@RequestParam(required = false) String agingBucket` optionally binds `?agingBucket=...`. So both filters are optional; a bare call to `/billing/reports/overdue` passes `null` for both.

The body is a single statement. `reportService.getOverdueReport(region, agingBucket)` delegates entirely to the service layer, passing both (possibly `null`) filters; the service computes the report. `ResponseEntity.ok(...)` wraps that returned DTO in a response with **HTTP status 200 OK**, and `return` sends it back. The controller adds no logic of its own beyond delegation and status wrapping.

```java
// L36-L40
    /**
     * GET /teleConnect/billing/reports/collection
     * Returns collection efficiency metrics for a given period and region.
     * Query: fromDate=2026-05-01, toDate=2026-05-31, region=South
     */
```

Javadoc for the second endpoint: it returns collection-efficiency metrics for a date period and (optionally) a region, with example query parameters showing the ISO date format expected.

```java
// L41-L47
    @GetMapping("/collection")
    public ResponseEntity<CollectionReportResponse> getCollectionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String region) {
        return ResponseEntity.ok(reportService.getCollectionReport(fromDate, toDate, region));
    }
```

`@GetMapping("/collection")` maps `GET /billing/reports/collection` to this method, which returns `ResponseEntity<CollectionReportResponse>`.

It has three parameters:
- `fromDate` (`LocalDate`): bound by `@RequestParam` (with no `required` attribute, so it defaults to **required** — omitting `?fromDate=...` yields an HTTP 400). The added `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` tells Spring to parse the query string using the **ISO-8601 date format `yyyy-MM-dd`** (e.g. `2026-05-01`) when converting it into a `LocalDate`.
- `toDate` (`LocalDate`): identical handling — required, ISO-date-parsed. Together `fromDate` and `toDate` define the reporting period.
- `region` (`String`): `@RequestParam(required = false)`, so it is the optional region filter (`null` when omitted).

The body delegates to `reportService.getCollectionReport(fromDate, toDate, region)` and wraps the resulting `CollectionReportResponse` DTO in a **200 OK** `ResponseEntity`. No date-range validation (e.g. checking `fromDate <= toDate`) is performed here; any such rule would be enforced in the service layer.

```java
// L49-L53
    /**
     * GET /teleConnect/billing/reports/disputes/summary
     * Returns dispute SLA compliance summary.
     * Query: fromDate=2026-05-01, toDate=2026-05-31
     */
```

Javadoc for the third endpoint: it returns a dispute SLA-compliance summary over a date period, with example ISO-date query parameters.

```java
// L54-L59
    @GetMapping("/disputes/summary")
    public ResponseEntity<DisputeSummaryResponse> getDisputeSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(reportService.getDisputeSummary(fromDate, toDate));
    }
```

`@GetMapping("/disputes/summary")` maps `GET /billing/reports/disputes/summary` (a nested two-segment path appended to the base) to this method, which returns `ResponseEntity<DisputeSummaryResponse>`.

It takes two parameters, `fromDate` and `toDate`, both `LocalDate`, both bound via `@RequestParam` with **no `required` attribute (so both are mandatory)** and both parsed with `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` as ISO `yyyy-MM-dd` dates. Unlike the collection endpoint, there is no `region` parameter here.

The body delegates to `reportService.getDisputeSummary(fromDate, toDate)` and wraps the returned `DisputeSummaryResponse` DTO in a **200 OK** response.

```java
// L60
}
```

Closes the `ReportController` class. There are no other fields or methods — the controller is intentionally thin.

## How this connects

`ReportController` is the **entry point** of the reporting feature in the Controller → Service → Repository → Entity/DB chain:

- **Inbound (who calls it):** Spring's `DispatcherServlet` routes HTTP `GET` requests under `/billing/reports` (possibly behind a `/teleConnect` context path) to these three methods, converting query strings into the typed parameters via `@RequestParam` and `@DateTimeFormat`.
- **Downstream (what it calls):** every method delegates to the injected `ReportService` interface (`src/main/java/com/teleconnect/billing_service/service/ReportService.java`), whose three methods — `getOverdueReport(String, String)`, `getCollectionReport(LocalDate, LocalDate, String)`, and `getDisputeSummary(LocalDate, LocalDate)` — match the controller's three handlers one-to-one. The concrete `ReportService` implementation (wired in by `@Autowired`) is where the actual business logic and repository/entity access live; this controller deliberately contains none of it.
- **Data contract:** responses are serialized from the DTOs `OverdueReportResponse`, `CollectionReportResponse`, and `DisputeSummaryResponse` in `com.teleconnect.billing_service.dto.response`, keeping the JSON API shape decoupled from the underlying JPA entities.

In short, this file is pure HTTP plumbing: route mapping, parameter binding/parsing, delegation, and 200-OK wrapping.
