# Aceptación de la PBI 11b — Anular una eliminación

**Tarea:** 11b.4 (#299) · **Test:** `AceptacionAnulacionTest`.

Estas pruebas recorren la **API HTTP real**: se crea y arranca una partida,
se producen eliminaciones con el código de vida, se abre una disputa y el máster
decide, o bien se produce una resolución automática por zona `DENTRO`.
La operación `ServicioAnulacion.anular` **no se invoca directamente**.
Se comprueba el estado de la base de datos y lo que reciben las API de carta y feed.
La infraestructura de pruebas utiliza Spring Boot y PostgreSQL/PostGIS con Testcontainers.

## Criterio 1 — Revivir, recalcular y rehacer el ciclo

| Test | Comportamiento comprobado |
| --- | --- |
| `criterio1LaDecisionReviveLaVictimaRecalculaElMarcadorYRehaceElCiclo` | Se anula por decisión favorable a la víctima; revive, el cazador pierde el punto, las cartas se actualizan y hay un ciclo único sin emparejar entre sí a los implicados cuando puede evitarse. |
| `criterio1UnaAnulacionEnCadenaNoRevocaLasEliminacionesDeOtros` | Dos eliminaciones sucesivas del mismo cazador se anulan juntas; una eliminación de otro cazador permanece, con su marcador. |
| `criterio1SiElCazadorEstaEliminadoNoReviveNiRecibeOtraMision` | El cazador ya eliminado permanece eliminado; solo se reasigna la misión del revivido, sin error de servidor. |
| `criterio1ConDosVivosElCicloEsReciprocoYSoloSeCambianMisionesAfectadas` | Con el cazador ya eliminado, la anulación deja exactamente dos supervivientes: se forma el único ciclo posible entre ellos, el tercero sigue eliminado y solo cambia la misión del revivido. |
| `criterio1SiSeDaLaRazonAlCazadorNoHayRevocacion` | Una resolución favorable al cazador no anula eliminaciones, no crea eventos de anulación y no puede resolverse dos veces. |
| `criterio1ZonaDentroAnulaAutomaticamenteYActualizaLaCarta` | La zona `DENTRO` da automáticamente la razón a la víctima y materializa la anulación. |
| `mobile/tests/caught-feed.test.cjs`: «al revivir por una anulación vuelve la carta con su misión nueva, y una eliminación nueva vuelve a avisar (11b.3)» | En la app, la víctima revivida vuelve a ver su carta con la misión nueva. |
| `mobile/tests/disputes.test.cjs`: «una eliminación anulada en cadena explica por qué se cerró la disputa» | En la app, la disputa de una eliminación anulada en cadena dice «Disputa cerrada: eliminación anulada». |

La verificación del ciclo recorre exactamente todos los vivos y exige volver al
origen sin repetir jugadores intermedios; los eliminados quedan fuera. Para un
caso con suficientes jugadores se verifica además que cazador y víctima no
quedan emparejados en ninguna dirección.

## Criterio 2 — Misiones de biblioteca y feed de todos

| Test | Comportamiento comprobado |
| --- | --- |
| `criterio2LasMisionesSeReasignanDeBibliotecaSinAprobacion` | Cazador y víctima reciben misiones activas de la biblioteca, distintas entre sí; no se altera la misión de un vivo ajeno a la anulación. Las cartas reflejan los cambios sin aprobación. |
| `criterio2ElFeedMuestraLaAnulacionATodosSinRevelarQuienCazo` | Jugadores vivos, eliminado si lo hubiera y máster consultan el feed y ven `ANULACION`, con alias y fecha pero sin revelar al cazador. Una cuenta ajena no puede verlo. |
| `criterio2ElFeedPublicaUnaAnulacionPorCadaRevividoDeLaCadena` | Cada jugador revivido de una cadena genera una entrada de tipo `ANULACION` en el feed. |
| `mobile/tests/caught-feed.test.cjs`: «el feed pinta una anulación en verde y dice que la víctima vuelve al juego» | En la app, el feed enseña la anulación con el alias de la víctima. |

La reconstrucción de eventos se comprueba adicionalmente en `ArranqueAceptacionTest`: `DISPUTA_ABIERTA`, `DISPUTA_RESPONDIDA`, `DISPUTA_ARBITRO_SORTEADO` y `DISPUTA_RESUELTA` deben reproducir las filas de `disputa`, incluso la anulación en cadena con una disputa `SIN_OBJETO`.

La consulta del feed conserva el histórico de eliminaciones y añade los avisos
de anulación. El formato público de una entrada es `tipo`, `alias` y `ocurridoEn`.

## Ejecución

Desde `backend/`, con Docker Desktop disponible para Testcontainers:

```powershell
.\mvnw.cmd '-Dtest=AceptacionAnulacionTest,AnulacionTest,MisionesAnulacionTest' test
.\mvnw.cmd -B clean verify
```

Las pruebas de la app de la #298, desde `mobile` y con las dependencias instaladas
(`npm.cmd ci`):

```powershell
node --test tests/caught-feed.test.cjs tests/disputes.test.cjs
```

No se modifica código de producción de las tareas #296, #297 ni #298.
La reproducción de eventos `ELIMINACION_ANULADA` ya está cubierta por
`ArranqueAceptacionTest` de la #296; aquí se prueba el flujo de aceptación HTTP.

Los tests de eliminación generan el código con `CodigosDePrueba.emitir`,
según el contrato de 60 segundos de la incidencia #261.
