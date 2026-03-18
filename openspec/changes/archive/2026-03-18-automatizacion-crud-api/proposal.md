# Proposal: automatizacion-crud-api — API Automation: Full CRUD Cycle
## Serenity BDD 4.x + Screenplay REST + Cucumber 7 + REST Assured 5

> Source: Engram #20 | Phase: sdd-propose | Project: AUTO_API_SCREENPLAY

---

## A. Veredicto de viabilidad

**VIABLE** — Con las APIs documentadas se puede construir un flujo CRUD automatizable completo. El flujo NO es CRUD puro sobre un único recurso: involucra dos recursos (`orders` y `tasks`) más autenticación JWT. El gap principal es que **POST /api/orders no retorna `orderId`**. Solución: recuperarlo indirectamente desde `GET /api/tasks/station/{station}` filtrando por `tableNumber` único por ejecución.

---

## B. Flujo CRUD Propuesto

| Paso | Operación CRUD | Acción | Endpoint |
|------|---------------|--------|----------|
| 1 | Precondición | Register | POST /api/auth/register |
| 2 | Precondición | Login | POST /api/auth/login |
| 3 | **CREATE** | Create Order | POST /api/orders |
| 4 | **READ (indirecto)** | Get Tasks by Station | GET /api/tasks/station/{station}?status=PENDING |
| 5 | **UPDATE** | Start Task | PATCH /api/tasks/{taskId}/start |
| 6 | **READ** | Get Order Status | GET /api/orders/{orderId}/status |
| 7 | **DELETE** | Delete Order | DELETE /api/orders/{orderId} |
| 8 | **READ (post-delete)** | Verify 404 | GET /api/orders/{orderId}/status |

> **Nota:** El UPDATE se materializa sobre la entidad `Task` (paso 5) y se valida sobre la entidad `Order` (paso 6). No existe PATCH/PUT directo sobre `/api/orders`.

---

## C. Dependencias del Flujo

| Dato | Tipo | Origen | Reutilizado en pasos |
|------|------|--------|---------------------|
| `username` | String dinámico | `TestDataGenerator` | Paso 1 |
| `email` | String dinámico | `TestDataGenerator` | Pasos 1, 2, 3 |
| `password` | Constante | `"Test@1234"` | Pasos 1, 2 |
| `authToken` | JWT String | POST /login → `$.token` | Pasos 3–8 (Bearer header) |
| `tableNumber` | Integer único | `TestDataGenerator` | Paso 3 (body), Paso 4 (filtro) |
| `station` | Enum String | Derivado del tipo de producto | Paso 4 (path param) |
| `orderId` | Long/String | GET tasks → `$[?tableNumber==N].orderId` | Pasos 6, 7, 8 |
| `taskId` | Long/String | GET tasks → `$[?tableNumber==N].id` | Paso 5 |

**Regla de derivación de station:**
- `DRINK` → `BAR`
- `HOT_DISH` → `HOT_KITCHEN`
- `COLD_DISH` → `COLD_KITCHEN`

---

## D. APIs a Usar

| Paso | Op. CRUD | Método | Endpoint | Auth | Request Body | Response Esperado | Dato a Extraer | Validaciones Clave |
|------|----------|--------|----------|------|-------------|-------------------|---------------|-------------------|
| 1 | Precond. | POST | /api/auth/register | No | `{username, email, password}` | 201 sin body | — | Status 201; body vacío |
| 2 | Precond. | POST | /api/auth/login | No | `{identifier, password}` | 200 `{token}` | `$.token` | Status 200; token presente, no vacío |
| 3 | CREATE | POST | /api/orders | Sí | `{tableNumber, customerName, customerEmail, products[]}` | 201 `{tableNumber, tasksCreated, message}` | `$.tasksCreated` | Status 201; tasksCreated > 0; **sin orderId** |
| 4 | READ (ind.) | GET | /api/tasks/station/{station}?status=PENDING | Sí | — | 200 `TaskResponse[]` | `$.id`, `$.orderId` | Array no vacío; match por tableNumber |
| 5 | UPDATE | PATCH | /api/tasks/{taskId}/start | Sí | — | 200 `TaskResponse` | `$.status` | Status 200; status == IN_PREPARATION |
| 6 | READ | GET | /api/orders/{orderId}/status | Sí | — | 200 `{orderId, status}` | `$.status` | Status 200; status == IN_PREPARATION |
| 7 | DELETE | DELETE | /api/orders/{orderId} | Sí | — | 204 sin body | — | Status 204; body vacío |
| 8 | READ (ver.) | GET | /api/orders/{orderId}/status | Sí | — | 404 | — | Status 404 |

