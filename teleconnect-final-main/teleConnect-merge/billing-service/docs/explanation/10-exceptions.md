# Exception Handling Layer

This part of the billing microservice defines two custom unchecked exceptions (`BillingException`, `ResourceNotFoundException`) and a single global error handler (`GlobalExceptionHandler`) that converts any exception thrown anywhere in the request-handling pipeline into a clean, structured JSON HTTP error response. In the standard Controller -> Service -> Repository -> Entity/DB layering, exceptions are typically *thrown* deep in the Service layer (for example, when a lookup against the Repository returns nothing, or when a business rule is violated) and *bubble up* through the Controller without being caught there. The `GlobalExceptionHandler` sits to the side of all controllers as a cross-cutting concern: Spring routes any uncaught exception to the matching handler method here, which then builds the response body and the correct HTTP status code. This keeps controllers free of repetitive `try/catch` blocks and guarantees a consistent error shape across every endpoint.

---

## src/main/java/com/teleconnect/billing_service/exception/BillingException.java

This file defines the application's general-purpose business exception. It is thrown when a request is syntactically valid but violates a business rule (for example, an invalid state transition or a disallowed operation), and the global handler maps it to HTTP 400 Bad Request.

```java
// L1
package com.teleconnect.billing_service.exception;
```

The `package` statement declares the namespace this class belongs to: `com.teleconnect.billing_service.exception`. In Java, the package must mirror the directory structure on disk (`.../com/teleconnect/billing_service/exception/`). Placing all three exception-related classes in the same package keeps the error-handling concern grouped together and lets them reference each other without imports.

```java
// L3
public class BillingException extends RuntimeException {
```

This declares a `public` class named `BillingException` that `extends RuntimeException`. `RuntimeException` is Java's base class for *unchecked* exceptions, meaning the compiler does not force callers to declare `throws BillingException` or wrap calls in `try/catch`. This is the conventional choice for Spring applications: service methods can throw it freely, it propagates up through the controller automatically, and the `GlobalExceptionHandler` catches it centrally. Because it extends `RuntimeException` (and not the checked `Exception`), it carries all the standard exception machinery (message, cause, stack trace) for free.

```java
// L5-L7
    public BillingException(String message) {
        super(message);
    }
```

This is the single constructor. It takes one parameter, `String message` — a human-readable description of what went wrong (for example, `"Cannot generate bill for an inactive account"`). The body calls `super(message)`, which invokes the `RuntimeException(String)` constructor, storing the text so it can later be retrieved via `getMessage()`. That stored message is exactly what the global handler reads (`ex.getMessage()`) and places into the JSON response body. There is no no-argument constructor and no cause-accepting constructor, so callers must always supply a message.

```java
// L8
}
```

The closing brace ends the class definition. The class is intentionally minimal: it adds no fields or methods of its own and exists purely to give business-rule failures a distinct *type* that the handler can match against separately from other exceptions.

---

## src/main/java/com/teleconnect/billing_service/exception/ResourceNotFoundException.java

This file defines a more specific exception for the case where a requested entity (an account, invoice, plan, etc.) does not exist. The global handler maps it to HTTP 404 Not Found, distinguishing "you asked for something that isn't here" from generic business errors.

```java
// L1
package com.teleconnect.billing_service.exception;
```

Same package as the other exception classes (`com.teleconnect.billing_service.exception`), keeping all error types together.

```java
// L3
public class ResourceNotFoundException extends RuntimeException {
```

Declares a `public` class `ResourceNotFoundException` that, like `BillingException`, `extends RuntimeException` and is therefore unchecked. Note that it extends `RuntimeException` directly and does **not** extend `BillingException`; the two are *siblings*, not parent/child. That is deliberate: it lets the `GlobalExceptionHandler` register a separate handler for each so they can produce different HTTP status codes (404 versus 400). Typical usage is in the Service layer, for example: `repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Account " + id + " not found"))`.

```java
// L5-L7
    public ResourceNotFoundException(String message) {
        super(message);
    }
```

A single message-only constructor that delegates to `super(message)` (the `RuntimeException(String)` constructor), storing the description for later retrieval via `getMessage()`. The handler reads that message and returns it to the client in the 404 response body.

```java
// L8
}
```

Closes the class. Like `BillingException`, it is intentionally bare — its purpose is to provide a distinct exception *type* for "resource missing," not to add behavior.

---

## src/main/java/com/teleconnect/billing_service/exception/GlobalExceptionHandler.java

This is the centralized, application-wide error handler. Annotated with `@RestControllerAdvice`, it intercepts exceptions thrown by any controller, matches each to a handler method by exception type, and returns a uniform JSON body with an HTTP status code — so every endpoint reports errors the same way without per-controller boilerplate.

```java
// L1
package com.teleconnect.billing_service.exception;
```

