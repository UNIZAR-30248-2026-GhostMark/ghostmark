# PBI 3 — Tests de aceptación (#58)

Referencias: PBI 3 (#4), tarea 3.6 (#58) y los cuatro criterios de
`docs/pilaDeProducto/instantaneas/S1-antes-de-planificar.md`.

## Alcance y trazabilidad

La tarea pide al menos un test automático por cada caso de los criterios,
un test de acceso de un jugador no inscrito y ampliar el de borrado de cuenta.
La comprobación del horario después del arranque real se completa al integrar
la PBI 4. No exige una suite de todos los detalles de implementación de #57.

Se reutilizan las pruebas existentes de 3.2, 3.3 y 3.4. Los casos nuevos
completan la cobertura por capas, sin modificar producción ni migraciones.

| Criterio o caso | Pruebas automáticas |
| --- | --- |
| CA1: entrada por QR | `invitation.test.cjs`: lectura, navegación y permisos; `acceptance-inscription.test.cjs`: «entrar por QR consulta la partida y permite revisar sus normas» |
| CA1: entrada por enlace | `invitation.test.cjs`: app cerrada, app abierta y sesión; `acceptance-inscription.test.cjs`: «entrar por enlace consulta la partida y permite revisar sus normas» |
| CA1: el jugador inscrito aparece en la lista del máster | `InscripcionTest.inscribirseCopiaHorarioAceptaNormasYGuardaEvento`: alta mediante API real y comprobación de id y alias en la lista del máster |
| CA2: las cuatro normas se muestran antes de aceptar | `acceptance-inscription.test.cjs`: «muestra las cuatro normas antes de aceptar» |
| CA2: aceptación explícita y rechazo | `InscripcionTest.inscribirseCopiaHorarioAceptaNormasYGuardaEvento` y `normasNoAceptadasNoCreanInscripcionNiEvento` verifican aceptación guardada y rechazo sin inscripción |
| CA2: volver sin aceptar | `acceptance-inscription.test.cjs`: «volver sin aceptar no inscribe ni anuncia que el jugador se ha unido» |
| CA2: aceptación omitida o nula | `AceptacionInscripcionTest.ca2SinAceptacionNoApareceEnNingunaLista`, dos casos parametrizados |
| CA3: consultar y editar horario antes del arranque | `AceptacionInscripcionTest.ca3RevisarYEditarHorarioAntesDelArranqueConservaLaInscripcion` |
| CA3: conservar la copia al cambiar el horario general | `AceptacionInscripcionTest.ca3CambiarElHorarioGeneralNoReescribeElDeLaPartida` |
| CA3: horario inmutable después del arranque real | Pendiente de integrar PBI 4, excepción expresa de #58 |
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
navegación y componentes nativos. Comprueba QR/enlace hasta las normas y la
salida sin aceptar. El alta y la lista del máster se verifican por separado en
el servidor. Estas pruebas **no acreditan el recorrido completo de pulsar
«ACEPTO Y ME UNO» hasta ver la sala con una inscripción persistida**.

La versión recibida de #57 conserva problemas de integración en el botón de
aceptación, la lista de la sala y la navegación al horario por partida. Esta
corrección del alcance de los tests no resuelve esos problemas ni permite
declarar terminada toda la PBI 3. Deben verificarse al completar la integración.
Se retiran de esta suite las expectativas sobre solicitudes en curso,
doble pulsación y presentación de errores, adicionales al alcance de #58.
No se usan tests omitidos ni se simula un alta como sustituto del flujo real.

## Ejecución y CI

`ci-backend` ejecuta `verify`, que descubre las clases `*Test`.
`ci-mobile` ejecuta `typecheck`, `lint` y `npm test`; este último incluye las
pruebas de sesión, invitaciones y los cuatro nuevos casos móviles.

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

## Validación de esta revisión

- Móvil: sesión correcta y 23 tests pasan (19 existentes y 4 de esta suite),
  sin fallos ni omisiones, con las dependencias del entorno de revisión.
- Backend: no se cambia respecto al parche original, cuyo `verify` el autor
  ha ejecutado con `BUILD SUCCESS`. No se ha vuelto a ejecutar aquí: este
  entorno dispone de Java 17 y no de Docker.
- Typecheck y lint completos: pasaron en el equipo del autor con la versión
  anterior. Repetirlos tras aplicar esta corrección; no se afirma una ejecución
  completa local en el entorno de revisión.
- La ejecución de CI en GitHub debe confirmarse al publicar la rama; no se
  deduce de una ejecución local.
- Cámara y esquema nativo `ghostmark://`: la prueba en Android sigue siendo
  independiente de estas pruebas de componentes.
