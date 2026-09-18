# Ejecutar todo con Docker

Requisito único: Docker Desktop con contenedores Linux (WSL 2 en Windows).

## Inicio automático en Windows
Hacé doble clic en **Iniciar.cmd**, o ejecutá:
```powershell
.\Iniciar.cmd
```
Activa Docker Desktop si hace falta, crea .env si no existe, construye las imágenes,
inicia PostgreSQL, ejecuta Flyway al arrancar el backend, espera los controles de salud
y abre la app en http://127.0.0.1:8090.
La primera construcción descarga dependencias; puede tardar varios minutos.
Repetir el inicio aplica cambios del proyecto y conserva los datos.

## Inicio con un comando
```powershell
docker compose up --build -d --wait --wait-timeout 300
```
No requiere Java, Maven, Node ni PostgreSQL instalados en tu equipo.

## Detener y consultar
```powershell
.\Detener.cmd
docker compose ps
docker compose logs -f
```
Detener conserva el volumen postgres_data. El script no elimina datos.
Los servicios se reinician automáticamente cuando Docker se reinicia, salvo que
los hayas detenido. Para iniciar Docker al iniciar sesión, activá esa opción en
Docker Desktop.

## Configuración
Copiá .env.example a .env (el iniciador lo hace automáticamente).
APP_PORT cambia el puerto de la app. DB_PASSWORD cambia la contraseña de una base nueva.
Una base ya inicializada conserva su contraseña: cambiar .env no la cambia en PostgreSQL.
El entorno es local: solamente se publica el frontend en la interfaz 127.0.0.1.
Nginx entrega el frontend y redirige /api al backend dentro de Docker.

## Desarrollo fuera de Docker
Para publicar también PostgreSQL en 5432 y la API en 8080:
```powershell
docker compose -f compose.yaml -f compose.dev.yaml up --build -d --wait
```
Si ejecutás backend y frontend directamente, podés iniciar solo la base:
```powershell
docker compose -f compose.yaml -f compose.dev.yaml up -d postgres
```

## Verificación
La imagen frontend ejecuta las pruebas de Excel antes de compilar.
La imagen backend compila con Java 25; no ejecuta Testcontainers dentro del build.
Los controles de salud verifican PostgreSQL, la API con acceso a la base y Nginx.
Para la suite completa de backend, con Docker activo y JDK 25:
```powershell
.\mvnw.cmd clean verify
```

## Pruebas completas sin instalar Java
Ejecutá Verificar.cmd. Inicia el proyecto y después ejecuta las pruebas con Java 25
y PostgreSQL 17 dentro de un proyecto de Docker separado. No comparte datos ni
monta el socket de Docker. Los contenedores de pruebas se eliminan al finalizar.
Comando equivalente para la suite:
```powershell
docker compose -p api-cuestionarios-practica-tests -f compose.test.yaml up --build --abort-on-container-exit --exit-code-from backend-tests
docker compose -p api-cuestionarios-practica-tests -f compose.test.yaml down
```
Las pruebas usan INTEGRATION_DB_URL dentro de Compose. Sin esa variable, usan
Testcontainers como antes al ejecutarse desde el equipo.

## Start y stop manuales
Desde la carpeta del repositorio, ejecutar:
```powershell
.\start.cmd
.\stop.cmd
```
También se pueden abrir con doble clic. start construye y levanta todo, espera
que los servicios estén listos y abre la aplicación. stop detiene el proyecto
sin borrar la base de datos y sin cerrar Docker Desktop.
Para iniciar sin abrir el navegador: .\start.cmd -NoBrowser.
Versiones PowerShell disponibles: start.ps1 y stop.ps1.
Los scripts resuelven las rutas desde su propia ubicación.
