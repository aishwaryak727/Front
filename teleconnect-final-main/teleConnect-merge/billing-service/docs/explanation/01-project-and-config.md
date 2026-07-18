# Part 1 — Project Build, Runtime Config & Application Bootstrap

This part covers the foundational, non-business-logic layer of the TeleConnect billing microservice: the Maven build descriptor (`pom.xml`), the runtime properties (`application.properties`), the Spring Boot entry point (`BillingServiceApplication`), and the two cross-cutting configuration classes (`SchedulingConfig`, `SecurityConfig`). These files do not implement billing rules themselves; instead they assemble the application context, wire in the database, security, scheduling and web stack, and start the JVM process inside which the Controller -> Service -> Repository -> Entity/DB layers later run. Think of them as the scaffolding that boots and configures the whole application before any HTTP request is ever served.

---

## pom.xml

This is the Maven Project Object Model — the build descriptor. It declares the project coordinates, the Spring Boot parent (which pins dependency versions and plugin defaults), every library the service depends on, and the build plugins. It defines *what the application is made of* and *how it is compiled and packaged*.

```xml
// L1-L4
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
```

Line 1 is the standard XML declaration (version + UTF-8 encoding). Lines 2-3 open the root `<project>` element and bind the Maven POM XML namespace plus the `xsi` namespace used for schema validation; `schemaLocation` tells tooling which XSD validates this document. `<modelVersion>4.0.0</modelVersion>` (L4) declares the POM model format — `4.0.0` is the only valid value for modern Maven and is effectively mandatory.

```xml
// L5-L10
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>3.4.1</version>
		<relativePath/>
	</parent>
```

This declares the **parent POM**: `spring-boot-starter-parent` version `3.4.1`. Inheriting from it gives the project Spring Boot's curated **dependency management** (a BOM that pins compatible versions of hundreds of libraries so the `<dependencies>` below can omit `<version>` tags), sensible plugin configuration, Java compiler defaults, and resource filtering. The empty `<relativePath/>` element disables Maven's default behavior of looking for a parent POM in the parent directory on disk, forcing it to resolve the parent from the configured repositories instead.

```xml
// L11-L15
	<groupId>com.teleconnect</groupId>
	<artifactId>billing-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>billing-service</name>
	<description>TeleConnect Billing and Invoice Management Module</description>
```

These are the project's own **Maven coordinates** (the GAV). `groupId` `com.teleconnect` is the organization/namespace, `artifactId` `billing-service` is this module's name, and `version` `0.0.1-SNAPSHOT` marks it as an in-development build (the `-SNAPSHOT` suffix tells Maven this is a mutable, pre-release version). `<name>` and `<description>` are human-readable metadata; the description states this is the TeleConnect billing and invoice management module.

```xml
// L16-L18
	<properties>
		<java.version>21</java.version>
	</properties>
```

The `<properties>` block defines build-wide variables. `java.version` = `21` is a property the Spring Boot parent recognizes: it configures the compiler to use Java 21 for both source and target bytecode levels. So this service compiles against and runs on the Java 21 language/runtime.

```xml
// L19-L23
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
```

Opens the dependency list. The first dependency is **`spring-boot-starter-data-jpa`** — a "starter" (an aggregator dependency that pulls in a coherent set of libraries). This one brings in Spring Data JPA, Hibernate (the default JPA provider/ORM), and the Jakarta Persistence API. It is what lets the application define `@Entity` classes and `Repository` interfaces (the Repository and Entity layers of the architecture) and have Hibernate translate them into SQL. No `<version>` is needed because the parent BOM supplies it.

```xml
// L24-L27
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
```

**`spring-boot-starter-security`** pulls in Spring Security. Its presence is what activates the autoconfiguration that `SecurityConfig` (documented below) customizes — without this dependency the `SecurityFilterChain`, `PasswordEncoder`, and `UserDetailsService` beans would have no framework to plug into.

```xml
// L28-L31
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
```

**`spring-boot-starter-validation`** brings in the Jakarta Bean Validation API and the Hibernate Validator implementation. This is what makes annotations such as `@Valid`, `@NotNull`, `@Size`, etc. functional on controller request bodies and entity fields elsewhere in the codebase.

```xml
// L32-L35
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
```

