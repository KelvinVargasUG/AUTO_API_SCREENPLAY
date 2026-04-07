# language: es
@carga-masiva
Característica: Carga masiva de productos mediante CSV vía API

  Antecedentes:
    Dado que se registra un nuevo usuario para carga masiva
    Y el usuario de carga masiva se autentica con credenciales válidas

  @carga-init
  Escenario: Iniciar una sesión de carga masiva
    Cuando el usuario inicia una sesión de carga masiva
    Entonces el sistema debe retornar un uploadId válido con status 201

  @carga-completa
  Escenario: Flujo completo de carga masiva con CSV
    Cuando el usuario inicia una sesión de carga masiva
    Y el usuario sube el contenido CSV al chunk 0
    Y el usuario completa la sesión de carga
    Entonces el sistema debe retornar el resumen del procesamiento con status 202

  @carga-template
  Escenario: Descargar la plantilla CSV de carga masiva
    Cuando el usuario descarga la plantilla de carga masiva
    Entonces el sistema debe retornar la plantilla CSV con status 200

  @carga-tamano
  Escenario: Rechazo de chunk que supera el tamaño máximo permitido
    Cuando el usuario inicia una sesión de carga masiva
    Y el usuario sube un chunk que supera el tamaño máximo permitido
    Entonces el sistema debe rechazar el archivo con status 413

  @carga-invalida
  Escenario: Rechazo de CSV con cabeceras incorrectas
    Cuando el usuario inicia una sesión de carga masiva
    Y el usuario sube un CSV con cabeceras incorrectas al chunk 0
    Y el usuario completa la sesión de carga
    Entonces el sistema debe rechazar el CSV con un error de validación

  @carga-errores
  Escenario: Reporte de registros inválidos disponible tras la carga
    Cuando el usuario inicia una sesión de carga masiva
    Y el usuario sube el contenido CSV al chunk 0
    Y el usuario completa la sesión de carga
    Entonces el sistema debe exponer el endpoint de errores con status 200

  @carga-visibilidad
  Escenario: Productos cargados quedan disponibles en el catálogo
    Cuando el usuario inicia una sesión de carga masiva
    Y el usuario sube el contenido CSV al chunk 0
    Y el usuario completa la sesión de carga
    Entonces los productos cargados deben aparecer en el listado del catálogo
