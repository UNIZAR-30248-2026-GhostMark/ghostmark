<!-- Instantánea congelada: no se edita. -->
> **Instantánea S3 · Pila del sprint**, tal como quedó en la planificación del 14 de noviembre de 2026: objetivo, capacidad, PBI elegidas y sus tareas estimadas en horas-persona, comprobadas contra los issues #275-#300 de GitHub, los defectos #248 y #254-#268 que vienen del sprint 2 y los #301-#303. La pila del producto de la que se partió está en [`S3-antes-de-planificar.md`](S3-antes-de-planificar.md), con los criterios de cada PBI.
>
> **Nota (28 de noviembre de 2026):** las PBI se re-estimaron en puntos de historia tras la corrección del informe de los sprints 1 y 2: 9 = 5, 10a = 5, 10b = 3, 11a = 8 y 11b = 8 (29 puntos; referencia: la 10b = 3). Se quitó la escala de tallas con la que se calcularon las horas; las horas de las tareas no cambian.

# Pila del sprint 3

## Objetivo del sprint

**La partida respeta la vida académica y se puede reclamar: nadie elimina ni es eliminado en su horario lectivo, el máster marca zonas seguras que se comprueban al eliminar sin guardar la posición, y una eliminación injusta se disputa y se anula.**

Lo decidió el Dueño del Producto, Imad Habib Jali, el 2026-11-14. Al acabar el sprint 2 se jugaba una partida de principio a fin, pero se podía eliminar en clase o en la biblioteca y una eliminación injusta no tenía remedio: con la 9, la 10a y la 10b el juego respeta horarios y zonas, y con la 11a y la 11b se puede reclamar y deshacer.

## Capacidad del equipo

Las tareas de los sprints 1 y 2 se hicieron más rápido de lo estimado, así que el sprint 3 se estima de forma optimista, con la escala de horas de la previsión del 8 de octubre, y las tareas de menos de 1 h cuentan 0 h por triviales. El equipo trabaja las horas que haga falta para lo planificado: la capacidad es la suma de las tareas y los defectos, y cada persona confirma las suyas. Las actividades Scrum y la gestión van aparte, en el 27 % que la hoja de costes suma sobre el desarrollo.

| Persona | Desarrollo (h) | Scrum y gestión, 27 % (h) | Total del sprint (h) |
|--|--|--|--|
| Imad Habib Jali | 14 | 3,8 | 17,8 |
| Alejo Beamonte Andreu | 16 | 4,3 | 20,3 |
| Sergio Saura Oliva | 18,5 | 5 | 23,5 |
| Diego Simón Íñiguez | 17 | 4,6 | 21,6 |
| **Total** | **65,5** | **17,7** | **83,2** |

La previsión del 8 de octubre daba 68,5 h de desarrollo para el sprint 3, de las que 4 eran para el informe.

## PBI y tareas

Por cada PBI elegida (en orden de prioridad): se le asigna el milestone `S3`, se parte en tareas (issues con la etiqueta `tarea`, *sub-issue* de su PBI) y se resta su total. Antes se restan los defectos. Las tareas no llevan orden ni persona asignada.

**Diferencia con el sprint 2.** Cuando una PBI grande se parte en dos, las dos partes se pueden hacer a la vez: la primera tarea es un diseño común y después ninguna parte espera a las tareas de la otra. Cada tarea sigue siendo de una sola capa (app, servidor o documentación) y de una sola persona, con «Tests de aceptación» y «Manual y comprobación en producción» al final de cada PBI. Los defectos de una misma capa se arreglan en una sola *pull request*.

### Defectos y mejoras que pasan al sprint 3 (se restan antes)

| Issue | Qué | Estimación (h) |
|--|--|--|
| #248 | `HttpsTest` falla en `ci-backend` con Render dormido (primero, acción 2 de la retrospectiva) | 1,5 |
| #267 | Manual del último rival (después del #260) | 0 |
| #254 | «Eliminar» con 3 intentos | 0 |
| #259 | «TERMINADO» en Android | 0 |
| #260 | Último rival sin «¡ELIMINADO!» | 0 |
| #262 | «Mi código» al ser eliminado | 0 |
| #263 | Casillas cortadas en Android | 0 |
| #266 | Empate de muchos en la pantalla de fin | 0 |
| #301 | Parte de la app del #261 (círculo de «Mi código») | 0 |
| #302 | Versión de la app en `mobile/package.json` | 0 |
| #255 | «1 inscritos» | 0 |
| #256 | Inicio no se recarga | 0 |
| #257 | Código bajo el QR | 0 |
| #258 | Pestaña «Misión» del máster | 0 |
| #264 | Aviso de arranque del máster | 0 |
| #265 | Diálogos de confirmación con el estilo de GhostMark | 1 |
| #261 | Código de vida con 60 s desde que se abre (servidor) | 0 |
| #268 | Reintento ante los 503 de Gemini | 1 |
| #303 | `GEMINI_CLAVE` y `GEMINI_MODELO` en `.env.example` | 0 |
| | **Total** | **3,5** |

