<!-- Instantánea congelada: no se edita. -->
> **Instantánea S2 · Pila del sprint**, tal como quedó en la planificación del 30 de octubre de 2026: objetivo, capacidad, PBI elegidas y sus tareas estimadas en horas-persona, comprobadas contra los issues #192-#204 de GitHub y los #185-#188 que vienen del sprint 1. La pila del producto de la que se partió está en [`S2-antes-de-planificar.md`](S2-antes-de-planificar.md), con los criterios de cada PBI.

# Pila del sprint 2

## Objetivo del sprint

**Se puede jugar una partida de principio a fin: cada jugador elimina con el código de vida, hereda objetivo y misión, y la partida se cierra con un ganador.**

Lo decidió el Dueño del Producto, Imad Habib Jali, el 2026-10-30. Al acabar el sprint 1 la partida arrancaba y cada jugador veía su carta, pero no se podía jugar; con la 7a, la 7b y la 8 se juega y termina. Para eso la 7a, la 7b y la 8 pasaron por delante de las misiones con IA (6a y 6b).

## Capacidad del equipo

Las horas son las de la previsión del proyecto para el sprint 2, hecha el 8 de octubre con la misma base que la del sprint 1 (unas 120 h por persona en todo el proyecto). Incluyen 8 h para el informe de los sprints 1 y 2, que no son de ninguna PBI. Las actividades Scrum y la gestión van aparte, en el 27 % que la hoja de costes suma sobre el desarrollo.

| Persona | Desarrollo (h) | Scrum y gestión, 27 % (h) | Total del sprint (h) |
|--|--|--|--|
| Imad Habib Jali | 14,5 | 3,9 | 18,4 |
| Alejo Beamonte Andreu | 17 | 4,6 | 21,6 |
| Sergio Saura Oliva | 18,5 | 5,0 | 23,5 |
| Diego Simón Íñiguez | 18,5 | 5,0 | 23,5 |
| **Total** | **68,5** | **18,5** | **87** |

Capacidad para la pila del sprint: 68,5 − 8 del informe = 60,5 h.

## PBI y tareas

Por cada PBI elegida (en orden de prioridad): se le asigna el milestone `S2`, se parte en tareas (issues con la etiqueta `tarea`, *sub-issue* de su PBI) y se resta su total. Antes se restan los defectos y mejoras que vienen del sprint 1. Las tareas, sin orden ni persona asignada, se crearon en GitHub el 2026-10-30.

**Diferencia con el S1.** Las tareas se unen por pieza de trabajo para que haya menos *pull requests*: el servidor de una PBI va en una o dos tareas según tenga una o dos partes independientes, la app igual, y los tests de aceptación, el manual y la comprobación en producción de cada PBI van juntos en una tarea al final (dos personas como mucho). En el S1 salieron 38 tareas de 2,7 h de media; aquí, 13 de 3,9 h.

### Defectos y mejoras del S1 que pasan al S2 (se restan antes)

| Issue | Qué | Tipo | Estimación (h) |
|--|--|--|--|
| #185 | Mensajes al añadir y quitar franjas del horario (R11-1): resultado de `mergeRange`, mensaje en el modal y un test por caso | mejora | 2,5 |
| #186 | «EMPEZADO!» no se ve en Android (R11-2): altura de línea, caja del título y test en `bangers.test.cjs` | defecto | 1 |
| #187 | Inicio no destaca partidas canceladas (R10-37): criterio de la partida destacada y tests | mejora | 1,5 |
| #188 | Documentación al día tras el S1 (R10-40): `arquitectura.md` con las PBI 2-5, capturas del manual rehechas y aceptación de las PBI 1 y 2 | mejora | 4,5 |
| | | | **Total: 9,5** |

### PBI 7a · Código de vida y eliminación (#7)

