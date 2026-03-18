# Spec: automatizacion-crud-api — Full CRUD API Automation
## Serenity BDD 4.x + Screenplay REST + Cucumber 7 + REST Assured 5

> Source: Engram #21 | Phase: sdd-spec | Project: AUTO_API_SCREENPLAY

---

## 1. Scope

Build an automated Serenity BDD Screenplay REST test suite that exercises the complete logical CRUD lifecycle of the `Order` domain over a live REST API. The suite SHALL cover: user registration (precondition), JWT-based auth, order creation, indirect orderId extraction via tasks, task status update, order status read, order deletion, and post-delete 404 verification. All interactions MUST be implemented as Screenplay Tasks and Questions with Actor memory for inter-step data passing.

---

## 2. Out of Scope

| Excluded Item | Reason |
|---------------|--------|
| POST /api/orders/{id}/invoice | Not part of the CRUD lifecycle; N4 negative case is optional |
| Multi-product / multi-station orders | Adds complexity without increasing CRUD coverage |
| Performance, load, or concurrency testing | Out of current change scope |
| Backend code changes | Test-only change |
| UI or non-REST automation | REST API only |
| Environment provisioning / DB seeding | Documented as prerequisite, not automated here |

---

## 3. Prerequisites

| Prerequisite | Rule |
|--------------|------|
| Server running | API server MUST be running at `serenity.base.url` before test execution |
| Base URL | MUST be configurable via `serenity.properties` property `serenity.base.url` |
| JDK | MUST be JDK 17 |
| Gradle | MUST be Gradle 8.x |
| Clean test data | `tableNumber` uniqueness strategy is sufficient |
| AppTest.java | MUST be disabled or deleted to avoid runner conflict |

---

## 4. Functional Scenarios

### S01 — Register User

| | |
|---|---|
| **GIVEN** | a unique username, email (`qa_auto_{ts}@test.com`) and password (`Test@1234`) are generated |
| **WHEN** | POST /api/auth/register is called with that payload |
| **THEN** | response status code SHALL be 201 |
| **AND** | response body SHALL be empty |

### S02 — Login and Token Extraction

| | |
|---|---|
| **GIVEN** | the user registered in S01 exists |
| **WHEN** | POST /api/auth/login is called with `{identifier: email, password}` |
| **THEN** | response status code SHALL be 200 |
| **AND** | field `$.token` MUST be present, non-null, and non-empty |
| **AND** | the token MUST be stored in Actor memory as `authToken` |

### S03 — Create Order (CREATE)

| | |
|---|---|
| **GIVEN** | a valid `authToken` and unique `tableNumber` (int, 1000–9999) are in Actor memory |
| **WHEN** | POST /api/orders is called with Bearer token and `{tableNumber, customerName, customerEmail, products:[{name:"Coffee", type:"DRINK", price:3.50}]}` |
| **THEN** | response status code SHALL be 201 |
| **AND** | `$.tasksCreated` MUST be exactly 1 |
| **AND** | `$.tableNumber` MUST equal the value sent |
| **AND** | response body MUST NOT contain an `orderId` field |

### S04 — Retrieve Tasks by Station (READ indirect + orderId/taskId extraction)

| | |
|---|---|
| **GIVEN** | the order from S03 was created |
| **WHEN** | GET /api/tasks/station/BAR?status=PENDING is called with Bearer (retrying up to 5× with 1s delay) |
| **THEN** | response status code SHALL be 200 |
| **AND** | array MUST contain at least one element where `tableNumber == <expected>` |
| **AND** | that element's `status` MUST be `"PENDING"` and `station` MUST be `"BAR"` |
| **AND** | `orderId` and `taskId` from that element MUST be stored in Actor memory |

### S05 — Start Task (UPDATE)

| | |
|---|---|
| **GIVEN** | `taskId` is in Actor memory from S04 |
| **WHEN** | PATCH /api/tasks/{taskId}/start is called with Bearer |
| **THEN** | response status code SHALL be 200 |
| **AND** | `$.status` MUST be `"IN_PREPARATION"` |
| **AND** | `$.id` MUST equal the `taskId` sent |

### S06 — Get Order Status (READ)

| | |
|---|---|
| **GIVEN** | `orderId` is in Actor memory from S04 |
| **WHEN** | GET /api/orders/{orderId}/status is called with Bearer |
| **THEN** | response status code SHALL be 200 |
| **AND** | `$.status` MUST be `"IN_PREPARATION"` |
| **AND** | `$.orderId` MUST equal the stored `orderId` |