**`spring-boot-starter-web`** brings in Spring MVC, the Jackson JSON library, and an embedded Tomcat servlet container. This is the dependency that turns the project into a runnable web server able to expose REST controllers and that, together with the `server.*` properties, listens for HTTP requests.

```xml
// L36-L41
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-devtools</artifactId>
			<scope>runtime</scope>
			<optional>true</optional>
		</dependency>
```

**`spring-boot-devtools`** is a development convenience dependency providing automatic application restart on classpath changes and disabled template caching. `<scope>runtime</scope>` means it is present when running but not needed to compile; `<optional>true</optional>` prevents it from being transitively inherited by any project that depends on this one (and Spring Boot also disables devtools automatically when the app runs as a packaged jar).

```xml
// L42-L46
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>
```

The **MySQL JDBC driver** (`mysql-connector-j`). `<scope>runtime</scope>` indicates the code never references the driver classes directly at compile time — it is loaded dynamically at runtime via the `driver-class-name` property. This driver is what physically connects to the MySQL database described in `application.properties`.

```xml
// L47-L51
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
```

**Lombok** is a compile-time annotation processor that generates boilerplate (getters, setters, constructors, builders, etc.) from annotations like `@Data`, `@Builder`, `@Getter`. `<optional>true</optional>` keeps it from leaking to downstream consumers — it is only needed during this project's own compilation, since the generated code is baked into the `.class` files.

```xml
// L52-L56
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
```

**`spring-boot-starter-test`** aggregates the testing stack: JUnit 5 (Jupiter), Mockito, AssertJ, Hamcrest, JSONassert and Spring's own test support (`@SpringBootTest`, `MockMvc`, etc.). `<scope>test</scope>` means these libraries are on the classpath only when compiling and running tests, never in production.

```xml
// L57-L61
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-test</artifactId>
			<scope>test</scope>
		</dependency>
```

**`spring-security-test`** adds Spring Security testing helpers (for example `@WithMockUser` and the `SecurityMockMvcRequestPostProcessors`), letting tests simulate authenticated users. Also `test`-scoped.

```xml
// L62-L66
		<dependency>
			<groupId>org.apache.pdfbox</groupId>
			<artifactId>pdfbox</artifactId>
			<version>3.0.3</version>
		</dependency>
	</dependencies>
```

**Apache PDFBox** version `3.0.3` is a library for creating and manipulating PDF documents — in a billing service this is almost certainly used to generate invoice PDFs in the service layer. Note that, unlike the Spring starters, this dependency pins an explicit `<version>` because it is not managed by the Spring Boot parent BOM. The `</dependencies>` tag closes the dependency block.

```xml
// L69-L82
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<excludes>
						<exclude>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</exclude>
					</excludes>
				</configuration>
			</plugin>
```

The `<build>` section configures build plugins. The **`spring-boot-maven-plugin`** is what repackages the compiled output into an executable "fat jar" (a self-contained jar that includes all dependencies and the embedded Tomcat) and enables `mvn spring-boot:run`. Its `<excludes>` configuration removes Lombok from the packaged jar — correct, because Lombok is only a build-time tool and its classes are not needed at runtime.

```xml
// L83-L97
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<configuration>
					<annotationProcessorPaths>
						<path>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</path>
					</annotationProcessorPaths>
				</configuration>
			</plugin>
		</plugins>
	</build>
</project>
```

The **`maven-compiler-plugin`** is configured with an `<annotationProcessorPaths>` entry registering Lombok as an annotation processor. This is required so that, during `javac`, Lombok runs and generates the getter/setter/builder code from its annotations. The closing `</plugins>`, `</build>`, and `</project>` tags end the build section and the document.

---

## src/main/resources/application.properties

This is the externalized runtime configuration. Spring Boot loads it automatically at startup and uses these key/value pairs to configure the data source, the JPA/Hibernate behavior, JSON serialization, and the embedded web server.

```properties
// L1
spring.application.name=billing-service
```

Sets the logical name of the application to `billing-service`. This name appears in startup logs and is used by management/observability tooling (and would be the service ID if this app registered with a service registry).

