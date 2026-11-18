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
| 4. `DENTRO` resuelve automáticamente a favor de la víctima | `criterio4ConZonaDentroLaDisputaSeResuelveAutomaticamente` |
| 4. `NO_CONCLUYENTE` requiere decisión del máster | `criterio4ConZonaNoConcluyenteDecideElMaster` |
| 5. El cierre espera el plazo de diez minutos y las disputas abiertas | `criterio5ElCierreEsperaElPlazoYLaResolucionDeDisputas` |

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
La vista y las interacciones de la app se comprueban en la tarea #293; esta
clase prueba la aceptación del contrato de servidor.

## Ejecución

Con Java 21 y Docker activos, desde `backend`:

```powershell
.\mvnw.cmd '-Dtest=AceptacionDisputasTest,DisputaTest,ArbitrajeTest' test
.\mvnw.cmd -B clean verify
```

`ci-backend` ejecuta `verify`, por lo que no es necesario modificar su flujo.
