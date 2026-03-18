# language: es
@crud
Característica: Ciclo de vida completo de una orden de restaurante

  Antecedentes:
    Dado que se registra una nueva cuenta de usuario en el sistema
    Y el usuario se autentica con credenciales válidas

  @happy-path @crud-delete
  Escenario: Crear y eliminar una orden de restaurante
    Cuando el usuario crea una nueva orden con una bebida para la mesa actual
    Entonces las tareas de la orden deben aparecer como PENDIENTES en la estación BAR
    Cuando se elimina la orden del sistema
    Entonces consultar el estado de la orden debe retornar un error 404

  @happy-path @crud-start
  Escenario: Iniciar la preparación de una tarea de orden
    Cuando el usuario crea una nueva orden con una bebida para la mesa actual
    Entonces las tareas de la orden deben aparecer como PENDIENTES en la estación BAR
    Cuando el sistema inicia la preparación de la tarea
    Entonces el estado de la orden debe ser EN_PREPARACION

  @negative
  Escenario: El login falla con credenciales incorrectas
    Cuando el usuario intenta autenticarse con una contraseña incorrecta
    Entonces el sistema debe rechazar el intento de autenticación

  @negative
  Escenario: Consultar una orden inexistente retorna no encontrado
    Cuando el usuario consulta el estado de una orden que no existe
    Entonces el sistema debe responder con un error de recurso no encontrado

  @negative
  Escenario: Eliminar una orden inexistente retorna no encontrado
    Cuando el usuario intenta eliminar una orden que no existe
    Entonces el sistema debe responder con un error de recurso no encontrado
