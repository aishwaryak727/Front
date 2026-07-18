# TeleConnect — Microservices Platform

A telecom management platform split into independent Spring Boot microservices,
fronted by a single **API Gateway**.

## Architecture

```
                         ┌──────────────────────────┐
        Client  ───────► │   API Gateway  :8080     │   ← single entry point
                         │  (Spring Cloud Gateway)  │
                         └────────────┬─────────────┘
            routes by URL path prefix │
   ┌───────────────┬──────────────────┼───────────────┬────────────────┐
   ▼               ▼                  ▼               ▼                ▼
 IAM :8081    Subscriber :8082    Plan :8083     Usage :8084     Billing :8085
```

Each service is an independent Spring Boot app with its **own MySQL database**.
The gateway routes incoming requests to the right service based on the URL path.

## Modules

| Module               | Port | Database                 | Responsibility                          |
|----------------------|------|--------------------------|-----------------------------------------|
| `IAM`                | 8081 | `teleConnect`            | Auth (JWT), users, roles, audit logs    |
| `subscriber`         | 8082 | `teleconnect_subscriber` | Subscriber accounts, SIM lines          |
| `Plan`               | 8083 | `teleconnect_plan`       | Plans, add-ons, subscriptions           |
| `Usage`              | 8084 | `teleconnect_db`         | Usage records, summaries, alerts        |
| `Billing`            | 8085 | `teleconnect_billing`    | Invoices, payments, disputes, reports   |
| `gateway`            | 8080 | —                        | API Gateway / single entry point        |
| `teleconnect-shared` | —    | —                        | Shared library (currently empty stubs)  |

## Gateway routes

All requests go through `http://localhost:8080` and are routed by path prefix:

| Path prefix                | Routed to            |
|----------------------------|----------------------|
| `/teleConnect/iam/**`      | IAM (8081)           |
| `/teleConnect/api/**`      | Subscriber (8082)    |
| `/teleConnect/plan/**`     | Plan (8083)          |
| `/teleConnect/usage/**`    | Usage (8084)         |
| `/teleConnect/billing/**`  | Billing (8085)       |

The gateway forwards the full path unchanged (each service already expects the
`/teleConnect/...` prefix), so an external call such as:

```
POST http://localhost:8080/teleConnect/iam/api/auth/login
```

is proxied to `http://localhost:8081/teleConnect/iam/api/auth/login`.

## Prerequisites

- **JDK 21**
- **MySQL** running on `localhost:3306`, user `root`, password `root`
  (databases are auto-created on first run)
- **Corporate network note:** Maven downloads require trusting the corporate
  CA. This project sets `-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT` in
  `.mvn/jvm.config`, which makes the Maven wrapper trust the Windows certificate
  store automatically. No extra setup needed when building with `mvnw`.

## Build

```powershell
.\mvnw.cmd clean install -DskipTests
```

This builds all 5 services + the gateway + the shared module.

## Run everything

```powershell
.\run-all.ps1     # starts all 5 services + gateway (logs in .\logs\)
.\stop-all.ps1    # stops them all
```

Then use the platform through the gateway at **http://localhost:8080**.

## Run a single service (for development)

```powershell
.\mvnw.cmd -pl IAM spring-boot:run
# or run a built jar:
java -jar IAM\target\IAM-1.0.0.jar
```

## Example end-to-end flow

The data chain across services:

```
IAM.User → Subscriber.account → SIM line → Plan.subscription → Usage → Billing.invoice
```

1. **Register / login** via IAM → get a JWT.
   `POST /teleConnect/iam/api/auth/login`
2. **Create a subscriber account** → `POST /teleConnect/api/subscribers`
3. **Add a SIM line** → `POST /teleConnect/api/subscribers/{accountId}/simLines`
4. **Subscribe to a plan** → `POST /teleConnect/plan/createSubscriptions`
5. **Record usage** → `POST /teleConnect/usage/createRecord`
6. **Generate an invoice** → `POST /teleConnect/billing/invoices/generate`

## Notes / current limitations

- **Auth is centralized at IAM** (issues JWT). Subscriber validates the same
  token (shared `jwt.secret`). Plan, Usage and Billing do not yet validate JWT —
  they are open. Unifying token validation across all services (or enforcing it
  at the gateway) is a natural next step.
- **No runtime service-to-service calls yet.** Modules reference each other only
  by IDs supplied in the request body. Wiring real inter-service calls
  (Feign/WebClient) is the deeper integration step.
- **Service discovery** uses static `localhost:<port>` URIs in the gateway. For a
  production setup, add Eureka and switch to `lb://` URIs.
- The Spring Boot parent version is **3.4.1** (was 3.3.4) so the build resolves
  against dependencies cached on this machine.
