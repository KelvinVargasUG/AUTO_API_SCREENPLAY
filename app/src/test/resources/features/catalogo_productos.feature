# language: es
@catalogo
Característica: Gestión del catálogo de productos vía API

  Antecedentes:
    Dado que se registra un nuevo usuario para gestionar el catálogo
    Y el usuario del catálogo se autentica con credenciales válidas

  @catalogo-crear
  Escenario: Crear un nuevo producto en el catálogo
    Cuando el usuario crea un producto "Café Americano" de tipo "DRINK" en categoría "Bebidas Calientes" con precio 3.50
    Entonces el producto debe crearse exitosamente con status 201

  @catalogo-listar
  Escenario: Listar los productos del catálogo
    Cuando el usuario crea un producto "Limonada Fría" de tipo "DRINK" en categoría "Bebidas Frías" con precio 2.50
    Y el usuario consulta el listado del catálogo
    Entonces el catálogo debe retornar status 200 con al menos un producto

  @catalogo-actualizar
  Escenario: Actualizar un producto existente
    Cuando el usuario crea un producto "Sopa del Día" de tipo "HOT_DISH" en categoría "Platos Calientes" con precio 5.00
    Y el usuario actualiza el producto con nombre "Sopa Especial" de tipo "HOT_DISH" en categoría "Platos Calientes" con precio 6.50
    Entonces el producto debe actualizarse con status 200

  @catalogo-desactivar
  Escenario: Desactivar un producto del catálogo
    Cuando el usuario crea un producto "Ensalada Verde" de tipo "COLD_DISH" en categoría "Ensaladas" con precio 4.00
    Y el usuario desactiva el producto creado del catálogo
    Entonces el producto debe desactivarse con status 200

  @catalogo-validacion
  Escenario: Crear producto con nombre vacío retorna error de validación
    Cuando el usuario intenta crear un producto con nombre vacío
    Entonces el sistema debe rechazar la creación con un error 4xx

  @catalogo-visibilidad
  Escenario: Producto activo aparece en el listado del catálogo
    Cuando el usuario crea un producto "Jugo de Naranja" de tipo "DRINK" en categoría "Bebidas Frías" con precio 2.00
    Y el usuario consulta el listado del catálogo
    Entonces el producto recién creado debe aparecer en el catálogo

  @catalogo-negativo
  Escenario: Actualizar un producto inexistente retorna 404
    Cuando el usuario intenta actualizar un producto que no existe
    Entonces el sistema debe responder con 404 al actualizar

  @catalogo-inactivo
  Escenario: Producto desactivado no aparece en el catálogo activo
    Cuando el usuario crea un producto "Té Helado" de tipo "DRINK" en categoría "Bebidas Frías" con precio 1.50
    Y el usuario desactiva el producto creado del catálogo
    Entonces el producto desactivado no debe aparecer en el listado del catálogo
