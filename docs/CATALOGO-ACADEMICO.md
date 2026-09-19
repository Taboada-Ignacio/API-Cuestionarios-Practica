# Catálogo académico
Fuente: [Guía oficial SIU de grado y pregrado](https://guiadecarreras.siu.edu.ar/ciie_ofertas/2.0/guia_grado.php).
Consulta: 2026-09-18T20:52:56.927Z. Filtros nacionales Estatal (PU) y Privado (PR), nivel 1.
Se procesaron 13380 registros y se deduplicaron relaciones por institución, unidad académica y título.
Catálogo: 129 instituciones, 1251 unidades académicas, 13371 relaciones de títulos. Se incorporó la duración oficial en años para 13346 relaciones; semestres y cuatrimestres se convierten a años y se redondean hacia arriba para limitar el selector de año sin excluir el último ciclo.
Las unidades académicas son las publicadas por SIU: pueden ser facultades, institutos, departamentos o sedes. Los nombres de carrera corresponden a títulos; no se incluyen posgrados en esta etapa.
Es una instantánea de la oferta publicada por SIU, sin garantía de exhaustividad o vigencia de cada oferta. No usa el antiguo dataset de 2019 de Datos Argentina.
El archivo `src/main/resources/academic-catalog.tsv` conserva IDs de institución SIU, IDs derivados estables para unidades/títulos y `durationYears`. Para actualizarlo conservar nombres e IDs existentes; una baja no debe invalidar perfiles históricos. El script `scripts/expand-academic-catalog.ps1` vuelve a consultar las ofertas públicas presencial/a distancia, estatales/privadas, y actualiza las duraciones.
El registro admite NONE (No posee), MISSING (No encuentro mi opción, requiere aclaración) o una relación válida del catálogo. Los perfiles guardan identificadores; no exponen contraseñas ni identidad interna de Google.