---

## E. Especificación de Datos de Prueba

```json
// Register
{
  "username": "qa_auto_1742256000000",
  "email":    "qa_auto_1742256000000@test.com",
  "password": "Test@1234"
}

// Login
{
  "identifier": "qa_auto_1742256000000@test.com",
  "password":   "Test@1234"
}

// Create Order
{
  "tableNumber":   9256,
  "customerName":  "Automation QA",
  "customerEmail": "qa_auto_1742256000000@test.com",
  "products": [
    { "name": "Coffee", "type": "DRINK", "price": 3.50 }
  ]
}
```

- `username` y `email`: sufijo `System.currentTimeMillis()` garantiza unicidad por ejecución.
- `tableNumber`: `(int)(System.currentTimeMillis() % 9000) + 1000` — único por ejecución.
- 1 producto tipo `DRINK` → exactamente 1 task en `BAR` → simplifica correlación.

---

## F. Reglas de Validación

| Endpoint | Status | Campos obligatorios | Validación negativa mínima |
|----------|--------|--------------------|-----------------------------|
| POST /register | 201 | body vacío | Email duplicado → 4xx |
| POST /login | 200 | `token` no nulo/vacío | Password incorrecto → 401 |
| POST /orders | 201 | `tasksCreated > 0`; **sin `orderId`** | — |
| GET /tasks/station | 200 | array no vacío; `id`, `orderId`, `tableNumber`, `status`, `station` | status == PENDING |
| PATCH /tasks/{id}/start | 200 | `status == IN_PREPARATION` | taskId inexistente → 404 |
| GET /orders/{id}/status | 200 | `orderId`, `status == IN_PREPARATION` | orderId inexistente → 404 |
| DELETE /orders/{id} | 204 | body vacío | orderId inexistente → 404 |
| GET /orders/{id}/status (post-delete) | 404 | — | mismo orderId que retornó 200 ahora da 404 |

---

## G. Criterio de Extracción y Encadenamiento

**Token:** `POST /api/auth/login → $.token → actor.remember("authToken", token) → Authorization: Bearer <token>`

> ⚠️ **ADVERTENCIA EXPLÍCITA: `POST /api/orders` no devuelve `orderId`.** Es imposible obtenerlo del CREATE.

**Estrategia de correlación mediante `tableNumber`:**
1. Usar `tableNumber` único e impredecible por ejecución.
2. Tras `POST /api/orders`, hacer `GET /api/tasks/station/BAR?status=PENDING`.
3. Filtrar array por `tableNumber == <valor usado en POST>`.
4. El match contiene `orderId` y `taskId`.

---

## H. Riesgos del Flujo

| # | Riesgo | Severidad | Mitigación |
|---|--------|-----------|------------|
| R1 | POST /api/orders no retorna orderId | **ALTA** | Recuperar desde GET /api/tasks/station + filtro tableNumber único |
| R2 | GET /api/tasks/station retorna múltiples registros | MEDIA | Filtro por exact tableNumber; sufijo timestamp |
| R3 | Eventual consistency | MEDIA | Polling 5 reintentos × 1s en TasksAtStation Question |
| R4 | Token en ejecución paralela | BAJA | Actor memory por instancia; sin static |
| R5 | Derivación ProductType→Station incorrecta | MEDIA | Verificar en ambiente antes de ejecutar |
| R6 | AppTest.java colisiona con Serenity/Cucumber runner | BAJA | Agregar @Ignore |
| R7 | DELETE deja tasks huérfanas | BAJA | Flujo principal es happy path CRUD |

---

## I. Diseño Recomendado en Serenity Screenplay REST

```
app/src/test/
├── java/com/automation/
│   ├── runners/            CucumberRunner.java
│   ├── stepdefinitions/    CrudFlowStepDefs.java
│   ├── tasks/              RegisterUser, LoginUser, CreateOrder, StartTask, DeleteOrder
│   ├── questions/          TasksAtStation (con polling), OrderStatus
│   ├── models/             LoginRequest, LoginResponse, CreateOrderRequest,
│   │                       ProductRequest, CreateOrderResponse, TaskResponse
│   └── utils/              ApiConstants, TestDataGenerator
└── resources/
    ├── features/           crud_flow.feature
    ├── serenity.properties
    └── cucumber.properties
```