### PBI 9 · Horarios seguros (#9)

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 9.1 | Diseño de los horarios seguros | #275 | diseño | 1 |
| 9.2 | Servidor: rechazo en horario lectivo y disponibilidad del objetivo | #276 | implementación | 4 |
| 9.3 | App: objetivo no disponible con cuenta atrás | #277 | implementación | 4 |
| 9.4 | Tests de aceptación de la 9 | #278 | pruebas | 2 |
| 9.5 | Manual y comprobación en producción de la 9 | #279 | documentación | 1 |
| | | | **Total PBI 9** | **12** |

### PBI 10a · Zonas seguras en el mapa (#10)

La primera tarea es el diseño común de la 10a y la 10b: modelo de zona, migración y contratos de las dos. Después las dos partes se hacen a la vez. La posición nunca llega al servidor: el móvil calcula si está dentro y envía solo el resultado.

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 10a.1 | Diseño común de las zonas seguras (10a y 10b) | #280 | diseño | 2 |
| 10a.2 | Servidor: zonas seguras del máster | #281 | implementación | 3 |
| 10a.3 | App: mapa del máster para dibujar zonas | #282 | implementación | 5 |
| 10a.4 | App: zonas seguras en la pestaña Partida | #283 | implementación | 1,5 |
| 10a.5 | Tests de aceptación de la 10a | #284 | pruebas | 1 |
| 10a.6 | Manual y comprobación en producción de la 10a | #285 | documentación | 0 |
| | | | **Total PBI 10a** | **12,5** |

### PBI 10b · La zona se comprueba al eliminar (#272)

Solo depende del diseño común (10a.1).

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 10b.1 | Servidor: resultado de zona con la eliminación | #286 | implementación | 2 |
| 10b.2 | App: comprobar la zona al meter el código | #287 | implementación | 3,5 |
| 10b.3 | Tests de aceptación de la 10b | #288 | pruebas | 1 |
| 10b.4 | Manual y comprobación en producción de la 10b | #289 | documentación | 0 |
| | | | **Total PBI 10b** | **6,5** |

### PBI 11a · Abrir y resolver una disputa (#11)

La primera tarea es el diseño común de la 11a y la 11b: modelo de la disputa, migración, contratos y la operación del servidor «anular la eliminación», que la 11a llama cuando decide el árbitro (o la zona «dentro») y que hace la 11b.

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 11a.1 | Diseño común de las disputas (11a y 11b) | #290 | diseño | 2 |
| 11a.2 | Servidor: abrir y responder una disputa | #291 | implementación | 4 |
| 11a.3 | Servidor: árbitro y decisión de la disputa | #292 | implementación | 3 |
| 11a.4 | App: abrir, responder y arbitrar una disputa | #293 | implementación | 4 |
| 11a.5 | Tests de aceptación de la 11a | #294 | pruebas | 2 |
| 11a.6 | Manual y comprobación en producción de la 11a | #295 | documentación | 1 |
| | | | **Total PBI 11a** | **16** |

### PBI 11b · Anular una eliminación (#273)

Solo depende del diseño común (11a.1); sus tests llaman directamente a la operación «anular». Es la última por prioridad: si el sprint se retrasa, es la primera que sale.

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 11b.1 | Servidor: anular una eliminación | #296 | implementación | 6 |
| 11b.2 | Servidor: misiones nuevas y anulación en el feed | #297 | implementación | 2,5 |
| 11b.3 | App: anulación en el feed y carta al revivir | #298 | implementación | 3 |
| 11b.4 | Tests de aceptación de la 11b | #299 | pruebas | 2,5 |
| 11b.5 | Manual y comprobación en producción de la 11b | #300 | documentación | 1 |
| | | | **Total PBI 11b** | **15** |

### Capacidad restante

| Parte | Horas estimadas | Capacidad restante (h) |
|--|--|--|
| Inicio (65,5 h) | — | 65,5 |
| Defectos y mejoras | 3,5 | 62 |
| 9 (#9) | 12 | 50 |
| 10a (#10) | 12,5 | 37,5 |
| 10b (#272) | 6,5 | 31 |
| 11a (#11) | 16 | 15 |
| 11b (#273) | 15 | 0 |

Pila del sprint: 5 PBI, 26 tareas y 19 defectos y mejoras, 65,5 h. Si el sprint se retrasa, sale la 11b (15 h).
