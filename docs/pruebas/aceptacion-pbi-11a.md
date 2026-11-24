# Aceptación de la PBI 11a — Abrir, responder y arbitrar disputas

Tarea #294 (11a.5), PBI #11. Las pruebas del servidor usan la implementación
#291 (apertura, respuesta, bloqueo y cierre) y la #292 (asignación, decisión y
resolución automática por zona). La presentación de las pantallas `g-cazado` y
`h-disputa` corresponde a la tarea de app #293.

## Trazabilidad de los cinco criterios

| Criterio | Prueba automática |
|---|---|
| 1. La víctima abre en los diez minutos siguientes y el cazador responde | `AceptacionDisputasTest.criterio1LaVictimaAbreAntesDeDiezMinutosYElCazadorResponde` |
| 1. El plazo de diez minutos ha vencido | `criterio1LaVictimaYaNoPuedeAbrirAlCumplirseLosDiezMinutos` |
| 1. El cazador abre por negativa hasta la fecha de fin y la víctima responde | `criterio1ElCazadorPuedeDisputarUnaNegativaHastaLaFechaDeFin` |
| 2. La disputa de la eliminación bloquea la siguiente eliminación del cazador hasta resolverla | `criterio2UnaDisputaAbiertaBloqueaOtraEliminacionDelCazador` |
| 3. El máster ve solo los datos previstos, los ajenos no acceden y puede confirmar o desestimar | `criterio3ElMasterArbitraConVistaLimitadaYConfirmaUnaNegativa` |
| 3. Si el máster está implicado, sorteo sin implicados y reasignación al vencer el plazo | `criterio3ConMasterImplicadoSeSorteaOtroArbitroYSeReasignaAlVencer` |
| 3. Si el máster no decide en el plazo, se sortea un jugador no implicado y el máster deja de ver la disputa | `criterio3SiElMasterNoDecideEnPlazoSeSorteaUnJugadorNoImplicado` |
| 4. `DENTRO` resuelve automáticamente a favor de la víctima | `criterio4ConZonaDentroLaDisputaSeResuelveAutomaticamente` |
| 4. `NO_CONCLUYENTE` requiere decisión del máster | `criterio4ConZonaNoConcluyenteDecideElMaster` |
| 5. El cierre espera el plazo de diez minutos y las disputas abiertas | `criterio5ElCierreEsperaElPlazoYLaResolucionDeDisputas` |
| 1. En la app, la víctima solo puede abrir la disputa mientras queden diez minutos | `mobile/tests/disputes.test.cjs`: «la víctima abre la disputa desde «Te han cazado» solo dentro de los 10 minutos» |
| 1. En la app, se envían el tipo y el texto, y el cazador puede abrir una disputa por negativa | `mobile/tests/disputes.test.cjs`: «abrir una disputa manda el tipo y el texto y sustituye el formulario por la disputa» y «sin bloqueo, «No me enseña el código» abre la disputa por negativa (R12-31)» |
| 1. En la app, el cazador responde una sola vez y espera la decisión | `mobile/tests/disputes.test.cjs`: «el cazador responde una vez a la disputa de su víctima y queda esperando al árbitro» |
| 2. En la app, una disputa abierta bloquea la introducción del código para eliminar | `mobile/tests/disputes.test.cjs`: «con una disputa abierta sobre una eliminación suya, «Eliminar» queda bloqueado» |
| 3. En la app, el árbitro puede resolver una negativa y una eliminación | `mobile/tests/disputes.test.cjs`: «el árbitro de una negativa elimina a la víctima o rechaza, sin resultado de zona» y «el árbitro de una eliminación ve la zona y puede anularla» |
| 3. La app distingue respuesta pendiente, árbitro y disputa sin objeto | `mobile/tests/disputes.test.cjs`: «mientras está abierta se espera la respuesta y luego al árbitro, sea quien sea» y «una disputa sin objeto no ofrece botones ni dice quién gana» |
| 4. En la app, el resultado muestra si ganó la víctima por zona; el servidor resuelve automáticamente `DENTRO` | `mobile/tests/disputes.test.cjs`: «el resultado distingue quién gana, la zona y la disputa sin objeto (R12-35)» |
| 5. En la app, el último jugador vivo espera el plazo y navega al cierre | `mobile/tests/disputes.test.cjs`: «el único vivo espera el plazo de disputa y pasa al fin cuando la partida se cierra» |

## Alcance de las pruebas

Las pruebas registran cuentas, crean una partida, inscriben jugadores y la
arrancan mediante la API HTTP real con PostgreSQL/PostGIS de Testcontainers.
Las eliminaciones usan el código de vida del objetivo y envían un resultado de
zona; las disputas, las respuestas y las decisiones se realizan por sus endpoints
HTTP autenticados. Se controla el reloj para cubrir las fronteras temporales sin
esperas reales, y se inspeccionan los eventos y estados persistidos.

En el criterio 3 se comprueban las claves de la vista del árbitro: alias del
cazador y de la víctima, misión, versiones de ambos, estado y plazos. No se
expone el identificador de inscripción de los implicados ni su posición. El
sorteo se verifica por sus restricciones, sin fijar qué jugador aleatorio sale.

La resolución a favor de la víctima en los criterios 3 y 4 publica el evento
`EliminacionAnulable`. **La restauración efectiva de jugadores, marcador y
ciclo es responsabilidad de la PBI 11b (#296), y no se exige en estas pruebas.**
La tarea de app #293 está integrada en `main`. Sus tests de
`mobile/tests/disputes.test.cjs` comprueban las pantallas `g-cazado` y
`h-disputa`, la apertura por negativa, la respuesta, el bloqueo de «Eliminar»,
la decisión del árbitro, la representación del resultado de zona y la espera
del último jugador vivo. La resolución automática de `DENTRO` y los plazos
del servidor se verifican en `AceptacionDisputasTest`; los tests móviles
comprueban su presentación y las interacciones de la app.

## Ejecución

Con Java 21 y Docker activos, desde `backend`:

```powershell
.\mvnw.cmd '-Dtest=AceptacionDisputasTest,DisputaTest,ArbitrajeTest' test
.\mvnw.cmd -B clean verify
```

`ci-backend` ejecuta `verify`, por lo que no es necesario modificar su flujo.

Las pruebas de la app de la #293, desde `mobile` y con las dependencias
instaladas (`npm.cmd ci`):

```powershell
node --test tests/disputes.test.cjs
```
