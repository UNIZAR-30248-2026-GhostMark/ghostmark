# PBI 3 — Tests de aceptación (#58)

Referencias: PBI 3 (#4), tarea 3.6 (#58) y los cuatro criterios de
`docs/pilaDeProducto/instantaneas/S1-especificacion-revisada.md`.

## Alcance y trazabilidad

La tarea pide al menos un test automático por cada caso de los criterios,
un test de acceso de un jugador no inscrito y ampliar el de borrado de cuenta.
La comprobación del horario después del arranque real la hace el test del
arranque de la PBI 4 (#61). No exige una suite de todos los detalles de implementación de #57.

Se reutilizan las pruebas existentes de 3.2, 3.3 y 3.4. Los casos nuevos
completan la cobertura por capas, sin modificar producción ni migraciones.

| Criterio o caso | Pruebas automáticas |
| --- | --- |
| CA1: entrada por QR | `invitation.test.cjs`: lectura, navegación y permisos; `acceptance-inscription.test.cjs`: «entrar por QR consulta la partida y permite revisar sus normas» |
| CA1: entrada por enlace | `invitation.test.cjs`: app cerrada, app abierta y sesión; `acceptance-inscription.test.cjs`: «entrar por enlace consulta la partida y permite revisar sus normas» |
| CA1: el jugador inscrito aparece en la lista del máster | `InscripcionTest.inscribirseCopiaHorarioAceptaNormasYGuardaEvento`: alta mediante API real y comprobación de id y alias en la lista del máster; `acceptance-inscription.test.cjs`: «aceptar las normas inscribe al jugador y la sala le muestra en la lista del máster», del enlace a la sala |
| CA2: las cuatro normas se muestran antes de aceptar | `acceptance-inscription.test.cjs`: «muestra las cuatro normas antes de aceptar» |
| CA2: aceptación explícita y rechazo | `InscripcionTest.inscribirseCopiaHorarioAceptaNormasYGuardaEvento` y `normasNoAceptadasNoCreanInscripcionNiEvento` verifican aceptación guardada y rechazo sin inscripción; `inscription.test.cjs`: «aceptar las normas inscribe y abre la sala de esa partida» y «si el servidor rechaza la inscripción no entra en la sala» |
| CA2: volver sin aceptar | `acceptance-inscription.test.cjs`: «volver sin aceptar no inscribe ni anuncia que el jugador se ha unido» |
| CA2: aceptación omitida o nula | `AceptacionInscripcionTest.ca2SinAceptacionNoApareceEnNingunaLista`, dos casos parametrizados |
| CA3: consultar y editar horario antes del arranque | `AceptacionInscripcionTest.ca3RevisarYEditarHorarioAntesDelArranqueConservaLaInscripcion`; `inscription.test.cjs`: «revisar el horario de una partida no toca el de la cuenta» |
| CA3: conservar la copia al cambiar el horario general | `AceptacionInscripcionTest.ca3CambiarElHorarioGeneralNoReescribeElDeLaPartida` |
| CA3: horario inmutable después del arranque real | `ArranqueTest.horarioQuedaCongeladoAlArrancar`: con tres inscritos el máster pulsa «Arrancar ya» y cambiar el horario de la partida da 409 `PARTIDA_NO_EDITABLE` |
| CA4: varias partidas simultáneas | `InscripcionTest.jugadorPuedeEstarEnVariasPartidasYVerlasSinDuplicados` |
| CA4: independencia entre partidas y jugadores | `AceptacionInscripcionTest.ca4HorarioIndependienteEntrePartidasYEntreJugadores` |
| Rol: un jugador no inscrito no ve datos privados | `AceptacionInscripcionTest.rolAjenoNoVeLaPartidaEnSusPartidasNiAccedeADatosPrivados` |
| Borrado de cuenta ampliado | `BorradoCuentaTest.borrarCuentaEliminaDatosEImpideIniciarSesion`: crea inscripción y horario, borra y comprueba su desaparición, la lista del máster y los eventos históricos |
| Excepción contractual de borrado durante el juego | `InscripcionTest.borrarCuentaEnJuegoConservaInscripcionSinCuentaNiHorarioNiEventosNuevos`: conserva inscripción anónima sin horario |

La invitación por código es pública para una cuenta autenticada. El test de
rol exige ocultar los datos privados y la partida de «mis partidas», sin
bloquear esa consulta pública necesaria para poder inscribirse.

## Qué acredita cada capa

El backend se ejecuta con Spring Boot, HTTP real y PostgreSQL de
Testcontainers. Sus pruebas no sustituyen servicios ni repositorios. La
ampliación del borrado también comprueba persistencia y eventos históricos.

El móvil ejecuta componentes y cliente HTTP reales sustituyendo transporte,
navegación y componentes nativos. Comprueba el recorrido desde el QR o el
enlace hasta las normas, la salida sin aceptar y, al aceptar, la inscripción
con `POST /api/v1/inscripciones` y la sala con los inscritos que devuelve
`GET /api/v1/partidas/{partidaId}/inscritos`. `inscription.test.cjs` comprueba
además, pantalla a pantalla, el aviso de «ya inscrito», los errores del
servidor y el horario de la partida.

## Ejecución y CI

`ci-backend` ejecuta `verify`, que descubre las clases `*Test`.
`ci-mobile` ejecuta `typecheck`, `lint` y `npm test`; este último incluye las
pruebas de sesión, invitaciones, inscripción y los cinco casos de aceptación móviles.

Desde la raíz del repositorio, con Java 21 y Docker Desktop iniciado:

```powershell
cd backend
.\mvnw.cmd verify
cd ..\mobile
npm.cmd ci
npm.cmd run typecheck
npm.cmd run lint
npm.cmd test
cd ..
```
