<!-- Instantánea congelada: no se edita. -->
> **Instantánea S1 · Pila del sprint**, tal como quedó en la planificación del 8 de octubre de 2026: objetivo, capacidad, PBI elegidas y sus tareas estimadas en horas-persona. Se exportó el 22 de octubre a partir del acta de la planificación y se comprobó tarea a tarea contra los issues #34-#71 de GitHub. La pila del producto de la que se partió está en [`S1-antes-de-planificar.md`](S1-antes-de-planificar.md), con los criterios de cada PBI.
>
> Cambio posterior, durante el sprint (22 de octubre): la 2.2 (#47) pasa a 4 h y la 2.6 (#51) a 4,5 h, porque dos rutas del contrato de la API no estaban en ninguna tarea. La pila del sprint queda en 102,5 h de 103. Esta instantánea conserva las cifras de la planificación.

# Pila del sprint 1

## Objetivo del sprint

**Un grupo puede crear su cuenta, montar una partida, inscribirse y arrancarla: cada jugador ve su objetivo y su misión.**

Lo decidió el Dueño del Producto, Imad Habib Jali, el 2026-10-01. Es compatible con las PBI 1-5 y se puede enseñar de principio a fin en la revisión.

## Capacidad del equipo

La capacidad sale de la estimación de costes del proyecto (hoja de costes, pestaña «Directos», valor promedio): unas 120 h por persona en todo el proyecto, de las que se han restado las horas reales de la fase previa (Imad 17 h, Alejo 5 h, Sergio y Diego 0 h) y se ha repartido el resto entre los sprints en proporción a su duración. Quien hizo menos en la fase previa tiene más horas ahora. Las actividades Scrum y la gestión no se restan aparte: van dentro del 27 % que la hoja de costes suma sobre el desarrollo (gestión general, gestión de configuraciones y aseguramiento de la calidad).

| Persona | Para PBI (h) | Scrum y gestión, 27 % (h) | Total del sprint (h) | Ausencias conocidas |
|--|--|--|--|--|
| Imad Habib Jali | 22 | 5,9 | 27,9 | Fiestas del Pilar (10-18 oct): disponibilidad posiblemente reducida |
| Alejo Beamonte Andreu | 26 | 7,0 | 33,0 | Fiestas del Pilar (10-18 oct): disponibilidad posiblemente reducida |
| Sergio Saura Oliva | 27,5 | 7,4 | 34,9 | Fiestas del Pilar (10-18 oct): disponibilidad posiblemente reducida |
| Diego Simón Íñiguez | 27,5 | 7,4 | 34,9 | Fiestas del Pilar (10-18 oct): disponibilidad posiblemente reducida |
| **Total** | **103** | **27,8** | **130,8** | |

Las horas son un valor único, sin rango (el rango mínimo-máximo está en la hoja de costes).

## PBI y tareas

Por cada PBI elegida (en orden de prioridad): se le asigna el milestone `S1`, se parte en tareas (issues con la etiqueta `tarea` que la mencionan) y se resta su total de la capacidad restante. Las tareas, sin orden ni persona asignada, se crearon en GitHub el 2026-10-03 (#34-#71).

### PBI 1 · Cuenta del jugador (#2)

Incluye la infraestructura común (tareas 1.1 a 1.4), que no pertenece a ninguna funcionalidad concreta y hace falta para la primera. Las tareas 1.1 y 1.3 se estiman por lo que falta: el proyecto del servidor y su integración continua se empezaron el 26 de septiembre, antes del sprint.

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 1.1 | Esqueleto del servidor: integrar en `main` el proyecto Spring Boot ya iniciado (Java 21, Maven, OpenAPI, Docker Compose) y añadir PostGIS, las migraciones y la tabla de eventos | #34 | implementación | 3,5 |
| 1.2 | Esqueleto de la app: Expo con TypeScript, navegación, tema visual y cliente de la API | #35 | implementación | 3,5 |
| 1.3 | Integración continua con GitHub Actions: integrar el flujo del servidor ya iniciado (compilar, tests, JaCoCo al 75 %, Checkstyle) y añadir el de la app con ESLint | #36 | integración | 2,5 |
| 1.4 | Despliegue: imagen Docker, elección del PaaS, servidor desplegado con HTTPS y secretos, y app publicada con EAS Update | #37 | despliegue | 6,5 |
| 1.5 | Convenciones comunes del modelo de datos (nombres, claves, fechas y relaciones), modelo de datos de la cuenta y contrato de la API (registro, inicio de sesión, horario, borrado) | #38 | diseño | 3,5 |
| 1.6 | Servidor: registro e inicio de sesión, con contraseña guardada con hash | #39 | implementación | 3,5 |
| 1.7 | Servidor: horario lectivo, permiso y aviso aceptados una sola vez, y borrado de la cuenta | #40 | implementación | 3,5 |
| 1.8 | App: bienvenida (alias, email y contraseña) e inicio de sesión | #41 | implementación | 3,5 |
| 1.9 | App: horario lectivo en rejilla semanal | #42 | implementación | 3,5 |
| 1.10 | App: permiso de ubicación, aviso de disponibilidad y borrar cuenta desde el perfil | #43 | implementación | 2,5 |
| 1.11 | Tests de aceptación de los tres criterios, más los de contraseña no legible y HTTPS | #44 | pruebas | 3,5 |
| 1.12 | Manual de usuario (alta de cuenta), documento de diseño y comprobación a mano en producción | #45 | documentación | 1,5 |
| | | | **Total PBI 1** | **41** |

### PBI 2 · Crear y configurar una partida (#3)

Los pasos de misiones y de zonas seguras del asistente no entran en este sprint. El tipo de misiones, el plazo de disputas y las entradas tardías se guardan; su efecto llega con otras PBI.

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 2.1 | Modelo de datos de la partida, sus estados y contrato de la API | #46 | diseño | 1,5 |
| 2.2 | Servidor: crear la partida con sus reglas, validar las fechas, guardar el evento de creación y fijar al creador como máster | #47 | implementación | 3,5 |
| 2.3 | Servidor: código de invitación de la partida y consulta de la partida por ese código | #48 | implementación | 1,5 |
| 2.4 | App: asistente de creación, pasos «Datos» y «Reglas», y la partida en la pestaña «Partidas» | #49 | implementación | 3,5 |
| 2.5 | App: paso «Compartir» con el QR y el enlace, y apertura de la partida desde el enlace | #50 | implementación | 2,5 |
| 2.6 | Tests de aceptación de los dos criterios y test de rol (solo el máster ve y cambia la configuración) | #51 | pruebas | 2,5 |
| 2.7 | Manual de usuario (crear partida), OpenAPI al día y comprobación a mano en producción | #52 | documentación | 1,5 |
| | | | **Total PBI 2** | **16,5** |

### PBI 3 · Inscribirse en una partida (#4)

La segunda mitad del criterio 3 (el horario no cambia después del arranque) solo se puede probar con una partida arrancada: su test se escribe aquí y pasa cuando la PBI 4 está integrada.

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 3.1 | Modelo de datos de la inscripción y contrato de la API | #53 | diseño | 1,5 |
| 3.2 | Servidor: inscribirse con el código aceptando las normas, lista de inscritos y varias partidas por jugador | #54 | implementación | 3,5 |
| 3.3 | Servidor: horario lectivo propio de cada partida, editable hasta el arranque | #55 | implementación | 1,5 |
| 3.4 | App: escáner de QR y entrada por enlace | #56 | implementación | 2,5 |
| 3.5 | App: pantalla de normas y sala de espera con los inscritos y «Revisar mi horario» | #57 | implementación | 3,5 |
| 3.6 | Tests de aceptación de los cuatro criterios y test de rol (un jugador no inscrito no ve la partida) | #58 | pruebas | 2,5 |
| 3.7 | Manual de usuario (unirse a una partida), OpenAPI al día y comprobación a mano en producción | #59 | documentación | 1,5 |
| | | | **Total PBI 3** | **16,5** |

### PBI 4 · Arrancar la partida y formar la cadena de objetivos (#5)

El test que reconstruye el estado a partir de los eventos viene de la definición de hecho: esta es la primera PBI con estado de partida que cambia.

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 4.1 | Estados de la partida, algoritmo del ciclo y eventos que se guardan | #60 | diseño | 1,5 |
| 4.2 | Servidor: arranque programado en la fecha de inicio, arranque adelantado por el máster y horarios congelados al arrancar | #61 | implementación | 3,5 |
| 4.3 | Servidor: generación del ciclo único y cerrado, guardado como eventos | #62 | implementación | 2,5 |
| 4.4 | Servidor: cancelación con menos de 3 inscritos | #63 | implementación | 1,5 |
| 4.5 | App: «Arrancar ya» en la sala de espera y pantallas «La partida ha empezado» y «Partida cancelada» | #64 | implementación | 2,5 |
| 4.6 | Tests de aceptación (arranque, ciclo con 3, 4 y 10 jugadores, cancelación) y test que reconstruye el estado a partir de los eventos | #65 | pruebas | 2,5 |
| 4.7 | Manual de usuario (arranque y cancelación), OpenAPI al día y comprobación a mano en producción | #66 | documentación | 1,5 |
| | | | **Total PBI 4** | **15,5** |

### PBI 5 · Ver mi objetivo y mi misión (#31)

| N.º | Tarea | Issue | Tipo | Estimación (h) |
|--|--|--|--|--|
| 5.1 | Biblioteca curada inicial: redactar y revisar unas 20 misiones sin contacto físico ni objetos que simulen armas, y cargarlas en la base de datos | #67 | análisis | 2,5 |
| 5.2 | Servidor: repartir una misión a cada jugador al arrancar y consulta de «mi objetivo y mi misión» | #68 | implementación | 2,5 |
| 5.3 | App: carta boca abajo que se gira mientras se mantiene pulsada | #69 | implementación | 2,5 |
| 5.4 | Tests de aceptación de los dos criterios y test por rol: nadie obtiene el objetivo ni la misión de otro, tampoco el máster | #70 | pruebas | 1,5 |
| 5.5 | Manual de usuario (mi carta), OpenAPI al día y comprobación a mano en producción | #71 | documentación | 1,5 |
| | | | **Total PBI 5** | **10,5** |

### Capacidad restante

| PBI | Horas estimadas | Capacidad restante (h) |
|--|--|--|
| Inicio | — | 103 |
| 1 (#2) | 41 | 62 |
| 2 (#3) | 16,5 | 45,5 |
| 3 (#4) | 16,5 | 29 |
| 4 (#5) | 15,5 | 13,5 |
| 5 (#31) | 10,5 | 3 |

Pila del sprint: 5 PBI, 38 tareas y 100 h de las 103 de capacidad. Quedan 3 h de margen.