```properties
// L3-L7
# MySQL DataSource
spring.datasource.url=jdbc:mysql://localhost:3306/teleconnect_billing?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

These four properties (after the `#` comment line) define the JDBC **DataSource** — the connection to the database used by the Repository/Entity layers. The URL points to a MySQL server on `localhost:3306`, database `teleconnect_billing`, with query parameters: `createDatabaseIfNotExist=true` (MySQL creates the schema automatically if missing), `useSSL=false` (no TLS for the DB connection), `serverTimezone=UTC` (interpret timestamps in UTC), and `allowPublicKeyRetrieval=true` (permits the client to fetch the server's public key during authentication — needed for MySQL's caching_sha2 auth). `username`/`password` are both `root`, and `driver-class-name` explicitly names the MySQL Connector/J driver class.

*Aside: hardcoding `root`/`root` credentials and disabling SSL are fine for local development but are security concerns for any non-local environment; they should be externalized (environment variables or a secrets store) before deployment.*

```properties
// L9-L13
# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

JPA/Hibernate tuning. `ddl-auto=update` tells Hibernate to inspect the entity mappings at startup and **alter the database schema to match** (creating missing tables/columns but not dropping anything) — convenient in development, risky in production where managed migrations are preferred. `show-sql=true` logs every generated SQL statement to the console. `hibernate.format_sql=true` pretty-prints that logged SQL across multiple lines for readability. `hibernate.dialect=org.hibernate.dialect.MySQLDialect` tells Hibernate to generate MySQL-flavored SQL.

```properties
// L15-L17
# Jackson - Date format
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.date-format=yyyy-MM-dd
```

Configures Jackson (the JSON library used by Spring MVC to serialize/deserialize REST payloads). `write-dates-as-timestamps=false` disables rendering dates as numeric epoch values, and `date-format=yyyy-MM-dd` formats dates as ISO-style calendar dates (e.g. `2026-06-15`) in JSON. Together these make date fields in API responses human-readable strings rather than numbers.

```properties
// L19-L21
# Server
server.port=8085
server.servlet.context-path=/teleConnect
```

Configures the embedded Tomcat server. `server.port=8085` makes the app listen on TCP port 8085 instead of the default 8080. `server.servlet.context-path=/teleConnect` prefixes every endpoint with `/teleConnect`, so a controller mapped to `/invoices` is actually reachable at `http://localhost:8085/teleConnect/invoices`.

---

## src/main/java/com/teleconnect/billing_service/BillingServiceApplication.java

This is the application's **entry point** — the class containing `main()` that boots the entire Spring application context and starts the embedded web server.

```java
// L1
package com.teleconnect.billing_service;
```

Declares the package. This is the **root/base package** of the application: because the main class lives here, Spring's component scanning (triggered by `@SpringBootApplication`) defaults to scanning this package and everything beneath it (`config`, `controller`, `service`, `repository`, `entity`, etc.).

```java
// L3-L4
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
```

Imports the two Spring Boot types used below: `SpringApplication` (the bootstrap helper) and the `@SpringBootApplication` annotation.

```java
// L6-L7
@SpringBootApplication
public class BillingServiceApplication {
```

**`@SpringBootApplication`** is the central Spring Boot meta-annotation. It bundles three things: (1) `@SpringBootConfiguration` — marks this class as a source of bean definitions; (2) `@EnableAutoConfiguration` — tells Spring Boot to auto-configure beans based on the libraries on the classpath (e.g. set up a DataSource because JPA + MySQL are present, set up Spring MVC because the web starter is present, set up Spring Security because that starter is present); and (3) `@ComponentScan` — automatically discovers and registers `@Component`/`@Service`/`@Repository`/`@Controller`/`@Configuration` beans in this package and its sub-packages. This single annotation is what makes the rest of the layered application wire itself together. `public class BillingServiceApplication` declares the class itself.

```java
// L9-L11
	public static void main(String[] args) {
		SpringApplication.run(BillingServiceApplication.class, args);
	}
```

The standard Java `main` method — the JVM's starting point. Its parameter `String[] args` carries command-line arguments. The single statement `SpringApplication.run(BillingServiceApplication.class, args)` bootstraps Spring Boot: it creates the application context, runs auto-configuration, performs component scanning starting from `BillingServiceApplication`, instantiates all beans (controllers, services, repositories, the security and scheduling config below), starts the embedded Tomcat on port 8085, and then blocks while the server runs. The `args` are forwarded so they can override properties or be consumed by command-line runners.