Same `exception` package as the two custom exceptions, so this handler can reference `BillingException` and `ResourceNotFoundException` directly without importing them.

```java
// L3-L9
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
```

These imports bring in the Spring web/MVC types used below:
- `HttpStatus` — an enum of HTTP status codes (e.g. `NOT_FOUND` = 404, `BAD_REQUEST` = 400, `INTERNAL_SERVER_ERROR` = 500). It exposes `.value()` for the numeric code and `.getReasonPhrase()` for the standard text (e.g. "Not Found").
- `ResponseEntity` — a generic wrapper representing a complete HTTP response: status code, headers, and body. `ResponseEntity<T>` lets a handler return both a payload and an explicit status.
- `FieldError` — Spring's representation of a single failed field validation, exposing the offending field name and its error message.
- `MethodArgumentNotValidException` — thrown by Spring MVC when a `@Valid`-annotated request body fails bean validation (e.g. a `@NotNull` or `@Size` constraint is violated). It carries the binding result containing all field errors.
- `@ExceptionHandler` — marks a method as the handler for one or more exception types.
- `@RestControllerAdvice` — marks the whole class as a global, REST-aware exception-handling advice (explained at L15).
- `MethodArgumentTypeMismatchException` — thrown when a request parameter or path variable cannot be converted to the method-argument type (e.g. passing `abc` where a `Long id` is expected).

```java
// L11-L13
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
```

Standard-library imports:
- `LocalDateTime` — a date-and-time value (no time zone), used to stamp each error response with the time it was produced.
- `LinkedHashMap` — a `Map` implementation that preserves *insertion order*. Using it (rather than a plain `HashMap`) means the JSON keys appear in the order they were added (`timestamp`, then `status`, then `error`, then `message`/`errors`), giving readable, consistent output.
- `Map` — the interface type used for the response body and the field-error collection.

```java
// L15-L16
@RestControllerAdvice
public class GlobalExceptionHandler {
```

`@RestControllerAdvice` is a Spring annotation that combines `@ControllerAdvice` and `@ResponseBody`. `@ControllerAdvice` registers this class as a global advice component applied across *all* controllers in the application; `@ResponseBody` means every value returned from its handler methods is serialized directly into the HTTP response body (as JSON, given the typical Jackson setup) rather than being interpreted as a view name. In practical terms: whenever a controller throws an exception that isn't caught locally, Spring looks through this class for an `@ExceptionHandler` method whose declared exception type matches (most-specific match wins), invokes it, and writes the returned object as the JSON response. The class itself holds no fields and no constructor — it is a stateless collection of handler methods, instantiated and managed as a Spring bean.

```java
// L18-L21
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }
```

`@ExceptionHandler(ResourceNotFoundException.class)` registers `handleResourceNotFound` as the handler invoked whenever a `ResourceNotFoundException` (defined in the file above) propagates out of a controller. The method receives the thrown exception instance as `ex`. It returns `ResponseEntity<Map<String, Object>>` — an HTTP response whose body is a map of string keys to arbitrary values (which serializes to a JSON object). The single line delegates to the private `buildError` helper (L56) with `HttpStatus.NOT_FOUND` (404) and the exception's message (`ex.getMessage()` — the text passed into the constructor). So a missing-resource error becomes a 404 with the descriptive message in the body.

```java
// L23-L26
    @ExceptionHandler(BillingException.class)
    public ResponseEntity<Map<String, Object>> handleBillingException(BillingException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
```

This handler is bound to `BillingException`. It mirrors the previous method but uses `HttpStatus.BAD_REQUEST` (400), reflecting that a business-rule violation is a client-side problem (the request was understood but cannot be fulfilled). Again it forwards the exception's message into `buildError`. Because `BillingException` and `ResourceNotFoundException` are sibling types, Spring routes each to its own handler and they yield different status codes.

```java
// L28-L35
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format(
                "Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return buildError(HttpStatus.BAD_REQUEST, message);
    }
```

This handler catches `MethodArgumentTypeMismatchException`, which Spring raises before the controller body even runs when an incoming parameter cannot be converted to the expected Java type (for example, `?id=abc` for a `Long` path variable). It builds a friendly, specific message with `String.format`, filling three placeholders:
- `ex.getValue()` — the raw, unconvertible value the client sent (the `%s` after "Invalid value").
- `ex.getName()` — the name of the parameter that failed conversion.
- The expected type name. The ternary `ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"` guards against a `null` required type: if Spring knows the target type, it uses the short class name (e.g. `Long`); otherwise it falls back to the literal string `"unknown"`. This null-check prevents a `NullPointerException` while formatting the message.

The composed message is returned via `buildError` with `HttpStatus.BAD_REQUEST` (400), since a malformed parameter is a client error.

```java
// L37-L49
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
```

