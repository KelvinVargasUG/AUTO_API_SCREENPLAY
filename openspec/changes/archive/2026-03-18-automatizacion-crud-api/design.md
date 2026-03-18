# Design: automatizacion-crud-api — API Automation: Full CRUD Cycle
## Serenity BDD 4.2.9 + Screenplay REST + Cucumber 7.18.1 + REST Assured 5.4.0

> Source: Engram #22 | Phase: sdd-design | Project: AUTO_API_SCREENPLAY

---

## Technical Approach

Migrate the existing Gradle skeleton (`app/build.gradle` + `gradle/libs.versions.toml`) from the JUnit 4 / Guava baseline to Serenity BDD 4.2.9 + Screenplay REST + Cucumber 7.18.1 + REST Assured 5.4.0. All test logic lives in `app/src/test/java/com/automation/`. Tasks encapsulate write operations, Questions encapsulate reads, and Actor memory carries state between Gherkin steps without static variables.

---

## Architecture Decisions

### ADR-01: Serenity Screenplay REST vs plain REST Assured

**Choice**: Serenity BDD 4.x Screenplay REST

**Rationale**:
- Screenplay provides first-class living documentation via Serenity reports — every `attemptsTo` and `asksAbout` call becomes a reportable step.
- Actor memory (`remember`/`recall`) solves cross-step state (token, orderId) without static fields → thread-safe by design.
- `CallAnApi` + `SerenityRest` integrates directly with Serenity's reporting pipeline.
- The project is already named AUTO_API_SCREENPLAY, establishing intent.
- Serenity 4.x supports Cucumber 7 and Java 17 natively.

### ADR-02: `tableNumber` as correlation key for orderId recovery

**Choice**: Unique `tableNumber` via `System.currentTimeMillis() % 9000 + 1000`

**Rationale**:
- POST /api/orders does NOT return `orderId` — documented gap.
- `tableNumber` is the only field sent in POST /api/orders body that also appears in `TaskResponse`.
- It is the only viable correlation key available without API changes.
- customerEmail could work as secondary filter but requires cross-model join; `tableNumber` is simpler.

### ADR-03: Polling strategy — manual retry loop vs Awaitility

**Choice**: Manual retry loop with `Thread.sleep` inside `TasksAtStation.answeredBy()`

**Rationale**:
- Manual retry (5 attempts, 1000ms) is explicit, readable, zero extra dependencies.
- Awaitility is listed as a future enhancement for more polling scenarios.
- Serenity's `eventually()` is designed for UI waits, not REST polling.
- Encapsulated entirely inside `TasksAtStation` — callers are unaware of retry logic.

### ADR-04: Actor memory pattern for token/state propagation

**Choice**: `actor.remember(key, value)` / `actor.recall(key)` for all cross-step state

**Rationale**:
- Actor memory is instance-scoped and scenario-scoped — new Actor per scenario = isolated state.
- Static fields would break parallel execution and leak state between scenarios.
- Actor memory produces named steps in Serenity report, aiding traceability.
- All keys are constants in `ApiConstants` to avoid typo-driven mismatches.

---

## Data Flow

```
TestDataGenerator
    │  generateUniqueEmail()    → email
    │  generateUniqueUsername() → username
    │  generateUniqueTableNumber() → tableNumber
    ↓
CrudFlowStepDefs (@Before)
    │  Actor.named("QA Tester").can(CallAnApi.at(BASE_URL))
    ↓
RegisterUser.performAs(actor)       → POST /api/auth/register → 201
    ↓
LoginUser.performAs(actor)          → POST /api/auth/login → 200 {token}
    │  actor.remember("authToken", token)
    ↓
CreateOrder.performAs(actor)        → POST /api/orders Bearer{token} → 201
    │  actor.remember("tableNumber", tableNumber)
    ↓
TasksAtStation.answeredBy(actor)    → GET /api/tasks/station/BAR?status=PENDING [retry loop]
    │  filter by tableNumber → first match
    │  actor.remember("orderId", task.orderId)
    │  actor.remember("taskId", task.id)
    ↓
StartTask.performAs(actor)          → PATCH /api/tasks/{taskId}/start → 200 {IN_PREPARATION}
    ↓
OrderStatus.answeredBy(actor)       → GET /api/orders/{orderId}/status → 200 {IN_PREPARATION}
    ↓
DeleteOrder.performAs(actor)        → DELETE /api/orders/{orderId} → 204
    ↓
OrderStatus.answeredBy(actor)       → GET /api/orders/{orderId}/status → 404
```

