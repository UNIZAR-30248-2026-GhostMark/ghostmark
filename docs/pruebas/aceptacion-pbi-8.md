# Aceptación de la PBI 8 — Cierre de partida

Tarea #204 (8.4), PBI #8. Las pruebas del servidor están en
[CierreTest.java](../../backend/src/test/java/es/unizar/ghostmark/backend/juego/CierreTest.java)
y las de la app en [game-close.test.cjs](../../mobile/tests/game-close.test.cjs).

| Criterio o caso | Prueba automática |
|---|---|
| 1. La partida termina cuando queda un único superviviente | `elUltimoVivoCierraLaPartidaAlEliminar` |
| 1. La partida termina al llegar la fecha de fin, y no un segundo antes | `laFechaDeFinCierraConElGanadorPorEliminaciones` |
| 1. La partida termina cuando la cierra el máster | `elMasterCierraConElGanadorPorEliminaciones` |
| 1. El máster cierra desde la pestaña Partida tras confirmar y se abre el fin; una partida ya cerrada abre el fin | `game-close.test.cjs`: «el máster cierra desde la pestaña Partida tras confirmar y se abre el fin (R11-24)» y «si la partida ya está cerrada, la pestaña Partida abre el fin» |
| 1. El fin y la tarjeta de Inicio dicen la hora real del cierre (R11-31) | `game-close.test.cjs`: «la pantalla de fin usa la hora real del cierre y no el fin previsto» y «la tarjeta grande de una partida cerrada dice cuándo se cerró realmente» |
| 2. En la fecha de fin gana quien tiene más eliminaciones | `laFechaDeFinCierraConElGanadorPorEliminaciones` |
| 2. Dos jugadores empatados en cabeza comparten el resultado | `laFechaDeFinCierraConEmpateEnCabeza`, con una eliminación para cada ganador y un tercer superviviente con cero |
| 2. La pantalla de fin dice quién gana, el empate o el último superviviente | `game-close.test.cjs`: «la pantalla de fin dice quién gana, la duración y vuelve a Inicio» y «la pantalla de fin dice el empate y el último superviviente» |
| El feed sigue visible después de cerrar | `elFeedSigueVisibleDespuesDelCierre` |
| Un jugador eliminado que intenta cerrar recibe `403 NO_ES_MASTER` | `unJugadorEliminadoNoPuedeCerrarLaPartida` |
| Quien no es máster no ve «Cerrar partida» | `game-close.test.cjs`: «un jugador que no es máster no ve las tareas ni «Cerrar partida» y vuelve a su misión» |
| El cierre por fecha y el del máster a la vez dejan un solo cierre | `elCierrePorFechaYElDelMasterSoloCierranUnaVez` |

## Cómo se comprueba

Las pruebas crean partidas, inscriben jugadores y envían eliminaciones por HTTP
contra el servidor con PostgreSQL/PostGIS de Testcontainers. Para simular la
fecha de fin se pasa ese instante a la tarea programada.

En la prueba de concurrencia se lanzan la tarea programada y el servicio que
usa el máster en dos hilos. Se llama al servicio directamente porque el
interceptor de HTTP comprueba los cierres pendientes antes de llegar al
controlador. Puede ganar cualquiera de las dos operaciones y se comprueba que
la partida termina cerrada con un solo evento `PARTIDA_CERRADA`.

Con Java 21 y Docker activos, desde `backend`:

```powershell
.\mvnw.cmd '-Dtest=CierreTest' test
.\mvnw.cmd clean verify
```

Desde `mobile`, para las pruebas enlazadas de la app:

```powershell
npm.cmd ci
node --test tests/game-close.test.cjs
```
