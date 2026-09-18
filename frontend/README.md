# Frontend Cuestionarios Práctica

Frontend React, Vite y Tailwind CSS con bienvenida y carga de cuestionarios.
Los paneles de intentos y resultados se muestran como próximos.
La bienvenida se puede abrir sola; guardar cuestionarios requiere el backend y PostgreSQL.

## Ejecutar
Node.js 22.12 o superior.
```powershell
cd frontend
npm install
npm run dev
```
Abrir la dirección local que muestra Vite.

## Compilar
```powershell
npm run build
npm run preview
```
El resultado se genera en dist. No se publica automáticamente.

## Panel de cuestionarios
Disponible en #cuestionarios/nuevo, desde la navegación o la bienvenida.
Carga manual de 2..10 opciones, revisión, edición y eliminación del borrador.
Importa .xlsx (hasta 5 MB y 1000 preguntas) usando la hoja Preguntas.
Modelo descargable en public/modelo-banco-preguntas.xlsx. El ejemplo no se importa.
La importación valida el archivo completo antes de agregar preguntas a la revisión.
El guardado usa POST /api/v1/bancos/carga. Vite redirige /api al backend en 8080.
Para guardar de verdad hay que ejecutar el backend y PostgreSQL.
El borrador se mantiene ante errores de guardado, pero no al recargar o salir del panel.
Para despliegue, configurar /api en el mismo origen o un proxy equivalente.
Pruebas de importación: npm test.

## Proyecto completo con Docker
Ejecutá Iniciar.cmd desde la raíz. Ver docs/DOCKER.md.

## Panel Intentos
Ruta `#intentos`: lista paginada de cuestionarios cargados, selección, participante identificado por su cuenta y cantidad de preguntas, confirmación, respuestas guardadas automáticamente y corrección final.
Permite revisar respuestas y finalizar con omisiones. Los resultados incluyen puntaje, porcentaje, explicación y opción correcta.
El identificador del intento actual se conserva en el navegador para recuperarlo al recargar. Volver al inicio desde resultados limpia esa referencia y muestra nuevamente el listado; el historial sigue persistido en el backend.
