# Exploration: automatizacion-crud-api

> Source: Engram #19 | Phase: sdd-explore | Project: AUTO_API_SCREENPLAY

---

## Current State

The project `AUTO_API_SCREENPLAY` is a Gradle 8.5 multi-module Java 17 template with a single `app` module.

**Current build.gradle highlights:**
- Plugin: `id 'application'`
- Dependencies: `junit:junit:4.13.2` (testImplementation), `com.google.guava:guava:32.1.2-jre` (implementation)
- Java toolchain: 17
- Main class: `com.automation.App`
- No REST client, no BDD framework, no Cucumber, no Serenity

**Current test code:** `AppTest.java` — single JUnit 4 `@Test` asserting `true`.

There is NO automation code at all. The project is a blank canvas that needs full scaffolding.

---

## CRUD Mapping

| CRUD | HTTP Method | Endpoint | Notes |
|------|-------------|----------|-------|
| **Create** | POST | `/api/orders` | Returns `tableNumber, tasksCreated, message` — NOT orderId |
| **Read** | GET | `/api/orders/{orderId}/status` | Read order state by ID |
| **Read** | GET | `/api/tasks/station/{station}` | Read tasks by station — used to recover orderId |
| **Update** | PATCH | `/api/tasks/{id}/start` | State transition: PENDING → IN_PREPARATION |
| **Delete** | DELETE | `/api/orders/{orderId}` | Hard delete |

Auth support calls (preconditions, not CRUD on domain):
- POST `/api/auth/register`
- POST `/api/auth/login` → token acquisition

---

## Data Flow and Chain

```
[1] POST /api/auth/register
    Input: { username, email, password }
    Output: 201, no body

[2] POST /api/auth/login
    Input: { identifier, password }
    Output: { "token": "..." }
    Chain → actor.remember("authToken", token)

[3] POST /api/orders
    Input: { tableNumber, customerName, customerEmail, products: [{name, type, price}] }
    Output: { tableNumber, tasksCreated, message }
    ⚠️  No orderId returned → must be recovered indirectly
    Chain → actor.remember("tableNumber", tableNumber)

[4] GET /api/tasks/station/{station}?status=PENDING
    station derived from product type: DRINK→BAR, HOT_DISH→HOT_KITCHEN, COLD_DISH→COLD_KITCHEN
    Output: TaskResponse[] with { id, orderId, station, tableNumber, status, ... }
    Strategy: filter by tableNumber
    Chain → actor.remember("orderId", task.orderId) + actor.remember("taskId", task.id)

[5] PATCH /api/tasks/{taskId}/start
    Output: TaskResponse { status → IN_PREPARATION }

[6] GET /api/orders/{orderId}/status
    Output: { orderId, status: "IN_PREPARATION" }

[7] DELETE /api/orders/{orderId}
    Output: 204 no body

[8] GET /api/orders/{orderId}/status
    Expected: 404 Not Found
```

---

## Risks and Gaps

| # | Risk | Severity | Mitigation |
|---|------|----------|------------|
| R1 | POST /api/orders does NOT return orderId | HIGH | Query GET /api/tasks/station filtered by unique tableNumber |
| R2 | GET /api/tasks/station may return multiple records | MEDIUM | Filter by exact tableNumber; use timestamp-derived tableNumber |
| R3 | Async task creation (eventual consistency) | MEDIUM | Polling mechanism: retry up to 5× with 1s delay |
| R4 | Token lifecycle in parallel execution | LOW | Actor memory is per-instance; never use static fields |
| R5 | DELETE before invoice may leave invalid state | LOW | Scope test to happy-path CRUD only |

---

## Tech Stack Delta Required

**Versions to add to `gradle/libs.versions.toml`:**
```toml
[versions]
serenity     = "4.2.9"
cucumber     = "7.18.1"
restAssured  = "5.4.0"
jackson      = "2.17.2"
lombok       = "1.18.34"

[libraries]
serenityCore         = { module = "net.serenity-bdd:serenity-core",          version.ref = "serenity" }
serenityCucumber     = { module = "net.serenity-bdd:serenity-cucumber",       version.ref = "serenity" }
serenityRestAssured  = { module = "net.serenity-bdd:serenity-rest-assured",   version.ref = "serenity" }
cucumberJava         = { module = "io.cucumber:cucumber-java",                version.ref = "cucumber" }
cucumberJunit        = { module = "io.cucumber:cucumber-junit",               version.ref = "cucumber" }
restAssured          = { module = "io.rest-assured:rest-assured",             version.ref = "restAssured" }
jacksonDatabind      = { module = "com.fasterxml.jackson.core:jackson-databind", version.ref = "jackson" }
lombok               = { module = "org.projectlombok:lombok",                 version.ref = "lombok" }
```

---

## Recommended Serenity Screenplay Project Structure

```
app/src/test/
├── java/com/automation/
│   ├── runners/       → CucumberRunner.java
│   ├── stepdefinitions/ → CrudFlowStepDefs.java
│   ├── tasks/         → RegisterUser, LoginUser, CreateOrder, StartTask, DeleteOrder
│   ├── questions/     → TasksAtStation, OrderStatus
│   ├── models/        → LoginRequest, LoginResponse, CreateOrderRequest,
│   │                    ProductRequest, CreateOrderResponse, TaskResponse
│   └── utils/         → ApiConstants, TestDataGenerator
└── resources/
    ├── features/      → crud_flow.feature
    ├── serenity.properties
    └── cucumber.properties
```

---

## Negative Scenarios (minimum 3)

| # | Scenario | Endpoint | Expected |
|---|----------|----------|----------|
| N1 | Login with invalid password | POST /api/auth/login | 401 |
| N2 | GET status of non-existent order | GET /api/orders/99999/status | 404 |
| N3 | DELETE non-existent order | DELETE /api/orders/99999 | 404 |
| N4 | Invoice for order in invalid state | POST /api/orders/{id}/invoice | 400 |
| N5 | PATCH start non-existent task | PATCH /api/tasks/99999/start | 404 |

---

## Viability Assessment

**VIABLE** — A complete CRUD automation flow is achievable. Design decisions:
1. Use `tableNumber` as surrogate identifier to correlate order with tasks.
2. Implement polling in `TasksAtStation` question (eventual consistency).
3. Derive station from product type at test construction time.
4. Store all state in Actor memory between steps.
5. Remove or disable `AppTest.java` to avoid JUnit 4/Serenity runner conflicts.