### S07 — Delete Order (DELETE)

| | |
|---|---|
| **GIVEN** | `orderId` is in Actor memory from S04 |
| **WHEN** | DELETE /api/orders/{orderId} is called with Bearer |
| **THEN** | response status code SHALL be 204 |
| **AND** | response body SHALL be empty |

### S08 — Verify 404 After Delete (READ post-delete)

| | |
|---|---|
| **GIVEN** | the order was deleted in S07 |
| **WHEN** | GET /api/orders/{orderId}/status is called with Bearer |
| **THEN** | response status code SHALL be 404 |

---

## 5. Negative Scenarios

### SN01 — Invalid Login (401)

| | |
|---|---|
| **GIVEN** | an email and password that do not match any registered account |
| **WHEN** | POST /api/auth/login is called with `{identifier:"noexiste@test.com", password:"wrongpassword"}` |
| **THEN** | response status code SHALL be 401 |

### SN02 — GET Status of Non-Existent Order (404)

| | |
|---|---|
| **GIVEN** | `orderId = 99999` does not exist |
| **WHEN** | GET /api/orders/99999/status with valid Bearer |
| **THEN** | response status code SHALL be 404 |

### SN03 — DELETE Non-Existent Order (404)

| | |
|---|---|
| **GIVEN** | `orderId = 99999` does not exist |
| **WHEN** | DELETE /api/orders/99999 with valid Bearer |
| **THEN** | response status code SHALL be 404 |

---

## 6. Data Specification

| Field | Strategy | Format | Constraint |
|-------|----------|--------|------------|
| `username` | `"qa_auto_" + System.currentTimeMillis()` | String | Unique per run |
| `email` | `"qa_auto_" + ts + "@test.com"` | RFC 5321 email | Unique per run |
| `password` | Constant `"Test@1234"` | String ≥6 chars | Fixed |
| `tableNumber` | `(int)(System.currentTimeMillis() % 9000) + 1000` | Integer 1000–9999 | Unique per run |
| `customerName` | Constant `"Automation QA"` | String | Fixed |
| `product.type` | Constant `"DRINK"` | Enum: DRINK, HOT_DISH, COLD_DISH | Fixed for main flow |
| `product.price` | `3.50` | Double | Positive decimal |
| Negative IDs | `99999` | Long | High value, unlikely to collide |

**Station derivation:** `DRINK → BAR` | `HOT_DISH → HOT_KITCHEN` | `COLD_DISH → COLD_KITCHEN`

---

## 7. Validation Requirements

| Step | Status | Body presence | Field constraints | Consistency check |
|------|--------|---------------|-------------------|-------------------|
| S01 Register | 201 | MUST be empty | — | — |
| S02 Login | 200 | MUST contain `token` | token non-null, non-empty | token stored in Actor |
| S03 Create Order | 201 | `{tableNumber, tasksCreated, message}` | tasksCreated == 1; tableNumber matches | orderId MUST NOT be in response |
| S04 Get Tasks | 200 | Array of TaskResponse | task with matching tableNumber; status=="PENDING"; station=="BAR" | orderId + taskId stored |
| S05 Start Task | 200 | TaskResponse | status=="IN_PREPARATION"; id==taskId | State changed from PENDING |
| S06 Order Status | 200 | `{orderId, status}` | status=="IN_PREPARATION"; orderId matches | Reflects task update |
| S07 Delete | 204 | MUST be empty | — | — |
| S08 Post-Delete | 404 | — | — | Same orderId that was 200 is now 404 |
| SN01 Bad Login | 401 | — | — | — |
| SN02 GET 99999 | 404 | — | — | — |
| SN03 DELETE 99999 | 404 | — | — | — |

---

## 8. Non-Functional Requirements

| # | Requirement | Rule |
|---|-------------|------|
| NFR1 | Polling for task materialization | `TasksAtStation` MUST retry up to 5× with 1s delay |
| NFR2 | Bearer token thread safety | Token MUST be in Actor memory only; never in a static field |
| NFR3 | Test isolation | Each Scenario MUST generate independent `username`, `email`, and `tableNumber` |
| NFR4 | No hardcoded base URL | Base URL MUST come from `serenity.base.url` |
| NFR5 | Serenity reports | `./gradlew test aggregate` MUST generate HTML report under `app/build/reports/serenity/` |

---

## 9. Gradle Dependency Requirements

### gradle/libs.versions.toml — new entries

