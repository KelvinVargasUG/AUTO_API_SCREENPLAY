# AUTO_API_SCREENPLAY

Automatización de API REST para el ciclo de vida completo de una orden de restaurante.  
Stack: **Serenity BDD 4.x · Screenplay · Serenity Rest · Cucumber 7 · REST Assured 5 · Gradle · Java 17**

---

## Prerrequisitos

| Herramienta | Versión mínima |
|---|---|
| Java (JDK) | 17 |
| Gradle | 8.x (wrapper incluido) |
| Servidor bajo prueba | corriendo en `http://localhost:8080` |

- [FoodTech-Kitchen-Services](https://github.com/KelvinVargasUG/FoodTech-Kitchen-Services/tree/main)
- Ejecutar Script inicial [seed_data.sh](scripts/seed_data.sh)

> El wrapper `gradlew` descarga Gradle automáticamente; no es necesario instalarlo manualmente.

---

## Ejecutar los tests

```bash
./gradlew clean :app:test
```

Para ejecutar sólo los escenarios happy-path:

```bash
./gradlew clean :app:test -Dcucumber.filter.tags="@happy-path"
```

Para ejecutar sólo los escenarios negativos:

```bash
./gradlew clean :app:test -Dcucumber.filter.tags="@negative"
```

---

## Ver el reporte Serenity

Abrir en el navegador:

```
app/target/site/serenity/index.html
```

El reporte HTML de Cucumber también se genera en:

```
app/target/cucumber-reports/cucumber.html
```

---

## Estructura del proyecto

```
app/src/test/
├── java/com/automation/
│   ├── tasks/              # Tareas Screenplay (acciones del actor)
│   │   ├── RegisterUser.java
│   │   ├── LoginUser.java
│   │   ├── AttemptLoginWith.java
│   │   ├── CreateOrder.java
│   │   ├── DeleteOrder.java
│   │   └── StartTask.java
│   ├── questions/          # Preguntas Screenplay (observaciones del sistema)
│   │   ├── TasksAtStation.java
│   │   └── OrderStatus.java
│   ├── models/             # POJOs de request/response
│   ├── utils/              # Constantes y builder de RequestSpecification
│   ├── runners/            # CucumberRunner
│   └── stepdefinitions/   # Bindings Gherkin → Screenplay
└── resources/
    ├── features/
    │   └── crud_flow.feature
    ├── serenity.conf
    └── cucumber.properties
```

---

## Flujo CRUD automatizado

| Paso | Verbo HTTP | Endpoint |
|---|---|---|
| Registrar usuario | `POST` | `/api/auth/register` |
| Autenticar usuario | `POST` | `/api/auth/login` |
| Crear orden | `POST` | `/api/orders` |
| Consultar tareas por estación | `GET` | `/api/tasks/station/{station}?status=PENDING` |
| Iniciar preparación de tarea | `PATCH` | `/api/tasks/{taskId}/start` |
| Eliminar orden | `DELETE` | `/api/orders/{orderId}` |
| Consultar estado de orden | `GET` | `/api/orders/{orderId}/status` |

---
