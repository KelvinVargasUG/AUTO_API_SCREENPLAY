# AUTO_API_SCREENPLAY

## Descripción

Proyecto de automatización de pruebas a nivel de **API REST** sobre el backend **FoodTech-Kitchen-Services**. Valida los contratos del servicio enviando peticiones HTTP directamente a los endpoints, sin interactuar con la interfaz gráfica. Utiliza el patrón **Screenplay** adaptado para APIs.

## Enfoque de prueba

A diferencia de los proyectos de automatización de frontend, este proyecto prueba el backend directamente mediante llamadas HTTP. Esto permite validar que los endpoints responden correctamente en términos de códigos de estado, estructura de respuesta y reglas de negocio, independientemente de la interfaz gráfica.

## Escenarios cubiertos

| Feature | Escenarios | Tipo |
|---|---|---|
| **Ciclo de vida de una orden** | Crear y eliminar orden; Iniciar preparación de tarea; Login fallido con credenciales incorrectas; Consultar orden inexistente (404); Eliminar orden inexistente (404) | Positivo / Negativo |
| **Gestión del catálogo vía API** | Crear producto (201); Listar productos (200); Actualizar producto (200); Desactivar producto (200); Crear producto con nombre vacío (4xx); Producto activo en listado; Actualizar producto inexistente (404); Producto desactivado no en catálogo activo | Positivo / Negativo |
| **Carga masiva vía API** | Iniciar sesión de carga (201); Flujo completo init-upload-complete (202); Descargar plantilla CSV (200); Rechazo chunk por tamaño excedido (413); Rechazo CSV con cabeceras incorrectas; Reporte de errores disponible (200); Productos cargados en catálogo | Positivo / Negativo |

**Total: 20 escenarios**

## Flujo principal automatizado

| Paso | Acción |
|---|---|
| 1 | Registrar usuario |
| 2 | Autenticar usuario |
| 3 | Crear orden con productos |
| 4 | Consultar tareas por estación |
| 5 | Iniciar preparación de tarea |
| 6 | Eliminar orden |
| 7 | Consultar estado de orden eliminada |

## Relación con los otros proyectos

Este proyecto es el **complemento a nivel de API** de los dos proyectos de automatización de frontend (AUTO_FRONT_SCREENPLAY y AUTO_FRONT_POM_FACTORY). Juntos proporcionan cobertura en múltiples capas: los proyectos de frontend validan la experiencia del usuario desde el navegador, y este valida que el backend cumple sus contratos.

## Requisitos previos

- Java 17 o superior
- FoodTech-Kitchen-Services corriendo en `http://localhost:8080`
- Ejecutar el script de datos iniciales: [seed_data.sh](scripts/seed_data.sh)

## Comandos disponibles

```bash
./gradlew clean :app:test                                          # Ejecutar todos los tests
./gradlew clean :app:test -Dcucumber.filter.tags="@happy-path"     # Solo escenarios happy-path
./gradlew clean :app:test -Dcucumber.filter.tags="@negative"       # Solo escenarios negativos
```

## Reportes

Los reportes se generan en:

```
app/target/site/serenity/index.html          # Reporte Serenity
app/target/cucumber-reports/cucumber.html    # Reporte Cucumber
```

---

Proyecto académico — Sofka Technologies — 2026
