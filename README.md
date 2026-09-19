# API-Cuestionarios-Practica

## Inicio completo con Docker

Ejecutá Iniciar.cmd para levantar todo y abrir la app. También podés usar:

```powershell
docker compose up --build -d --wait --wait-timeout 300
```

App: http://127.0.0.1:8090. Documentación: [docs/DOCKER.md](docs/DOCKER.md).

API REST para la creación y gestión de cuestionarios, bancos de preguntas, intentos personalizados y corrección automática. Desarrollada con Java, Spring Boot y PostgreSQL.

## Tecnologías

Java 25 LTS, Spring Boot 4.1.1, Maven, Spring Web MVC, JPA/Hibernate,
Jakarta Bean Validation, PostgreSQL 17 y Flyway. Tests con JUnit 5,
Mockito, Spring Boot Test y Testcontainers. Autenticación con sesiones y Spring Security. Frontend React disponible.

### Requisitos

- JDK 25: configurar JAVA_HOME y PATH para que apunten al JDK 25.
- Docker Desktop en ejecución para PostgreSQL y tests de integración.
- Maven Wrapper incluido: no requiere Maven instalado.

### Ejecutar desde PowerShell

```powershell
java -version
.\mvnw.cmd -version
docker compose -f compose.yaml -f compose.dev.yaml up -d --wait postgres
.\mvnw.cmd spring-boot:run
```

El servidor usa el puerto 8080. Los endpoints están disponibles bajo /api/v1.
Credenciales locales: base y usuario `cuestionarios`, contraseña
`cuestionarios_local`. Son valores solo para desarrollo.

Se puede configurar DB_URL, DB_USERNAME, DB_PASSWORD y SERVER_PORT mediante
variables de entorno. Al cambiar la contraseña, usar el mismo DB_PASSWORD
para Compose y la aplicación. Compose lee `.env`; Spring Boot lee variables
del proceso, no carga `.env` automáticamente.

### Verificar

```powershell
.\mvnw.cmd clean verify
```

La prueba de integración inicia su propio PostgreSQL 17 con Testcontainers,
arranca el servidor y verifica la conexión y Flyway. Docker debe estar activo;
la prueba no se omite si falta Docker.

### Estructura

- `src/main/java/ar/com/cuestionarios`: aplicación Spring Boot.
- `src/main/resources/application.properties`: configuración del entorno.
- `src/main/resources/db/migration`: migraciones SQL versionadas del dominio y usuarios.
- `src/test/java`: pruebas de integración.
- `frontend`: aplicación React, importador Excel y pruebas del cliente.
- `docs`: contrato de API y guías de usuarios, catálogo académico y Docker.
- `compose.yaml`: aplicación completa con PostgreSQL, backend y frontend Nginx.

Hibernate valida el esquema; los cambios de tablas se harán con Flyway.
El dominio inicial incluye bancos, preguntas, cuestionarios e intentos.

## API implementada

Bancos, preguntas, cuestionarios e intentos con corrección automática.
Contrato y ejemplos en [docs/API.md](docs/API.md). DELETE desactiva recursos.

Además incluye cuentas locales y Google OIDC opcional, perfiles académicos,
sesiones con CSRF, propiedad de recursos y estados BORRADOR, PRIVADO y PUBLICO.
Ver [docs/USUARIOS.md](docs/USUARIOS.md) y
[docs/CATALOGO-ACADEMICO.md](docs/CATALOGO-ACADEMICO.md).

## Frontend

Aplicación React con bienvenida, registro e ingreso, perfil, creación manual o
por Excel, gestión de estados, práctica, guardado automático e historial de
resultados. Ver [frontend/README.md](frontend/README.md) para ejecutarla.