La partida solo existe hoy como eventos (`CICLO_FORMADO`, `MISIONES_REPARTIDAS`) y la carta se calcula con el ciclo inicial. Con las eliminaciones el ciclo cambia, así que el diseño tiene que decidir cómo se obtiene el objetivo y la misión actuales (reproducir los eventos o una tabla con el estado de cada jugador) sin romper el test que reconstruye el estado.

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 7a.1 | Diseño: estado de cada jugador en la partida (vivo o eliminado, objetivo, misión, eliminaciones), eventos nuevos, secreto y cálculo del código de vida, y contrato en OpenAPI (mi código, eliminar, feed y carta tras heredar) | #192 | diseño | 2,5 |
| 7a.2 | Servidor: código de vida de 6 dígitos que cambia cada 60 s, que solo obtiene su dueño vivo | #193 | implementación | 3 |
| 7a.3 | Servidor: eliminación con el código del objetivo, herencia de objetivo y misión con el ciclo cerrado, eliminaciones del cazador y feed sin decir quién | #194 | implementación | 6,5 |
| 7a.4 | App: «Mi código» con la cuenta atrás y «Eliminar» con el teclado y la pantalla de acierto | #195 | implementación | 5 |
| 7a.5 | App: «Te han cazado», pestaña Feed y carta con el objetivo y la misión heredados | #196 | implementación | 3,5 |
| 7a.6 | Tests de aceptación de los tres criterios (con dos vivos, cada uno objetivo del otro), manual de usuario y comprobación a mano en producción y en Android | #197 | pruebas y documentación | 5 |
| | | | **Total PBI 7a** | **25,5** |

### PBI 7b · Códigos rechazados y eliminaciones simultáneas (#189)

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 7b.1 | Servidor: rechazo de códigos caducados, ajenos o de un eliminado, bloqueo tras 3 fallos hasta que el código rote y, si dos eliminaciones coinciden, solo la primera; errores en OpenAPI | #198 | implementación | 4 |
| 7b.2 | App: «Te quedan N intentos», mensajes de cada rechazo y bloqueo con cuenta atrás en «Eliminar» | #199 | implementación | 2 |
| 7b.3 | Tests de aceptación de los dos criterios (incluida la concurrencia), manual de usuario y comprobación a mano en producción | #200 | pruebas y documentación | 2,5 |
| | | | **Total PBI 7b** | **8,5** |

### PBI 8 · Cierre de partida, ganador y empate (#8)

El cierre por fecha de fin sigue el mismo camino que el arranque: tarea programada cada minuto y cierre pendiente al consultar, porque Render duerme el servidor. El borrado de los datos al cerrar es de la PBI 14.

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 8.1 | Diseño: transición a `CERRADA` por las tres vías, evento de cierre con el resultado (ganador, último superviviente o empate) y contrato en OpenAPI (cerrar y resultado) | #201 | diseño | 1,5 |
| 8.2 | Servidor: cierre al quedar uno, en la fecha de fin (programado y pendiente al consultar) y por el máster; ganador por eliminaciones y empate en cabeza | #202 | implementación | 6 |
| 8.3 | App: pestaña Partida con «Cerrar partida» para el máster, pantalla de fin con el resultado y partidas terminadas en Inicio | #203 | implementación | 5 |
| 8.4 | Tests de aceptación de los tres casos de cierre, del ganador y del empate, manual de usuario y comprobación a mano en producción y en Android | #204 | pruebas y documentación | 4,5 |
| | | | **Total PBI 8** | **17** |

### Total del sprint

| Parte | Horas estimadas | Acumulado (h) |
|--|--|--|
| Defectos y mejoras (#185-#188) | 9,5 | 9,5 |
| 7a (#7) | 25,5 | 35 |
| 7b (#189) | 8,5 | 43,5 |
| 8 (#8) | 17 | 60,5 |
| Informe de los sprints 1 y 2 (15 nov), fuera de la pila | 8 | 68,5 |

Pila del sprint: 3 PBI, 13 tareas y 4 issues del S1, 60,5 h; con el informe, 68,5 h, las mismas de la previsión del 8 de octubre. Si al consultar las horas con el equipo no llega, sale primero la 7b (8,5 h): el objetivo se cumple sin ella, porque solo protege frente a trampas y casos límite.