```java
// L13
}
```

Closes the class. There is no other logic here by design — Spring Boot entry points are intentionally minimal.

---

## src/main/java/com/teleconnect/billing_service/config/SchedulingConfig.java

A tiny configuration class whose sole purpose is to enable Spring's scheduled-task support across the application, so that `@Scheduled` methods elsewhere (for example, periodic invoice generation or overdue-billing jobs) actually run.

```java
// L1
package com.teleconnect.billing_service.config;
```

Declares that this class lives in the `config` sub-package — within the base package, so it is picked up by the component scan started in `BillingServiceApplication`.

```java
// L3-L4
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
```

Imports the two annotations applied below: `@Configuration` and `@EnableScheduling`.

```java
// L6-L9
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

**`@Configuration`** marks this as a Spring configuration class — a source of bean definitions and configuration that Spring processes at startup (it is also itself registered as a bean). **`@EnableScheduling`** activates Spring's task-scheduling infrastructure: it registers the post-processor that scans all beans for methods annotated with `@Scheduled` and arranges for them to be invoked on a background thread according to their cron expression, fixed rate, or fixed delay. The class body is intentionally empty (`{ }`) — its only job is to host these two annotations. Without this class (or `@EnableScheduling` somewhere), any `@Scheduled` methods in the service layer would simply never fire.

---

## src/main/java/com/teleconnect/billing_service/config/SecurityConfig.java

This class configures Spring Security for the service. It defines the HTTP security filter chain (CSRF, session policy, authorization rules), the password encoder, and an in-memory set of users. It is the cross-cutting layer that sits in front of all Controller endpoints and decides what is permitted.

```java
// L1
package com.teleconnect.billing_service.config;
```

Same `config` sub-package as `SchedulingConfig`; discovered by component scanning.

```java
// L3-L16
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
```

The imports bring in the Spring Security building blocks used below. The notable ones: `@Bean` and `@Configuration` (core Spring); `HttpSecurity` (the builder used to configure web security); `@EnableWebSecurity` (turns on web security); `AbstractHttpConfigurer` (used to disable CSRF via a method reference); `SessionCreationPolicy` (enum for session handling, here used for STATELESS); `User`/`UserDetails`/`UserDetailsService` (the user-model abstraction Spring Security authenticates against); `BCryptPasswordEncoder`/`PasswordEncoder` (password hashing); `InMemoryUserDetailsManager` (an implementation that stores users in memory rather than a database); and `SecurityFilterChain` (the bean type representing the configured chain of security filters). `Customizer` is imported but, as noted later, is not actually used here.

```java
// L18-L20
@Configuration
@EnableWebSecurity
public class SecurityConfig {
```

`@Configuration` marks this as a bean-defining configuration class. **`@EnableWebSecurity`** switches on Spring Security's web security support and lets this class supply a custom `SecurityFilterChain`, overriding Spring Boot's default secured-by-everything behavior.

```java
// L22-L23
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
```

**`@Bean`** declares that the object returned by this method should be registered in the Spring container as a managed bean. Here the method `filterChain` produces a `SecurityFilterChain` — the ordered set of servlet filters Spring Security applies to every incoming HTTP request. Spring injects the configured `HttpSecurity` builder as the `http` parameter. The method declares `throws Exception` because the builder's configuration calls are checked-exception-prone.

```java
// L24-L25
        http
            .csrf(AbstractHttpConfigurer::disable)
```

Begins configuring the `http` builder. `.csrf(AbstractHttpConfigurer::disable)` **disables CSRF (Cross-Site Request Forgery) protection** using a method reference to `AbstractHttpConfigurer::disable`. CSRF protection is primarily relevant for browser session/cookie-based apps; disabling it is the conventional choice for a stateless REST API (which this is, per the next lines), where clients authenticate per-request rather than via a session cookie.

```java
// L26-L27
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

Configures session management with a lambda. `SessionCreationPolicy.STATELESS` tells Spring Security **never to create or use an HTTP session** — no `JSESSIONID`, no server-side session state. Every request must carry its own credentials and is authenticated independently, which is the standard model for a REST microservice.

```java
// L28-L30
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
```

Configures URL authorization. `.anyRequest().permitAll()` means **every request to every endpoint is allowed without authentication**. So although users and roles are defined further down, no endpoint is actually protected by them as configured — the entire API is open.

*Aside: this is a notable point. The class defines a `UserDetailsService` with `BILLING`/`ADMIN` users and roles, yet `permitAll()` makes authentication and those roles effectively unused for HTTP access control. This may be intentional during development, but as written the security model is "wide open." It looks like the role definitions are scaffolding awaiting `.authenticated()` / `.hasRole(...)` rules that have not yet been added.*

```java
// L32-L33
        return http.build();
    }
```

Calls `http.build()` to finalize the configuration and produce the `SecurityFilterChain` instance, which is returned and registered as the security bean. The closing brace ends the method.

```java
// L35-L38
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
```

Another `@Bean` method exposing a `PasswordEncoder`. It returns a new **`BCryptPasswordEncoder`**, which hashes passwords using the BCrypt adaptive hashing algorithm (with a per-hash random salt). Registering it as a bean makes it injectable elsewhere; here it is used by `userDetailsService` below to hash the in-memory users' passwords, and it would also be used to verify submitted passwords during authentication.

```java
// L40-L41
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
```

A `@Bean` method producing a **`UserDetailsService`** — the interface Spring Security uses to look up user accounts by username during authentication. Spring injects the `PasswordEncoder` bean (defined just above) as the `encoder` parameter, so passwords can be hashed when the users are created.

```java
// L42-L46
        UserDetails billingUser = User.builder()
                .username("billing")
                .password(encoder.encode("billing123"))
                .roles("BILLING")
                .build();
```

Builds the first user via the fluent `User.builder()` API. Username `billing`, password `billing123` stored as a BCrypt hash (via `encoder.encode(...)`), and a single role `BILLING`. `.build()` produces the immutable `UserDetails` object. (Internally Spring prefixes roles with `ROLE_`, so this user holds authority `ROLE_BILLING`.)

```java
// L48-L52
        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN", "BILLING")
                .build();
```

Builds a second user: username `admin`, password `admin123` (also BCrypt-hashed), with two roles — `ADMIN` and `BILLING` (i.e. authorities `ROLE_ADMIN` and `ROLE_BILLING`). This represents a privileged account that has both administrative and billing access.

*Aside: as with the data source credentials, hardcoded plaintext passwords (`billing123`, `admin123`) and in-memory users are appropriate only for development/demo. A production system would back `UserDetailsService` with the database and externalize credentials.*

```java
// L54-L56
        return new InMemoryUserDetailsManager(billingUser, admin);
    }
}
```

Returns an **`InMemoryUserDetailsManager`** seeded with the two users. This implementation of `UserDetailsService` keeps the accounts entirely in memory (lost on restart, not persisted to the database). The final braces close the method and the class.

---

## How this connects

These five artifacts form the base on which the rest of the architecture stands. `pom.xml` supplies the libraries — Spring MVC/Web (for the **Controller** layer), Spring Data JPA + Hibernate + the MySQL driver (for the **Repository** and **Entity** layers and the **DB**), Spring Security, Validation, Lombok, and PDFBox. `application.properties` then configures those libraries at runtime: the DataSource the repositories talk to, the Hibernate schema/dialect behavior that maps entities to tables, the Jackson date formatting controllers use in JSON responses, and the port/context-path Tomcat exposes. `BillingServiceApplication.main()` is the ignition: its `@SpringBootApplication` triggers component scanning of `com.teleconnect.billing_service`, which discovers and instantiates every `@Configuration`, `@RestController`, `@Service`, and `@Repository` bean — including the two config classes here. `SchedulingConfig` enables any `@Scheduled` background jobs in the service layer, and `SecurityConfig` installs the filter chain that fronts every controller endpoint (currently wide-open via `permitAll()`, with `BILLING`/`ADMIN` users and a BCrypt encoder defined but not yet enforced). With this scaffolding in place, an HTTP request flows: Tomcat -> Spring Security filter chain -> Controller -> Service -> Repository -> Hibernate -> MySQL, and back.