---

## File Changes

| File | Action | Responsibility |
|------|--------|---------------|
| `app/build.gradle` | MODIFY (full rewrite) | Replace application plugin + JUnit4/Guava with Serenity BDD 4.x + Cucumber 7 |
| `gradle/libs.versions.toml` | MODIFY (additions) | Add version catalog entries |
| `app/src/test/java/com/automation/AppTest.java` | MODIFY | Add `@Ignore` |
| `app/src/test/java/com/automation/runners/CucumberRunner.java` | CREATE | `@RunWith(CucumberWithSerenity)` entry point |
| `app/src/test/java/com/automation/stepdefinitions/CrudFlowStepDefs.java` | CREATE | Gherkin glue; Actor lifecycle |
| `app/src/test/java/com/automation/tasks/RegisterUser.java` | CREATE | Performable: POST /api/auth/register |
| `app/src/test/java/com/automation/tasks/LoginUser.java` | CREATE | Performable: POST /api/auth/login; extracts + remembers token |
| `app/src/test/java/com/automation/tasks/CreateOrder.java` | CREATE | Performable: POST /api/orders with Bearer |
| `app/src/test/java/com/automation/tasks/StartTask.java` | CREATE | Performable: PATCH /api/tasks/{taskId}/start |
| `app/src/test/java/com/automation/tasks/DeleteOrder.java` | CREATE | Performable: DELETE /api/orders/{orderId} |
| `app/src/test/java/com/automation/questions/TasksAtStation.java` | CREATE | Question + polling retry + filter by tableNumber |
| `app/src/test/java/com/automation/questions/OrderStatus.java` | CREATE | Question<Integer>: returns HTTP status code |
| `app/src/test/java/com/automation/models/LoginRequest.java` | CREATE | DTO: `{identifier, password}` |
| `app/src/test/java/com/automation/models/LoginResponse.java` | CREATE | DTO: `{token}` |
| `app/src/test/java/com/automation/models/CreateOrderRequest.java` | CREATE | DTO: `{tableNumber, customerName, customerEmail, products[]}` |
| `app/src/test/java/com/automation/models/ProductRequest.java` | CREATE | DTO: `{name, type, price}` |
| `app/src/test/java/com/automation/models/CreateOrderResponse.java` | CREATE | DTO: `{tableNumber, tasksCreated, message}` |
| `app/src/test/java/com/automation/models/TaskResponse.java` | CREATE | DTO con todos los campos de TaskResponse |
| `app/src/test/java/com/automation/utils/ApiConstants.java` | CREATE | URL constants + actor memory key constants |
| `app/src/test/java/com/automation/utils/TestDataGenerator.java` | CREATE | Unique data generators |
| `app/src/test/resources/features/crud_flow.feature` | CREATE | Gherkin: 4 scenarios |
| `app/src/test/resources/serenity.properties` | CREATE | Serenity runtime config |
| `app/src/test/resources/cucumber.properties` | CREATE | Cucumber config |

---

## Dependency Versions

| Dependency | Version | Rationale |
|-----------|---------|-----------|
| serenity-bdd | `4.2.9` | Latest Serenity 4.x stable; compatible with Cucumber 7.x, Java 17, REST Assured 5.x |
| cucumber-java / cucumber-junit | `7.18.1` | Latest Cucumber 7.x; Serenity 4.2.x validated against 7.x (not 8.x — JUnit 5 only) |
| rest-assured | `5.4.0` | Latest REST Assured 5.x; JSON path improvements; serenity-rest-assured 4.2.x wraps RA 5.x |
| jackson-databind | `2.17.2` | Latest Jackson 2.17.x; LTS line aligned with Spring Boot 3.3 |
| junit (runner) | `4.13.2` | CucumberWithSerenity still requires JUnit 4 |
| lombok | `1.18.34` | Reduces model boilerplate |

---

## `app/build.gradle` — Full Rewrite

