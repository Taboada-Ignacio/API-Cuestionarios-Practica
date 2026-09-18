# Usuarios y sesiones
## Uso
Abrir `http://127.0.0.1:8090/#registro` para crear una cuenta. Nombre, apellido, email, contraseña (8 a 72 caracteres, máximo 72 bytes UTF-8; una mayúscula, una minúscula y un número) y datos académicos son obligatorios.
Universidad, facultad/unidad académica y carrera son buscadores con resultados seleccionables debajo; permiten navegación por flechas, Enter y Escape. Escribir un nombre sin seleccionar el resultado no confirma la opción. `No posee` completa los campos dependientes; `No encuentro mi opción` pide una aclaración. Ver CATALOGO-ACADEMICO.md.
Ingreso en `#ingresar`, perfil en `#perfil`, gestión de estados en `#cuestionarios` e intentos en `#intentos`.
Cuestionarios nuevos son borradores por defecto. Borrador: solo propietario, sin intentos. Privado: solo propietario, con intentos. Público: cualquier usuario registrado y con perfil completo puede realizarlo; claves y edición siguen reservadas al propietario.
Los resultados e historial pertenecen al usuario del intento, incluso si el cuestionario es de otra persona. Las copias históricas se conservan al cambiar el cuestionario.
## Endpoints
GET /api/v1/auth/csrf devuelve token y headerName. Enviar esa cabecera en POST, PUT y DELETE; obtenerla nuevamente tras autenticar o cerrar sesión.
POST /api/v1/auth/registro: name, lastName, email, password, academic {university, faculty, career, note}.
POST /api/v1/auth/ingresar: email, password. GET /api/v1/auth/me: perfil actual. PUT /api/v1/auth/perfil: {name, lastName, academic {university, faculty, career, note}}. POST /api/v1/auth/salir: invalidar sesión.
GET /api/v1/auth/config indica si Google está habilitado. GET /api/v1/academia/universidades, /facultades?university=ID y /carreras?faculty=ID.
GET /api/v1/bancos muestra solo los propios. `?scope=practice` muestra los propios privados/públicos activos y los públicos de terceros.
PUT /api/v1/bancos/{id}/estado acepta {status: BORRADOR|PRIVADO|PUBLICO}. POST /bancos/carga admite status opcional, por defecto BORRADOR.
Los identificadores de propietarios nunca se toman de los cuerpos del cliente.
## Seguridad
Spring Security, BCrypt con coste 12, email normalizado único, cookie HttpOnly/SameSite=Lax, CSRF, renovación del ID al autenticar y vencimiento por inactividad de 30 minutos. Cierre de sesión invalida la sesión del servidor.
El límite de ingreso es 10 solicitudes por email/IP y 100 por IP en 15 minutos; registro 30 por IP. Es un límite en memoria por instancia. Detrás de Nginx se usa la IP reenviada; el backend de producción no publica su puerto al host.
En producción usar HTTPS y COOKIE_SECURE=true. Mantener el backend privado y permitir encabezados reenviados únicamente desde el proxy confiable. Las sesiones en memoria se pierden al reiniciar el backend; una instalación con varias instancias necesitará Spring Session y un almacenamiento compartido.
No se incluye recuperación de contraseña ni verificación por email en esta etapa.
## Preparar Google
1. En Google Cloud configurar pantalla de consentimiento y cliente OAuth de tipo aplicación web.
2. Registrar exactamente `http://127.0.0.1:8090/api/v1/auth/google/callback/google` como redirección autorizada local (usar el dominio HTTPS real en producción).
3. Completar GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET en .env y poner GOOGLE_ENABLED=true. No versionar .env ni credenciales.
4. Ejecutar start.cmd para aplicar los cambios. El botón habilita el flujo OIDC de Google; no se almacena contraseña de Google ni tokens en localStorage.
Para iniciar: /api/v1/auth/google/authorize/google. El servidor valida la identidad OIDC y exige email verificado por Google. Una cuenta Google nueva debe completar formación antes de usar los paneles.
No se fusionan cuentas por email automáticamente. Un usuario local debe iniciar sesión y elegir Vincular mi cuenta con Google desde su perfil; el email debe coincidir. POST /api/v1/auth/google/vincular devuelve la URL para comenzar el vínculo.
Sin credenciales Google permanece deshabilitado. La configuración y reglas de vinculación están probadas; la prueba real del proveedor queda pendiente hasta configurar un cliente válido.
## Migración de desarrollo
V3 elimina exclusivamente preguntas, cuestionarios, configuraciones e intentos existentes, por autorización del usuario, antes de introducir propietarios obligatorios. No borra el volumen ni afecta otros proyectos Docker. No aplicar esa migración de reinicio a un entorno con datos reales.

V4 agrega apellido sin eliminar usuarios ni historial. Las cuentas anteriores sin apellido deben completarlo desde el perfil. La política nueva de contraseña se aplica al registro; las contraseñas existentes siguen permitiendo el ingreso. Google obtiene nombre y apellido por separado; si falta el apellido, deberá completarse en el perfil.