This handler catches `MethodArgumentNotValidException`, raised when a `@Valid`-annotated request body fails bean-validation constraints. Unlike the other handlers, it does **not** call `buildError`, because it needs to return *multiple* field-level errors rather than a single message. Step by step:
- It creates `fieldErrors`, an order-preserving `LinkedHashMap<String, String>` mapping each invalid field name to its validation message.
- The `for` loop iterates over `ex.getBindingResult().getFieldErrors()`. `getBindingResult()` returns the validation result Spring produced while binding the request body, and `getFieldErrors()` returns the list of `FieldError` objects, one per failed constraint. For each, it puts an entry keyed by `fieldError.getField()` (the property name, e.g. `amount`) with value `fieldError.getDefaultMessage()` (the constraint's message, e.g. `must be greater than 0`).
- It then builds the outer `body` map, also a `LinkedHashMap`, with keys in this order: `"timestamp"` set to `LocalDateTime.now()`; `"status"` set to `HttpStatus.BAD_REQUEST.value()` (the integer `400`); `"error"` set to the literal `"Validation Failed"`; and `"errors"` set to the nested `fieldErrors` map.
- Finally it returns `ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)`, i.e. a 400 response whose JSON body contains a nested `errors` object listing every field that failed validation.

*Aside: if there are multiple validation errors on the same field, the loop's `put` would overwrite earlier entries, so only the last message per field survives. Also note this handler only collects field-level errors via `getFieldErrors()` and ignores any global/object-level errors (`getGlobalErrors()`); for typical DTO field constraints this is fine, but object-level constraint violations would not appear in the response.*

```java
// L51-L54
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getMessage());
    }
```

This is the catch-all fallback. `@ExceptionHandler(Exception.class)` matches any exception type not handled by a more specific method above (Spring always prefers the most specific match, so this only fires for unanticipated errors). It returns a 500 `INTERNAL_SERVER_ERROR` via `buildError`, prefixing the exception's message with `"An unexpected error occurred: "`. This guarantees that even unexpected failures (e.g. an `NullPointerException` or a database/IO error) produce the same structured JSON shape rather than leaking a raw stack trace or Spring's default error page.

*Aside: because this handler concatenates `ex.getMessage()` into the client-facing response, internal error details can be exposed to callers. That is convenient during development but is something to weigh for production, where leaking internal messages can be a minor information-disclosure concern.*

```java
// L56-L63
    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
```

`buildError` is the private helper shared by the single-message handlers (`handleResourceNotFound`, `handleBillingException`, `handleTypeMismatch`, `handleGeneral`). It takes the desired `HttpStatus` and a `message` string, and returns a `ResponseEntity<Map<String, Object>>`. It assembles an order-preserving `LinkedHashMap` body with four keys:
- `"timestamp"` — `LocalDateTime.now()`, the moment the error was created.
- `"status"` — `status.value()`, the numeric HTTP code (e.g. `404`).
- `"error"` — `status.getReasonPhrase()`, the standard reason text (e.g. `"Not Found"`).
- `"message"` — the supplied human-readable detail.

It returns `ResponseEntity.status(status).body(body)`, producing a response with the matching HTTP status and this JSON body. Centralizing this construction keeps the error shape (`timestamp`/`status`/`error`/`message`) identical across all single-message handlers; the validation handler at L37 is the only one that deviates, using `"errors"` (a nested map) instead of `"message"`.

```java
// L64
}
```

Closes the class. There are no other members — `GlobalExceptionHandler` is purely a set of exception-to-response mappings plus the shared builder.

---

## How this connects

- **Custom exceptions (`BillingException`, `ResourceNotFoundException`)** are typically thrown in the **Service layer** when business rules fail or a Repository lookup (e.g. `findById(...)`) returns no entity. Being unchecked `RuntimeException`s, they propagate up through the **Controller** without needing `try/catch`.
- **`GlobalExceptionHandler`** acts as a cross-cutting advice over *all* controllers. Spring routes each propagated exception to the most specific matching `@ExceptionHandler`: `ResourceNotFoundException` -> 404, `BillingException` -> 400, type-mismatch on a parameter -> 400, `@Valid` body failures -> 400 with per-field errors, and anything else -> 500.
- It depends on Spring MVC's `ResponseEntity`/`HttpStatus` to set status codes and on Jackson (via `@ResponseBody`, supplied by `@RestControllerAdvice`) to serialize the `Map` bodies into JSON.
- The validation handler (`MethodArgumentNotValidException`) ties directly to **DTOs annotated with Jakarta Validation constraints** (`@NotNull`, `@Size`, etc.) combined with `@Valid` on controller method parameters — so this layer is the consumer of the validation rules declared on the request models. Together these pieces ensure the Entity/DB and business layers can fail loudly, while clients always receive a clean, consistent HTTP error contract.