```groovy
plugins {
    id 'java'
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    cucumberRuntime {
        extendsFrom testImplementation
    }
}

dependencies {
    testImplementation libs.serenityCore
    testImplementation libs.serenityCucumber
    testImplementation libs.serenityRestAssured
    testImplementation libs.cucumberJava
    testImplementation libs.cucumberJunit
    testImplementation libs.junit
    testImplementation libs.restAssured
    testImplementation libs.jacksonDatabind
    compileOnly           libs.lombok
    testCompileOnly       libs.lombok
    annotationProcessor   libs.lombok
    testAnnotationProcessor libs.lombok
}

test {
    useJUnit()
    testLogging {
        events 'passed', 'skipped', 'failed'
        showStandardStreams = true
    }
    systemProperty 'cucumber.filter.tags', System.getProperty('cucumber.filter.tags', '')
}

task aggregate(type: JavaExec) {
    dependsOn test
    mainClass = 'net.serenitybdd.core.Serenity'
    classpath = configurations.cucumberRuntime
    args = [
        '--project-dir', projectDir.absolutePath,
        '--source',      "${buildDir}/site/serenity"
    ]
}

task functionalTest(dependsOn: aggregate) {
    description = 'Runs Cucumber scenarios and generates Serenity report'
    group = 'verification'
}
```

---

## `gradle/libs.versions.toml` — Full Delta

```toml
[versions]
guava         = "32.1.2-jre"
junit         = "4.13.2"
serenity      = "4.2.9"
cucumber      = "7.18.1"
restAssured   = "5.4.0"
jackson       = "2.17.2"
lombok        = "1.18.34"

[libraries]
guava               = { module = "com.google.guava:guava",                        version.ref = "guava" }
junit               = { module = "junit:junit",                                   version.ref = "junit" }
serenityCore        = { module = "net.serenity-bdd:serenity-core",               version.ref = "serenity" }
serenityCucumber    = { module = "net.serenity-bdd:serenity-cucumber",           version.ref = "serenity" }
serenityRestAssured = { module = "net.serenity-bdd:serenity-rest-assured",       version.ref = "serenity" }
cucumberJava        = { module = "io.cucumber:cucumber-java",                    version.ref = "cucumber" }
cucumberJunit       = { module = "io.cucumber:cucumber-junit",                   version.ref = "cucumber" }
restAssured         = { module = "io.rest-assured:rest-assured",                 version.ref = "restAssured" }
jacksonDatabind     = { module = "com.fasterxml.jackson.core:jackson-databind",  version.ref = "jackson" }
lombok              = { module = "org.projectlombok:lombok",                     version.ref = "lombok" }
```

---

## `serenity.properties` — Full Content

```properties
serenity.project.name=AUTO_API_SCREENPLAY
serenity.project.version=1.0.0
serenity.base.url=http://localhost:8080
serenity.outputDirectory=target/site/serenity
serenity.reports.show.step.details=true
serenity.report.encoding=UTF-8
restassured.enableLoggingOfRequestAndResponseIfValidationFails=true
serenity.rest.displayActualBodyInReport=true
webdriver.driver=
serenity.fail.on.pending=false
```

---

## `cucumber.properties` — Full Content

```properties
cucumber.publish.quiet=true
cucumber.plugin=pretty, \
  io.cucumber.core.plugin.SerenityReporter, \
  json:target/cucumber-reports/cucumber.json, \
  html:target/cucumber-reports/cucumber.html
cucumber.features=classpath:features
cucumber.glue=com.automation.stepdefinitions
cucumber.filter.tags=
```

---

## Key Class Signatures

### `CucumberRunner.java`
```java
@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
    features = "classpath:features",
    glue     = "com.automation.stepdefinitions",
    plugin   = {"pretty"},
    tags     = ""
)
public class CucumberRunner {}
```

### `TasksAtStation.java` (polling core)
```java
public class TasksAtStation implements Question<TaskResponse> {
    private static final int MAX_RETRIES = 5;
    private static final long POLL_MS    = 1000L;
    private final String station;

    public static TasksAtStation atStation(String station) {
        return new TasksAtStation(station);
    }

    @Override
    public TaskResponse answeredBy(Actor actor) {
        String token    = actor.recall(ApiConstants.KEY_TOKEN);
        int tableNumber = actor.recall(ApiConstants.KEY_TABLE_NUMBER);
        for (int i = 0; i < MAX_RETRIES; i++) {
            actor.attemptsTo(
                Get.resource(ApiConstants.TASKS_STATION.replace("{station}", station))
                   .with(req -> req.header("Authorization", "Bearer " + token)
                                   .queryParam("status", "PENDING"))
            );
            List<TaskResponse> tasks = SerenityRest.lastResponse()
                .jsonPath().getList("$", TaskResponse.class);
            Optional<TaskResponse> match = tasks.stream()
                .filter(t -> Integer.parseInt(t.getTableNumber()) == tableNumber)
                .findFirst();
            if (match.isPresent()) {
                actor.remember(ApiConstants.KEY_ORDER_ID, match.get().getOrderId());
                actor.remember(ApiConstants.KEY_TASK_ID,  match.get().getId());
                return match.get();
            }
            try { Thread.sleep(POLL_MS); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        throw new AssertionError(
            "No task found for tableNumber=" + tableNumber + " after " + MAX_RETRIES + " retries"
        );
    }
}
```

