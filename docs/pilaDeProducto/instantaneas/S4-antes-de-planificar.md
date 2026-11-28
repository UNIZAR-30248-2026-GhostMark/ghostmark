<!-- Instantánea congelada: no se edita. -->
> **Instantánea S4 · Pila del producto antes de planificar el sprint 4**, exportada de los issues de GitHub y de los campos «Prioridad» y «Puntos de historia» del Project el 28 de noviembre de 2026, después del grooming (la PBI 12 partida en 12a y 12b, criterios completos de la 12a, la 12b, la 14, la 16 y la 17, y nueva prioridad) y antes de crear las tareas del sprint 4. Las PBI 1-5 están hechas (sprint 1), la 7a, la 8, la 7b, la 6a y la 6b (sprint 2) y la 9, la 10a, la 10b, la 11a y la 11b (sprint 3). Las PBI se estiman en puntos de historia desde el 28 de noviembre (escala 1, 2, 3, 5, 8, 13, 20, 40, 100; referencia: la 10b = 3); las hechas en los sprints 1 y 2 no llevan estimación. Es la pila de partida del capítulo 5.1 del informe final.

# Pila del producto antes de planificar el sprint 4

## Prioridad y estimación

| Prioridad | PBI | Issue | Funcionalidad | Puntos | Lanzamiento | Estado |
|--|--|--|--------------------------------|--|--|--|
| 1 | 1 | #2 | Cuenta del jugador (crear cuenta e inicio de sesión) | — | L1 | Hecha (sprint 1) |
| 2 | 2 | #3 | Crear y configurar una partida | — | L1 | Hecha (sprint 1) |
| 3 | 3 | #4 | Inscribirse en una partida | — | L1 | Hecha (sprint 1) |
| 4 | 4 | #5 | Arrancar la partida y formar la cadena de objetivos | — | L1 | Hecha (sprint 1) |
| 5 | 5 | #31 | Ver mi objetivo y mi misión | — | L1 | Hecha (sprint 1) |
| 6 | 7a | #7 | Código de vida y eliminación | — | L1 | Hecha (sprint 2) |
| 7 | 8 | #8 | Cierre de partida, ganador y empate | — | L1 | Hecha (sprint 2) |
| 8 | 7b | #189 | Códigos rechazados y eliminaciones simultáneas | — | L1 | Hecha (sprint 2) |
| 9 | 6a | #6 | Misiones generadas con IA y aprobadas por el máster | — | L1 | Hecha (sprint 2) |
| 10 | 6b | #190 | La partida sigue con la biblioteca si la IA falla | — | L1 | Hecha (sprint 2) |
| 11 | 9 | #9 | Horarios seguros | 5 | L1 | Hecha (sprint 3) |
| 12 | 10a | #10 | Zonas seguras en el mapa | 5 | L1 | Hecha (sprint 3) |
| 13 | 10b | #272 | La zona se comprueba al eliminar | 3 | L1 | Hecha (sprint 3) |
| 14 | 11a | #11 | Abrir y resolver una disputa | 8 | L1 | Hecha (sprint 3) |
| 15 | 11b | #273 | Anular una eliminación | 8 | L1 | Hecha (sprint 3) |
| 16 | 12a | #12 | Abandonar y expulsar | 8 | L1 | En la pila |
| 17 | 12b | #364 | Reportar una misión o una conducta | 5 | L1 | En la pila |
| 18 | 14 | #14 | Ghostmark Wrapped y borrado de datos al cerrar | 8 | L1 | En la pila |
| 19 | 16 | #16 | Entradas tardías | 5 | L1 | En la pila |
| 20 | 17 | #17 | Importar el horario lectivo desde el calendario | 3 | L1 | En la pila |
| 21 | 13 | #13 | Avisos al móvil | 13 | L1 | En la pila |
| 22 | 15 | #15 | Amigos e invitaciones directas | 8 | L1 | En la pila |
| 23 | 18 | #32 | Acceso con Google | 5 | L1 | En la pila |
| 24 | 19 | #18 | Modo por equipos | 40 | L2 | En la pila |
| 25 | 20 | #19 | Purga final | 40 | L2 | En la pila |
| 26 | 21 | #20 | Partida relámpago | 40 | L2 | En la pila |
| 27 | 22 | #21 | Co-másters | 40 | L2 | En la pila |
| 28 | 23 | #22 | Perfiles de organización | 100 | L3 | En la pila |
| 29 | 24 | #23 | Plantillas de partida | 100 | L3 | En la pila |
| 30 | 25 | #24 | Panel de estadísticas | 100 | L3 | En la pila |
| 31 | 26 | #25 | Eventos de varias partidas | 100 | L3 | En la pila |
| 32 | 27 | #26 | Validación reforzada | 100 | L3 | En la pila |
| 33 | 28 | #27 | Premios | 100 | L3 | En la pila |