---

## J. Escenario Gherkin Propuesto

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

## K. Casos Negativos Mínimos Recomendados

| # | Caso | Endpoint | Esperado | Valor |
|---|------|----------|----------|-------|
| N1 | Login con password incorrecto | POST /api/auth/login | 401 | Coverage de seguridad |
| N2 | GET status de orden inexistente | GET /api/orders/99999/status | 404 | Verifica manejo de IDs ficticios |
| N3 | DELETE orden inexistente | DELETE /api/orders/99999 | 404 | Valida idempotencia del delete |
| N4 | Invoice en orden no completada | POST /api/orders/{id}/invoice | 400 | Reglas de negocio |
| N5 | PATCH start de task inexistente | PATCH /api/tasks/99999/start | 404 | Espejo de N3 para Task |

---

## L. Pseudocódigo de Automatización

```
[BEFORE SCENARIO — Background]
  timestamp   = System.currentTimeMillis()
  email       = "qa_auto_" + timestamp + "@test.com"
  password    = "Test@1234"
  tableNumber = (int)(timestamp % 9000) + 1000
  actor = Actor.named("QA Tester").can(CallAnApi.at(BASE_URL))

  POST /api/auth/register con {username, email, password} → assert 201
  POST /api/auth/login con {identifier: email, password} → assert 200 → extract token → actor.remember

[SCENARIO — Ciclo CRUD]
  POST /api/orders con Bearer + {tableNumber, productos} → assert 201; tasksCreated > 0
  RETRY 5x/1s: GET /api/tasks/station/BAR?status=PENDING → filter por tableNumber
    → actor.remember(orderId, taskId)
  PATCH /api/tasks/{taskId}/start → assert 200; status == IN_PREPARATION
  GET /api/orders/{orderId}/status → assert 200; status == IN_PREPARATION
  DELETE /api/orders/{orderId} → assert 204
  GET /api/orders/{orderId}/status → assert 404
```

---

## M. Búsqueda en Engram

| Elemento | Estado |
|----------|--------|
| `ProductType` enum (DRINK, HOT_DISH, COLD_DISH) | **Verificado en Engram #19** |
| `Station` enum (BAR, HOT_KITCHEN, COLD_KITCHEN) | **Verificado en Engram #19** |
| `TaskStatus` enum (PENDING, IN_PREPARATION, COMPLETED) | **Verificado en Engram #19** |
| `TaskResponse` DTO: id, orderId, station, tableNumber, status, timestamps | **Verificado en Engram #19** |
| POST /api/orders no retorna orderId | **Verificado en Engram #19** |
| Autenticación Bearer JWT | **Verificado en Engram #19** |
| Stack delta necesario | **Verificado en Engram #19** |
| AppTest.java puede colisionar | **Verificado en Engram #19** |
| Código fuente real del OrderController | No verificable en Engram con la información disponible |
| Reglas de negocio internas del backend | No verificable en Engram con la información disponible |

---

## N. Supuestos Explícitos

| # | Supuesto | Impacto si incorrecto |
|---|----------|----------------------|
| S1 | DRINK→BAR; HOT_DISH→HOT_KITCHEN; COLD_DISH→COLD_KITCHEN | El paso 4 no encontrará tasks |
| S2 | GET /api/tasks/station requiere auth Bearer | Inocuo si no requiere |
| S3 | `identifier` en login acepta email | Cambiar a username si solo acepta username |
| S4 | `orderId` en TaskResponse es el mismo ID de /api/orders | Si distintos, encadenamiento falla |
| S5 | tableNumber único por ejecución aísla tasks | Datos históricos pueden colisionar |
| S6 | POST /api/orders/{id}/invoice retorna 400 si estado != COMPLETED | No documentado explícitamente |
| S7 | Servidor levantado en URL de serenity.properties | Prerequisito de ambiente |
| S8 | price acepta decimal (Double) | Si solo Integer, usar precio entero |
| S9 | DELETE hace cascade a tasks | Si no, tasks huérfanas contaminan ambiente |
| S10 | username debe ser único en el sistema | Estrategia timestamp-email es suficiente |