```toml
[versions]
serenity            = "4.1.20"
cucumber            = "7.15.0"
restAssured         = "5.4.0"
jacksonDatabind     = "2.16.1"
awaitility          = "4.2.1"

[libraries]
serenity-core             = { module = "net.serenity-bdd:serenity-core",             version.ref = "serenity" }
serenity-junit            = { module = "net.serenity-bdd:serenity-junit",            version.ref = "serenity" }
serenity-cucumber         = { module = "net.serenity-bdd:serenity-cucumber",         version.ref = "serenity" }
serenity-rest-assured     = { module = "net.serenity-bdd:serenity-rest-assured",     version.ref = "serenity" }
serenity-screenplay       = { module = "net.serenity-bdd:serenity-screenplay",       version.ref = "serenity" }
serenity-screenplay-rest  = { module = "net.serenity-bdd:serenity-screenplay-rest",  version.ref = "serenity" }
cucumber-junit            = { module = "io.cucumber:cucumber-junit",                 version.ref = "cucumber" }
rest-assured              = { module = "io.rest-assured:rest-assured",               version.ref = "restAssured" }
jackson-databind          = { module = "com.fasterxml.jackson.core:jackson-databind",version.ref = "jacksonDatabind" }
awaitility                = { module = "org.awaitility:awaitility",                  version.ref = "awaitility" }

[plugins]
serenity = { id = "net.serenity-bdd.core", version.ref = "serenity" }
```

---

## 10. File Structure Specification

### Files to CREATE

| File | Purpose |
|------|---------|
| `app/src/test/java/com/automation/runners/CucumberRunner.java` | `@RunWith(CucumberWithSerenity.class)` entry point |
| `app/src/test/java/com/automation/stepdefinitions/CrudFlowStepDefs.java` | Cucumber glue; manages Actor lifecycle |
| `app/src/test/java/com/automation/tasks/RegisterUser.java` | Performable: POST /api/auth/register |
| `app/src/test/java/com/automation/tasks/LoginUser.java` | Performable: POST /api/auth/login; extracts token |
| `app/src/test/java/com/automation/tasks/CreateOrder.java` | Performable: POST /api/orders with Bearer |
| `app/src/test/java/com/automation/tasks/StartTask.java` | Performable: PATCH /api/tasks/{taskId}/start |
| `app/src/test/java/com/automation/tasks/DeleteOrder.java` | Performable: DELETE /api/orders/{orderId} |
| `app/src/test/java/com/automation/questions/TasksAtStation.java` | Question + polling retry; extracts orderId + taskId |
| `app/src/test/java/com/automation/questions/OrderStatus.java` | Question<Integer>: returns HTTP status code |
| `app/src/test/java/com/automation/models/LoginRequest.java` | DTO: `{identifier, password}` |
| `app/src/test/java/com/automation/models/LoginResponse.java` | DTO: `{token}` |
| `app/src/test/java/com/automation/models/CreateOrderRequest.java` | DTO: `{tableNumber, customerName, customerEmail, products[]}` |
| `app/src/test/java/com/automation/models/ProductRequest.java` | DTO: `{name, type, price}` |
| `app/src/test/java/com/automation/models/CreateOrderResponse.java` | DTO: `{tableNumber, tasksCreated, message}` |
| `app/src/test/java/com/automation/models/TaskResponse.java` | DTO: `{id, orderId, station, tableNumber, status, ...}` |
| `app/src/test/java/com/automation/utils/ApiConstants.java` | URL constants, endpoint paths, actor memory keys |
| `app/src/test/java/com/automation/utils/TestDataGenerator.java` | Unique data generators |
| `app/src/test/resources/features/crud_flow.feature` | Gherkin: 4 scenarios |
| `app/src/test/resources/serenity.properties` | Runtime config |
| `app/src/test/resources/cucumber.properties` | Cucumber config |

### Files to MODIFY

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Add all versioned entries from §9 |
| `app/build.gradle` | Replace deps block; add Serenity plugin; add aggregate task |
| `app/src/test/java/com/automation/AppTest.java` | Add `@Ignore` |

---

## Assumptions

| # | Assumption |
|---|-----------|
| A1 | DRINK → BAR station derivation is correct in the running backend |
| A2 | `identifier` field in login accepts email |
| A3 | `orderId` in `TaskResponse` is the same ID used in order endpoints |
| A4 | Unique `tableNumber` per run provides sufficient test isolation |
| A5 | Server is running at `serenity.base.url` before `./gradlew test` |
