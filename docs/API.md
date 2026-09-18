# API Cuestionarios Práctica

Base: `http://localhost:8080/api/v1`. Todos los cuerpos usan JSON.
Los índices de opciones comienzan en cero. Identificadores UUID.
Las listas usan `?page=0&size=20` (máximo 100) y devuelven items, page, size y total.
Incluyen recursos desactivados con active=false para conservar el historial.

## Bancos
- POST /bancos — crear: `{"name":"Java"}`.
- GET /bancos — listar.
- GET /bancos/{id} — consultar.
- PUT /bancos/{id} — actualizar nombre.
- DELETE /bancos/{id} — desactivar (204).

## Preguntas
- POST /bancos/{id}/preguntas — crear.
- GET /bancos/{id}/preguntas — listar.
- GET /preguntas/{id} — consultar.
- PUT /preguntas/{id} — reemplazar enunciado, explicación y opciones.
- DELETE /preguntas/{id} — desactivar.

Ejemplo:
```json
{
  "statement": "¿Cuánto es 2 + 2?",
  "explanation": "La suma es cuatro.",
  "options": [
    {"text": "4", "correct": true},
    {"text": "5", "correct": false}
  ]
}
```
Se requieren 2..10 opciones distintas y exactamente una correcta.
Los endpoints de gestión de preguntas muestran la clave para permitir edición.

## Cuestionarios
- POST /cuestionarios — crear con bankId, name y questionCount (1..1000).
- GET /cuestionarios — listar.
- GET /cuestionarios/{id} — consultar.
- PUT /cuestionarios/{id} — actualizar configuración.
- DELETE /cuestionarios/{id} — desactivar.
La disponibilidad se verifica al iniciar cada intento.

## Intentos
- POST /cuestionarios/{id}/intentos — iniciar, selección aleatoria sin repetición.
- GET /intentos — listar.
- GET /intentos/{id} — consultar preguntas y respuestas guardadas.
- PUT /intentos/{id}/respuestas/{questionId} — responder con `{"optionIndex":0}`.
  questionId es el UUID de la copia dentro del intento, no el de la pregunta original.
  Repetir la operación reemplaza la respuesta mientras el intento esté abierto.
- POST /intentos/{id}/finalizar — cerrar y corregir; repetir devuelve el resultado existente.

Un intento contiene copias persistidas de preguntas, opciones, explicación y clave.
Editar o desactivar los originales no altera el intento.
Antes del cierre, correctOption, explanation, score y percentage son null.
Después aparecen el puntaje, total, porcentaje y la corrección de cada pregunta.
Un acierto vale un punto; errores y omisiones valen cero.
Cerrar no exige responder todas las preguntas. El cierre bloquea nuevas respuestas.
Las escrituras de respuestas y cierre se serializan mediante bloqueo de la fila del intento.

## Errores
- 400: validación, JSON o índice de opción inválidos.
- 404: recurso inexistente o pregunta ajena al intento.
- 409: recurso desactivado, intento cerrado o preguntas disponibles insuficientes.
Los errores de dominio y validación usan ProblemDetail, con detail y status.

## Ejecución y pruebas
JDK 25 y Docker Desktop activo.
```powershell
docker compose -f compose.yaml -f compose.dev.yaml up -d --wait postgres
.\mvnw.cmd spring-boot:run
.\mvnw.cmd clean verify
```
Para ejecutar solamente las pruebas de reglas sin PostgreSQL:
```powershell
.\mvnw.cmd "-Dtest=BackendServiceTests" test
```

Se requiere sesión para acceder a cuestionarios, preguntas e intentos. La propiedad se valida en el backend.
La ocultación de claves se aplica a los endpoints de intentos;
los endpoints de gestión de preguntas siguen mostrando las claves.

### Compatibilidad de tests
Spring Boot 4.1 usa una integración SpringExtension que requiere JUnit 6.
Para mantener JUnit 5, las pruebas de integración arrancan Spring Boot explícitamente
con SpringApplicationBuilder y un PostgreSQL 17 de Testcontainers.


## Carga completa de un banco
POST /api/v1/bancos/carga crea un banco y todas sus preguntas en una transacción.
Cuerpo: {"name":"Java","questions":[{"statement":"Pregunta","explanation":"Opcional","options":[{"text":"A","correct":true},{"text":"B","correct":false}]}]}.
Admite 1..1000 preguntas. Si una falla, no se guarda el banco ni ninguna pregunta.
Devuelve 201 con id, name y active del banco creado.

## Inicio desde un cuestionario cargado (panel Intentos)
En el frontend los recursos `/bancos` se llaman Cuestionarios.
- GET /bancos/{id}/disponibilidad: id, name, active y questionCount (preguntas activas).
- POST /bancos/{id}/intentos: `{"questionCount":10,"participantName":"Ana"}`.
El nombre del participante se obtiene de la cuenta autenticada; participantName enviado por el cliente se ignora. La cantidad debe estar entre 1 y 1000 y no superar las preguntas activas.
La configuración y el intento se crean juntos; si no hay disponibilidad no se guarda ninguno.
La respuesta devuelve las preguntas y participantName. El resto del flujo usa `/intentos`.