## Fichas

### 1. Cuenta del jugador (crear cuenta e inicio de sesión) (#2)

**Lanzamiento:** L1  
**Prioridad inicial:** 1 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Se pueda crear la cuenta (alias, email y contraseña) e iniciar sesión, y se verifique que con datos incorrectos no entra |
| 2 | Se pueda configurar el horario lectivo y aceptar el permiso de ubicación y el aviso de disponibilidad, y se verifique que se piden una sola vez y que la cuenta solo guarda alias, datos de acceso, horario y amigos |
| 3 | Se pueda borrar la cuenta y todos sus datos en cualquier momento, y se verifique que ya no se puede iniciar sesión y el alias desaparece |

#### No incluye

El acceso con una cuenta de Google (PBI 18) ni importar el horario desde un calendario (PBI 17).

#### Pantallas del prototipo

<img src="../pantallas/f1-a1.png" alt="Alias y acceso" width="220"> <img src="../pantallas/f1-a2.png" alt="Horario lectivo" width="220"> <img src="../pantallas/f1-a3.png" alt="Permiso de ubicación" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 2. Crear y configurar una partida (#3)

**Lanzamiento:** L1  
**Prioridad inicial:** 2 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | El máster pueda crear una partida (nombre, fechas, tipo de misiones, plazo de disputas, entradas tardías, si juega), y se verifique que se guarda con esos valores y que se rechaza un fin anterior al inicio |
| 2 | Obtenga un QR y un enlace para compartirla, y se verifique que el enlace abre esa partida |

#### No incluye

Los pasos del asistente de misiones (PBI 6) y de zonas seguras (PBI 10) ni la invitación directa a amigos (PBI 15).

#### Pantallas del prototipo

<img src="../pantallas/f2-e1.png" alt="Datos" width="220"> <img src="../pantallas/f2-e2.png" alt="Reglas" width="220"> <img src="../pantallas/f2-e5.png" alt="Compartir" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 3. Inscribirse en una partida (#4)

**Lanzamiento:** L1  
**Prioridad inicial:** 3 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Un jugador pueda entrar por QR o enlace, y se verifique que aparece en la lista de inscritos del máster |
| 2 | Acepte las normas de la partida (misiones sin contacto físico, sin objetos que simulen armas, zonas y horarios seguros y libertad para abandonar), y se verifique que sin aceptarlas no queda inscrito |
| 3 | Pueda revisar su horario lectivo hasta que arranque el juego, y se verifique que después no cambia en esa partida |
| 4 | Pueda estar en varias partidas a la vez, y se verifique que cada una es independiente |

#### No incluye

Unirse a una partida ya empezada (PBI 16).

#### Pantallas del prototipo

<img src="../pantallas/f3-d1.png" alt="Escáner de QR" width="220"> <img src="../pantallas/f3-d2.png" alt="Normas" width="220"> <img src="../pantallas/f3-d3.png" alt="Sala de espera" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

Aceptada por el Dueño del Producto (26 oct). Todas las tareas (#53-#59) están cerradas y todas las casillas de sus PR están marcadas.

| Criterio | Test de aceptación | En producción |
|--|--|--|
| 1. Entrar por QR o enlace y aparecer en la lista de inscritos del máster | `InscripcionTest.inscribirseCopiaHorarioAceptaNormasYGuardaEvento`; `acceptance-inscription.test.cjs` («CA1 - entrar por QR» y «por enlace», «CA1 - aceptar las normas inscribe al jugador») | Por la API, abrir la invitación no inscribe, un código inexistente da 404, el inscrito sale en la lista del máster y la segunda inscripción da 409 `YA_INSCRITO`. En Android 16 con el APK 0.3.0, el escáner rechaza un QR ajeno y abre la invitación con el QR de la partida, y el enlace abre GhostMark con la app abierta y cerrada (#118) |
| 2. Aceptar las normas; sin aceptarlas no queda inscrito | `AceptacionInscripcionTest.ca2SinAceptacionNoApareceEnNingunaLista`; `InscripcionTest.normasNoAceptadasNoCreanInscripcionNiEvento`; `acceptance-inscription.test.cjs` («CA2 - muestra las cuatro normas» y «CA2 - volver sin aceptar») | Por la API, sin aceptar da 400 `NORMAS_NO_ACEPTADAS` y el jugador no sale en la lista ni tiene horario. En Android 16, las cuatro normas antes de «Acepto y me uno» y, al aceptar, la sala de espera (#119) |
| 3. Revisar el horario hasta que arranca; después no cambia en esa partida | `AceptacionInscripcionTest.ca3RevisarYEditarHorarioAntesDelArranqueConservaLaInscripcion` y `ca3CambiarElHorarioGeneralNoReescribeElDeLaPartida`; `ArranqueTest.horarioQuedaCongeladoAlArrancar` | Por la API, al unirse se copia el horario de la cuenta, editarlo no toca el de la cuenta y, tras «Arrancar ya», cambiarlo da 409 `PARTIDA_NO_EDITABLE` y la partida conserva el suyo. En Android 16, «Revisar mi horario» desde la sala enseña el horario copiado (#123, #119) |
| 4. Estar en varias partidas a la vez, cada una independiente | `InscripcionTest.jugadorPuedeEstarEnVariasPartidasYVerlasSinDuplicados`; `AceptacionInscripcionTest.ca4HorarioIndependienteEntrePartidasYEntreJugadores` | Por la API, un jugador en dos partidas con fechas que se solapan: Inicio tiene las dos, cada una copia el horario de la cuenta y cambiar una no toca la otra; en la lista de cada partida solo están sus inscritos |

El defecto de interfaz #142 («1 inscritos», puntos de progreso en el horario y mensaje del 409) tiene su arreglo en revisión en la #145; no impide ningún criterio.

### 4. Arrancar la partida y formar la cadena de objetivos (#5)

**Lanzamiento:** L1  
**Prioridad inicial:** 4 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | La partida arranque sola en la fecha de inicio o antes si el máster la adelanta, y se verifique que pasa a estar en juego |
| 2 | Con al menos 3 jugadores se forme un único ciclo cerrado, y se verifique con 3, 4 y 10 jugadores que cada uno tiene un objetivo, es objetivo de otro y no se persigue a sí mismo |
| 3 | Con menos de 3 inscritos la partida se cancele, y se verifique que no llega a arrancar y que los inscritos lo ven al abrir la aplicación |

#### No incluye

El aviso al móvil de arranque o de cancelación (PBI 13).

#### Pantallas del prototipo

<img src="../pantallas/f4-arranca.png" alt="La partida arranca" width="220"> <img src="../pantallas/f4-cancelada.png" alt="Partida cancelada" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 5. Ver mi objetivo y mi misión (#31)

**Lanzamiento:** L1  
**Prioridad inicial:** 5 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Cada jugador vea solo su objetivo y su misión, y se verifique que nadie ve los ajenos, tampoco el máster si juega |
| 2 | Cada jugador reciba al arrancar una misión de la biblioteca curada, y se verifique que ninguna implica contacto físico ni objetos que simulen armas |

#### No incluye

Las misiones generadas con IA ni la aprobación del máster (PBI 6).

#### Pantallas del prototipo

<img src="../pantallas/f4-f1.png" alt="Carta boca abajo" width="220"> <img src="../pantallas/f4-f2.png" alt="Carta girada" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 7a. Código de vida y eliminación (#7)

**Lanzamiento:** L1  
**Prioridad inicial:** 7 de 28 · **Partida el 30 oct:** los antiguos criterios 4 y 5 pasan a la PBI 7b (#189)

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Cada jugador vivo vea en su móvil un código de 6 dígitos que cambia cada 60 s mientras tiene abierta su pantalla, y se verifique que solo lo ve él |
| 2 | El cazador introduzca el código de su víctima, y se verifique que ella pasa a eliminada, aparece en el feed como «X fue eliminado» sin decir quién y al cazador se le suma la eliminación |
| 3 | El cazador herede objetivo y misión, y se verifique que el ciclo sigue siendo único y cerrado y que con dos vivos cada uno es objetivo del otro |

#### No incluye

Los códigos rechazados, el bloqueo tras 3 fallos y las eliminaciones simultáneas (PBI 7b), los horarios seguros (PBI 9), las zonas seguras y el permiso de ubicación (PBI 10), las disputas (PBI 11) ni los avisos al móvil (PBI 13).

#### Pantallas del prototipo

<img src="../pantallas/f-codigo.png" alt="Mi código de vida" width="220"> <img src="../pantallas/f-introducir.png" alt="Introducir el código de la víctima" width="220"> <img src="../pantallas/f-acierto.png" alt="Eliminación aceptada" width="220"> <img src="../pantallas/f-feed.png" alt="Feed de la partida" width="220"> <img src="../pantallas/g-cazado.png" alt="Te han cazado" width="220">

En el feed del S2 solo aparece «X fue eliminado»; las anulaciones, abandonos y expulsiones llegan con sus PBI.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 8. Cierre de partida, ganador y empate (#8)

**Lanzamiento:** L1  
**Prioridad inicial:** 8 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | La partida termine al quedar un único superviviente, al llegar la fecha de fin o cuando la cierre el máster, y se verifique con los tres casos |
| 2 | Se verifique que en la fecha de fin gana quien más eliminaciones tiene y que un empate en cabeza es empate de partida |

#### No incluye

Las disputas (PBI 11), el Wrapped y el borrado de datos (PBI 14) ni el aviso de cierre (PBI 13).

#### Pantallas del prototipo

<img src="../pantallas/f-partida.png" alt="Pestaña Partida, con «Cerrar partida» para el máster" width="220"> <img src="../pantallas/i-fin.png" alt="La partida ha terminado, con el ganador" width="220">

Al cerrar, la pantalla de fin dice quién gana («Gana X con N eliminaciones», «X, último superviviente» o «Empate entre X e Y») y lleva a «Volver a mis partidas»; el Wrapped llega con la PBI 14.

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 7b. Códigos rechazados y eliminaciones simultáneas (#189)

**Lanzamiento:** L1  
**Prioridad inicial:** — · **Sale de partir la PBI 7 (#7) el 30 oct:** son sus antiguos criterios 4 y 5

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Se verifique que se rechaza un código caducado, ajeno o de una víctima ya eliminada, y que tras 3 fallos seguidos se bloquean los intentos hasta que el código rote |
| 2 | Se verifique que, si dos eliminaciones coinciden en la cadena, solo vale la que el sistema registra primero |

#### No incluye

Ver el código, eliminar y heredar (PBI 7a), los horarios seguros (PBI 9) ni las disputas (PBI 11).

#### Pantallas del prototipo

<img src="../pantallas/f-introducir.png" alt="Introducir el código, con los intentos que quedan" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 6a. Misiones generadas con IA y aprobadas por el máster (#6)

**Lanzamiento:** L1  
**Prioridad inicial:** 6 de 28 · **Partida el 30 oct:** el antiguo criterio 3 pasa a la PBI 6b (#190)

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | El máster pida misiones a la IA con un contexto (por ejemplo, «campus») y las apruebe o rechace una a una, y se verifique que ninguna entra en juego sin su aprobación |
| 2 | Se verifique que una misión de la IA con formato inválido o de una categoría prohibida no llega al máster |

#### No incluye

Seguir con la biblioteca si la IA falla (PBI 6b), el reparto inicial desde la biblioteca (PBI 5) ni reportar una misión (PBI 12).

El máster pide tandas de 8 misiones, hasta 40 por partida; al arrancar, si hay menos aprobadas que jugadores, se completa con la biblioteca. Proveedor: Gemini con la clave gratuita de Google AI Studio.

#### Pantallas del prototipo

<img src="../pantallas/f5-e3a.png" alt="Misiones con IA y contexto" width="220"> <img src="../pantallas/f5-e3b.png" alt="Aprobar las misiones" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 6b. La partida sigue con la biblioteca si la IA falla (#190)

**Lanzamiento:** L1  
**Prioridad inicial:** — · **Sale de partir la PBI 6 (#6) el 30 oct:** es su antiguo criterio 3

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Si la IA falla o no responde, la partida siga con misiones de la biblioteca curada, y se verifique con el servicio simulado caído que no se bloquea |
| 2 | Si la IA falla, no responde a tiempo o se ha agotado la cuota gratuita cuando el máster pide misiones, el máster vea que se usarán misiones de la biblioteca y pueda seguir creando la partida, y se verifique con el servicio simulado caído |

#### No incluye

Pedir y aprobar misiones de la IA (PBI 6a).

#### Pantallas del prototipo

<img src="../pantallas/f5-e3a.png" alt="Paso de misiones del asistente" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 9. Horarios seguros (#9)

**Puntos de historia:** 5  
**Lanzamiento:** L1  
**Prioridad inicial:** 9 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Un intento de eliminación se rechace si el cazador o la víctima están en su horario lectivo, y se verifique que se acepta fuera de él |
| 2 | El cazador vea cuándo su objetivo pasa a no disponible (con cuenta atrás) y cuándo vuelve a estarlo, y se verifique que lo ve en el momento del cambio |

#### No incluye

Los avisos al móvil (PBI 13), importar el horario desde un `.ics` (PBI 17) ni el bloqueo por una disputa abierta (PBI 11a).

#### Pantallas del prototipo

<img src="../pantallas/f-dormido.png" alt="Objetivo no disponible, con cuenta atrás" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 10a. Zonas seguras en el mapa (#10)

**Puntos de historia:** 5  
**Lanzamiento:** L1  
**Prioridad inicial:** 10 de 28 · **Partida el 14 nov** en 10a (esta) y 10b: el máster gestiona las zonas y los jugadores las ven; la comprobación al eliminar va en la 10b

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | El máster dibuje, modifique y borre zonas seguras en el mapa al crear la partida o mientras está en inscripción, y se verifique que se guardan, valen para toda la partida y no se pueden cambiar tras el arranque |
| 2 | Cada jugador de la partida vea en la pestaña Partida el mapa con las zonas y sus nombres, y se verifique que no muestra la posición de nadie |

#### No incluye

Comprobar la zona al eliminar ni el permiso de ubicación (PBI 10b) ni las disputas por zona (PBI 11a).

#### Pantallas del prototipo

<img src="../pantallas/e4-zonas.png" alt="Dibujo de zonas seguras" width="220"> <img src="../pantallas/f-partida-zonas.png" alt="Pestaña Partida con las zonas seguras" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 10b. La zona se comprueba al eliminar (#272)

**Puntos de historia:** 3  
**Lanzamiento:** L1  
**Prioridad inicial:** — · **Sale de partir la PBI 10 (#10) el 14 nov:** su antiguo criterio 2

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Al introducir el código, el móvil del cazador compruebe si está en una zona segura, y se verifique que la eliminación se acepta siempre y que solo se guarda «dentro», «fuera» o «no concluyente», nunca su posición |
| 2 | Un jugador sin permiso de ubicación intente introducir un código, y se verifique que no puede hasta concederlo |

#### No incluye

Dibujar ni ver las zonas (PBI 10a) ni lo que pasa con el resultado de zona en una disputa (PBI 11a).

#### Pantallas del prototipo

<img src="../pantallas/f-introducir.png" alt="Introducir el código" width="220"> <img src="../pantallas/f-sin-ubicacion.png" alt="Sin permiso de ubicación" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 11a. Abrir y resolver una disputa (#11)

**Puntos de historia:** 8  
**Lanzamiento:** L1  
**Prioridad inicial:** 11 de 28 · **Partida el 14 nov** en 11a (esta) y 11b: abrir, responder y arbitrar aquí; los efectos de anular, en la 11b

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | La víctima abra una disputa con un texto en los 10 minutos siguientes a su eliminación y el cazador responda, y se verifique que pasado ese plazo ya no se puede, salvo la del cazador cuando la víctima se niega a enseñar el código, que se admite hasta el cierre |
| 2 | Se verifique que, con una disputa abierta sobre una eliminación suya, el cazador no puede intentar otra |
| 3 | Arbitre el máster o, si está implicado o vence el plazo para resolver, un jugador sorteado no implicado, y se verifique que solo ve cazador, víctima, misión y las dos versiones, y puede anular o confirmar |
| 4 | Se verifique que con resultado de zona «dentro» la eliminación se anula sola y con «no concluyente» decide el máster |
| 5 | Se verifique que la partida no se cierra mientras quede plazo de disputa o alguna disputa abierta |

#### No incluye

Lo que pasa al anular una eliminación (PBI 11b), los avisos al móvil (PBI 13) ni expulsar a un jugador con una disputa abierta (PBI 12). El criterio 5 viene de la PBI 8.

#### Pantallas del prototipo

<img src="../pantallas/g-cazado.png" alt="Te han cazado, con «Abrir disputa»" width="220"> <img src="../pantallas/h-disputa.png" alt="Disputa" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 11b. Anular una eliminación (#273)

**Puntos de historia:** 8  
**Lanzamiento:** L1  
**Prioridad inicial:** — · **Sale de partir la PBI 11 (#11) el 14 nov:** su antiguo criterio 2, con el reparto de misiones tras anular

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Se anule una eliminación, y se verifique que la víctima revive con las eliminaciones que dependían de ella, el marcador se recalcula y se rehace un único ciclo cerrado entre los vivos, sin volver a emparejar a los implicados si se puede |
| 2 | Se verifique que las misiones de cazador y víctima se sustituyen por otras de la biblioteca sin aprobación y que la anulación se ve en el feed de todos |

#### No incluye

Abrir, responder y arbitrar la disputa (PBI 11a) ni el aviso de cambio de objetivo (PBI 13).

#### Pantallas del prototipo

<img src="../pantallas/f-feed.png" alt="Feed" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 12a. Abandonar y expulsar (#12)

**Puntos de historia:** 8  
**Lanzamiento:** L1  
**Prioridad inicial:** 12 de 28 · **Partida el 28 nov:** el criterio de reportar pasa a la PBI 12b (#364)

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Un jugador pueda abandonar la partida cuando quiera, sin dar explicaciones, y se verifique que su cazador hereda su objetivo y su misión y que no suma eliminación |
| 2 | El máster pueda expulsar a un jugador, y se verifique que queda como «expulsado» a la vista de todos, que su cazador hereda igual que en un abandono, que sus eliminaciones anteriores siguen contando y que, si tenía una disputa abierta, la pierde |

#### No incluye

Reportar (PBI 12b) ni los avisos de expulsión o de objetivo nuevo (PBI 13).

#### Pantallas del prototipo

<img src="../pantallas/f-partida.png" alt="Pestaña Partida, con «Abandonar»" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 12b. Reportar una misión o una conducta (#364)

**Puntos de historia:** 5  
**Lanzamiento:** L1  
**Prioridad inicial:** — · **Sale de partir la PBI 12 (#12) el 28 nov:** su antiguo criterio de reportar

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Cualquier jugador pueda reportar una misión o una conducta, y se verifique que el reporte le llega al máster en «Tareas de máster» |
| 2 | Se verifique que, si quien reportó borra su cuenta, sus reportes ya no lo identifican: su alias pasa a «cuenta eliminada» |

#### No incluye

Expulsar (PBI 12a), un panel para que el equipo de Ghostmark revise dentro de la app las misiones de la biblioteca reportadas, ni el aviso de reporte recibido (PBI 13).

#### Pantallas del prototipo

<img src="../pantallas/f-partida.png" alt="Pestaña Partida, con «Reportar» y «Abandonar»" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 14. Ghostmark Wrapped y borrado de datos al cerrar (#14)

**Puntos de historia:** 8  
**Lanzamiento:** L1  
**Prioridad inicial:** 14 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Al cerrar la partida, cada jugador vea su Wrapped, y se verifique que incluye su participación, la duración de la partida, sus eliminaciones y las misiones que usó |
| 2 | El jugador pueda compartir su Wrapped fuera de la app como imagen, y se verifique que solo muestra alias, sin objetivos, misiones ni códigos |
| 3 | Se verifique que, tras el cierre, se borran los objetivos, las misiones y los códigos de esa partida, queda solo el resumen agregado y se conservan las cuentas de los jugadores |

#### No incluye

El aviso de partida cerrada (PBI 13) ni las estadísticas de organización (PBI 25).

#### Pantallas del prototipo

<img src="../pantallas/i-w1.png" alt="Wrapped, historia 1" width="220"> <img src="../pantallas/i-w2.png" alt="Wrapped, historia 2" width="220"> <img src="../pantallas/i-w3.png" alt="Wrapped, historia 3" width="220"> <img src="../pantallas/i-w4.png" alt="Wrapped, historia 4" width="220"> <img src="../pantallas/i-w5.png" alt="Wrapped, historia 5" width="220"> <img src="../pantallas/i-w6.png" alt="Wrapped, historia 6" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 16. Entradas tardías (#16)

**Puntos de historia:** 5  
**Lanzamiento:** L1  
**Prioridad inicial:** 16 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Un jugador pueda unirse a una partida empezada si el máster permitió las entradas tardías, y se verifique que, si no las permitió, no puede |
| 2 | El recién llegado entre en la cadena en una posición al azar, y se verifique que la cadena sigue siendo un único ciclo cerrado y que quien lo persigue recibe su objetivo nuevo |
| 3 | Se verifique que la misión inicial del recién llegado sale de la biblioteca, sin aprobación del máster |

#### No incluye

Invitar a amigos (PBI 15) ni el aviso a quien persigue al recién llegado (PBI 13).

#### Pantallas del prototipo

<img src="../pantallas/f2-e2.png" alt="Reglas, con «Permitir entradas tardías»" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 17. Importar el horario lectivo desde el calendario (#17)

**Puntos de historia:** 3  
**Lanzamiento:** L1  
**Prioridad inicial:** 17 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Un jugador pueda importar un fichero .ics con su horario, y se verifique que sus clases semanales aparecen en la rejilla y puede corregirlas antes de guardar |
| 2 | Se verifique que, con un fichero no válido o sin clases, se muestra un error y el horario queda como estaba |

#### No incluye

Sincronizar con Google Calendar u otros servicios ni otros formatos que .ics.

#### Pantallas del prototipo

<img src="../pantallas/f1-a2.png" alt="Horario lectivo, con «Importar calendario»" width="220">

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 13. Avisos al móvil (#13)

**Puntos de historia:** 13  
**Lanzamiento:** L1  
**Prioridad inicial:** 13 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Cambie algo en la partida, y se verifique que el aviso llega al momento y solo a quien toca |
| 2 | Un jugador tenga una disputa abierta o le toque arbitrar una, y se verifique que la carta de misión y la tarjeta de la partida en Inicio lo marcan sin entrar en Partida |
| 3 | Se verifique que, si el jugador borra su cuenta, lo que esta PBI guarda de él se borra o deja de identificarlo: su alias pasa a «cuenta eliminada» |

<!-- Nivel mínimo: un criterio. Se detalla al acercarse su planificación. -->

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 15. Amigos e invitaciones directas (#15)

**Puntos de historia:** 8  
**Lanzamiento:** L1  
**Prioridad inicial:** 15 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Un jugador añada amigos por su alias y el máster los invite a una partida, y se verifique que la invitación les llega en la aplicación |
| 2 | Se verifique que, si el jugador borra su cuenta, lo que esta PBI guarda de él se borra o deja de identificarlo: su alias pasa a «cuenta eliminada» |

<!-- Nivel mínimo: un criterio. Se detalla al acercarse su planificación. -->

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 18. Acceso con Google (#32)

**Puntos de historia:** 5  
**Lanzamiento:** L1  
**Prioridad inicial:** 18 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Un jugador cree su cuenta o inicie sesión con su cuenta de Google, y se verifique que no se le pide contraseña y que la cuenta guarda los mismos datos |
| 2 | Se verifique que, si el jugador borra su cuenta, lo que esta PBI guarda de él se borra o deja de identificarlo: su alias pasa a «cuenta eliminada» |

<!-- Nivel mínimo: un criterio. Se detalla al acercarse su planificación. -->

#### Tareas

Se crean en la planificación del sprint: cada tarea es un issue que menciona esta PBI.

### 19. Modo por equipos (#18)

**Puntos de historia:** 40  
**Lanzamiento:** L2  
**Prioridad inicial:** 19 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Se pueda jugar una partida por equipos |

*Funcionalidad del lanzamiento L2. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.*

#### Tareas

Ninguna hasta que se desglose.

### 20. Purga final (#19)

**Puntos de historia:** 40  
**Lanzamiento:** L2  
**Prioridad inicial:** 20 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Una partida con fecha límite pueda terminar con un ganador gracias a una purga final |

*Funcionalidad del lanzamiento L2. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.*

#### Tareas

Ninguna hasta que se desglose.

### 21. Partida relámpago (#20)

**Puntos de historia:** 40  
**Lanzamiento:** L2  
**Prioridad inicial:** 21 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Se pueda jugar una partida corta, pensada para una sola tarde |

*Funcionalidad del lanzamiento L2. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.*

#### Tareas

Ninguna hasta que se desglose.

### 22. Co-másters (#21)

**Puntos de historia:** 40  
**Lanzamiento:** L2  
**Prioridad inicial:** 22 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Varios másters puedan repartirse la gestión de una partida grande |

*Funcionalidad del lanzamiento L2. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.*

#### Tareas

Ninguna hasta que se desglose.

### 23. Perfiles de organización (#22)

**Puntos de historia:** 100  
**Lanzamiento:** L3  
**Prioridad inicial:** 23 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Una organización pueda gestionar sus partidas desde un perfil propio |

*Funcionalidad del lanzamiento L3. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.*

#### Tareas

Ninguna hasta que se desglose.

### 24. Plantillas de partida (#23)

**Puntos de historia:** 100  
**Lanzamiento:** L3  
**Prioridad inicial:** 24 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Una organización pueda reutilizar la configuración de sus partidas |

*Funcionalidad del lanzamiento L3. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.*

#### Tareas

Ninguna hasta que se desglose.

### 25. Panel de estadísticas (#24)

**Puntos de historia:** 100  
**Lanzamiento:** L3  
**Prioridad inicial:** 25 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Una organización pueda consultar estadísticas agregadas de sus partidas |

*Funcionalidad del lanzamiento L3. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.*

#### Tareas

Ninguna hasta que se desglose.

### 26. Eventos de varias partidas (#25)

**Puntos de historia:** 100  
**Lanzamiento:** L3  
**Prioridad inicial:** 26 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Una organización pueda agrupar varias partidas en un mismo evento |

*Funcionalidad del lanzamiento L3. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.*

#### Tareas

Ninguna hasta que se desglose.

### 27. Validación reforzada (#26)

**Puntos de historia:** 100  
**Lanzamiento:** L3  
**Prioridad inicial:** 27 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Las eliminaciones se validen con más garantías frente a las trampas |

*Funcionalidad del lanzamiento L3. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.*

#### Tareas

Ninguna hasta que se desglose.

### 28. Premios (#27)

**Puntos de historia:** 100  
**Lanzamiento:** L3  
**Prioridad inicial:** 28 de 28

#### Criterios de satisfacción

| N.º | Estará cumplido cuando… |
|--|--|
| 1 | Una organización pueda ofrecer premios con reglas definidas de antemano |

*Funcionalidad del lanzamiento L3. Todavía sin desglosar: se partirá en PBI más pequeñas cuando se acerque su lanzamiento.*

#### Tareas

Ninguna hasta que se desglose.
