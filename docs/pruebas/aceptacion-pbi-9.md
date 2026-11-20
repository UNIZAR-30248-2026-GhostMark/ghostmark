# Aceptación de la PBI 9 — Horarios seguros

Tarea #278 (9.4), PBI #9. Pruebas de aceptación del servidor, sobre el diseño
#275 y la implementación #276. La pantalla `f-dormido` y la cuenta atrás visual
se prueban en la tarea de app #277.

## Trazabilidad de los criterios

| Criterio o caso | Prueba automática |
|---|---|
| 1. El cazador está en horario lectivo: se rechaza eliminar | `AceptacionHorariosSegurosTest.criterio1SeRechazaLaEliminacionSiElCazadorEstaEnClase` |
| 1. La víctima está en horario lectivo: se rechaza eliminar | `criterio1SeRechazaLaEliminacionSiLaVictimaEstaEnClase` |
| 1. Fuera del horario lectivo: se acepta la eliminación | `criterio1FueraDelHorarioLectivoSeAceptaLaEliminacion` |
| 2. El objetivo pasa a no disponible y vuelve a disponible en el momento del cambio | `criterio2LaCartaCambiaAlEmpezarYTerminarLaClase` |
| 1. «Eliminar» explica el rechazo por horario y no descuenta intentos | `life-code-screens.test.cjs`: «Eliminar explica el horario lectivo sin descontar intentos» |
| 2. La carta dormida con la cuenta atrás y «Eliminar» desactivado, con el objetivo o el cazador en clase (`f-dormido`) | `mission-card.test.cjs`: «la carta dormida explica que el objetivo está en clase y desactiva Eliminar» y «la carta dormida dice que estás en clase si el horario es del cazador» |
| 2. La carta cambia sola en el momento del cambio | `mission-card.test.cjs`: «la carta se vuelve a pedir en el siguiente en punto o y media y se duerme si el objetivo entra en clase» |

## Cómo se comprueba

Los tests registran cuentas, crean una partida, inscriben tres jugadores y
arrancan la partida por HTTP contra el servidor real, usando PostgreSQL/PostGIS
de Testcontainers. El horario de la primera persona se establece antes de su
inscripción para que se copie a la partida.

Se simulan dos franjas lectivas consecutivas de martes, de 10:00 a 11:00,
mediante un reloj controlado en `Europe/Madrid`. El código válido se consulta
por el endpoint privado de la víctima, sin fijar secretos manualmente ni
suponer un orden concreto del ciclo de jugadores.

Los rechazos comprueban `409 HORARIO_LECTIVO`, `motivo` (`CAZADOR` o
`OBJETIVO`), `disponibleEn`, la conservación del estado de la víctima y que no
se consumen intentos: se envía el código correcto y otro incorrecto, y los dos
se rechazan sin compararlos. Fuera de horario se envía el código correcto y se
comprueba que la víctima queda eliminada.

La transición de disponibilidad se comprueba justo antes de las 10:00, a
las 10:00, a las 10:30 y a las 11:00. La carta pasa de `noDisponible = null`
a `OBJETIVO` con `disponibleEn = 11:00`, y vuelve a `null` al terminar
el horario; en ese instante se acepta la eliminación. Es una comprobación
de la **API**, no de la actualización visual de la app o su temporizador.

## Ejecución

Con Java 21 y Docker activos, desde `backend`:

```powershell
.\mvnw.cmd '-Dtest=AceptacionHorariosSegurosTest,HorarioEliminacionTest' test
.\mvnw.cmd -B clean verify
```

El flujo `ci-backend` ejecuta `verify`, sin cambios adicionales en CI.

Las pruebas de la app de la #277 se ejecutan desde `mobile` tras `npm ci`:

```powershell
node --test tests/mission-card.test.cjs tests/life-code-screens.test.cjs
```
