# Migraciones

Flyway administra el esquema en orden y Hibernate solamente lo valida.

- `V1__cuestionarios.sql`: bancos, preguntas y opciones; configuraciones de
  cuestionarios; intentos y sus copias históricas de preguntas y respuestas.
- `V2__participante_intento.sql`: agrega el nombre del participante al intento.
- `V3__usuarios_y_propiedad.sql`: incorpora cuentas, identidad Google opcional,
  formación académica, propietario y estado de los bancos, y propietario de
  los intentos. Esta migración elimina datos previos del dominio para poder
  establecer propietarios obligatorios; está destinada al reinicio autorizado
  del entorno de desarrollo y no debe aplicarse a una base real con datos.
- `V4__apellido_usuario.sql`: agrega apellido al perfil sin borrar usuarios ni
  historial.
- `V5__clasificacion_academica.sql`: incorpora carreras, materias, etiquetas y
  clasificación opcional del cuestionario.
- `V6__clasificacion_catalogo_academico.sql`: agrega universidad, unidad
  académica, carrera del catálogo y año, todos opcionales.
- `V7__materia_manual_cuestionario.sql`: permite guardar el nombre normalizado
  de una materia ingresada manualmente al crear un cuestionario.

Las migraciones aplicadas no se modifican: todo cambio nuevo debe agregarse en
un archivo posterior con el formato `V5__descripcion.sql`, `V6__descripcion.sql`,
etc. Antes de incorporar una migración destructiva se debe definir de forma
explícita cómo conservar o trasladar los datos existentes.