### `ApiConstants.java`
```java
public final class ApiConstants {
    public static final String BASE_URL          = "http://localhost:8080";
    public static final String AUTH_REGISTER     = "/api/auth/register";
    public static final String AUTH_LOGIN        = "/api/auth/login";
    public static final String ORDERS            = "/api/orders";
    public static final String ORDERS_STATUS     = "/api/orders/{orderId}/status";
    public static final String TASKS_STATION     = "/api/tasks/station/{station}";
    public static final String TASKS_START       = "/api/tasks/{taskId}/start";
    public static final String KEY_TOKEN         = "authToken";
    public static final String KEY_TABLE_NUMBER  = "tableNumber";
    public static final String KEY_ORDER_ID      = "orderId";
    public static final String KEY_TASK_ID       = "taskId";
    public static final String KEY_EMAIL         = "email";
    public static final String KEY_PASSWORD      = "password";
    private ApiConstants() {}
}
```

---

## `crud_flow.feature` — Full Content

```gherkin
# language: es
@crud
Feature: Ciclo de vida completo de una orden de restaurante

  Background:
    Given que se registra una nueva cuenta de usuario en el sistema
    And el usuario se autentica con credenciales válidas

  @happy-path @crud-full
  Scenario: Ciclo CRUD completo de una orden de restaurante
    When el usuario crea una nueva orden con una bebida para la mesa actual
    Then las tareas de la orden deben aparecer como PENDIENTES en la estación BAR
    When el sistema inicia la preparación de la tarea
    Then el estado de la orden debe ser EN_PREPARACION
    When se elimina la orden del sistema
    Then consultar el estado de la orden debe retornar un error 404

  @negative
  Scenario: El login falla con credenciales incorrectas
    When el usuario intenta autenticarse con una contraseña incorrecta
    Then el sistema debe responder con un error de acceso no autorizado

  @negative
  Scenario: Consultar una orden inexistente retorna no encontrado
    When el usuario consulta el estado de una orden que no existe
    Then el sistema debe responder con un error de recurso no encontrado

  @negative
  Scenario: Eliminar una orden inexistente retorna no encontrado
    When el usuario intenta eliminar una orden que no existe
    Then el sistema debe responder con un error de recurso no encontrado
```

---

## Migration / Rollout Order

1. Commit current state to git (rollback point)
2. Apply `libs.versions.toml` delta
3. Rewrite `app/build.gradle`
4. `./gradlew dependencies` — verify resolution
5. Create all source files under `app/src/test/java/com/automation/`
6. Add `@Ignore` to `AppTest.java`
7. `./gradlew test` — verify compilation + test
8. `./gradlew aggregate` — verify Serenity HTML report

---

## Open Questions

| # | Question | Impact |
|---|----------|--------|
| OQ-1 | Does GET /api/tasks/station/{station} require Bearer auth? | Affects TasksAtStation header |
| OQ-2 | Is `identifier` in POST /api/auth/login the email or the username? | Affects LoginRequest |
| OQ-3 | Does DELETE /api/orders cascade-delete tasks? | Affects env cleanup |
| OQ-4 | What status does an Order have when all its tasks are PENDING? | Status before StartTask call |

---

## Risks

| # | Risk | Severity | Mitigation |
|---|------|----------|-----------|
| R1 | POST /api/orders does not return orderId | HIGH | Recover via TasksAtStation polling filtered by unique tableNumber |
| R2 | Eventual consistency: tasks not visible immediately | MEDIUM | MAX_RETRIES=5 × 1000ms; fail-fast with clear error message |
| R3 | tableNumber collision in non-clean environment | MEDIUM | Timestamp-based generation; createdAt filter as fallback |
